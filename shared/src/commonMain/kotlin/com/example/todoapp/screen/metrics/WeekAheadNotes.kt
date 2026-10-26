package com.example.todoapp.screen.metrics

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.AheadLine
import com.example.todoapp.screen.basicutils.IconInk
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.NoteInk
import com.example.todoapp.screen.diary.plan.ChipText
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.wand

/** What next week adjusts: one line each, titles on pillar chips (spec 7.1). */
@Composable
fun WeekAheadNotes(title: String, lines: List<AheadLine>) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp)) {
        Text(title, fontSize = 11.sp, letterSpacing = 1.2.sp, color = InkGray, modifier = Modifier.semantics { heading() })
        lines.forEach { line ->
            Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                // The wand always means "the app adjusted something" (spec 9.1); any other line gets a quiet dot.
                if (line.adjusted) {
                    Image(
                        painterResource(Res.drawable.wand), contentDescription = null,
                        colorFilter = ColorFilter.tint(IconInk), modifier = Modifier.padding(top = 3.dp).size(20.dp)
                    )
                } else {
                    Box(modifier = Modifier.padding(top = 3.dp).size(20.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(6.dp).background(InkGray, CircleShape))
                    }
                }
                Spacer(Modifier.width(10.dp))
                ChipText(line.parts, fontSize = 15.sp, color = NoteInk, italic = true)
            }
        }
    }
}
