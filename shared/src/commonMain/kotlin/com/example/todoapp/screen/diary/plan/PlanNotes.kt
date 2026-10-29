package com.example.todoapp.screen.diary.plan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Orange180
import com.example.todoapp.screen.basicutils.PeachInk
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.StickyYellow
import com.example.todoapp.screen.diary.DiaryUiState
import com.example.todoapp.screen.diary.DidntFitRow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.flag_open

/** Slightly tilted sticky note, like the mission cards. A screen reader reads it as one group. */
@Composable
fun StickyNote(rotation: Float, color: Color = StickyYellow, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.fillMaxWidth()
            .semantics { isTraversalGroup = true }
            .padding(horizontal = 30.dp, vertical = 12.dp)
            .rotate(rotation)
            .shadow(6.dp, RoundedCornerShape(4.dp))
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) { content() }
}

@Composable
fun DidntFitNote(row: DidntFitRow) {
    StickyNote(rotation = -1.2f) {
        Row(verticalAlignment = Alignment.Top) {
            Image(
                painterResource(Res.drawable.flag_open), contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFFB8692E)), modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "DIDN'T FIT TODAY",
                    fontSize = 11.sp, letterSpacing = 1.2.sp, color = Color(0xFF6A5A2A),
                    modifier = Modifier.semantics { heading() }
                )
                Text(row.title, fontSize = 19.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light, color = InkBlack)
                Text(row.text, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = Color(0xFF5F5A48))
            }
        }
    }
}

/** Morning buttons: Swap one, Lighter day, and Looks good when [showLooksGood]. */
@Composable
fun PlanActions(editMode: Boolean, showLooksGood: Boolean, onEdit: () -> Unit, onLighter: () -> Unit, onLooksGood: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        PlanButton(if (editMode) "Done swapping" else "Swap one", filled = false, onClick = onEdit)
        PlanButton("Lighter day", filled = false, onClick = onLighter)
        if (showLooksGood) PlanButton("Looks good", filled = true, onClick = onLooksGood)
    }
}

/** The peach button: filled with dark text, or outlined; at least 44dp tall. Also used by the mission editor. */
@Composable
internal fun PlanButton(text: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(if (filled) Modifier.background(Orange180) else Modifier.border(1.5.dp, Orange180, RoundedCornerShape(10.dp)))
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (filled) PeachInk else PeachText)
    }
}

/** One line of a highlights card: an icon (a quiet dot without one), then [content]. */
@Composable
internal fun HighlightRow(icon: DrawableResource?, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .background(Color(0xFFFDFAF1), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(
                painterResource(icon), contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFF3A3A3A)), modifier = Modifier.size(22.dp)
            )
        } else {
            Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(6.dp).background(InkGray, CircleShape))
            }
        }
        Spacer(Modifier.width(14.dp))
        content()
    }
}
