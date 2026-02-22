package com.example.todoapp.screen.missions

import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import com.example.todoapp.db.data.mission.MissionWithDetails
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.di.KoinF
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlin.random.Random

class MissionsControllerViewModel constructor(
    private val missionRepository: MissionRepository
) : ViewModel() {
    val randPaddings = List(100) {
        Pair(Random.nextInt(0, 31).dp, Random.nextInt(0, 11).dp)
    }
    private val _missionIds = MutableSharedFlow<List<Long>>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val missionIds: SharedFlow<List<Long>> get() = _missionIds

    private val _selectedMissionId = MutableSharedFlow<Long?>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val selectedMissionId: SharedFlow<Long?> get() = _selectedMissionId

    private val _selectedMission = MutableSharedFlow<MissionWithDetails?>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val selectedMission: SharedFlow<MissionWithDetails?> get() = _selectedMission



    fun insertFullMission(updatedMission: MissionWithDetails) {
        viewModelScope.launch {
            val newMissionId = missionRepository.insertFullMission(updatedMission)
            val userId = KoinF.di?.get<CalDiaryViewModel>()?.userId ?: -1L
            loadMissionIds(userId)
        }
    }

    fun resetSelectedMissionId()  {
        _selectedMissionId.tryEmit(null)
    }
    fun selectMissionId(missionId: Long)  {
        _selectedMissionId.tryEmit(missionId)
    }
    fun resetSelectedMission()  {
        _selectedMission.tryEmit(null)
    }
    fun selectMission(mission: MissionWithDetails)  {
        _selectedMission.tryEmit(mission)
    }

    fun updateFullMission(updatedMission: MissionWithDetails) {
        viewModelScope.launch {
            updatedMission.mission?.let { missionRepository.updateMission(it) }
            updatedMission.pillar?.let { missionRepository.updatePillarMapping(updatedMission.mission?.missionId?:0L, it) }
            updatedMission.missionFrequency?.let { missionRepository.updateMissionFrequency(it, updatedMission.mission?.missionId?:0L) }
            updatedMission.milestones?.let { missionRepository.updateMilestones(it, updatedMission.mission?.missionId?:0L) }
        }
    }

    // Load function to get all pillars
//    fun loadPillars() {
//        viewModelScope.launch {
//            missionRepository.getAllPillars()
//                .map { AllPillarsListUiState(it) }
//                .collectLatest {
//                    _pillars.tryEmit(it)
//                }
//        }
//    }


    fun loadMissionIds(userId: Long) {
        viewModelScope.launch {
            missionRepository.getAllMissionIds(userId)
                .collect { missionList ->
                    _missionIds.tryEmit(missionList)
                }
        }
    }

}
