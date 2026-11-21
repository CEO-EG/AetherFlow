package com.ceoeg.aetherflow.core.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.IpPrefix
import android.net.ProxyInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import java.net.InetAddress
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.ceoeg.aetherflow.core.common.design.formatBitrateHuman
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger
import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.SplitTunnelMode
import com.ceoeg.aetherflow.core.common.model.VpnConnectionState
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Android 16 (SDK 36) compliant `VpnService` featuring:
 * - `FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED` foreground promotion (with `SPECIAL_USE` fallback)
 * - Customizable MTU (1280–9000), Strict Route, and Kill-Switch (`setBlocking(true)`)
 * - Per-App Split Tunneling (`Tunnel Selected Apps` vs `Bypass Selected Apps`) + self-loop protection
 * - Seamless Wi-Fi <-> Cellular roaming handoff & Always-On VPN auto-start support
 */
@AndroidEntryPoint
class AetherVpnService : VpnService() {

    @Inject lateinit var orchestrator: UnifiedEngineOrchestrator
    @Inject lateinit var settingsStore: EncryptedSettingsStore
    @Inject lateinit var handoffMonitor: NetworkHandoffMonitor
    @Inject lateinit var logger: DiagnosticLogger

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tunInterface: ParcelFileDescriptor? = null
    private var monitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_DISCONNECT) {
            serviceScope.launch {
                monitorJob?.cancel()
                orchestrator.stopSessionInternal()
                closeTunInterface()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        // Immediately promote to Foreground Service within Android 14–16's strict window
        promoteToForeground(
            title = "AetherFlow Tunnel",
            subtitle = "Initializing encrypted tunnel...",
        )

        val explicitProfileId = intent?.getStringExtra(EXTRA_PROFILE_ID)
        // Note: When launched by Android Always-On VPN, intent?.action may be "android.net.VpnService"
        serviceScope.launch(Dispatchers.IO) {
            val settings = settingsStore.current()
            val pfd = establishAndroidTunInterface(settings)
            if (pfd == null) {
                logger.error("VpnService", "VpnService.Builder.establish() returned null (VPN permission revoked)")
                orchestrator.stopSessionInternal()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }
            tunInterface = pfd
            val result = orchestrator.startSessionInternal(
                tunFd = pfd.fd,
                explicitProfileId = explicitProfileId,
            )
            if (result.isFailure) {
                closeTunInterface()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                startLiveNotificationAndHandoffObserver(pfd.fd)
            }
        }

        return START_STICKY
    }

    override fun onRevoke() {
        logger.warn("VpnService", "VPN permission revoked by Android OS")
        serviceScope.launch {
            monitorJob?.cancel()
            orchestrator.stopSessionInternal()
            closeTunInterface()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        super.onRevoke()
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        closeTunInterface()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun establishAndroidTunInterface(settings: AppSettings): ParcelFileDescriptor? {
        closeTunInterface()
        val tun = settings.tunSettings
        val lan = settings.lanTethering
        val mtu = tun.mtu.coerceIn(1280, 9000)

        val builder = Builder()
            .setSession("AetherFlow Enterprise Tunnel")
            .setMtu(mtu)
            .addAddress("26.26.26.1", 30)
            .addDnsServer("26.26.26.2")
            .setBlocking(tun.killSwitchBlocking)

        if (lan.enabled) {
            // Allow local Hotspot / USB Tethering / LAN subnets to reach the phone's 0.0.0.0 proxy
            // ports directly without being captured into tun0 (Neko-ray / v2rayNG LAN-bypass model).
            runCatching { builder.allowBypass() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                builder.addRoute("0.0.0.0", 0)
                LAN_EXCLUDED_IPV4_PREFIXES.forEach { (cidrIp, prefixLen) ->
                    runCatching {
                        builder.excludeRoute(IpPrefix(InetAddress.getByName(cidrIp), prefixLen))
                    }
                }
            } else {
                // API 24–32 fallback: add inverse public IPv4 routes so RFC1918/hotspot subnets bypass TUN
                PUBLIC_IPV4_ROUTES_BYPASSING_LAN.forEach { (cidrIp, prefixLen) ->
                    builder.addRoute(cidrIp, prefixLen)
                }
                // Keep virtual TUN DNS subnet (26.26.26.0/30) routed inside tun0
                builder.addRoute("26.26.26.0", 30)
            }
        } else {
            builder.addRoute("0.0.0.0", 0)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
            // Optional Neko-ray "Append HTTP Proxy to VPN" setting (requires open HTTP inbound)
            if (lan.enabled && lan.appendHttpProxyToVpn && !(lan.requireAuth && lan.password.isNotBlank())) {
                runCatching {
                    builder.setHttpProxy(ProxyInfo.buildDirectProxy("127.0.0.1", lan.httpPort))
                }
            }
        }

        if (tun.enableIpv6 && !settings.dnsSettings.blockIpv6Leak) {
            builder.addAddress("fc00::1", 126)
            builder.addRoute("::", 0)
            if (lan.enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    builder.excludeRoute(IpPrefix(InetAddress.getByName("fe80::"), 10))
                    builder.excludeRoute(IpPrefix(InetAddress.getByName("fc00::"), 7))
                }
            }
        }

        // Per-App Split Tunneling + mandatory self-exclusion to prevent proxy routing loops
        when (settings.splitTunnelMode) {
            SplitTunnelMode.TUNNEL_SELECTED_APPS -> {
                val allowed = settings.splitTunnelPackages - packageName
                if (allowed.isNotEmpty()) {
                    allowed.forEach { pkg ->
                        runCatching { builder.addAllowedApplication(pkg) }
                    }
                } else {
                    runCatching { builder.addDisallowedApplication(packageName) }
                }
            }
            SplitTunnelMode.BYPASS_SELECTED_APPS -> {
                (settings.splitTunnelPackages + packageName).forEach { pkg ->
                    runCatching { builder.addDisallowedApplication(pkg) }
                }
            }
            SplitTunnelMode.DISABLED -> {
                runCatching { builder.addDisallowedApplication(packageName) }
            }
        }

        return runCatching { builder.establish() }.getOrNull()
    }

    private fun startLiveNotificationAndHandoffObserver(tunFd: Int) {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            launch {
                // Drop the initial NetworkCallback snapshot for the already-active transport
                // so only genuine Wi-Fi <-> Cellular roaming transitions trigger a socket check.
                handoffMonitor.observePhysicalTransportChanges()
                    .drop(1)
                    .collect { transport ->
                        if (transport == PhysicalTransportType.NONE) return@collect
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            runCatching { setUnderlyingNetworks(null) }
                        }
                        orchestrator.onNetworkHandoffDetected(transport, tunFd)
                    }
            }
            launch {
                combine(
                    orchestrator.connectionState,
                    orchestrator.latestTraffic,
                ) { state, traffic -> state to traffic }.collect { (state, traffic) ->
                    when (state) {
                        is VpnConnectionState.Connected -> {
                            val subtitle = "↓ ${formatBitrateHuman(traffic.downlinkBps)}  •  ↑ ${formatBitrateHuman(traffic.uplinkBps)}"
                            val nm = getSystemService(NotificationManager::class.java)
                            nm?.notify(
                                NOTIFICATION_ID,
                                buildForegroundNotification(
                                    title = "${state.profileName} (${state.engine.displayName})",
                                    subtitle = subtitle,
                                )
                            )
                        }
                        is VpnConnectionState.Error,
                        is VpnConnectionState.Disconnected -> {
                            // Tear down the blocking tun0 interface immediately on terminal failure/disconnect
                            closeTunInterface()
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                            monitorJob?.cancel()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun promoteToForeground(title: String, subtitle: String) {
        val notification = buildForegroundNotification(title, subtitle)
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
        }.onFailure {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatching {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                    )
                }
            }
        }
    }

    private fun buildForegroundNotification(title: String, subtitle: String): Notification {
        val disconnectIntent = Intent(this, AetherVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPending = PendingIntent.getService(
            this,
            101,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentPending = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                100,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPending)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Disconnect",
                disconnectPending,
            )
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AetherFlow Active Tunnel",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Displays real-time VPN tunnel status and throughput metrics"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun closeTunInterface() {
        runCatching { tunInterface?.close() }
        tunInterface = null
    }

    companion object {
        const val ACTION_CONNECT = "com.ceoeg.aetherflow.action.CONNECT"
        const val ACTION_DISCONNECT = "com.ceoeg.aetherflow.action.DISCONNECT"
        const val EXTRA_PROFILE_ID = "extra_profile_id"
        private const val CHANNEL_ID = "aetherflow_vpn_telemetry"
        private const val NOTIFICATION_ID = 4401

        // Private / Hotspot / USB-Tethering / Link-Local subnets excluded on Android 13+ (API 33+)
        private val LAN_EXCLUDED_IPV4_PREFIXES = listOf(
            "10.0.0.0" to 8,
            "100.64.0.0" to 10,
            "127.0.0.0" to 8,
            "169.254.0.0" to 16,
            "172.16.0.0" to 12,
            "192.168.0.0" to 16,
            "224.0.0.0" to 4,
            "255.255.255.255" to 32,
        )

        // Android 7–12 (API 24–32) inverse public IPv4 routes excluding RFC1918 & hotspot subnets
        // (same CIDR split strategy used by v2rayNG / Neko-ray when Bypass LAN / LAN Share is active)
        private val PUBLIC_IPV4_ROUTES_BYPASSING_LAN = listOf(
            "0.0.0.0" to 5,
            "8.0.0.0" to 7,
            "11.0.0.0" to 8,
            "12.0.0.0" to 6,
            "16.0.0.0" to 4,
            "32.0.0.0" to 3,
            "64.0.0.0" to 3,
            "96.0.0.0" to 6,
            "100.0.0.0" to 10,
            "100.128.0.0" to 9,
            "101.0.0.0" to 8,
            "102.0.0.0" to 7,
            "104.0.0.0" to 5,
            "112.0.0.0" to 5,
            "120.0.0.0" to 6,
            "124.0.0.0" to 7,
            "126.0.0.0" to 8,
            "128.0.0.0" to 3,
            "160.0.0.0" to 5,
            "168.0.0.0" to 8,
            "169.0.0.0" to 9,
            "169.128.0.0" to 10,
            "169.192.0.0" to 11,
            "169.224.0.0" to 12,
            "169.240.0.0" to 13,
            "169.248.0.0" to 14,
            "169.252.0.0" to 15,
            "169.255.0.0" to 16,
            "170.0.0.0" to 7,
            "172.0.0.0" to 12,
            "172.32.0.0" to 11,
            "172.64.0.0" to 10,
            "172.128.0.0" to 9,
            "173.0.0.0" to 8,
            "174.0.0.0" to 7,
            "176.0.0.0" to 4,
            "192.0.0.0" to 9,
            "192.128.0.0" to 11,
            "192.160.0.0" to 13,
            "192.169.0.0" to 16,
            "192.170.0.0" to 15,
            "192.172.0.0" to 14,
            "192.176.0.0" to 12,
            "192.192.0.0" to 10,
            "193.0.0.0" to 8,
            "194.0.0.0" to 7,
            "196.0.0.0" to 6,
            "200.0.0.0" to 5,
            "208.0.0.0" to 4,
        )
    }
}
