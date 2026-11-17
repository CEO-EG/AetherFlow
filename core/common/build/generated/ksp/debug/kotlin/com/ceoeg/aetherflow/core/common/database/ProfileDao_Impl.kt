package com.ceoeg.aetherflow.core.common.database

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performInTransactionSuspending
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ProfileDao_Impl(
  __db: RoomDatabase,
) : ProfileDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfProfileEntity: EntityInsertAdapter<ProfileEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfProfileEntity = object : EntityInsertAdapter<ProfileEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `tunnel_profiles` (`id`,`subscriptionId`,`name`,`protocolName`,`serverAddress`,`serverPort`,`regionCode`,`tcpPingMs`,`urlTestDelayMs`,`downloadSpeedMbps`,`lastTestedEpochMs`,`createdAtEpochMs`,`encryptedPayload`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ProfileEntity) {
        statement.bindText(1, entity.id)
        val _tmpSubscriptionId: String? = entity.subscriptionId
        if (_tmpSubscriptionId == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpSubscriptionId)
        }
        statement.bindText(3, entity.name)
        statement.bindText(4, entity.protocolName)
        statement.bindText(5, entity.serverAddress)
        statement.bindLong(6, entity.serverPort.toLong())
        statement.bindText(7, entity.regionCode)
        statement.bindLong(8, entity.tcpPingMs)
        statement.bindLong(9, entity.urlTestDelayMs)
        statement.bindDouble(10, entity.downloadSpeedMbps)
        statement.bindLong(11, entity.lastTestedEpochMs)
        statement.bindLong(12, entity.createdAtEpochMs)
        statement.bindText(13, entity.encryptedPayload)
      }
    }
  }

  public override suspend fun upsert(entity: ProfileEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfProfileEntity.insert(_connection, entity)
  }

  public override suspend fun upsertAll(entities: List<ProfileEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfProfileEntity.insert(_connection, entities)
  }

  public override suspend fun replaceSubscriptionProfiles(subscriptionId: String, entities: List<ProfileEntity>): Unit = performInTransactionSuspending(__db) {
    super@ProfileDao_Impl.replaceSubscriptionProfiles(subscriptionId, entities)
  }

  public override fun observeAll(): Flow<List<ProfileEntity>> {
    val _sql: String = "SELECT * FROM tunnel_profiles ORDER BY CASE WHEN urlTestDelayMs > 0 THEN urlTestDelayMs ELSE 999999 END ASC, createdAtEpochMs DESC"
    return createFlow(__db, false, arrayOf("tunnel_profiles")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSubscriptionId: Int = getColumnIndexOrThrow(_stmt, "subscriptionId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfProtocolName: Int = getColumnIndexOrThrow(_stmt, "protocolName")
        val _columnIndexOfServerAddress: Int = getColumnIndexOrThrow(_stmt, "serverAddress")
        val _columnIndexOfServerPort: Int = getColumnIndexOrThrow(_stmt, "serverPort")
        val _columnIndexOfRegionCode: Int = getColumnIndexOrThrow(_stmt, "regionCode")
        val _columnIndexOfTcpPingMs: Int = getColumnIndexOrThrow(_stmt, "tcpPingMs")
        val _columnIndexOfUrlTestDelayMs: Int = getColumnIndexOrThrow(_stmt, "urlTestDelayMs")
        val _columnIndexOfDownloadSpeedMbps: Int = getColumnIndexOrThrow(_stmt, "downloadSpeedMbps")
        val _columnIndexOfLastTestedEpochMs: Int = getColumnIndexOrThrow(_stmt, "lastTestedEpochMs")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _columnIndexOfEncryptedPayload: Int = getColumnIndexOrThrow(_stmt, "encryptedPayload")
        val _result: MutableList<ProfileEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProfileEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSubscriptionId: String?
          if (_stmt.isNull(_columnIndexOfSubscriptionId)) {
            _tmpSubscriptionId = null
          } else {
            _tmpSubscriptionId = _stmt.getText(_columnIndexOfSubscriptionId)
          }
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpProtocolName: String
          _tmpProtocolName = _stmt.getText(_columnIndexOfProtocolName)
          val _tmpServerAddress: String
          _tmpServerAddress = _stmt.getText(_columnIndexOfServerAddress)
          val _tmpServerPort: Int
          _tmpServerPort = _stmt.getLong(_columnIndexOfServerPort).toInt()
          val _tmpRegionCode: String
          _tmpRegionCode = _stmt.getText(_columnIndexOfRegionCode)
          val _tmpTcpPingMs: Long
          _tmpTcpPingMs = _stmt.getLong(_columnIndexOfTcpPingMs)
          val _tmpUrlTestDelayMs: Long
          _tmpUrlTestDelayMs = _stmt.getLong(_columnIndexOfUrlTestDelayMs)
          val _tmpDownloadSpeedMbps: Double
          _tmpDownloadSpeedMbps = _stmt.getDouble(_columnIndexOfDownloadSpeedMbps)
          val _tmpLastTestedEpochMs: Long
          _tmpLastTestedEpochMs = _stmt.getLong(_columnIndexOfLastTestedEpochMs)
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          val _tmpEncryptedPayload: String
          _tmpEncryptedPayload = _stmt.getText(_columnIndexOfEncryptedPayload)
          _item = ProfileEntity(_tmpId,_tmpSubscriptionId,_tmpName,_tmpProtocolName,_tmpServerAddress,_tmpServerPort,_tmpRegionCode,_tmpTcpPingMs,_tmpUrlTestDelayMs,_tmpDownloadSpeedMbps,_tmpLastTestedEpochMs,_tmpCreatedAtEpochMs,_tmpEncryptedPayload)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<ProfileEntity> {
    val _sql: String = "SELECT * FROM tunnel_profiles"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSubscriptionId: Int = getColumnIndexOrThrow(_stmt, "subscriptionId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfProtocolName: Int = getColumnIndexOrThrow(_stmt, "protocolName")
        val _columnIndexOfServerAddress: Int = getColumnIndexOrThrow(_stmt, "serverAddress")
        val _columnIndexOfServerPort: Int = getColumnIndexOrThrow(_stmt, "serverPort")
        val _columnIndexOfRegionCode: Int = getColumnIndexOrThrow(_stmt, "regionCode")
        val _columnIndexOfTcpPingMs: Int = getColumnIndexOrThrow(_stmt, "tcpPingMs")
        val _columnIndexOfUrlTestDelayMs: Int = getColumnIndexOrThrow(_stmt, "urlTestDelayMs")
        val _columnIndexOfDownloadSpeedMbps: Int = getColumnIndexOrThrow(_stmt, "downloadSpeedMbps")
        val _columnIndexOfLastTestedEpochMs: Int = getColumnIndexOrThrow(_stmt, "lastTestedEpochMs")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _columnIndexOfEncryptedPayload: Int = getColumnIndexOrThrow(_stmt, "encryptedPayload")
        val _result: MutableList<ProfileEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProfileEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSubscriptionId: String?
          if (_stmt.isNull(_columnIndexOfSubscriptionId)) {
            _tmpSubscriptionId = null
          } else {
            _tmpSubscriptionId = _stmt.getText(_columnIndexOfSubscriptionId)
          }
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpProtocolName: String
          _tmpProtocolName = _stmt.getText(_columnIndexOfProtocolName)
          val _tmpServerAddress: String
          _tmpServerAddress = _stmt.getText(_columnIndexOfServerAddress)
          val _tmpServerPort: Int
          _tmpServerPort = _stmt.getLong(_columnIndexOfServerPort).toInt()
          val _tmpRegionCode: String
          _tmpRegionCode = _stmt.getText(_columnIndexOfRegionCode)
          val _tmpTcpPingMs: Long
          _tmpTcpPingMs = _stmt.getLong(_columnIndexOfTcpPingMs)
          val _tmpUrlTestDelayMs: Long
          _tmpUrlTestDelayMs = _stmt.getLong(_columnIndexOfUrlTestDelayMs)
          val _tmpDownloadSpeedMbps: Double
          _tmpDownloadSpeedMbps = _stmt.getDouble(_columnIndexOfDownloadSpeedMbps)
          val _tmpLastTestedEpochMs: Long
          _tmpLastTestedEpochMs = _stmt.getLong(_columnIndexOfLastTestedEpochMs)
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          val _tmpEncryptedPayload: String
          _tmpEncryptedPayload = _stmt.getText(_columnIndexOfEncryptedPayload)
          _item = ProfileEntity(_tmpId,_tmpSubscriptionId,_tmpName,_tmpProtocolName,_tmpServerAddress,_tmpServerPort,_tmpRegionCode,_tmpTcpPingMs,_tmpUrlTestDelayMs,_tmpDownloadSpeedMbps,_tmpLastTestedEpochMs,_tmpCreatedAtEpochMs,_tmpEncryptedPayload)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getBySubscription(subscriptionId: String): List<ProfileEntity> {
    val _sql: String = "SELECT * FROM tunnel_profiles WHERE subscriptionId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, subscriptionId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSubscriptionId: Int = getColumnIndexOrThrow(_stmt, "subscriptionId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfProtocolName: Int = getColumnIndexOrThrow(_stmt, "protocolName")
        val _columnIndexOfServerAddress: Int = getColumnIndexOrThrow(_stmt, "serverAddress")
        val _columnIndexOfServerPort: Int = getColumnIndexOrThrow(_stmt, "serverPort")
        val _columnIndexOfRegionCode: Int = getColumnIndexOrThrow(_stmt, "regionCode")
        val _columnIndexOfTcpPingMs: Int = getColumnIndexOrThrow(_stmt, "tcpPingMs")
        val _columnIndexOfUrlTestDelayMs: Int = getColumnIndexOrThrow(_stmt, "urlTestDelayMs")
        val _columnIndexOfDownloadSpeedMbps: Int = getColumnIndexOrThrow(_stmt, "downloadSpeedMbps")
        val _columnIndexOfLastTestedEpochMs: Int = getColumnIndexOrThrow(_stmt, "lastTestedEpochMs")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _columnIndexOfEncryptedPayload: Int = getColumnIndexOrThrow(_stmt, "encryptedPayload")
        val _result: MutableList<ProfileEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProfileEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSubscriptionId: String?
          if (_stmt.isNull(_columnIndexOfSubscriptionId)) {
            _tmpSubscriptionId = null
          } else {
            _tmpSubscriptionId = _stmt.getText(_columnIndexOfSubscriptionId)
          }
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpProtocolName: String
          _tmpProtocolName = _stmt.getText(_columnIndexOfProtocolName)
          val _tmpServerAddress: String
          _tmpServerAddress = _stmt.getText(_columnIndexOfServerAddress)
          val _tmpServerPort: Int
          _tmpServerPort = _stmt.getLong(_columnIndexOfServerPort).toInt()
          val _tmpRegionCode: String
          _tmpRegionCode = _stmt.getText(_columnIndexOfRegionCode)
          val _tmpTcpPingMs: Long
          _tmpTcpPingMs = _stmt.getLong(_columnIndexOfTcpPingMs)
          val _tmpUrlTestDelayMs: Long
          _tmpUrlTestDelayMs = _stmt.getLong(_columnIndexOfUrlTestDelayMs)
          val _tmpDownloadSpeedMbps: Double
          _tmpDownloadSpeedMbps = _stmt.getDouble(_columnIndexOfDownloadSpeedMbps)
          val _tmpLastTestedEpochMs: Long
          _tmpLastTestedEpochMs = _stmt.getLong(_columnIndexOfLastTestedEpochMs)
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          val _tmpEncryptedPayload: String
          _tmpEncryptedPayload = _stmt.getText(_columnIndexOfEncryptedPayload)
          _item = ProfileEntity(_tmpId,_tmpSubscriptionId,_tmpName,_tmpProtocolName,_tmpServerAddress,_tmpServerPort,_tmpRegionCode,_tmpTcpPingMs,_tmpUrlTestDelayMs,_tmpDownloadSpeedMbps,_tmpLastTestedEpochMs,_tmpCreatedAtEpochMs,_tmpEncryptedPayload)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: String): ProfileEntity? {
    val _sql: String = "SELECT * FROM tunnel_profiles WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSubscriptionId: Int = getColumnIndexOrThrow(_stmt, "subscriptionId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfProtocolName: Int = getColumnIndexOrThrow(_stmt, "protocolName")
        val _columnIndexOfServerAddress: Int = getColumnIndexOrThrow(_stmt, "serverAddress")
        val _columnIndexOfServerPort: Int = getColumnIndexOrThrow(_stmt, "serverPort")
        val _columnIndexOfRegionCode: Int = getColumnIndexOrThrow(_stmt, "regionCode")
        val _columnIndexOfTcpPingMs: Int = getColumnIndexOrThrow(_stmt, "tcpPingMs")
        val _columnIndexOfUrlTestDelayMs: Int = getColumnIndexOrThrow(_stmt, "urlTestDelayMs")
        val _columnIndexOfDownloadSpeedMbps: Int = getColumnIndexOrThrow(_stmt, "downloadSpeedMbps")
        val _columnIndexOfLastTestedEpochMs: Int = getColumnIndexOrThrow(_stmt, "lastTestedEpochMs")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _columnIndexOfEncryptedPayload: Int = getColumnIndexOrThrow(_stmt, "encryptedPayload")
        val _result: ProfileEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSubscriptionId: String?
          if (_stmt.isNull(_columnIndexOfSubscriptionId)) {
            _tmpSubscriptionId = null
          } else {
            _tmpSubscriptionId = _stmt.getText(_columnIndexOfSubscriptionId)
          }
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpProtocolName: String
          _tmpProtocolName = _stmt.getText(_columnIndexOfProtocolName)
          val _tmpServerAddress: String
          _tmpServerAddress = _stmt.getText(_columnIndexOfServerAddress)
          val _tmpServerPort: Int
          _tmpServerPort = _stmt.getLong(_columnIndexOfServerPort).toInt()
          val _tmpRegionCode: String
          _tmpRegionCode = _stmt.getText(_columnIndexOfRegionCode)
          val _tmpTcpPingMs: Long
          _tmpTcpPingMs = _stmt.getLong(_columnIndexOfTcpPingMs)
          val _tmpUrlTestDelayMs: Long
          _tmpUrlTestDelayMs = _stmt.getLong(_columnIndexOfUrlTestDelayMs)
          val _tmpDownloadSpeedMbps: Double
          _tmpDownloadSpeedMbps = _stmt.getDouble(_columnIndexOfDownloadSpeedMbps)
          val _tmpLastTestedEpochMs: Long
          _tmpLastTestedEpochMs = _stmt.getLong(_columnIndexOfLastTestedEpochMs)
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          val _tmpEncryptedPayload: String
          _tmpEncryptedPayload = _stmt.getText(_columnIndexOfEncryptedPayload)
          _result = ProfileEntity(_tmpId,_tmpSubscriptionId,_tmpName,_tmpProtocolName,_tmpServerAddress,_tmpServerPort,_tmpRegionCode,_tmpTcpPingMs,_tmpUrlTestDelayMs,_tmpDownloadSpeedMbps,_tmpLastTestedEpochMs,_tmpCreatedAtEpochMs,_tmpEncryptedPayload)
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
    val _sql: String = "DELETE FROM tunnel_profiles WHERE id = ?"
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

  public override suspend fun deleteByIds(ids: List<String>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM tunnel_profiles WHERE id IN (")
    val _inputSize: Int = ids.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in ids) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteBySubscription(subscriptionId: String) {
    val _sql: String = "DELETE FROM tunnel_profiles WHERE subscriptionId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, subscriptionId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM tunnel_profiles"
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
