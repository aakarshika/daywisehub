package com.example.todoapp.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Weeks run Monday to Sunday. */
object WeekMath {
    fun weekStart(date: LocalDate): LocalDate = date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    fun weekEnd(date: LocalDate): LocalDate = weekStart(date).plus(6, DateTimeUnit.DAY)

    /** Monday → 7, Sunday → 1. */
    fun daysLeftIncludingToday(date: LocalDate): Int = 8 - date.dayOfWeek.isoDayNumber
}
