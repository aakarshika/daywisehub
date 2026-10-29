package com.example.todoapp.screen.diary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.factor.DayFactors
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.DayBadges
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.DayFactorRow
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodHeader
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeHeader
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.water_daily_limit
import com.example.todoapp.screen.metrics.badges.BadgeSticker
import com.example.todoapp.screen.metrics.badges.WaterGlassSticker
import kotlinx.datetime.LocalDate

private val CardEdge = Color(0xFFE7DFC6)
private val WaterInk = Color(0xFF3F6EA8)
private val FactorInk = Color(0xFF2F5E22)

/**
 * Mood, what shaped the day and water, folded into one line near the top of the page: the picked face,
 * the glasses drunk and the picked factors. Tapping it opens the three rows to change them.
 * The item's key is "check-in".
 */
fun LazyListScope.CheckInItem(
    selection: LocalDate,
    todayMoodList: List<TodayMoodWithDetails>?,
    upsertMood: (TodayMood) -> Unit,
    dayFactors: List<String>,
    onFactor: (icon: String, picked: Boolean) -> Unit,
    waterIntake: WaterIntake?,
    upsertWaterIntake: (WaterIntake) -> Unit,
    waterIntakeCompleted: (Boolean) -> Unit,
    notes: List<AnnotatedString>,
) {
    item(key = "check-in") {
        var open by rememberSaveable(selection) { mutableStateOf(false) }
        // No box: the check-in sits on the paper like the rest of the page, closed off by a hairline.
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            FoldedLine(selection, todayMoodList, dayFactors, waterIntake, open) { open = !open }
            AnimatedVisibility(open) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    MoodHeader(todayMoodList, selection, upsertMood)
                    DayFactorRow(dayFactors, onFactor)
                    WaterIntakeHeader(waterIntake?.waterIntakeProgress ?: 0.0, selection) { progress ->
                        upsertWaterIntake(
                            (waterIntake ?: WaterIntake(waterIntakeId = 0, waterIntakeProgress = progress, waterDate = MyDate.fromLocalDate(selection)))
                                .copy(waterIntakeProgress = progress)
                        )
                        waterIntakeCompleted(progress.toInt() == water_daily_limit - 1)
                    }
                    if (notes.isNotEmpty()) DayNotesList(notes)
                }
            }
            Divider()
        }
    }
}

@Composable
private fun FoldedLine(
    selection: LocalDate,
    todayMoodList: List<TodayMoodWithDetails>?,
    dayFactors: List<String>,
    waterIntake: WaterIntake?,
    open: Boolean,
    onToggle: () -> Unit,
) {
    val mood = todayMoodList.orEmpty().firstOrNull { it.todayMood?.tmMoodStatus == "ACTIVE" }?.mood?.moodIcon
    // The glass row fills glasses 0 to the stored number, so that's one more than the number.
    val glasses = ((waterIntake?.waterIntakeProgress ?: 0.0).toInt() + 1).coerceIn(0, water_daily_limit)
    val factors = DayFactors.all.filter { it.first in dayFactors }.joinToString(", ") { it.second }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClickLabel = if (open) "Fold" else "Open", onClick = onToggle)
            .semantics { stateDescription = if (open) "Open" else "Folded" }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val sticker = mood?.let { DayBadges.moodBadge(it) }
        if (sticker != null) {
            BadgeSticker(sticker, selection, 32.dp, byRarity = false)
        } else {
            Text("How's today?", fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray)
        }
        Bar()
        Row(verticalAlignment = Alignment.CenterVertically) {
            WaterGlassSticker(0, tilt = -4f, size = 22.dp)
            Spacer(Modifier.width(3.dp))
            Text("$glasses/$water_daily_limit", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WaterInk)
        }
        // With no mood and no factors, "How's today?" is the only prompt; a second one is noise.
        if (sticker == null && factors.isEmpty()) {
            Spacer(Modifier.weight(1f))
        } else {
            Bar()
            Text(
                factors.ifEmpty { "What shaped the day?" },
                fontSize = 13.sp, fontStyle = if (factors.isEmpty()) FontStyle.Italic else FontStyle.Normal,
                color = if (factors.isEmpty()) InkGray else FactorInk,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
        }
        Icon(
            Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = InkGray,
            modifier = Modifier.size(22.dp).rotate(if (open) 180f else 0f)
        )
    }
}

@Composable
private fun Bar() = Box(Modifier.width(1.dp).height(22.dp).background(CardEdge))

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(CardEdge))
