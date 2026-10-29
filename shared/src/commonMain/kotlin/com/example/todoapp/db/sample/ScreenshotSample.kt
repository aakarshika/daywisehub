package com.example.todoapp.db.sample

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.factor.DayFactor
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.week.PaceOffer
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.db.sample.ShotOptions.Offer
import com.example.todoapp.db.sample.ShotOptions.Stage
import com.example.todoapp.db.sample.ShotOptions.Today
import com.example.todoapp.domain.Effort
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.IfMissed
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.OfferResponse
import com.example.todoapp.domain.OfferType
import com.example.todoapp.domain.Period
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.Points
import com.example.todoapp.domain.Reason
import com.example.todoapp.domain.ReasonCode
import com.example.todoapp.domain.TaskProgress
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.TaskState.DONE
import com.example.todoapp.domain.TaskState.MOVED
import com.example.todoapp.domain.TaskState.PARTIAL
import com.example.todoapp.domain.TaskState.PLANNED
import com.example.todoapp.domain.TaskState.SKIPPED
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.shortLabel
import com.example.todoapp.domain.weekdayName
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.water_daily_limit
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Three months of one believable life for the store screenshots, written around Thursday [ANCHOR]
 * (shoot with `-shotDate 2026-10-29`, or an earlier Thursday to watch October fill up). ShotOptions
 * picks the scene. Every day is drawn by hand so the calendar wears only pies (Did some, Halfway,
 * Full enough, Clean sweep), Recharging-day moons, milestone flags and at most one cake: no crowns
 * and no comebacks. A day shows its rarest badge, so each recipe keeps the rarer ones off:
 * - a week's pillars stay under full (80%), except the cake's Love on Oct 24;
 * - Halfway follows a day with nothing ticked, or "Two in a row" would show instead;
 * - quiet days come at most two in a row, or the next active day is "Back at it";
 * - quiet days log no mood, or "Felt happy" would cover the moon;
 * - past leftovers are skipped, as the app's own day close would leave them, so nothing moves on launch.
 */
object ScreenshotSample {

    val ANCHOR = LocalDate(2026, 10, 29)
    /** A Monday, so every week below starts on one. */
    private val START = LocalDate(2026, 8, 3)

    private const val RUN = 1L
    private const val WORK = 2L
    private const val MOM = 3L
    private const val DATE = 4L
    private const val DANCE = 5L
    private const val SPANISH = 6L
    private const val BATH = 7L

    /**
     * A day's items. C is a clean sweep, F full enough, H halfway, D did some, R a rest (the moon),
     * K the cake. Weekdays hold the Deep work block, ticked for you when done.
     */
    private val recipes: Map<String, List<Pair<Long, TaskState>>> = mapOf(
        "C1" to listOf(WORK to DONE, RUN to DONE, SPANISH to DONE),
        "C2" to listOf(WORK to DONE, DATE to DONE, BATH to DONE),
        "C3" to listOf(WORK to DONE, SPANISH to DONE, BATH to DONE),
        "C5" to listOf(WORK to DONE, DANCE to DONE, BATH to DONE),
        "CS" to listOf(WORK to SKIPPED, SPANISH to DONE, BATH to DONE),
        "F1" to listOf(WORK to DONE, RUN to DONE, SPANISH to DONE, DANCE to PARTIAL, BATH to SKIPPED),
        "F2" to listOf(WORK to DONE, RUN to DONE, DATE to DONE, DANCE to PARTIAL, BATH to SKIPPED),
        "HR" to listOf(WORK to PARTIAL, RUN to DONE, SPANISH to SKIPPED),
        "HS" to listOf(WORK to PARTIAL, SPANISH to DONE, RUN to SKIPPED),
        "HM" to listOf(WORK to PARTIAL, MOM to DONE, RUN to SKIPPED),
        "DD" to listOf(WORK to PARTIAL, DANCE to PARTIAL, RUN to SKIPPED),
        "DB" to listOf(BATH to PARTIAL, RUN to SKIPPED),
        "DW" to listOf(WORK to PARTIAL, RUN to SKIPPED),
        "DX" to listOf(BATH to PARTIAL),
        "RW" to listOf(WORK to SKIPPED, RUN to SKIPPED, BATH to SKIPPED),
        "RE" to listOf(RUN to SKIPPED, BATH to SKIPPED),
        "KM" to listOf(MOM to DONE, BATH to DONE),
    )

