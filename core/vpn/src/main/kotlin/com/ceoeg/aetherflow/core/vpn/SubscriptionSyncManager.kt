package com.ceoeg.aetherflow.core.vpn

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ceoeg.aetherflow.core.common.database.TunnelRepository
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger
import com.ceoeg.aetherflow.core.common.model.SubscriptionFeed
import com.ceoeg.aetherflow.core.parser.ClashAndJsonBundleCodec
import com.ceoeg.aetherflow.core.parser.UniversalConfigParser
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise Subscription Hub Manager:
 * - Fetches remote feeds with configurable User-Agent presets (`v2rayNG`, `ClashMeta`, `sing-box`, `Shadowrocket`)
 * - Extracts `Subscription-Userinfo` (`upload`, `download`, `total`, `expire`) quota telemetry
 * - Schedules periodic background refresh via WorkManager
 */
@Singleton
class SubscriptionSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TunnelRepository,
    private val parser: UniversalConfigParser,
    private val logger: DiagnosticLogger,
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun syncSubscription(feed: SubscriptionFeed): Result<SubscriptionFeed> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(feed.url)
                    .header("User-Agent", feed.userAgent.headerValue)
                    .header("Accept", "*/*")
                    .build()

                httpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        error("HTTP ${resp.code} syncing '${feed.name}'")
                    }
                    val body = resp.body?.string() ?: error("Empty subscription payload")
                    val userInfoHeader = resp.header("Subscription-Userinfo")
                        ?: resp.header("subscription-userinfo")
                    val supportUrl = resp.header("support-url").orEmpty()
                    val quota = ClashAndJsonBundleCodec.parseSubscriptionUserInfoHeader(userInfoHeader)

                    val parsedProfiles = parser.importAny(
                        rawContent = body,
                        subscriptionId = feed.id,
                        hintName = feed.name,
                    )
                    require(parsedProfiles.isNotEmpty()) {
                        "Subscription response contained 0 recognizable tunnel endpoints."
                    }

                    repository.replaceSubscriptionProfiles(feed.id, parsedProfiles)

                    val updatedFeed = feed.copy(
                        uploadBytes = quota.uploadBytes,
                        downloadBytes = quota.downloadBytes,
                        totalQuotaBytes = quota.totalBytes,
                        expireEpochSec = quota.expireEpochSec,
                        supportUrl = supportUrl.ifBlank { feed.supportUrl },
                        profileCount = parsedProfiles.size,
                        lastSyncedEpochMs = System.currentTimeMillis(),
                    )
                    repository.upsertSubscription(updatedFeed)
                    logger.info(
                        "SubscriptionSync",
                        "Synced '${feed.name}': ${parsedProfiles.size} nodes (${updatedFeed.userAgent.label})"
                    )
                    updatedFeed
                }
            }
        }

    suspend fun syncAllAutoEnabled(): Int {
        val feeds = repository.getAllSubscriptions().filter { it.autoUpdateEnabled }
        var succeeded = 0
        for (feed in feeds) {
            if (syncSubscription(feed).isSuccess) succeeded++
        }
        return succeeded
    }

    fun schedulePeriodicSync(intervalHours: Int = 12) {
        val safeHours = intervalHours.coerceIn(1, 168).toLong()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = PeriodicWorkRequestBuilder<SubscriptionSyncWorker>(safeHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "aetherflow_subscription_periodic_sync"
    }
}

@HiltWorker
class SubscriptionSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SubscriptionSyncManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            syncManager.syncAllAutoEnabled()
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}
