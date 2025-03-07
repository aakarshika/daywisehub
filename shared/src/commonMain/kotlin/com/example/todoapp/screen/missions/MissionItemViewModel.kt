package com.example.todoapp.screen.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MissionItemViewModel(
    private val missionRepository: MissionRepository,
    private val missionId: Long
) : ViewModel() {

    val mission: MutableSharedFlow<Mission?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val pillar: MutableSharedFlow<Pillar?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val missionFrequency: MutableSharedFlow<MissionFrequency?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val milestones: MutableSharedFlow<List<Milestone>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)


    fun loadMissionDetails() {
        viewModelScope.launch {
            missionRepository.getMission(missionId)
                .collectLatest {
                    Logger.w("loading mission for item: $it")
                    mission.tryEmit(it)
                }
        }
        viewModelScope.launch {
            missionRepository.getPillar(missionId)
                .collectLatest {
                    Logger.w("loading pillar for item: $it")
                    pillar.tryEmit(it)
                }
        }
        viewModelScope.launch {
            missionRepository.getMissionFrequency(missionId)
                .collectLatest {
                    Logger.w("loading missionFreq for item: $it")
                    missionFrequency.tryEmit(it)
                }
        }
        viewModelScope.launch {
            missionRepository.getMilestones(missionId)
                .collectLatest {
                    Logger.w("loading milestones for item: $it")
                    milestones.tryEmit(it)
                }
        }
    }
}
