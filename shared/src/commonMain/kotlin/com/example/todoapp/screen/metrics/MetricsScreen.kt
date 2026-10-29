package com.example.todoapp.screen.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.example.todoapp.di.KoinF
import com.example.todoapp.domain.CalendarDay
import com.example.todoapp.domain.LinePart
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.MissionsMarker
import com.example.todoapp.screen.basicutils.Yellow180
import com.example.todoapp.screen.basicutils.components.MarkerHeading
import com.example.todoapp.screen.diary.plan.ChipText
import com.example.todoapp.screen.diary.plan.SmallChip
import com.example.todoapp.screen.metrics.allmonths.AllMonthsScreen
import com.example.todoapp.screen.metrics.allmonths.AllMonthsViewModel
import com.example.todoapp.screen.metrics.badges.DayBadgesDialog
import kotlinx.coroutines.cancel
import kotlinx.datetime.LocalDate

/** Patterns shown before "More patterns". */
private const val FOLDED_PATTERNS = 4

/**
 * Spec 9: the Metrics tab, one week at a time. [openDay] shows a date in the diary: a calendar day with
 * badges lists them first (spec 9.3), and its "Open this day" goes on to the diary. The calendar's title
 * and its "All months" chip open All months over the tab (spec 9.4), with its own view model, made only
 * while it's open.
 */
@Composable
fun MetricsScreen(viewModel: MetricsViewModel, openDay: (LocalDate) -> Unit) {
    val state by viewModel.state.collectAsState()
    val s = state
    // The day whose badges are open. A new view model after midnight closes it, since today's badges change.
    var badgeDay by remember(viewModel) { mutableStateOf<CalendarDay?>(null) }
    // Every pattern, or the first few. A new view model folds them again.
    var allPatterns by remember(viewModel) { mutableStateOf(false) }
    // All months, open over the tab. A new view model after midnight closes it, since its "today" is fixed.
    var allMonths by remember(viewModel) { mutableStateOf(false) }
    // A new view model (after midnight, or on reopening the tab) starts at the top too.
    val listState = key(viewModel) { rememberLazyListState() }

    Box(modifier = Modifier.fillMaxSize().background(Light_Yellowww)) {
        // Drawn only once the state is built, so it opens at the top. Drawn sooner, its bottom space would be its
        // only item, and the list would keep that in view as the cards arrived above it.
        if (s != null) {
            LazyColumn(
                state = listState,
                // Under All months it keeps its place, but a screen reader doesn't reach it.
                modifier = Modifier.fillMaxSize().then(if (allMonths) Modifier.clearAndSetSemantics {} else Modifier)
            ) {
                item(key = "header") {
                    WeekHeader(s, onPrevious = viewModel::previousWeek, onNext = viewModel::nextWeek, onThisWeek = viewModel::backToThisWeek)
                }
                item(key = "rings") { PillarRings(s.score, weekOver = !s.isThisWeek) }
                if (s.ahead.isNotEmpty()) item(key = "week-ahead") { WeekAheadNotes(s.aheadTitle, s.ahead) }
                item(key = "patterns-heading") {
                    MarkerHeading("PATTERNS", Yellow180, Modifier.padding(top = 24.dp, bottom = 4.dp))
                }
                // Patterns follow today, whichever week is on screen.
                if (!s.isThisWeek) {
                    item(key = "patterns-as-of") {
                        Text(
                            "As of today", fontSize = 12.sp, fontStyle = FontStyle.Italic, color = InkGray,
                            modifier = Modifier.padding(horizontal = 22.dp)
                        )
                    }
                }
                if (s.patterns.isEmpty()) {
                    item(key = "patterns-none") { RuledLine("Nothing stands out yet.", color = InkGray) }
                }
                val patterns = if (allPatterns) s.patterns else s.patterns.take(FOLDED_PATTERNS)
                // Keyed by their words, so a line keeps its place as others come and go; a repeat falls back to its place.
                val keys = mutableSetOf<String>()
                patterns.forEachIndexed { i, line ->
                    val text = "pattern:" + line.joinToString("") { it.text }
                    item(key = if (keys.add(text)) text else "pattern-$i") { RuledLine(line) }
                }
                if (s.patterns.size > FOLDED_PATTERNS) {
                    item(key = "patterns-more") {
                        Box(modifier = Modifier.padding(start = 22.dp, top = 4.dp)) {
                            SmallChip(
                                if (allPatterns) "Fewer patterns" else "More patterns",
                                modifier = Modifier.semantics { stateDescription = if (allPatterns) "expanded" else "collapsed" },
                                onClick = { allPatterns = !allPatterns }
                            )
                        }
                    }
                }
                item(key = "calendar") {
                    MonthCalendarCard(
                        s.calendar,
                        onDay = { day -> if (day.badges.isEmpty()) openDay(day.date) else badgeDay = day },
                        caption = POINTS_CAPTION,
                        onAllMonths = { allMonths = true }
                    )
                }
                item(key = "missions-heading") {
                    MarkerHeading("MISSIONS", MissionsMarker, Modifier.padding(top = 24.dp, bottom = 4.dp))
                }
                if (s.missions.isEmpty()) {
                    item(key = "missions-none") { RuledLine("Missions you add show their progress here.", color = InkGray) }
                }
                s.missions.forEach { line -> item(key = "mission-${line.missionId}") { MissionProgressRow(line) } }
                // The tab bar is the scaffold's own, outside this screen: a little room is enough.
                item(key = "bottom-space") { Spacer(Modifier.height(24.dp)) }
            }
        }
        if (allMonths) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Made here, so it exists only while the screen is open. Nothing owns it, so its work stops
                // when the screen closes or the tab leaves, as the Metrics view model's does.
                val allMonthsViewModel = remember { KoinF.di!!.get<AllMonthsViewModel>() }
                DisposableEffect(allMonthsViewModel) {
                    onDispose { allMonthsViewModel.viewModelScope.cancel() }
                }
                // "Open this day" goes straight to the diary, which leaves the tab and closes everything.
                AllMonthsScreen(allMonthsViewModel, openDay = openDay, onClose = { allMonths = false })
            }
        }
    }
    badgeDay?.let { day ->
        DayBadgesDialog(
            day,
            onOpen = {
                badgeDay = null
                openDay(day.date)
            },
            onDismiss = { badgeDay = null }
        )
    }
}

