package com.example.todoapp.screen.metrics.bottomhighlights

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

class BottomHighlightsViewModel(
    private val todayTaskRepository: TodayTaskRepository,
    private val currentDate: LocalDate = LocalDate.now()
) : ViewModel() {

    val dayTasks: MutableSharedFlow<List<TodayTaskWithFewDetails>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)


    fun loadTaskDetails() {
        viewModelScope.launch {
            todayTaskRepository.getAllTasksForDate2(MyDate.fromLocalDate(currentDate))
                .collectLatest {
                    Logger.w("tasks collected BH 2: ${currentDate} ${it.size}")
                    dayTasks.emit(it.filter { it.todayTask.active!="N" })
                }
        }
    }
}
