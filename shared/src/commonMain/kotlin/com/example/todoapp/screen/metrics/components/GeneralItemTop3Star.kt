package com.example.todoapp.screen.metrics.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.screen.basicblocks.CustomTop3Button
import com.example.todoapp.screen.metrics.ComboTask
import com.example.todoapp.screen.metrics.cal.clickable

@Composable
fun GeneralItemTop3Star(
    RowHeight: Int,
    todoTask: ComboTask,
    editingMode:String,
    taskIsTop: (ComboTask?, Boolean) -> Unit,
) {
    val isChecked = todoTask?.todayTask?.taskPageTag == "TOP3"

    Box(modifier = Modifier.fillMaxWidth().height(RowHeight.dp)){
        Row(modifier = Modifier.fillMaxWidth()
        ){
            columnWeights.forEachIndexed { i, item->
                Box(
                    modifier = Modifier.wrapContentHeight().fillMaxWidth()
                        .weight(columnWeights[i])
                ){
                    if(i == 0) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(15.dp)
                            ) {
                                CustomTop3Button(
                                    checked = isChecked,
                                    onCheckedChange = {},
                                    modifier = Modifier,
                                    checkedColor = Orange80,
                                    uncheckedColor = if(editingMode == "prioritize") Orange80 else Color.Transparent,
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

