package com.example.core.engine.offline

/**
 * JNI bridge to llama.cpp (libllama-android.so, arm64-v8a).
 * Built from llama.cpp + this repo's jni/llama-android.cpp via Android NDK.
 */
object LlamaBridge {

    val isAvailable: Boolean

    /** Why the native library failed to load (null when loaded OK). */
    val loadError: String?

    init {
        var ok = false
        var err: String? = null
        try {
            System.loadLibrary("llama-android")
            ok = true
        } catch (e: UnsatisfiedLinkError) {
            err = e.message
        } catch (e: SecurityException) {
            err = e.message
        }
        isAvailable = ok
        loadError = err
    }

    interface TokenCallback {
        fun onToken(token: String)
    }

    /** @return native session handle, 0 on failure */
    external fun nativeLoadModel(modelPath: String, nCtx: Int, nThreads: Int): Long

    /**
     * Runs the full generation loop, streaming tokens through [callback].
     * @return generated token count, negative on error
     */
    external fun nativeGenerate(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        callback: TokenCallback
    ): Int

    external fun nativeCancel(handle: Long)

    external fun nativeUnload(handle: Long)
}
