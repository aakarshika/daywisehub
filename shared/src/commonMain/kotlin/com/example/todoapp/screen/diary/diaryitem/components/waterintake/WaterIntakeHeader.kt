package com.example.todoapp.screen.diary.diaryitem.components.waterintake
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.todoapp.screen.basicutils.SketchInk
import com.example.todoapp.screen.metrics.badges.WATER_GLASSES
import com.example.todoapp.screen.metrics.badges.WaterGlassSticker
import com.example.todoapp.screen.metrics.badges.waterGlassLines
import kotlin.random.Random
import kotlinx.datetime.LocalDate

val water_daily_limit = 8

@Composable
fun WaterIntakeHeader(
    waterIntake: Double = 0.0,
    selection: LocalDate,
    upsertWaterIntake : (Double) -> Unit
) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()) {
        Row {
            Column(
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {

                // Popping in only after a tap, not when the page opens.
                var tapped by remember { mutableStateOf(false) }
                // A mix of glasses along the row, the same mix each time a day's page opens.
                val glasses = remember(selection) {
                    List(water_daily_limit) { it % WATER_GLASSES }.shuffled(Random(selection.toEpochDays()))
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(modifier = Modifier.align(Alignment.Center)) {
                        items(water_daily_limit) { glassNumber ->
                            // A filled glass is its sticker; an empty one, its grey line drawing.
                            val glass = glasses[glassNumber]
                            val filled = waterIntake >= glassNumber
                            val pop = remember { Animatable(1f) }
                            LaunchedEffect(filled) {
                                if (filled && tapped) {
                                    pop.snapTo(0.3f)
                                    pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow))
                                }
                            }
                            val modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    tapped = true
                                    upsertWaterIntake((if (glassNumber == waterIntake.toInt()) glassNumber-1 else glassNumber ).toDouble())
                                }
                            if (filled) {
                                WaterGlassSticker(
                                    glass, tilt = (glassNumber * 5 % 7 - 3) * 1.5f, size = 40.dp,
                                    modifier = modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                                )
                            } else {
                                Icon(waterGlassLines(glass), contentDescription = "water-glass", modifier = modifier.padding(2.dp), tint = SketchInk)
                            }
                        }
                    }
                }
            }
        }
    }
}
