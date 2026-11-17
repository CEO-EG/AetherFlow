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
public class RoutingRuleDao_Impl(
  __db: RoomDatabase,
) : RoutingRuleDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfRoutingRuleEntity: EntityInsertAdapter<RoutingRuleEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfRoutingRuleEntity = object : EntityInsertAdapter<RoutingRuleEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `routing_rules` (`id`,`name`,`enabled`,`matchTypeName`,`value`,`actionName`,`priority`,`isBuiltInPreset`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RoutingRuleEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        val _tmp: Int = if (entity.enabled) 1 else 0
        statement.bindLong(3, _tmp.toLong())
        statement.bindText(4, entity.matchTypeName)
        statement.bindText(5, entity.value)
        statement.bindText(6, entity.actionName)
        statement.bindLong(7, entity.priority.toLong())
        val _tmp_1: Int = if (entity.isBuiltInPreset) 1 else 0
        statement.bindLong(8, _tmp_1.toLong())
      }
    }
  }

  public override suspend fun upsert(entity: RoutingRuleEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfRoutingRuleEntity.insert(_connection, entity)
  }

  public override suspend fun upsertAll(entities: List<RoutingRuleEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfRoutingRuleEntity.insert(_connection, entities)
  }

  public override fun observeAll(): Flow<List<RoutingRuleEntity>> {
    val _sql: String = "SELECT * FROM routing_rules ORDER BY priority ASC, name ASC"
    return createFlow(__db, false, arrayOf("routing_rules")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfMatchTypeName: Int = getColumnIndexOrThrow(_stmt, "matchTypeName")
        val _columnIndexOfValue: Int = getColumnIndexOrThrow(_stmt, "value")
        val _columnIndexOfActionName: Int = getColumnIndexOrThrow(_stmt, "actionName")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfIsBuiltInPreset: Int = getColumnIndexOrThrow(_stmt, "isBuiltInPreset")
        val _result: MutableList<RoutingRuleEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RoutingRuleEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpMatchTypeName: String
          _tmpMatchTypeName = _stmt.getText(_columnIndexOfMatchTypeName)
          val _tmpValue: String
          _tmpValue = _stmt.getText(_columnIndexOfValue)
          val _tmpActionName: String
          _tmpActionName = _stmt.getText(_columnIndexOfActionName)
          val _tmpPriority: Int
          _tmpPriority = _stmt.getLong(_columnIndexOfPriority).toInt()
          val _tmpIsBuiltInPreset: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsBuiltInPreset).toInt()
          _tmpIsBuiltInPreset = _tmp_1 != 0
          _item = RoutingRuleEntity(_tmpId,_tmpName,_tmpEnabled,_tmpMatchTypeName,_tmpValue,_tmpActionName,_tmpPriority,_tmpIsBuiltInPreset)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<RoutingRuleEntity> {
    val _sql: String = "SELECT * FROM routing_rules ORDER BY priority ASC, name ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfMatchTypeName: Int = getColumnIndexOrThrow(_stmt, "matchTypeName")
        val _columnIndexOfValue: Int = getColumnIndexOrThrow(_stmt, "value")
        val _columnIndexOfActionName: Int = getColumnIndexOrThrow(_stmt, "actionName")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfIsBuiltInPreset: Int = getColumnIndexOrThrow(_stmt, "isBuiltInPreset")
        val _result: MutableList<RoutingRuleEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RoutingRuleEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpMatchTypeName: String
          _tmpMatchTypeName = _stmt.getText(_columnIndexOfMatchTypeName)
          val _tmpValue: String
          _tmpValue = _stmt.getText(_columnIndexOfValue)
          val _tmpActionName: String
          _tmpActionName = _stmt.getText(_columnIndexOfActionName)
          val _tmpPriority: Int
          _tmpPriority = _stmt.getLong(_columnIndexOfPriority).toInt()
          val _tmpIsBuiltInPreset: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsBuiltInPreset).toInt()
          _tmpIsBuiltInPreset = _tmp_1 != 0
          _item = RoutingRuleEntity(_tmpId,_tmpName,_tmpEnabled,_tmpMatchTypeName,_tmpValue,_tmpActionName,_tmpPriority,_tmpIsBuiltInPreset)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: String) {
    val _sql: String = "DELETE FROM routing_rules WHERE id = ?"
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
    val _sql: String = "DELETE FROM routing_rules"
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
