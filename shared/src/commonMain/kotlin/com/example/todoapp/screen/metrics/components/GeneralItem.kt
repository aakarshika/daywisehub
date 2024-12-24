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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.screen.basicblocks.DiaryLineText
import com.example.todoapp.screen.basicblocks.WriteText
import com.example.todoapp.screen.missions.defaultMissionWithDetails

@Composable
fun GeneralItem(
                RowHeight: Int,
                mission: Mission,
                textArranged: (Int) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().height(RowHeight.dp)){
        Row(modifier = Modifier.fillMaxWidth()
        ){
            columnWeights.forEachIndexed { i, item->
                Box(
                    modifier = Modifier.wrapContentHeight().fillMaxWidth()
                        .weight(columnWeights[i])
                ){
                    Box(modifier = Modifier.fillMaxWidth()){
                        Column(modifier = Modifier
                            .height((RowHeight+5).dp)
                            .fillMaxWidth()
                            ) {
                                (1..(RowHeight/26)).forEach {
                                    NotebookLine()
                                }
                            NotebookLine()
                            Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Blue80))
                        }
                    }
                    if(i == 1) {
                        Box(modifier = Modifier) {
                            DiaryLineText(text = mission.missionTitle,
                                onTextLayout = {textLayoutResult->
                                    if(textLayoutResult.lineCount>1) {
                                        textArranged(
                                            textLayoutResult.lineCount
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotebookLine() {
    Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Blue80))
    Box(modifier = Modifier.height(25.dp).fillMaxWidth())
}
