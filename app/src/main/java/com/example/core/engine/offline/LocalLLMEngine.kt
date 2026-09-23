package com.example.core.engine.offline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

data class InferenceParameters(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 1024,
    val threadCount: Int = 4,
    val contextLength: Int = 2048
)

data class GenerationResult(
    val fullText: String,
    val tokenCount: Int,
    val latencyMs: Long,
    val tokensPerSecond: Float
)

interface LocalLLMEngine {
    val activeModelName: String
    val isModelLoaded: Boolean
    val allocatedMemoryMb: Long
    val backend: InferenceBackend

    suspend fun loadModel(modelId: String, modelPath: String?, modelName: String, estimatedRamMb: Long): ModelLoadResult
    suspend fun loadGGUFFile(file: File): ModelLoadResult
    suspend fun unloadModel()
    suspend fun generateStream(
        prompt: String,
        systemPrompt: String? = null,
        parameters: InferenceParameters = InferenceParameters(),
        onToken: (String) -> Unit
    ): GenerationResult
    fun cancelGeneration()
}

class VibeLocalLLMEngine(private val context: Context) : LocalLLMEngine {

    override val backend: InferenceBackend = AndroidLocalInferenceBackend(context)

    private val isCancelled = AtomicBoolean(false)

    override val activeModelName: String
        get() = if (backend.isModelLoaded) {
            backend.loadedModelPath?.let { File(it).name } ?: "GGUF Model"
        } else {
            "No Model Loaded"
        }

    override val isModelLoaded: Boolean
        get() = backend.isModelLoaded

    override val allocatedMemoryMb: Long
        get() = if (backend.isModelLoaded) 850L else 0L

    override suspend fun loadModel(
        modelId: String,
        modelPath: String?,
        modelName: String,
        estimatedRamMb: Long
    ): ModelLoadResult = withContext(Dispatchers.IO) {
        if (modelPath == null) {
            // Check if there is a local file in the workspace or models directory
            val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
            val candidate = File(modelsDir, "$modelName.gguf")
            return@withContext if (candidate.exists()) {
                backend.loadModel(candidate)
            } else {
                ModelLoadResult.Failure("Model file not found on device: $modelName.gguf")
            }
        }
        val file = File(modelPath)
        backend.loadModel(file)
    }

    override suspend fun loadGGUFFile(file: File): ModelLoadResult = withContext(Dispatchers.IO) {
        backend.loadModel(file)
    }

    override suspend fun unloadModel() = withContext(Dispatchers.IO) {
        backend.unloadModel()
    }

    override fun cancelGeneration() {
        isCancelled.set(true)
    }

    override suspend fun generateStream(
        prompt: String,
        systemPrompt: String?,
        parameters: InferenceParameters,
        onToken: (String) -> Unit
    ): GenerationResult = withContext(Dispatchers.Default) {
        isCancelled.set(false)

        val fullPrompt = if (systemPrompt != null) {
            "[System: $systemPrompt]\n\n[User: $prompt]"
        } else prompt

        backend.streamInference(fullPrompt, parameters, isCancelled, onToken)
    }
}
