package com.example.todoapp.db

import androidx.room.TypeConverter
import com.example.todoapp.db.models.MyDate

class Converters {
    @TypeConverter
    fun fromMyDate(myDate: MyDate): String {
        return myDate.dateString
    }

    @TypeConverter
    fun toMyDate(dateString: String): MyDate {
        return MyDate(dateString)
    }
}
