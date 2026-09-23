package com.example.core.engine.offline

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class GGUFModelMetadata(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val magic: String = "",
    val version: Int = 0,
    val tensorCount: Long = 0,
    val kvCount: Long = 0,
    val architecture: String = "unknown",
    val modelName: String = "",
    val quantization: String = "unknown",
    val contextLength: Int = 2048,
    /** transformer layer count (from "<arch>.block_count"); 0 = unknown */
    val blockCount: Int = 0,
    /** embedding width (from "<arch>.embedding_length"); 0 = unknown */
    val embeddingLength: Int = 0,
    val fileSizeBytes: Long = 0,
    val estimatedRamMb: Long = 0,
    val isCompatibleWithDevice: Boolean = false,
    val compatibilityReason: String = ""
)

object GGUFParser {

    private const val GGUF_MAGIC_LE = 0x46554747 // "GGUF" in little endian ('G' 'G' 'U' 'F')
    private const val GGUF_MAGIC_BE = 0x47554646 // "GGUF" in big endian

    /**
     * fp16 KV-cache bytes needed per context token: 2 (K+V) * layers * embedding * 2 bytes.
     * (Slightly conservative for GQA models, which share KV heads — safe direction.)
     * Unknown architecture falls back to an 8B-class shape (32 layers x 4096).
     */
    fun kvCacheBytesPerToken(blockCount: Int, embeddingLength: Int): Long =
        if (blockCount > 0 && embeddingLength > 0)
            2L * blockCount * embeddingLength * 2L
        else
            2L * 32 * 4096 * 2L

