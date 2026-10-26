package com.example.todoapp.screen.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.models.MyDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * The date the diary is showing, shared across tabs. Moves to the new day at midnight if it was showing today.
 * [today] follows the date too, so the Metrics and Missions tabs can start again on a new day.
 */
class CalDiaryViewModel : ViewModel() {

    private val _today = MutableStateFlow(MyDate.now().toLocalDate())
    val today: StateFlow<LocalDate> get() = _today

    private val _currentDate = MutableStateFlow(_today.value)
    val currentDate: StateFlow<LocalDate> get() = _currentDate

    init {
        // A backstop: NavScreens also checks when the app comes back to the front and on every tab change.
        viewModelScope.launch {
            while (true) {
                delay(DAY_CHECK_MILLIS)
                checkDate()
            }
        }
    }

    /** Moves to the new day if the date has changed since the last look. Main thread only, like every caller. */
    fun checkDate() {
        val now = MyDate.now().toLocalDate()
        if (now != _today.value) {
            if (_currentDate.value == _today.value) _currentDate.value = now
            _today.value = now
        }
    }

    fun selectDate(selectedDate: LocalDate) {
        _currentDate.value = selectedDate
    }

    private companion object {
        const val DAY_CHECK_MILLIS = 5_000L
    }
}
