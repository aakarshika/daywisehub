package com.example.todoapp.screen.diary.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.basicutils.components.WriteText
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.Orange180
import com.example.todoapp.screen.basicutils.Orange20
import com.example.todoapp.screen.basicutils.Orange40
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.pencil
import todoapp.shared.generated.resources.wand


@Composable
fun Planner(planningMode: MutableState<Boolean>, todaysDate: LocalDate, back : () -> Unit, done : () -> Unit) {
    Box {
        if (!planningMode.value) {
            Box(
                modifier = Modifier
                    .size(500.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    planningMode.value = true
                                }
                                .wrapContentSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Blue80)
                                .padding(20.dp)
                        ) {
                            Icon(
                                painterResource(Res.drawable.wand),
                                tint = Color.White,
                                contentDescription = "Magic wand icon",
                                modifier = Modifier.size(30.dp))
                            WriteText("  Let's plan your day", fontSize = 20f, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(16.dp)
                    .background(
                        color = Orange40,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Let's pla",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
//                            IconButton(
//                                onClick = { back() },
//                                modifier = Modifier
//                                    .size(36.dp)
//                                    .background(
//                                        color = MaterialTheme.colorScheme.surfaceVariant,
//                                        shape = CircleShape
//                                    )
//                            ) {
//                                Icon(
//                                    Icons.Default.Close,
//                                    contentDescription = "Close",
//                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                            }

                            Button(
                                onClick = { done() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Orange180
                                ),
                                shape = RoundedCornerShape(8.dp),
                                elevation =  ButtonDefaults. buttonElevation(5.dp)
                            ) {
                                Text("Done")
                            }
                        }
                    }

                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Box(
                        modifier = Modifier
                            .wrapContentHeight()
                    ) {
                        PlanningList(
                            todaysDate,
                            KoinF.di?.get<PlanningViewModel>()!!
                        )
                    }
                }
            }
        }
    }
}
