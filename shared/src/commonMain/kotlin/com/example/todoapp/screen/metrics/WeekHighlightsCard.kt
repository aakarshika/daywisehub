package com.example.todoapp.screen.metrics

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.WeekHighlights
import com.example.todoapp.screen.basicutils.CardCream
import com.example.todoapp.screen.basicutils.InkBlack
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.crown_a
import todoapp.shared.generated.resources.heart_filled
import todoapp.shared.generated.resources.metrics_calendar
import todoapp.shared.generated.resources.star_filled

/** The week's highlights, in the style of the diary's evening card. Never interprets titles. */
@Composable
fun WeekHighlightsCard(state: WeekHighlights) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(CardCream, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.header,
                fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp, color = Color(0xFF3A3A3A),
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Image(
                painterResource(Res.drawable.metrics_calendar), contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFF6B6B6B)), modifier = Modifier.size(30.dp)
            )
        }
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(Color(0xFFDDD6C2)))
        HighlightLine(Res.drawable.star_filled, state.points)
        HighlightLine(if (state.balanced) Res.drawable.crown_a else Res.drawable.heart_filled, state.pillars)
        HighlightLine(Res.drawable.crown_a, state.balancedWeeks)
    }
}

/** One line of the card: an icon, then its text, as on the diary's highlights. */
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
