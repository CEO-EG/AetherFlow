package com.ceoeg.aetherflow.core.common.security

import com.ceoeg.aetherflow.core.common.model.SecurityMode
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class PeerCertPinInfo(
    val sha256Hex: String,
    val certNames: List<String>,
    val isCaCert: Boolean,
)

/**
 * Resolves live X.509 peer certificate SHA-256 fingerprints (`pcs` / `pinnedPeerCertSha256`) and
 * certificate SAN/CN names (`vcn` / `verifyPeerCertByName`) for Xray-core v26.6+ compatibility.
 *
 * Xray-core v26.6+ removed legacy `allowInsecure` in favor of cryptographic leaf/CA pinning
 * (`pinnedPeerCertSha256`) and hostname override verification (`verifyPeerCertByName`), enabling
 * custom SNI / bug-host fronting (e.g., `sni=partners.playstation.net`) and self-signed server
 * certificates without exposing the tunnel to arbitrary MITM certificates.
 */
object TlsCertificatePinResolver {

    private val cnRegex = Regex("""(?:^|[,+])\s*CN=([^,+]+)""", RegexOption.IGNORE_CASE)

    suspend fun fetchServerCertPin(
        host: String,
        port: Int,
        sni: String,
        timeoutMs: Int = 3500,
    ): Result<PeerCertPinInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmedHost = host.trim().removeSurrounding("[", "]")
            require(trimmedHost.isNotEmpty() && port in 1..65535) {
                "Invalid endpoint host or port ($host:$port)"
            }

            var capturedChain: Array<X509Certificate>? = null
            @android.annotation.SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
            val captureTrustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                    capturedChain = chain
                }
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }

            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<TrustManager>(captureTrustManager), SecureRandom())
            }

            Socket().use { rawSocket ->
                rawSocket.soTimeout = timeoutMs
                rawSocket.connect(InetSocketAddress(trimmedHost, port), timeoutMs)

                val effectiveSni = sni.trim().ifBlank { trimmedHost }
                val sslSocket = (sslContext.socketFactory.createSocket(
                    rawSocket,
                    effectiveSni,
                    port,
                    true,
                ) as SSLSocket).apply {
                    soTimeout = timeoutMs
                    runCatching {
                        sslParameters = sslParameters.apply {
                            serverNames = listOf(SNIHostName(effectiveSni))
                        }
                    }
                }

                sslSocket.use { tls ->
                    runCatching { tls.startHandshake() }
                    val leaf = capturedChain?.firstOrNull()
                        ?: runCatching {
                            tls.session.peerCertificates.firstOrNull() as? X509Certificate
                        }.getOrNull()
                        ?: error("No X.509 certificate returned by $trimmedHost:$port")

                    val digest = MessageDigest.getInstance("SHA-256").digest(leaf.encoded)
                    val sha256Hex = digest.joinToString("") { "%02x".format(it) }
                    val certNames = extractCertificateNames(leaf)
                    val isCaCert = leaf.basicConstraints >= 0

                    PeerCertPinInfo(
                        sha256Hex = sha256Hex,
                        certNames = certNames,
                        isCaCert = isCaCert,
                    )
                }
            }
        }
    }

    /**
     * Automatically populates `pinnedPeerCertSha256` (and `verifyPeerCertByName` when the server
     * uses a self-signed `CA:TRUE` certificate) if `security == TLS` and `allowInsecure == true`.
     */
    suspend fun resolveProfileIfNeeded(profile: TunnelProfile): TunnelProfile {
        val tls = profile.tlsReality
        if (tls.security != SecurityMode.TLS || !tls.allowInsecure || tls.pinnedPeerCertSha256.isNotBlank()) {
            return profile
        }
        val info = fetchServerCertPin(
            host = profile.serverAddress,
            port = profile.serverPort,
            sni = tls.sni,
        ).getOrNull() ?: return profile

        val updatedVerifyName = if (info.isCaCert && tls.verifyPeerCertByName.isBlank() && info.certNames.isNotEmpty()) {
            info.certNames.joinToString(",")
        } else {
            tls.verifyPeerCertByName
        }

        return profile.copy(
            tlsReality = tls.copy(
                pinnedPeerCertSha256 = info.sha256Hex,
                verifyPeerCertByName = updatedVerifyName,
            )
        )
    }

    private fun extractCertificateNames(cert: X509Certificate): List<String> {
        val names = LinkedHashSet<String>()
        runCatching {
            cert.subjectAlternativeNames?.forEach { entry ->
                val generalNameType = entry.getOrNull(0) as? Int
                val value = entry.getOrNull(1)?.toString()?.trim().orEmpty()
                // 2 = dNSName, 7 = iPAddress
                if ((generalNameType == 2 || generalNameType == 7) && value.isNotEmpty()) {
                    names.add(value)
                }
            }
        }
        runCatching {
            val subjectPrincipal = cert.subjectX500Principal?.name.orEmpty()
            cnRegex.find(subjectPrincipal)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let {
                names.add(it)
            }
        }
        return names.toList()
    }
}
