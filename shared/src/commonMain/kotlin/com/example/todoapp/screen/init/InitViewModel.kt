package com.example.todoapp.screen.init

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.models.LOCAL_USER_ID
import com.example.todoapp.repo.TodayMoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InitViewModel(
    private val todayMoodRepository: TodayMoodRepository
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> get() = _isReady

    fun insertDefaultData() {
        viewModelScope.launch {
            todayMoodRepository.insertDefaultMoods(LOCAL_USER_ID)
            _isReady.value = true
        }
    }
}
