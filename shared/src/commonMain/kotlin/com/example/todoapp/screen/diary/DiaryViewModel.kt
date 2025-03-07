package com.example.todoapp.screen.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.repo.TodayTaskRepository
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class DiaryViewModel(
    private val todayTaskRepository: TodayTaskRepository,
    private val currentDate: LocalDate = LocalDate.now(),
) : ViewModel() {

    val dayTasks: MutableSharedFlow<List<ComboTask>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val habitList: MutableSharedFlow<List<ComboTask>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)


    fun loadTaskDetails() {
        viewModelScope.launch {
            todayTaskRepository.getAllTasksForDate1(MyDate.fromLocalDate(currentDate))
                .collectLatest {
                    Logger.i("tasks collected D: ${currentDate} ${it}")
                    dayTasks.emit(it)
                }
        }
//        viewModelScope.launch {
//            todayTaskRepository.getHabitMissions(MyDate.fromLocalDate(currentDate))
//                .collectLatest {
//                    Logger.i("habits collected  ${currentDate} ${it}")
//                    habitList.tryEmit(it)
//                }
//        }
    }

    fun updateTaskStatus(todayTaskId: Long, taskStatus: String) {
        Logger.w("updating Task status: $todayTaskId $taskStatus")
        viewModelScope.launch {
            todayTaskRepository.updateStatus(todayTaskId, taskStatus)
        }
    }
    //update tag
    fun updateTaskTag(todayTaskId: Long, taskTag: String) {
        Logger.w("updating Task tag: $todayTaskId $taskTag")
        viewModelScope.launch {
            todayTaskRepository.updateTag(todayTaskId, taskTag)
        }
    }

    fun insertTask(task: ComboTask) {
        Logger.w("inserting Task: $task")
        viewModelScope.launch {
            Logger.e("insertinggggggg Task: $task")
            val t = todayTaskRepository.insertFullTask(
                TodayTaskWithFewDetails(
                    todayTask = task.todayTask!!,
                    todayTaskReminder = task.todayTaskReminder,
                    mission = null,
                    pillar = null,
                    missionFrequency = null
                )
            )
            Logger.w("inserted Task: $t")
        }
    }
    //update todaytask
    fun updateTodayTask(task: TodayTask) {
        viewModelScope.launch {
            todayTaskRepository.updateTodayTask(task)
        }
    }

    fun generateTodaysTasks(date: LocalDate) {
        Logger.w("generateTodaysTasks adding random Task for $date")
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
        Logger.w("VM adding random Task for $date")
        viewModelScope.launch {
            val taskId = todayTaskRepository.addRandomMissionForDay(date)
        }
    }
}
