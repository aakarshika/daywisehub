package com.example.todoapp.db.models

import com.kizitonwose.calendar.core.now
import kotlinx.datetime.LocalDate

data class MyDate(
    val dateString: String,
    private val date: LocalDate? = null
) {
    companion object {

        fun daysBetween(date1: MyDate, date2: MyDate): Int {
            return 4
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
}