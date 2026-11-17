package com.ceoeg.aetherflow.core.parser

import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object Base64Codec {

    fun encodeStandard(text: String): String =
        Base64.Default.encode(text.encodeToByteArray())

    fun encodeUrlSafeNoPad(text: String): String =
        Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(text.encodeToByteArray())

    fun decodeFlexible(encoded: String): String? {
        val cleaned = encoded.trim()
            .replace("\r", "")
            .replace("\n", "")
            .replace(" ", "")
        if (cleaned.isEmpty()) return null

        val normalized = cleaned.replace('-', '+').replace('_', '/')
        val padded = when (normalized.length % 4) {
            2 -> "$normalized=="
            3 -> "$normalized="
            0 -> normalized
            else -> return null
        }
        return runCatching {
            Base64.Default.decode(padded).decodeToString()
        }.getOrNull()
    }

    fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    fun urlDecode(value: String): String =
        runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrDefault(value)
}
