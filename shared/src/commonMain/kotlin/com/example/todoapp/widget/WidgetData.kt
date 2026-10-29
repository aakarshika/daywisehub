package com.example.todoapp.widget

import com.example.todoapp.app.AppClock
import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.plan.DaySettings
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.BadgeInput
import com.example.todoapp.domain.DayBadges
import com.example.todoapp.domain.DayLogs
import com.example.todoapp.domain.DayTasks
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.History
import com.example.todoapp.domain.MonthGrid
import com.example.todoapp.domain.PaceOffers
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.minutesLabel
import com.example.todoapp.domain.scored
import com.example.todoapp.domain.taskState
import com.example.todoapp.domain.weekStartDate
import com.example.todoapp.domain.weekdayName
import com.example.todoapp.repo.DayLogRepository
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayMoodRepository
import com.example.todoapp.repo.TodayTaskRepository
import com.example.todoapp.repo.WaterIntakeRepository
import com.example.todoapp.repo.WeekRepository
import com.example.todoapp.screen.diary.BadgeExtras
import com.example.todoapp.screen.diary.DiaryExtras
import com.example.todoapp.screen.diary.DiaryStateBuilder
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.water_daily_limit
import com.example.todoapp.screen.diary.plan.balanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * The widgets' window on the app: a snapshot of today, the week and the month, and the few things a
 * widget can do. It reads through the same repositories and builders as the diary and Metrics tab, so a
 * widget never disagrees with the app. Task ticks go through PlanRepository's own public write.
 */
