package com.example.todoapp.screen.diary.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.TrackBeige
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarStrong
import com.example.todoapp.screen.diary.PillarProgress

/**
 * The page's week as bars under the date: points earned against each pillar's target. [label] names
 * the week ("THIS WEEK", "LAST WEEK", …); [freshStart] adds "A FRESH START", on today's page only.
 */
@Composable
fun PillarWeekStrip(pillars: List<PillarProgress>, label: String, freshStart: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(if (freshStart) "$label · A FRESH START" else label, fontSize = 11.sp, letterSpacing = 1.2.sp, color = InkGray)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pillars.forEach { progress ->
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(progress.pillar.title, fontSize = 12.sp, color = Color(0xFF4A4A4A))
                        Text("${progress.earned}/${progress.target}", fontSize = 12.sp, color = Color(0xFF4A4A4A))
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(7.dp)
                            .clip(RoundedCornerShape(4.dp)).background(TrackBeige)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(progress.share).fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp)).background(pillarStrong(progress.pillar))
                        )
                    }
                    if (progress.extra > 0) {
                        Text(
                            "+${progress.extra} extra",
                            fontSize = 11.sp, color = pillarInk(progress.pillar),
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
