package com.ceoeg.aetherflow.core.common.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.ceoeg.aetherflow.core.common.model.RoutingRule
import com.ceoeg.aetherflow.core.common.model.RuleAction
import com.ceoeg.aetherflow.core.common.model.RuleMatchType
import com.ceoeg.aetherflow.core.common.model.SubscriptionFeed
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import com.ceoeg.aetherflow.core.common.model.UserAgentPreset
import com.ceoeg.aetherflow.core.common.security.CredentialCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Entity(tableName = "tunnel_profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val subscriptionId: String?,
    val name: String,
    val protocolName: String,
    val serverAddress: String,
    val serverPort: Int,
    val regionCode: String,
    val tcpPingMs: Long,
    val urlTestDelayMs: Long,
    val downloadSpeedMbps: Double,
    val lastTestedEpochMs: Long,
    val createdAtEpochMs: Long,
    /** Encrypted serialized JSON payload containing UUIDs, keys, passwords, and transport config. */
    val encryptedPayload: String,
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val encryptedUrl: String,
    val userAgentName: String,
    val autoUpdateEnabled: Boolean,
    val updateIntervalHours: Int,
    val smartAutoSelect: Boolean,
    val uploadBytes: Long,
    val downloadBytes: Long,
    val totalQuotaBytes: Long,
    val expireEpochSec: Long,
    val supportUrl: String,
    val profileCount: Int,
    val lastSyncedEpochMs: Long,
)

@Entity(tableName = "routing_rules")
data class RoutingRuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val enabled: Boolean,
    val matchTypeName: String,
    val value: String,
    val actionName: String,
    val priority: Int,
    val isBuiltInPreset: Boolean,
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM tunnel_profiles ORDER BY CASE WHEN urlTestDelayMs > 0 THEN urlTestDelayMs ELSE 999999 END ASC, createdAtEpochMs DESC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM tunnel_profiles")
    suspend fun getAll(): List<ProfileEntity>

    @Query("SELECT * FROM tunnel_profiles WHERE subscriptionId = :subscriptionId")
    suspend fun getBySubscription(subscriptionId: String): List<ProfileEntity>

    @Query("SELECT * FROM tunnel_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ProfileEntity>)

    @Query("DELETE FROM tunnel_profiles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tunnel_profiles WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM tunnel_profiles WHERE subscriptionId = :subscriptionId")
    suspend fun deleteBySubscription(subscriptionId: String)

    @Query("DELETE FROM tunnel_profiles")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceSubscriptionProfiles(subscriptionId: String, entities: List<ProfileEntity>) {
        deleteBySubscription(subscriptionId)
        upsertAll(entities)
    }
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY name ASC")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions")
    suspend fun getAll(): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SubscriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SubscriptionEntity>)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAll()
}

@Dao
interface RoutingRuleDao {
    @Query("SELECT * FROM routing_rules ORDER BY priority ASC, name ASC")
    fun observeAll(): Flow<List<RoutingRuleEntity>>

    @Query("SELECT * FROM routing_rules ORDER BY priority ASC, name ASC")
    suspend fun getAll(): List<RoutingRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RoutingRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<RoutingRuleEntity>)

    @Query("DELETE FROM routing_rules WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM routing_rules")
    suspend fun deleteAll()
}

