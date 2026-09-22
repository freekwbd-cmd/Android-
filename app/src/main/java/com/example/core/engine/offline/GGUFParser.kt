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
    val fileSizeBytes: Long = 0,
    val estimatedRamMb: Long = 0,
    val isCompatibleWithDevice: Boolean = false,
    val compatibilityReason: String = ""
)

object GGUFParser {

    private const val GGUF_MAGIC = 0x46554747 // "GGUF" in little endian

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
                val read = fis.read(headerBuf)
                if (read < 24) {
                    return@withContext GGUFModelMetadata(isValid = false, errorMessage = "Could not read GGUF header")
                }

                val byteBuffer = ByteBuffer.wrap(headerBuf).order(ByteOrder.LITTLE_ENDIAN)
                val magic = byteBuffer.int

                if (magic != GGUF_MAGIC) {
                    return@withContext GGUFModelMetadata(
                        isValid = false,
                        errorMessage = "Invalid GGUF magic header. Expected 0x46554747 ('GGUF'), found 0x${Integer.toHexString(magic).uppercase()}",
                        fileSizeBytes = fileSize
                    )
                }

                val version = byteBuffer.int
                if (version !in 1..3) {
                    return@withContext GGUFModelMetadata(
                        isValid = false,
                        errorMessage = "Unsupported GGUF version: $version (expected v2 or v3)",
                        version = version,
                        fileSizeBytes = fileSize
                    )
                }

                val tensorCount = byteBuffer.long
                val kvCount = byteBuffer.long

                // Parse key-value metadata pairs up to a safety limit (first 100 entries or 2MB)
                var architecture = "unknown"
                var modelName = file.nameWithoutExtension
                var quantization = inferQuantizationFromName(file.name)
                var contextLength = 2048

                var parsedKVs = 0
                while (parsedKVs < kvCount && parsedKVs < 60) {
                    val key = readGGUFString(fis) ?: break
                    val valueType = readUInt32(fis) ?: break

                    when (key) {
                        "general.architecture" -> {
                            if (valueType == 8) {
                                architecture = readGGUFString(fis) ?: architecture
                            } else skipGGUFValue(fis, valueType)
                        }
                        "general.name" -> {
                            if (valueType == 8) {
                                modelName = readGGUFString(fis) ?: modelName
                            } else skipGGUFValue(fis, valueType)
                        }
                        "general.file_type" -> {
                            if (valueType == 4 || valueType == 5) {
                                val fileType = readUInt32(fis) ?: 0
                                quantization = decodeGGUFFileType(fileType)
                            } else skipGGUFValue(fis, valueType)
                        }
                        "$architecture.context_length", "llama.context_length", "qwen2.context_length" -> {
                            if (valueType in listOf(2, 3, 4, 5)) {
                                contextLength = (readUInt32(fis) ?: 2048).coerceIn(512, 131072)
                            } else if (valueType in listOf(10, 11)) {
                                contextLength = (readUInt64(fis) ?: 2048L).toInt().coerceIn(512, 131072)
                            } else skipGGUFValue(fis, valueType)
                        }
                        else -> {
                            skipGGUFValue(fis, valueType)
                        }
                    }
                    parsedKVs++
                }

                // Calculate memory requirement: Model weights + KV Cache budget
                val fileSizeMb = fileSize / (1024 * 1024)
                val kvCacheEstimateMb = (contextLength * 0.15f).toLong().coerceIn(64, 1024)
                val estimatedRamMb = fileSizeMb + kvCacheEstimateMb + 200 // 200MB execution scratchpad

                // Check device capability
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                val availableDeviceRamMb = memInfo.availMem / (1024 * 1024)

