package com.example.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val SALT_LENGTH_BYTES = 16
    private const val PBKDF2_ITERATIONS = 100000

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEYSTORE_ALIAS = "vibe_secure_credentials_key"

    // --- AES-GCM Authenticated File Encryption ---

    suspend fun encryptFileGcm(
        sourceFile: File,
        destFile: File,
        passphrase: CharArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Source file does not exist"))
            }

            val random = SecureRandom()
            val salt = ByteArray(SALT_LENGTH_BYTES).apply { random.nextBytes(this) }
            val iv = ByteArray(GCM_IV_LENGTH_BYTES).apply { random.nextBytes(this) }

            val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keySpec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, 256)
            val secretKey = SecretKeySpec(keyFactory.generateSecret(keySpec).encoded, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            FileOutputStream(destFile).use { fos ->
                // Write Salt and IV in header
                fos.write(salt)
                fos.write(iv)

                FileInputStream(sourceFile).use { fis ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (fis.read(buffer).also { read = it } != -1) {
                        val output = cipher.update(buffer, 0, read)
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

    suspend fun decryptFileGcm(
        sourceFile: File,
        destFile: File,
        passphrase: CharArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Encrypted file does not exist"))
            }

            FileInputStream(sourceFile).use { fis ->
                val salt = ByteArray(SALT_LENGTH_BYTES)
                val iv = ByteArray(GCM_IV_LENGTH_BYTES)

                val saltRead = fis.read(salt)
                val ivRead = fis.read(iv)

                if (saltRead < SALT_LENGTH_BYTES || ivRead < GCM_IV_LENGTH_BYTES) {
                    return@withContext Result.failure(IllegalStateException("File is too small or corrupted (missing salt/IV header)"))
                }

                val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val keySpec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, 256)
                val secretKey = SecretKeySpec(keyFactory.generateSecret(keySpec).encoded, "AES")

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

                FileOutputStream(destFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (fis.read(buffer).also { read = it } != -1) {
                        val output = cipher.update(buffer, 0, read)
                        if (output != null) fos.write(output)
                    }
                    val finalOutput = cipher.doFinal()
                    if (finalOutput != null) fos.write(finalOutput)
                }
            }
            Result.success(Unit)
        } catch (e: AEADBadTagException) {
            Result.failure(SecurityException("Authentication failed: Incorrect passphrase or file ciphertext was modified."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Android Keystore Credential Encryption ---

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keyGenSpec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    fun encryptCredential(plaintext: String): String {
        if (plaintext.isBlank()) return ""
        return try {
            val key = getOrCreateKeystoreKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = iv + encryptedBytes
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            plaintext // Fallback if keystore unavailable
        }
    }

    fun decryptCredential(cipherTextBase64: String): String {
        if (cipherTextBase64.isBlank()) return ""
        return try {
            val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH_BYTES) return cipherTextBase64

            val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val encrypted = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)

            val key = getOrCreateKeystoreKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val decryptedBytes = cipher.doFinal(encrypted)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            cipherTextBase64 // Return raw string if not encrypted
        }
    }

    fun redactCredential(key: String): String {
        if (key.isBlank()) return "Not configured"
        if (key.length <= 8) return "••••••••"
        return "${key.take(3)}••••••••${key.takeLast(4)}"
    }
}
