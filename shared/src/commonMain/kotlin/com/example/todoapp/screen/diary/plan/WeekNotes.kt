package com.example.todoapp.screen.diary.plan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.OfferResponse
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.CardCream
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.StickyBlue
import com.example.todoapp.screen.basicutils.StickyYellow
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarLight
import com.example.todoapp.screen.diary.LastWeekState
import com.example.todoapp.screen.diary.OfferNote
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.crown_a
import todoapp.shared.generated.resources.flag_open
import todoapp.shared.generated.resources.heart_filled
import todoapp.shared.generated.resources.star_filled
import todoapp.shared.generated.resources.wand
import kotlin.math.abs

private const val CHIP_TAG = "chip"

/**
 * A sentence whose mission titles sit on chips in their pillar's light color (spec 7.1), so any title
 * reads fine whatever the words around it. No-break spaces pad the chip inside.
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
                    drawRoundRect(
                        pillarLight(pillar),
                        topLeft = Offset(minOf(left, right), l.getLineTop(line) + inset),
                        size = Size(abs(right - left), l.getLineBottom(line) - l.getLineTop(line) - 2 * inset),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                    )
                }
            }
        }
    )
}

/** Monday's look back, in the style of the evening highlights card (spec 9). */
@Composable
fun LastWeekCard(state: LastWeekState) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(CardCream, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "LAST WEEK",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp, color = Color(0xFF3A3A3A),
                modifier = Modifier.weight(1f)
            )
            Text(state.range, fontSize = 13.sp, color = InkGray)
        }
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(Color(0xFFDDD6C2)))
        HighlightRow(Res.drawable.star_filled) { Text(state.points, fontSize = 15.sp, color = InkBlack) }
        HighlightRow(if (state.balanced) Res.drawable.crown_a else Res.drawable.heart_filled) {
            Text(state.pillars, fontSize = 15.sp, color = InkBlack)
        }
        // The wand always means "the app adjusted something" (spec 9.1).
        state.adjusted.forEach { line -> HighlightRow(Res.drawable.wand) { ChipText(line, fontSize = 15.sp, color = InkBlack) } }
    }
}

/** A pace offer as a tilted sticky (spec 5.2): easing off on yellow with the wand, stepping up on blue with the flag. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaceOfferNote(note: OfferNote, rotation: Float, onAnswer: (Long, OfferResponse) -> Unit) {
    val words = note.words
    // One answer per offer: a second tap before the note goes away does nothing.
    var answered by remember(note.offerId) { mutableStateOf(false) }
    val answer = { response: OfferResponse ->
        if (!answered) {
            answered = true
            onAnswer(note.offerId, response)
        }
    }
    val ink = if (words.goingWell) pillarInk(PillarType.LIFE) else Color(0xFF6A5A2A)
    StickyNote(rotation = rotation, color = if (words.goingWell) StickyBlue else StickyYellow) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(if (words.goingWell) Res.drawable.flag_open else Res.drawable.wand), contentDescription = null,
                    colorFilter = ColorFilter.tint(ink), modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(words.header, fontSize = 11.sp, letterSpacing = 1.2.sp, color = ink)
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
