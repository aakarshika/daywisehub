package com.example.todoapp.screen.missions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.reminder.ReminderSettings
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.PeachText

/** Which notifications to send and when, in quarter hours; and which fixed blocks get a "starts now". */
@Composable
fun RemindersDialog(settings: ReminderSettings, blocks: List<Mission>, onSave: (ReminderSettings) -> Unit, onDismiss: () -> Unit) {
    var s by remember { mutableStateOf(settings) }
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = PeachText)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(s) }, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp)) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp)) { Text("Cancel") }
        },
        title = { Text("Reminders") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Tomorrow gets planned the night before. If it isn't, the night one lists its picks, and the morning asks once more.",
                    fontSize = 13.sp, color = InkGray
                )
                TimedRow("Evening check, then plan tomorrow", s.eveningOn, s.eveningMinute,
                    onOn = { s = s.copy(eveningOn = it) }, onMinute = { s = s.copy(eveningMinute = it) })
                TimedRow("Night, if tomorrow isn't planned", s.nightOn, s.nightMinute,
                    onOn = { s = s.copy(nightOn = it) }, onMinute = { s = s.copy(nightMinute = it) })
                TimedRow("Morning: your day, or plan it", s.morningOn, s.morningMinute,
                    onOn = { s = s.copy(morningOn = it) }, onMinute = { s = s.copy(morningMinute = it) })
                SwitchRow("New week note on Sunday and Monday", s.weekOn) { s = s.copy(weekOn = it) }
                if (blocks.isNotEmpty()) {
                    Text("WHEN A BLOCK STARTS", fontSize = 11.sp, letterSpacing = 1.2.sp, color = InkGray, modifier = Modifier.padding(top = 14.dp))
                    blocks.forEach { block ->
                        val time = block.startTime?.let { " · $it" }.orEmpty()
                        SwitchRow(block.title + time, block.missionId in s.blockIds) { s = s.withBlock(block.missionId, it) }
                    }
                }
            }
        },
        containerColor = Light_Yellowww
    )
}

@Composable
private fun TimedRow(label: String, on: Boolean, minute: Int, onOn: (Boolean) -> Unit, onMinute: (Int) -> Unit) {
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = PeachText)
    val step = 15
    SwitchRow(label, on, onOn)
    if (on) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { onMinute((minute - step).mod(DAY)) }, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp)
            ) { Text("−15") }
            Text(MissionWords.clockLabel(minute), fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            TextButton(
                onClick = { onMinute((minute + step).mod(DAY)) }, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp)
            ) { Text("+15") }
        }
    }
}

@Composable
private fun SwitchRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = on, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = PeachText)
        )
    }
}

private const val DAY = 24 * 60
