package com.example.todoapp.screen.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.CalendarDay
import com.example.todoapp.domain.MonthCalendar
import com.example.todoapp.screen.basicutils.CrownGold
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.pillarStrong
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.crown_a

// Flat tints instead of the old gradient (spec 9.1).
private val CalendarTint = Color(0xFFF4F0FA)
private val SelectedWeekTint = Color(0xFFE9E1F6)
private val EmptyRing = Color(0xFFE4DDEC)
private val DayInk = Color(0xFF2A2A2A)
private val CrownColumn = 20.dp
private val weekLetters = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Spec 9: the Monday-first month. Each day's ring is split by points per pillar, with white for what
 * didn't happen, and each balanced week has a crown. The week on screen is tinted. Tapping a day opens
 * it in the diary.
 */
@Composable
fun MonthCalendarCard(calendar: MonthCalendar, openDay: (LocalDate) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp)
            .background(CalendarTint, RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 10.dp)
    ) {
        Text(
            calendar.title, fontSize = 13.sp, letterSpacing = 1.sp, color = Color(0xFF4A4A4A), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() }
        )
        // The letters are for the eye; each day tells a screen reader its own name.
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp).clearAndSetSemantics {}) {
            Spacer(Modifier.width(CrownColumn))
            weekLetters.forEach { letter ->
                Text(letter, fontSize = 11.sp, color = InkGray, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        calendar.weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    .background(if (week.selected) SelectedWeekTint else CalendarTint, RoundedCornerShape(10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(CrownColumn), contentAlignment = Alignment.Center) {
                    if (week.crowned) {
                        Image(
                            painterResource(Res.drawable.crown_a), contentDescription = "Balanced week",
                            colorFilter = ColorFilter.tint(CrownGold), modifier = Modifier.size(16.dp)
                        )
                    }
                }
                week.days.forEach { day -> DayCell(day, Modifier.weight(1f)) { openDay(day.date) } }
            }
        }
        Text(
            "Ring slices are sized by points. White is what didn't happen.",
            fontSize = 12.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
    }
}

/** A 44dp tap target with the day's ring and number. */
@Composable
private fun DayCell(day: CalendarDay, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.height(44.dp)
            .clickable(role = Role.Button, onClickLabel = "Open in the diary", onClick = onClick)
            .semantics { contentDescription = day.description },
        contentAlignment = Alignment.Center
    ) {
        if (!day.isFuture) DayRing(day)
        Text(
            "${day.date.dayOfMonth}",
            fontSize = 13.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.inMonth) DayInk else InkGray.copy(alpha = 0.6f),
            modifier = Modifier.clearAndSetSemantics {}
        )
    }
}

/** Slices in pillar order from the top, sized by points, then white for what didn't happen. */
@Composable
private fun DayRing(day: CalendarDay) {
    Canvas(modifier = Modifier.size(38.dp)) {
        val stroke = 6.dp.toPx()
        val topLeft = Offset(stroke / 2, stroke / 2)
        val arc = Size(size.width - stroke, size.height - stroke)
        val total = day.slices.sumOf { it.points } + day.notDone
        if (total == 0) {
            drawArc(EmptyRing, 0f, 360f, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke))
            return@Canvas
        }
        var start = -90f
        day.slices.forEach { slice ->
            val sweep = 360f * slice.points / total
            drawArc(pillarStrong(slice.pillar), start, sweep, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke))
            start += sweep
        }
        if (day.notDone > 0) {
            drawArc(Color.White, start, 360f * day.notDone / total, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke))
        }
    }
}