@Database(
    entities = [
        ProfileEntity::class,
        SubscriptionEntity::class,
        RoutingRuleEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AetherFlowDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun routingRuleDao(): RoutingRuleDao
}

/**
 * Encrypted Repository over Room DAOs ensuring all credentials in TunnelProfile and
 * Subscription URLs are encrypted via Android Keystore AES-256-GCM at rest.
 */
@Singleton
class TunnelRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val subscriptionDao: SubscriptionDao,
    private val routingRuleDao: RoutingRuleDao,
    private val cipher: CredentialCipher,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val profilesFlow: Flow<List<TunnelProfile>> = profileDao.observeAll().map { list ->
        list.mapNotNull { decodeProfile(it) }
    }

    val subscriptionsFlow: Flow<List<SubscriptionFeed>> = subscriptionDao.observeAll().map { list ->
        list.map { decodeSubscription(it) }
    }

    val routingRulesFlow: Flow<List<RoutingRule>> = routingRuleDao.observeAll().map { list ->
        list.map { decodeRule(it) }
    }

    suspend fun getAllProfiles(): List<TunnelProfile> =
        profileDao.getAll().mapNotNull { decodeProfile(it) }

    suspend fun getProfilesBySubscription(subscriptionId: String): List<TunnelProfile> =
        profileDao.getBySubscription(subscriptionId).mapNotNull { decodeProfile(it) }

    suspend fun getProfileById(id: String): TunnelProfile? =
        profileDao.getById(id)?.let { decodeProfile(it) }

    suspend fun upsertProfile(profile: TunnelProfile) {
        profileDao.upsert(encodeProfile(profile))
    }

    suspend fun upsertProfiles(profiles: List<TunnelProfile>) {
        profileDao.upsertAll(profiles.map { encodeProfile(it) })
    }

    suspend fun replaceSubscriptionProfiles(subscriptionId: String, profiles: List<TunnelProfile>) {
        profileDao.replaceSubscriptionProfiles(subscriptionId, profiles.map { encodeProfile(it) })
    }

    suspend fun deleteProfile(id: String) = profileDao.deleteById(id)

    suspend fun deleteProfiles(ids: List<String>) {
        if (ids.isNotEmpty()) profileDao.deleteByIds(ids)
    }

    suspend fun deduplicateProfiles(): Int {
        val all = getAllProfiles()
        val seen = HashSet<String>()
        val duplicates = mutableListOf<String>()
        for (profile in all) {
            if (!seen.add(profile.deduplicationKey())) {
                duplicates += profile.id
            }
        }
        if (duplicates.isNotEmpty()) {
            profileDao.deleteByIds(duplicates)
        }
        return duplicates.size
    }

    suspend fun purgeUnreachableProfiles(): Int {
        val unreachableIds = getAllProfiles()
            .filter { it.lastTestedEpochMs > 0L && it.urlTestDelayMs <= 0L && it.tcpPingMs <= 0L }
            .map { it.id }
        if (unreachableIds.isNotEmpty()) {
            profileDao.deleteByIds(unreachableIds)
        }
        return unreachableIds.size
    }

    suspend fun getAllSubscriptions(): List<SubscriptionFeed> =
        subscriptionDao.getAll().map { decodeSubscription(it) }

    suspend fun getSubscriptionById(id: String): SubscriptionFeed? =
        subscriptionDao.getById(id)?.let { decodeSubscription(it) }

    suspend fun upsertSubscription(feed: SubscriptionFeed) {
        subscriptionDao.upsert(encodeSubscription(feed))
    }

    suspend fun deleteSubscriptionAndProfiles(id: String) {
        profileDao.deleteBySubscription(id)
        subscriptionDao.deleteById(id)
    }

    suspend fun getAllRoutingRules(): List<RoutingRule> =
        routingRuleDao.getAll().map { decodeRule(it) }

    suspend fun upsertRoutingRule(rule: RoutingRule) {
        routingRuleDao.upsert(encodeRule(rule))
    }

    suspend fun upsertRoutingRules(rules: List<RoutingRule>) {
        routingRuleDao.upsertAll(rules.map { encodeRule(it) })
    }

    suspend fun deleteRoutingRule(id: String) = routingRuleDao.deleteById(id)

    suspend fun restoreFullSnapshot(
        profiles: List<TunnelProfile>,
        subscriptions: List<SubscriptionFeed>,
        rules: List<RoutingRule>,
    ) {
        profileDao.deleteAll()
        subscriptionDao.deleteAll()
        routingRuleDao.deleteAll()
        subscriptionDao.upsertAll(subscriptions.map { encodeSubscription(it) })
        profileDao.upsertAll(profiles.map { encodeProfile(it) })
        routingRuleDao.upsertAll(rules.map { encodeRule(it) })
    }

    private fun encodeProfile(profile: TunnelProfile): ProfileEntity {
        val rawJson = json.encodeToString(TunnelProfile.serializer(), profile)
        return ProfileEntity(
            id = profile.id,
            subscriptionId = profile.subscriptionId,
            name = profile.name,
            protocolName = profile.protocol.name,
            serverAddress = profile.serverAddress,
            serverPort = profile.serverPort,
            regionCode = profile.regionCode,
            tcpPingMs = profile.tcpPingMs,
            urlTestDelayMs = profile.urlTestDelayMs,
            downloadSpeedMbps = profile.downloadSpeedMbps,
            lastTestedEpochMs = profile.lastTestedEpochMs,
            createdAtEpochMs = profile.createdAtEpochMs,
            encryptedPayload = cipher.encryptString(rawJson),
        )
    }

    private fun decodeProfile(entity: ProfileEntity): TunnelProfile? {
        return runCatching {
            val rawJson = cipher.decryptString(entity.encryptedPayload)
            json.decodeFromString(TunnelProfile.serializer(), rawJson).copy(
                tcpPingMs = entity.tcpPingMs,
                urlTestDelayMs = entity.urlTestDelayMs,
                downloadSpeedMbps = entity.downloadSpeedMbps,
                lastTestedEpochMs = entity.lastTestedEpochMs,
            )
        }.getOrNull()
    }

    private fun encodeSubscription(feed: SubscriptionFeed): SubscriptionEntity {
        return SubscriptionEntity(
            id = feed.id,
            name = feed.name,
            encryptedUrl = cipher.encryptString(feed.url),
            userAgentName = feed.userAgent.name,
            autoUpdateEnabled = feed.autoUpdateEnabled,
            updateIntervalHours = feed.updateIntervalHours,
            smartAutoSelect = feed.smartAutoSelect,
            uploadBytes = feed.uploadBytes,
            downloadBytes = feed.downloadBytes,
            totalQuotaBytes = feed.totalQuotaBytes,
            expireEpochSec = feed.expireEpochSec,
            supportUrl = feed.supportUrl,
            profileCount = feed.profileCount,
            lastSyncedEpochMs = feed.lastSyncedEpochMs,
        )
    }

    private fun decodeSubscription(entity: SubscriptionEntity): SubscriptionFeed {
        return SubscriptionFeed(
            id = entity.id,
            name = entity.name,
            url = runCatching { cipher.decryptString(entity.encryptedUrl) }.getOrDefault(""),
            userAgent = runCatching { UserAgentPreset.valueOf(entity.userAgentName) }
                .getOrDefault(UserAgentPreset.SING_BOX),
            autoUpdateEnabled = entity.autoUpdateEnabled,
            updateIntervalHours = entity.updateIntervalHours,
            smartAutoSelect = entity.smartAutoSelect,
            uploadBytes = entity.uploadBytes,
            downloadBytes = entity.downloadBytes,
            totalQuotaBytes = entity.totalQuotaBytes,
            expireEpochSec = entity.expireEpochSec,
            supportUrl = entity.supportUrl,
            profileCount = entity.profileCount,
            lastSyncedEpochMs = entity.lastSyncedEpochMs,
        )
    }

    private fun encodeRule(rule: RoutingRule): RoutingRuleEntity {
        return RoutingRuleEntity(
            id = rule.id,
            name = rule.name,
            enabled = rule.enabled,
            matchTypeName = rule.matchType.name,
            value = rule.value,
            actionName = rule.action.name,
            priority = rule.priority,
            isBuiltInPreset = rule.isBuiltInPreset,
        )
    }

    private fun decodeRule(entity: RoutingRuleEntity): RoutingRule {
        return RoutingRule(
            id = entity.id,
            name = entity.name,
            enabled = entity.enabled,
            matchType = runCatching { RuleMatchType.valueOf(entity.matchTypeName) }
                .getOrDefault(RuleMatchType.DOMAIN_SUFFIX),
            value = entity.value,
            action = runCatching { RuleAction.valueOf(entity.actionName) }
                .getOrDefault(RuleAction.PROXY),
            priority = entity.priority,
            isBuiltInPreset = entity.isBuiltInPreset,
        )
    }
}
