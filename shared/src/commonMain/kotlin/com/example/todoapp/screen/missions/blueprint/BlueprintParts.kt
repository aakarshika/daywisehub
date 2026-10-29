package com.example.todoapp.screen.missions.blueprint

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.Blueprint
import com.example.todoapp.domain.BlueprintDay
import com.example.todoapp.domain.BlueprintFilter
import com.example.todoapp.domain.BlueprintWeek
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.CardBorder
import com.example.todoapp.screen.basicutils.CardCream
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.MilestoneFlag
import com.example.todoapp.screen.basicutils.TodayOutline
import com.example.todoapp.screen.basicutils.TrackBeige
import com.example.todoapp.screen.basicutils.pillarDot
import com.example.todoapp.screen.basicutils.pillarStrong
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/** A missed day when one mission is picked. */
private val MissedRing = Color(0xFFC9BFA8)
internal val WeekLetters = listOf("M", "T", "W", "T", "F", "S", "S")

/** The most done minutes on any past day under [filter], so the bars share one scale. */
internal fun maxDoneMinutes(blueprint: Blueprint, filter: BlueprintFilter): Int =
    blueprint.weeks.flatMap { it.days }.filter { it.past }
        .maxOfOrNull { day -> day.items.filter { filter.keeps(it) && it.done == true }.sumOf { it.minutes } }
        ?.coerceAtLeast(60) ?: 60

/**
 * One day. A day that's over is a bar of what got done, in pillar slices, as long as the time it took. A day
 * ahead is a card of dots, one per mission: filled for fixed days, a ring for flexible ones. With one
 * mission picked, a day that's over is filled when it was done and a ring when it was missed.
 */
internal fun DrawScope.drawBlueprintDay(
    day: BlueprintDay,
    filter: BlueprintFilter,
    topLeft: Offset,
    cell: Size,
    maxMinutes: Int,
    dotRadius: Float,
) {
    val corner = CornerRadius(minOf(cell.height / 3f, 6.dp.toPx()))
    val kept = day.items.filter(filter::keeps)
    if (day.past) {
        drawRoundRect(TrackBeige, topLeft, cell, corner)
        if (filter.missionId != null) {
            kept.firstOrNull()?.let { item ->
                if (item.done == true) drawRoundRect(pillarStrong(item.pillar), topLeft, cell, corner)
                else drawRoundRect(MissedRing, topLeft, cell, corner, style = Stroke(1.2.dp.toPx()))
            }
        } else {
            val done = kept.filter { it.done == true }
            val total = done.sumOf { it.minutes }
            if (total > 0) {
                val width = cell.width * (total.toFloat() / maxMinutes).coerceIn(0.3f, 1f)
                val bar = Path().apply { addRoundRect(RoundRect(Rect(topLeft, Size(width, cell.height)), corner)) }
                clipPath(bar) {
                    var x = topLeft.x
                    PillarType.entries.forEach { pillar ->
                        val minutes = done.filter { it.pillar == pillar }.sumOf { it.minutes }
                        if (minutes == 0) return@forEach
                        val w = width * minutes / total
                        drawRect(pillarStrong(pillar), Offset(x, topLeft.y), Size(w, cell.height))
                        x += w
                    }
                }
            }
        }
    } else {
        val weekend = day.date.dayOfWeek.isoDayNumber >= 6
        drawRoundRect(if (weekend) CardCream else Color.White, topLeft, cell, corner)
        drawRoundRect(CardBorder, topLeft, cell, corner, style = Stroke(1.dp.toPx()))
        drawDots(kept.map { it.pillar to it.flexible }, topLeft, cell, dotRadius)
    }
    if (day.today) drawRoundRect(TodayOutline, topLeft, cell, corner, style = Stroke(1.5.dp.toPx()))
    val flags = day.flags.filter(filter::keeps)
    if (flags.isNotEmpty()) {
        drawFlag(Offset(topLeft.x + cell.width - 3.dp.toPx(), topLeft.y - 3.dp.toPx()), cell.height * 0.8f, if (flags.all { it.reached }) InkGray else MilestoneFlag)
    }
}

/** Dots in rows, centred in the cell, as many as fit. */
private fun DrawScope.drawDots(dots: List<Pair<PillarType, Boolean>>, topLeft: Offset, cell: Size, radius: Float) {
    if (dots.isEmpty()) return
    val gap = radius * 0.8f
    val step = radius * 2 + gap
    val pad = radius
    val perRow = ((cell.width - pad * 2 + gap) / step).toInt().coerceAtLeast(1)
    val maxRows = ((cell.height - 2.dp.toPx() + gap) / step).toInt().coerceAtLeast(1)
    val shown = dots.take(perRow * maxRows)
    val rows = (shown.size + perRow - 1) / perRow
    val top = topLeft.y + (cell.height - (rows * step - gap)) / 2 + radius
    val stroke = (radius * 0.55f).coerceAtLeast(1.dp.toPx())
    shown.forEachIndexed { i, (pillar, flexible) ->
        val inRow = if (i / perRow == rows - 1) shown.size - (rows - 1) * perRow else perRow
        val left = topLeft.x + (cell.width - (inRow * step - gap)) / 2 + radius
        val center = Offset(left + (i % perRow) * step, top + (i / perRow) * step)
        if (flexible) drawCircle(pillarDot(pillar), radius - stroke / 2, center, style = Stroke(stroke))
        else drawCircle(pillarDot(pillar), radius, center)
    }
}

