package com.example.todoapp.screen.diary.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mission.Mission
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
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

//    val allMissions = MutableSharedFlow<List<MissionWithFewDetails>>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val tasksPastMonth = MutableSharedFlow<List<TodayTask>>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _allMissions = MutableStateFlow<List<MissionWithFewDetails>>(emptyList())
    val allMissions: StateFlow<List<MissionWithFewDetails>> = _allMissions.asStateFlow()


    fun loadTasksPastMonth() {
        viewModelScope.launch {
            todayTaskRepository.getTasksPastMonth()
                .collectLatest { tl ->
                    Logger.w("loading tasks for month: ${tl}")
                    tasksPastMonth.tryEmit(tl)
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

//    fun loadMissions() {
//        viewModelScope.launch {
//            missionRepository.getMissions()
//                .collectLatest { missionList ->
//                    Logger.w("loading missions: ${missionList}")
//                    allMissions.tryEmit(missionList)
//                }
//        }
//    }


    fun loadMissions() {
        viewModelScope.launch {
            missionRepository.getMissions()
                .collectLatest { missionList ->
                    Logger.w("loading missions: ${missionList}")
                    _allMissions.value = missionList // Updates only the latest list
                }
        }
    }
}
