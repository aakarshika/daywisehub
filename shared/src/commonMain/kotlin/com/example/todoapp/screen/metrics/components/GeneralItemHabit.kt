package com.example.todoapp.screen.metrics.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.screen.basicblocks.CustomCheckbox
import com.example.todoapp.screen.basicblocks.CustomHabitButton
import com.example.todoapp.screen.basicblocks.WriteText
import com.example.todoapp.screen.metrics.cal.clickable
import com.example.todoapp.screen.missions.getPillarColor
import com.example.todoapp.screen.missions.getThemeColor

@Composable
fun GeneralItemHabit(
    todoTask: TodayTaskWithFewDetails?,
    editingMode :String,
    taskIsHabit: (TodayTaskWithFewDetails?, Boolean) -> Unit,
) {
    val isChecked = todoTask?.todayTask?.taskPageTag == "HABIT"

    val ld = LocalDensity.current

    Box(modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 20.dp)){
        Row(modifier = Modifier.fillMaxWidth()
            .clickable  {
                if (isChecked) taskIsHabit(todoTask, false)
                else taskIsHabit(todoTask,  true)
            }
        ){
            columnWeights.forEachIndexed { i, item->
                Box(
                    modifier = Modifier.wrapContentHeight().fillMaxWidth()
                        .weight(columnWeights[i])
                ){
                    if(i == 2) {
                        Box(modifier = Modifier.padding(start = 10.dp)) {
                            Column(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                            ) {
                                CustomHabitButton(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (isChecked) taskIsHabit(todoTask, false)
                                        else taskIsHabit(todoTask,  true)
                                    },
                                    modifier = Modifier,
                                    checkedColor =  Color.Transparent,
                                    uncheckedColor = LightGray,
                                    disabledColor = Color.Transparent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

val LightGray = Color(0xEEEEEEEE)
