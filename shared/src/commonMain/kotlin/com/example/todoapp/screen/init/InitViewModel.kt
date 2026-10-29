package com.example.todoapp.screen.init

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.models.LOCAL_USER_ID
import com.example.todoapp.db.sample.SampleSeed
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
    private val database: AppDatabase,
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> get() = _isReady
    private val _startOnMissions = MutableStateFlow(false)
    /** True when there are no missions yet, so the app opens on Missions with the ideas library up. */
    val startOnMissions: StateFlow<Boolean> get() = _startOnMissions
    private var job: Job? = null

    /**
     * A fresh install gets the default settings and no missions. [sample] is dev-only (see SampleSeed) and
     * fills a database with no missions yet.
     * A failure here is logged and the app opens anyway; the next start tries again.
     */
    fun insertDefaultData(sample: SampleSeed? = null) {
        if (job != null) return
        job = viewModelScope.launch {
            try {
                todayMoodRepository.insertDefaultMoods(LOCAL_USER_ID)
                planRepository.ensureDefaults()
                if (sample != null && missionRepository.count() == 0) {
                    sample.run(database, missionRepository, todayTaskRepository)
                }
                _startOnMissions.value = missionRepository.count() == 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Startup defaults couldn't be written; the app opens anyway." }
            }
            _isReady.value = true
        }
    }
}
