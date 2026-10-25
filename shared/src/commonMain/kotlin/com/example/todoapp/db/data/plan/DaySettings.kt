package com.example.todoapp.db.data.plan

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Free minutes the planner may fill on each weekday (ISO numbers, Monday = 1). */
@Entity(tableName = "day_settings")
data class DaySettings(
    @PrimaryKey @ColumnInfo(name = "settings_weekday") val weekday: Int,
    @ColumnInfo(name = "settings_free_minutes") val freeMinutes: Int,
) {
    companion object {
        fun defaultFor(weekday: Int): Int = if (weekday <= 5) 3 * 60 else 10 * 60

        val defaults: List<DaySettings> = (1..7).map { DaySettings(it, defaultFor(it)) }
    }
}
