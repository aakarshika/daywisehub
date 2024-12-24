package com.example.todoapp.screen.diary.diaryitem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.screen.basicutils.components.DiaryLineText

@Composable
fun ItemStrikethrough(
    RowHeight: Int,
    todoTask: TodayTask?,
    pillar: Pillar,
    mission: Mission,
    editingMode :String,
    taskErased: (TodayTask?, Boolean) -> Unit,
) {
    val isChecked = todoTask?.taskStatus== "COMPLETED"

    val ld = LocalDensity.current

    Box(modifier = Modifier.fillMaxWidth().height(RowHeight.dp)){
        Row(modifier = Modifier.fillMaxWidth()
        ){
            columnWeights.forEachIndexed { i, item->
                Box(
                    modifier = Modifier.wrapContentHeight().fillMaxWidth()
                        .weight(columnWeights[i])
                ){
                    if(i == 1) {
                        Box(modifier = Modifier
                            .fillMaxSize()
                        ) {
                            DiaryLineText(text = mission.missionTitle)
                        }
                    }
                }
            }
        }
    }
}
