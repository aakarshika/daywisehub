package com.example.todoapp.screen.metrics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.screen.basicblocks.DiaryLineText
import com.example.todoapp.screen.basicblocks.WriteText
import com.example.todoapp.screen.missions.defaultMissionWithDetails

val RowHeight = 52.dp
@Composable
fun GeneralItem(
                mission: Mission
) {
    Box(modifier = Modifier.fillMaxWidth().height(RowHeight)){
        Row(modifier = Modifier.fillMaxWidth()
        ){
            columnWeights.forEachIndexed { i, item->
                Box(
                    modifier = Modifier.wrapContentHeight().fillMaxWidth()
                        .weight(columnWeights[i])
                ){
                    Box(modifier = Modifier.fillMaxWidth()){
                        Column(modifier = Modifier
                            .height(70.dp)
                            .fillMaxWidth()
                            ) {
                            Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Blue80))
                            Box(modifier = Modifier.height(25.dp).fillMaxWidth())
                            Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Blue80))
                            Box(modifier = Modifier.height(25.dp).fillMaxWidth())
                            Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Blue80))
                        }
                    }
                    if(i == 1) {
                        Box(modifier = Modifier) {
                            DiaryLineText(text = "${mission?.missionTitle ?: ""}")
                        }
                    }
                }
            }
        }
    }
}
