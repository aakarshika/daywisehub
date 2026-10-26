package com.example.todoapp.screen.metrics.allmonths

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.domain.BadgeInput
import com.example.todoapp.domain.CalendarFilter
import com.example.todoapp.domain.DayBadgeBook
import com.example.todoapp.domain.DayBadges
import com.example.todoapp.domain.DayTasks
import com.example.todoapp.domain.History
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.MonthCalendar
import com.example.todoapp.domain.MonthGrid
import com.example.todoapp.domain.PaceOffers
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.pillarType
import com.example.todoapp.domain.weekStartDate
import com.example.todoapp.repo.PlanRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** A row of the Missions checklist. */
data class MissionChoice(val missionId: Long, val title: String, val pillar: PillarType, val archived: Boolean)

/** Everything All months reads. */
data class AllMonthsInput(
    val today: LocalDate,
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

/**
 * What All months works out once per change to the data, whatever the filter: the indexes every month
 * reads, and the months without a filter, badges and all. A filter toggle builds only the filtered months
 * from it, and clearing the filter shows [unfiltered] again.
 */
class AllMonthsData internal constructor(
    val input: AllMonthsInput,
    /** The first of the first stored item's month, or of this month with nothing stored. */
    val firstMonth: LocalDate,
    val days: DayTasks,
    val byWeek: Map<LocalDate, WeekSummary>,
    /** PaceOffers.openedWeeks: the rest of the finished weeks are away. */
    val opened: Set<LocalDate>,
    /** Newest first, as on the Metrics calendar. */
    val unfiltered: List<MonthCalendar>,
    val choices: List<MissionChoice>,
)

data class AllMonthsUiState(
    /** Newest first: this month, then back to the first stored item's month. This month alone with nothing stored. */
    val months: List<MonthCalendar>,
    val filter: CalendarFilter,
    /**
     * "⟨Call mom⟩ and ⟨Gym⟩: done 23 times, 4 in part", over every month shown, or "Everything counts."
     * while the filter is off.
     */
    val summary: List<LinePart>,
    /** Every mission for the checklist, in pillar order, active ones first, then archived, then by title. */
    val choices: List<MissionChoice>,
)

object AllMonthsStateBuilder {

    /** Once per change to the data. The one badge book covers every month's days. */
    fun data(input: AllMonthsInput): AllMonthsData {
        val today = input.today
        val start = minOf(input.firstDay ?: today, today)
        val firstMonth = LocalDate(start.year, start.month, 1)
        val thisMonth = LocalDate(today.year, today.month, 1)
        // Read once here; every month looks its days up instead of re-reading dates.
        val days = DayTasks(input.tasks)
        val byWeek = input.summaries.associateBy { it.weekStartDate }
        val opened = PaceOffers.openedWeeks(input.tasks)
        val badges = DayBadges.book(
            BadgeInput(
                today = today, from = MonthGrid.span(firstMonth).start, to = MonthGrid.span(thisMonth).endInclusive,
                allMissions = input.allMissions, history = History(input.tasks), plans = input.plans,
                milestones = input.milestones, summaries = byWeek, firstDay = input.firstDay,
                lookBackDays = PlanRepository.HISTORY_DAYS, maxCrowns = PlanRepository.MAX_CROWNS,
            )
        )
        return AllMonthsData(
            input = input,
            firstMonth = firstMonth,
            days = days,
            byWeek = byWeek,
            opened = opened,
            unfiltered = months(input, firstMonth, days, byWeek, opened, badges, filter = null),
            choices = input.allMissions
                .map { MissionChoice(it.missionId, it.title, it.pillarType, archived = !it.active) }
                .sortedWith(compareBy({ it.pillar.ordinal }, { it.archived }, { it.title.lowercase() })),
        )
    }

    /**
     * Once per change to the data or the filter. With the filter off it's [AllMonthsData.unfiltered] as it
     * stands; with one on, the months are built again by items, without badges.
     */
    fun build(data: AllMonthsData, filter: CalendarFilter): AllMonthsUiState {
        val input = data.input
        return AllMonthsUiState(
            months = if (!filter.isOn) data.unfiltered
            else months(input, data.firstMonth, data.days, data.byWeek, data.opened, badges = null, filter),
            filter = filter,
            // The months' own days, up to today, without away weeks: what the rings show.
            summary = filter.summary(input.allMissions, input.tasks, data.firstMonth, input.today, data.opened)
                ?: listOf(LinePart("Everything counts.")),
            choices = data.choices,
        )
    }

    /** From this month back to [firstMonth]. */
    private fun months(
        input: AllMonthsInput,
        firstMonth: LocalDate,
        days: DayTasks,
        byWeek: Map<LocalDate, WeekSummary>,
        opened: Set<LocalDate>,
        badges: DayBadgeBook?,
        filter: CalendarFilter?,
    ): List<MonthCalendar> {
        val months = mutableListOf<MonthCalendar>()
        var month = LocalDate(input.today.year, input.today.month, 1)
        while (month >= firstMonth) {
            months += MonthGrid.build(month, input.today, input.allMissions, days, byWeek, opened, badges, selected = null, filter = filter)
            month = month.minus(1, DateTimeUnit.MONTH)
        }
        return months
    }
}