class WidgetData(
    private val database: AppDatabase,
    private val missionRepository: MissionRepository,
    private val todayTaskRepository: TodayTaskRepository,
    private val planRepository: PlanRepository,
    private val milestoneRepository: MilestoneRepository,
    private val weekRepository: WeekRepository,
    private val dayLogRepository: DayLogRepository,
    private val waterIntakeRepository: WaterIntakeRepository,
    private val todayMoodRepository: TodayMoodRepository,
) {

    /** Today's snapshot, then a new one each time the data under it changes. The day is fixed when it's called. */
    fun observe(): Flow<WidgetSnapshot> {
        val now = AppClock.now()
        val today = now.date
        val span = MonthGrid.span(today)
        val bookFrom = minOf(span.start, today.minus(STREAK_DAYS, DateTimeUnit.DAY))
        val from = bookFrom.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY)
        val to = maxOf(today.plus(7, DateTimeUnit.DAY), span.endInclusive)
        val todayKey = key(today)
        val flows: List<Flow<Any?>> = listOf(
            missionRepository.observeActive(),
            missionRepository.observeAll(),
            todayTaskRepository.observeBetween(from, to),
            planRepository.observePlans(from, to),
            planRepository.observeSettings(),
            weekRepository.observeSummaries(),
            milestoneRepository.observeIncludingArchived(),
            todayTaskRepository.observeFirstDate(),
            dayLogRepository.observe(bookFrom, today),
            database.getWaterIntakeDao().getWaterIntake(todayKey),
            database.getTodayMoodDao().getTodayMood(todayKey),
        )
        return combine(flows) { v ->
            @Suppress("UNCHECKED_CAST")
            build(
                Input(
                    now = now, today = today, bookFrom = bookFrom,
                    missions = v[0] as List<Mission>, allMissions = v[1] as List<Mission>,
                    tasks = v[2] as List<TodayTask>, plans = v[3] as List<DayPlan>, settings = v[4] as List<DaySettings>,
                    summaries = v[5] as List<WeekSummary>, milestones = v[6] as List<Milestone>,
                    firstDay = v[7] as LocalDate?, logs = v[8] as DayLogs,
                    water = v[9] as WaterIntake?, moods = v[10] as List<TodayMoodWithDetails>,
                )
            )
        }.flowOn(Dispatchers.Default)
    }

    suspend fun snapshot(): WidgetSnapshot = observe().first()

    /** The diary's tick box: done, or back to planned. */
    suspend fun toggleTask(taskId: Long) = planRepository.toggleDone(taskId)

    /** One more glass, up to the day's goal. */
    suspend fun addGlass() = write {
        val today = AppClock.today()
        val row = waterIntakeRepository.getWaterIntake(today).first()
        val glasses = glasses(row)
        if (glasses >= water_daily_limit) return@write
        // The diary stores the last filled glass's index.
        val progress = glasses.toDouble()
        waterIntakeRepository.upsertWaterIntake(
            row?.copy(waterIntakeProgress = progress) ?: WaterIntake(waterIntakeProgress = progress, waterDate = MyDate.fromLocalDate(today))
        )
    }

    /** Picks or unpicks today's [icon] mood ("happy"), as a tap on the diary's face does. */
    suspend fun toggleMood(icon: String) = write {
        val today = AppClock.today()
        val mood = todayMoodRepository.getTodayMood(today).first().firstOrNull { it.mood.moodIcon == icon } ?: return@write
        val active = mood.todayMood?.tmMoodStatus == "ACTIVE"
        val row = mood.todayMood ?: TodayMood(tmMoodId = mood.mood.moodId, tmMoodStatus = "NEW", tmDate = MyDate.fromLocalDate(today))
        todayMoodRepository.upsertTodayMood(row.copy(tmMoodStatus = if (active) "INACTIVE" else "ACTIVE"))
    }

    /**
     * Makes this process's open queries on today read again. The iPhone widget writes from its own
     * process, which this process's change tracking doesn't see, so the app calls this on coming back.
     */
    suspend fun touchToday() = write {
        val day = key(AppClock.today())
        database.getTodayTaskDao().touchDay(day)
        database.getWaterIntakeDao().touchDay(day)
        database.getTodayMoodDao().touchDay(day)
    }

    private suspend fun write(block: suspend () -> Unit) = withContext(NonCancellable + Dispatchers.Default) { block() }

    private class Input(
        val now: LocalDateTime,
        val today: LocalDate,
        val bookFrom: LocalDate,
        val missions: List<Mission>,
        val allMissions: List<Mission>,
        val tasks: List<TodayTask>,
        val plans: List<DayPlan>,
        val settings: List<DaySettings>,
        val summaries: List<WeekSummary>,
        val milestones: List<Milestone>,
        val firstDay: LocalDate?,
        val logs: DayLogs,
        val water: WaterIntake?,
        val moods: List<TodayMoodWithDetails>,
    )

    private fun build(input: Input): WidgetSnapshot {
        val today = input.today
        val weekStart = WeekMath.weekStart(today)
        val ahead = today.plus(7, DateTimeUnit.DAY)
        val byWeek = input.summaries.associateBy { it.weekStartDate }
        val byId = input.allMissions.associateBy { it.missionId }
        val days = DayTasks(input.tasks)
        val plansByDay = input.plans.associateBy { it.date }

        // The diary's own page for today: pillars, rows and the didn't-fit note.
        val diary = DiaryStateBuilder.build(
            date = today, today = today,
            missions = input.missions, allMissions = input.allMissions, tasks = input.tasks,
            extras = DiaryExtras(
                plans = input.plans.filter { it.date >= key(weekStart) && it.date <= key(ahead) },
                settings = input.settings,
                lastWeek = byWeek[weekStart.minus(7, DateTimeUnit.DAY)],
                pageWeek = null,
                offers = emptyList(),
            ),
            badgeExtras = BadgeExtras(input.milestones, input.firstDay, input.logs),
        )
        val pillars = diary.pillars.map {
            WidgetPillar(it.pillar, it.earned, it.target, it.share, grown = if (it.target == 0) 0f else it.share, extra = it.extra > 0)
        }
        val balance = balanceOf(diary.pillars)
        val rows = (diary.fixed + diary.picks).filter { it.state.isOpen }
        val tasks = rows.map { row ->
            val block = byId[row.missionId]?.startTime?.takeIf { row.isFixedBlock }
            WidgetTask(
                taskId = row.taskId, title = row.title, pillar = row.pillar,
                done = row.state.countsAsDone, progress = row.progress, partial = row.state == TaskState.PARTIAL,
                timeLabel = block ?: minutesLabel(row.minutes),
            )
        }
        val moodSet = input.moods.any { it.todayMood?.tmMoodStatus == "ACTIVE" }
        val scoredPicks = diary.picks.filter { it.state.isOpen && it.pillar.scored }
        val mode = when {
            input.now.hour >= EVENING_HOUR && !moodSet -> TodayMode.EVENING
            !diary.hasPlan || rows.isEmpty() && !diary.hasMissions -> TodayMode.EMPTY
            rows.none { it.pillar.scored } -> TodayMode.REST
            diary.allPicksDone && scoredPicks.isNotEmpty() -> TodayMode.ALL_DONE
            else -> TodayMode.BALANCE
        }

        fun dayType(date: LocalDate): DayType {
            val scored = days.on(date).filter { it.taskState.isOpen && byId[it.missionId]?.scored == true }
            if (scored.isEmpty()) return DayType.REST
            val energy = plansByDay[key(date)]?.energy
            if (energy == Energy.LOW.name) return DayType.SLOW
            val minutes = scored.filter { !it.isFixedBlock }.sumOf { byId[it.missionId]?.durationMinutes ?: 0 }
            return if (minutes >= BIG_DAY_MINUTES || energy == Energy.HIGH.name) DayType.BIG else DayType.SLOW
        }
        fun plannedMinutes(date: LocalDate) = days.on(date)
            .filter { it.taskState.isOpen && !it.isFixedBlock && byId[it.missionId]?.scored == true }
            .sumOf { byId[it.missionId]?.durationMinutes ?: 0 }

        // The planner's own note: a mission that didn't fit today, and the day it found room on.
        val todayPlan = plansByDay[key(today)]
        val roomDay = todayPlan?.didntFitRoomDay?.let { MyDate(it).toLocalDate() }
            ?.takeIf { diary.didntFit != null && it > today && it < today.plus(7, DateTimeUnit.DAY) }
        val lightDay = roomDay ?: (1..6).map { today.plus(it, DateTimeUnit.DAY) }
            .filter { dayType(it) != DayType.REST }
            .minByOrNull { plannedMinutes(it) }
            ?.takeIf { light -> (1..6).any { plannedMinutes(today.plus(it, DateTimeUnit.DAY)) > plannedMinutes(light) } }
        val weekAhead = (0..6).map { i ->
            val date = today.plus(i, DateTimeUnit.DAY)
            val open = days.on(date).filter { it.taskState.isOpen }
            WidgetDay(
                dateKey = key(date), letter = date.weekdayName().take(1), dayType = dayType(date),
                done = i == 0 && open.isNotEmpty() && open.all { it.taskState.countsAsDone },
                isToday = i == 0, light = date == lightDay,
            )
        }

        val book = DayBadges.book(
            BadgeInput(
                today = today, from = input.bookFrom, to = MonthGrid.span(today).endInclusive,
                allMissions = input.allMissions, history = History(input.tasks), plans = input.plans,
                milestones = input.milestones, summaries = byWeek, firstDay = input.firstDay, logs = input.logs,
                lookBackDays = PlanRepository.HISTORY_DAYS, maxCrowns = PlanRepository.MAX_CROWNS,
            )
        )
        val weekStickers = (0..6).map { i ->
            val date = weekStart.plus(i, DateTimeUnit.DAY)
            WidgetSticker(key(date), if (date > today) null else book.on(date).firstOrNull()?.badge, future = date > today)
        }
        // Days running with a badge besides "Recharging day", as the Metrics tab's pattern counts them.
        var day = if (book.counts(today)) today else today.minus(1, DateTimeUnit.DAY)
        var run = 0
        while (book.counts(day)) {
            run++
            day = day.minus(1, DateTimeUnit.DAY)
        }
        val calendar = MonthGrid.build(
            today, today, input.allMissions, days, byWeek, PaceOffers.openedWeeks(input.tasks), book, selected = null,
        )
        val month = WidgetMonth(
            title = today.month.name.lowercase().replaceFirstChar { it.uppercase() },
            crowns = calendar.weeks.count { it.crowned },
            weeks = calendar.weeks.map { week ->
                WidgetMonthWeek(week.crowned, week.days.map {
                    WidgetMonthDay(key(it.date), it.date.dayOfMonth, it.inMonth, it.isToday, it.badges.firstOrNull()?.badge)
                })
            },
        )

        val moodsByIcon = input.moods.associateBy { it.mood.moodIcon }
        return WidgetSnapshot(
            dateKey = key(today),
            dateTitle = "${today.weekdayName()}, ${today.dayOfMonth} ${shortMonth(today)}",
            shortDate = "${today.weekdayName().take(3)} ${today.dayOfMonth}",
            timeLabel = clock(input.now),
            mode = mode,
            dayType = dayType(today),
            pillars = pillars,
            verdict = balance?.words ?: "In balance",
            verdictPillar = balance?.takeIf { !it.balanced }?.lowest,
            tilt = balance?.takeUnless { it.balanced }?.tilt ?: 0f,
            tasks = tasks,
            doneCount = tasks.count { it.done },
            totalCount = tasks.size,
            water = glasses(input.water),
            waterGoal = water_daily_limit,
            moods = MOOD_ORDER.mapNotNull { icon ->
                val badge = DayBadges.moodBadge(icon) ?: return@mapNotNull null
                val mood = moodsByIcon[icon] ?: return@mapNotNull null
                WidgetMood(icon, badge, active = mood.todayMood?.tmMoodStatus == "ACTIVE")
            },
            moodSet = moodSet,
            weekAhead = weekAhead,
            lightLine = lightDay?.let { "${it.weekdayName()} is light." },
            suggestion = if (lightDay != null && lightDay == roomDay) diary.didntFit?.let { "Move “${it.title}” there?" } else null,
            lightDateKey = lightDay?.let { key(it) },
            weekStickers = weekStickers,
            streakLine = when {
                run >= 2 -> "$run-day run · keep it up"
                run == 1 -> "1 day so far · keep going"
                else -> "One tick starts a run"
            },
            month = month,
        )
    }

    companion object {
        /** From 8 pm, an unset mood turns the Today widget into "How was today?". */
        const val EVENING_HOUR = 20
        /** Open scored minutes that make a big day. */
        const val BIG_DAY_MINUTES = 90
        /** How far back the sticker run looks. */
        private const val STREAK_DAYS = 30
        /** The faces the widgets offer, out of the diary's six. The first three are the evening's. */
        val MOOD_ORDER = listOf("happy", "bored", "sad", "dreamy", "angry")

        private val shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        private fun key(date: LocalDate): String = MyDate.fromLocalDate(date).dateString

        private fun shortMonth(date: LocalDate) = shortMonths[date.monthNumber - 1]

        /** The diary fills glasses up to the stored index; no row means none yet. */
        private fun glasses(row: WaterIntake?): Int = row?.let { (it.waterIntakeProgress.toInt() + 1).coerceIn(0, water_daily_limit) } ?: 0

        private fun clock(now: LocalDateTime): String {
            val h = now.hour % 12
            return "${if (h == 0) 12 else h}:${now.minute.toString().padStart(2, '0')} ${if (now.hour < 12) "AM" else "PM"}"
        }
    }
}

/**
 * Next up's place in today's open items, saved by each platform as "yyyy-MM-dd#index", so a new day
 * starts again at the first.
 */
object NextUp {
    fun index(saved: String?, dateKey: String, count: Int): Int {
        if (count == 0) return 0
        val (day, i) = saved?.split('#')?.takeIf { it.size == 2 } ?: return 0
        if (day != dateKey) return 0
        return (i.toIntOrNull() ?: 0).mod(count)
    }

    fun step(saved: String?, dateKey: String, count: Int, by: Int): String =
        "$dateKey#${if (count == 0) 0 else (index(saved, dateKey, count) + by).mod(count)}"
}

/** How each platform reloads its widgets; the app calls [changed] after its own changes. */
object WidgetHooks {
    var refresh: (() -> Unit)? = null

    fun changed() {
        refresh?.invoke()
    }
}
