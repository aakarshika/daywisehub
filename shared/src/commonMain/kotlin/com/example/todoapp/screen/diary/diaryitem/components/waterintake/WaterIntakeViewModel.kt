package com.example.todoapp.screen.diary.diaryitem.components.waterintake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.repo.WaterIntakeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class WaterIntakeViewModel(
    private val waterIntakeRepository: WaterIntakeRepository,
) : ViewModel() {

    private val _waterIntake = MutableStateFlow<WaterIntake?>(null)
    val waterIntake: StateFlow<WaterIntake?> = _waterIntake.asStateFlow()

    fun loadWaterIntake(cDate: LocalDate) {
        viewModelScope.launch {
            waterIntakeRepository.getWaterIntake(cDate)
                .collectLatest {
                    _waterIntake.value = it
                }
        }
    }
    fun upsertWaterIntake(waterIntake: WaterIntake) {
        viewModelScope.launch {
            waterIntakeRepository.upsertWaterIntake(waterIntake)
        }
    }
}
