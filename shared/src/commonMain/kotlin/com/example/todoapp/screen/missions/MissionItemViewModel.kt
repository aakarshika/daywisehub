package com.example.todoapp.screen.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionWithDetails
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.repo.MissionRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MissionItemViewModel(
    private val missionRepository: MissionRepository,
    private val missionId: Long
) : ViewModel() {

    private val _mission: MutableSharedFlow<Mission?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val mission: SharedFlow<Mission?> = _mission

    private val _pillar: MutableSharedFlow<Pillar?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val pillar: SharedFlow<Pillar?> = _pillar

    private val _missionFrequency: MutableSharedFlow<MissionFrequency?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val missionFrequency: SharedFlow<MissionFrequency?> = _missionFrequency

    private val _milestones: MutableSharedFlow<List<Milestone>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val milestones: SharedFlow<List<Milestone>?> = _milestones


    fun loadMissionDetails() {
        viewModelScope.launch {
            missionRepository.getMission(missionId)
                .collectLatest {
                    _mission.tryEmit(it)
                }
        }
        viewModelScope.launch {
            missionRepository.getPillar(missionId)
                .collectLatest {
                    _pillar.tryEmit(it)
                }
        }
        viewModelScope.launch {
            missionRepository.getMissionFrequency(missionId)
                .collectLatest {
                    _missionFrequency.tryEmit(it)
                }
        }
        viewModelScope.launch {
            missionRepository.getMilestones(missionId)
                .collectLatest {
                    _milestones.tryEmit(it)
                }
        }
    }
}