/** A little pennant: the pole at [poleTop], the cloth to its left. */
internal fun DrawScope.drawFlag(poleTop: Offset, height: Float, color: Color) {
    val cloth = height * 0.45f
    drawLine(color, poleTop, poleTop + Offset(0f, height), strokeWidth = 1.3.dp.toPx())
    val path = Path().apply {
        moveTo(poleTop.x, poleTop.y)
        lineTo(poleTop.x - cloth * 1.2f, poleTop.y + cloth / 2)
        lineTo(poleTop.x, poleTop.y + cloth)
        close()
    }
    drawPath(path, color)
}

/** Seven days of [week] side by side; a tap names the day under it. */
@Composable
internal fun BlueprintCells(
    week: BlueprintWeek,
    filter: BlueprintFilter,
    maxMinutes: Int,
    height: Dp,
    dotRadius: Dp,
    gap: Dp,
    modifier: Modifier = Modifier,
    onDay: (BlueprintDay) -> Unit,
) {
    val tap by rememberUpdatedState(onDay)
    Canvas(
        modifier = modifier.height(height).pointerInput(week, gap) {
            detectTapGestures { at ->
                val g = gap.toPx()
                val w = (size.width - g * 6) / 7
                tap(week.days[(at.x / (w + g)).toInt().coerceIn(0, 6)])
            }
        }
    ) {
        val g = gap.toPx()
        val w = (size.width - g * 6) / 7
        week.days.forEachIndexed { i, day ->
            drawBlueprintDay(day, filter, Offset(i * (w + g), 0f), Size(w, size.height), maxMinutes, dotRadius.toPx())
        }
    }
}

/** Done, fixed, flexible and milestone, in the cells' own marks. */
@Composable
internal fun BlueprintLegend(modifier: Modifier = Modifier) {
    val sample = PillarType.WEALTH
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendMark("Done") {
            drawRoundRect(pillarStrong(sample), Offset(0f, size.height * 0.2f), Size(size.width, size.height * 0.6f), CornerRadius(3.dp.toPx()))
        }
        LegendMark("Fixed day") { drawCircle(pillarDot(sample), 3.5.dp.toPx()) }
        LegendMark("Flexible") { drawCircle(pillarDot(sample), 3.dp.toPx(), style = Stroke(1.4.dp.toPx())) }
        LegendMark("Milestone") { drawFlag(Offset(size.width * 0.7f, 0f), size.height, MilestoneFlag) }
    }
}

@Composable
private fun LegendMark(label: String, mark: DrawScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(width = 14.dp, height = 12.dp), onDraw = mark)
        Text(label, fontSize = 11.sp, color = InkGray, modifier = Modifier.padding(start = 4.dp))
    }
}

/**
 * The Missions tab's peek: this week and next, as the diary has them planned. A tap anywhere opens the
 * whole year.
 */
@Composable
fun BlueprintStrip(blueprint: Blueprint, onOpen: () -> Unit) {
    val filter = remember { BlueprintFilter() }
    val maxMinutes = remember(blueprint) { maxDoneMinutes(blueprint, filter) }
    val next = blueprint.thisWeek.plus(7, DateTimeUnit.DAY)
    val weeks = blueprint.weeks.filter { it.start == blueprint.thisWeek || it.start == next }
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 4.dp)
            .shadow(4.dp, shape).clip(shape).background(Color.White)
            .clickable(role = Role.Button, onClickLabel = "Open the year") { onOpen() }
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Blueprint", fontSize = 18.sp, fontStyle = FontStyle.Italic, color = InkBlack, modifier = Modifier.weight(1f))
            Text("See the year", fontSize = 13.sp, color = InkGray)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = InkGray, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.padding(end = 6.dp)) {
            weeks.forEach { week ->
                Text(
                    if (week.start == blueprint.thisWeek) "This week" else "Next week",
                    fontSize = 12.sp, fontStyle = FontStyle.Italic, color = InkGray, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    week.days.forEachIndexed { i, day ->
                        Text(
                            "${WeekLetters[i]} ${day.date.dayOfMonth}", fontSize = 11.sp, textAlign = TextAlign.Center,
                            color = if (day.today) InkBlack else InkGray,
                            fontWeight = if (day.today) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                BlueprintCells(week, filter, maxMinutes, height = 30.dp, dotRadius = 3.5.dp, gap = 5.dp, modifier = Modifier.fillMaxWidth()) { onOpen() }
            }
            BlueprintLegend(Modifier.padding(top = 12.dp))
        }
    }
}

/** 150 minutes → "2.5 h"; under 6 minutes → "–". */
internal fun hoursLabel(minutes: Int): String {
    val tenths = (minutes + 3) / 6
    return if (tenths == 0) "–" else "${tenths / 10}.${tenths % 10} h"
}

internal val MonthColumn = 34.dp
internal val CountColumn = 28.dp
internal fun Modifier.monthColumn() = this.width(MonthColumn)
