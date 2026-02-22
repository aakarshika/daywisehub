package com.example.todoapp.screen.diary.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.mission.Mission
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import com.example.todoapp.db.data.mission.MissionWithFewDetails
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.LocalDate

class PlanningViewModel (
    private val missionRepository: MissionRepository,
    private val todayTaskRepository: TodayTaskRepository
) : ViewModel() {

    private val _tasksPastMonth = MutableSharedFlow<List<TodayTask>>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val tasksPastMonth: SharedFlow<List<TodayTask>> = _tasksPastMonth

    private val _allMissions = MutableStateFlow<List<MissionWithFewDetails>>(emptyList())
    val allMissions: StateFlow<List<MissionWithFewDetails>> = _allMissions.asStateFlow()


    fun loadTasksPastMonth() {
        viewModelScope.launch {
            todayTaskRepository.getTasksPastMonth()
                .collectLatest { tl ->
                    _tasksPastMonth.tryEmit(tl)
                }
        }
    }

    fun addTaskForToday(date: LocalDate, mission: Mission){
        viewModelScope.launch {
            val taskId = todayTaskRepository.addMissionForDay(date, mission)
        }
    }
    fun deleteTaskForToday(taskId: Long) {
        viewModelScope.launch {
            todayTaskRepository.deleteTaskForToday(taskId)
        }
    }

    fun updateTag(taskId: Long, tag: String) {
        viewModelScope.launch {
            todayTaskRepository.updateTag(
                todayTaskId = taskId,
                tag = tag
            )
        }
    }
    fun activateTaskForToday(taskId: Long) {
        viewModelScope.launch {
            todayTaskRepository.activateTaskForToday(taskId)
        }
    }

    fun loadMissions() {
        viewModelScope.launch {
            missionRepository.getMissions()
                .collectLatest { missionList ->
                    _allMissions.value = missionList // Updates only the latest list
                }
        }
    }
}
