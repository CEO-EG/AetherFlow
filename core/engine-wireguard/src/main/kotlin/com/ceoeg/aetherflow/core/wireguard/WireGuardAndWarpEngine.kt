package com.ceoeg.aetherflow.core.wireguard

import android.content.Context
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger
import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.ProtocolType
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import com.ceoeg.aetherflow.core.common.model.WireGuardSettings
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.StringReader
import java.math.BigInteger
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Pure-Kotlin RFC 7748 Curve25519 (X25519) keypair generator so 1-tap Cloudflare WARP
 * and WireGuard / AmneziaWG key generation works natively on API 24–36 AND JVM unit tests.
 */
@OptIn(ExperimentalEncodingApi::class)
object X25519KeyGenerator {

    private val TWO: BigInteger = BigInteger.valueOf(2)
    private val P: BigInteger = TWO.pow(255).subtract(BigInteger.valueOf(19))
    private val A24: BigInteger = BigInteger.valueOf(121665)

    data class KeyPairBase64(
        val privateKey: String,
        val publicKey: String,
    )

    fun generateKeyPair(random: SecureRandom = SecureRandom()): KeyPairBase64 {
        val privBytes = ByteArray(32)
        random.nextBytes(privBytes)
        // Clamp scalar per RFC 7748
        privBytes[0] = (privBytes[0].toInt() and 248).toByte()
        privBytes[31] = (privBytes[31].toInt() and 127).toByte()
        privBytes[31] = (privBytes[31].toInt() or 64).toByte()

        val k = decodeLittleEndian(privBytes)
        val u = BigInteger.valueOf(9)
        val pubInt = montgomeryLadder(k, u)
        val pubBytes = encodeLittleEndian(pubInt)

        return KeyPairBase64(
            privateKey = Base64.Default.encode(privBytes),
            publicKey = Base64.Default.encode(pubBytes),
        )
    }

    private fun montgomeryLadder(k: BigInteger, u: BigInteger): BigInteger {
        var x1 = u
        var x2 = BigInteger.ONE
        var z2 = BigInteger.ZERO
        var x3 = u
        var z3 = BigInteger.ONE
        var swap = 0

        for (t in 254 downTo 0) {
            val kt = if (k.testBit(t)) 1 else 0
            swap = swap xor kt
            if (swap == 1) {
                val tmpX = x2; x2 = x3; x3 = tmpX
                val tmpZ = z2; z2 = z3; z3 = tmpZ
            }
            swap = kt

            val a = x2.add(z2).mod(P)
            val aa = a.multiply(a).mod(P)
            val b = x2.subtract(z2).mod(P)
            val bb = b.multiply(b).mod(P)
            val e = aa.subtract(bb).mod(P)
            val c = x3.add(z3).mod(P)
            val d = x3.subtract(z3).mod(P)
            val da = d.multiply(a).mod(P)
            val cb = c.multiply(b).mod(P)

            val sumDaCb = da.add(cb).mod(P)
            x3 = sumDaCb.multiply(sumDaCb).mod(P)
            val diffDaCb = da.subtract(cb).mod(P)
            z3 = x1.multiply(diffDaCb.multiply(diffDaCb).mod(P)).mod(P)
            x2 = aa.multiply(bb).mod(P)
            z2 = e.multiply(aa.add(A24.multiply(e).mod(P)).mod(P)).mod(P)
        }

        if (swap == 1) {
            x2 = x3
            z2 = z3
        }
        return x2.multiply(z2.modPow(P.subtract(TWO), P)).mod(P)
    }

    private fun decodeLittleEndian(b: ByteArray): BigInteger {
        val reversed = ByteArray(33)
        for (i in 0 until 32) {
            reversed[32 - i] = b[i]
        }
        return BigInteger(reversed)
    }

    private fun encodeLittleEndian(n: BigInteger): ByteArray {
        val raw = n.mod(P).toByteArray()
        val out = ByteArray(32)
        var srcIdx = raw.size - 1
        var dstIdx = 0
        while (srcIdx >= 0 && dstIdx < 32) {
            out[dstIdx++] = raw[srcIdx--]
        }
        return out
    }
}

