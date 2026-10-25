package com.example.todoapp.db.data.week

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One finished week (spec 5.1), stored the first time the diary opens in a later week.
 * [weekStart] is its Monday, yyyy-MM-dd. Earned points add up the week's tasks; targets are
 * computed at close-out, so a pace change accepted later doesn't rewrite an old week.
 * Read the pillar columns through domain/WeekClose.kt.
 */
@Entity(tableName = "week_summary")
data class WeekSummary(
    @PrimaryKey @ColumnInfo(name = "summary_week_start") val weekStart: String,
    @ColumnInfo(name = "summary_health_earned") val healthEarned: Int,
    @ColumnInfo(name = "summary_health_target") val healthTarget: Int,
    @ColumnInfo(name = "summary_wealth_earned") val wealthEarned: Int,
    @ColumnInfo(name = "summary_wealth_target") val wealthTarget: Int,
    @ColumnInfo(name = "summary_love_earned") val loveEarned: Int,
    @ColumnInfo(name = "summary_love_target") val loveTarget: Int,
    @ColumnInfo(name = "summary_life_earned") val lifeEarned: Int,
    @ColumnInfo(name = "summary_life_target") val lifeTarget: Int,
    /** Every pillar full (spec 5): the week gets a crown. */
    @ColumnInfo(name = "summary_balanced") val balanced: Boolean,
)
