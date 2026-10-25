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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.CardCream
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.Orange180
import com.example.todoapp.screen.basicutils.PeachInk
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.StickyYellow
import com.example.todoapp.screen.diary.DiaryUiState
import com.example.todoapp.screen.diary.DidntFitRow
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.crown_a
import todoapp.shared.generated.resources.diary_heart_closed
import todoapp.shared.generated.resources.flag_open
import todoapp.shared.generated.resources.heart_filled
import todoapp.shared.generated.resources.star_filled

/** Slightly tilted sticky note, like the mission cards. */
@Composable
fun StickyNote(rotation: Float, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 12.dp)
            .rotate(rotation)
            .shadow(6.dp, RoundedCornerShape(4.dp))
            .background(StickyYellow, RoundedCornerShape(4.dp))
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
                Text("DIDN'T FIT TODAY", fontSize = 11.sp, letterSpacing = 1.2.sp, color = Color(0xFF6A5A2A))
                Text(row.title, fontSize = 19.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light, color = InkBlack)
                Text(row.text, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = Color(0xFF5F5A48))
            }
        }
    }
}

/** Morning buttons: Swap one, Lighter day, Looks good. */
@Composable
fun PlanActions(editMode: Boolean, onEdit: () -> Unit, onLighter: () -> Unit, onLooksGood: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        PlanButton(if (editMode) "Done swapping" else "Swap one", filled = false, onClick = onEdit)
        PlanButton("Lighter day", filled = false, onClick = onLighter)
        PlanButton("Looks good", filled = true, onClick = onLooksGood)
    }
}

@Composable
private fun PlanButton(text: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(if (filled) Modifier.background(Orange180) else Modifier.border(1.5.dp, Orange180, RoundedCornerShape(10.dp)))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (filled) PeachInk else PeachText)
    }
}

/** The evening card, in the style of the old metrics highlights. Never interprets titles. */
@Composable
fun DayHighlightsCard(state: DiaryUiState, date: LocalDate) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(CardCream, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${date.dayOfMonth} ${date.month.name}",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3A3A3A),
                modifier = Modifier.weight(1f)
            )
            Image(
                painterResource(Res.drawable.diary_heart_closed), contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFF6B6B6B)), modifier = Modifier.size(30.dp)
            )
        }
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(Color(0xFFDDD6C2)))
        HighlightLine(Res.drawable.star_filled, "+${state.pointsToday} points today")
        HighlightLine(
            Res.drawable.heart_filled,
            "${state.pillarsTouched} of ${PillarType.scoredPillars.size} pillars got something."
        )
        HighlightLine(
            Res.drawable.crown_a,
            if (state.crownedTotal == 0) "No crowns today." else "${state.crownedDone} of ${state.crownedTotal} crowned done."
        )
    }
}

@Composable
private fun HighlightLine(icon: DrawableResource, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .background(Color(0xFFFDFAF1), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(icon), contentDescription = null,
            colorFilter = ColorFilter.tint(Color(0xFF3A3A3A)), modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(text, fontSize = 15.sp, color = InkBlack)
    }
}
