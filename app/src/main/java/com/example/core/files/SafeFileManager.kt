package com.example.core.files

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class FileCategory {
    DOCUMENTS,
    CODE,
    IMAGES,
    AUDIO,
    VIDEO,
    ARCHIVES,
    OTHER
}

data class SafeFileItem(
    val file: File,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val category: FileCategory
)

object SafeFileManager {

    fun categorize(file: File): FileCategory {
        if (file.isDirectory) return FileCategory.OTHER
        val ext = file.extension.lowercase()
        return when (ext) {
            "txt", "md", "pdf", "doc", "docx", "csv", "json", "xml" -> FileCategory.DOCUMENTS
            "kt", "java", "py", "js", "ts", "c", "cpp", "h", "sh", "html", "css" -> FileCategory.CODE
            "jpg", "jpeg", "png", "webp", "gif", "svg" -> FileCategory.IMAGES
            "mp3", "wav", "m4a", "ogg", "flac" -> FileCategory.AUDIO
            "mp4", "mkv", "webm", "avi", "mov" -> FileCategory.VIDEO
            "zip", "tar", "gz", "rar", "7z" -> FileCategory.ARCHIVES
            else -> FileCategory.OTHER
        }
    }

    fun getRootWorkDirectory(context: Context): File {
        val dir = File(context.filesDir, "vibe_workspace")
        if (!dir.exists()) {
            dir.mkdirs()
            // Create some initial sample files
            populateSampleWorkspace(dir)
        }
        return dir
    }

    private fun populateSampleWorkspace(workDir: File) {
        val projectsDir = File(workDir, "projects").apply { mkdirs() }
        val sampleCode = File(projectsDir, "NeuralAgent.kt")
        if (!sampleCode.exists()) {
            sampleCode.writeText(
                """package com.example.agent

// VibeAI Autonomous Agent Pipeline
class NeuralAgent(val id: String) {
    fun executeTask(goal: String): Boolean {
        println("Decomposing goal: " + goal)
        return true
    }
}
"""
            )
        }

        val docsDir = File(workDir, "documents").apply { mkdirs() }
        val sampleDoc = File(docsDir, "VibeAI_Specs.md")
        if (!sampleDoc.exists()) {
            sampleDoc.writeText(
                """# VibeAI Workstation Specs
**Architect:** Shorif Uddin Piash

### Systems:
1. GGUF Local Neural LLM Engine.
2. OpenAI-Compatible Online Provider.
3. 7-Stage Autonomous Agent Controller.
4. AES-256 Storage Encryption.
"""
            )
        }
    }

    suspend fun listFiles(directory: File): List<SafeFileItem> = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.isDirectory) return@withContext emptyList()
        val files = directory.listFiles() ?: return@withContext emptyList()
        files.map { file ->
            SafeFileItem(
                file = file,
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isDirectory) 0 else file.length(),
                lastModified = file.lastModified(),
                category = categorize(file)
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    suspend fun readTextChunked(file: File, maxChars: Int = 12000): String = withContext(Dispatchers.IO) {
        if (!file.exists() || file.isDirectory) return@withContext ""
        try {
            val stream = file.inputStream()
            val buffer = ByteArray(maxChars)
            val bytesRead = stream.read(buffer)
            stream.close()
            if (bytesRead > 0) {
                String(buffer, 0, bytesRead, Charsets.UTF_8)
            } else ""
        } catch (e: Exception) {
            "Error reading file: ${e.localizedMessage}"
        }
    }

    suspend fun createZipArchive(filesToCompress: List<File>, destinationZip: File): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(destinationZip))).use { out ->
                    for (file in filesToCompress) {
                        if (file.isFile) {
                            FileInputStream(file).use { origin ->
                                val entry = ZipEntry(file.name)
                                out.putNextEntry(entry)
                                origin.copyTo(out)
                                out.closeEntry()
                            }
                        }
                    }
                }
                Result.success(destinationZip)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun extractZipSafely(zipFile: File, outputDir: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (!outputDir.exists()) outputDir.mkdirs()
            val canonicalDestDir = outputDir.canonicalPath
            var extractedCount = 0

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    val currentEntry = entry ?: break
                    val targetFile = File(outputDir, currentEntry.name)
                    val canonicalTarget = targetFile.canonicalPath

                    // Path Traversal Security Verification (CRITICAL)
                    if (!canonicalTarget.startsWith(canonicalDestDir + File.separator) &&
                        canonicalTarget != canonicalDestDir
                    ) {
                        throw SecurityException("Path Traversal vulnerability detected in zip entry: ${currentEntry.name}")
                    }

                    if (currentEntry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        extractedCount++
                    }
                    zis.closeEntry()
                }
            }
            Result.success(extractedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // AES-256 File Encryption
    suspend fun encryptFile(sourceFile: File, destFile: File, passphrase: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
                val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }

                val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val keySpec = PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256)
                val secretKey = SecretKeySpec(keyFactory.generateSecret(keySpec).encoded, "AES")

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

                FileOutputStream(destFile).use { fos ->
                    fos.write(salt)
                    fos.write(iv)
                    FileInputStream(sourceFile).use { fis ->
                        val inputBuffer = ByteArray(8192)
                        var bytesRead: Int
                        while (fis.read(inputBuffer).also { bytesRead = it } != -1) {
                            val output = cipher.update(inputBuffer, 0, bytesRead)
                            if (output != null) fos.write(output)
                        }
                        val finalOutput = cipher.doFinal()
                        if (finalOutput != null) fos.write(finalOutput)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // AES-256 File Decryption
    suspend fun decryptFile(sourceFile: File, destFile: File, passphrase: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                FileInputStream(sourceFile).use { fis ->
                    val salt = ByteArray(16)
                    val iv = ByteArray(16)
                    fis.read(salt)
                    fis.read(iv)

                    val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    val keySpec = PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256)
                    val secretKey = SecretKeySpec(keyFactory.generateSecret(keySpec).encoded, "AES")

                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

                    FileOutputStream(destFile).use { fos ->
                        val inputBuffer = ByteArray(8192)
                        var bytesRead: Int
                        while (fis.read(inputBuffer).also { bytesRead = it } != -1) {
                            val output = cipher.update(inputBuffer, 0, bytesRead)
                            if (output != null) fos.write(output)
                        }
                        val finalOutput = cipher.doFinal()
                        if (finalOutput != null) fos.write(finalOutput)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
