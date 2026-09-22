// JNI bridge: VibeAI (com.example) <-> llama.cpp
// Exposes: loadModel / generate (streaming w/ token callback) / cancel / unload
#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <android/log.h>

#include "llama.h"

#define LOG_TAG "llama-android"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct Session {
    llama_model *model = nullptr;
    llama_context *ctx = nullptr;
    llama_sampler *sampler = nullptr;
    std::atomic<bool> cancelled{false};
};

static std::string jstring_to_std(JNIEnv *env, jstring js) {
    const char *c = env->GetStringUTFChars(js, nullptr);
    std::string s(c ? c : "");
    if (c) env->ReleaseStringUTFChars(js, c);
    return s;
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_core_engine_offline_LlamaBridge_nativeLoadModel(
        JNIEnv *env, jobject /*thiz*/, jstring jmodelPath, jint jnCtx, jint jnThreads) {
    static bool backend_init = false;
    if (!backend_init) {
        llama_backend_init();
        backend_init = true;
    }

    std::string path = jstring_to_std(env, jmodelPath);
    LOGI("loading model: %s", path.c_str());

    llama_model_params mparams = llama_model_default_params();
    // keep mmap on: 5GB+ model must NOT be fully copied into RAM
    mparams.use_mmap = true;

    llama_model *model = llama_model_load_from_file(path.c_str(), mparams);
    if (!model) {
        LOGE("llama_model_load_from_file failed");
        return 0;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = (uint32_t) jnCtx;
    cparams.n_threads = jnThreads;
    cparams.n_threads_batch = jnThreads;

    llama_context *ctx = llama_init_from_model(model, cparams);
    if (!ctx) {
        LOGE("llama_init_from_model failed");
        llama_model_free(model);
        return 0;
    }

    // sampler chain: temp -> top_p -> dist
    llama_sampler *sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(1234));

    auto *sess = new Session();
    sess->model = model;
    sess->ctx = ctx;
    sess->sampler = sampler;

    LOGI("model loaded, n_ctx=%u", llama_n_ctx(ctx));
    return reinterpret_cast<jlong>(sess);
}

JNIEXPORT jint JNICALL
Java_com_example_core_engine_offline_LlamaBridge_nativeGenerate(
        JNIEnv *env, jobject /*thiz*/, jlong jhandle, jstring jprompt,
        jint jmaxTokens, jfloat jtemp, jfloat jtopP, jobject jcallback) {
    auto *sess = reinterpret_cast<Session *>(jhandle);
    if (!sess || !sess->model || !sess->ctx || !sess->sampler) return -1;

    sess->cancelled.store(false);

    jclass cbCls = env->GetObjectClass(jcallback);
    jmethodID onToken = env->GetMethodID(cbCls, "onToken", "(Ljava/lang/String;)V");
    if (!onToken) {
        LOGE("callback onToken not found");
        return -2;
    }

    // rebuild sampler with requested temp/top_p
    llama_sampler_free(sess->sampler);
    sess->sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sess->sampler, llama_sampler_init_temp(jtemp));
    llama_sampler_chain_add(sess->sampler, llama_sampler_init_top_p(jtopP, 1));
    llama_sampler_chain_add(sess->sampler, llama_sampler_init_dist((uint32_t) time(nullptr)));

    std::string prompt = jstring_to_std(env, jprompt);

    const int n_ctx = (int) llama_n_ctx(sess->ctx);
    std::vector<llama_token> tokens(n_ctx);
    int n_tokens = llama_tokenize(sess->model, prompt.c_str(), (int) prompt.size(),
                                 tokens.data(), (int) tokens.size(), true, true);
    if (n_tokens < 0) {
        // buffer too small (shouldn't happen); retry with bigger buffer
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(sess->model, prompt.c_str(), (int) prompt.size(),
                                 tokens.data(), (int) tokens.size(), true, true);
    }
    if (n_tokens <= 0) return -3;
    tokens.resize(n_tokens);

    if (n_tokens >= n_ctx - 4) {
        LOGE("prompt too long for context");
        return -4;
    }

    // evaluate prompt
    llama_batch batch = llama_batch_get_one(tokens.data(), n_tokens);
    if (llama_decode(sess->ctx, batch) != 0) {
        LOGE("llama_decode(prompt) failed");
        return -5;
    }

    int n_past = n_tokens;
    int generated = 0;
    char piece[256];

    for (int i = 0; i < jmaxTokens; i++) {
        if (sess->cancelled.load()) break;

        llama_token tok = llama_sampler_sample(sess->sampler, sess->ctx, -1);
        if (llama_token_is_eog(sess->model, tok)) break;

        int n = llama_token_to_piece(sess->model, tok, piece, sizeof(piece), 0, true);
        if (n > 0) {
            jstring jtok = env->NewStringUTF(std::string(piece, n).c_str());
            env->CallVoidMethod(jcallback, onToken, jtok);
            env->DeleteLocalRef(jtok);
            if (env->ExceptionCheck()) { env->ExceptionClear(); break; }
        }

        llama_sampler_accept(sess->sampler, tok);
        generated++;

        llama_batch b = llama_batch_get_one(&tok, 1);
        if (llama_decode(sess->ctx, b) != 0) break;
        n_past++;

        if (n_past >= n_ctx - 4) break; // context full
    }

    LOGI("generated %d tokens", generated);
    return generated;
}

JNIEXPORT void JNICALL
Java_com_example_core_engine_offline_LlamaBridge_nativeCancel(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong jhandle) {
    auto *sess = reinterpret_cast<Session *>(jhandle);
    if (sess) sess->cancelled.store(true);
}

JNIEXPORT void JNICALL
Java_com_example_core_engine_offline_LlamaBridge_nativeUnload(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong jhandle) {
    auto *sess = reinterpret_cast<Session *>(jhandle);
    if (!sess) return;
    if (sess->sampler) llama_sampler_free(sess->sampler);
    if (sess->ctx) llama_free(sess->ctx);
    if (sess->model) llama_model_free(sess->model);
    delete sess;
    LOGI("model unloaded");
}

} // extern "C"
