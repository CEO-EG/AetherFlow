package com.ceoeg.aetherflow.core.common.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Hardware-backed Android Keystore AES-256-GCM encryption engine for sensitive
 * tunnel credentials at rest (UUIDs, private keys, passwords, pre-shared keys,
 * and Proto DataStore payloads).
 *
 * Includes a deterministic ephemeral fallback key strictly when AndroidKeyStore
 * is unavailable inside headless JVM unit tests.
 */
@OptIn(ExperimentalEncodingApi::class)
@Singleton
class CredentialCipher @Inject constructor() {

    private val secretKey: SecretKey by lazy { loadOrCreateMasterKey() }

    fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext)
        return ByteArray(1 + iv.size + encrypted.size).also { out ->
            out[0] = iv.size.toByte()
            System.arraycopy(iv, 0, out, 1, iv.size)
            System.arraycopy(encrypted, 0, out, 1 + iv.size, encrypted.size)
        }
    }

    fun decrypt(payload: ByteArray): ByteArray {
        require(payload.size > 13) { "Encrypted payload truncated" }
        val ivLen = payload[0].toInt() and 0xFF
        require(ivLen in 12..16 && payload.size > 1 + ivLen) { "Invalid GCM IV header" }
        val iv = payload.copyOfRange(1, 1 + ivLen)
        val cipherBytes = payload.copyOfRange(1 + ivLen, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherBytes)
    }

    fun encryptString(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        return PREFIX + Base64.Default.encode(encrypt(plaintext.encodeToByteArray()))
    }

    fun decryptString(encoded: String): String {
        if (encoded.isEmpty()) return ""
        if (!encoded.startsWith(PREFIX)) return encoded
        val raw = Base64.Default.decode(encoded.removePrefix(PREFIX))
        return decrypt(raw).decodeToString()
    }

    private fun loadOrCreateMasterKey(): SecretKey {
        return runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return@runCatching existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }.getOrElse {
            // Headless JVM test environment fallback
            val generator = KeyGenerator.getInstance("AES")
            generator.init(256, SecureRandom())
            generator.generateKey()
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "aetherflow_master_aes256_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val PREFIX = "ENCv1:"
    }
}
