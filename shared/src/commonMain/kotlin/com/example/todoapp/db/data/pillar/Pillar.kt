package com.example.todoapp.db.data.pillar

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pillar")
data class Pillar(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "pillar_id") val pillarId: Long = 0,
    @ColumnInfo(name = "pillar_user_id") val pillarUserId: Long,
    @ColumnInfo(name = "pillar_description") val pillarDescription: String?,
    @ColumnInfo(name = "pillar_benefits") val pillarBenefits: String?,
    @ColumnInfo(name = "pillar_name") val pillarName: String?,
    @ColumnInfo(name = "pillar_ui_data") val uiData: String?,
    @ColumnInfo(name = "pillar_frequency") val pillarFrequency: Int,
    @ColumnInfo(name = "pillar_frequency_period") val pillarFrequencyPeriod: String?,
    @ColumnInfo(name = "pillar_frequency_unit") val pillarFrequencyUnit: String?,
    @ColumnInfo(name = "pillar_relative_importance") val pillarRelativeImportance: Int?
)
