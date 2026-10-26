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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.Yellow180
import com.example.todoapp.screen.basicutils.components.MarkerHeading
import com.example.todoapp.screen.diary.plan.SmallChip
import kotlinx.datetime.LocalDate

private val MissionsMarker = Color(0xFFE8DEF8)

/** Spec 9: the Metrics tab, one week at a time. [openDay] shows a date in the diary. */
@Composable
fun MetricsScreen(viewModel: MetricsViewModel, openDay: (LocalDate) -> Unit) {
    val state by viewModel.state.collectAsState()
    val s = state

    Box(modifier = Modifier.fillMaxSize().background(Light_Yellowww)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (s != null) {
                item(key = "header") {
                    WeekHeader(s, onPrevious = viewModel::previousWeek, onNext = viewModel::nextWeek, onThisWeek = viewModel::backToThisWeek)
                }
                item(key = "rings") { PillarRings(s.score) }
                item(key = "week-ahead") { WeekAheadNotes(s.aheadTitle, s.ahead) }
                item(key = "highlights") { WeekHighlightsCard(s.highlights) }
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
                s.patterns.forEachIndexed { i, line -> item(key = "pattern-$i") { RuledLine(line) } }
                item(key = "calendar") { MonthCalendarCard(s.calendar, openDay) }
                item(key = "missions-heading") {
                    MarkerHeading("MISSIONS", MissionsMarker, Modifier.padding(top = 24.dp, bottom = 4.dp))
                }
                if (s.missions.isEmpty()) {
                    item(key = "missions-none") { RuledLine("Missions you add show their progress here.", color = InkGray) }
                }
                s.missions.forEach { line -> item(key = "mission-${line.missionId}") { MissionProgressRow(line) } }
            }
            item(key = "bottom-space") { Spacer(Modifier.height(120.dp)) }
        }
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
                Text(state.range, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, modifier = Modifier.padding(top = 4.dp))
            }
            WeekArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous week", enabled = state.canGoBack, onClick = onPrevious)
            WeekArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next week", enabled = state.canGoForward, onClick = onNext)
        }
        // The chip's room stays on this week too, so the page doesn't jump when it comes and goes.
        Box(modifier = Modifier.heightIn(min = 44.dp)) {
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
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).heightIn(min = 38.dp)
            .drawBehind {
                val y = size.height - 1.dp.toPx()
                drawLine(Blue80, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text, fontSize = 17.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light, letterSpacing = 0.3.sp,
            color = color, modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}
