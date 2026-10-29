package com.example.todoapp.screen.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.screen.basicutils.InkGray

/** Small italic bullets: the picked mood line under the mood emojis, the day factor lines under the weather row. */
@Composable
fun DayNotesList(notes: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        notes.forEach { Text("•  $it", fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, lineHeight = 16.sp) }
    }
}

/** All picked moods in one line ("Felt dreamy, happy, and confused"), or none. */
fun moodNoteLines(moods: List<String>): List<String> = listOfNotNull(
    when (moods.size) {
        0 -> null
        1 -> "Felt ${moods[0]}"
        2 -> "Felt ${moods[0]} and ${moods[1]}"
        else -> "Felt ${moods.dropLast(1).joinToString(", ")}, and ${moods.last()}"
    }
)

/** One line per picked day factor, in the row's order. */
fun factorNoteLines(factors: List<String>): List<String> = factors.map(::factorSentence)

private fun factorSentence(icon: String): String = when (icon) {
    "guests" -> "Had company. Guests over, visitors, hosting"
    "weather" -> "Weather got in the way. Rain, heat, snow, storms, plans cancelled"
    "work" -> "Work took over. Long hours, deadline crunch, urgent calls"
    "errand" -> "Surprise errand. Unplanned chores, appointments, paperwork"
    "travel" -> "On the road. Travel, traffic, away from home"
    "family" -> "Loved ones needed me. Parents, kids, pets."
    "home" -> "Glitch. Repairs, power or internet cuts, deliveries, noise"
    else -> icon
}
