package com.example.todoapp.screen.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.repo.MilestoneRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class DiaryViewModel(
    private val milestoneRepository: MilestoneRepository,
    private val currentDate: LocalDate,
) : ViewModel() {

    val milestones: StateFlow<List<MilestoneWithDetails>> = milestoneRepository
        .observeInRange(MyDate.fromLocalDate(currentDate).subtractDays(7), MyDate.fromLocalDate(currentDate).addDays(7))
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun updateMilestoneStatus(milestone: Milestone, achieved: Boolean, completionDate: LocalDate) {
        viewModelScope.launch {
            val updated = if (achieved) {
                milestone.copy(status = TaskStatus.COMPLETED.value, actualCompletionDate = MyDate.fromLocalDate(completionDate))
            } else {
                milestone.copy(status = TaskStatus.IN_PROGRESS.value, actualCompletionDate = null)
            }
            milestoneRepository.updateMilestone(updated)
        }
    }
}