    // Weeks, Monday first. Each keeps Health ≤ 4 of 6, Wealth ≤ 11 of 15, Love ≤ 2 of 4, Life ≤ 7 of 10.
    private const val A = "C1 F1 RW HM C5 RE DB"
    private const val A2 = "C1 F2 RW HS C5 DB RE"
    private const val B = "HR C2 F1 DD CS RE RE"
    /** Oct 19: Dance class moves from Saturday to Tuesday and Morning run from Sunday to Monday. */
    private const val LAST = "HR C2 C3 F1 RW RE DX"
    /** Oct 26: Monday and Tuesday do the moved items; Wednesday moves Dance class into next week. */
    private const val THIS = "C1 C5 DW"
    private val weeks = listOf(A, B, A2, B, A, B, A2, B, A, B, A2, LAST, THIS)

    private val milestoneTexts = listOf("Order coffee in Spanish", "Ask for directions", "Hold a 5-minute chat", "Watch a film without subtitles")
    private val milestonesReached = listOf(LocalDate(2026, 9, 11), LocalDate(2026, 10, 9), LocalDate(2026, 10, 21))

    private val moods = listOf("happy", "happy", "dreamy", "happy", "bored", "happy", "dreamy")
    private val factors = mapOf(
        LocalDate(2026, 10, 10) to "travel", LocalDate(2026, 10, 11) to "travel",
        LocalDate(2026, 10, 18) to "weather", LocalDate(2026, 10, 23) to "family",
    )

    suspend fun seed(db: AppDatabase) {
        val today = MyDate.now().toLocalDate()
        BadgeSample.write(db, build(today))
        val week = MyDate.fromLocalDate(WeekMath.weekStart(today)).dateString
        // This week's offers are made here, so the app doesn't pick its own on the first open.
        val lighter = ShotOptions.offer == Offer.LIGHTER || ShotOptions.offer == Offer.BOTH
        val stepUp = ShotOptions.offer == Offer.STEPUP || ShotOptions.offer == Offer.BOTH
        db.getWeekDao().insertOffers(listOfNotNull(
            PaceOffer(missionId = DANCE, type = OfferType.LOWER.name, week = week, response = if (lighter) null else OfferResponse.KEPT.name),
            if (stepUp) PaceOffer(missionId = DATE, type = OfferType.STEP_UP.name, week = week) else null,
        ))
        val (water, notes) = when (ShotOptions.today) {
            Today.MORNING -> 1.0 to ""
            Today.MIDDAY -> 4.0 to "Coffee with Ana at 4. Ask her about the Lisbon trip!"
            Today.EVENING -> (water_daily_limit - 1).toDouble() to "Coffee with Ana. Lisbon is booked for March!"
            Today.REST -> 3.0 to ""
        }
        db.getWaterIntakeDao().upsertWaterIntake(WaterIntake(waterIntakeProgress = water, notes = notes, waterDate = MyDate.fromLocalDate(today)))
        factors.filterKeys { it < today }.forEach { (date, icon) -> db.getDayFactorDao().insert(DayFactor(MyDate.fromLocalDate(date), icon)) }
    }

