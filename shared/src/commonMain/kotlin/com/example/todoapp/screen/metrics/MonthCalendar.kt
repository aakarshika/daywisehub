package com.example.todoapp.screen.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import com.example.todoapp.screen.basicutils.CalendarTint
import com.example.todoapp.screen.basicutils.CalendarTitleInk
import com.example.todoapp.screen.basicutils.CrownGold
import com.example.todoapp.screen.basicutils.DayRingEdge
import com.example.todoapp.screen.basicutils.EmptyDayRing
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.NoteInk
import com.example.todoapp.screen.basicutils.SelectedWeekTint
import com.example.todoapp.screen.basicutils.TodayOutline
import com.example.todoapp.screen.basicutils.pillarStrong
import com.example.todoapp.screen.metrics.badges.BadgeSticker
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.crown_a

private val CrownColumn = 20.dp
private val weekLetters = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Spec 9: the Monday-first month. Each day's ring is split by points per pillar, with white for what
 * didn't happen, and each balanced week has a crown. The week on screen is tinted, with flat tints
 * instead of the old gradient (spec 9.1). A day with badges wears its rarest as a sticker (spec 9.3).
 * Tapping a day hands it to [onDay].
 */
@Composable
fun MonthCalendarCard(calendar: MonthCalendar, onDay: (CalendarDay) -> Unit) {
    Column(
        // The same inset as the highlights card.
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .background(CalendarTint, RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 10.dp)
    ) {
        Text(
            calendar.title, fontSize = 13.sp, letterSpacing = 1.sp, color = CalendarTitleInk, textAlign = TextAlign.Center,
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
                            painterResource(Res.drawable.crown_a), contentDescription = week.crownDescription,
                            colorFilter = ColorFilter.tint(CrownGold), modifier = Modifier.size(16.dp)
                        )
                    }
                }
                week.days.forEach { day -> DayCell(day, Modifier.weight(1f)) { onDay(day) } }
            }
        }
        Text(
            "Ring slices are sized by points. White is what didn't happen.",
            fontSize = 12.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
    }
}

/**
 * A tap target at least 44dp tall with the day's ring and number. Today has an outline as well as a bold
 * number. The day's rarest badge sits on the ring's lower right like an avatar's badge, clear of the number;
 * the day's description names every badge.
 */
@Composable
private fun DayCell(day: CalendarDay, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClickLabel = if (day.badges.isEmpty()) "Open in the diary" else "Show badges", onClick = onClick)
            .semantics { contentDescription = day.description }
            .then(if (day.isToday) Modifier.border(1.5.dp, TodayOutline, RoundedCornerShape(10.dp)) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (day.hasRing) DayRing(day)
        Text(
            "${day.date.dayOfMonth}",
            fontSize = 13.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.inMonth) NoteInk else InkGray.copy(alpha = 0.6f),
            modifier = Modifier.clearAndSetSemantics {}
        )
        day.badges.firstOrNull()?.let { BadgeSticker(it.badge, day.date, 20.dp, Modifier.offset(11.dp, 11.dp)) }
    }
}

/**
 * Slices in pillar order from the top, sized by points, then white for what didn't happen, between two
 * thin edges, so a day with nothing done still reads as a ring. A day with nothing on it is one plain line.
 */
@Composable
private fun DayRing(day: CalendarDay) {
    Canvas(modifier = Modifier.size(38.dp)) {
        val stroke = 6.dp.toPx()
        val edge = 1.dp.toPx()
        val outer = size.minDimension / 2
        val band = outer - edge - stroke / 2
        val total = day.slices.sumOf { it.points } + day.notDone
        if (total == 0) {
            drawCircle(EmptyDayRing, radius = band, style = Stroke(1.5.dp.toPx()))
            return@Canvas
        }
        val topLeft = Offset(center.x - band, center.y - band)
        val arc = Size(band * 2, band * 2)
        var start = -90f
        day.slices.forEach { slice ->
            val sweep = 360f * slice.points / total
            drawArc(pillarStrong(slice.pillar), start, sweep, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke))
            start += sweep
        }
        if (day.notDone > 0) {
            drawArc(Color.White, start, 360f * day.notDone / total, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke))
        }
        drawCircle(DayRingEdge, radius = outer - edge / 2, style = Stroke(edge))
        drawCircle(DayRingEdge, radius = band - stroke / 2 - edge / 2, style = Stroke(edge))
    }
}
