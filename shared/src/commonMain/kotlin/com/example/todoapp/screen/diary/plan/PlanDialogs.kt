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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.minutesLabel
import com.example.todoapp.domain.missionKind
import com.example.todoapp.domain.pillarType
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.pillarStrong

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
                    TextButton(onClick = { minutes = (minutes + 30).coerceAtMost(16 * 60) }, colors = buttonColors) { Text("+30 min") }
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

/** The pencil on TO DO LIST: add any mission that isn't on today's list yet. */
@Composable
fun AddMissionDialog(missions: List<Mission>, todayMissionIds: Set<Long>, onAdd: (Long) -> Unit, onDismiss: () -> Unit) {
    val available = missions
        .filter { it.missionKind != MissionKind.BLOCK && it.missionId !in todayMissionIds }
        .sortedBy { it.pillarType.ordinal }
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = PeachText)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss, colors = buttonColors) { Text("Done") } },
        title = { Text("Add to today") },
        text = {
            if (available.isEmpty()) {
                Text("Everything is already on today's list.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(available, key = { it.missionId }) { mission ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onAdd(mission.missionId) }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(pillarStrong(mission.pillarType)))
                            Spacer(Modifier.width(10.dp))
                            Text(mission.title, fontSize = 17.sp, fontStyle = FontStyle.Italic, modifier = Modifier.weight(1f))
                            Text(minutesLabel(mission.durationMinutes), fontSize = 12.sp, color = InkGray)
                        }
                    }
                }
            }
        },
        containerColor = Light_Yellowww
    )
}
