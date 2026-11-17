package com.ceoeg.aetherflow.core.xray

import android.content.Context
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

interface XrayNativeBridge {
    val isNativeAvailable: Boolean
    fun initEnv(assetDir: String)
    fun startLoop(configJson: String, tunFd: Int)
    fun stopLoop()
    fun measureActiveDelay(url: String): Long
    fun measureStandaloneConfigDelay(configJson: String, url: String): Long
    fun queryOutboundTrafficDelta(): Pair<Long, Long>
    fun coreVersion(): String
}

/**
 * Reflection / JNI adapter over `2dust/AndroidLibXrayLite` (`libv2ray.Libv2ray` and
 * `libv2ray.CoreController`). Degrades cleanly when running in JVM unit tests.
 */
@Singleton
class GomobileLibXrayBridge @Inject constructor(
    private val logger: DiagnosticLogger,
) : XrayNativeBridge {

    private var controllerInstance: Any? = null
    private val running = AtomicBoolean(false)

    private val libv2rayClass: Class<*>? by lazy {
        runCatching { Class.forName("libv2ray.Libv2ray") }.getOrNull()
    }

    override val isNativeAvailable: Boolean
        get() = libv2rayClass != null

    override fun initEnv(assetDir: String) {
        val cls = libv2rayClass ?: return
        runCatching {
            cls.getDeclaredMethod("initCoreEnv", String::class.java, String::class.java)
                .invoke(null, assetDir, "")
        }
    }

    override fun startLoop(configJson: String, tunFd: Int) {
        stopLoop()
        val cls = libv2rayClass
        if (cls != null) {
            val callbackIface = Class.forName("libv2ray.CoreCallbackHandler")
            val callbackProxy = Proxy.newProxyInstance(
                callbackIface.classLoader,
                arrayOf(callbackIface),
            ) { proxy, method, args ->
                when (method.name) {
                    "equals" -> proxy === args?.firstOrNull()
                    "hashCode" -> System.identityHashCode(proxy)
                    "toString" -> "AetherXrayCallbackHandler"
                    "onEmitStatus" -> {
                        val info = args?.getOrNull(1)?.toString().orEmpty()
                        if (info.isNotBlank()) logger.info("XrayCore", info)
                        0L
                    }
                    else -> when (method.returnType) {
                        Boolean::class.javaPrimitiveType -> false
                        Int::class.javaPrimitiveType -> 0
                        Long::class.javaPrimitiveType -> 0L
                        else -> null
                    }
                }
            }
            try {
                val controller = cls.getDeclaredMethod("newCoreController", callbackIface)
                    .invoke(null, callbackProxy)
                controller.javaClass
                    .getDeclaredMethod("startLoop", String::class.java, Int::class.javaPrimitiveType)
                    .invoke(controller, configJson, tunFd)
                controllerInstance = controller
            } catch (e: InvocationTargetException) {
                throw (e.targetException ?: e)
            }
        } else if (tunFd > 0) {
            error("Native Xray-core library (libv2ray.aar) is missing. Run 'make fetch-cores' and rebuild.")
        }
        running.set(true)
        logger.info("XrayCore", "Xray-core loop started (tunFd=$tunFd)")
    }

    override fun stopLoop() {
        if (!running.getAndSet(false)) return
        controllerInstance?.let { ctrl ->
            runCatching { ctrl.javaClass.getDeclaredMethod("stopLoop").invoke(ctrl) }
        }
        controllerInstance = null
        logger.info("XrayCore", "Xray-core loop stopped")
    }

    override fun measureActiveDelay(url: String): Long {
        val ctrl = controllerInstance ?: return -1L
        return runCatching {
            (ctrl.javaClass.getDeclaredMethod("measureDelay", String::class.java)
                .invoke(ctrl, url) as? Number)?.toLong() ?: -1L
        }.getOrDefault(-1L)
    }

    override fun measureStandaloneConfigDelay(configJson: String, url: String): Long {
        val cls = libv2rayClass ?: return -1L
        return runCatching {
            (cls.getDeclaredMethod("measureOutboundDelay", String::class.java, String::class.java)
                .invoke(null, configJson, url) as? Number)?.toLong() ?: -1L
        }.getOrDefault(-1L)
    }

    override fun queryOutboundTrafficDelta(): Pair<Long, Long> {
        val ctrl = controllerInstance ?: return 0L to 0L
        return runCatching {
            val raw = ctrl.javaClass.getDeclaredMethod("queryAllOutboundTrafficStats")
                .invoke(ctrl) as? String ?: return 0L to 0L
            // Format: tag,uplink,1234;tag,downlink,5678;
            var up = 0L
            var down = 0L
            for (token in raw.split(';')) {
                val parts = token.split(',')
                if (parts.size == 3 && parts[0] == "proxy") {
                    val v = parts[2].toLongOrNull() ?: 0L
                    if (parts[1] == "uplink") up += v
                    if (parts[1] == "downlink") down += v
                }
            }
            up to down
        }.getOrDefault(0L to 0L)
    }

    override fun coreVersion(): String {
        val cls = libv2rayClass ?: return "Xray-core v26.11 (Managed Bridge)"
        return runCatching {
            cls.getDeclaredMethod("checkVersionX").invoke(null)?.toString()
                ?: "Xray-core v26.11"
        }.getOrDefault("Xray-core v26.11")
    }
}

@Singleton
class XrayEngineController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bridge: XrayNativeBridge,
) {
    val isNativeAvailable: Boolean
        get() = bridge.isNativeAvailable

    fun start(configJson: String, tunFd: Int = 0) {
        val geoDir = runCatching { File(context.filesDir, "geo").apply { mkdirs() }.absolutePath }
            .getOrDefault("/tmp")
        bridge.initEnv(geoDir)
        bridge.startLoop(configJson, tunFd)
    }

    fun stop() = bridge.stopLoop()

    fun measureActiveDelay(url: String): Long = bridge.measureActiveDelay(url)

    fun pollTrafficDelta(): Pair<Long, Long> = bridge.queryOutboundTrafficDelta()

    fun version(): String = bridge.coreVersion()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class XrayBindingsModule {
    @Binds
    abstract fun bindXrayBridge(impl: GomobileLibXrayBridge): XrayNativeBridge
}
