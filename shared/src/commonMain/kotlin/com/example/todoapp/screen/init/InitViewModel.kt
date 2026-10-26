package com.example.todoapp.screen.init

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.MetricsSample
import com.example.todoapp.db.SEED_METRICS_HISTORY
import com.example.todoapp.db.SEED_SAMPLE_MISSIONS
import com.example.todoapp.db.SampleData
import com.example.todoapp.db.models.LOCAL_USER_ID
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayMoodRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private var job: Job? = null

    /** A failure here is logged and the app opens anyway; the next start tries again. */
    fun insertDefaultData() {
        if (job != null) return
        job = viewModelScope.launch {
            try {
                todayMoodRepository.insertDefaultMoods(LOCAL_USER_ID)
                planRepository.ensureDefaults()
                if (SEED_SAMPLE_MISSIONS && missionRepository.count() == 0) {
                    SampleData.seed(missionRepository, todayTaskRepository)
                    if (SEED_METRICS_HISTORY) MetricsSample.seed(missionRepository, todayTaskRepository)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Startup defaults couldn't be written; the app opens anyway." }
            }
            _isReady.value = true
        }
    }
}
