package com.example.todoapp.screen.diary.diaryitem.components.diarymood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.repo.TodayMoodRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val _dayFactors: MutableSharedFlow<List<String>> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val dayFactors: SharedFlow<List<String>> = _dayFactors

    fun loadDayFactors(cDate: LocalDate) {
        viewModelScope.launch {
            todayMoodRepository.getDayFactors(cDate).collectLatest { _dayFactors.tryEmit(it) }
        }
    }

    fun setDayFactor(cDate: LocalDate, icon: String, picked: Boolean) {
        viewModelScope.launch {
            try {
                withContext(NonCancellable) { todayMoodRepository.setDayFactor(cDate, icon, picked) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "The day factor didn't save." }
            }
        }
    }

    /** A failed save is logged, not thrown, so it can't close the app. */
    fun upsertTodayMood(todayMood: TodayMood) {
        viewModelScope.launch {
            try {
                withContext(NonCancellable) { todayMoodRepository.upsertTodayMood(todayMood) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "The mood didn't save." }
            }
        }
    }
}
