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
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.Layout
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
import com.example.todoapp.domain.Badge
import com.example.todoapp.domain.CalendarDay
import com.example.todoapp.domain.MonthCalendar
import com.example.todoapp.screen.basicutils.CalendarTint
import com.example.todoapp.screen.basicutils.CalendarTitleInk
import com.example.todoapp.screen.basicutils.CrownGold
import com.example.todoapp.screen.basicutils.MilestoneFlag
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.NoteInk
import com.example.todoapp.screen.basicutils.SelectedWeekTint
import com.example.todoapp.screen.basicutils.TodayOutline
import com.example.todoapp.screen.basicutils.pillarLight
import com.example.todoapp.screen.basicutils.pillarStrong
import com.example.todoapp.screen.diary.plan.SmallChip
import com.example.todoapp.screen.metrics.badges.BadgeSticker
import com.example.todoapp.screen.metrics.badges.badgeLines
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.crown_a

private val CrownColumn = 20.dp
private val weekLetters = listOf("M", "T", "W", "T", "F", "S", "S")

/** Under the Metrics calendar, and once on All months while no filter is on. */
const val POINTS_CAPTION = "Ring slices are sized by points. White is what didn't happen, or a light shade once the day was close enough."

/** Once on All months while a filter is on (spec 9.4). */
const val ITEMS_CAPTION = "Slices are the counted items. White is what didn't happen."

/**
 * Spec 9: the Monday-first month. Each day's ring is split by points per pillar, with white for what
 * didn't happen, or each pillar's light color once a finished day was close enough (spec 9.2), and each
 * balanced week has a crown. With a filter on (spec 9.4) rings count items instead, without crowns. The
 * selected week is tinted, with flat tints instead of the old gradient (spec 9.1). A day with badges
 * wears its rarest as a sticker (spec 9.3). Tapping a day hands it to [onDay]. With [onAllMonths], the
 * title row has an "All months" chip, and tapping the title does the same. [caption] goes under the
 * days; null leaves it out.
 */
@Composable
fun MonthCalendarCard(
    calendar: MonthCalendar,
    onDay: (CalendarDay) -> Unit,
    caption: String? = null,
    onAllMonths: (() -> Unit)? = null,
) {
    Column(
        // The same inset as the rings.
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .background(CalendarTint, RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 10.dp)
    ) {
        if (onAllMonths == null) {
            MonthTitle(calendar.title, Modifier.fillMaxWidth())
        } else {
            CentredBeside(
                title = {
                    Box(
                        modifier = Modifier.heightIn(min = 44.dp)
                            .clickable(role = Role.Button, onClickLabel = "Open all months", onClick = onAllMonths),
                        contentAlignment = Alignment.Center
                    ) {
                        MonthTitle(calendar.title, Modifier)
                    }
                },
                end = { SmallChip("All months", onClick = onAllMonths) },
                modifier = Modifier.fillMaxWidth()
            )
        }
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
        if (caption != null) CalendarCaption(caption, Modifier.fillMaxWidth().padding(top = 6.dp))
    }
}

/**
 * [title] centred across the full width, with [end] at the end. The title gets what's left between [end]
 * and the same width on the other side, so it stays centred and, at large text sizes, wraps instead of
 * running under [end].
 */
@Composable
private fun CentredBeside(title: @Composable () -> Unit, end: @Composable () -> Unit, modifier: Modifier) {
    Layout(contents = listOf(title, end), modifier = modifier) { (titles, ends), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val endPlaceable = ends.first().measure(loose)
        val side = endPlaceable.width
        val middle = (constraints.maxWidth - 2 * side).coerceAtLeast(0)
        val titlePlaceable = titles.first().measure(loose.copy(minWidth = middle, maxWidth = middle))
        val height = maxOf(titlePlaceable.height, endPlaceable.height).coerceAtLeast(constraints.minHeight)
        layout(constraints.maxWidth, height) {
            titlePlaceable.place(side, (height - titlePlaceable.height) / 2)
            endPlaceable.place(constraints.maxWidth - side, (height - endPlaceable.height) / 2)
        }
    }
}

