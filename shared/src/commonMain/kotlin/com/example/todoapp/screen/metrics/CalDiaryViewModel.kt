package com.example.todoapp.screen.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.models.MyDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** The date the diary is showing, shared across tabs. Moves to the new day at midnight if it was showing today. */
class CalDiaryViewModel : ViewModel() {

    private var today: LocalDate = MyDate.now().toLocalDate()

    private val _currentDate = MutableStateFlow(today)
    val currentDate: StateFlow<LocalDate> get() = _currentDate

    init {
        viewModelScope.launch {
            while (true) {
                delay(DAY_CHECK_MILLIS)
                val now = MyDate.now().toLocalDate()
                if (now != today) {
                    if (_currentDate.value == today) _currentDate.value = now
                    today = now
                }
            }
        }
    }

    fun selectDate(selectedDate: LocalDate) {
        _currentDate.value = selectedDate
    }

    private companion object {
        const val DAY_CHECK_MILLIS = 60_000L
    }
}
