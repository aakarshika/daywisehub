package com.example.todoapp.screen.diary.diaryitem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.example.todoapp.screen.diary.ComboTask
import com.example.todoapp.screen.missions.Blue80
import kotlinx.datetime.LocalDate

@Composable
fun MenuItem(
    selection: LocalDate,
    RowHeight: Int,
    editingMode :String,
    prioritizeClicked: ( String ) -> Unit,
    addRandomTask: (  ) -> Unit,
    generateTodaysTasks: (  ) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(RowHeight.dp)){
        Row(modifier = Modifier.fillMaxWidth()
        ){
            columnWeights.forEachIndexed { i, item->
                Box(
                    modifier = Modifier.wrapContentHeight().fillMaxWidth()
                        .weight(columnWeights[i])
                ){
                    if(i == 1) {
                        Box(modifier = Modifier) {
                            Row {
                                Box(modifier = Modifier.clickable {
                                    generateTodaysTasks()
                                }) { Text("               $$ ") }
                                Box(modifier = Modifier.clickable {
                                    addRandomTask()
                                }) { Text("     +    ") }

                                Box(modifier = Modifier.clickable {
                                    Logger.e("Is Priority?")
                                    if (editingMode == "ViewItems")
                                        prioritizeClicked("prioritize")
                                    else
                                        prioritizeClicked("ViewItems")
                                }) { Text(if (editingMode == "prioritize") "                  Done " else "    Prioritize ") }
                            }
                        }
                    }
                }
            }
        }
    }
}
