package com.example.todoapp.db.data.mission

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.todoapp.db.models.MyDate

/**
 * One thing to make room for. Enum-backed columns store the enum name; read them through
 * the typed accessors in domain/MissionRules.kt.
 *
 * How it repeats depends on [kind]:
 * - HABIT, FINISH_LINE, RECHARGE: [count] per [period] (DAY means every day). [goalCount] is the pace a gentle start steps up to.
 * - RHYTHM: once every [intervalCount] [intervalUnit].
 * - BLOCK: on [weekDays] (ISO numbers, Monday = 1) from [startTime] to [endTime].
 * On habits, [weekDays] holds optional preferred days.
 */
@Entity(tableName = "mission")
data class Mission(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "mission_id") val missionId: Long = 0,
    @ColumnInfo(name = "mission_title") val title: String,
    @ColumnInfo(name = "mission_pillar") val pillar: String,
    @ColumnInfo(name = "mission_kind") val kind: String,
    @ColumnInfo(name = "mission_weight") val weight: Int,
    @ColumnInfo(name = "mission_minutes") val durationMinutes: Int,
    @ColumnInfo(name = "mission_effort") val effort: String,
    @ColumnInfo(name = "mission_if_missed") val ifMissed: String,
    @ColumnInfo(name = "mission_count") val count: Int = 1,
    @ColumnInfo(name = "mission_period") val period: String = "WEEK",
    @ColumnInfo(name = "mission_goal_count") val goalCount: Int? = null,
    @ColumnInfo(name = "mission_interval_count") val intervalCount: Int = 0,
    @ColumnInfo(name = "mission_interval_unit") val intervalUnit: String = "WEEK",
    @ColumnInfo(name = "mission_week_days") val weekDays: String = "",
    @ColumnInfo(name = "mission_start_time") val startTime: String? = null,
    @ColumnInfo(name = "mission_end_time") val endTime: String? = null,
    @ColumnInfo(name = "mission_start_date") val startDate: MyDate,
    @ColumnInfo(name = "mission_end_date") val endDate: MyDate? = null,
    @ColumnInfo(name = "mission_paused_until") val pausedUntil: MyDate? = null,
    @ColumnInfo(name = "mission_motivation") val motivation: String? = null,
    @ColumnInfo(name = "mission_active") val active: Boolean = true,
)
