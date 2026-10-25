package com.example.todoapp.domain

import kotlinx.datetime.LocalDate

private val shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "Monday" */
fun LocalDate.weekdayName(): String = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

/** "Dec 20" */
fun LocalDate.shortLabel(): String = "${shortMonths[monthNumber - 1]} $dayOfMonth"

/** 45 → "45 min", 90 → "1½ h", 120 → "2 h", 135 → "2 h 15 min" */
fun minutesLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    minutes % 60 == 30 -> "${minutes / 60}½ h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
