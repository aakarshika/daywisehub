package com.example.todoapp.screen.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.TaskState
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class DiaryViewModel(
    missionRepository: MissionRepository,
    todayTaskRepository: TodayTaskRepository,
    private val planRepository: PlanRepository,
    private val milestoneRepository: MilestoneRepository,
    private val currentDate: LocalDate,
) : ViewModel() {

    private val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val isToday: Boolean = currentDate == now.date
    /** Past days, and today from 18:00. Fixed for this view model's life. */
    val isEvening: Boolean = currentDate < now.date || (currentDate == now.date && now.hour >= EVENING_HOUR)

    /** False until today's plan is built, so the list doesn't flicker through half-built states. */
    private val planReady = MutableStateFlow(!isToday)

    val state: StateFlow<DiaryUiState?> = combine(
        missionRepository.observeActive(),
        todayTaskRepository.observeBetween(
            currentDate.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY),
            currentDate.plus(7, DateTimeUnit.DAY),
        ),
        planRepository.observePlan(currentDate),
        planReady,
    ) { missions, tasks, plan, ready ->
        if (ready) DiaryStateBuilder.build(currentDate, missions, tasks, plan) else null
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val missions: StateFlow<List<Mission>> = missionRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val milestones: StateFlow<List<MilestoneWithDetails>> = milestoneRepository
        .observeInRange(MyDate.fromLocalDate(currentDate).subtractDays(7), MyDate.fromLocalDate(currentDate).addDays(7))
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            try {
                if (isToday) planRepository.ensurePlanned(currentDate)
                if (isEvening) planRepository.tickFixedBlocks(currentDate)   // past days too; it does nothing without a plan
            } finally {
                planReady.value = true
            }
        }
    }

    fun toggleDone(taskId: Long) = act { planRepository.toggleDone(taskId) }
    fun didSome(taskId: Long) = act { planRepository.setState(taskId, TaskState.PARTIAL) }
    fun moveTask(taskId: Long) = act { planRepository.moveToNextRoom(taskId) }
    fun skipTask(taskId: Long) = act { planRepository.setState(taskId, TaskState.SKIPPED) }
    fun toggleCrown(taskId: Long) = act { planRepository.toggleCrown(taskId) }
    fun swap(taskId: Long) = act { planRepository.swap(taskId) }
    fun removeTask(taskId: Long) = act { planRepository.removeTask(taskId) }
    fun addMission(missionId: Long) = act { planRepository.addMission(currentDate, missionId) }
    fun lighterDay() = act { planRepository.lighterDay(currentDate) }
    fun changeDay(freeMinutes: Int, energy: Energy) = act { planRepository.changeDay(currentDate, freeMinutes, energy) }
    fun confirm() = act { planRepository.confirm(currentDate) }

    fun updateMilestoneStatus(milestone: Milestone, achieved: Boolean, completionDate: LocalDate) = act {
        val updated = if (achieved) {
            milestone.copy(status = TaskStatus.COMPLETED.value, actualCompletionDate = MyDate.fromLocalDate(completionDate))
        } else {
            milestone.copy(status = TaskStatus.IN_PROGRESS.value, actualCompletionDate = null)
        }
        milestoneRepository.updateMilestone(updated)
    }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        const val EVENING_HOUR = 18
    }
}
