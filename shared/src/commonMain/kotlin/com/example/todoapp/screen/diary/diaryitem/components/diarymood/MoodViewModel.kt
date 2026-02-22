package com.example.todoapp.screen.diary.diaryitem.components.diarymood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.repo.TodayMoodRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class MoodViewModel(
    private val todayMoodRepository: TodayMoodRepository,
) : ViewModel() {
    private val _todayMoodStatuses: MutableSharedFlow<List<TodayMoodWithDetails>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val todayMoodStatuses: SharedFlow<List<TodayMoodWithDetails>?> = _todayMoodStatuses

    fun loadDailyMoodStatus(cDate: LocalDate) {
        viewModelScope.launch {
            todayMoodRepository.getTodayMood(cDate)
                .collectLatest {
                    _todayMoodStatuses.tryEmit(it)
                }
        }
    }

    fun upsertTodayMood(todayMood: TodayMood) {
        viewModelScope.launch {
            todayMoodRepository.upsertTodayMood(todayMood)
        }
    }
}
