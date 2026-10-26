package com.example.todoapp.screen.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayTaskRepository
import com.example.todoapp.repo.WeekRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * The Metrics tab (spec 9). It only reads, so nothing here needs PlanRepository's write lock.
 * The tab makes a new one each time it opens and cancels it when it closes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MetricsViewModel(
    missionRepository: MissionRepository,
    todayTaskRepository: TodayTaskRepository,
    milestoneRepository: MilestoneRepository,
    weekRepository: WeekRepository,
) : ViewModel() {

    /** Fixed for this view model's life, as on the Missions tab. */
    private val today: LocalDate = MyDate.now().toLocalDate()
    private val thisWeek: LocalDate = WeekMath.weekStart(today)

    /** Monday of the week on screen. */
    private val week = MutableStateFlow(thisWeek)

    /**
     * Tasks from a year before today (or earlier, when an older week's month needs it) to the end of
     * next week. The window only moves when a week that old is shown.
     */
    private val tasks = week
        .map { firstDay(it) }
        .distinctUntilChanged()
        .flatMapLatest { from -> todayTaskRepository.observeBetween(from, thisWeek.plus(13, DateTimeUnit.DAY)) }

    /** Built off the main thread: months of tasks go through it on every change. */
    val state: StateFlow<MetricsUiState?> = combine(
        week,
        missionRepository.observeAll(),
        tasks,
        weekRepository.observeSummaries(),
        milestoneRepository.observeAll(),
    ) { week, missions, tasks, summaries, milestones ->
        MetricsStateBuilder.build(MetricsInput(today, week, missions, tasks, summaries, milestones))
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Steps from the week on screen, so two quick taps never go past the first stored week. */
    fun previousWeek() {
        val s = state.value ?: return
        if (s.canGoBack) week.value = s.weekStart.minus(7, DateTimeUnit.DAY)
    }

    fun nextWeek() {
        val s = state.value ?: return
        if (s.canGoForward) week.value = s.weekStart.plus(7, DateTimeUnit.DAY)
    }

    fun backToThisWeek() {
        week.value = thisWeek
    }

    /** A year back, or the history the week's month grid needs for due dates, whichever is earlier. */
    private fun firstDay(week: LocalDate): LocalDate {
        val gridStart = WeekMath.weekStart(LocalDate(week.year, week.month, 1))
        return minOf(today.minus(LOOK_BACK_DAYS, DateTimeUnit.DAY), gridStart.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY))
    }

    companion object {
        /** How far back patterns and per-mission lines look (best streaks, average gaps). */
        const val LOOK_BACK_DAYS = 365
    }
}
