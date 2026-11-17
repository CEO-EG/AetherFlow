package com.ceoeg.aetherflow.core.tun

import android.content.Context
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger
import com.ceoeg.aetherflow.core.common.model.SessionLoopbackAuth
import com.ceoeg.aetherflow.core.common.model.TunBackendMode
import com.ceoeg.aetherflow.core.common.model.TunSettings
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clean abstraction over JNI / native TUN packet forwarders (`hev-socks5-tunnel` and
 * `badvpn-tun2socks`) so JVM unit tests run deterministically without a live kernel `/dev/tun`.
 */
interface HevTunNativeBridge {
    val isNativeLibraryAvailable: Boolean
    fun startService(configFilePath: String, tunFd: Int)
    fun stopService()
    fun getStats(): LongArray
}

interface BadVpnNativeBridge {
    val isBinaryAvailable: Boolean
    fun startTun2Socks(
        tunFd: Int,
        mtu: Int,
        socksPort: Int,
        socksUser: String,
        socksPass: String,
        enableIpv6: Boolean,
    )
    fun stopTun2Socks()
}

private interface HevJniDispatcher {
    fun start(configPath: String, fd: Int): Boolean
    fun stop(): Boolean
    fun stats(): LongArray?
}

@Singleton
class JniHevTunNativeBridge @Inject constructor(
    private val logger: DiagnosticLogger,
) : HevTunNativeBridge {

    private val running = AtomicBoolean(false)

    private val dispatcher: HevJniDispatcher? by lazy {
        runCatching {
            // Ensure both candidate JNI registration classes are loaded before JNI_OnLoad runs
            Class.forName("com.v2ray.ang.service.TProxyService")
            Class.forName("hev.htproxy.TProxyService")
            System.loadLibrary("hev-socks5-tunnel")

            val upstreamProbe = runCatching {
                com.v2ray.ang.service.TProxyService.TProxyIsRunning()
                object : HevJniDispatcher {
                    override fun start(configPath: String, fd: Int): Boolean =
                        com.v2ray.ang.service.TProxyService.TProxyStartService(configPath, fd)
                    override fun stop(): Boolean =
                        com.v2ray.ang.service.TProxyService.TProxyStopService()
                    override fun stats(): LongArray? =
                        com.v2ray.ang.service.TProxyService.TProxyGetStats()
                }
            }.getOrNull()

            upstreamProbe ?: runCatching {
                hev.htproxy.TProxyService.TProxyIsRunning()
                object : HevJniDispatcher {
                    override fun start(configPath: String, fd: Int): Boolean =
                        hev.htproxy.TProxyService.TProxyStartService(configPath, fd)
                    override fun stop(): Boolean =
                        hev.htproxy.TProxyService.TProxyStopService()
                    override fun stats(): LongArray? =
                        hev.htproxy.TProxyService.TProxyGetStats()
                }
            }.getOrNull()
        }.onFailure {
            logger.warn("HevTUN", "Native libhev-socks5-tunnel.so not loaded: ${it.message}")
        }.getOrNull()
    }

    override val isNativeLibraryAvailable: Boolean
        get() = dispatcher != null

    override fun startService(configFilePath: String, tunFd: Int) {
        val jni = dispatcher
        if (jni != null) {
            val started = jni.start(configFilePath, tunFd)
            if (!started) {
                error("hev-socks5-tunnel failed to start on fd=$tunFd")
            }
        } else if (tunFd > 0) {
            error("Native libhev-socks5-tunnel.so is missing. Run 'make fetch-cores' and rebuild.")
        }
        running.set(true)
        logger.info("HevTUN", "Started hev-socks5-tunnel on fd=$tunFd using $configFilePath")
    }

    override fun stopService() {
        if (!running.getAndSet(false)) return
        dispatcher?.let { jni ->
            runCatching { jni.stop() }
        }
        logger.info("HevTUN", "Stopped hev-socks5-tunnel")
    }

    override fun getStats(): LongArray {
        if (!running.get()) return longArrayOf(0L, 0L, 0L, 0L)
        val jni = dispatcher ?: return longArrayOf(0L, 0L, 0L, 0L)
        return runCatching {
            jni.stats() ?: longArrayOf(0L, 0L, 0L, 0L)
        }.getOrDefault(longArrayOf(0L, 0L, 0L, 0L))
    }
}

