package com.example.todoapp.screen.init

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.SEED_SAMPLE_MISSIONS
import com.example.todoapp.db.SampleData
import com.example.todoapp.db.models.LOCAL_USER_ID
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayMoodRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InitViewModel(
    private val missionRepository: MissionRepository,
    private val todayTaskRepository: TodayTaskRepository,
    private val planRepository: PlanRepository,
    private val todayMoodRepository: TodayMoodRepository,
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> get() = _isReady

    fun insertDefaultData() {
        viewModelScope.launch {
            todayMoodRepository.insertDefaultMoods(LOCAL_USER_ID)
            planRepository.ensureDefaults()
            if (SEED_SAMPLE_MISSIONS && missionRepository.count() == 0) {
                SampleData.seed(missionRepository, todayTaskRepository)
            }
            _isReady.value = true
        }
    }
}
