package com.example.todoapp.screen.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mission.MissionWithDetails
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
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


    fun loadTaskDetails() {
        viewModelScope.launch {
            todayTaskRepository.getAllTasksForDate1(MyDate.fromLocalDate(currentDate))
                .collectLatest {
                    Logger.w("tasks collected Diary1: ${currentDate} ${it.size}")
                    dayTasks.emit(it)
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


    fun addRandomTask(): Boolean {
        if(nMissions>0) {
            var randomMission = (1L..nMissions).random()
            val newTask = getNewRandomTask(randomMission)
            viewModelScope.launch {
                val newMissionId = todayTaskRepository.insertFullTask(newTask)
            }
            return true
        }
        return false
    }

    private fun getNewRandomTask(randomMissionId: Long): TodayTaskWithFewDetails {

        val newTask = TodayTaskWithFewDetails(
            todayTask = TodayTask(
                todayTaskId = 0L, // Example task ID
                userId = 1L, // Example userId
                missionId = randomMissionId, // Example missionId
                taskDate = MyDate.fromLocalDate(currentDate), // currentDate, // Example task date (current date)
                taskStatus = "ADDED", // Example status
                taskType = "AG_RANDOM", // Example type
                taskPageTag = "TODO",
                taskProgressVal = 0f, // Example progress
                taskText = "This is a randomly picked task", // Example text
                taskPictureUrl = null, // Example URL (can be null)
                taskLink = null // Example link (can be null)
            ),
            mission = null,
            pillar = null,
            todayTaskReminder = TodayTaskReminder(
                todayTaskId = 0L, // Example task ID
                timeOfDay = "08:00", // Example time
                alarmTone = "Default Tone", // Example alarm tone
                timeBefore = 10, // Example time before
                timeBeforeUnit = 1 // Example time before unit (e.g., minutes)
            ),
            missionFrequency = null
        )
        return newTask
    }


}
