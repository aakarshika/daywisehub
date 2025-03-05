package com.example.todoapp.screen.diary.diaryitem.components.diarymood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.repo.TodayMoodRepository
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class MoodViewModel(
    private val todayMoodRepository: TodayMoodRepository,
    private val c1Date: LocalDate = LocalDate.now(),
) : ViewModel() {
    val todayMoodStatuses: MutableSharedFlow<List<TodayMoodWithDetails>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val allMoods: MutableSharedFlow<List<TodayMoodWithDetails>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        viewModelScope.launch {
            todayMoodRepository.getAllMoods()
                .collectLatest {
                    Logger.i("all moods collected  ${it}")
                    allMoods.tryEmit(it)
                }
        }
    }
    fun loadDailyMoodStatus(cDate: LocalDate) {

        viewModelScope.launch {
            Logger.d { "Load daily mood status for ${cDate.toString()}" }
            todayMoodRepository.getTodayMood(cDate)
                .collectLatest {
                    Logger.i("mood status collected  ${cDate} ${it}")
                    todayMoodStatuses.tryEmit(it)
                }
        }
    }

    fun upsertTodayMood(todayMood: TodayMood) {
        viewModelScope.launch {
            Logger.i("upsert today mood: $todayMood")
            todayMoodRepository.upsertTodayMood(todayMood)
        }
    }
}