    fun build(today: LocalDate): BadgeSample.Data {
        val stage = ShotOptions.stage
        val missionStart = START.minus(1, DateTimeUnit.DAY)
        val missions = listOf(
            mission(RUN, "Morning run", PillarType.HEALTH, MissionKind.HABIT, 2, 30, Effort.STEADY, missionStart, count = 3),
            mission(WORK, "Deep work", PillarType.WEALTH, MissionKind.BLOCK, 3, 180, Effort.STEADY, missionStart,
                weekDays = "1,2,3,4,5", startTime = "9:00", endTime = "12:00"),
            mission(MOM, "Call Mom", PillarType.LOVE, MissionKind.HABIT, 2, 20, Effort.LIGHT, missionStart),
            mission(DATE, "Date night", PillarType.LOVE, MissionKind.HABIT, 2, 120, Effort.STEADY, missionStart),
            mission(DANCE, "Dance class", PillarType.LIFE, MissionKind.HABIT, 2, 60, Effort.STEADY, missionStart, count = 3),
            mission(SPANISH, "Learn Spanish", PillarType.LIFE, MissionKind.FINISH_LINE, 2, 30, Effort.STEADY, missionStart,
                count = 2, endDate = LocalDate(2026, 12, 31)),
            mission(BATH, "Bath & a book", PillarType.RECHARGE, MissionKind.RECHARGE, 0, 45, Effort.LIGHT, missionStart, count = 2),
        )
        val byId = missions.associateBy { it.missionId }

        val tasks = mutableListOf<TodayTask>()
        val plans = mutableListOf<DayPlan>()
        val moodDays = mutableListOf<Pair<LocalDate, String>>()
        val water = mutableListOf<LocalDate>()
        // Ticks so far in each week, by mission, for the "1 of 3 this week" lines.
        val doneInWeek = mutableMapOf<Pair<LocalDate, Long>, Int>()

        fun add(id: Long, date: LocalDate, state: TaskState, order: Int, reason: Reason? = null, movedFrom: LocalDate? = null,
                autoTicked: Boolean = false, progress: Int? = null) {
            val mission = byId.getValue(id)
            val monday = WeekMath.weekStart(date)
            val before = doneInWeek[monday to id] ?: 0
            val left = (7 - monday.daysUntil(date)).toString()
            val why = reason ?: when (id) {
                WORK -> Reason(ReasonCode.BLOCK, listOf("9:00", "12:00"))
                BATH -> Reason(ReasonCode.RECHARGE_USUAL, listOf("2"))
                else -> Reason(ReasonCode.QUOTA, listOf(before.toString(), mission.count.toString(), left))
            }
            tasks += TodayTask(
                missionId = id,
                taskDate = MyDate.fromLocalDate(date),
                state = state.name,
                progress = TaskProgress.stored(state, progress),
                pointsEarned = Points.forState(mission.weight, state),
                isFixedBlock = id == WORK,
                movedFromDate = movedFrom?.let { MyDate.fromLocalDate(it) },
                reasonCode = why.code.name,
                reasonArgs = why.encodeArgs(),
                sortOrder = order,
                autoTicked = autoTicked,
            )
            if (state == DONE || state == PARTIAL) doneInWeek[monday to id] = before + 1
        }

        // The cake: Love fills on Saturday Oct 24, when Call Mom joins Tuesday's Date night.
        val cakeDay = LocalDate(2026, 10, 24)
        val moves = mapOf(
            LocalDate(2026, 10, 24) to (DANCE to LocalDate(2026, 10, 27)),
            LocalDate(2026, 10, 25) to (RUN to LocalDate(2026, 10, 26)),
            LocalDate(2026, 10, 28) to (DANCE to LocalDate(2026, 11, 2)),
        )
        val movedIn = moves.entries.associate { (from, move) -> (move.second to move.first) to from }
        // On an earlier day (watching October fill up), today is that day's own recipe, so its sticker fits the month.
        val onAnchor = today == ANCHOR

        weeks.forEachIndexed { w, week ->
            week.split(" ").forEachIndexed { d, code ->
                val date = START.plus(w * 7 + d, DateTimeUnit.DAY)
                if (date > today || (date == today && onAnchor)) return@forEachIndexed
                val recipe = if (date == cakeDay && stage == Stage.CAKE) "KM" else code
                val items = recipes.getValue(recipe)
                items.forEachIndexed { order, (id, state) ->
                    val from = movedIn[date to id]
                    val reason = from?.let { Reason(ReasonCode.MOVED, listOf(it.weekdayName())) }
                    // A ticked block was ticked for you; one done in part was changed by hand.
                    add(id, date, state, order, reason, movedFrom = from, autoTicked = id == WORK && state == DONE)
                }
                moves[date]?.let { (id, _) -> add(id, date, MOVED, items.size) }
                val rest = recipe.startsWith("R")
                plans += DayPlan(
                    date = MyDate.fromLocalDate(date).dateString, freeMinutes = if (rest) 60 else 180,
                    energy = if (rest) Energy.LOW.name else Energy.NORMAL.name, confirmed = true, fixedTicked = true,
                )
                if (!rest) {
                    moodDays += date to moods[(w + d) % moods.size]
                    if ((w * 7 + d) % 3 == 0) water += date
                }
            }
        }
        // Moves that land on or after today wait there, planned.
        moves.forEach { (from, move) ->
            val (id, to) = move
            if (from < today && to > today) {
                add(id, to, PLANNED, 0, Reason(ReasonCode.MOVED, listOf(from.weekdayName())), movedFrom = from)
            }
        }
        // Last week's card shows on today's page only when today is the first day opened this week.
        if (ShotOptions.weekNotes) plans.removeAll { MyDate(it.date).toLocalDate() >= WeekMath.weekStart(today) }

        val milestones = milestoneTexts.mapIndexed { i, text ->
            val reached = milestonesReached.getOrNull(i)?.takeIf { stage != Stage.PIES && it < today }
            val due = when {
                reached != null -> reached
                i == milestoneTexts.lastIndex && ShotOptions.milestoneDue -> today
                stage == Stage.PIES -> LocalDate(2026, 11, 5).plus(14 * i, DateTimeUnit.DAY)
                else -> milestonesReached.getOrNull(i) ?: LocalDate(2026, 11, 20)
            }
            milestone(text, i + 1, missionStart, due, reached)
        }
        val nextMilestone = milestones.indexOfFirst { it.actualCompletionDate == null }

        // Today: Deep work starts ticked, then the day's own picks, by how far the day has got.
        val left = (7 - WeekMath.weekStart(today).daysUntil(today)).toString()
        fun quota(id: Long) = Reason(ReasonCode.QUOTA, listOf((doneInWeek[WeekMath.weekStart(today) to id] ?: 0).toString(),
            byId.getValue(id).count.toString(), left))
        val spanishReason = if (ShotOptions.milestoneDue && nextMilestone >= 0) {
            Reason(ReasonCode.MILESTONE, listOf((nextMilestone + 1).toString(), milestones.size.toString(), today.shortLabel()))
        } else quota(SPANISH)
        val rest = ShotOptions.today == Today.REST
        // A Recharging day: the block is skipped and only Bath & a book is left, so the Today widget shows its moon.
        val todayItems = if (rest) listOf(
            Triple(WORK, Reason(ReasonCode.BLOCK, listOf("9:00", "12:00")), SKIPPED),
            Triple(BATH, Reason(ReasonCode.RECHARGE_USUAL, listOf("2")), PLANNED),
        ) else listOf(
            Triple(WORK, Reason(ReasonCode.BLOCK, listOf("9:00", "12:00")), DONE),
            Triple(RUN, quota(RUN), if (ShotOptions.today == Today.MORNING) PLANNED else DONE),
            Triple(SPANISH, spanishReason, if (ShotOptions.today == Today.MORNING) PLANNED else DONE),
            Triple(MOM, quota(MOM), if (ShotOptions.today == Today.EVENING) DONE else PLANNED),
            Triple(DATE, Reason(ReasonCode.PILLAR_LIGHT, listOf(PillarType.LOVE.title)), if (ShotOptions.today == Today.EVENING) PARTIAL else PLANNED),
            Triple(BATH, Reason(ReasonCode.RECHARGE_USUAL, listOf("2")), PLANNED),
        )
        if (onAnchor) {
            todayItems.forEachIndexed { order, (id, reason, state) ->
                add(id, today, state, order, reason, autoTicked = id == WORK && state == DONE, progress = if (state == PARTIAL) 60 else null)
            }
            plans += DayPlan(
                date = MyDate.fromLocalDate(today).dateString, freeMinutes = if (rest) 60 else 240,
                energy = if (rest) Energy.LOW.name else Energy.NORMAL.name,
                confirmed = ShotOptions.today != Today.MORNING, fixedTicked = true,
            )
            if (ShotOptions.today != Today.MORNING && !rest) moodDays += today to "happy"
        }

        return BadgeSample.Data(missions, milestones, tasks, plans, moodDays, water)
    }