@Singleton
class ProcessBadVpnNativeBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: DiagnosticLogger,
) : BadVpnNativeBridge {

    private var process: Process? = null

    private val binaryFile: File
        get() = File(context.applicationInfo.nativeLibraryDir, "libtun2socks.so")

    override val isBinaryAvailable: Boolean
        get() = runCatching { binaryFile.exists() }.getOrDefault(false)

    override fun startTun2Socks(
        tunFd: Int,
        mtu: Int,
        socksPort: Int,
        socksUser: String,
        socksPass: String,
        enableIpv6: Boolean,
    ) {
        stopTun2Socks()
        if (!isBinaryAvailable) {
            error("badvpn-tun2socks binary not bundled; switch TUN Backend to hev-socks5-tunnel in Config.")
        }
        val cmd = buildList {
            add(binaryFile.absolutePath)
            add("--netif-ipaddr")
            add("26.26.26.2")
            add("--netif-netmask")
            add("255.255.255.252")
            add("--socks-server-addr")
            add("127.0.0.1:$socksPort")
            add("--tunmtu")
            add(mtu.toString())
            add("--username")
            add(socksUser)
            add("--password")
            add(socksPass)
            if (enableIpv6) {
                add("--netif-ip6addr")
                add("fc00::2")
            }
            add("--loglevel")
            add("warning")
        }
        process = ProcessBuilder(cmd)
            .directory(context.filesDir)
            .redirectErrorStream(true)
            .start()
        logger.info("BadVPN", "Started badvpn-tun2socks fallback (MTU=$mtu, fd=$tunFd)")
    }

    override fun stopTun2Socks() {
        process?.let { proc ->
            runCatching {
                proc.destroy()
                proc.waitFor()
            }
        }
        process = null
    }
}

/**
 * Dual-backend TUN switcher (`hev-socks5-tunnel` primary vs `badvpn-tun2socks` fallback vs
 * Native Core TUN vs Proxy-Only mode).
 */
@Singleton
class TunEngineController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hevConfigBuilder: HevTunConfigBuilder,
    private val hevBridge: HevTunNativeBridge,
    private val badVpnBridge: BadVpnNativeBridge,
    private val logger: DiagnosticLogger,
) {
    private var activeBackend: TunBackendMode? = null

    val isHevAvailable: Boolean
        get() = hevBridge.isNativeLibraryAvailable

    fun startTun(
        tunFd: Int,
        tunSettings: TunSettings,
        sessionAuth: SessionLoopbackAuth,
    ) {
        stopTun()
        when (tunSettings.backend) {
            TunBackendMode.HEV_SOCKS5_TUNNEL -> {
                val yaml = hevConfigBuilder.buildYaml(tunSettings, sessionAuth)
                val configDir = runCatching { context.filesDir ?: context.cacheDir }.getOrNull()
                    ?: File(System.getProperty("java.io.tmpdir", "/tmp"))
                val configFile = File(configDir, "hev-socks5-tunnel.yml")
                configFile.writeText(yaml)
                hevBridge.startService(configFile.absolutePath, tunFd)
                activeBackend = TunBackendMode.HEV_SOCKS5_TUNNEL
            }
            TunBackendMode.BADVPN_TUN2SOCKS -> {
                if (!badVpnBridge.isBinaryAvailable && hevBridge.isNativeLibraryAvailable) {
                    logger.warn("TunEngine", "libtun2socks.so unavailable; auto-falling back to hev-socks5-tunnel")
                    val yaml = hevConfigBuilder.buildYaml(tunSettings, sessionAuth)
                    val configDir = runCatching { context.filesDir ?: context.cacheDir }.getOrNull()
                        ?: File(System.getProperty("java.io.tmpdir", "/tmp"))
                    val configFile = File(configDir, "hev-socks5-tunnel.yml")
                    configFile.writeText(yaml)
                    hevBridge.startService(configFile.absolutePath, tunFd)
                    activeBackend = TunBackendMode.HEV_SOCKS5_TUNNEL
                } else {
                    badVpnBridge.startTun2Socks(
                        tunFd = tunFd,
                        mtu = tunSettings.mtu.coerceIn(1280, 9000),
                        socksPort = sessionAuth.socksPort,
                        socksUser = sessionAuth.username,
                        socksPass = sessionAuth.password,
                        enableIpv6 = tunSettings.enableIpv6,
                    )
                    activeBackend = TunBackendMode.BADVPN_TUN2SOCKS
                }
            }
            TunBackendMode.CORE_NATIVE_TUN -> {
                logger.info("TunEngine", "Native Core TUN active: tunFd=$tunFd delegated directly to proxy core.")
                activeBackend = TunBackendMode.CORE_NATIVE_TUN
            }
            TunBackendMode.PROXY_ONLY -> {
                logger.info("TunEngine", "Proxy-Only mode active on 127.0.0.1:${sessionAuth.socksPort} (no TUN fd).")
                activeBackend = TunBackendMode.PROXY_ONLY
            }
        }
    }

    fun stopTun() {
        when (activeBackend) {
            TunBackendMode.HEV_SOCKS5_TUNNEL -> hevBridge.stopService()
            TunBackendMode.BADVPN_TUN2SOCKS -> badVpnBridge.stopTun2Socks()
            else -> Unit
        }
        activeBackend = null
    }

    fun readTunBytesStats(): Pair<Long, Long> {
        if (activeBackend == TunBackendMode.HEV_SOCKS5_TUNNEL) {
            val stats = hevBridge.getStats()
            // [txPackets, txBytes, rxPackets, rxBytes]
            if (stats.size >= 4) return stats[1] to stats[3]
        }
        return 0L to 0L
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TunBindingsModule {
    @Binds
    abstract fun bindHevTunBridge(impl: JniHevTunNativeBridge): HevTunNativeBridge

    @Binds
    abstract fun bindBadVpnBridge(impl: ProcessBadVpnNativeBridge): BadVpnNativeBridge
}
