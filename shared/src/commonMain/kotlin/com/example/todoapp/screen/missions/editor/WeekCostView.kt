package com.example.todoapp.screen.missions.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.WeekCost
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Orange180
import com.example.todoapp.screen.basicutils.TrackBeige
import com.example.todoapp.screen.basicutils.pillarStrong

/** Spec 4.2: each pillar's target this week with the new points striped, the plain-words line, and free hours. */
@Composable
fun WeekCostView(cost: WeekCost, draftPillar: PillarType) {
    val scale = cost.pillars.maxOf { maxOf(it.before, it.after) }.coerceAtLeast(1)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        cost.pillars.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.pillar.title, fontSize = 14.sp, color = Color(0xFF3A3A3A), modifier = Modifier.width(64.dp))
                CostBar(p.before, p.after, scale, pillarStrong(p.pillar), pillarStrong(p.pillar), Modifier.weight(1f))
                Text(
                    if (p.before == p.after) "${p.after}" else "${p.before} → ${p.after}",
                    fontSize = 13.sp, color = Color(0xFF4A4A4A), textAlign = TextAlign.End,
                    modifier = Modifier.width(64.dp)
                )
            }
        }
    }
    Text(
        cost.line,
        fontSize = 14.sp, fontStyle = FontStyle.Italic, lineHeight = 20.sp, color = Color(0xFF3A3A3A),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)
    )
    Column(modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Free hours planned", fontSize = 13.sp, color = Color(0xFF4A4A4A))
            Text(
                "${MissionWords.hoursNumber(cost.plannedAfter)} of ${MissionWords.hoursNumber(cost.freeMinutes)}",
                fontSize = 13.sp, color = Color(0xFF4A4A4A)
            )
        }
        Spacer(Modifier.height(6.dp))
        CostBar(
            cost.plannedBefore, cost.plannedAfter, maxOf(cost.freeMinutes, cost.plannedAfter, 1),
            Orange180, pillarStrong(draftPillar), Modifier.fillMaxWidth()
        )
        Text(
            MissionWords.spareLine(cost.freeMinutes, cost.plannedAfter),
            fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/** [before] in solid [color]; anything [after] adds on top, striped in [stripe]. */
@Composable
private fun CostBar(before: Int, after: Int, scale: Int, color: Color, stripe: Color, modifier: Modifier) {
    Box(
        modifier = modifier.height(12.dp).clip(RoundedCornerShape(6.dp)).background(TrackBeige)
            .drawBehind {
                val unit = size.width / scale
                val solid = minOf(before, after) * unit
                if (solid > 0f) drawRect(color, size = Size(solid, size.height))
                val added = (after - before).coerceAtLeast(0) * unit
                if (added > 0f) {
                    clipRect(left = solid, top = 0f, right = solid + added, bottom = size.height) {
                        drawRect(stripe.copy(alpha = 0.35f), topLeft = Offset(solid, 0f), size = Size(added, size.height))
                        val gap = 8.dp.toPx()
                        var x = solid - size.height
                        while (x < solid + added) {
                            drawLine(stripe, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 3.dp.toPx())
                            x += gap
                        }
                    }
                }
            }
    )
}
