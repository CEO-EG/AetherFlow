package com.ceoeg.aetherflow.core.common.database

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class SubscriptionDao_Impl(
  __db: RoomDatabase,
) : SubscriptionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSubscriptionEntity: EntityInsertAdapter<SubscriptionEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfSubscriptionEntity = object : EntityInsertAdapter<SubscriptionEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `subscriptions` (`id`,`name`,`encryptedUrl`,`userAgentName`,`autoUpdateEnabled`,`updateIntervalHours`,`smartAutoSelect`,`uploadBytes`,`downloadBytes`,`totalQuotaBytes`,`expireEpochSec`,`supportUrl`,`profileCount`,`lastSyncedEpochMs`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SubscriptionEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.encryptedUrl)
        statement.bindText(4, entity.userAgentName)
        val _tmp: Int = if (entity.autoUpdateEnabled) 1 else 0
        statement.bindLong(5, _tmp.toLong())
        statement.bindLong(6, entity.updateIntervalHours.toLong())
        val _tmp_1: Int = if (entity.smartAutoSelect) 1 else 0
        statement.bindLong(7, _tmp_1.toLong())
        statement.bindLong(8, entity.uploadBytes)
        statement.bindLong(9, entity.downloadBytes)
        statement.bindLong(10, entity.totalQuotaBytes)
        statement.bindLong(11, entity.expireEpochSec)
        statement.bindText(12, entity.supportUrl)
        statement.bindLong(13, entity.profileCount.toLong())
        statement.bindLong(14, entity.lastSyncedEpochMs)
      }
    }
  }

  public override suspend fun upsert(entity: SubscriptionEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfSubscriptionEntity.insert(_connection, entity)
  }

  public override suspend fun upsertAll(entities: List<SubscriptionEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfSubscriptionEntity.insert(_connection, entities)
  }

  public override fun observeAll(): Flow<List<SubscriptionEntity>> {
    val _sql: String = "SELECT * FROM subscriptions ORDER BY name ASC"
    return createFlow(__db, false, arrayOf("subscriptions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfEncryptedUrl: Int = getColumnIndexOrThrow(_stmt, "encryptedUrl")
        val _columnIndexOfUserAgentName: Int = getColumnIndexOrThrow(_stmt, "userAgentName")
        val _columnIndexOfAutoUpdateEnabled: Int = getColumnIndexOrThrow(_stmt, "autoUpdateEnabled")
        val _columnIndexOfUpdateIntervalHours: Int = getColumnIndexOrThrow(_stmt, "updateIntervalHours")
        val _columnIndexOfSmartAutoSelect: Int = getColumnIndexOrThrow(_stmt, "smartAutoSelect")
        val _columnIndexOfUploadBytes: Int = getColumnIndexOrThrow(_stmt, "uploadBytes")
        val _columnIndexOfDownloadBytes: Int = getColumnIndexOrThrow(_stmt, "downloadBytes")
        val _columnIndexOfTotalQuotaBytes: Int = getColumnIndexOrThrow(_stmt, "totalQuotaBytes")
        val _columnIndexOfExpireEpochSec: Int = getColumnIndexOrThrow(_stmt, "expireEpochSec")
        val _columnIndexOfSupportUrl: Int = getColumnIndexOrThrow(_stmt, "supportUrl")
        val _columnIndexOfProfileCount: Int = getColumnIndexOrThrow(_stmt, "profileCount")
        val _columnIndexOfLastSyncedEpochMs: Int = getColumnIndexOrThrow(_stmt, "lastSyncedEpochMs")
        val _result: MutableList<SubscriptionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubscriptionEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpEncryptedUrl: String
          _tmpEncryptedUrl = _stmt.getText(_columnIndexOfEncryptedUrl)
          val _tmpUserAgentName: String
          _tmpUserAgentName = _stmt.getText(_columnIndexOfUserAgentName)
          val _tmpAutoUpdateEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfAutoUpdateEnabled).toInt()
          _tmpAutoUpdateEnabled = _tmp != 0
          val _tmpUpdateIntervalHours: Int
          _tmpUpdateIntervalHours = _stmt.getLong(_columnIndexOfUpdateIntervalHours).toInt()
          val _tmpSmartAutoSelect: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfSmartAutoSelect).toInt()
          _tmpSmartAutoSelect = _tmp_1 != 0
          val _tmpUploadBytes: Long
          _tmpUploadBytes = _stmt.getLong(_columnIndexOfUploadBytes)
          val _tmpDownloadBytes: Long
          _tmpDownloadBytes = _stmt.getLong(_columnIndexOfDownloadBytes)
          val _tmpTotalQuotaBytes: Long
          _tmpTotalQuotaBytes = _stmt.getLong(_columnIndexOfTotalQuotaBytes)
          val _tmpExpireEpochSec: Long
          _tmpExpireEpochSec = _stmt.getLong(_columnIndexOfExpireEpochSec)
          val _tmpSupportUrl: String
          _tmpSupportUrl = _stmt.getText(_columnIndexOfSupportUrl)
          val _tmpProfileCount: Int
          _tmpProfileCount = _stmt.getLong(_columnIndexOfProfileCount).toInt()
          val _tmpLastSyncedEpochMs: Long
          _tmpLastSyncedEpochMs = _stmt.getLong(_columnIndexOfLastSyncedEpochMs)
          _item = SubscriptionEntity(_tmpId,_tmpName,_tmpEncryptedUrl,_tmpUserAgentName,_tmpAutoUpdateEnabled,_tmpUpdateIntervalHours,_tmpSmartAutoSelect,_tmpUploadBytes,_tmpDownloadBytes,_tmpTotalQuotaBytes,_tmpExpireEpochSec,_tmpSupportUrl,_tmpProfileCount,_tmpLastSyncedEpochMs)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<SubscriptionEntity> {
    val _sql: String = "SELECT * FROM subscriptions"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfEncryptedUrl: Int = getColumnIndexOrThrow(_stmt, "encryptedUrl")
        val _columnIndexOfUserAgentName: Int = getColumnIndexOrThrow(_stmt, "userAgentName")
        val _columnIndexOfAutoUpdateEnabled: Int = getColumnIndexOrThrow(_stmt, "autoUpdateEnabled")
        val _columnIndexOfUpdateIntervalHours: Int = getColumnIndexOrThrow(_stmt, "updateIntervalHours")
        val _columnIndexOfSmartAutoSelect: Int = getColumnIndexOrThrow(_stmt, "smartAutoSelect")
        val _columnIndexOfUploadBytes: Int = getColumnIndexOrThrow(_stmt, "uploadBytes")
        val _columnIndexOfDownloadBytes: Int = getColumnIndexOrThrow(_stmt, "downloadBytes")
        val _columnIndexOfTotalQuotaBytes: Int = getColumnIndexOrThrow(_stmt, "totalQuotaBytes")
        val _columnIndexOfExpireEpochSec: Int = getColumnIndexOrThrow(_stmt, "expireEpochSec")
        val _columnIndexOfSupportUrl: Int = getColumnIndexOrThrow(_stmt, "supportUrl")
        val _columnIndexOfProfileCount: Int = getColumnIndexOrThrow(_stmt, "profileCount")
        val _columnIndexOfLastSyncedEpochMs: Int = getColumnIndexOrThrow(_stmt, "lastSyncedEpochMs")
        val _result: MutableList<SubscriptionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubscriptionEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpEncryptedUrl: String
          _tmpEncryptedUrl = _stmt.getText(_columnIndexOfEncryptedUrl)
          val _tmpUserAgentName: String
          _tmpUserAgentName = _stmt.getText(_columnIndexOfUserAgentName)
          val _tmpAutoUpdateEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfAutoUpdateEnabled).toInt()
          _tmpAutoUpdateEnabled = _tmp != 0
          val _tmpUpdateIntervalHours: Int
          _tmpUpdateIntervalHours = _stmt.getLong(_columnIndexOfUpdateIntervalHours).toInt()
          val _tmpSmartAutoSelect: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfSmartAutoSelect).toInt()
          _tmpSmartAutoSelect = _tmp_1 != 0
          val _tmpUploadBytes: Long
          _tmpUploadBytes = _stmt.getLong(_columnIndexOfUploadBytes)
          val _tmpDownloadBytes: Long
          _tmpDownloadBytes = _stmt.getLong(_columnIndexOfDownloadBytes)
          val _tmpTotalQuotaBytes: Long
          _tmpTotalQuotaBytes = _stmt.getLong(_columnIndexOfTotalQuotaBytes)
          val _tmpExpireEpochSec: Long
          _tmpExpireEpochSec = _stmt.getLong(_columnIndexOfExpireEpochSec)
          val _tmpSupportUrl: String
          _tmpSupportUrl = _stmt.getText(_columnIndexOfSupportUrl)
          val _tmpProfileCount: Int
          _tmpProfileCount = _stmt.getLong(_columnIndexOfProfileCount).toInt()
          val _tmpLastSyncedEpochMs: Long
          _tmpLastSyncedEpochMs = _stmt.getLong(_columnIndexOfLastSyncedEpochMs)
          _item = SubscriptionEntity(_tmpId,_tmpName,_tmpEncryptedUrl,_tmpUserAgentName,_tmpAutoUpdateEnabled,_tmpUpdateIntervalHours,_tmpSmartAutoSelect,_tmpUploadBytes,_tmpDownloadBytes,_tmpTotalQuotaBytes,_tmpExpireEpochSec,_tmpSupportUrl,_tmpProfileCount,_tmpLastSyncedEpochMs)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: String): SubscriptionEntity? {
    val _sql: String = "SELECT * FROM subscriptions WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfEncryptedUrl: Int = getColumnIndexOrThrow(_stmt, "encryptedUrl")
        val _columnIndexOfUserAgentName: Int = getColumnIndexOrThrow(_stmt, "userAgentName")
        val _columnIndexOfAutoUpdateEnabled: Int = getColumnIndexOrThrow(_stmt, "autoUpdateEnabled")
        val _columnIndexOfUpdateIntervalHours: Int = getColumnIndexOrThrow(_stmt, "updateIntervalHours")
        val _columnIndexOfSmartAutoSelect: Int = getColumnIndexOrThrow(_stmt, "smartAutoSelect")
        val _columnIndexOfUploadBytes: Int = getColumnIndexOrThrow(_stmt, "uploadBytes")
        val _columnIndexOfDownloadBytes: Int = getColumnIndexOrThrow(_stmt, "downloadBytes")
        val _columnIndexOfTotalQuotaBytes: Int = getColumnIndexOrThrow(_stmt, "totalQuotaBytes")
        val _columnIndexOfExpireEpochSec: Int = getColumnIndexOrThrow(_stmt, "expireEpochSec")
        val _columnIndexOfSupportUrl: Int = getColumnIndexOrThrow(_stmt, "supportUrl")
        val _columnIndexOfProfileCount: Int = getColumnIndexOrThrow(_stmt, "profileCount")
        val _columnIndexOfLastSyncedEpochMs: Int = getColumnIndexOrThrow(_stmt, "lastSyncedEpochMs")
        val _result: SubscriptionEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpEncryptedUrl: String
          _tmpEncryptedUrl = _stmt.getText(_columnIndexOfEncryptedUrl)
          val _tmpUserAgentName: String
          _tmpUserAgentName = _stmt.getText(_columnIndexOfUserAgentName)
          val _tmpAutoUpdateEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfAutoUpdateEnabled).toInt()
          _tmpAutoUpdateEnabled = _tmp != 0
          val _tmpUpdateIntervalHours: Int
          _tmpUpdateIntervalHours = _stmt.getLong(_columnIndexOfUpdateIntervalHours).toInt()
          val _tmpSmartAutoSelect: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfSmartAutoSelect).toInt()
          _tmpSmartAutoSelect = _tmp_1 != 0
          val _tmpUploadBytes: Long
          _tmpUploadBytes = _stmt.getLong(_columnIndexOfUploadBytes)
          val _tmpDownloadBytes: Long
          _tmpDownloadBytes = _stmt.getLong(_columnIndexOfDownloadBytes)
          val _tmpTotalQuotaBytes: Long
          _tmpTotalQuotaBytes = _stmt.getLong(_columnIndexOfTotalQuotaBytes)
          val _tmpExpireEpochSec: Long
          _tmpExpireEpochSec = _stmt.getLong(_columnIndexOfExpireEpochSec)
          val _tmpSupportUrl: String
          _tmpSupportUrl = _stmt.getText(_columnIndexOfSupportUrl)
          val _tmpProfileCount: Int
          _tmpProfileCount = _stmt.getLong(_columnIndexOfProfileCount).toInt()
          val _tmpLastSyncedEpochMs: Long
          _tmpLastSyncedEpochMs = _stmt.getLong(_columnIndexOfLastSyncedEpochMs)
          _result = SubscriptionEntity(_tmpId,_tmpName,_tmpEncryptedUrl,_tmpUserAgentName,_tmpAutoUpdateEnabled,_tmpUpdateIntervalHours,_tmpSmartAutoSelect,_tmpUploadBytes,_tmpDownloadBytes,_tmpTotalQuotaBytes,_tmpExpireEpochSec,_tmpSupportUrl,_tmpProfileCount,_tmpLastSyncedEpochMs)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: String) {
    val _sql: String = "DELETE FROM subscriptions WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM subscriptions"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
