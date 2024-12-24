package com.example.todoapp.screen.metrics

import androidx.compose.ui.unit.dp
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
import kotlin.random.Random

class CalDiaryViewModel(
    private val missionRepository: MissionRepository
) : ViewModel() {
//
    private val _missionIds = MutableSharedFlow<List<Long>>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val missionIds: SharedFlow<List<Long>> get() = _missionIds

//    private val _currentDate = MutableStateFlow(LocalDate.fromEpochDays(0))
    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> get() = _currentDate



    fun selectDate(selectedDate: LocalDate) {
        _currentDate.value = selectedDate
    }

    fun loadMissionIds() {
        viewModelScope.launch {
            missionRepository.getAllMissionIds()
                .collect { missionList ->
                    _missionIds.tryEmit(missionList)
                }
        }
    }
}
