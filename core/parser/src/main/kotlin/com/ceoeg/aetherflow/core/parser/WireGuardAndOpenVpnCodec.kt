package com.ceoeg.aetherflow.core.parser

import com.ceoeg.aetherflow.core.common.model.OpenVpnSettings
import com.ceoeg.aetherflow.core.common.model.ProtocolType
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import com.ceoeg.aetherflow.core.common.model.WireGuardSettings

/**
 * Bidirectional parser & serializer for WireGuard / AmneziaWG `.conf` INI files
 * (including Jc, Jmin, Jmax, S1, S2, H1-H4 DPI obfuscation headers) and OpenVPN `.ovpn` files.
 */
object WireGuardAndOpenVpnCodec {

    fun parseWireGuardConf(
        confContent: String,
        profileName: String = "WireGuard Tunnel",
        subscriptionId: String? = null,
    ): TunnelProfile? {
        if (!confContent.contains("[Interface]", ignoreCase = true) ||
            !confContent.contains("[Peer]", ignoreCase = true)
        ) {
            return null
        }

        var currentSection = ""
        var privateKey = ""
        var addresses = listOf("172.16.0.2/32")
        var dns = listOf("1.1.1.1")
        var mtu = 1400
        var reserved = emptyList<Int>()
        var jc = 0
        var jmin = 0
        var jmax = 0
        var s1 = 0
        var s2 = 0
        var h1 = ""
        var h2 = ""
        var h3 = ""
        var h4 = ""

        var peerPubKey = ""
        var presharedKey = ""
        var endpointHost = ""
        var endpointPort = 51820
        var allowedIps = listOf("0.0.0.0/0", "::/0")
        var keepalive = 25

        for (rawLine in confContent.lineSequence()) {
            val line = rawLine.substringBefore('#').substringBefore(';').trim()
            if (line.isEmpty()) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length - 1).trim().lowercase()
                continue
            }
            if (!line.contains('=')) continue
            val key = line.substringBefore('=').trim().lowercase()
            val value = line.substringAfter('=').trim()

