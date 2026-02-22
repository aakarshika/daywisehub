package com.example.todoapp.screen.missions.editpopup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
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
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.screen.missions.calendar.blueprint.MissionBlueprintCalendarScreen
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.missions.calendar.progress.DayMissionProgressViewModel
import com.example.todoapp.screen.basicutils.darken
import com.example.todoapp.screen.basicutils.getPillarColor
import org.koin.core.parameter.parametersOf

@Composable
fun FrequencySection(
    mission: MutableState<Mission?>,
    missionFrequency: MutableState<MissionFrequency?>,
    pillarSelected: MutableState<Pillar?>,
    expandFrequency: MutableState<Boolean>,
    fieldBackground: Color
) {
    Surface(
        color = Color.White.copy(alpha = 0.45f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column {
            // Frequency header with expand/collapse button
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "With tackle frequency",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    if (expandFrequency.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle frequency details",
                    tint = Color.Black,
                    modifier = Modifier
                        .clickable { expandFrequency.value = !expandFrequency.value }
                        .size(24.dp)
                )
            }

            // Frequency slider
            FrequencySlider(
                mission = mission,
                pillar = pillarSelected.value,
                onValueChange = { newValue ->

                    // Update frequency set based on slider value
                    val freqSet = when {
                        newValue < 0.25f -> "OCCASIONAL"
                        newValue in 0.25f..0.5f -> "COMFORTABLE"
                        newValue in 0.5f..0.75f -> "BALANCED"
                        else -> "RIGOROUS"
                    }

                    //
                    mission.value = mission.value?.copy(frequencySetValue = newValue)
                    mission.value = mission.value?.copy(frequencySet = freqSet)
                    // OCCASIONAL ka 0-0.25 will be spread into - 1 time yearly and 11 times yearly (reverse calculate once in x months).
                    // COMFORTABLE ka 0.25-0.5 will be spread into - 1 time monthly and 4 times monthly.
                    // BALANCED ka 0.5-0.75 will be spread into - 1 time weekly and 7 times weekly.
                    // RIGOROUS ka 0.75-1 will be spread into - 1 time daily and 10 times daily.
                    // freq value 0-1 is input.
                    // "x times Y" is output. x= integer, y = daily/weekly/monthly/yearly.

                    // Update mission frequency
                    updateMissionFrequency(newValue, mission.value, missionFrequency)
                }
            )

            // Expanded frequency details
            AnimatedVisibility(visible = expandFrequency.value) {
                Column {
                    // Calendar visualization
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            MissionBlueprintCalendarScreen(mission,missionFrequency, pillarSelected,
                                KoinF.di?.get<DayMissionProgressViewModel> { parametersOf(mission.value?.missionId) }!!,
                                )
                        }
                    }
                    // Detailed frequency settings
                    Surface(
                        color = fieldBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // Amount dropdown - shows values based on frequency period
                                var freqAmountExpanded = remember { mutableStateOf(false) }

                                // Determine allowable frequency values based on period
                                val frequencyOptions =
                                    when (missionFrequency.value?.frequencyPeriod) {
                                        "DAILY" -> (1..10).toList()
                                        "WEEKLY" -> (1..7).toList()
                                        "MONTHLY" -> (1..4).toList()
                                        "YEARLY" -> (1..11).toList()
                                        else -> (1..4).toList() // Default to monthly
                                    }

                                Box(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { freqAmountExpanded.value = true },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        )
                                    ) {
                                        Text(
                                            text = missionFrequency.value?.frequency?.toString()
                                                ?: "1",
                                            fontSize = 14.sp
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select frequency amount"
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = freqAmountExpanded.value,
                                        onDismissRequest = { freqAmountExpanded.value = false },
                                        modifier = Modifier.width(70.dp)
                                    ) {
                                        frequencyOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text("${option.toString()}")
                                                },
                                                onClick = {
                                                    missionFrequency.value =
                                                        missionFrequency.value?.copy(frequency = option)
                                                    updateFrequencyFromDetail(
                                                        mission,
                                                        missionFrequency
                                                    )
                                                    freqAmountExpanded.value = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Frequency unit dropdown (TIMES, HOURS)
                                var unitExpanded = remember { mutableStateOf(false) }
                                val frequencyUnits = listOf("TIMES", "HOURS")

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .width(100.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { unitExpanded.value = true },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = getPillarColor(pillarSelected.value?.pillarName).darken(),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        )
                                    ) {
                                        Text(
                                            text = missionFrequency.value?.frequencyUnit ?: "TIMES",
                                            fontSize = 14.sp
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select frequency unit"
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = unitExpanded.value,
                                        onDismissRequest = { unitExpanded.value = false },
                                        modifier = Modifier.width(100.dp)
                                    ) {
                                        frequencyUnits.forEach { unit ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(text = unit)
                                                },
                                                onClick = {
                                                    missionFrequency.value =
                                                        missionFrequency.value?.copy(frequencyUnit = unit)
                                                    updateFrequencyFromDetail(
                                                        mission,
                                                        missionFrequency
                                                    )
                                                    unitExpanded.value = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Frequency period dropdown (DAILY, WEEKLY, MONTHLY, YEARLY)
                                var periodExpanded = remember { mutableStateOf(false) }
                                val frequencyPeriods =
                                    listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .width(110.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { periodExpanded.value = true },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = getPillarColor(pillarSelected.value?.pillarName).darken(),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        )
                                    ) {
                                        Text(
                                            text = missionFrequency.value?.frequencyPeriod
                                                ?: "WEEKLY",
                                            fontSize = 14.sp
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select frequency period"
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = periodExpanded.value,
                                        onDismissRequest = { periodExpanded.value = false },
                                        modifier = Modifier.width(110.dp)
                                    ) {
                                        frequencyPeriods.forEach { period ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(text = period)
                                                },
                                                onClick = {
                                                    // Update period
                                                    val newPeriod = period

                                                    // Ensure frequency is within valid range for new period
                                                    val currentFreq =
                                                        missionFrequency.value?.frequency ?: 1
                                                    val newFreq = when (newPeriod) {
                                                        "DAILY" -> currentFreq.coerceIn(1, 10)
                                                        "WEEKLY" -> currentFreq.coerceIn(1, 7)
                                                        "MONTHLY" -> currentFreq.coerceIn(1, 4)
                                                        "YEARLY" -> currentFreq.coerceIn(1, 11)
                                                        else -> currentFreq
                                                    }

                                                    // Update frequency object
                                                    missionFrequency.value =
                                                        missionFrequency.value?.copy(
                                                            frequencyPeriod = newPeriod,
                                                            frequency = newFreq,
                                                            isDailyHabit = newPeriod == "DAILY"
                                                        )

                                                    updateFrequencyFromDetail(
                                                        mission,
                                                        missionFrequency
                                                    )
                                                    periodExpanded.value = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Collapsed frequency summary
            if (!expandFrequency.value) {
                Surface(
                    color = fieldBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Approx. ${missionFrequency.value?.frequency ?: 1} " +
                                "${missionFrequency.value?.frequencyUnit ?: "TIMES"} " +
                                "${missionFrequency.value?.frequencyPeriod ?: "WEEKLY"}",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}