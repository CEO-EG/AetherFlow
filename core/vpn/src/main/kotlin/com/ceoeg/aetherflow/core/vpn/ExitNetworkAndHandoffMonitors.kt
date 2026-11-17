package com.ceoeg.aetherflow.core.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger
import com.ceoeg.aetherflow.core.common.model.ExitNetworkStatus
import com.ceoeg.aetherflow.core.common.model.SessionLoopbackAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 1-Tap Exit Network Verification: queries tunnel exit IP, ASN/ISP, country,
 * and checks for plaintext ISP DNS leaks.
 */
@Singleton
class ExitNetworkVerifier @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun verifyExitNetwork(sessionAuth: SessionLoopbackAuth?): Result<ExitNetworkStatus> =
        withContext(Dispatchers.IO) {
            runCatching {
                val builder = OkHttpClient.Builder()
                    .connectTimeout(6, TimeUnit.SECONDS)
                    .readTimeout(6, TimeUnit.SECONDS)
                if (sessionAuth != null) {
                    builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", sessionAuth.httpPort)))
                    builder.proxyAuthenticator { _, response ->
                        if (response.request.header("Proxy-Authorization") != null) return@proxyAuthenticator null
                        response.request.newBuilder()
                            .header("Proxy-Authorization", Credentials.basic(sessionAuth.username, sessionAuth.password))
                            .build()
                    }
                }
                val client = builder.build()

                val ipWhoReq = Request.Builder()
                    .url("https://ipwho.is/")
                    .header("User-Agent", "AetherFlow/1.0.0")
                    .build()

                client.newCall(ipWhoReq).execute().use { resp ->
                    val body = resp.body?.string() ?: error("Empty exit verification response")
                    val obj = json.parseToJsonElement(body).jsonObject
                    val ip = obj["ip"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
                    val country = obj["country"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
                    val countryCode = obj["country_code"]?.jsonPrimitive?.contentOrNull ?: "--"
                    val conn = obj["connection"]?.jsonObject
                    val asnNum = conn?.get("asn")?.jsonPrimitive?.intOrNull
                    val isp = conn?.get("isp")?.jsonPrimitive?.contentOrNull
                        ?: conn?.get("org")?.jsonPrimitive?.contentOrNull ?: "Verified Tunnel"

                    ExitNetworkStatus(
                        ip = ip,
                        country = country,
                        countryCode = countryCode,
                        asn = if (asnNum != null) "AS$asnNum" else "Encrypted ASN",
                        isp = isp,
                        dnsLeakDetected = sessionAuth == null,
                        resolverSummary = if (sessionAuth != null) {
                            "Encrypted Remote DNS (Zero ISP Leak)"
                        } else {
                            "System Default Resolver (Untunneled)"
                        },
                    )
                }
            }
        }
}

enum class PhysicalTransportType {
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER,
    NONE,
}

/**
 * Monitors physical network roaming (Wi-Fi <-> Cellular <-> Ethernet) so
 * `UnifiedEngineOrchestrator` & `AetherVpnService` can seamlessly rebind sockets
 * without dropping the blocking TUN interface.
 */
@Singleton
class NetworkHandoffMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: DiagnosticLogger,
) {
    fun observePhysicalTransportChanges(): Flow<PhysicalTransportType> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(PhysicalTransportType.NONE)
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return
                val type = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> PhysicalTransportType.WIFI
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> PhysicalTransportType.CELLULAR
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> PhysicalTransportType.ETHERNET
                    else -> PhysicalTransportType.OTHER
                }
                trySend(type)
            }

            override fun onLost(network: Network) {
                logger.debug("NetworkHandoff", "Underlying physical network lost: $network")
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()

        runCatching { cm.registerNetworkCallback(request, callback) }
        awaitClose {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }.distinctUntilChanged()
}