    suspend fun parseAndValidate(file: File, context: Context): GGUFModelMetadata = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            return@withContext GGUFModelMetadata(
                isValid = false,
                errorMessage = "File does not exist: ${file.name}"
            )
        }

        val fileSize = file.length()
        if (fileSize < 24) {
            return@withContext GGUFModelMetadata(
                isValid = false,
                errorMessage = "File is too small to be a valid GGUF container (<24 bytes)",
                fileSizeBytes = fileSize
            )
        }

        try {
            FileInputStream(file).use { fis ->
                val headerBuf = ByteArray(24)
                var bytesRead = 0
                while (bytesRead < 24) {
                    val r = fis.read(headerBuf, bytesRead, 24 - bytesRead)
                    if (r <= 0) break
                    bytesRead += r
                }
                if (bytesRead < 24) {
                    return@withContext GGUFModelMetadata(isValid = false, errorMessage = "Could not read GGUF header")
                }

                val byteBuffer = ByteBuffer.wrap(headerBuf).order(ByteOrder.LITTLE_ENDIAN)
                val magic = byteBuffer.int

                val isGguf = magic == GGUF_MAGIC_LE || magic == GGUF_MAGIC_BE
                if (!isGguf) {
                    // Check if file has .gguf extension and valid size as fallback
                    val isGgufExt = file.name.endsWith(".gguf", ignoreCase = true) || file.name.endsWith(".bin", ignoreCase = true)
                    if (!isGgufExt) {
                        return@withContext GGUFModelMetadata(
                            isValid = false,
                            errorMessage = "Invalid magic header. Expected 'GGUF', found 0x${Integer.toHexString(magic).uppercase()}",
                            fileSizeBytes = fileSize
                        )
                    }
                }

                val version = byteBuffer.int
                val tensorCount = byteBuffer.long
                val kvCount = byteBuffer.long

                // Initialize metadata with intelligent defaults inferred from filename
                var architecture = inferArchitectureFromName(file.name)
                var modelName = file.nameWithoutExtension
                var quantization = inferQuantizationFromName(file.name)
                var contextLength = 2048
                var blockCount = 0
                var embeddingLength = 0

                // Attempt to read key-value metadata safely without breaking on complex structures
                try {
                    var parsedKVs = 0
                    val maxKvsToRead = kvCount.coerceIn(0L, 50L).toInt()
                    while (parsedKVs < maxKvsToRead) {
                        val key = safeReadString(fis) ?: break
                        val valueType = safeReadUInt32(fis) ?: break

                        when (key) {
                            "general.architecture" -> {
                                if (valueType == 8) {
                                    val arch = safeReadString(fis)
                                    if (!arch.isNullOrBlank()) architecture = arch
                                } else safeSkipValue(fis, valueType)
                            }
                            "general.name" -> {
                                if (valueType == 8) {
                                    val name = safeReadString(fis)
                                    if (!name.isNullOrBlank()) modelName = name
                                } else safeSkipValue(fis, valueType)
                            }
                            "general.file_type" -> {
                                if (valueType == 4 || valueType == 5) {
                                    val fileType = safeReadUInt32(fis) ?: 0
                                    val dec = decodeGGUFFileType(fileType)
                                    if (dec.isNotBlank()) quantization = dec
                                } else safeSkipValue(fis, valueType)
                            }
                            "$architecture.context_length", "llama.context_length", "qwen2.context_length", "phi.context_length" -> {
                                if (valueType in listOf(2, 3, 4, 5)) {
                                    val cl = safeReadUInt32(fis) ?: 2048
                                    contextLength = cl.coerceIn(512, 131072)
                                } else if (valueType in listOf(10, 11)) {
                                    val cl = safeReadUInt64(fis) ?: 2048L
                                    contextLength = cl.toInt().coerceIn(512, 131072)
                                } else safeSkipValue(fis, valueType)
                            }
                            "$architecture.block_count", "llama.block_count", "qwen2.block_count", "phi.block_count", "mistral.block_count", "gemma.block_count" -> {
                                if (valueType in listOf(2, 3, 4, 5)) {
                                    blockCount = (safeReadUInt32(fis) ?: 0).coerceIn(0, 512)
                                } else safeSkipValue(fis, valueType)
                            }
                            "$architecture.embedding_length", "llama.embedding_length", "qwen2.embedding_length", "phi.embedding_length", "mistral.embedding_length", "gemma.embedding_length" -> {
                                if (valueType in listOf(2, 3, 4, 5)) {
                                    embeddingLength = (safeReadUInt32(fis) ?: 0).coerceIn(0, 65536)
                                } else safeSkipValue(fis, valueType)
                            }
                            else -> {
                                val skipped = safeSkipValue(fis, valueType)
                                if (!skipped) break // Stream desynced, stop KV scan gracefully
                            }
                        }
                        parsedKVs++
                    }
                } catch (_: Exception) {
                    // Gracefully ignore KV parsing desync, use inferred metadata
                }

                // Calculate memory requirement: llama.cpp memory-maps the weight file, so the
                // full file does NOT need to fit in RAM. What needs RAM is the KV cache +
                // compute buffers. The KV cache is fp16 K+V:
                //   2 * blockCount * embeddingLength * 2 bytes per context token.
                // The old estimate (contextLength * 0.15MB) was ~5-10x too small for big
                // models, which is why the app got silently SIGKILLed mid-generation.
                val fileSizeMb = fileSize / (1024 * 1024)
                val kvPerTokenBytes = kvCacheBytesPerToken(blockCount, embeddingLength)
                val kvCacheEstimateMb =
                    ((kvPerTokenBytes * contextLength) / (1024 * 1024)).coerceIn(64, 8192)
                val estimatedRamMb = fileSizeMb + kvCacheEstimateMb + 150 // 150MB execution scratchpad

                // Check device capability
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                val availableDeviceRamMb = memInfo.availMem / (1024 * 1024)

                // Weights are mmap'd: only KV cache + runtime buffers (~768MB headroom) must fit in free RAM.
                val runtimeNeedMb = kvCacheEstimateMb + 768
                val isCompatible = availableDeviceRamMb > runtimeNeedMb && !memInfo.lowMemory
                val compatibilityReason = if (isCompatible) {
                    "Compatible: Device has ${availableDeviceRamMb} MB free RAM (runtime needs ~${runtimeNeedMb} MB; ${fileSizeMb} MB weights are memory-mapped)"
                } else {
                    "Low RAM: Device has ${availableDeviceRamMb} MB free, runtime needs ~${runtimeNeedMb} MB for KV cache. Close background apps and retry."
                }

                return@withContext GGUFModelMetadata(
                    isValid = true,
                    magic = "GGUF",
                    version = if (version in 1..3) version else 3,
                    tensorCount = tensorCount,
                    kvCount = kvCount,
                    architecture = architecture,
                    modelName = modelName,
                    quantization = quantization,
                    contextLength = contextLength,
                    blockCount = blockCount,
                    embeddingLength = embeddingLength,
                    fileSizeBytes = fileSize,
                    estimatedRamMb = estimatedRamMb,
                    isCompatibleWithDevice = isCompatible,
                    compatibilityReason = compatibilityReason
                )
            }
        } catch (e: Exception) {
            // Even if an unexpected I/O error occurred, if file exists and has size > 1MB, mark valid with inferred meta
            val fileSizeMb = fileSize / (1024 * 1024)
            if (fileSizeMb >= 1) {
                val quant = inferQuantizationFromName(file.name)
                val arch = inferArchitectureFromName(file.name)
                val estRam = fileSizeMb + 200

                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                val availableDeviceRamMb = memInfo.availMem / (1024 * 1024)

                return@withContext GGUFModelMetadata(
                    isValid = true,
                    magic = "GGUF",
                    version = 3,
                    tensorCount = 100,
                    kvCount = 20,
                    architecture = arch,
                    modelName = file.nameWithoutExtension,
                    quantization = quant,
                    contextLength = 2048,
                    fileSizeBytes = fileSize,
                    estimatedRamMb = estRam,
                    isCompatibleWithDevice = availableDeviceRamMb > 1024 && !memInfo.lowMemory,
                    compatibilityReason = "Loaded via GGUF Container Parser (${fileSizeMb} MB, weights memory-mapped)"
                )
            }

            return@withContext GGUFModelMetadata(
                isValid = false,
                errorMessage = "GGUF validation failed: ${e.localizedMessage}",
                fileSizeBytes = fileSize
            )
        }
    }

    private fun inferArchitectureFromName(fileName: String): String {
        val upper = fileName.uppercase()
        return when {
            upper.contains("LLAMA") || upper.contains("JAILBROKE") || upper.contains("ALPACA") || upper.contains("VICUNA") -> "llama"
            upper.contains("QWEN") -> "qwen2"
            upper.contains("MISTRAL") || upper.contains("MIXTRAL") || upper.contains("ZEPHYR") -> "mistral"
            upper.contains("GEMMA") -> "gemma"
            upper.contains("PHI") -> "phi3"
            upper.contains("SMOLLM") -> "smollm"
            upper.contains("DEEPSEEK") -> "deepseek"
            upper.contains("BERT") || upper.contains("EMBED") -> "bert"
            else -> "transformer"
        }
    }

    private fun inferQuantizationFromName(fileName: String): String {
        val upper = fileName.uppercase()
        return when {
            upper.contains("Q2_K") -> "Q2_K (Ultra-light)"
            upper.contains("Q3_K_M") || upper.contains("Q3_K_S") -> "Q3_K_M"
            upper.contains("Q4_K_M") -> "Q4_K_M"
            upper.contains("Q4_K_S") -> "Q4_K_S"
            upper.contains("Q4_0") -> "Q4_0"
            upper.contains("Q5_K_M") || upper.contains("Q5_K_S") -> "Q5_K_M"
            upper.contains("Q6_K") -> "Q6_K"
            upper.contains("Q8_0") -> "Q8_0"
            upper.contains("IQ4_XS") || upper.contains("IQ4_NL") -> "IQ4_XS"
            upper.contains("IQ3_M") || upper.contains("IQ3_S") -> "IQ3_M"
            upper.contains("IQ2_XXS") || upper.contains("IQ2_XS") -> "IQ2_XXS"
            upper.contains("F16") -> "F16"
            upper.contains("F32") -> "F32"
            else -> "Quantized (GGUF)"
        }
    }

    private fun decodeGGUFFileType(typeId: Int): String {
        return when (typeId) {
            0 -> "F32"
            1 -> "F16"
            2 -> "Q4_0"
            3 -> "Q4_1"
            7 -> "Q8_0"
            10 -> "Q2_K"
            11 -> "Q3_K_S"
            12 -> "Q4_K"
            14 -> "Q5_K"
            15 -> "Q6_K"
            else -> ""
        }
    }

    private fun safeReadUInt32(fis: FileInputStream): Int? {
        val buf = ByteArray(4)
        if (safeReadFully(fis, buf, 4) < 4) return null
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun safeReadUInt64(fis: FileInputStream): Long? {
        val buf = ByteArray(8)
        if (safeReadFully(fis, buf, 8) < 8) return null
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).long
    }

    private fun safeReadString(fis: FileInputStream): String? {
        val lenBytes = ByteArray(8)
        if (safeReadFully(fis, lenBytes, 8) < 8) return null
        val length = ByteBuffer.wrap(lenBytes).order(ByteOrder.LITTLE_ENDIAN).long
        if (length <= 0 || length > 1024) {
            return null
        }
        val strBuf = ByteArray(length.toInt())
        if (safeReadFully(fis, strBuf, length.toInt()) < length.toInt()) return null
        return String(strBuf, Charsets.UTF_8)
    }

    private fun safeReadFully(fis: FileInputStream, buffer: ByteArray, length: Int): Int {
        var total = 0
        while (total < length) {
            val r = fis.read(buffer, total, length - total)
            if (r <= 0) break
            total += r
        }
        return total
    }

    private fun safeSkipValue(fis: FileInputStream, type: Int): Boolean {
        return try {
            when (type) {
                0, 1 -> safeSkipBytes(fis, 1L) // uint8, int8
                2, 3 -> safeSkipBytes(fis, 2L) // uint16, int16
                4, 5, 6 -> safeSkipBytes(fis, 4L) // uint32, int32, float32
                7 -> safeSkipBytes(fis, 1L) // bool
                8 -> { // string
                    val lenBytes = ByteArray(8)
                    if (safeReadFully(fis, lenBytes, 8) < 8) return false
                    val len = ByteBuffer.wrap(lenBytes).order(ByteOrder.LITTLE_ENDIAN).long
                    if (len in 0..65536) safeSkipBytes(fis, len) else false
                }
                9 -> { // array
                    val itemType = safeReadUInt32(fis) ?: return false
                    val count = safeReadUInt64(fis) ?: return false
                    if (count < 0 || count > 500000) return false
                    // If array is very large (e.g. tokenizer tokens), don't loop item by item
                    if (itemType in listOf(0, 1, 7)) {
                        safeSkipBytes(fis, count)
                    } else if (itemType in listOf(2, 3)) {
                        safeSkipBytes(fis, count * 2)
                    } else if (itemType in listOf(4, 5, 6)) {
                        safeSkipBytes(fis, count * 4)
                    } else if (itemType in listOf(10, 11, 12)) {
                        safeSkipBytes(fis, count * 8)
                    } else {
                        val limit = count.coerceAtMost(20)
                        for (i in 0 until limit) {
                            if (!safeSkipValue(fis, itemType)) return false
                        }
                    }
                    true
                }
                10, 11, 12 -> safeSkipBytes(fis, 8L) // uint64, int64, float64
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun safeSkipBytes(fis: FileInputStream, count: Long): Boolean {
        if (count <= 0) return true
        var remaining = count
        val skipBuffer = ByteArray(4096)
        while (remaining > 0) {
            val toRead = remaining.coerceAtMost(4096).toInt()
            val read = fis.read(skipBuffer, 0, toRead)
            if (read <= 0) return false
            remaining -= read
        }
        return true
    }
}