/** "OCTOBER 2026", a heading. */
@Composable
private fun MonthTitle(title: String, modifier: Modifier) {
    Text(
        title, fontSize = 13.sp, letterSpacing = 1.sp, color = CalendarTitleInk, textAlign = TextAlign.Center,
        modifier = modifier.semantics { heading() }
    )
}

/** What the rings' slices and white mean, in small italics. */
@Composable
fun CalendarCaption(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.Center, modifier = modifier)
}

/**
 * A tap target at least 44dp tall with the day's ring and number. Today has an outline as well as a bold
 * number. The day's rarest badge sits in the ring's middle as a 26dp sticker, over the number, which then
 * doesn't show. "Recharging day" stays a 20dp sticker on the ring's lower right, so a quiet day keeps its
 * number (spec 9.3). A day from the month before or after keeps its faded number too, with any badge as
 * the small corner sticker, faded the same. The day's description names the date and every badge.
 */
@Composable
private fun DayCell(day: CalendarDay, modifier: Modifier, onClick: () -> Unit) {
    val top = day.badges.firstOrNull()?.badge
    // In the corner, clear of the number, rather than over it.
    val corner = top == Badge.RECHARGING_DAY || !day.inMonth
    Box(
        modifier = modifier.heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClickLabel = if (day.badges.isEmpty()) "Open in the diary" else "Show badges", onClick = onClick)
            .semantics { contentDescription = day.description }
            .then(if (day.isToday) Modifier.border(1.5.dp, TodayOutline, RoundedCornerShape(10.dp)) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (day.hasRing) DayRing(day)
        if (top == null || corner) {
            Text(
                "${day.date.dayOfMonth}",
                fontSize = 15.sp,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (day.inMonth) NoteInk else InkGray.copy(alpha = 0.6f),
                modifier = Modifier.clearAndSetSemantics {}
            )
        }
        when {
            top == null -> Unit
            corner -> BadgeSticker(
                top, day.date, 22.dp,
                Modifier.offset(11.dp, 11.dp).then(if (day.inMonth) Modifier else Modifier.alpha(0.6f)),
                byRarity = false
            )
            // The rarer ones spill over the ring.
            else -> BadgeSticker(top, day.date, 46.dp)
        }
        // An open milestone due this day: a small red flag in the top corner. Done, it's the day's badge instead.
        if (day.milestoneDue) {
            Icon(
                badgeLines(Badge.MILESTONE), contentDescription = null, tint = MilestoneFlag,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp).size(18.dp)
            )
        }
    }
}

/**
 * The old app's ring: a rounded square outside with a round hole inside, so the band is thin at the sides
 * and full at the corners, with no outline. Slices in pillar order from the top, sized by points (or by
 * items, with a filter on), then white for what didn't happen. A closed day draws what didn't happen as
 * each pillar's light slice instead, in pillar order, so it reads as done. A day with nothing on it has no ring.
 */
@Composable
private fun DayRing(day: CalendarDay) {
    Canvas(modifier = Modifier.size(38.dp)) {
        val total = day.slices.sumOf { it.points } + day.notDone
        if (total == 0) return@Canvas
        val hole = size.minDimension / 2 - 4.dp.toPx()
        val band = Path().apply {
            addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(12.dp.toPx())))
            addOval(Rect(center, hole))
            fillType = PathFillType.EvenOdd
        }
        // Pie slices big enough to reach the corners, cut to the band.
        val reach = size.maxDimension
        val topLeft = Offset(center.x - reach, center.y - reach)
        val pie = Size(reach * 2, reach * 2)
        clipPath(band) {
            var start = -90f
            fun slice(color: Color, points: Int) {
                val sweep = 360f * points / total
                drawArc(color, start, sweep, useCenter = true, topLeft = topLeft, size = pie)
                start += sweep
            }
            day.slices.forEach { slice(pillarStrong(it.pillar), it.points) }
            if (day.closed) day.missed.forEach { slice(pillarLight(it.pillar), it.points) }
            else if (day.notDone > 0) slice(Color.White, day.notDone)
        }
    }
}
