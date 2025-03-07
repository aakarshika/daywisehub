package com.example.todoapp.screen.diary.diaryitem.components.waterintake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mood.WaterIntake
import com.example.todoapp.repo.TodayMoodRepository
import com.example.todoapp.repo.WaterIntakeRepository
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class WaterIntakeViewModel(
    private val waterIntakeRepository: WaterIntakeRepository,
    private val c1Date: LocalDate = LocalDate.now(),
) : ViewModel() {

    // Private mutable state flow with initial value
    private val _waterIntake = MutableStateFlow<WaterIntake?>(null)
    // Public immutable state flow exposed to collectors
    val waterIntake: StateFlow<WaterIntake?> = _waterIntake.asStateFlow()

    fun loadWaterIntake(cDate: LocalDate) {
        viewModelScope.launch {
            Logger.d { "Load daily water status for ${cDate.toString()}" }
            waterIntakeRepository.getWaterIntake(cDate)
                .collectLatest {
                    Logger.i("water status collected  ${cDate} ${it}")
                    _waterIntake.value = it  // Update the private mutable flow
                }
        }
    }
    fun upsertWaterIntake(waterIntake: WaterIntake) {
        viewModelScope.launch {
            Logger.i("upsert water intake: $waterIntake")
            waterIntakeRepository.upsertWaterIntake(waterIntake)
        }
    }
}
