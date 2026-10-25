package com.example.todoapp.screen.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.LOCAL_USER_ID
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.TodayTaskRepository
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MetricsViewModel(
    private val todayTaskRepository: TodayTaskRepository,
    private val milestoneRepository: MilestoneRepository
) : ViewModel() {

    private val _allTasks: MutableSharedFlow<List<TodayTaskWithFewDetails>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val allTasks: SharedFlow<List<TodayTaskWithFewDetails>?> = _allTasks

    private val _allMilestones: MutableSharedFlow<List<MilestoneWithDetails>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val allMilestones: SharedFlow<List<MilestoneWithDetails>?> = _allMilestones

    fun loadAllTasks() {
        viewModelScope.launch {
            todayTaskRepository.getAllTasks()
                .collectLatest {
                    _allTasks.emit(it.filter { it.todayTask.active != "N" })
                }
        }
    }

    fun loadAllMilestones() {
        viewModelScope.launch {
            val now = MyDate.now()
            val startMyDate = now.subtractDays(3 * 365)
            val endMyDate = now.addDays(3 * 365)
            milestoneRepository.getMilestonesForUserInDateRange(LOCAL_USER_ID, startMyDate, endMyDate)
                .collectLatest {
                    _allMilestones.emit(it)
                }
        }
    }
}
