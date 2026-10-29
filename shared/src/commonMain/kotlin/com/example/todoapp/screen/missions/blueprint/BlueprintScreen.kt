package com.example.todoapp.screen.missions.blueprint

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.screen.basicutils.components.swipeDownToClose
import com.example.todoapp.screen.basicutils.components.edgeSwipeBack
import com.example.todoapp.domain.Blueprint
import com.example.todoapp.domain.BlueprintDay
import com.example.todoapp.domain.BlueprintFilter
import com.example.todoapp.domain.BlueprintItem
import com.example.todoapp.domain.BlueprintWeek
import com.example.todoapp.domain.Blueprints
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.Projection
import com.example.todoapp.domain.UsualDay
import com.example.todoapp.domain.minutesLabel
import com.example.todoapp.domain.periodUnit
import com.example.todoapp.domain.pillarType
import com.example.todoapp.domain.shortLabel
import com.example.todoapp.domain.weekdayName
import com.example.todoapp.screen.basicutils.CardBorder
import com.example.todoapp.screen.basicutils.ChipBorder
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.MilestoneFlag
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.SelectedWeekTint
import com.example.todoapp.screen.basicutils.TrackBeige
import com.example.todoapp.screen.basicutils.pillarDot
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarLight
import com.example.todoapp.screen.basicutils.pillarStrong
import com.example.todoapp.domain.Period
import kotlinx.datetime.LocalDate
import moe.tlaster.precompose.navigation.BackHandler
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.flag_open

/**
 * The whole year over the Missions tab: the usual week up top, then a row per week, from [Blueprints.WEEKS_BACK]
 * weeks back to the end. It opens a couple of weeks above this one. Pillar and mission chips narrow it
 * down; a tapped day shows what's on it in a card at the bottom. Back closes the card, then the screen.
 */
@Composable
fun BlueprintScreen(blueprint: Blueprint, onClose: () -> Unit) {
    var filter by remember { mutableStateOf(BlueprintFilter()) }
    var picked by remember { mutableStateOf<LocalDate?>(null) }
    BackHandler { if (picked != null) picked = null else onClose() }
    val maxMinutes = remember(blueprint, filter) { maxDoneMinutes(blueprint, filter) }
    val usual = remember(blueprint, filter) { Blueprints.usualWeek(blueprint, filter) }
    val thisWeek = blueprint.weeks.indexOfFirst { it.start == blueprint.thisWeek }.coerceAtLeast(0)
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (thisWeek - 2).coerceAtLeast(0))
    val day = picked?.let { date -> blueprint.weeks.flatMap { it.days }.firstOrNull { it.date == date } }

    Column(
        modifier = Modifier.fillMaxSize().edgeSwipeBack(onClose).background(Light_Yellowww)
            // Over the Missions tab: a touch stops here instead of reaching the list underneath.
            .pointerInput(Unit) {}
            .semantics { paneTitle = "Blueprint" }
    ) {
        TopBar(onClose)
        Filters(blueprint, filter) { filter = it }
        UsualWeek(usual)
        Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp)) {
            Box(Modifier.width(MonthColumn))
            WeekLetters.forEach { letter ->
                Text(letter, fontSize = 11.sp, color = InkGray, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
            Box(Modifier.width(CountColumn))
        }
        LazyColumn(
            state = list,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 16.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            itemsIndexed(blueprint.weeks, key = { _, week -> week.start.toString() }) { i, week ->
                WeekRow(blueprint, week, i, filter, maxMinutes, onDay = { picked = it.date })
            }
            item(key = "legend") { BlueprintLegend(Modifier.padding(start = MonthColumn, top = 12.dp)) }
        }
        day?.let { DayCard(blueprint, it, filter, onClose = { picked = null }) }
    }
}

@Composable
private fun TopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clickable(role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back to Missions", tint = InkGray)
        }
        Text(
            "BLUEPRINT", fontSize = 30.sp, fontStyle = FontStyle.Italic, letterSpacing = 0.5.sp, color = InkGray,
            modifier = Modifier.padding(start = 4.dp).semantics { heading() }
        )
    }
}

