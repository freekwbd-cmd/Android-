package com.example.core.network

import android.content.Context
import com.example.core.engine.offline.GGUFModelMetadata
import com.example.core.engine.offline.GGUFParser
import com.example.data.AppDatabase
import com.example.data.model.LocalModelEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

enum class DownloadStatus {
    IDLE,
    CONNECTING,
    DOWNLOADING,
    VALIDATING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class OnlineModelPreset(
    val id: String,
    val name: String,
    val description: String,
    val parameterSize: String,
    val quantization: String,
    val approxSizeMb: Long,
    val directUrl: String,
    val author: String,
    val isRecommended: Boolean = false
)

data class DownloadProgress(
    val modelId: String = "",
    val modelName: String = "",
    val status: DownloadStatus = DownloadStatus.IDLE,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progressPercent: Float = 0f,
    val speedBytesPerSec: Long = 0L,
    val errorMessage: String? = null,
    val targetFile: File? = null
)

class ModelDownloader(
    private val context: Context,
    private val db: AppDatabase,
    private val scope: CoroutineScope
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _downloadProgress = MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    private var activeJob: Job? = null

    companion object {
        val CURATED_MODELS = listOf(
            OnlineModelPreset(
                id = "smollm2-135m-q4km",
                name = "SmolLM2 135M Instruct",
                description = "Ultra-compact mobile checkpoint. Instant response, minimal memory (<150MB RAM).",
                parameterSize = "135M",
                quantization = "Q4_K_M",
                approxSizeMb = 95,
                directUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct-GGUF/resolve/main/smollm2-135m-instruct-q4_k_m.gguf",
                author = "HuggingFaceTB",
                isRecommended = true
            ),
            OnlineModelPreset(
                id = "smollm2-360m-q4km",
                name = "SmolLM2 360M Instruct",
                description = "Exceptional reasoning-to-size ratio. Extremely responsive on mobile devices.",
                parameterSize = "360M",
                quantization = "Q4_K_M",
                approxSizeMb = 229,
                directUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q4_k_m.gguf",
                author = "HuggingFaceTB",
                isRecommended = true
            ),
            OnlineModelPreset(
                id = "qwen2.5-0.5b-q4km",
                name = "Qwen2.5 0.5B Instruct",
                description = "Alibaba's advanced multilingual architecture. Outstanding code & instruction following.",
                parameterSize = "0.5B",
                quantization = "Q4_K_M",
                approxSizeMb = 398,
                directUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
                author = "Qwen Team"
            ),
            OnlineModelPreset(
                id = "tinyllama-1.1b-q4km",
                name = "TinyLlama 1.1B Chat",
                description = "Trained on 3 trillion tokens. Solid general conversational assistant.",
                parameterSize = "1.1B",
                quantization = "Q4_K_M",
                approxSizeMb = 669,
                directUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
                author = "TheBloke"
            )
        )
    }

    fun startDownload(modelName: String, url: String, modelId: String = "model_${System.currentTimeMillis()}") {
        activeJob?.cancel()

        activeJob = scope.launch(Dispatchers.IO) {
            val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
            val cleanFileName = if (url.contains("/")) {
                val candidate = url.substringAfterLast("/").substringBefore("?")
                if (candidate.endsWith(".gguf", ignoreCase = true)) candidate else "$modelId.gguf"
            } else {
                "$modelId.gguf"
            }
            val destinationFile = File(modelsDir, cleanFileName)

            _downloadProgress.value = DownloadProgress(
                modelId = modelId,
                modelName = modelName,
                status = DownloadStatus.CONNECTING,
                targetFile = destinationFile
            )

            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "VibeAI-Android-Client/2.5")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    _downloadProgress.value = _downloadProgress.value.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = "HTTP Error ${response.code}: ${response.message}"
                    )
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    _downloadProgress.value = _downloadProgress.value.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = "Empty response body from remote server"
                    )
                    return@launch
                }

                val contentLength = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(destinationFile)

                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L
                var lastSpeedCalcTime = System.currentTimeMillis()
                var bytesSinceLastSpeedCalc = 0L
                var currentSpeed = 0L

                _downloadProgress.value = _downloadProgress.value.copy(
                    status = DownloadStatus.DOWNLOADING,
                    totalBytes = contentLength
                )

                try {
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        bytesSinceLastSpeedCalc += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastSpeedCalcTime >= 500) {
                            val timeDeltaSec = (now - lastSpeedCalcTime) / 1000f
                            if (timeDeltaSec > 0) {
                                currentSpeed = (bytesSinceLastSpeedCalc / timeDeltaSec).toLong()
                            }
                            lastSpeedCalcTime = now
                            bytesSinceLastSpeedCalc = 0L

                            val progress = if (contentLength > 0) {
                                (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            _downloadProgress.value = _downloadProgress.value.copy(
                                downloadedBytes = totalBytesRead,
                                totalBytes = contentLength,
                                progressPercent = progress,
                                speedBytesPerSec = currentSpeed
                            )
                        }
                    }
                    outputStream.flush()
                } finally {
                    outputStream.close()
                    inputStream.close()
                }

                // Download Finished -> Validate GGUF Magic & Headers
                _downloadProgress.value = _downloadProgress.value.copy(
                    status = DownloadStatus.VALIDATING,
                    progressPercent = 1f,
                    downloadedBytes = totalBytesRead
                )

                val metadata: GGUFModelMetadata = GGUFParser.parseAndValidate(destinationFile, context)

                // Register in Room Database
                val entity = LocalModelEntity(
                    id = modelId,
                    name = modelName.ifBlank { metadata.modelName },
                    parameterSize = if (metadata.isValid) metadata.architecture else "Custom",
                    quantization = if (metadata.isValid) metadata.quantization else "Q4_K_M",
                    fileSizeMb = destinationFile.length() / (1024 * 1024),
                    ramEstimateMb = if (metadata.isValid) metadata.estimatedRamMb else (destinationFile.length() / (1024 * 1024)) + 200,
                    contextLength = if (metadata.isValid) metadata.contextLength else 2048,
                    backend = "GGUF / llama.cpp",
                    status = "INSTALLED",
                    downloadProgress = 100,
                    filePath = destinationFile.absolutePath,
                    isDefault = false
                )

                db.localModelDao().insertOrUpdate(entity)

                _downloadProgress.value = _downloadProgress.value.copy(
                    status = DownloadStatus.COMPLETED
                )

            } catch (e: CancellationException) {
                if (destinationFile.exists()) destinationFile.delete()
                _downloadProgress.value = _downloadProgress.value.copy(
                    status = DownloadStatus.CANCELLED,
                    errorMessage = "Download was cancelled by user"
                )
            } catch (e: Exception) {
                if (destinationFile.exists()) destinationFile.delete()
                _downloadProgress.value = _downloadProgress.value.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Download failed unexpectedly"
                )
            }
        }
    }

    fun cancelDownload() {
        activeJob?.cancel()
        _downloadProgress.value = _downloadProgress.value.copy(
            status = DownloadStatus.CANCELLED,
            errorMessage = "Download cancelled"
        )
    }

    fun resetState() {
        _downloadProgress.value = DownloadProgress()
    }
}
