package com.example.todoapp.screen.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.PillarScore
import com.example.todoapp.domain.WeekScore
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarStrong

private val RingText = Color(0xFF3A3A3A)

/**
 * Spec 9: the week's four pillar rings, each with its points of target and "full" or "light" at the
 * 80% rule. A week the diary wasn't opened in reads "away" instead, so its zeros don't look like misses.
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
    val extra = if (score.extra > 0) ", +${score.extra} extra" else ""
    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "${score.pillar.title}, ${score.earned} of ${score.target} points, $state$extra"
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(72.dp)) {
                val stroke = 9.dp.toPx()
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(Color.White, 0f, 360f, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke))
                if (score.share > 0f) {
                    drawArc(
                        pillarStrong(score.pillar), -90f, 360f * score.share, useCenter = false,
                        topLeft = topLeft, size = arc, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Text("${score.earned}/${score.target}", fontSize = 14.sp, color = RingText)
        }
        Text(score.pillar.title, fontSize = 13.sp, color = RingText, modifier = Modifier.padding(top = 4.dp))
        Text(state, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = if (score.full && !away) pillarInk(score.pillar) else InkGray)
        if (score.extra > 0) Text("+${score.extra} extra", fontSize = 11.sp, color = pillarInk(score.pillar))
    }
}