            when (currentSection) {
                "interface" -> when (key) {
                    "privatekey" -> privateKey = value
                    "address" -> addresses = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    "dns" -> dns = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    "mtu" -> mtu = value.toIntOrNull() ?: 1400
                    "reserved" -> reserved = value.removeSurrounding("[", "]")
                        .split(',').mapNotNull { it.trim().toIntOrNull() }
                    "jc" -> jc = value.toIntOrNull() ?: 0
                    "jmin" -> jmin = value.toIntOrNull() ?: 0
                    "jmax" -> jmax = value.toIntOrNull() ?: 0
                    "s1" -> s1 = value.toIntOrNull() ?: 0
                    "s2" -> s2 = value.toIntOrNull() ?: 0
                    "h1" -> h1 = value
                    "h2" -> h2 = value
                    "h3" -> h3 = value
                    "h4" -> h4 = value
                }
                "peer" -> when (key) {
                    "publickey" -> peerPubKey = value
                    "presharedkey" -> presharedKey = value
                    "allowedips" -> allowedIps = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    "persistentkeepalive" -> keepalive = value.toIntOrNull() ?: 25
                    "endpoint" -> {
                        if (value.startsWith("[")) {
                            endpointHost = value.substringAfter('[').substringBefore(']')
                            endpointPort = value.substringAfter("]:", "51820").toIntOrNull() ?: 51820
                        } else {
                            endpointHost = value.substringBeforeLast(':')
                            endpointPort = value.substringAfterLast(':', "51820").toIntOrNull() ?: 51820
                        }
                    }
                }
            }
        }

        if (privateKey.isBlank() || peerPubKey.isBlank() || endpointHost.isBlank()) return null

        val wg = WireGuardSettings(
            privateKey = privateKey,
            peerPublicKey = peerPubKey,
            presharedKey = presharedKey,
            localAddresses = addresses,
            dnsServers = dns,
            allowedIps = allowedIps,
            mtu = mtu,
            persistentKeepalive = keepalive,
            reservedBytes = reserved,
            jc = jc,
            jmin = jmin,
            jmax = jmax,
            s1 = s1,
            s2 = s2,
            h1 = h1,
            h2 = h2,
            h3 = h3,
            h4 = h4,
        )

        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = profileName,
            protocol = if (wg.hasAmneziaObfuscation) ProtocolType.AMNEZIA_WG else ProtocolType.WIREGUARD,
            serverAddress = endpointHost,
            serverPort = endpointPort,
            wireGuard = wg,
        )
    }

    fun serializeWireGuardConf(profile: TunnelProfile): String {
        val wg = profile.wireGuard
        val formattedEndpoint = if (profile.serverAddress.contains(':') && !profile.serverAddress.startsWith("[")) {
            "[${profile.serverAddress}]:${profile.serverPort}"
        } else {
            "${profile.serverAddress}:${profile.serverPort}"
        }
        return buildString {
            appendLine("[Interface]")
            appendLine("PrivateKey = ${wg.privateKey}")
            appendLine("Address = ${wg.localAddresses.joinToString(", ")}")
            if (wg.dnsServers.isNotEmpty()) {
                appendLine("DNS = ${wg.dnsServers.joinToString(", ")}")
            }
            appendLine("MTU = ${wg.mtu}")
            if (wg.reservedBytes.isNotEmpty()) {
                appendLine("Reserved = ${wg.reservedBytes.joinToString(", ")}")
            }
            if (wg.hasAmneziaObfuscation || profile.protocol == ProtocolType.AMNEZIA_WG) {
                appendLine("Jc = ${wg.jc}")
                appendLine("Jmin = ${wg.jmin}")
                appendLine("Jmax = ${wg.jmax}")
                appendLine("S1 = ${wg.s1}")
                appendLine("S2 = ${wg.s2}")
                if (wg.h1.isNotBlank()) appendLine("H1 = ${wg.h1}")
                if (wg.h2.isNotBlank()) appendLine("H2 = ${wg.h2}")
                if (wg.h3.isNotBlank()) appendLine("H3 = ${wg.h3}")
                if (wg.h4.isNotBlank()) appendLine("H4 = ${wg.h4}")
            }
            appendLine()
            appendLine("[Peer]")
            appendLine("PublicKey = ${wg.peerPublicKey}")
            if (wg.presharedKey.isNotBlank()) {
                appendLine("PresharedKey = ${wg.presharedKey}")
            }
            appendLine("AllowedIPs = ${wg.allowedIps.joinToString(", ")}")
            appendLine("Endpoint = $formattedEndpoint")
            if (wg.persistentKeepalive > 0) {
                appendLine("PersistentKeepalive = ${wg.persistentKeepalive}")
            }
        }.trimEnd()
    }

    fun parseOpenVpn(
        ovpnContent: String,
        profileName: String = "OpenVPN Profile",
        subscriptionId: String? = null,
    ): TunnelProfile? {
        if (!ovpnContent.contains("remote ", ignoreCase = true) &&
            !ovpnContent.contains("<ca>", ignoreCase = true)
        ) {
            return null
        }

        var remoteHost = ""
        var remotePort = 1194
        var proto = "udp"
        var cipher = "AES-256-GCM"
        var auth = "SHA256"

        for (rawLine in ovpnContent.lineSequence()) {
            val line = rawLine.substringBefore('#').substringBefore(';').trim()
            if (line.isEmpty()) continue
            val parts = line.split(Regex("\\s+"))
            when (parts.firstOrNull()?.lowercase()) {
                "remote" -> {
                    remoteHost = parts.getOrNull(1) ?: ""
                    remotePort = parts.getOrNull(2)?.toIntOrNull() ?: 1194
                    parts.getOrNull(3)?.let { proto = it.lowercase() }
                }
                "proto" -> proto = parts.getOrNull(1)?.lowercase() ?: proto
                "cipher" -> cipher = parts.getOrNull(1) ?: cipher
                "auth" -> auth = parts.getOrNull(1) ?: auth
            }
        }
        if (remoteHost.isBlank()) return null

        fun extractTag(tag: String): String {
            val regex = Regex("<$tag>(.*?)</$tag>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            return regex.find(ovpnContent)?.groupValues?.getOrNull(1)?.trim() ?: ""
        }

        val tlsCrypt = extractTag("tls-crypt")
        val tlsAuth = extractTag("tls-auth")

        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = profileName,
            protocol = ProtocolType.OPENVPN,
            serverAddress = remoteHost,
            serverPort = remotePort,
            openVpn = OpenVpnSettings(
                proto = proto,
                cipher = cipher,
                auth = auth,
                caCert = extractTag("ca"),
                clientCert = extractTag("cert"),
                clientKey = extractTag("key"),
                tlsAuthOrCryptKey = tlsCrypt.ifBlank { tlsAuth },
                tlsMode = if (tlsCrypt.isNotBlank()) "tls-crypt" else "tls-auth",
                rawOvpnContent = ovpnContent.trim(),
            ),
        )
    }

    fun serializeOpenVpn(profile: TunnelProfile): String {
        val ovpn = profile.openVpn
        if (ovpn.rawOvpnContent.isNotBlank()) return ovpn.rawOvpnContent
        return buildString {
            appendLine("client")
            appendLine("dev tun")
            appendLine("proto ${ovpn.proto}")
            appendLine("remote ${profile.serverAddress} ${profile.serverPort}")
            appendLine("resolv-retry infinite")
            appendLine("nobind")
            appendLine("persist-key")
            appendLine("persist-tun")
            appendLine("cipher ${ovpn.cipher}")
            appendLine("auth ${ovpn.auth}")
            if (ovpn.caCert.isNotBlank()) {
                appendLine("<ca>")
                appendLine(ovpn.caCert)
                appendLine("</ca>")
            }
            if (ovpn.clientCert.isNotBlank()) {
                appendLine("<cert>")
                appendLine(ovpn.clientCert)
                appendLine("</cert>")
            }
            if (ovpn.clientKey.isNotBlank()) {
                appendLine("<key>")
                appendLine(ovpn.clientKey)
                appendLine("</key>")
            }
            if (ovpn.tlsAuthOrCryptKey.isNotBlank()) {
                appendLine("<${ovpn.tlsMode}>")
                appendLine(ovpn.tlsAuthOrCryptKey)
                appendLine("</${ovpn.tlsMode}>")
            }
        }.trimEnd()
    }
}
