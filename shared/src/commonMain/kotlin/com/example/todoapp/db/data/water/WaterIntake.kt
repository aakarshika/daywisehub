package com.example.todoapp.db.data.water

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.todoapp.db.models.MyDate

@Entity(tableName = "water_intake")
data class WaterIntake(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "water_intake_id") val waterIntakeId: Long = 0,
    @ColumnInfo(name = "water_intake_user_id") val waterIntakeUserId: Long = 1,
    @ColumnInfo(name = "water_intake_progress") val waterIntakeProgress: Double = 0.0,
    @ColumnInfo(name = "notes") val notes: String="",
    @ColumnInfo(name = "water_date") val waterDate: MyDate

)
