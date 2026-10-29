package com.example.todoapp.db.backup

import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.room.PooledConnection
import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.models.MyDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** A backup file, read and checked, waiting for the user to confirm the restore. */
class Backup(val version: Int, val exportedOn: String, val tables: Map<String, JsonArray>) {
    val missionCount: Int get() = tables["mission"]?.size ?: 0
}

/**
 * Backup and restore as one JSON file: every table, row by row, as column name to value.
 * The same file works on Android and iOS, since both run the same Room schema.
 * Tables and columns are read from the database itself, so a new table or column needs no change here.
 * A backup from an older version restores (missing columns take their defaults); one from a newer
 * version is refused, since this build can't know its new columns.
 */
class BackupRepository(private val db: AppDatabase) {

    suspend fun export(): String = db.useReaderConnection { conn ->
        val version = conn.userVersion()
        val tables = buildJsonObject {
            conn.tableNames().forEach { table ->
                val types = conn.columnTypes(table)
                put(table, conn.usePrepared("SELECT * FROM `$table`") { stmt ->
                    buildJsonArray {
                        val names = stmt.getColumnNames()
                        while (stmt.step()) {
                            add(buildJsonObject {
                                names.forEachIndexed { i, name ->
                                    val value: JsonElement = when {
                                        stmt.isNull(i) -> JsonNull
                                        types[name].isInteger() -> JsonPrimitive(stmt.getLong(i))
                                        types[name].isReal() -> JsonPrimitive(stmt.getDouble(i))
                                        else -> JsonPrimitive(stmt.getText(i))
                                    }
                                    put(name, value)
                                }
                            })
                        }
                    }
                })
            }
        }
        buildJsonObject {
            put("app", APP)
            put("version", version)
            put("exportedOn", MyDate.now().dateString)
            put("tables", tables)
        }.toString()
    }

    /** Reads [text] as a backup. Throws [BackupException] with a line for the user when it isn't one. */
    suspend fun read(text: String): Backup {
        val root = try {
            Json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            throw BackupException("That file isn't an Inbalance backup.")
        }
        if (root["app"]?.jsonPrimitive?.content != APP) throw BackupException("That file isn't an Inbalance backup.")
        val version = root["version"]?.jsonPrimitive?.intOrNull ?: throw BackupException("That backup is damaged.")
        val current = db.useReaderConnection { it.userVersion() }
        if (version > current) throw BackupException("That backup is from a newer version of Inbalance. Update the app, then try again.")
        val tables = try {
            root["tables"]!!.jsonObject.mapValues { it.value.jsonArray }
        } catch (e: Exception) {
            throw BackupException("That backup is damaged.")
        }
        return Backup(version, root["exportedOn"]?.jsonPrimitive?.content ?: "", tables)
    }

    /** Replaces everything with [backup], all or none. Open screens refresh through Room's invalidation. */
    suspend fun restore(backup: Backup) {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val tables = tableNames().sortedBy { PARENTS_FIRST.indexOf(it).let { i -> if (i < 0) PARENTS_FIRST.size else i } }
                tables.reversed().forEach { table -> usePrepared("DELETE FROM `$table`") { it.step() } }
                tables.forEach { table ->
                    val rows = backup.tables[table] ?: return@forEach
                    val types = columnTypes(table)
                    rows.forEach row@{ row ->
                        val values = row.jsonObject.filterKeys { it in types }
                        if (values.isEmpty()) return@row
                        val columns = values.keys.toList()
                        val sql = "INSERT INTO `$table` (${columns.joinToString { "`$it`" }}) VALUES (${columns.joinToString { "?" }})"
                        usePrepared(sql) { stmt ->
                            columns.forEachIndexed { i, column ->
                                val value = values[column]
                                val index = i + 1
                                if (value == null || value is JsonNull) {
                                    stmt.bindNull(index)
                                } else {
                                    val p = value.jsonPrimitive
                                    when {
                                        types[column].isInteger() ->
                                            stmt.bindLong(index, p.longOrNull ?: p.booleanOrNull?.let { if (it) 1L else 0L } ?: 0L)
                                        types[column].isReal() -> stmt.bindDouble(index, p.doubleOrNull ?: 0.0)
                                        else -> stmt.bindText(index, p.content)
                                    }
                                }
                            }
                            stmt.step()
                        }
                    }
                }
            }
        }
        db.invalidationTracker.refreshAsync()
    }

    private suspend fun PooledConnection.userVersion(): Int =
        usePrepared("PRAGMA user_version") { if (it.step()) it.getLong(0).toInt() else 0 }

    /** The app's own tables: Room's bookkeeping and SQLite's are left out. */
    private suspend fun PooledConnection.tableNames(): List<String> =
        usePrepared("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name") { stmt ->
            buildList { while (stmt.step()) add(stmt.getText(0)) }
        }.filterNot { it.startsWith("sqlite_") || it.startsWith("room_") || it == "android_metadata" }

    /** Column name to declared type ("INTEGER", "REAL", "TEXT"). */
    private suspend fun PooledConnection.columnTypes(table: String): Map<String, String> =
        usePrepared("PRAGMA table_info(`$table`)") { stmt ->
            buildMap { while (stmt.step()) put(stmt.getText(1), stmt.getText(2).uppercase()) }
        }

    private fun String?.isInteger() = this != null && "INT" in this
    private fun String?.isReal() = this != null && ("REAL" in this || "FLOA" in this || "DOUB" in this)

    companion object {
        private const val APP = "Inbalance"

        /** Tables other rows point at: filled first and emptied last, so foreign keys hold. */
        private val PARENTS_FIRST = listOf("mission", "mood")
    }
}

class BackupException(message: String) : Exception(message)
