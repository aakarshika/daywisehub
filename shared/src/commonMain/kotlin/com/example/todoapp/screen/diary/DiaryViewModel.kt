package com.example.todoapp.screen.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.repo.TodayTaskRepository
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class DiaryViewModel(
    private val todayTaskRepository: TodayTaskRepository,
    private val currentDate: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val _dayTasks: MutableSharedFlow<List<ComboTask>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val dayTasks: SharedFlow<List<ComboTask>?> = _dayTasks

    fun loadTaskDetails() {
        viewModelScope.launch {
            todayTaskRepository.getAllTasksForDate1(MyDate.fromLocalDate(currentDate))
                .collectLatest {
                    _dayTasks.emit(it)
                }
        }
    }

    fun updateTaskStatus(todayTaskId: Long, taskStatus: String) {
        viewModelScope.launch {
            todayTaskRepository.updateStatus(todayTaskId, taskStatus)
        }
    }

    fun insertTask(task: ComboTask) {
        viewModelScope.launch {
            val t = todayTaskRepository.insertFullTask(
                TodayTaskWithFewDetails(
                    todayTask = task.todayTask!!,
                    todayTaskReminder = task.todayTaskReminder,
                    mission = null,
                    pillar = null,
                    missionFrequency = null
                )
            )
        }
    }

    fun updateTodayTask(task: TodayTask) {
        viewModelScope.launch {
            todayTaskRepository.updateTodayTask(task)
        }
    }

    fun generateTodaysTasks(date: LocalDate) {
        viewModelScope.launch {
            val taskId = todayTaskRepository.addRandomMissionForDay(date)
        }

        viewModelScope.launch {
            val taskId = todayTaskRepository.addRandomMissionForDay(date)
        }

        viewModelScope.launch {
            val taskId = todayTaskRepository.addRandomMissionForDay(date)
        }
    }
    fun addRandomTask(date: LocalDate) {
        viewModelScope.launch {
            val taskId = todayTaskRepository.addRandomMissionForDay(date)
        }
    }
}
