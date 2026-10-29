package com.example.todoapp.screen.diary.diaryitem.components.diarymood

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.factor.DayFactors
import com.example.todoapp.screen.basicutils.SketchInk
import com.example.todoapp.screen.metrics.badges.DayFactorSticker
import com.example.todoapp.screen.metrics.badges.dayFactorLines
import com.example.todoapp.screen.basicutils.DARKGREEN180

/** The things that happened to the day, as icons like the moods: tap to pick, tap again to unpick. Any number can be picked. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayFactorRow(picked: List<String>, onToggle: (icon: String, picked: Boolean) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        DayFactors.all.forEach { (icon, label) ->
            val on = icon in picked
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClickLabel = if (on) "Unpick" else "Pick") { onToggle(icon, !on) }
                    .semantics { contentDescription = label; selected = on }
            ) {
                DayFactorIcon(icon, label, on)
            }
        }
    }
}

/**
 * A picked factor is its sticker, popping in when tapped; the others are its grey line drawing. Falls back
 * to the label's first letter for a name with no drawing.
 */
@Composable
private fun DayFactorIcon(icon: String, label: String, on: Boolean) {
    val pop = remember { Animatable(1f) }
    // Only a change after the row first shows pops, not the page opening with it picked.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(on) {
        if (on && shown) {
            pop.snapTo(0.3f)
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow))
        }
        shown = true
    }
    val lines = dayFactorLines(icon)
    when {
        lines == null -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(20.dp).background(if (on) DARKGREEN180 else Color.LightGray, CircleShape)
        ) {
            Text(label.take(1), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        on -> DayFactorSticker(
            icon, tilt = (icon.length % 5 - 2) * 2f, size = 44.dp,
            modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
        )
        else -> Icon(lines, contentDescription = null, modifier = Modifier.size(44.dp).padding(2.dp), tint = SketchInk)
    }
}
