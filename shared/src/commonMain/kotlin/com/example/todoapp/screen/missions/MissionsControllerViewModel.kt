package com.example.todoapp.screen.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import com.example.todoapp.db.data.mission.MissionWithDetails
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.repo.MissionRepository
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest

class MissionsControllerViewModel constructor(
    private val missionRepository: MissionRepository
) : ViewModel() {

    val pillars = listOf(
            Pillar(
                pillarId = 1L,
                pillarUserId = 1L,
                pillarDescription = "Take care of your body. It is the only one you have!",
                pillarBenefits = "Proper attention to physical wellness at least 3-5 times a week is recommended for our body",
                pillarName = "HEALTH",
                uiData = "health_icon",
                pillarFrequency = 4,
                pillarFrequencyPeriod = "WEEKLY",
                pillarFrequencyUnit = "TIMES",
                pillarRelativeImportance = 10
            ),
            Pillar(
                pillarId = 2L,
                pillarUserId = 1L,
                pillarDescription = "Money isn't everything, but it is some things..",
                pillarBenefits = "Improve your skills, stay connected, and constantly walk towards your dreams",
                pillarName = "WEALTH",
                uiData = "wealth_icon",
                pillarFrequency = 3,
                pillarFrequencyPeriod = "WEEKLY",
                pillarFrequencyUnit = "TIMES",
                pillarRelativeImportance = 7
            ),
            Pillar(
                pillarId = 3L,
                pillarUserId = 1L,
                pillarDescription = "We all need love from our family, friends, neighbours, and internet trolls..",
                pillarBenefits = "Keep your social life alive by making it a priority",
                pillarName = "LOVE",
                uiData = "love_icon",
                pillarFrequency = 1,
                pillarFrequencyPeriod = "DAILY",
                pillarFrequencyUnit = "TIMES",
                pillarRelativeImportance = 10
            ),
            Pillar(
                pillarId = 4L,
                pillarUserId = 1L,
                pillarDescription = "Meditate, Create, Dream, Build, Destroy..",
                pillarBenefits = "Mental Peace needs attention. Multi-dimensional growth is needed to feel sustained and alive",
                pillarName = "LIFE",
                uiData = "life_icon",
                pillarFrequency = 4,
                pillarFrequencyPeriod = "WEEKLY",
                pillarFrequencyUnit = "TIMES",
                pillarRelativeImportance = 8
            )
    )

    private val _missionIds = MutableSharedFlow<List<Long>>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val missionIds: SharedFlow<List<Long>> get() = _missionIds

    private val _selectedMissionId = MutableSharedFlow<Long?>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val selectedMissionId: SharedFlow<Long?> get() = _selectedMissionId

    private val _selectedMission = MutableSharedFlow<MissionWithDetails?>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val selectedMission: SharedFlow<MissionWithDetails?> get() = _selectedMission



    fun insertFullMission(updatedMission: MissionWithDetails) {
        viewModelScope.launch {
            val newMissionId = missionRepository.insertFullMission(updatedMission)
            loadMissionIds()
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


    fun loadMissionIds() {
        viewModelScope.launch {
            missionRepository.getAllMissionIds()
                .collect { missionList ->
                    _missionIds.tryEmit(missionList)
                }
        }
    }

}
