package com.example.todoapp.screen.metrics.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.example.todoapp.screen.basicblocks.WriteText

@Composable
fun GeneralItemCheckbox(
    todoTask: TodayTaskWithFewDetails?,
    editingMode :String,
    taskErased: (TodayTaskWithFewDetails?, Boolean) -> Unit,
) {
    val isChecked = todoTask?.todayTask?.taskStatus== "COMPLETED"

    val ld = LocalDensity.current

    Box(modifier = Modifier.fillMaxWidth().height(35.dp).padding(horizontal = 20.dp)){

        Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {


            Box(modifier = Modifier.fillMaxWidth().height(35.dp).padding(horizontal = 20.dp)

            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterStart)
                        .padding(horizontal = 10.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.CenterVertically)
                            .clip(CircleShape)
                    ) {
                            CustomCheckbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (isChecked) taskErased(
                                        todoTask,
                                        false
                                    ) else taskErased(todoTask,  true)
                                },
                                modifier = Modifier,
                                checkedColor = Orange80,
                                uncheckedColor = Color.Gray,
                                disabledColor = Color.Gray
                            )
                    }

                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                    ) {
                        WriteText(text = " ")
                    }
                }
            }
        }
    }

}
