package com.example.todoapp.screen.diary.diaryitem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.diary.ComboTask
import com.example.todoapp.screen.missions.Blue80
import com.kizitonwose.calendar.core.minusDays
import kotlinx.datetime.LocalDate

@Composable
fun GeneralItemHabit(
    RowHeight: Int,
    selection: LocalDate,
    hTask: ComboTask,
    taskProgressWeekList:List<TodayTask>?,
    editingMode :String,
    taskIsHabit: (ComboTask?, Boolean) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().height(RowHeight.dp).padding(end = 10.dp)){
        Row(modifier = Modifier.fillMaxWidth().height(RowHeight.dp).align(Alignment.TopEnd).padding(top= 1.dp,end = 5.dp), horizontalArrangement = Arrangement.End) {
            (1..6).forEach {
                val boxDate = MyDate.fromLocalDate(selection.minusDays(5 - (it)))
                val t = taskProgressWeekList?.find { it.taskDate.dateString == boxDate.dateString }
                if(it==5){
                    Box(
                        modifier = Modifier
                            .height(7.dp).width(9.dp).clip(RoundedCornerShape(2.dp))
                            .background(Blue80)
                    ){Box(
                        modifier = Modifier
                            .height(5.dp).width(7.dp).clip(RoundedCornerShape(2.dp))
                            .align(Alignment.Center)
                            .background(
                                if (hTask.todayTask?.taskStatus == "COMPLETED") com.example.todoapp.screen.missions.Orange80
                                else Color.White
                            )
                    )}
                } else {
                    Box(
                        modifier = Modifier
                            .height(5.dp).width(7.dp).clip(RoundedCornerShape(2.dp))
                            .background(
                                if (t?.taskStatus == "COMPLETED") com.example.todoapp.screen.missions.Orange80
                                else Color.White
                            )
                    )
                }
            }
        }
    }
}

val LightGray = Color(0xEEEEEEEE)
