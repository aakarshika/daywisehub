package com.example.todoapp.screen.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Which mission the editor is open on; 0 for a new one. */
data class EditorTarget(val missionId: Long)

class MissionsViewModel(
    missionRepository: MissionRepository,
    todayTaskRepository: TodayTaskRepository,
    private val planRepository: PlanRepository,
    milestoneRepository: MilestoneRepository,
) : ViewModel() {

    /** Fixed for this view model's life; the tab makes a new one each time it opens. */
    private val today: LocalDate = MyDate.now().toLocalDate()

    val state: StateFlow<MissionsUiState?> = combine(
        missionRepository.observeActive(),
        todayTaskRepository.observeBetween(
            today.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY),
            today.plus(7, DateTimeUnit.DAY),
        ),
        milestoneRepository.observeAll(),
        planRepository.observeSettings(),
    ) { missions, tasks, milestones, settings ->
        MissionsStateBuilder.build(today, missions, tasks, milestones, settings)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _editor = MutableStateFlow<EditorTarget?>(null)
    /** Non-null while the sentence flow is open. */
    val editor: StateFlow<EditorTarget?> = _editor

    /** A second tap while the editor is opening does nothing. */
    fun openNew() {
        if (_editor.value == null) _editor.value = EditorTarget(0L)
    }

    fun openEdit(missionId: Long) {
        if (_editor.value == null) _editor.value = EditorTarget(missionId)
    }

    fun closeEditor() {
        _editor.value = null
    }

    /** The write itself can't be cancelled (PlanRepository.write), so leaving the tab doesn't lose it. */
    fun saveFreeTime(minutesByWeekday: Map<Int, Int>) {
        viewModelScope.launch { planRepository.saveSettings(minutesByWeekday) }
    }
}
