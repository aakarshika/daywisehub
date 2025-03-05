package com.example.todoapp.screen.diary.diaryitem.components.dateheader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.screen.basicutils.components.WriteText
import kotlinx.datetime.LocalDate

@Composable
fun WeekDayHeader(cDate: LocalDate) {

    Box(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()) {
        Row {
            Column(
                modifier = Modifier.weight(1f)
                    .padding( 10.dp)
            ) {
                WriteText(""+cDate.dayOfWeek.name, fontSize = 24f)
            }
        }
    }
}