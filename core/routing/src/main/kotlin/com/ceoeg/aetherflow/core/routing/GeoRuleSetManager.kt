package com.ceoeg.aetherflow.core.routing

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class GeoAssetStatus(
    val id: String,
    val fileName: String,
    val engineFormat: String,
    val downloadUrl: String,
    val installed: Boolean,
    val sizeBytes: Long,
    val lastModifiedEpochMs: Long,
    val sha256Prefix: String,
)

@Singleton
class GeoRuleSetManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    val geoDirectory: File
        get() = File(context.filesDir, "geo").apply { mkdirs() }

    private val _assets = MutableStateFlow<List<GeoAssetStatus>>(emptyList())
    val assets: StateFlow<List<GeoAssetStatus>> = _assets.asStateFlow()

    suspend fun refreshStatus(): List<GeoAssetStatus> = withContext(Dispatchers.IO) {
        val list = DEFAULT_GEO_FEEDS.map { spec ->
            val file = File(geoDirectory, spec.fileName)
            val exists = file.exists() && file.length() > 0L
            GeoAssetStatus(
                id = spec.id,
                fileName = spec.fileName,
                engineFormat = spec.engineFormat,
                downloadUrl = spec.url,
                installed = exists,
                sizeBytes = if (exists) file.length() else 0L,
                lastModifiedEpochMs = if (exists) file.lastModified() else 0L,
                sha256Prefix = if (exists) sha256Short(file) else "not-installed",
            )
        }
        _assets.value = list
        list
    }

    suspend fun updateAllAssets(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var updated = 0
            for (spec in DEFAULT_GEO_FEEDS) {
                val request = Request.Builder()
                    .url(spec.url)
                    .header("User-Agent", "AetherFlow/1.0.0")
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("HTTP ${response.code} fetching ${spec.fileName}")
                    }
                    val body = response.body ?: error("Empty response for ${spec.fileName}")
                    val tmpFile = File(geoDirectory, "${spec.fileName}.tmp")
                    body.byteStream().use { input ->
                        tmpFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val dest = File(geoDirectory, spec.fileName)
                    tmpFile.copyTo(dest, overwrite = true)
                    tmpFile.delete()
                    updated++
                }
            }
            refreshStatus()
            updated
        }
    }

    private fun sha256Short(file: File): String {
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buf = ByteArray(8192)
                var read = input.read(buf)
                while (read > 0) {
                    digest.update(buf, 0, read)
                    read = input.read(buf)
                }
            }
            digest.digest().take(6).joinToString("") { "%02x".format(it) }
        }.getOrDefault("unknown")
    }

    private data class GeoFeedSpec(
        val id: String,
        val fileName: String,
        val engineFormat: String,
        val url: String,
    )

    companion object {
        val REGIONAL_PRESETS = listOf(
            "" to "Disabled (Global Routing)",
            "cn" to "China Mainland Direct Bypass (CN)",
            "ir" to "Iran Domestic Direct Bypass (IR)",
            "ru" to "Russia Domestic Direct Bypass (RU)",
            "tr" to "Turkey Domestic Direct Bypass (TR)",
            "id" to "Indonesia Domestic Direct Bypass (ID)",
        )

        private val DEFAULT_GEO_FEEDS = listOf(
            GeoFeedSpec(
                id = "xray-geoip",
                fileName = "geoip.dat",
                engineFormat = "Xray (.dat)",
                url = "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geoip.dat",
            ),
            GeoFeedSpec(
                id = "xray-geosite",
                fileName = "geosite.dat",
                engineFormat = "Xray (.dat)",
                url = "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geosite.dat",
            ),
            GeoFeedSpec(
                id = "singbox-ads-srs",
                fileName = "geosite-category-ads-all.srs",
                engineFormat = "Sing-box (.srs)",
                url = "https://raw.githubusercontent.com/SagerNet/sing-geosite/rule-set/geosite-category-ads-all.srs",
            ),
            GeoFeedSpec(
                id = "singbox-geoip-private",
                fileName = "geoip-private.srs",
                engineFormat = "Sing-box (.srs)",
                url = "https://raw.githubusercontent.com/SagerNet/sing-geoip/rule-set/geoip-private.srs",
            ),
        )
    }
}
