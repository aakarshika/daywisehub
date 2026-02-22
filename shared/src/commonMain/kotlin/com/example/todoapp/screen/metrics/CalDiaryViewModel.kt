package com.example.todoapp.screen.metrics

import androidx.lifecycle.ViewModel
import com.example.todoapp.repo.MissionRepository
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

class CalDiaryViewModel(
    private val missionRepository: MissionRepository
) : ViewModel() {

    var userId: Long = -1L
    var username: String = ""

    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> get() = _currentDate

    fun selectDate(selectedDate: LocalDate) {
        _currentDate.value = selectedDate
    }
}
