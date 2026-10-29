package com.example.todoapp.screen.diary.diaryitem.components.dateheader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todoapp.screen.basicutils.components.WriteText
import com.example.todoapp.screen.diary.WeekCalendarScreen
import com.example.todoapp.screen.basicutils.Orange80
import com.example.todoapp.screen.basicutils.Red80
import com.example.todoapp.screen.basicutils.getOrdinal
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@Composable
fun DateHeader(cDate: LocalDate,
               changeDate: (LocalDate) -> Unit) {

    var diaryViewMode by remember { mutableStateOf("week") }

    val swipeDistance = with(LocalDensity.current) { 60.dp.toPx() }
    Box(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .pointerInput(cDate) {
            // Swipe left for the next day, right for the day before.
            var dragged = 0f
            detectHorizontalDragGestures(
                onDragStart = { dragged = 0f },
                onDragEnd = {
                    if (dragged <= -swipeDistance) changeDate(cDate.plus(1, DateTimeUnit.DAY))
                    else if (dragged >= swipeDistance) changeDate(cDate.minus(1, DateTimeUnit.DAY))
                },
                onHorizontalDrag = { change, amount ->
                    change.consume()
                    dragged += amount
                }
            )
        }) {
        Column(modifier = Modifier.padding(top = 20.dp, start = 20.dp)) {
            Row(
                modifier = Modifier
                    .then(
                        if (diaryViewMode == "month") Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { diaryViewMode = "week" }
                        else Modifier
                    )
            ) {
                val daa = getOrdinal(cDate.dayOfMonth)
                WriteText(""+daa.substring(0,daa.length-2), fontSize = 22f, color = Color.Gray, fontWeight = FontWeight.Normal)
                WriteText(""+daa.substring(daa.length-2), fontSize = 17f, color = Color.Gray, fontWeight = FontWeight.Normal)
                WriteText(" "+cDate.month.toString().substring(0,1), fontSize = 22f, color = Color.Gray, fontWeight = FontWeight.Normal)
                WriteText(""+cDate.month.toString().lowercase().substring(1)+",", fontSize = 19f, color = Color.Gray, fontWeight = FontWeight.Normal)
                WriteText(" "+cDate.year, fontSize = 22f, color = Color.Gray, fontWeight = FontWeight.Normal)
            }

            Row {
                Row(
                    modifier = Modifier
                        .padding(10.dp)
                        .then(
                            if (diaryViewMode == "month") Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { diaryViewMode = "week" }
                            else Modifier
                        )
                ) {
                    WriteText("" + cDate.dayOfWeek.name.substring(0,1), fontSize = 24f, fontWeight = FontWeight.Normal, color = Color.Gray)
                    WriteText("" + cDate.dayOfWeek.name.lowercase().substring(1), fontSize = 20f, fontWeight = FontWeight.Normal, color = Color.Gray)
                }
                WeekCalendarScreen(
                    selectedDate = cDate,
                    changeDate = changeDate,
                    diaryViewMode = diaryViewMode,
                    onViewModeChange = { diaryViewMode = it }
                )
            }
        }
    }
}
@Composable
private fun SpringGraphic() {
    Box(modifier = Modifier.fillMaxWidth().height(50.dp)) {

        Box(modifier = Modifier.align(Alignment.TopEnd).wrapContentSize()) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Face",
                tint = Red80,
                modifier = Modifier.size(38.dp)
            )
        }
        Box(modifier = Modifier.padding(end = 5.dp).align(Alignment.TopEnd).wrapContentSize()) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Face",
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }

        Box(modifier = Modifier.padding(end = 10.dp).align(Alignment.TopEnd).wrapContentSize()) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Face",
                tint = Orange80,
                modifier = Modifier.size(38.dp)
            )
        }
        Box(modifier = Modifier.padding(end = 15.dp).align(Alignment.TopEnd).wrapContentSize()) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Face",
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
        Box(modifier = Modifier.padding(end = 20.dp).align(Alignment.TopEnd).wrapContentSize()) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Face",
                tint = Red80,
                modifier = Modifier.size(38.dp)
            )
        }
        Box(modifier = Modifier.padding(end = 25.dp).align(Alignment.TopEnd).wrapContentSize()) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Face",
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
        Box(modifier = Modifier.padding(end = 30.dp).align(Alignment.TopEnd).wrapContentSize()) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Face",
                tint = Orange80,
                modifier = Modifier.size(38.dp)
            )
        }
        Box(modifier = Modifier.padding(end = 35.dp).align(Alignment.TopEnd).wrapContentSize()) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Face",
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
        Box(modifier = Modifier.padding(end = 40.dp).align(Alignment.TopEnd).wrapContentSize()) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Face",
                tint = Red80,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}
