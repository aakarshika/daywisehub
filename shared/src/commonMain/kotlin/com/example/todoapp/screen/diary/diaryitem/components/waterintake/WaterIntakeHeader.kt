package com.example.todoapp.screen.diary.diaryitem.components.waterintake
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.example.todoapp.screen.diary.diaryitem.components.Blue80
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.happy


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
                Box(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(modifier = Modifier.align(Alignment.Center)) {
                        items( listOf(1,2,3,4,5,6,7,8)) {
                            val glassNumber = it
                            Logger.e("glass ${glassNumber}")
                            Icon(
                                tint = if (waterIntake >= glassNumber) Blue80 else Color.LightGray,
                                painter = painterResource(Res.drawable.happy),
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(3.dp)
                                    .clickable {
                                        upsertWaterIntake(glassNumber.toDouble())
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
