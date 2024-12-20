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
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class CalDiaryViewModel(
    private val todayTaskRepository: TodayTaskRepository
//    private val missionRepository: MissionRepository
) : ViewModel() {

    val dayTasks: MutableSharedFlow<List<TodayTaskWithFewDetails>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _missionIds = MutableSharedFlow<List<Long>>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val missionIds: SharedFlow<List<Long>> get() = _missionIds

    private val _currentDate = MutableStateFlow<LocalDate>(getTodaysDate())
    val currentDate: StateFlow<LocalDate> get() = _currentDate


    private fun getTodaysDate(): LocalDate{
        return LocalDate.now()
    }
//    init {
//        loadMissionIds()
//    }
//
//    //load all missions
//    fun loadMissionIds() {
//        viewModelScope.launch {
//            missionRepository.getAllMissionIds()
//                .collect { missionList ->
//                    _missionIds.tryEmit(missionList)
//                }
//        }
//    }
    fun loadTaskDetails() {
        viewModelScope.launch {
            todayTaskRepository.getAllTasks()
                .collectLatest {
                    if(it.size>0){
                        Logger.w("tasks: ${it}")
                    }
                    dayTasks.tryEmit(it)
                }
        }
    }

    fun selectDate(selectedDate: LocalDate) {
        _currentDate.value = selectedDate
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
//        var randomMission = _missionIds.value.random()
////        if (missions.value.size > dayTasks..size) {
////            while (allTasks.value.itemList.any { it.task.missionId == randomMission.missionId }) {
//                randomMission = missions.value.random()
//                val newTask =  getNewRandomTask(randomMission, taskType = "AG_RANDOM")
//                insertFullTask(newTask)
//                return true
////            }
////        }
        return false
    }

    private fun getNewRandomTask(randomMission: MissionWithDetails, taskType: String): TodayTaskWithFewDetails {

        val newTask = TodayTaskWithFewDetails(
            todayTask = TodayTask(
                userId = 1L, // Example userId
                missionId = randomMission.mission?.missionId?:0L, // Example missionId
                taskDate = MyDate.fromLocalDate(currentDate.value), // Example task date (current date)
                taskStatus = "ADDED", // Example status
                taskType = "AG_RANDOM", // Example type
                taskPageTag = "TODO",
                taskProgressVal = 0f, // Example progress
                taskText = "This is a randomly picked task", // Example text
                taskPictureUrl = null, // Example URL (can be null)
                taskLink = null // Example link (can be null)
            ),
            mission = randomMission.mission,
            pillar = randomMission.pillar,
            todayTaskReminder = TodayTaskReminder(
                todayTaskId = 1L, // Example task ID
                timeOfDay = "08:00", // Example time
                alarmTone = "Default Tone", // Example alarm tone
                timeBefore = 10, // Example time before
                timeBeforeUnit = 1 // Example time before unit (e.g., minutes)
            ),
            missionFrequency = randomMission.missionFrequency
        )
        return newTask
    }


}
