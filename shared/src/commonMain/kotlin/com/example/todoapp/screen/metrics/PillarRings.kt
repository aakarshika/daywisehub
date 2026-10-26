package com.example.todoapp.screen.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.PillarScore
import com.example.todoapp.domain.WeekScore
import com.example.todoapp.screen.basicutils.CardHeadInk
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarStrong

private val RingStroke = 9.dp

/**
 * Spec 9: the week's four pillar rings, each with its points of target and "full" or "light" at the
 * 80% rule. A week the diary wasn't opened in reads "away" instead, with no numbers and no arc, so its
 * zeros don't look like misses.
 */
@Composable
fun PillarRings(week: WeekScore) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        week.pillars.forEach { score -> PillarRing(score, away = week.away, Modifier.weight(1f)) }
    }
}

@Composable
private fun PillarRing(score: PillarScore, away: Boolean, modifier: Modifier) {
    val state = when {
        away -> "away"
        score.full -> "full"
        else -> "light"
    }
    val extra = if (!away && score.extra > 0) ", +${score.extra} extra" else ""
    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = if (away) {
                "${score.pillar.title}, away"
            } else {
                "${score.pillar.title}, ${score.earned} of ${MissionWords.pointsPhrase(score.target)}, $state$extra"
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Round at any width: 72dp at most, smaller on a narrow screen or with a large display size.
        BoxWithConstraints(
            modifier = Modifier.widthIn(max = 72.dp).fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = RingStroke.toPx()
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(Color.White, 0f, 360f, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke))
                if (!away && score.share > 0f) {
                    drawArc(
                        pillarStrong(score.pillar), -90f, 360f * score.share, useCenter = false,
                        topLeft = topLeft, size = arc, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
            val numbers = if (away) "–" else "${score.earned}/${score.target}"
            Text(numbers, fontSize = ringFontSize(numbers, maxWidth), maxLines = 1, softWrap = false, color = CardHeadInk)
        }
        Text(score.pillar.title, fontSize = 13.sp, color = CardHeadInk, modifier = Modifier.padding(top = 4.dp))
        Text(state, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = if (score.full && !away) pillarInk(score.pillar) else InkGray)
        if (!away && score.extra > 0) Text("+${score.extra} extra", fontSize = 11.sp, color = pillarInk(score.pillar))
    }
}

/**
 * 14sp, or smaller when a large font scale would push [text] past the inside of a [ring] that wide. A
 * digit is about 0.6em wide; the numbers are in the ring's description too.
 */
@Composable
private fun ringFontSize(text: String, ring: Dp): TextUnit {
    val density = LocalDensity.current
    val inside = ring - RingStroke * 2 - 6.dp
    val fits = inside / (text.length.coerceAtLeast(1) * 0.6f)
    return with(density) { minOf(14.sp.toDp(), fits).toSp() }
}
