package com.example.todoapp.screen.diary.diaryitem.components.dateheader

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoapp.screen.basicutils.components.WriteText
import com.example.todoapp.screen.diary.WeekCalendarScreen
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.Red80
import com.example.todoapp.screen.missions.Orange80
import com.example.todoapp.screen.missions.getOrdinal
import kotlinx.datetime.LocalDate

@Composable
fun DateHeader(cDate: LocalDate,
               changeDate: (LocalDate) -> Unit) {

    Box(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()) {
        Row {
            Column(
                modifier = Modifier.weight(1f)
                    .padding( 10.dp)
            ) {
                WriteText(""+getOrdinal(cDate.dayOfMonth) +" "+cDate.month+", "+cDate.year, fontSize = 22f)
            }
            Column(
                modifier = Modifier.weight(1f)
                    .padding()
            ) {

                WeekCalendarScreen(cDate, changeDate)
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
