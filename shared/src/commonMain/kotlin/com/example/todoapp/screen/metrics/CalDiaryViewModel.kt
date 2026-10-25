package com.example.todoapp.screen.metrics

import androidx.lifecycle.ViewModel
import com.example.todoapp.db.models.MyDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

/** The date the diary is showing, shared across tabs. */
class CalDiaryViewModel : ViewModel() {

    private val _currentDate = MutableStateFlow(MyDate.now().toLocalDate())
    val currentDate: StateFlow<LocalDate> get() = _currentDate

    fun selectDate(selectedDate: LocalDate) {
        _currentDate.value = selectedDate
    }
}
