// JNI bridge: VibeAI (com.example) <-> llama.cpp
// Exposes: loadModel / generate (streaming w/ token callback) / cancel / unload
#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <ctime>
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

// Convert raw UTF-8 bytes to a Java String without ever aborting the runtime.
// (env->NewStringUTF() kills the process on malformed input, and token pieces
// from byte-fallback BPE tokens are frequently not valid UTF-8.)
static jstring utf8_bytes_to_jstring(JNIEnv *env, const char *s, size_t len) {
    std::vector<jchar> utf16;
    utf16.reserve(len);
    size_t i = 0;
    while (i < len) {
        uint32_t cp = 0xFFFD;
        size_t n = 1;
        unsigned char c0 = static_cast<unsigned char>(s[i]);
        if (c0 < 0x80) {
            cp = c0; n = 1;
        } else if ((c0 >> 5) == 0x6 && i + 1 < len) {
            unsigned char c1 = static_cast<unsigned char>(s[i + 1]);
            if ((c1 >> 6) == 0x2) {
                cp = ((c0 & 0x1F) << 6) | (c1 & 0x3F); n = 2;
                if (cp < 0x80) cp = 0xFFFD;
            }
        } else if ((c0 >> 4) == 0xE && i + 2 < len) {
            unsigned char c1 = static_cast<unsigned char>(s[i + 1]);
            unsigned char c2 = static_cast<unsigned char>(s[i + 2]);
            if ((c1 >> 6) == 0x2 && (c2 >> 6) == 0x2) {
                cp = ((c0 & 0x0F) << 12) | ((c1 & 0x3F) << 6) | (c2 & 0x3F); n = 3;
                if (cp < 0x800 || (cp >= 0xD800 && cp <= 0xDFFF)) cp = 0xFFFD;
            }
        } else if ((c0 >> 3) == 0x1E && i + 3 < len) {
            unsigned char c1 = static_cast<unsigned char>(s[i + 1]);
            unsigned char c2 = static_cast<unsigned char>(s[i + 2]);
            unsigned char c3 = static_cast<unsigned char>(s[i + 3]);
            if ((c1 >> 6) == 0x2 && (c2 >> 6) == 0x2 && (c3 >> 6) == 0x2) {
                cp = ((c0 & 0x07) << 18) | ((c1 & 0x3F) << 12) | ((c2 & 0x3F) << 6) | (c3 & 0x3F);
                n = 4;
                if (cp < 0x10000 || cp > 0x10FFFF) cp = 0xFFFD;
            }
        }
        if (cp <= 0xFFFF) {
            utf16.push_back(static_cast<jchar>(cp));
        } else {
            cp -= 0x10000;
            utf16.push_back(static_cast<jchar>(0xD800 + (cp >> 10)));
            utf16.push_back(static_cast<jchar>(0xDC00 + (cp & 0x3FF)));
        }
        i += n;
    }
    return env->NewString(utf16.data(), (jsize) utf16.size());
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_core_engine_offline_LlamaBridge_nativeLoadModel(
        JNIEnv *env, jobject /*thiz*/, jstring jmodelPath, jint jnCtx, jint jnThreads) {
    try {
    static bool backend_init = false;
    if (!backend_init) {
        llama_backend_init();
        backend_init = true;
    }

    std::string path = jstring_to_std(env, jmodelPath);
    LOGI("loading model: %s", path.c_str());

    llama_model_params mparams = llama_model_default_params();
    // note: current llama.cpp mmaps weights by default (no full RAM copy)

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
    } catch (const std::exception &e) {
        LOGE("nativeLoadModel crashed: %s", e.what());
        return 0;
    } catch (...) {
        LOGE("nativeLoadModel crashed: unknown");
        return 0;
    }
}

JNIEXPORT jint JNICALL
Java_com_example_core_engine_offline_LlamaBridge_nativeGenerate(
        JNIEnv *env, jobject /*thiz*/, jlong jhandle, jstring jprompt,
        jint jmaxTokens, jfloat jtemp, jfloat jtopP, jobject jcallback) {
    try {
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

    const struct llama_vocab *vocab = llama_model_get_vocab(sess->model);
    std::string prompt = jstring_to_std(env, jprompt);

    const int n_ctx = (int) llama_n_ctx(sess->ctx);
    std::vector<llama_token> tokens(n_ctx);
    int n_tokens = llama_tokenize(vocab, prompt.c_str(), (int) prompt.size(),
                                  tokens.data(), (int) tokens.size(), true, true);
    if (n_tokens < 0) {
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(vocab, prompt.c_str(), (int) prompt.size(),
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
        if (llama_vocab_is_eog(vocab, tok)) break;

        int n = llama_token_to_piece(vocab, tok, piece, sizeof(piece), 0, true);
        if (n > 0) {
            jstring jtok = utf8_bytes_to_jstring(env, piece, (size_t) n);
            if (jtok) {
                env->CallVoidMethod(jcallback, onToken, jtok);
                env->DeleteLocalRef(jtok);
            }
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
    } catch (const std::exception &e) {
        LOGE("nativeGenerate crashed: %s", e.what());
        return -6;
    } catch (...) {
        LOGE("nativeGenerate crashed: unknown");
        return -6;
    }
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
