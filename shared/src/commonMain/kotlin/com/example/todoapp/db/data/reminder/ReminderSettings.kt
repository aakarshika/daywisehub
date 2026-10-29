package com.example.todoapp.db.data.reminder

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Which reminders to send, and when (minutes after midnight). One row, [id] 1; no row means the defaults.
 * [blockMissionIds] are the fixed blocks that get a "starts now", comma-separated.
 */
@Entity(tableName = "reminder_settings")
data class ReminderSettings(
    @PrimaryKey @ColumnInfo(name = "reminder_id") val id: Int = 1,
    @ColumnInfo(name = "reminder_evening_on", defaultValue = "1") val eveningOn: Boolean = true,
    @ColumnInfo(name = "reminder_evening_minute", defaultValue = "1200") val eveningMinute: Int = 20 * 60,
    @ColumnInfo(name = "reminder_night_on", defaultValue = "1") val nightOn: Boolean = true,
    @ColumnInfo(name = "reminder_night_minute", defaultValue = "1320") val nightMinute: Int = 22 * 60,
    @ColumnInfo(name = "reminder_morning_on", defaultValue = "1") val morningOn: Boolean = true,
    @ColumnInfo(name = "reminder_morning_minute", defaultValue = "480") val morningMinute: Int = 8 * 60,
    @ColumnInfo(name = "reminder_week_on", defaultValue = "1") val weekOn: Boolean = true,
    @ColumnInfo(name = "reminder_block_ids", defaultValue = "") val blockMissionIds: String = "",
) {
    val blockIds: Set<Long> get() = blockMissionIds.split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()

    fun withBlock(missionId: Long, on: Boolean): ReminderSettings =
        copy(blockMissionIds = (if (on) blockIds + missionId else blockIds - missionId).sorted().joinToString(","))
}