                val isCompatible = availableDeviceRamMb > estimatedRamMb && !memInfo.lowMemory
                val compatibilityReason = if (isCompatible) {
                    "Compatible: Device has ${availableDeviceRamMb}MB free RAM (Requires ~${estimatedRamMb}MB)"
                } else {
                    "Incompatible: Requires ~${estimatedRamMb}MB RAM, but device only has ${availableDeviceRamMb}MB available. Loading would trigger OOM crash."
                }

                return@withContext GGUFModelMetadata(
                    isValid = true,
                    magic = "GGUF",
                    version = version,
                    tensorCount = tensorCount,
                    kvCount = kvCount,
                    architecture = architecture,
                    modelName = modelName,
                    quantization = quantization,
                    contextLength = contextLength,
                    fileSizeBytes = fileSize,
                    estimatedRamMb = estimatedRamMb,
                    isCompatibleWithDevice = isCompatible,
                    compatibilityReason = compatibilityReason
                )
            }
        } catch (e: Exception) {
            return@withContext GGUFModelMetadata(
                isValid = false,
                errorMessage = "GGUF parsing exception: ${e.localizedMessage}",
                fileSizeBytes = fileSize
            )
        }
    }

    private fun inferQuantizationFromName(fileName: String): String {
        val upper = fileName.uppercase()
        return when {
            upper.contains("Q4_K_M") -> "Q4_K_M"
            upper.contains("Q4_K_S") -> "Q4_K_S"
            upper.contains("Q4_0") -> "Q4_0"
            upper.contains("Q5_K_M") -> "Q5_K_M"
            upper.contains("Q8_0") -> "Q8_0"
            upper.contains("F16") -> "F16"
            upper.contains("IQ3_M") -> "IQ3_M"
            else -> "4-bit (GGUF)"
        }
    }

    private fun decodeGGUFFileType(typeId: Int): String {
        return when (typeId) {
            0 -> "F32"
            1 -> "F16"
            2 -> "Q4_0"
            3 -> "Q4_1"
            7 -> "Q8_0"
            12 -> "Q4_K"
            14 -> "Q5_K"
            15 -> "Q6_K"
            else -> "Quantized ($typeId)"
        }
    }

    private fun readUInt32(fis: FileInputStream): Int? {
        val buf = ByteArray(4)
        if (fis.read(buf) < 4) return null
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun readUInt64(fis: FileInputStream): Long? {
        val buf = ByteArray(8)
        if (fis.read(buf) < 8) return null
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).long
    }

    private fun readGGUFString(fis: FileInputStream): String? {
        val lenBytes = ByteArray(8)
        if (fis.read(lenBytes) < 8) return null
        val length = ByteBuffer.wrap(lenBytes).order(ByteOrder.LITTLE_ENDIAN).long
        if (length <= 0 || length > 256) {
            // Safety guard: skip if excessively long or invalid
            if (length > 0) fis.skip(length)
            return null
        }
        val strBuf = ByteArray(length.toInt())
        val read = fis.read(strBuf)
        if (read < length.toInt()) return null
        return String(strBuf, Charsets.UTF_8)
    }

    private fun skipGGUFValue(fis: FileInputStream, type: Int) {
        val bytesToSkip = when (type) {
            0, 1 -> 1L // uint8, int8
            2, 3 -> 2L // uint16, int16
            4, 5, 6 -> 4L // uint32, int32, float32
            7 -> 1L // bool
            8 -> { // string
                val lenBytes = ByteArray(8)
                if (fis.read(lenBytes) < 8) return
                ByteBuffer.wrap(lenBytes).order(ByteOrder.LITTLE_ENDIAN).long
            }
            9 -> { // array
                val itemType = readUInt32(fis) ?: return
                val count = readUInt64(fis) ?: return
                var total = 0L
                for (i in 0 until count.coerceAtMost(20)) {
                    skipGGUFValue(fis, itemType)
                }
                0L
            }
            10, 11, 12 -> 8L // uint64, int64, float64
            else -> 0L
        }
        if (bytesToSkip > 0) {
            fis.skip(bytesToSkip)
        }
    }
}