/** "This week" and its dates, arrows a week back and forward, and "Back to this week" when away from it. */
@Composable
private fun WeekHeader(state: MetricsUiState, onPrevious: () -> Unit, onNext: () -> Unit, onThisWeek: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 30.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Title and dates read as one, and are read again when the week changes.
            Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                Text(
                    state.title, fontSize = 30.sp, fontStyle = FontStyle.Italic, letterSpacing = 0.5.sp, color = InkGray,
                    modifier = Modifier.semantics { heading() }
                )
            }
            WeekArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous week", enabled = state.canGoBack, onClick = onPrevious)
            WeekArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next week", enabled = state.canGoForward, onClick = onNext)
        }
        // The dates share their row with "Back to this week", whose room stays on this week too, so the page
        // doesn't jump when it comes and goes.
        Row(modifier = Modifier.heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(state.range, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, modifier = Modifier.padding(end = 12.dp))
            if (!state.isThisWeek) SmallChip("Back to this week", onClick = onThisWeek)
        }
    }
}

/** A 44dp arrow button; faded and inactive at either end. */
@Composable
private fun WeekArrow(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) InkGray else InkGray.copy(alpha = 0.3f))
    }
}

/** One line in the diary's hand, on a ruled line (spec 9.1). */
@Composable
private fun RuledLine(text: String, color: Color = InkBlack) {
    Ruled {
        Text(
            text, fontSize = 17.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light, letterSpacing = 0.3.sp,
            color = color, modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}

/** The same line, its titles on chips (spec 7.1). */
@Composable
private fun RuledLine(parts: List<LinePart>) {
    Ruled {
        // ChipText takes its weight and spacing from here, so the hand matches the plain lines'.
        ProvideTextStyle(TextStyle(fontWeight = FontWeight.Light, letterSpacing = 0.3.sp)) {
            ChipText(parts, fontSize = 17.sp, color = InkBlack, italic = true, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

/** The ruled line a line of the diary's hand sits on. */
@Composable
private fun Ruled(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).heightIn(min = 38.dp)
            .drawBehind {
                val y = size.height - 1.dp.toPx()
                drawLine(Blue80, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            },
        contentAlignment = Alignment.CenterStart
    ) {
        content()
    }
}
