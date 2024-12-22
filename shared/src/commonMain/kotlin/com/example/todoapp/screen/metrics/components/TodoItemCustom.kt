package com.example.todoapp.screen.metrics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TodoItemCustom(editingMode : String, magicMode : String, doStuff: (String)-> Unit ) {
    Box(modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 20.dp)){

        Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {

            Box(
                modifier = Modifier.height(40.dp)
                    .width(1.dp)
                    .background(Light_Mint)
                    .align(Alignment.CenterEnd)
            ) {
            }
            Box(
                modifier = Modifier.height(40.dp)
                    .width(1.dp)
                    .background(Light_Mint)
                    .align(Alignment.CenterStart)
            ) {
            }
            Box(modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 20.dp)
            ) {
                Row (
                    modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart)
                        .padding(horizontal = 10.dp)
                ) {
                    if(editingMode == "PrepareButton") {
                        Box(
                            modifier = Modifier.clickable {
                                doStuff("autoPrepareButtonClick")
                            }
                        ) {
                            Text(
                                text = "Auto-Prepare",
                                modifier = Modifier,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    else if(editingMode == "ViewItems") {
                        Box(
                            modifier = Modifier.clickable {
                                doStuff("addRandomTask")
                            }
                        ) {
                            Text(
                                text = " +            ",
                                modifier = Modifier,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        if(magicMode == "CHECK"){

                            Box(
                                modifier = Modifier
                                    .clickable {
                                        doStuff("magicModeDRAW")
                                    }
                            ) {
                                Text(
                                    text = " CHECK ",
                                    modifier = Modifier,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {

                            Box(
                                modifier = Modifier
                                    .clickable {
                                        doStuff("magicModeCHECK")
                                    }
                            ) {
                                Text(
                                    text = " DRAW ",
                                    modifier = Modifier,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clickable {
                                    doStuff("habitEdit")
                                    }
                        ) {
                            Text(
                                text = " Habit? ",
                                modifier = Modifier,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clickable {
                                    doStuff("plannerMode")
                                }
                        ) {
                            Text(
                                text = " Planner ",
                                modifier = Modifier,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                    }
                    else if(editingMode == "Habit?") {
                        Box(
                            modifier = Modifier.clickable {
                                doStuff("doneTaskList")
                            }
                        ) {
                            Text(
                                text = "                     Done  ",
                                modifier = Modifier,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    else if(editingMode == "PrepareItems") {
                        Box(
                            modifier = Modifier.clickable {
                                doStuff("addRandomTask")
                            }
                        ) {
                            Text(
                                text = " +            ",
                                modifier = Modifier,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Box(
                            modifier = Modifier.clickable {
                                doStuff("prepareDoneTaskList")
                            }
                        ) {
                            Text(
                                text = "                     Done  ",
                                modifier = Modifier,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }


                }
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .height(1.dp)
                        .background(Light_Mint)
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 10.dp)
                ) {
                }
            }
        }
    }

}
