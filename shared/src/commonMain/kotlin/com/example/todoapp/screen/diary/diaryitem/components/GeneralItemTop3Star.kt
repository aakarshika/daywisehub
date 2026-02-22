package com.example.todoapp.screen.diary.diaryitem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoapp.screen.basicutils.components.CustomTop3Button
import com.example.todoapp.db.models.TaskType
import com.example.todoapp.screen.diary.ComboTask
import com.example.todoapp.screen.basicutils.Orange80

@Composable
fun GeneralItemTop3Star(
    RowHeight: Int,
    todoTask: ComboTask,
    editingMode:String,
    taskIsTop: (ComboTask?, Boolean) -> Unit,
) {
    val isChecked = todoTask?.todayTask?.taskPageTag == TaskType.TOP3.value

    Box(modifier = Modifier.fillMaxWidth().height(RowHeight.dp)){
        Row(modifier = Modifier.fillMaxWidth()
        ){
            columnWeightsLeftShifted.forEachIndexed { i, item->
                Box(
                    modifier = Modifier.wrapContentHeight().fillMaxWidth()
                        .weight(columnWeightsLeftShifted[i])
                ){
                    if(i == 0) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(14.dp)
                            ) {
                                CustomTop3Button(
                                    checked = isChecked,
                                    onCheckedChange = {},
                                    modifier = Modifier,
                                    checkedColor = Color.Gray,
                                    uncheckedColor = if(editingMode == "prioritize") Color.LightGray else Color.Transparent,
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

