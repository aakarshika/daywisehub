package com.example.todoapp.db.data.week

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoapp.db.data.mission.Mission

/**
 * A pace change offered on a week's first diary day (spec 5.2). [type] and [response] store
 * domain.OfferType and domain.OfferResponse names; [response] stays null until it's answered,
 * or until an edit to the mission's pace withdraws the offer.
 * [week] and [snoozeUntil] are Mondays, yyyy-MM-dd. "Not yet" sets [snoozeUntil].
 * A mission gets at most one offer a week; the unique index holds that.
 */
@Entity(
    tableName = "pace_offer",
    foreignKeys = [ForeignKey(
        entity = Mission::class,
        parentColumns = ["mission_id"],
        childColumns = ["offer_mission_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["offer_mission_id"]), Index(value = ["offer_week", "offer_mission_id"], unique = true)]
)
data class PaceOffer(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "offer_id") val offerId: Long = 0,
    @ColumnInfo(name = "offer_mission_id") val missionId: Long,
    @ColumnInfo(name = "offer_type") val type: String,
    @ColumnInfo(name = "offer_week") val week: String,
    @ColumnInfo(name = "offer_response") val response: String? = null,
    @ColumnInfo(name = "offer_snooze_until") val snoozeUntil: String? = null,
)