    private fun milestone(text: String, order: Int, start: LocalDate, due: LocalDate, reached: LocalDate?) = Milestone(
        missionId = SPANISH, text = text, priority = "MEDIUM",
        expectedCompletionDate = MyDate.fromLocalDate(due), milestoneOrder = order,
        status = if (reached != null) TaskStatus.COMPLETED.value else TaskStatus.IN_PROGRESS.value,
        progressValue = if (reached != null) 1f else 0f, progressUnit = 1f, progressText = "",
        mileStartDate = MyDate.fromLocalDate(start), actualCompletionDate = reached?.let { MyDate.fromLocalDate(it) },
        completionTaskId = null,
    )

    private fun mission(
        id: Long,
        title: String,
        pillar: PillarType,
        kind: MissionKind,
        weight: Int,
        minutes: Int,
        effort: Effort,
        start: LocalDate,
        count: Int = 1,
        weekDays: String = "",
        startTime: String? = null,
        endTime: String? = null,
        endDate: LocalDate? = null,
    ) = Mission(
        missionId = id,
        title = title,
        pillar = pillar.name,
        kind = kind.name,
        weight = weight,
        durationMinutes = minutes,
        effort = effort.name,
        ifMissed = IfMissed.MOVE.name,
        count = count,
        period = Period.WEEK.name,
        weekDays = weekDays,
        startTime = startTime,
        endTime = endTime,
        startDate = MyDate.fromLocalDate(start),
        endDate = endDate?.let { MyDate.fromLocalDate(it) },
    )
}
