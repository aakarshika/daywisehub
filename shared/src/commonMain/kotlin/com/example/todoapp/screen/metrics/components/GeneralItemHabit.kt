package com.example.todoapp.screen.metrics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.basicblocks.CustomCheckbox
import com.example.todoapp.screen.basicblocks.CustomHabitButton
import com.example.todoapp.screen.basicblocks.WriteText
import com.example.todoapp.screen.metrics.ComboTask
import com.example.todoapp.screen.metrics.cal.clickable
import com.example.todoapp.screen.missions.getPillarColor
import com.example.todoapp.screen.missions.getThemeColor
import com.kizitonwose.calendar.core.minusDays
import kotlinx.datetime.LocalDate

@Composable
fun GeneralItemHabit(
    selection: LocalDate,
    hTask: ComboTask,
    taskProgressWeekList:List<TodayTask>?,
    editingMode :String,
    taskIsHabit: (ComboTask?, Boolean) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().height(RowHeight)){
        Row(modifier = Modifier.fillMaxWidth().padding(end = 5.dp), horizontalArrangement = Arrangement.End) {
            (1..6).forEach {
                val boxDate = MyDate.fromLocalDate(selection.minusDays(5 - (it)))
                val t = taskProgressWeekList?.find { it.taskDate.dateString == boxDate.dateString }
                if(it==5){
                    Box(
                        modifier = Modifier
                            .padding(start=4.dp, top = 5.dp)
                            .height(15.dp).width(9.dp).clip(RoundedCornerShape(2.dp))
                            .align(Alignment.CenterVertically)
                            .background(Color.LightGray)
                    ){Box(
                        modifier = Modifier
                            .height(13.dp).width(7.dp).clip(RoundedCornerShape(2.dp))
                            .align(Alignment.Center)
                            .background(
                                if (hTask.todayTask?.taskStatus == "COMPLETED") com.example.todoapp.screen.missions.Orange80
                                else Color.White
                            )
                    )}
                } else {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp, top = 5.dp)
                            .height(13.dp).width(7.dp).clip(RoundedCornerShape(2.dp))
                            .align(Alignment.CenterVertically)
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
