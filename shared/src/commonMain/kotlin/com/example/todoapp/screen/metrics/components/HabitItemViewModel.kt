package com.example.todoapp.screen.metrics.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.repo.TodayTaskRepository
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class HabitItemViewModel(
    private val todayTaskRepository: TodayTaskRepository,
    private val cDate: LocalDate,
    private val missionId: Long
) : ViewModel() {
    val taskProgressForPastAround: MutableSharedFlow<List<TodayTask>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun loadTaskProgressForPastAround() {
        viewModelScope.launch {
            todayTaskRepository.getTaskProgressForPastAround(missionId, cDate)
                .collectLatest {
                    Logger.e("loadMissionDetails mission: $it")
                    taskProgressForPastAround.tryEmit(it)
                }
        }
    }

}
