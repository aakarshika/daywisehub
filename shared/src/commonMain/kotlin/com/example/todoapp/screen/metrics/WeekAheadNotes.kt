package com.example.todoapp.screen.metrics

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.LinePart
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.diary.plan.ChipText
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.wand

/** What next week adjusts: one wand line each, titles on pillar chips (spec 7.1). */
@Composable
fun WeekAheadNotes(title: String, lines: List<List<LinePart>>) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp)) {
        Text(title, fontSize = 11.sp, letterSpacing = 1.2.sp, color = InkGray, modifier = Modifier.semantics { heading() })
        lines.forEach { line ->
            Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                // The wand always means "the app adjusted something" (spec 9.1).
                Image(
                    painterResource(Res.drawable.wand), contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(0xFF6B6B6B)), modifier = Modifier.padding(top = 3.dp).size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                ChipText(line, fontSize = 15.sp, color = Color(0xFF2A2A2A), italic = true)
            }
        }
    }
}
