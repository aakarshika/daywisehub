package com.example.todoapp.screen.metrics.allmonths

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.BadgeInput
import com.example.todoapp.domain.CalendarFilter
import com.example.todoapp.domain.DayBadges
import com.example.todoapp.domain.DayTasks
import com.example.todoapp.domain.History
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.MonthCalendar
import com.example.todoapp.domain.MonthGrid
import com.example.todoapp.domain.PaceOffers
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.pillarType
import com.example.todoapp.domain.weekStartDate
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** A row of the Missions checklist. */
data class MissionChoice(val missionId: Long, val title: String, val pillar: PillarType, val archived: Boolean)

/** Everything All months reads. */
data class AllMonthsInput(
    val today: LocalDate,
    val filter: CalendarFilter,
    /** Active and archived missions: archived ones keep their days, and can be picked. */
    val allMissions: List<Mission>,
    /** Tasks from the first month's grid, less PlanRepository.HISTORY_DAYS, to the end of this week. */
    val tasks: List<TodayTask>,
    /** Every finished week, oldest first. */
    val summaries: List<WeekSummary>,
    /** Every milestone, archived missions' too. */
    val milestones: List<Milestone>,
    /** Day plans over the same window as [tasks], for the badges' energy. */
    val plans: List<DayPlan>,
    /** The first stored item's day, where the months start; null on an empty install. */
    val firstDay: LocalDate?,
)

data class AllMonthsUiState(
    /** Newest first: this month, then back to the first stored item's month. This month alone with nothing stored. */
    val months: List<MonthCalendar>,
    val filter: CalendarFilter,
    /** "⟨Call mom⟩ and ⟨Gym⟩: done 23 times, 4 in part", over every month shown; null while the filter is off. */
    val summary: List<LinePart>?,
    /** Every mission for the checklist, in pillar order, active ones first, then archived, then by title. */
    val choices: List<MissionChoice>,
)

object AllMonthsStateBuilder {

    fun build(input: AllMonthsInput): AllMonthsUiState {
        val today = input.today
        val thisMonth = LocalDate(today.year, today.month, 1)
        val start = minOf(input.firstDay ?: today, today)
        val firstMonth = LocalDate(start.year, start.month, 1)
        val filter = input.filter
        // Read once here; every month looks its days up instead of re-reading dates.
        val days = DayTasks(input.tasks)
        val byWeek = input.summaries.associateBy { it.weekStartDate }
        val opened = PaceOffers.openedWeeks(input.tasks)
        // One book for every month's days, and none with a filter on: badges speak for the whole day.
        val badges = if (filter.isOn) null else DayBadges.book(
            BadgeInput(
                today = today, from = MonthGrid.span(firstMonth).start, to = MonthGrid.span(thisMonth).endInclusive,
                allMissions = input.allMissions, history = History(input.tasks), plans = input.plans,
                milestones = input.milestones, summaries = byWeek, firstDay = input.firstDay,
                lookBackDays = PlanRepository.HISTORY_DAYS, maxCrowns = PlanRepository.MAX_CROWNS,
            )
        )
        val months = mutableListOf<MonthCalendar>()
        var month = thisMonth
        while (month >= firstMonth) {
            months += MonthGrid.build(month, today, input.allMissions, days, byWeek, opened, badges, selected = null, filter = filter)
            month = month.minus(1, DateTimeUnit.MONTH)
        }
        return AllMonthsUiState(
            months = months,
            filter = filter,
            // The months' own days, up to today: what the rings show.
            summary = filter.summary(input.allMissions, input.tasks, firstMonth, today),
            choices = input.allMissions
                .map { MissionChoice(it.missionId, it.title, it.pillarType, archived = !it.active) }
                .sortedWith(compareBy({ it.pillar.ordinal }, { it.archived }, { it.title.lowercase() })),
        )
    }
}

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
) : ViewModel() {

    /** Fixed for this view model's life; a new day closes the screen with the Metrics tab's view model. */
    private val today: LocalDate = MyDate.now().toLocalDate()

    private val filter = MutableStateFlow(CalendarFilter())

    /** Tasks and day plans for every month shown, and the first stored day they start from. */
    private class Window(val firstDay: LocalDate?, val tasks: List<TodayTask>, val plans: List<DayPlan>)

    /**
     * From the first stored day's month grid, less the badges' look-back (for due dates and badges), to the
     * end of this week. The window only moves when the first stored day does.
     */
    private val window = todayTaskRepository.observeFirstDate()
        .distinctUntilChanged()
        .flatMapLatest { first ->
            val start = minOf(first ?: today, today)
            val from = WeekMath.weekStart(LocalDate(start.year, start.month, 1)).minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY)
            val to = WeekMath.weekEnd(today)
            combine(
                todayTaskRepository.observeBetween(from, to),
                planRepository.observePlans(from, to),
            ) { tasks, plans -> Window(first, tasks, plans) }
        }

    /**
     * Built off the main thread, once per change to the data or the filter. Null until the first build. A
     * builder failure keeps what's on screen instead of closing the app.
     */
    val state: StateFlow<AllMonthsUiState?> = combine(
        filter,
        missionRepository.observeAll(),
        window,
        weekRepository.observeSummaries(),
        // Archived missions' too, so a finished line keeps its badges once it's archived.
        milestoneRepository.observeIncludingArchived(),
    ) { filter, missions, window, summaries, milestones ->
        AllMonthsStateBuilder.build(AllMonthsInput(today, filter, missions, window.tasks, summaries, milestones, window.plans, window.firstDay))
    }
        .flowOn(Dispatchers.Default)
        .catch { e ->
            if (e is CancellationException) throw e
            Logger.w(e) { "All months couldn't be built; the screen keeps what it showed." }
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
