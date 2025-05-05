package com.example.todoapp.screen.diary.diaryitem.components.waterintake
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.example.todoapp.screen.missions.Blue180
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.glass_a
import todoapp.shared.generated.resources.glass_b
import todoapp.shared.generated.resources.glass_c
import todoapp.shared.generated.resources.happy

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

                val icons = listOf(
                    Res.drawable.glass_a, Res.drawable.glass_b,
                    Res.drawable.glass_c  )
                var iconss = icons
                listOf(1, water_daily_limit/3).forEach {
                    iconss = iconss+ icons
                }
                val randomIcon = remember { iconss.shuffled() }
                Box(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(modifier = Modifier.align(Alignment.Center)) {
                        items( water_daily_limit) { glassNumber->
                            Icon(
                                tint = if (waterIntake >= glassNumber) Blue180 else Color.LightGray,
                                painter = painterResource(randomIcon[glassNumber]),
                                modifier = Modifier
                                    .height(40.dp)
                                    .wrapContentWidth()
                                    .padding(3.dp)
                                    .clickable {
                                        upsertWaterIntake((if (glassNumber == waterIntake.toInt()) glassNumber-1 else glassNumber ).toDouble())
                                    },
                                contentDescription = "mood-icon-smile"
                            )
                        }
                    }
                }
            }
        }
    }
}
