package com.ceoeg.aetherflow.core.common.security

import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.RoutingRule
import com.ceoeg.aetherflow.core.common.model.SubscriptionFeed
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class VpnBackupSnapshot(
    val archiveVersion: Int = 1,
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val settings: AppSettings,
    val profiles: List<TunnelProfile>,
    val subscriptions: List<SubscriptionFeed>,
    val routingRules: List<RoutingRule>,
)

/**
 * Encrypts and decrypts portable `.vpnbak` backup archives using PBKDF2-HMAC-SHA256
 * (210,000 OWASP-compliant iterations, 16-byte CSPRNG salt) + AES-256-GCM.
 */
@Singleton
class BackupArchiveManager @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }
    private val secureRandom = SecureRandom()

    fun exportEncryptedArchive(snapshot: VpnBackupSnapshot, password: String): ByteArray {
        require(password.length >= 6) { "Backup password must be at least 6 characters" }
        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { secureRandom.nextBytes(it) }
        val key = deriveAes256Key(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC_HEADER)
        val plaintext = json.encodeToString(VpnBackupSnapshot.serializer(), snapshot).encodeToByteArray()
        val ciphertext = cipher.doFinal(plaintext)

        return ByteBuffer.allocate(MAGIC_HEADER.size + SALT_BYTES + IV_BYTES + ciphertext.size)
            .put(MAGIC_HEADER)
            .put(salt)
            .put(iv)
            .put(ciphertext)
            .array()
    }

    fun importEncryptedArchive(archiveBytes: ByteArray, password: String): VpnBackupSnapshot {
        val minSize = MAGIC_HEADER.size + SALT_BYTES + IV_BYTES + 16
        require(archiveBytes.size >= minSize) { "Invalid or truncated .vpnbak archive" }
        val header = archiveBytes.copyOfRange(0, MAGIC_HEADER.size)
        require(header.contentEquals(MAGIC_HEADER)) { "Unrecognized .vpnbak magic header" }

        val saltStart = MAGIC_HEADER.size
        val ivStart = saltStart + SALT_BYTES
        val bodyStart = ivStart + IV_BYTES

        val salt = archiveBytes.copyOfRange(saltStart, ivStart)
        val iv = archiveBytes.copyOfRange(ivStart, bodyStart)
        val ciphertext = archiveBytes.copyOfRange(bodyStart, archiveBytes.size)

        val key = deriveAes256Key(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC_HEADER)
        val decrypted = cipher.doFinal(ciphertext)
        return json.decodeFromString(VpnBackupSnapshot.serializer(), decrypted.decodeToString())
    }

    private fun deriveAes256Key(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val raw = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(raw, "AES")
    }

    companion object {
        private val MAGIC_HEADER = "AFVPNBAK1".encodeToByteArray()
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val PBKDF2_ITERATIONS = 210_000
    }
}