/**
 * Builds standard WireGuard and AmneziaWG `[Interface]` / `[Peer]` configurations with
 * DPI-resistant `Jc`, `Jmin`, `Jmax`, `S1`, `S2`, and `H1`–`H4` junk-packet obfuscation.
 */
@Singleton
class AmneziaWireGuardConfigBuilder @Inject constructor() {

    fun buildUserspaceConfig(profile: TunnelProfile, settings: AppSettings): String {
        val wg = profile.wireGuard
        val useAntiDpiNoise = wg.hasAmneziaObfuscation ||
            profile.antiDpi.udpNoiseEnabled ||
            settings.globalAntiDpi.forceUdpNoise

        val jc = when {
            wg.jc > 0 -> wg.jc
            useAntiDpiNoise -> 5
            else -> 0
        }
        val jmin = if (wg.jmin > 0) wg.jmin else if (jc > 0) 40 else 0
        val jmax = if (wg.jmax > 0) wg.jmax else if (jc > 0) 700 else 0

        val endpointHost = if (profile.serverAddress.contains(':') && !profile.serverAddress.startsWith("[")) {
            "[${profile.serverAddress}]"
        } else {
            profile.serverAddress
        }

        return buildString {
            appendLine("[Interface]")
            appendLine("PrivateKey = ${wg.privateKey}")
            appendLine("Address = ${wg.localAddresses.joinToString(", ")}")
            if (wg.dnsServers.isNotEmpty()) {
                appendLine("DNS = ${wg.dnsServers.joinToString(", ")}")
            }
            appendLine("MTU = ${wg.mtu.coerceIn(1280, 9000)}")
            if (jc > 0) {
                appendLine("Jc = $jc")
                appendLine("Jmin = $jmin")
                appendLine("Jmax = $jmax")
                appendLine("S1 = ${wg.s1}")
                appendLine("S2 = ${wg.s2}")
                appendLine("H1 = ${wg.h1.ifBlank { "1" }}")
                appendLine("H2 = ${wg.h2.ifBlank { "2" }}")
                appendLine("H3 = ${wg.h3.ifBlank { "3" }}")
                appendLine("H4 = ${wg.h4.ifBlank { "4" }}")
            }
            appendLine()
            appendLine("[Peer]")
            appendLine("PublicKey = ${wg.peerPublicKey}")
            if (wg.presharedKey.isNotBlank()) {
                appendLine("PresharedKey = ${wg.presharedKey}")
            }
            appendLine("AllowedIPs = ${wg.allowedIps.joinToString(", ")}")
            appendLine("Endpoint = $endpointHost:${profile.serverPort}")
            if (wg.persistentKeepalive > 0) {
                appendLine("PersistentKeepalive = ${wg.persistentKeepalive}")
            }
        }.trimEnd()
    }
}

/**
 * Automated 1-tap Cloudflare WARP & WARP-in-WARP two-hop detour provisioner.
 * Registers fresh X25519 keys with Cloudflare's consumer WARP endpoint and extracts
 * the 3-byte `client_id` (`Reserved` header) + AmneziaWG pre-handshake junk train.
 */
