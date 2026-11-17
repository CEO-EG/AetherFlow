package com.ceoeg.aetherflow.core.common.security

import com.ceoeg.aetherflow.core.common.model.SessionLoopbackAuth
import java.net.InetAddress
import java.net.ServerSocket
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardens local anti-fingerprinting by dynamically allocating ephemeral OS-verified
 * loopback ports (never static 127.0.0.1:10808 / 9090) and generating per-session
 * 256-bit cryptographic SOCKS5 & Clash-API credentials on every connection startup.
 */
@Singleton
class EphemeralLoopbackAuthGenerator @Inject constructor() {

    private val secureRandom = SecureRandom()

    fun generateSessionAuth(): SessionLoopbackAuth {
        val allocatedPorts = allocateDistinctLoopbackPorts(count = 3)
        return SessionLoopbackAuth(
            socksPort = allocatedPorts[0],
            httpPort = allocatedPorts[1],
            controlApiPort = allocatedPorts[2],
            username = "af_${randomHex(12)}",
            password = randomHex(24),
            clashApiSecret = randomHex(24),
        )
    }

    private fun allocateDistinctLoopbackPorts(count: Int): List<Int> {
        val loopback = InetAddress.getByName("127.0.0.1")
        val sockets = mutableListOf<ServerSocket>()
        return try {
            repeat(count) {
                sockets += ServerSocket(0, 1, loopback).apply { reuseAddress = true }
            }
            sockets.map { it.localPort }
        } catch (_: Throwable) {
            // Fallback randomized unprivileged range (32768..60999) avoiding known proxy ports
            val chosen = linkedSetOf<Int>()
            while (chosen.size < count) {
                val candidate = 32768 + secureRandom.nextInt(28000)
                if (candidate !in WELL_KNOWN_PROXY_PORTS) {
                    chosen += candidate
                }
            }
            chosen.toList()
        } finally {
            sockets.forEach { runCatching { it.close() } }
        }
    }

    private fun randomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        val WELL_KNOWN_PROXY_PORTS = setOf(1080, 10808, 10809, 2080, 7890, 7891, 9090, 8080, 8888)
    }
}
