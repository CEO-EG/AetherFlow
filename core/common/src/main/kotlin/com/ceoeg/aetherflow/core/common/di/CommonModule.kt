package com.ceoeg.aetherflow.core.common.di

import android.content.Context
import androidx.room.Room
import com.ceoeg.aetherflow.core.common.database.AetherFlowDatabase
import com.ceoeg.aetherflow.core.common.database.ProfileDao
import com.ceoeg.aetherflow.core.common.database.RoutingRuleDao
import com.ceoeg.aetherflow.core.common.database.SubscriptionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AetherFlowDatabase {
        return Room.databaseBuilder(
            context,
            AetherFlowDatabase::class.java,
            "aetherflow_enterprise.db",
        ).fallbackToDestructiveMigration(false).build()
    }

    @Provides
    fun provideProfileDao(db: AetherFlowDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideSubscriptionDao(db: AetherFlowDatabase): SubscriptionDao = db.subscriptionDao()

    @Provides
    fun provideRoutingRuleDao(db: AetherFlowDatabase): RoutingRuleDao = db.routingRuleDao()
}
