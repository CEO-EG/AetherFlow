package com.ceoeg.aetherflow.core.routing

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class AppCategory(val label: String) {
    ALL("All"),
    BROWSER("Browsers"),
    MESSAGING("Messaging"),
    MEDIA("Media & Video"),
    BANKING_FINANCE("Banking & Finance"),
    SYSTEM_UTILITY("System & Utilities"),
}

data class InstalledAppEntry(
    val packageName: String,
    val label: String,
    val uid: Int,
    val isSystemApp: Boolean,
    val category: AppCategory,
)

@Singleton
class SplitTunnelAppManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cacheMutex = Mutex()
    private var cachedApps: List<InstalledAppEntry>? = null
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<InstalledAppEntry> =
        withContext(Dispatchers.IO) {
            cacheMutex.withLock {
                if (!forceRefresh && cachedApps != null) {
                    return@withContext cachedApps.orEmpty()
                }
                val pm = context.packageManager
                val selfPkg = context.packageName
                val apps = runCatching {
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                        .asSequence()
                        .filter { it.packageName != selfPkg }
                        .map { info ->
                            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                                (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
                            val label = runCatching { pm.getApplicationLabel(info).toString() }
                                .getOrDefault(info.packageName)
                            InstalledAppEntry(
                                packageName = info.packageName,
                                label = label,
                                uid = info.uid,
                                isSystemApp = isSystem,
                                category = classifyPackage(info.packageName, label, isSystem),
                            )
                        }
                        .sortedBy { it.label.lowercase() }
                        .toList()
                }.getOrDefault(emptyList())
                cachedApps = apps
                apps
            }
        }

    suspend fun loadAppIcon(packageName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        iconCache[packageName]?.let { return@withContext it }
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val width = drawable.intrinsicWidth.coerceIn(48, 144)
            val height = drawable.intrinsicHeight.coerceIn(48, 144)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap().also { iconCache[packageName] = it }
        }.getOrNull()
    }

    companion object {
        fun classifyPackage(packageName: String, label: String, isSystemApp: Boolean): AppCategory {
            val p = packageName.lowercase()
            val l = label.lowercase()
            return when {
                BROWSER_HINTS.any { p.contains(it) || l.contains(it) } -> AppCategory.BROWSER
                MESSAGING_HINTS.any { p.contains(it) || l.contains(it) } -> AppCategory.MESSAGING
                MEDIA_HINTS.any { p.contains(it) || l.contains(it) } -> AppCategory.MEDIA
                FINANCE_HINTS.any { p.contains(it) || l.contains(it) } -> AppCategory.BANKING_FINANCE
                isSystemApp -> AppCategory.SYSTEM_UTILITY
                else -> AppCategory.SYSTEM_UTILITY
            }
        }

        private val BROWSER_HINTS = listOf(
            "chrome", "firefox", "mozilla", "brave", "opera", "vivaldi", "duckduckgo", "edge", "browser", "torproject"
        )
        private val MESSAGING_HINTS = listOf(
            "telegram", "whatsapp", "signal", "discord", "slack", "messenger", "viber", "line", "Element", "matrix", "wechat"
        )
        private val MEDIA_HINTS = listOf(
            "youtube", "spotify", "netflix", "twitch", "tiktok", "plex", "vlc", "jellyfin", "soundcloud"
        )
        private val FINANCE_HINTS = listOf(
            "bank", "wallet", "pay", "revolut", "wise", "paypal", "stripe", "cashapp", "venmo", "finance", "crypto", "binance"
        )
    }
}
