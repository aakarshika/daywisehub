package com.example.todoapp.db.models

import com.kizitonwose.calendar.core.minusDays
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusDays
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

data class MyDate(
    val dateString: String // yyyy-MM-dd (zero-padded ISO format)
) {
    companion object {

        fun daysBetween(date1: MyDate, date2: MyDate): Int {
            return date1.toLocalDate().daysUntil(date2.toLocalDate())
        }
        fun now(): MyDate {
            return fromLocalDate(LocalDate.now())
        }
        fun fromLocalDate(date: LocalDate): MyDate {
            val m = date.monthNumber.toString().padStart(2, '0')
            val d = date.dayOfMonth.toString().padStart(2, '0')
            return MyDate(dateString = "${date.year}-$m-$d")
        }
    }
    fun addDays(days: Int): MyDate {
        return fromLocalDate(toLocalDate().plusDays(days))
    }
    fun subtractDays(days: Int): MyDate {
        return fromLocalDate(toLocalDate().minusDays(days))
    }
    fun toLocalDate(): LocalDate {
        val parts = dateString.split("-").map { it.toInt() }
        val year = parts[0]
        val month = parts[1]
        val day = parts[2]
        return LocalDate(year, month, day)
    }
}
