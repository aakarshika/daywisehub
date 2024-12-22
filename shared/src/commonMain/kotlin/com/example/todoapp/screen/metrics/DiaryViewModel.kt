package com.example.todoapp.screen.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.todotask.HabitTaskWithFewDetails
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskReminder
import com.example.todoapp.db.data.todotask.TodayTaskWithDetails
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.TodayTaskRepository
import com.example.todoapp.screen.missions.defaultMissionWithDetails
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class DiaryViewModel(
    private val todayTaskRepository: TodayTaskRepository,
    private val currentDate: LocalDate,
    private val nMissions: Int
) : ViewModel() {

    val dayTasks: MutableSharedFlow<List<TodayTaskWithFewDetails>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val habitList: MutableSharedFlow<List<HabitTaskWithFewDetails>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)


    fun loadTaskDetails() {
        viewModelScope.launch {
            todayTaskRepository.getAllTasksForDate1(MyDate.fromLocalDate(currentDate))
                .collectLatest {
                    Logger.w("tasks collected Diary1: ${currentDate} ${it.size}")
                    dayTasks.emit(it)
                }
        }
        viewModelScope.launch {
            todayTaskRepository.getHabitMissions(MyDate.fromLocalDate(currentDate))
                .collectLatest {
                    Logger.e("habit missions list: $it")
                    habitList.tryEmit(it)
                }
        }
    }

    fun updateTaskStatus(todayTaskId: Long, taskStatus: String) {
        viewModelScope.launch {
            todayTaskRepository.updateStatus(todayTaskId, taskStatus)
        }
    }
    //update tag
    fun updateTaskTag(todayTaskId: Long, taskTag: String) {
        viewModelScope.launch {
            todayTaskRepository.updateTag(todayTaskId, taskTag)
        }
    }

    fun insertTask(task: TodayTaskWithFewDetails) {
        viewModelScope.launch {
            todayTaskRepository.insertFullTask(task)
        }
    }

    fun addRandomTask() {
        viewModelScope.launch {
            val taskId = todayTaskRepository.addRandomMissionForDay(currentDate)
        }
    }
}
