package com.example.todoapp.screen.missions.calendar.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.TodayTaskRepository
import com.kizitonwose.calendar.core.CalendarDay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DayMissionProgressViewModel(
    private val milestoneRepository: MilestoneRepository,
    private val todayTaskRepository: TodayTaskRepository,
    private val missionId: Long
) : ViewModel() {

    val milestones: MutableSharedFlow<List<MilestoneWithDetails>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val tasks: MutableSharedFlow<List<TodayTaskWithFewDetails>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
//    val dayTasks: MutableSharedFlow<Mission?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)


    fun loadMileDetails() {
        viewModelScope.launch {
            milestoneRepository.getAllMilestonesForMission(missionId)
                .collectLatest {
                    Logger.w("milestones: ${it}")
                    milestones.tryEmit(it)
                }
        }
    }

    fun loadTaskDetails() {
        viewModelScope.launch {
            todayTaskRepository.getAllTasksForMission(missionId)
                .collectLatest {
                    Logger.w("tasks: ${it}")
                    tasks.tryEmit(it)
                }
        }
    }
}
