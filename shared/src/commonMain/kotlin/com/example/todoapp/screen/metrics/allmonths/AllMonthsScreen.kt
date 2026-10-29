package com.example.todoapp.screen.metrics.allmonths

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.CalendarDay
import com.example.todoapp.domain.CalendarFilter
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.diary.plan.ChipText
import com.example.todoapp.screen.diary.plan.SmallChip
import com.example.todoapp.screen.metrics.CalendarCaption
import com.example.todoapp.screen.metrics.ITEMS_CAPTION
import com.example.todoapp.screen.metrics.MonthCalendarCard
import com.example.todoapp.screen.metrics.POINTS_CAPTION
import com.example.todoapp.screen.metrics.badges.DayBadgesDialog
import kotlinx.datetime.LocalDate
import moe.tlaster.precompose.navigation.BackHandler

/**
 * Spec 9.4: every month from the first stored item's to this one, over the Metrics tab. It opens on this
 * month at the bottom, with older ones above, under the top bar and the filter chips, which stay pinned.
 * The line summing the filter up and the caption sit under this month, so the months get the room. With
 * a filter on, or for a day without badges, tapping a day hands it to [openDay]; otherwise its badges
 * show first, as on the Metrics calendar. The back arrow and the system back call [onClose].
 */
@Composable
fun AllMonthsScreen(viewModel: AllMonthsViewModel, openDay: (LocalDate) -> Unit, onClose: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val s = state
    // The day whose badges are open, and whether the Missions checklist is.
    var badgeDay by remember { mutableStateOf<CalendarDay?>(null) }
    var picking by remember { mutableStateOf(false) }
    BackHandler { onClose() }

    Column(
        modifier = Modifier.fillMaxSize().background(Light_Yellowww)
            // Over the Metrics tab: a touch anywhere stops here instead of reaching the list underneath.
            .pointerInput(Unit) {}
            // A screen reader announces the screen as it opens.
            .semantics { paneTitle = "All months" }
    ) {
        TopBar(onClose)
        if (s != null) {
            FilterChips(s.filter, s.choices.mapTo(mutableSetOf()) { it.pillar }, onPillar = viewModel::togglePillar, onMissions = { picking = true }, onClear = viewModel::clear)
            LazyColumn(
                // The first items sit at the bottom: the caption, the summary just above it, then this month, so
                // the screen opens on it, with older months above.
                reverseLayout = true,
                // The tab bar is the scaffold's own, outside this screen.
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                item(key = "caption") {
                    CalendarCaption(
                        if (s.filter.isOn) ITEMS_CAPTION else POINTS_CAPTION,
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
                item(key = "summary") {
                    // Always there, so the first pick changes the line and a screen reader reads it again.
                    ChipText(
                        s.summary, fontSize = 16.sp, color = InkBlack, italic = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                items(s.months, key = { it.title }) { month ->
                    MonthCalendarCard(month, onDay = { day ->
                        if (s.filter.isOn || day.badges.isEmpty()) openDay(day.date) else badgeDay = day
                    })
                }
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
    if (picking && s != null) {
        MissionFilterDialog(
            s.choices, s.filter.missionIds,
            onDone = { ids ->
                picking = false
                viewModel.setMissions(ids)
            },
            onDismiss = { picking = false }
        )
    }
}

/** A 44dp back arrow and "ALL MONTHS", in the Metrics header's hand. */
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
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back to Metrics", tint = InkGray)
        }
        Text(
            "ALL MONTHS", fontSize = 30.sp, fontStyle = FontStyle.Italic, letterSpacing = 0.5.sp, color = InkGray,
            modifier = Modifier.padding(start = 4.dp).semantics { heading() }
        )
    }
}

/** A chip for each pillar with a mission and one for missions ("2 missions" once some are picked), then "Clear" while anything is. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChips(filter: CalendarFilter, withMissions: Set<PillarType>, onPillar: (PillarType) -> Unit, onMissions: () -> Unit, onClear: () -> Unit) {
    FlowRow(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp)) {
        // A pillar with no mission, active or archived, has nothing to filter by. A picked one stays to unpick.
        PillarType.entries.filter { it in withMissions || it in filter.pillars }.forEach { pillar ->
            SmallChip(pillar.title, selected = pillar in filter.pillars, onClick = { onPillar(pillar) })
        }
        val picked = filter.missionIds.size
        SmallChip(
            when (picked) {
                0 -> "Missions"
                1 -> "1 mission"
                else -> "$picked missions"
            },
            selected = picked > 0,
            onClick = onMissions
        )
        if (filter.isOn) SmallChip("Clear", onClick = onClear)
    }
}
