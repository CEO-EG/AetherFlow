package com.ceoeg.aetherflow.core.common.database

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AetherFlowDatabase_Impl : AetherFlowDatabase() {
  private val _profileDao: Lazy<ProfileDao> = lazy {
    ProfileDao_Impl(this)
  }

  private val _subscriptionDao: Lazy<SubscriptionDao> = lazy {
    SubscriptionDao_Impl(this)
  }

  private val _routingRuleDao: Lazy<RoutingRuleDao> = lazy {
    RoutingRuleDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "3c3e12dd065dfe55c79ec4b5acf74417", "6ecbbcd7393b745e75ae26670f1c29d9") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tunnel_profiles` (`id` TEXT NOT NULL, `subscriptionId` TEXT, `name` TEXT NOT NULL, `protocolName` TEXT NOT NULL, `serverAddress` TEXT NOT NULL, `serverPort` INTEGER NOT NULL, `regionCode` TEXT NOT NULL, `tcpPingMs` INTEGER NOT NULL, `urlTestDelayMs` INTEGER NOT NULL, `downloadSpeedMbps` REAL NOT NULL, `lastTestedEpochMs` INTEGER NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, `encryptedPayload` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `subscriptions` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `encryptedUrl` TEXT NOT NULL, `userAgentName` TEXT NOT NULL, `autoUpdateEnabled` INTEGER NOT NULL, `updateIntervalHours` INTEGER NOT NULL, `smartAutoSelect` INTEGER NOT NULL, `uploadBytes` INTEGER NOT NULL, `downloadBytes` INTEGER NOT NULL, `totalQuotaBytes` INTEGER NOT NULL, `expireEpochSec` INTEGER NOT NULL, `supportUrl` TEXT NOT NULL, `profileCount` INTEGER NOT NULL, `lastSyncedEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `routing_rules` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `matchTypeName` TEXT NOT NULL, `value` TEXT NOT NULL, `actionName` TEXT NOT NULL, `priority` INTEGER NOT NULL, `isBuiltInPreset` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3c3e12dd065dfe55c79ec4b5acf74417')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `tunnel_profiles`")
        connection.execSQL("DROP TABLE IF EXISTS `subscriptions`")
        connection.execSQL("DROP TABLE IF EXISTS `routing_rules`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsTunnelProfiles: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTunnelProfiles.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTunnelProfiles.put("subscriptionId", TableInfo.Column("subscriptionId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTunnelProfiles.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTunnelProfiles.put("protocolName", TableInfo.Column("protocolName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTunnelProfiles.put("serverAddress", TableInfo.Column("serverAddress", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTunnelProfiles.put("serverPort", TableInfo.Column("serverPort", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTunnelProfiles.put("regionCode", TableInfo.Column("regionCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTunnelProfiles.put("tcpPingMs", TableInfo.Column("tcpPingMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTunnelProfiles.put("urlTestDelayMs", TableInfo.Column("urlTestDelayMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTunnelProfiles.put("downloadSpeedMbps", TableInfo.Column("downloadSpeedMbps", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTunnelProfiles.put("lastTestedEpochMs", TableInfo.Column("lastTestedEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTunnelProfiles.put("createdAtEpochMs", TableInfo.Column("createdAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTunnelProfiles.put("encryptedPayload", TableInfo.Column("encryptedPayload", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTunnelProfiles: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTunnelProfiles: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTunnelProfiles: TableInfo = TableInfo("tunnel_profiles", _columnsTunnelProfiles, _foreignKeysTunnelProfiles, _indicesTunnelProfiles)
        val _existingTunnelProfiles: TableInfo = read(connection, "tunnel_profiles")
        if (!_infoTunnelProfiles.equals(_existingTunnelProfiles)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |tunnel_profiles(com.ceoeg.aetherflow.core.common.database.ProfileEntity).
              | Expected:
              |""".trimMargin() + _infoTunnelProfiles + """
              |
              | Found:
              |""".trimMargin() + _existingTunnelProfiles)
        }
        val _columnsSubscriptions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSubscriptions.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubscriptions.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubscriptions.put("encryptedUrl", TableInfo.Column("encryptedUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubscriptions.put("userAgentName", TableInfo.Column("userAgentName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubscriptions.put("autoUpdateEnabled", TableInfo.Column("autoUpdateEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubscriptions.put("updateIntervalHours", TableInfo.Column("updateIntervalHours", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubscriptions.put("smartAutoSelect", TableInfo.Column("smartAutoSelect", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubscriptions.put("uploadBytes", TableInfo.Column("uploadBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubscriptions.put("downloadBytes", TableInfo.Column("downloadBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubscriptions.put("totalQuotaBytes", TableInfo.Column("totalQuotaBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubscriptions.put("expireEpochSec", TableInfo.Column("expireEpochSec", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubscriptions.put("supportUrl", TableInfo.Column("supportUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubscriptions.put("profileCount", TableInfo.Column("profileCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubscriptions.put("lastSyncedEpochMs", TableInfo.Column("lastSyncedEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSubscriptions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSubscriptions: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSubscriptions: TableInfo = TableInfo("subscriptions", _columnsSubscriptions, _foreignKeysSubscriptions, _indicesSubscriptions)
        val _existingSubscriptions: TableInfo = read(connection, "subscriptions")
        if (!_infoSubscriptions.equals(_existingSubscriptions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |subscriptions(com.ceoeg.aetherflow.core.common.database.SubscriptionEntity).
              | Expected:
              |""".trimMargin() + _infoSubscriptions + """
              |
              | Found:
              |""".trimMargin() + _existingSubscriptions)
        }
        val _columnsRoutingRules: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRoutingRules.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRoutingRules.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRoutingRules.put("enabled", TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRoutingRules.put("matchTypeName", TableInfo.Column("matchTypeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRoutingRules.put("value", TableInfo.Column("value", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRoutingRules.put("actionName", TableInfo.Column("actionName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRoutingRules.put("priority", TableInfo.Column("priority", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRoutingRules.put("isBuiltInPreset", TableInfo.Column("isBuiltInPreset", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRoutingRules: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRoutingRules: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRoutingRules: TableInfo = TableInfo("routing_rules", _columnsRoutingRules, _foreignKeysRoutingRules, _indicesRoutingRules)
        val _existingRoutingRules: TableInfo = read(connection, "routing_rules")
        if (!_infoRoutingRules.equals(_existingRoutingRules)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |routing_rules(com.ceoeg.aetherflow.core.common.database.RoutingRuleEntity).
              | Expected:
              |""".trimMargin() + _infoRoutingRules + """
              |
              | Found:
              |""".trimMargin() + _existingRoutingRules)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "tunnel_profiles", "subscriptions", "routing_rules")
  }

  public override fun clearAllTables() {
    super.performClear(false, "tunnel_profiles", "subscriptions", "routing_rules")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(ProfileDao::class, ProfileDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(SubscriptionDao::class, SubscriptionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(RoutingRuleDao::class, RoutingRuleDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun profileDao(): ProfileDao = _profileDao.value

  public override fun subscriptionDao(): SubscriptionDao = _subscriptionDao.value

  public override fun routingRuleDao(): RoutingRuleDao = _routingRuleDao.value
}
