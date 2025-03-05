package com.example.todoapp.screen.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.repo.TodayTaskRepository
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class MetricsViewModel(
    private val todayTaskRepository: TodayTaskRepository
) : ViewModel() {

    val allTasks: MutableSharedFlow<List<TodayTaskWithFewDetails>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun loadAllTasks() {
        viewModelScope.launch {
            todayTaskRepository.getAllTasks()
                .collectLatest {
                    Logger.w("tasks collected - all for metrics: ${it.size}")
                    allTasks.emit(it)
                }
        }
    }
}
