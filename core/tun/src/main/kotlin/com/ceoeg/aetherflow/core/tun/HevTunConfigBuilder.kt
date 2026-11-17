package com.ceoeg.aetherflow.core.tun

import com.ceoeg.aetherflow.core.common.model.SessionLoopbackAuth
import com.ceoeg.aetherflow.core.common.model.TunSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates high-throughput `hev-socks5-tunnel` YAML configurations synchronized with
 * the per-session ephemeral loopback SOCKS5 port and credentials.
 */
@Singleton
class HevTunConfigBuilder @Inject constructor() {

    fun buildYaml(
        tunSettings: TunSettings,
        sessionAuth: SessionLoopbackAuth,
    ): String {
        val clampedMtu = tunSettings.mtu.coerceIn(1280, 9000)
        val udpMode = if (tunSettings.hevUdpInTcp) "tcp" else "udp"
        val tcpBuf = tunSettings.hevTcpBufferSize.coerceIn(4096, 262144)
        val stackSize = tunSettings.hevTaskStackSize.coerceAtLeast(20480 + tcpBuf)
        val escapedUser = sessionAuth.username.replace("'", "''")
        val escapedPass = sessionAuth.password.replace("'", "''")

        return buildString {
            appendLine("tunnel:")
            appendLine("  name: tun0")
            appendLine("  mtu: $clampedMtu")
            appendLine("  multi-queue: ${tunSettings.hevMultiQueue}")
            appendLine("  ipv4: 26.26.26.1")
            if (tunSettings.enableIpv6) {
                appendLine("  ipv6: 'fc00::1'")
            }
            appendLine()
            appendLine("socks5:")
            appendLine("  address: 127.0.0.1")
            appendLine("  port: ${sessionAuth.socksPort}")
            appendLine("  udp: '$udpMode'")
            appendLine("  username: '$escapedUser'")
            appendLine("  password: '$escapedPass'")
            appendLine()
            if (tunSettings.hevMapDnsEnabled) {
                appendLine("mapdns:")
                appendLine("  address: 26.26.26.2")
                appendLine("  port: 53")
                appendLine("  network: 240.0.0.0")
                appendLine("  netmask: 240.0.0.0")
                appendLine("  cache-size: 10000")
                appendLine()
            }
            appendLine("misc:")
            appendLine("  task-stack-size: $stackSize")
            appendLine("  tcp-buffer-size: $tcpBuf")
            appendLine("  connect-timeout: 10000")
            appendLine("  tcp-read-write-timeout: 300000")
            appendLine("  udp-read-write-timeout: 60000")
            appendLine("  log-file: stderr")
            appendLine("  log-level: warn")
        }
    }
}