/** All, then a chip per pillar with a mission; with a pillar picked, a chip per mission in it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Filters(blueprint: Blueprint, filter: BlueprintFilter, onChange: (BlueprintFilter) -> Unit) {
    val active = blueprint.missions.filter { it.active }
    val pillars = PillarType.entries.filter { pillar -> active.any { it.pillarType == pillar } }
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 6.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip("All", filter.pillar == null, Color(0xFFFFE7A3)) { onChange(BlueprintFilter()) }
            pillars.forEach { pillar ->
                FilterChip(pillar.title, filter.pillar == pillar, pillarLight(pillar)) {
                    onChange(if (filter.pillar == pillar && filter.missionId == null) BlueprintFilter() else BlueprintFilter(pillar))
                }
            }
        }
        val pillar = filter.pillar
        if (pillar != null) {
            FlowRow(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                active.filter { it.pillarType == pillar }.forEach { mission ->
                    FilterChip(mission.title, filter.missionId == mission.missionId, pillarStrong(pillar), dashed = Blueprints.isFlexible(mission)) {
                        onChange(BlueprintFilter(pillar, if (filter.missionId == mission.missionId) null else mission.missionId))
                    }
                }
            }
        }
    }
}

/** A rounded chip, filled with [color] while on. A [dashed] edge marks a flexible mission. */
@Composable
private fun FilterChip(text: String, selected: Boolean, color: Color, dashed: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier.heightIn(min = 36.dp).clip(shape)
            .background(if (selected) color else Color.White.copy(alpha = 0.6f))
            .then(if (selected) Modifier else chipEdge(shape, dashed))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, color = if (selected) InkBlack else Color(0xFF4A4A4A))
    }
}

private fun chipEdge(shape: RoundedCornerShape, dashed: Boolean): Modifier =
    if (!dashed) Modifier.border(1.dp, ChipBorder, shape)
    else Modifier.drawBehind {
        drawRoundRect(
            ChipBorder, cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
        )
    }

/** Average hours per weekday from today on: fixed days solid at the bottom, flexible ones pale above. */
@Composable
private fun UsualWeek(days: List<UsualDay>) {
    val shape = RoundedCornerShape(14.dp)
    val most = (days.maxOfOrNull { it.total } ?: 0).coerceAtLeast(60)
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 10.dp)
            .shadow(3.dp, shape).clip(shape).background(Color.White)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Your usual week", fontSize = 15.sp, fontStyle = FontStyle.Italic, color = InkBlack, modifier = Modifier.weight(1f))
            Text("solid: fixed · pale: flexible", fontSize = 11.sp, color = InkGray)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEachIndexed { i, usual ->
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        val corner = CornerRadius(5.dp.toPx())
                        drawRoundRect(TrackBeige.copy(alpha = 0.6f), cornerRadius = corner)
                        var bottom = size.height
                        fun slab(minutes: Int, fill: Color, edge: Color?) {
                            val h = size.height * minutes / most
                            drawRoundRect(fill, Offset(0f, bottom - h), Size(size.width, h), CornerRadius(3.dp.toPx()))
                            if (edge != null) drawRoundRect(edge, Offset(0f, bottom - h), Size(size.width, h), CornerRadius(3.dp.toPx()), style = Stroke(1.dp.toPx()))
                            bottom -= h
                        }
                        PillarType.entries.forEach { p -> usual.fixed[p]?.let { slab(it, pillarStrong(p), null) } }
                        PillarType.entries.forEach { p -> usual.flexible[p]?.let { slab(it, pillarLight(p), pillarStrong(p)) } }
                    }
                    Text(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[i], fontSize = 11.sp, color = InkGray, modifier = Modifier.padding(top = 4.dp))
                    Text(hoursLabel(usual.total), fontSize = 11.sp, color = InkBlack)
                }
            }
        }
    }
}

/**
 * A week: the month's name by the week its 1st falls in, the seven days, and at the end how the week
 * differs. With one flexible weekly mission picked that's its count; otherwise "−1" for each eased-off one.
 */
@Composable
private fun WeekRow(blueprint: Blueprint, week: BlueprintWeek, index: Int, filter: BlueprintFilter, maxMinutes: Int, onDay: (BlueprintDay) -> Unit) {
    val current = week.start == blueprint.thisWeek
    val first = week.days.firstOrNull { it.date.dayOfMonth == 1 } ?: week.days.first().takeIf { index == 0 }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            .background(if (current) SelectedWeekTint else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            first?.date?.shortLabel()?.substringBefore(" ").orEmpty(),
            fontSize = 11.sp, fontStyle = FontStyle.Italic, color = InkGray, modifier = Modifier.monthColumn().padding(start = 4.dp)
        )
        BlueprintCells(week, filter, maxMinutes, height = 18.dp, dotRadius = 2.6.dp, gap = 4.dp, modifier = Modifier.weight(1f), onDay = onDay)
        val (text, warm) = countFor(blueprint, week, filter)
        Text(text, fontSize = 11.sp, color = if (warm) PeachText else InkGray, textAlign = TextAlign.End, modifier = Modifier.width(CountColumn))
    }
}

