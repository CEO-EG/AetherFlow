package com.ceoeg.aetherflow

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ceoeg.aetherflow.core.vpn.SubscriptionSyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AetherFlowApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var subscriptionSyncManager: SubscriptionSyncManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        runCatching {
            subscriptionSyncManager.schedulePeriodicSync(intervalHours = 12)
        }
    }
}
