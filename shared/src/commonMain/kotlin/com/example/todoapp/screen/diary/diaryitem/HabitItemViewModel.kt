package com.example.todoapp.screen.diary.diaryitem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class HabitItemViewModel(
    private val todayTaskRepository: TodayTaskRepository,
    private val cDate: LocalDate,
    private val missionId: Long
) : ViewModel() {
    private val _taskProgressForPastAround: MutableSharedFlow<List<TodayTask>?> = MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val taskProgressForPastAround: SharedFlow<List<TodayTask>?> = _taskProgressForPastAround

    fun loadTaskProgressForPastAround() {
        viewModelScope.launch {
            todayTaskRepository.getTaskProgressForPastAround(missionId, cDate)
                .collectLatest {
                    _taskProgressForPastAround.tryEmit(it)
                }
        }
    }

}
