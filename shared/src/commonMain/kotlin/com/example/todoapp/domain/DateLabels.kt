package com.example.todoapp.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private val shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "Monday" */
fun LocalDate.weekdayName(): String = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

/** "Dec 20" */
fun LocalDate.shortLabel(): String = "${shortMonths[monthNumber - 1]} $dayOfMonth"

/** "Oct 12", or "Oct 12, 2025" in another year than [thisWeek]'s. */
fun LocalDate.labelFrom(thisWeek: LocalDate): String = if (year == thisWeek.year) shortLabel() else "${shortLabel()}, $year"

/**
 * "This week", "Last week", "Next week" or "Week of Oct 12" for the week starting [week], with the year
 * in another year than [thisWeek]'s. The Metrics tab and the diary's week strip name weeks this way.
 */
fun weekTitle(week: LocalDate, thisWeek: LocalDate): String = when (week) {
    thisWeek -> "This week"
    thisWeek.minus(7, DateTimeUnit.DAY) -> "Last week"
    thisWeek.plus(7, DateTimeUnit.DAY) -> "Next week"
    else -> "Week of ${week.labelFrom(thisWeek)}"
}

/** 45 → "45 min", 90 → "1½ h", 120 → "2 h", 135 → "2 h 15 min" */
fun minutesLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    minutes % 60 == 30 -> "${minutes / 60}½ h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
