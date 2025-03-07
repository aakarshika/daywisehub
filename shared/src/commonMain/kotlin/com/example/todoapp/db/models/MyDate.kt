package com.example.todoapp.db.models

import co.touchlab.kermit.Logger
import com.kizitonwose.calendar.core.minusDays
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusDays
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

data class MyDate(
    val dateString: String // yyyy-mm-dd
) {
    companion object {

        fun daysBetween(date1: MyDate, date2: MyDate): Int {
            val x = date1.toLocalDate().daysUntil(date2.toLocalDate())
            Logger.e("daysBetween $date1, $date2 = $x")
            return x
        }
        fun now(): MyDate {
            val d = LocalDate.now()
            return MyDate(dateString = "${d.year}-${d.monthNumber}-${d.dayOfMonth}")
        }
        //locale date to mydate
        fun fromLocalDate(date: LocalDate): MyDate {
            return MyDate(dateString = "${date.year}-${date.monthNumber}-${date.dayOfMonth}")
        }

    }
    fun addDays(days: Int): MyDate {
        return MyDate(dateString = fromLocalDate(toLocalDate().plusDays(days)).dateString)
    }
    fun subtractDays(days: Int): MyDate {
        return MyDate(dateString = fromLocalDate(toLocalDate().minusDays(days)).dateString)
    }
    fun toLocalDate(): LocalDate {
        val parts = dateString.split("-").map { it.toInt() }
        val year = parts[0]
        val month = parts[1]
        val day = parts[2]
        return LocalDate(year, month, day)
    }
}