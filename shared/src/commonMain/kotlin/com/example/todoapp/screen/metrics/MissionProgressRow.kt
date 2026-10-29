package com.example.todoapp.screen.metrics

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.MissionLine
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.DetailInk
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarStrong
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.flag_open

/** One mission: a pillar dot, the title as typed, and its progress line. A screen reader reads it as one item. */
@Composable
fun MissionProgressRow(line: MissionLine) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .heightIn(min = 44.dp)
            .drawBehind {
                val y = size.height - 1.dp.toPx()
                drawLine(Blue80, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 4.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(10.dp).background(pillarStrong(line.pillar), CircleShape)
                .semantics { contentDescription = line.pillar.title }
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Medium, as on the Missions tab.
            Text(line.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = InkBlack)
            Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (line.finishLine) {
                    Image(
                        painterResource(Res.drawable.flag_open), contentDescription = "Finish line",
                        colorFilter = ColorFilter.tint(pillarInk(line.pillar)), modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(line.detail, fontSize = 13.sp, color = DetailInk)
            }
        }
    }
}