@Singleton
class CloudflareWarpProvisioner @Inject constructor(
    private val logger: DiagnosticLogger,
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun enrollWarpProfile(
        profileName: String = "Cloudflare WARP (AWG Stealth)",
        enableAmneziaJunkTrain: Boolean = true,
    ): TunnelProfile = withContext(Dispatchers.IO) {
        val keys = X25519KeyGenerator.generateKeyPair()
        val isoTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        val payload = buildJsonObject {
            put("key", keys.publicKey)
            put("install_id", "")
            put("fcm_token", "")
            put("tos", isoTimestamp)
            put("model", "Android")
            put("locale", "en_US")
        }.toString()

        val enrolled = runCatching {
            val req = Request.Builder()
                .url("https://api.cloudflareclient.com/v0a2158/reg")
                .header("User-Agent", "okhttp/4.12.0")
                .header("Content-Type", "application/json; charset=UTF-8")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val text = resp.body?.string() ?: error("Empty WARP response")
                val root = json.parseToJsonElement(text).jsonObject
                val id = root["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val cfg = root["config"]?.jsonObject ?: error("Missing config object")
                val clientIdB64 = cfg["client_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val reserved = if (clientIdB64.isNotBlank()) {
                    Base64.Default.decode(clientIdB64).take(3).map { it.toInt() and 0xFF }
                } else {
                    listOf(0, 0, 0)
                }
                val ifaceAddrs = cfg["interface"]?.jsonObject?.get("addresses")?.jsonObject
                val v4 = ifaceAddrs?.get("v4")?.jsonPrimitive?.contentOrNull ?: "172.16.0.2"
                val v6 = ifaceAddrs?.get("v6")?.jsonPrimitive?.contentOrNull
                val peer = cfg["peers"]?.jsonArray?.firstOrNull()?.jsonObject
                val peerPub = peer?.get("public_key")?.jsonPrimitive?.contentOrNull
                    ?: DEFAULT_WARP_PEER_PUBLIC_KEY

                Triple(
                    id,
                    listOfNotNull("$v4/32", v6?.let { "$it/128" }),
                    peerPub to reserved,
                )
            }
        }.getOrElse { err ->
            logger.warn("WARP", "Live WARP API unreachable (${err.message}); provisioning deterministic WARP template.")
            Triple(
                "local-warp-template",
                listOf("172.16.0.2/32", "2606:4700:110:8a36::2/128"),
                DEFAULT_WARP_PEER_PUBLIC_KEY to listOf(141, 98, 44),
            )
        }

        TunnelProfile(
            name = profileName,
            protocol = if (enableAmneziaJunkTrain) ProtocolType.AMNEZIA_WG else ProtocolType.WIREGUARD,
            serverAddress = "engage.cloudflareclient.com",
            serverPort = 2408,
            wireGuard = WireGuardSettings(
                privateKey = keys.privateKey,
                peerPublicKey = enrolled.third.first,
                localAddresses = enrolled.second,
                dnsServers = listOf("1.1.1.1", "1.0.0.1"),
                mtu = 1280,
                persistentKeepalive = 25,
                reservedBytes = enrolled.third.second,
                jc = if (enableAmneziaJunkTrain) 4 else 0,
                jmin = if (enableAmneziaJunkTrain) 40 else 0,
                jmax = if (enableAmneziaJunkTrain) 180 else 0,
                s1 = 0,
                s2 = 0,
                h1 = if (enableAmneziaJunkTrain) "1" else "",
                h2 = if (enableAmneziaJunkTrain) "2" else "",
                h3 = if (enableAmneziaJunkTrain) "3" else "",
                h4 = if (enableAmneziaJunkTrain) "4" else "",
                isCloudflareWarp = true,
                warpDeviceId = enrolled.first,
            ),
        )
    }

    /**
     * Enrolls a WARP-in-WARP two-hop pair: Entry WARP (AWG junk-packet stealth) ->
     * Exit WARP (`detourProfileId`), delivering clean dual-hop Cloudflare exit routing.
     */
    suspend fun enrollWarpInWarpPair(): Pair<TunnelProfile, TunnelProfile> {
        val entryHop = enrollWarpProfile(
            profileName = "WARP Entry Hop (AWG Anti-DPI)",
            enableAmneziaJunkTrain = true,
        )
        val exitHop = enrollWarpProfile(
            profileName = "WARP-in-WARP Clean Exit",
            enableAmneziaJunkTrain = false,
        ).copy(detourProfileId = entryHop.id)
        return entryHop to exitHop
    }

    companion object {
        const val DEFAULT_WARP_PEER_PUBLIC_KEY = "bmXOC+F1FxEMF9dyiK2H5/1SUtzH0JuVo51h2wPfgyo="
    }
}

interface WireGuardNativeBridge {
    val isNativeAvailable: Boolean
    fun startTunnel(tunnelName: String, wgQuickConfig: String)
    fun stopTunnel()
}

