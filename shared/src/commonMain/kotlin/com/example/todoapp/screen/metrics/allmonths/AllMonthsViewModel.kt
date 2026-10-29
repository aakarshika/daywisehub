package com.example.todoapp.screen.metrics.allmonths

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.CalendarFilter
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.DayLogs
import com.example.todoapp.repo.DayLogRepository
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * Spec 9.4: every month on one screen, filtered by pillars or missions. It only reads, so nothing here
 * needs PlanRepository's write lock. The Metrics tab makes one when the screen opens and cancels it when
 * it closes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AllMonthsViewModel(
    missionRepository: MissionRepository,
    todayTaskRepository: TodayTaskRepository,
    milestoneRepository: MilestoneRepository,
    weekRepository: WeekRepository,
    planRepository: PlanRepository,
    dayLogRepository: DayLogRepository,
) : ViewModel() {

    /** Fixed for this view model's life; a new day closes the screen with the Metrics tab's view model. */
    private val today: LocalDate = MyDate.now().toLocalDate()

    private val filter = MutableStateFlow(CalendarFilter())

    /** Tasks and day plans for every month shown, and the first stored day they start from. */
    private class Window(val firstDay: LocalDate?, val tasks: List<TodayTask>, val plans: List<DayPlan>, val logs: DayLogs)

    /**
     * From the first stored day's month grid, less the badges' look-back (for due dates and badges), to the
     * end of this week. The window only moves when the first stored day does.
     */
    private val window = todayTaskRepository.observeFirstDate()
        .flatMapLatest { first ->
            val start = minOf(first ?: today, today)
            val from = WeekMath.weekStart(LocalDate(start.year, start.month, 1)).minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY)
            val to = WeekMath.weekEnd(today)
            combine(
                todayTaskRepository.observeBetween(from, to),
                planRepository.observePlans(from, to),
                dayLogRepository.observe(from, to),
            ) { tasks, plans, logs -> Window(first, tasks, plans, logs) }
        }

    /**
     * The indexes, the badge book and the unfiltered months, once per change to the data and never for a
     * filter toggle. A failed build is skipped, so the next change builds again.
     */
    private val data = combine(
        missionRepository.observeAll(),
        window,
        weekRepository.observeSummaries(),
        // Archived missions' too, so a finished line keeps its badges once it's archived.
        milestoneRepository.observeIncludingArchived(),
    ) { missions, window, summaries, milestones ->
        try {
            AllMonthsStateBuilder.data(AllMonthsInput(today, missions, window.tasks, summaries, milestones, window.plans, window.firstDay, window.logs))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "All months couldn't be built; the screen keeps what it showed." }
            null
        }
    }.filterNotNull()

    /**
     * Built off the main thread, both stages: a filter toggle only builds the filtered months again, and
     * clearing it shows the unfiltered ones as they were. Null until the first build. A builder failure
     * keeps what's on screen instead of closing the app, and the next change of data or filter builds again.
     */
    val state: StateFlow<AllMonthsUiState?> = combine(data, filter) { data, filter ->
        // One bad build is skipped, so the flow goes on and the last state stays.
        try {
            AllMonthsStateBuilder.build(data, filter)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "All months couldn't be filtered; the screen keeps what it showed." }
            null
        }
    }
        .filterNotNull()
        .flowOn(Dispatchers.Default)
        .catch { e ->
            if (e is CancellationException) throw e
            Logger.w(e) { "All months' data stopped loading; the screen keeps what it showed." }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun togglePillar(pillar: PillarType) {
        filter.update { it.copy(pillars = if (pillar in it.pillars) it.pillars - pillar else it.pillars + pillar) }
    }

    /** The Missions checklist's "Done": exactly these missions are picked. */
    fun setMissions(ids: Set<Long>) {
        filter.update { it.copy(missionIds = ids) }
    }

    /** Nothing picked: the months look as on the Metrics calendar. */
    fun clear() {
        filter.value = CalendarFilter()
    }
}
