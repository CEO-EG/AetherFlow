package com.ceoeg.aetherflow.core.vpn

import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.SessionLoopbackAuth
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import com.ceoeg.aetherflow.core.common.security.TlsCertificatePinResolver
import com.ceoeg.aetherflow.core.xray.XrayConfigBuilder
import com.ceoeg.aetherflow.core.xray.XrayNativeBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

data class NodeProbeResult(
    val profileId: String,
    val tcpPingMs: Long,
    val httpDelayMs: Long,
    val downloadSpeedMbps: Double = 0.0,
) {
    val isReachable: Boolean
        get() = httpDelayMs > 0L || tcpPingMs > 0L

    val effectiveScoreMs: Long
        get() = when {
            httpDelayMs > 0L -> httpDelayMs
            tcpPingMs > 0L -> tcpPingMs + 150L
            else -> Long.MAX_VALUE
        }
}

interface EndpointProber {
    suspend fun measureTcpHandshake(host: String, port: Int, timeoutMs: Int = 3000): Long
    suspend fun measureProfileDelay(profile: TunnelProfile, settings: AppSettings): NodeProbeResult
    suspend fun measureActiveTunnelHttpDelay(testUrl: String, sessionAuth: SessionLoopbackAuth?): Long
    suspend fun measureActiveDownloadSpeedMbps(speedUrl: String, sessionAuth: SessionLoopbackAuth?): Double
}

@Singleton
class DefaultEndpointProber @Inject constructor(
    private val xrayConfigBuilder: XrayConfigBuilder,
    private val xrayBridge: XrayNativeBridge,
) : EndpointProber {

    override suspend fun measureTcpHandshake(host: String, port: Int, timeoutMs: Int): Long =
        withContext(Dispatchers.IO) {
            if (host.isBlank() || port !in 1..65535) return@withContext -1L
            runCatching {
                var elapsed = -1L
                Socket().use { socket ->
                    elapsed = measureTimeMillis {
                        socket.connect(InetSocketAddress(host, port), timeoutMs)
                    }
                }
                elapsed.coerceAtLeast(1L)
            }.getOrDefault(-1L)
        }

    override suspend fun measureProfileDelay(
        profile: TunnelProfile,
        settings: AppSettings,
    ): NodeProbeResult = withContext(Dispatchers.IO) {
        val tcpMs = measureTcpHandshake(profile.serverAddress, profile.serverPort, 3000)
        val urlDelay = if (xrayBridge.isNativeAvailable && profile.resolvedEngine().name == "XRAY") {
            val resolvedProfile = TlsCertificatePinResolver.resolveProfileIfNeeded(profile)
            val dummyAuth = SessionLoopbackAuth(49100, 49101, 49102, "probe", "probe", "probe")
            val cfg = xrayConfigBuilder.buildConfig(resolvedProfile, settings, dummyAuth)
            xrayBridge.measureStandaloneConfigDelay(cfg, settings.latencyTestUrl)
        } else {
            tcpMs
        }
        NodeProbeResult(
            profileId = profile.id,
            tcpPingMs = tcpMs,
            httpDelayMs = urlDelay,
        )
    }

    override suspend fun measureActiveTunnelHttpDelay(
        testUrl: String,
        sessionAuth: SessionLoopbackAuth?,
    ): Long = withContext(Dispatchers.IO) {
        if (xrayBridge.isNativeAvailable) {
            val nativeDelay = xrayBridge.measureActiveDelay(testUrl)
            if (nativeDelay > 0L) return@withContext nativeDelay
        }
        runCatching {
            val client = buildLoopbackHttpClient(sessionAuth, timeoutSec = 5L)
            val req = Request.Builder().url(testUrl).get().build()
            var rtt = -1L
            val elapsed = measureTimeMillis {
                client.newCall(req).execute().use { resp ->
                    if (resp.code in 200..399) {
                        rtt = 1L
                    }
                }
            }
            if (rtt > 0L) elapsed.coerceAtLeast(1L) else -1L
        }.getOrDefault(-1L)
    }

    override suspend fun measureActiveDownloadSpeedMbps(
        speedUrl: String,
        sessionAuth: SessionLoopbackAuth?,
    ): Double = withContext(Dispatchers.IO) {
        runCatching {
            val client = buildLoopbackHttpClient(sessionAuth, timeoutSec = 15L)
            val req = Request.Builder().url(speedUrl).get().build()
            var totalBytes = 0L
            val elapsedMs = measureTimeMillis {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val stream = resp.body?.byteStream() ?: error("Empty body")
                    val buf = ByteArray(16 * 1024)
                    var read = stream.read(buf)
                    while (read > 0) {
                        totalBytes += read
                        read = stream.read(buf)
                    }
                }
            }.coerceAtLeast(1L)
            val bits = totalBytes * 8.0
            (bits / (elapsedMs / 1000.0)) / 1_000_000.0
        }.getOrDefault(0.0)
    }

    private fun buildLoopbackHttpClient(
        sessionAuth: SessionLoopbackAuth?,
        timeoutSec: Long,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(timeoutSec, TimeUnit.SECONDS)
            .readTimeout(timeoutSec, TimeUnit.SECONDS)
        if (sessionAuth != null) {
            java.net.Authenticator.setDefault(
                object : java.net.Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication? {
                        if (requestingHost == "127.0.0.1" &&
                            (requestingPort == sessionAuth.httpPort || requestingPort == sessionAuth.socksPort)
                        ) {
                            return PasswordAuthentication(
                                sessionAuth.username,
                                sessionAuth.password.toCharArray(),
                            )
                        }
                        return null
                    }
                }
            )
            builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", sessionAuth.httpPort)))
            builder.proxyAuthenticator(
                Authenticator { _, response ->
                    if (response.request.header("Proxy-Authorization") != null) return@Authenticator null
                    val credential = Credentials.basic(sessionAuth.username, sessionAuth.password)
                    response.request.newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build()
                }
            )
        }
        return builder.build()
    }
}

