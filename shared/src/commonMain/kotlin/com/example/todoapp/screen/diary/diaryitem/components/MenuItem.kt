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
import kotlinx.datetime.LocalDate

@Composable
fun MenuItem(
    selection: LocalDate,
    RowHeight: Int,
    editingMode :String,
    prioritizeClicked: ( String ) -> Unit,
    addRandomTask: (  ) -> Unit,
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
                                    Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(
                                        Blue80
                                    ))
                                    Box(modifier = Modifier.height(25.dp).fillMaxWidth())
                                }
                                Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(
                                    Blue80
                                ))
                                Box(modifier = Modifier.height(25.dp).fillMaxWidth())
                                Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(
                                    Blue80
                                ))
                        }
                    }
                    if(i == 1) {
                        Box(modifier = Modifier) {
                            Row {
                                Box(modifier = Modifier.clickable {
                                    addRandomTask()
                                }) { Text("               +    ") }

//            Box(modifier = Modifier.clickable {
//                Logger.e("check/draw")
//                if (magicMode.value == "CHECK")
//                    magicMode.value = "DRAW" else
//                    magicMode.value = "CHECK"
//            }) { Text(if (magicMode.value == "CHECK") " CHECK " else " DRAW ") }

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
