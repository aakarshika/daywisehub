package com.example.todoapp.screen.missions.editpopup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mission.MissionWithDetails
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.screen.missions.components.BottomActionBar
import com.example.todoapp.screen.missions.getPillarColor

/**
 * EditMissionPopup - A modal screen for creating or editing a mission.
 * Supports setting mission importance, frequency, milestones, and pillar.
 */
@Composable
fun OptimizedEditMissionPopup(
    selectedMission: MissionWithDetails,
    pillarOptions: List<Pillar>,
    pillarTotalValCount: Map<String?, Int>,
    onCloseClicked: () -> Unit,
    onSaveClicked: (MissionWithDetails) -> Unit
) {
    // State
    val mission = remember { mutableStateOf(selectedMission.mission) }
    val missionFrequency = remember { mutableStateOf(selectedMission.missionFrequency) }
    val pillarSelected = remember { mutableStateOf(selectedMission.pillar) }
    val milestones = remember { mutableStateOf(selectedMission.milestones) }
    val expandFrequency = remember { mutableStateOf(true) }
    val page1 = remember { mutableStateOf(true)}


    // Pillar colors
    val pillarColor = getPillarColor(pillarSelected.value?.pillarName)
    val fieldBackground = Color.White.copy(alpha = 0.45f)

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandIn(),
        exit = fadeOut() + shrinkOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .fillMaxHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(color = Color.White)
            ) {
                // Main content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(color = pillarColor.copy(alpha = 0.5f))
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
//                    if(page1.value) {
                        // Title & Pillar section
                        MissionHeaderSection(
                            mission = mission,
                            pillarSelected = pillarSelected,
                            pillarOptions = pillarOptions,
                            fieldBackground = fieldBackground
                        )

//                    Spacer(modifier = Modifier.height(16.dp))

//                    // Importance section
//                    ImportanceSection(
//                        mission = mission,
//                        pillarSelected = pillarSelected,
//                        pillarTotalValCount = pillarTotalValCount,
//                        pillarOptions = pillarOptions,
//                        fieldBackground = fieldBackground
//                    )
//                    } else {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Frequency section
                        FrequencySection(
                            mission = mission,
                            missionFrequency = missionFrequency,
                            pillarSelected = pillarSelected,
                            expandFrequency = expandFrequency,
                            fieldBackground = fieldBackground
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Milestones section
                        MilestonesSection(
                            milestones = milestones,
                            mission = mission,
                            fieldBackground = fieldBackground
                        )
//                    }
                    Spacer(modifier = Modifier.height(60.dp))

                    // Bottom action bar
                    BottomActionBar(
                        page1 = page1.value,
                        onCloseClicked = onCloseClicked,
                        onNextClicked = {
                            page1.value = false
                            val updatedMission = MissionWithDetails(
                                mission = mission.value!!,
                                missionFrequency = missionFrequency.value!!,
                                pillar = pillarSelected.value!!,
                                milestones = milestones.value
                            )
    //                        onSaveClicked(updatedMission)
                        },
                        onPrevClicked = {
                            page1.value = true
                            val updatedMission = MissionWithDetails(
                                mission = mission.value!!,
                                missionFrequency = missionFrequency.value!!,
                                pillar = pillarSelected.value!!,
                                milestones = milestones.value
                            )
    //                        onSaveClicked(updatedMission)
                        },
                        onSaveClicked = {
                            val updatedMission = MissionWithDetails(
                                mission = mission.value!!,
                                missionFrequency = missionFrequency.value!!,
                                pillar = pillarSelected.value!!,
                                milestones = milestones.value
                            )
                            onSaveClicked(updatedMission)
                        },
                        pillarColor = pillarColor
                    )
                }
            }
        }
    }
}