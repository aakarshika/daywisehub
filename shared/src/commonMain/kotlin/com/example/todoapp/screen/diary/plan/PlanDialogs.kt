package com.example.todoapp.screen.diary.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.minutesLabel
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarStrong
import com.example.todoapp.screen.diary.AddOption

/** "change" on the diary: today's free time and energy. Saving re-picks the open items. */
@Composable
fun DayCapacityDialog(freeMinutes: Int, energy: Energy, onSave: (Int, Energy) -> Unit, onDismiss: () -> Unit) {
    var minutes by remember { mutableStateOf(freeMinutes) }
    var level by remember { mutableStateOf(energy) }
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = PeachText)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(minutes, level) }, colors = buttonColors) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss, colors = buttonColors) { Text("Cancel") } },
        title = { Text("Your day") },
        text = {
            Column {
                Text("Free time today", fontSize = 14.sp, color = InkGray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { minutes = (minutes - 30).coerceAtLeast(0) }, colors = buttonColors) { Text("−30 min") }
                    Text(minutesLabel(minutes), fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    TextButton(onClick = { minutes = (minutes + 30).coerceAtMost(PlanRepository.MAX_FREE_MINUTES) }, colors = buttonColors) { Text("+30 min") }
                }
                Spacer(Modifier.height(12.dp))
                Text("Energy", fontSize = 14.sp, color = InkGray)
                Row {
                    Energy.entries.forEach { e -> SmallChip(e.label, selected = e == level) { level = e } }
                }
            }
        },
        containerColor = Light_Yellowww
    )
}

/**
 * The pencil on TO DO LIST (spec 6.3): every mission that isn't on the day's list yet, grouped by
 * pillar, each with its reason line. Today's candidates come first in each group. [isToday] only
 * changes the wording ("Add to this day" on any other day).
 */
@Composable
fun AddMissionDialog(
    options: List<AddOption>,
    hasMissions: Boolean,
    isToday: Boolean,
    todayMissionIds: Set<Long>,
    onAdd: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val available = options.filter { it.missionId !in todayMissionIds }
    val groups = PillarType.entries
        .map { pillar -> pillar to available.filter { it.pillar == pillar } }
        .filter { (_, rows) -> rows.isNotEmpty() }
    // What was tapped while the dialog was open, so a screen reader hears "Added" (the row just disappears).
    var tapped by remember { mutableStateOf(emptySet<Long>()) }
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = PeachText)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss, colors = buttonColors) { Text("Done") } },
        title = { Text(if (isToday) "Add to today" else "Add to this day") },
        text = {
            Column {
                if (tapped.isNotEmpty()) {
                    Text(
                        if (tapped.size == 1) "Added" else "Added · ${tapped.size} so far",
                        fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
                        modifier = Modifier.padding(bottom = 4.dp).semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                if (available.isEmpty()) {
                    Text(
                        when {
                            !hasMissions -> "No missions yet. Add some on the Missions tab."
                            // Nothing came up at all: it could be paused or not started, or a fixed block, so say only what's true.
                            options.isEmpty() -> "Nothing else to add. Fixed blocks, paused and not-yet-started missions aren't listed."
                            else -> "Everything is already on ${if (isToday) "today's" else "this day's"} list."
                        }
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        groups.forEach { (pillar, rows) ->
                            item(key = "pillar-${pillar.name}") {
                                Text(
                                    pillar.title.uppercase(),
                                    fontSize = 11.sp, letterSpacing = 1.2.sp, color = pillarInk(pillar),
                                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp).semantics { heading() }
                                )
                            }
                            items(rows, key = { it.missionId }) { option ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                                        .clickable(role = Role.Button) {
                                            tapped = tapped + option.missionId
                                            onAdd(option.missionId)
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(pillarStrong(option.pillar)))
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(option.title, fontSize = 17.sp, fontStyle = FontStyle.Italic)
                                        if (option.reason.isNotEmpty()) {
                                            Text(option.reason, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = InkGray)
                                        }
                                    }
                                    Text(minutesLabel(option.minutes), fontSize = 12.sp, color = InkGray, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Light_Yellowww
    )
}
