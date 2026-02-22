package com.example.todoapp.screen.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class MetricsViewModel(
    private val todayTaskRepository: TodayTaskRepository
) : ViewModel() {

    private val _allTasks: MutableSharedFlow<List<TodayTaskWithFewDetails>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val allTasks: SharedFlow<List<TodayTaskWithFewDetails>?> = _allTasks

    fun loadAllTasks() {
        viewModelScope.launch {
            todayTaskRepository.getAllTasks()
                .collectLatest {
                    _allTasks.emit(it.filter { it.todayTask.active != "N" })
                }
        }
    }
}
