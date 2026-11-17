package com.ceoeg.aetherflow.core.common.logging

import com.ceoeg.aetherflow.core.common.model.DiagnosticLogEntry
import com.ceoeg.aetherflow.core.common.model.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Structured ring-buffer diagnostic logger with 1-tap IPv4/IPv6, UUID, key, and
 * credential redaction for safe enterprise log sharing.
 */
@Singleton
class DiagnosticLogger @Inject constructor() {

    private val sequence = AtomicLong(1L)
    private val _entries = MutableStateFlow<List<DiagnosticLogEntry>>(emptyList())
    val entries: StateFlow<List<DiagnosticLogEntry>> = _entries.asStateFlow()

    fun log(severity: LogSeverity, source: String, message: String) {
        if (severity == LogSeverity.NONE) return
        val entry = DiagnosticLogEntry(
            id = sequence.getAndIncrement(),
            timestampMs = System.currentTimeMillis(),
            severity = severity,
            source = source,
            message = message,
        )
        _entries.update { current ->
            val next = current + entry
            if (next.size > MAX_ENTRIES) next.takeLast(MAX_ENTRIES) else next
        }
    }

    fun info(source: String, message: String) = log(LogSeverity.INFO, source, message)
    fun warn(source: String, message: String) = log(LogSeverity.WARNING, source, message)
    fun error(source: String, message: String) = log(LogSeverity.ERROR, source, message)
    fun debug(source: String, message: String) = log(LogSeverity.DEBUG, source, message)

    fun clear() {
        _entries.value = emptyList()
    }

    fun exportFormattedLogs(
        minSeverity: LogSeverity = LogSeverity.DEBUG,
        keyword: String = "",
        redactSensitive: Boolean = true,
    ): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return _entries.value
            .asSequence()
            .filter { it.severity.rank <= minSeverity.rank && it.severity != LogSeverity.NONE }
            .filter {
                keyword.isBlank() ||
                    it.message.contains(keyword, ignoreCase = true) ||
                    it.source.contains(keyword, ignoreCase = true)
            }
            .joinToString("\n") { entry ->
                val ts = formatter.format(Date(entry.timestampMs))
                val msg = if (redactSensitive) redactSensitiveData(entry.message) else entry.message
                "$ts [${entry.severity.wireLevel.uppercase()}] [${entry.source}] $msg"
            }
    }

    companion object {
        private const val MAX_ENTRIES = 1500

        private val IPV4_REGEX = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""")
        private val IPV6_REGEX = Regex("""\b(?:[0-9a-fA-F]{1,4}:){2,7}[0-9a-fA-F]{1,4}\b""")
        private val UUID_REGEX = Regex(
            """\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b"""
        )
        private val SECRET_KV_REGEX = Regex(
            """(?i)(password|pass|secret|private_?key|public_?key|pbk|preshared_?key|token|auth)\s*([=:])\s*([^\s,"'}\]]+)"""
        )

        fun redactSensitiveData(raw: String): String {
            return raw
                .replace(UUID_REGEX, "<REDACTED_UUID>")
                .replace(SECRET_KV_REGEX) { match ->
                    "${match.groupValues[1]}${match.groupValues[2]}<REDACTED_SECRET>"
                }
                .replace(IPV4_REGEX) { match ->
                    val ip = match.value
                    if (ip.startsWith("127.") || ip.startsWith("198.18.") || ip == "0.0.0.0") {
                        ip
                    } else {
                        "<REDACTED_IPV4>"
                    }
                }
                .replace(IPV6_REGEX, "<REDACTED_IPV6>")
        }
    }
}
