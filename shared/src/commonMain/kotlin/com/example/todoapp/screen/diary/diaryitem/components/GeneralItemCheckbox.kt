package com.example.todoapp.screen.diary.diaryitem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoapp.screen.basicutils.components.CustomCheckbox
import com.example.todoapp.screen.diary.ComboTask
import com.example.todoapp.screen.basicutils.cal.clickable
import com.example.todoapp.screen.missions.getPillarColor

@Composable
fun GeneralItemCheckbox(
    RowHeight: Int,
    ct: ComboTask?,
    editingMode :String,
    taskErased: (ComboTask?, Boolean) -> Unit,
) {
    val isChecked = ct?.todayTask?.taskStatus== "COMPLETED"

    Box(modifier = Modifier.fillMaxWidth().height(RowHeight.dp)){
        Row(modifier = Modifier.fillMaxWidth()
        ){
            columnWeights.forEachIndexed { i, item->
                Box(
                    modifier = Modifier.wrapContentHeight().fillMaxWidth()
                        .weight(columnWeights[i])
                ){
                    if(i == 0) {
                        Box(modifier = Modifier
                            .fillMaxWidth().height(RowHeight.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth().height(RowHeight.dp)
                                    .clickable {
                                        if (isChecked) taskErased(
                                            ct,
                                            false
                                        ) else taskErased(ct,  true)
                                    }
                                    .padding(start = 10.dp, top = 5.dp)
                            ) {
                                CustomCheckbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (isChecked) taskErased(
                                            ct,
                                            false
                                        ) else taskErased(ct,  true)
                                    },
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                    ,
                                    checkedColor = getPillarColor(ct?.pillar?.pillarName),
                                    uncheckedColor = Color.Transparent,
                                    disabledColor = Color.Transparent
                                )
                            }
                        }
                    }
                    if(i == 2){
                        Box(modifier = Modifier
                            .fillMaxWidth().height(RowHeight.dp)
                        ) {
                            ProgressBarMetrics(
                                taskProgress = (ct?.todayTask?.taskProgressVal ?: 0f)* totalProgressWidth,
                                missionFrequency = ct?.missionFrequency!!
                            )
                        }
                    }
                }
            }
        }
    }
}