private fun countFor(blueprint: Blueprint, week: BlueprintWeek, filter: BlueprintFilter): Pair<String, Boolean> {
    val id = filter.missionId
    if (id != null) {
        val mission = blueprint.missions.firstOrNull { it.missionId == id } ?: return "" to false
        if (!Blueprints.isFlexible(mission) || mission.periodUnit != Period.WEEK) return "" to false
        val count = week.days.sumOf { day -> day.items.count { it.missionId == id } }
        return "$count×" to (count < mission.count)
    }
    val eased = week.easeOffs.count(filter::keeps)
    return (if (eased > 0) "−$eased" else "") to true
}

/** What's on the tapped day: its missions, its milestones, and anything the week eased off. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayCard(blueprint: Blueprint, day: BlueprintDay, filter: BlueprintFilter, onClose: () -> Unit) {
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    val week = blueprint.weeks.first { day.date in it.days.map { d -> d.date } }
    val items = day.items.filter(filter::keeps)
    val guessed = !day.past && day.date > Projection.lastDay(blueprint.today)
    Column(
        modifier = Modifier.fillMaxWidth().swipeDownToClose(onClose).shadow(10.dp, shape).clip(shape).background(Color.White)
            .border(1.dp, CardBorder, shape)
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${day.date.weekdayName()}, ${day.date.shortLabel()}" + when {
                    day.today -> " · today"
                    guessed -> " · a guess"
                    !day.past -> " · planned"
                    else -> ""
                },
                fontSize = 16.sp, fontWeight = FontWeight.Medium, color = InkBlack, modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.size(44.dp).clickable(role = Role.Button, onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, contentDescription = "Close the day", tint = InkGray, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.padding(end = 12.dp)) {
            if (items.isEmpty()) {
                Text("Nothing planned.", fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.forEach { ItemChip(it, day.past) }
                }
            }
            day.flags.filter(filter::keeps).forEach { flag ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Image(
                        painterResource(Res.drawable.flag_open), contentDescription = null,
                        colorFilter = ColorFilter.tint(if (flag.reached) InkGray else MilestoneFlag), modifier = Modifier.size(16.dp)
                    )
                    val mission = blueprint.missions.firstOrNull { it.missionId == flag.missionId }?.title.orEmpty()
                    Text(
                        "${flag.text}${if (flag.reached) ", reached" else ""} · $mission",
                        fontSize = 13.sp, color = InkBlack, modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            week.easeOffs.filter(filter::keeps).forEach { ease ->
                Text(
                    "${ease.title} is ${ease.count}× this week, not ${ease.usual}×, because ${ease.because} lands on ${ease.on.weekdayName()}.",
                    fontSize = 12.sp, fontStyle = FontStyle.Italic, color = PeachText, modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (guessed && items.any { it.flexible }) {
                Text(
                    "Rings are flexible: the diary picks their days when the week comes.",
                    fontSize = 12.sp, fontStyle = FontStyle.Italic, color = InkGray, modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/** Fixed: filled in the pillar's colour. Flexible: a dashed outline. Missed: struck through. */
@Composable
private fun ItemChip(item: BlueprintItem, past: Boolean) {
    val shape = RoundedCornerShape(14.dp)
    val missed = past && item.done != true
    val label = (if (item.done == true) "✓ " else "") + "${item.title} · ${minutesLabel(item.minutes)}"
    val base = Modifier.clip(shape)
    val look = when {
        item.flexible -> base.drawBehind {
            drawRoundRect(
                pillarDot(item.pillar), cornerRadius = CornerRadius(14.dp.toPx()),
                style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())))
            )
        }
        else -> base.background(pillarLight(item.pillar))
    }
    Text(
        label, fontSize = 12.sp,
        color = if (missed) InkGray else if (item.flexible) InkBlack else pillarInk(item.pillar),
        textDecoration = if (missed) TextDecoration.LineThrough else null,
        modifier = look.padding(horizontal = 10.dp, vertical = 5.dp)
    )
}
