package com.example.todoapp.screen.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Patterns
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayTaskRepository
import com.example.todoapp.repo.WeekRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * The Metrics tab (spec 9). It only reads, so nothing here needs PlanRepository's write lock.
 * The tab makes a new one each time it opens, and again after midnight, and cancels it when it closes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MetricsViewModel(
    missionRepository: MissionRepository,
    todayTaskRepository: TodayTaskRepository,
    milestoneRepository: MilestoneRepository,
    weekRepository: WeekRepository,
) : ViewModel() {

    /** Fixed for this view model's life; NavScreens makes a new one when the date changes. */
    private val today: LocalDate = MyDate.now().toLocalDate()
    private val thisWeek: LocalDate = WeekMath.weekStart(today)

    /** Monday of the week asked for. The arrows step from it, so two quick taps move two weeks. */
    private val week = MutableStateFlow(thisWeek)

    private val summaries = weekRepository.observeSummaries().shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    /** The first stored week, or this week before there's one: the left arrow stops there. */
    private val earliest: StateFlow<LocalDate> = summaries
        .map { MetricsStateBuilder.earliest(it, thisWeek) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, thisWeek)

    /** Tasks from [from] on, so a week's state is built only once its window covers it. */
    private class Window(val from: LocalDate, val tasks: List<TodayTask>)

    /**
     * Tasks from a year before today (or earlier, when an older week's month needs it) to the end of
     * next week. The window only moves when a week that old is shown.
     */
    private val window = week
        .map { firstDay(it) }
        .distinctUntilChanged()
        .flatMapLatest { from ->
            todayTaskRepository.observeBetween(from, thisWeek.plus(13, DateTimeUnit.DAY)).map { Window(from, it) }
        }

    /**
     * Built off the main thread: months of tasks go through it on every change. Null until the first
     * build. A builder failure keeps what's on screen instead of closing the app.
     */
    val state: StateFlow<MetricsUiState?> = combine(
        week,
        missionRepository.observeAll(),
        window,
        summaries,
        milestoneRepository.observeAll(),
    ) { week, missions, window, summaries, milestones ->
        // An older week waits for its own window, so it never shows as away for a moment: the last state stays.
        if (window.from > firstDay(week)) null
        else MetricsStateBuilder.build(MetricsInput(today, week, missions, window.tasks, summaries, milestones))
    }
        .filterNotNull()
        .flowOn(Dispatchers.Default)
        .catch { e ->
            if (e is CancellationException) throw e
            Logger.w(e) { "Metrics couldn't be built; the tab keeps what it showed." }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun previousWeek() {
        week.update { if (it > earliest.value) it.minus(7, DateTimeUnit.DAY) else it }
    }

    fun nextWeek() {
        week.update { if (it < thisWeek) it.plus(7, DateTimeUnit.DAY) else it }
    }

    fun backToThisWeek() {
        week.value = thisWeek
    }

    /**
     * The look-back of patterns and mission lines (a year), or the history the week's month grid needs for
     * due dates, whichever is earlier.
     */
    private fun firstDay(week: LocalDate): LocalDate {
        val gridStart = WeekMath.weekStart(LocalDate(week.year, week.month, 1))
        return minOf(today.minus(Patterns.LOOK_BACK_DAYS, DateTimeUnit.DAY), gridStart.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY))
    }
}
