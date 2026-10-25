package com.example.todoapp.screen.missions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.plan.DaySettings
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.minutesLabel
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.PeachText

/** Free time per weekday (DaySettings), in half hours. */
@Composable
fun FreeTimeDialog(minutesByWeekday: Map<Int, Int>, onSave: (Map<Int, Int>) -> Unit, onDismiss: () -> Unit) {
    var minutes by remember { mutableStateOf(minutesByWeekday) }
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = PeachText)
    val step = 30
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(minutes) }, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp)) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp)) { Text("Cancel") }
        },
        title = { Text("Free time per day") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Time outside fixed blocks that the diary may fill. Days already planned keep theirs; change today from the diary.",
                    fontSize = 13.sp, color = InkGray
                )
                (1..7).forEach { day ->
                    val value = minutes[day] ?: DaySettings.defaultFor(day)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(MissionWords.shortDay(day), fontSize = 15.sp, modifier = Modifier.width(44.dp))
                        TextButton(
                            onClick = { minutes = minutes + (day to (value - step).coerceAtLeast(0)) },
                            enabled = value > 0, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp)
                        ) { Text("−30") }
                        Text(minutesLabel(value), fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { minutes = minutes + (day to (value + step).coerceAtMost(PlanRepository.MAX_FREE_MINUTES)) },
                            enabled = value < PlanRepository.MAX_FREE_MINUTES, colors = buttonColors,
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) { Text("+30") }
                    }
                }
                Text(
                    "${MissionWords.hoursPhrase((1..7).sumOf { minutes[it] ?: DaySettings.defaultFor(it) })} a week",
                    fontSize = 14.sp, color = InkGray, modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        containerColor = Light_Yellowww
    )
}
