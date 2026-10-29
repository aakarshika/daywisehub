package com.example.todoapp.db.data.factor

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.example.todoapp.db.models.MyDate

/** Something outside the user's control that shaped a day, by its icon name ("guests"). A row means it's picked. */
@Entity(tableName = "day_factor", primaryKeys = ["df_date", "df_icon"])
data class DayFactor(
    @ColumnInfo(name = "df_date") val date: MyDate,
    @ColumnInfo(name = "df_icon") val icon: String
)

/** The fixed list of day factors, in display order: icon name to short label. */
object DayFactors {
    val all = listOf(
        "guests" to "Company",
        "weather" to "Weather",
        "work" to "Work",
        "errand" to "Errand",
        "travel" to "Travel",
        "family" to "Family",
        "home" to "Home"
    )
}
