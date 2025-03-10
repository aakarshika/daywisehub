package com.example.todoapp.screen.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.MissionWithDetails
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.missions.editpopup.OptimizedEditMissionPopup
import org.jetbrains.compose.resources.painterResource
import org.koin.core.parameter.parametersOf
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.plus_sign

@Composable
fun MissionsControllerScreen(viewModel: MissionsControllerViewModel) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {


        val missionIds: List<Long> by viewModel.missionIds.collectAsState(emptyList())

        val selectedMissionId = viewModel.selectedMissionId.collectAsState(null)
        val selectedMission = viewModel.selectedMission.collectAsState(null)

        val triggerMissionId = remember { mutableStateOf(-1L) }
        val tt = remember { mutableStateOf<MissionWithDetails?>(null) }
        val pillarOptions = pillars

//        val pillarCountMap = missionIds.itemList.groupBy { it.pillar?.pillarName }
//            .mapValues { missionList->
//                missionList.value.sumOf { mis->
//                    if(mis.mission.importanceSet =="LOW")
//                        1 else if(mis.mission.importanceSet =="MEDIUM")
//                        2 else if(mis.mission.importanceSet =="HIGH") 3
//                    else 0 .toInt()
//                }
//            }

        val editMissionMode = remember { mutableStateOf("IDLE") }

        // Load mission IDs when the screen is composed
        LaunchedEffect(Unit) {
//            viewModel.loadPillars()
            viewModel.loadMissionIds()
        }

        var isAddMissionPopupVisible = remember { mutableStateOf(false) }

        LazyColumn(modifier = Modifier.padding(start = 40.dp),
            verticalArrangement = Arrangement.spacedBy(-15.dp)
        ) {
            item{
                Column {
                    Text(text = "Missions", fontSize = 24.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(15.dp))
                }
            }
            itemsIndexed(missionIds) { i, missionId ->
                val missionItemViewModel = KoinF.di?.get<MissionItemViewModel> { parametersOf(missionId) }
                if (missionItemViewModel != null) {
                    MissionItem(
                        missionId = missionId,
                        missionItemViewModel = missionItemViewModel,
                        isSelected = selectedMissionId.value == missionId,
                        isTrigger = triggerMissionId.value == missionId,
                        modifier = Modifier
                            .padding(
                                start = viewModel.randPaddings[i].first,
                                top = viewModel.randPaddings[i].second
                            ),
                        onEditClick = { m->
                            viewModel.selectMission(m)
                            triggerMissionId.value = 0L
                            editMissionMode.value = "EDIT"
                        },
                        onViewClick = { mId->
                            viewModel.selectMissionId(mId)
                        },
                        onCloseClick = {
                            viewModel.resetSelectedMissionId()
                            editMissionMode.value = "IDLE"
                        })
                }
            }
            item {
                Spacer(modifier = Modifier.height(255.dp))
            }
        }

        // Button to show the Add Mission Popup
        FloatingActionButton(
            onClick = { isAddMissionPopupVisible.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 100.dp)
        ) {
            Icon(painterResource(Res.drawable.plus_sign), tint = Color.Gray, contentDescription = "Add Mission", modifier = Modifier.size(25.dp))
        }

        // Show Add Mission Popup
        if (isAddMissionPopupVisible.value) {
            AddMissionPopup(
                pillarOptions,
                pillarTotalValCount = mapOf("HEALTH" to 0),
                onCloseClicked = {
                    isAddMissionPopupVisible.value = false
                }, onSaveClicked = { updatedMission ->
                    viewModel.insertFullMission(updatedMission)
                    isAddMissionPopupVisible.value = false
                }
            )
        }

        if(editMissionMode.value == "EDIT"){
            selectedMission.value?.let { mis ->
                if(pillarOptions.isNotEmpty()){
                    OptimizedEditMissionPopup(
                        mis,
                        pillarOptions,
                        pillarTotalValCount = mapOf("HEALTH" to 0),
                        onCloseClicked = {
                            editMissionMode.value = "IDLE"
                            viewModel.resetSelectedMission()
                            triggerMissionId.value = mis.mission?.missionId?:0L
                        }, onSaveClicked = { updatedMission ->
                            viewModel.updateFullMission(updatedMission)
                            viewModel.resetSelectedMission()
                            triggerMissionId.value = mis.mission?.missionId?:0L
                            editMissionMode.value = "IDLE"
                        }
                    )
                }
            }
        }
    }
}
