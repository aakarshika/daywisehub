package com.example.todoapp.db.data.mood

import com.example.todoapp.db.models.MyDate

/** A mood picked on a day: its icon name ("happy") and the date. */
data class DayMood(val icon: String, val date: MyDate)
