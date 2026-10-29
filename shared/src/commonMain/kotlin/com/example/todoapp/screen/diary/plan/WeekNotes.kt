package com.example.todoapp.screen.diary.plan

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.OfferResponse
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.StickyBlue
import com.example.todoapp.screen.basicutils.StickyYellow
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarLight
import com.example.todoapp.screen.basicutils.pillarStrong
import com.example.todoapp.screen.diary.LastWeekState
import com.example.todoapp.screen.diary.OfferNote
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.flag_open
import todoapp.shared.generated.resources.wand
import kotlin.math.abs

private const val CHIP_TAG = "chip"

/**
 * A sentence whose mission titles sit on chips in their pillar's light color (spec 7.1), so any title
 * reads fine whatever the words around it. No-break spaces pad the chip inside. A hairline in the
 * pillar's strong color keeps a chip visible on a sticky of a similar color. Every line box is the same
 * height, so a chip on a one-line sentence isn't cramped.
 */
@Composable
fun ChipText(parts: List<LinePart>, fontSize: TextUnit, color: Color, italic: Boolean = false, modifier: Modifier = Modifier) {
    val text = remember(parts) {
        buildAnnotatedString {
            parts.forEach { part ->
                val pillar = part.pillar
                if (pillar == null) {
                    append(part.text)
                } else {
                    pushStringAnnotation(CHIP_TAG, pillar.name)
                    withStyle(SpanStyle(color = pillarInk(pillar), fontStyle = FontStyle.Normal)) { append(" ${part.text} ") }
                    pop()
                }
            }
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text,
        fontSize = fontSize,
        lineHeight = fontSize * 1.6f,
        // Merged into the theme's style, so its letter spacing and font stay.
        style = LocalTextStyle.current.merge(
            TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None))
        ),
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        color = color,
        onTextLayout = { layout = it },
        modifier = modifier.drawBehind {
            val l = layout ?: return@drawBehind
            val inset = 2.dp.toPx()
            text.getStringAnnotations(CHIP_TAG, 0, text.length).forEach { chip ->
                val pillar = PillarType.entries.firstOrNull { it.name == chip.item } ?: return@forEach
                for (line in l.getLineForOffset(chip.start)..l.getLineForOffset(chip.end - 1)) {
                    val from = maxOf(chip.start, l.getLineStart(line))
                    val to = minOf(chip.end, l.getLineEnd(line))
                    if (to <= from) continue
                    val left = l.getBoundingBox(from).left
                    val right = l.getBoundingBox(to - 1).right
                    val topLeft = Offset(minOf(left, right), l.getLineTop(line) + inset)
                    val size = Size(abs(right - left), l.getLineBottom(line) - l.getLineTop(line) - 2 * inset)
                    drawRoundRect(pillarLight(pillar), topLeft, size, CornerRadius(6.dp.toPx()))
                    drawRoundRect(pillarStrong(pillar), topLeft, size, CornerRadius(6.dp.toPx()), style = Stroke(1.dp.toPx()))
                }
            }
        }
    )
}

/**
 * Monday's look back, under the week strip (spec 9): last week in one quiet line, then what the app
 * moved into this week with the wand. "Nothing to catch up on." is left out here; Metrics says it.
 */
@Composable
fun LastWeekNotes(state: LastWeekState) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 8.dp)) {
        Text(state.recap, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = InkGray)
        state.adjusted.filter { it.adjusted }.forEach { line ->
            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Top) {
                Image(
                    painterResource(Res.drawable.wand), contentDescription = null,
                    colorFilter = ColorFilter.tint(InkGray), modifier = Modifier.padding(top = 2.dp).size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                ChipText(line.parts, fontSize = 13.sp, color = InkBlack, italic = true)
            }
        }
    }
}

/** A pace offer as a tilted sticky (spec 5.2): easing off on yellow with the wand, stepping up on blue with the flag. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaceOfferNote(note: OfferNote, rotation: Float, onAnswer: (Long, OfferResponse) -> Unit, modifier: Modifier = Modifier) {
    val words = note.words
    // One answer per offer: a second tap before the note goes away does nothing, and the note dims meanwhile.
    var answered by remember(note.offerId) { mutableStateOf(false) }
    val answer = { response: OfferResponse ->
        if (!answered) {
            answered = true
            onAnswer(note.offerId, response)
        }
    }
    val ink = if (words.goingWell) pillarInk(PillarType.LIFE) else Color(0xFF6A5A2A)
    StickyNote(
        rotation = rotation,
        color = if (words.goingWell) StickyBlue else StickyYellow,
        modifier = modifier.alpha(if (answered) 0.6f else 1f)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(if (words.goingWell) Res.drawable.flag_open else Res.drawable.wand), contentDescription = null,
                    colorFilter = ColorFilter.tint(ink), modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(words.header, fontSize = 11.sp, letterSpacing = 1.2.sp, color = ink, modifier = Modifier.semantics { heading() })
            }
            ChipText(words.line, fontSize = 16.sp, color = InkBlack, italic = true, modifier = Modifier.padding(top = 6.dp))
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlanButton(words.accept, filled = true, onClick = { answer(OfferResponse.ACCEPTED) })
                PlanButton(
                    words.keep, filled = false,
                    onClick = { answer(if (words.goingWell) OfferResponse.NOT_YET else OfferResponse.KEPT) }
                )
                words.pause?.let { PlanButton(it, filled = false, onClick = { answer(OfferResponse.PAUSED) }) }
            }
        }
    }
}
