package com.example.todoapp.screen.missions.editpopup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.screen.basicutils.components.FrequencyPeriodButton
import com.example.todoapp.screen.missions.Orange80
import com.example.todoapp.screen.missions.getPillarColor

@Composable
fun ImportanceSection(
    mission: MutableState<Mission?>,
    pillarSelected: MutableState<Pillar?>,
    pillarTotalValCount: Map<String?, Int>,
    pillarOptions: List<Pillar>,
    fieldBackground: Color
) {
    Column {
        Text(
            text = "IMPORTANCE:",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Pillar value visualization
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                pillarOptions.forEach { pillar ->
                    val pillarValAdded = when(mission.value?.importanceSet) {
                        "LOW" -> 1
                        "MEDIUM" -> 2
                        "HIGH" -> 3
                        else -> 0
                    }

                    // Only show visualization for selected pillar
                    if (pillar.pillarName == pillarSelected.value?.pillarName) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                pillar.pillarName ?: "Pillar",
                                fontSize = 12.sp,
                                modifier = Modifier.width(60.dp)
                            )

                            Row {
                                repeat(pillarValAdded) {
                                    Box(
                                        modifier = Modifier
                                            .height(6.dp)
                                            .width(16.dp)
                                            .padding(horizontal = 1.dp)
                                            .background(
                                                getPillarColor(pillar.pillarName),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }

                                val pillarTotal = pillarTotalValCount[pillar.pillarName] ?: 1
                                repeat(pillarTotal) {
                                    Box(
                                        modifier = Modifier
                                            .height(6.dp)
                                            .width(16.dp)
                                            .padding(horizontal = 1.dp)
                                            .background(
                                                getPillarColor(pillar.pillarName).copy(alpha = 0.5f),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Importance selector buttons
        Surface(
            color = fieldBackground,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ImportanceButton(
                    text = "LOW",
                    isSelected = mission.value?.importanceSet == "LOW",
                    onClick = {
                        mission.value = mission.value?.copy(
                            importanceValue = 0.25f,
                            importanceSet = "LOW"
                        )
                    }
                )

                ImportanceButton(
                    text = "MEDIUM",
                    isSelected = mission.value?.importanceSet == "MEDIUM",
                    onClick = {
                        mission.value = mission.value?.copy(
                            importanceValue = 0.50f,
                            importanceSet = "MEDIUM"
                        )
                    }
                )

                ImportanceButton(
                    text = "HIGH",
                    isSelected = mission.value?.importanceSet == "HIGH",
                    onClick = {
                        mission.value = mission.value?.copy(
                            importanceValue = 0.70f,
                            importanceSet = "HIGH"
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ImportanceButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) Orange80 else Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        FrequencyPeriodButton(
            text = text,
            modifier = Modifier.wrapContentSize()
        )
    }
}