/**
 * Multi-threaded concurrent node ranker & zero-drop Smart Auto-Failover selector.
 */
@Singleton
class SmartNodeSelector @Inject constructor(
    private val prober: EndpointProber,
) {
    private val concurrencySemaphore = Semaphore(16)

    suspend fun probeAllConcurrently(
        profiles: List<TunnelProfile>,
        settings: AppSettings,
    ): List<TunnelProfile> = coroutineScope {
        val now = System.currentTimeMillis()
        profiles.map { profile ->
            async(Dispatchers.IO) {
                concurrencySemaphore.withPermit {
                    val result = prober.measureProfileDelay(profile, settings)
                    profile.copy(
                        tcpPingMs = result.tcpPingMs,
                        urlTestDelayMs = result.httpDelayMs,
                        lastTestedEpochMs = now,
                    )
                }
            }
        }.awaitAll().sortedWith(
            compareBy<TunnelProfile> {
                when {
                    it.urlTestDelayMs > 0L -> it.urlTestDelayMs
                    it.tcpPingMs > 0L -> it.tcpPingMs + 150L
                    else -> Long.MAX_VALUE
                }
            }.thenBy { it.name }
        )
    }

    suspend fun selectBestEndpoint(
        candidates: List<TunnelProfile>,
        settings: AppSettings,
        excludedIds: Set<String> = emptySet(),
    ): TunnelProfile? {
        val eligible = candidates.filter { it.id !in excludedIds }
        if (eligible.isEmpty()) return null

        // Use recent cached measurements if probed within the last 2 minutes; otherwise probe live
        val now = System.currentTimeMillis()
        val hasFreshMetrics = eligible.any {
            (it.urlTestDelayMs > 0L || it.tcpPingMs > 0L) && (now - it.lastTestedEpochMs) < 120_000L
        }
        val ranked = if (hasFreshMetrics) {
            eligible.sortedBy {
                when {
                    it.urlTestDelayMs > 0L -> it.urlTestDelayMs
                    it.tcpPingMs > 0L -> it.tcpPingMs + 150L
                    else -> Long.MAX_VALUE
                }
            }
        } else {
            probeAllConcurrently(eligible, settings)
        }

        return ranked.firstOrNull { it.urlTestDelayMs > 0L || it.tcpPingMs > 0L }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ProberBindingsModule {
    @Binds
    abstract fun bindEndpointProber(impl: DefaultEndpointProber): EndpointProber
}