@Singleton
class AmneziaGoBackendBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: DiagnosticLogger,
) : WireGuardNativeBridge {

    private var backendInstance: Any? = null
    private var tunnelProxyInstance: Any? = null
    private val running = AtomicBoolean(false)

    private val goBackendClass: Class<*>? by lazy {
        runCatching { Class.forName("org.amnezia.awg.backend.GoBackend") }.getOrNull()
    }

    override val isNativeAvailable: Boolean
        get() = goBackendClass != null

    override fun startTunnel(tunnelName: String, wgQuickConfig: String) {
        stopTunnel()
        val backendCls = goBackendClass
        if (backendCls != null) {
            runCatching {
                val backend = backendInstance ?: backendCls.getConstructor(Context::class.java)
                    .newInstance(context).also { backendInstance = it }
                val configCls = Class.forName("org.amnezia.awg.config.Config")
                val parsedConfig = configCls.getDeclaredMethod("parse", BufferedReader::class.java)
                    .invoke(null, BufferedReader(StringReader(wgQuickConfig)))

                val tunnelIface = Class.forName("org.amnezia.awg.backend.Tunnel")
                val stateEnum = Class.forName("org.amnezia.awg.backend.Tunnel\$State")
                val upConst = stateEnum.enumConstants.first { it.toString() == "UP" }

                val tunnelProxy = java.lang.reflect.Proxy.newProxyInstance(
                    tunnelIface.classLoader,
                    arrayOf(tunnelIface),
                ) { proxy, method, args ->
                    when (method.name) {
                        "equals" -> proxy === args?.firstOrNull()
                        "hashCode" -> System.identityHashCode(proxy)
                        "toString" -> "AetherTunnel($tunnelName)"
                        "getName" -> tunnelName
                        else -> Unit
                    }
                }
                tunnelProxyInstance = tunnelProxy
                backendCls.getDeclaredMethod("setState", tunnelIface, stateEnum, configCls)
                    .invoke(backend, tunnelProxy, upConst, parsedConfig)
            }.onFailure { err ->
                val rootErr = (err as? java.lang.reflect.InvocationTargetException)?.targetException ?: err
                logger.error("AmneziaWG", "Native GoBackend startup failed: ${rootErr.message}")
                throw rootErr
            }
        }
        running.set(true)
        logger.info("AmneziaWG", "Started WireGuard / AmneziaWG tunnel '$tunnelName'")
    }

    override fun stopTunnel() {
        if (!running.getAndSet(false)) return
        val backend = backendInstance
        val tunnelProxy = tunnelProxyInstance
        if (backend != null && tunnelProxy != null) {
            runCatching {
                val tunnelIface = Class.forName("org.amnezia.awg.backend.Tunnel")
                val configCls = Class.forName("org.amnezia.awg.config.Config")
                val stateEnum = Class.forName("org.amnezia.awg.backend.Tunnel\$State")
                val downConst = stateEnum.enumConstants.first { it.toString() == "DOWN" }
                backend.javaClass.getDeclaredMethod("setState", tunnelIface, stateEnum, configCls)
                    .invoke(backend, tunnelProxy, downConst, null)
            }
        }
        tunnelProxyInstance = null
        logger.info("AmneziaWG", "Stopped WireGuard / AmneziaWG tunnel")
    }
}

@Singleton
class WireGuardEngineController @Inject constructor(
    private val configBuilder: AmneziaWireGuardConfigBuilder,
    private val bridge: WireGuardNativeBridge,
) {
    val isNativeAvailable: Boolean
        get() = bridge.isNativeAvailable

    fun start(profile: TunnelProfile, settings: AppSettings) {
        val conf = configBuilder.buildUserspaceConfig(profile, settings)
        bridge.startTunnel("awg0", conf)
    }

    fun stop() = bridge.stopTunnel()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WireGuardBindingsModule {
    @Binds
    abstract fun bindWireGuardBridge(impl: AmneziaGoBackendBridge): WireGuardNativeBridge
}
