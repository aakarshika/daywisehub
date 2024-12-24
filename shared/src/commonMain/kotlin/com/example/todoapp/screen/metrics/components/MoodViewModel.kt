package com.example.todoapp.screen.metrics.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.repo.TodayMoodRepository
import com.example.todoapp.repo.TodayTaskRepository
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class MoodViewModel(
    private val todayMoodRepository: TodayMoodRepository,
    private val cDate: LocalDate = LocalDate.now(),
) : ViewModel() {
    val todayMoodStatuses: MutableSharedFlow<List<TodayMoodWithDetails>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun loadDailyMoodStatus() {
        viewModelScope.launch {
            Logger.d { "Load daily mood status for ${cDate.toString()}" }
            todayMoodRepository.getTodayMood(cDate)
                .collectLatest {
                    todayMoodStatuses.tryEmit(it)
                }
        }
    }

    fun upsertTodayMood(todayMood: TodayMood) {
        viewModelScope.launch {
            todayMoodRepository.upsertTodayMood(todayMood)
        }
    }
}
