package com.example.todoapp.db.data.plan

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Today's planning state. A row exists once the day's list has been built. Dates are yyyy-MM-dd. */
@Entity(tableName = "day_plan")
data class DayPlan(
    @PrimaryKey @ColumnInfo(name = "plan_date") val date: String,
    @ColumnInfo(name = "plan_free_minutes") val freeMinutes: Int,
    @ColumnInfo(name = "plan_energy") val energy: String = "NORMAL",
    @ColumnInfo(name = "plan_confirmed") val confirmed: Boolean = false,
    @ColumnInfo(name = "plan_fixed_ticked") val fixedTicked: Boolean = false,
    @ColumnInfo(name = "plan_didnt_fit_mission") val didntFitMissionId: Long? = null,
    @ColumnInfo(name = "plan_didnt_fit_due") val didntFitDue: String? = null,
    @ColumnInfo(name = "plan_didnt_fit_room") val didntFitRoomDay: String? = null,
)
