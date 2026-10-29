package com.example.todoapp.screen.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.screen.basicutils.InkGray

/** Small italic bullets: the picked mood line under the mood emojis, the day factor lines under the weather row. */
@Composable
fun DayNotesList(notes: List<AnnotatedString>) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        notes.forEach { Text(AnnotatedString("•  ") + it, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, lineHeight = 16.sp) }
    }
}

/** A darker shade of each mood sticker's face, to read as text on the cream card. */
private fun moodInk(icon: String): Color = when (icon) {
    "happy" -> Color(0xFFB07D00)
    "sad" -> Color(0xFF2F72B8)
    "angry" -> Color(0xFFC0392B)
    "dreamy" -> Color(0xFF7B4FBF)
    "bored" -> Color(0xFF8A7A5C)
    "unwell" -> Color(0xFF3E8E3E)
    else -> InkGray
}

/** The same for each day factor's sticker. */
private fun factorInk(icon: String): Color = when (icon) {
    "guests" -> Color(0xFFC2457E)
    "weather" -> Color(0xFF3F6EA8)
    "work" -> Color(0xFF9A5B26)
    "errand" -> Color(0xFFC2402F)
    "travel" -> Color(0xFFC94545)
    "family" -> Color(0xFFC93C63)
    "home" -> Color(0xFFB7791F)
    else -> InkGray
}

private fun AnnotatedString.Builder.bold(text: String, color: Color) =
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) { append(text) }

/** All picked moods in one line ("Felt dreamy, happy, and unwell"), each mood bold in its own colour, or none. */
fun moodNoteLines(moods: List<String>): List<AnnotatedString> = listOfNotNull(
    if (moods.isEmpty()) null else buildAnnotatedString {
        append("Felt ")
        moods.forEachIndexed { i, icon ->
            if (i > 0) append(if (i == moods.lastIndex) (if (moods.size == 2) " and " else ", and ") else ", ")
            bold(icon, moodInk(icon))
        }
    }
)

/** One line per picked day factor, in the row's order: the opening words bold in the factor's colour. */
fun factorNoteLines(factors: List<String>): List<AnnotatedString> = factors.map { icon ->
    val sentence = factorSentence(icon)
    val cut = sentence.indexOf('.')
    buildAnnotatedString {
        if (cut < 0) bold(sentence, factorInk(icon)) else {
            bold(sentence.substring(0, cut), factorInk(icon))
            append(sentence.substring(cut))
        }
    }
}

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
