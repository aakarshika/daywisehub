package com.example.todoapp.db.sample

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.domain.Effort
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.IfMissed
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.Period
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.Points
import com.example.todoapp.domain.Reason
import com.example.todoapp.domain.ReasonCode
import com.example.todoapp.domain.TaskProgress
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.weekdayName
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.water_daily_limit
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * About 13 weeks laid out so the Metrics calendar shows every day badge, each on a day of its own where it
 * can, then the badge's day goes quiet so the next one starts clean. Used instead of SampleData.
 */
object BadgeSample {

    class Data(
        val missions: List<Mission>,
        val milestones: List<Milestone>,
        val tasks: List<TodayTask>,
        val plans: List<DayPlan>,
        /** Mood icon names ("happy") by day. */
        val moods: List<Pair<LocalDate, String>>,
        val fullWater: List<LocalDate>,
    )

    suspend fun seed(db: AppDatabase) = write(db, build(MyDate.now().toLocalDate()))

    /** Writes [data] to an empty database. ScreenshotSample uses it too. */
    suspend fun write(db: AppDatabase, data: Data) {
        data.missions.forEach { db.getMissionDao().upsert(it) }
        data.milestones.forEach { db.getMilestoneDao().insert(it) }
        db.getTodayTaskDao().insertAll(data.tasks)
        data.plans.forEach { db.getPlanDao().upsertPlan(it) }
        val moodIds = db.getTodayMoodDao().getAllMoods().first().associate { it.mood.moodIcon to it.mood.moodId }
        data.moods.forEach { (date, icon) ->
            moodIds[icon]?.let { db.getTodayMoodDao().upsertTodayMood(TodayMood(tmMoodId = it, tmMoodStatus = "ACTIVE", tmDate = MyDate.fromLocalDate(date))) }
        }
        data.fullWater.forEach {
            db.getWaterIntakeDao().upsertWaterIntake(WaterIntake(waterIntakeProgress = (water_daily_limit - 1).toDouble(), waterDate = MyDate.fromLocalDate(it)))
        }
    }

    // Mission ids, fixed so the tasks can name them before anything is saved.
    private const val WALK = 1L
    private const val READ = 2L
    private const val BUDGET = 3L
    private const val CLEAN = 4L
    private const val MOM = 5L
    private const val DATE = 6L
    private const val NAP = 7L
    private const val SPANISH = 8L
    private const val WORK = 9L
    private const val LAKE = 10L
    private const val POTTERY = 11L

    /**
     * Day n is [start] + n, a Monday 12 weeks before the week of 4 days ago, so the 30-day run ends at
     * least 3 days back. Active days are never more than 2 quiet days apart, except before "Back at it".
     */
    fun build(today: LocalDate): Data {
        val start = WeekMath.weekStart(today.minus(4, DateTimeUnit.DAY)).minus(12 * 7, DateTimeUnit.DAY)
        fun day(n: Int) = start.plus(n, DateTimeUnit.DAY)
        val weights = mutableMapOf<Long, Int>()
        val missions = listOf(
            mission(WALK, "Walk", PillarType.HEALTH, MissionKind.HABIT, 1, Effort.LIGHT, day(-1), period = Period.DAY),
            mission(READ, "Read", PillarType.LIFE, MissionKind.HABIT, 2, Effort.STEADY, day(-1), count = 4),
            mission(BUDGET, "Budget", PillarType.WEALTH, MissionKind.HABIT, 2, Effort.STEADY, day(-1)),
            mission(CLEAN, "Deep clean", PillarType.LIFE, MissionKind.HABIT, 2, Effort.DRAINING, day(-1)),
            mission(MOM, "Call Mom", PillarType.LOVE, MissionKind.HABIT, 2, Effort.LIGHT, day(-1)),
            mission(DATE, "Date night", PillarType.LOVE, MissionKind.HABIT, 3, Effort.STEADY, day(-1)),
            mission(NAP, "Nap", PillarType.RECHARGE, MissionKind.RECHARGE, 0, Effort.LIGHT, day(-1), count = 2),
            mission(SPANISH, "Learn Spanish", PillarType.LIFE, MissionKind.FINISH_LINE, 1, Effort.STEADY, day(-1), endDate = day(120)),
            mission(WORK, "Work", PillarType.WEALTH, MissionKind.BLOCK, 1, Effort.STEADY, day(-1),
                weekDays = "1,2,3,4,5", startTime = "9:00", endTime = "5:00"),
            // Monthly, in Recharge so they add no target: one comes back after a missed month, one after two.
            mission(LAKE, "Visit the lake", PillarType.RECHARGE, MissionKind.RHYTHM, 1, Effort.LIGHT, day(53 - 65),
                intervalCount = 1, intervalUnit = Period.MONTH),
            mission(POTTERY, "Pottery class", PillarType.RECHARGE, MissionKind.RHYTHM, 1, Effort.LIGHT, day(55 - 100),
                intervalCount = 1, intervalUnit = Period.MONTH),
        ).onEach { weights[it.missionId] = it.weight }

        val tasks = mutableListOf<TodayTask>()
        fun add(id: Long, n: Int, state: TaskState, reason: Reason = habit, crowned: Boolean = false, fixed: Boolean = false, movedFrom: Int? = null) {
            tasks += TodayTask(
                missionId = id,
                taskDate = MyDate.fromLocalDate(day(n)),
                state = state.name,
                progress = TaskProgress.stored(state),
                pointsEarned = Points.forState(weights.getValue(id), state),
                isFixedBlock = fixed,
                crowned = crowned,
                movedFromDate = movedFrom?.let { MyDate.fromLocalDate(day(it)) },
                reasonCode = reason.code.name,
                reasonArgs = reason.encodeArgs(),
            )
        }
        fun done(n: Int, vararg ids: Long) = ids.forEach { add(it, n, TaskState.DONE, if (it == WALK) walk else habit) }
        fun planned(n: Int, vararg ids: Long) = ids.forEach { add(it, n, TaskState.PLANNED) }

        add(READ, 0, TaskState.PARTIAL); planned(0, BUDGET)                  // Did some
        done(2, WALK); planned(2, READ, BUDGET)                                        // First one
        done(4, NAP); planned(4, READ, BUDGET)                                         // Rested on purpose
        done(7, WALK); planned(7, READ, BUDGET)                                        // Strong start (Monday)
        done(9, READ); planned(9, BUDGET)                                              // Halfway
        done(10, WALK); planned(10, READ, BUDGET)                                      // Two in a row
        done(12, WALK, READ); planned(12, BUDGET, CLEAN, SPANISH)                      // Weekend warrior (Saturday)
        add(WORK, 14, TaskState.DONE, block, fixed = true); done(14, WALK); planned(14, READ, BUDGET, CLEAN) // Showed up
        add(READ, 16, TaskState.DONE, Reason(ReasonCode.ADDED)); planned(16, BUDGET, CLEAN)                  // Bonus round
        add(READ, 17, TaskState.MOVED)
        add(READ, 18, TaskState.DONE, Reason(ReasonCode.MOVED, listOf(day(17).weekdayName())), movedFrom = 17)
        planned(18, BUDGET, CLEAN)                                                     // Second chance
        done(21, WALK, READ, NAP); planned(21, BUDGET, CLEAN, SPANISH)                 // Hat trick
        add(READ, 23, TaskState.DONE, crowned = true); planned(23, BUDGET, CLEAN, WALK) // Crowned
        done(25, CLEAN); planned(25, READ, BUDGET, WALK)                               // Heavy lifting
        done(28, READ, BUDGET, CLEAN, WALK); planned(28, SPANISH, NAP)                 // Full enough
        done(30, WALK, READ); planned(30, BUDGET)                                      // We'll count it
        add(READ, 32, TaskState.DONE, Reason(ReasonCode.RHYTHM_OVERDUE, listOf("1 week"))); planned(32, BUDGET, CLEAN) // Overdue no more
        // Days 33 to 36 are quiet: the first three recharge, the fourth stays blank.
        done(37, WALK); planned(37, READ, BUDGET)                                      // Back at it
        done(39, READ, BUDGET, CLEAN, WALK); planned(39, SPANISH, NAP)                 // Gentle win (a low-energy day)
        done(42, WALK, BUDGET, MOM, READ); planned(42, CLEAN, SPANISH)                 // All four
        listOf(WALK, READ, BUDGET).forEach { add(it, 44, TaskState.DONE, if (it == WALK) walk else habit, crowned = true) }
        planned(44, CLEAN, SPANISH, MOM)                                               // All three crowns
        done(46, READ, BUDGET)                                                         // Clean sweep
        // A 3-day walk run, the best that the 30-day run passes on its fourth day.
        (47..49).forEach { done(it, WALK); planned(it, READ) }
        // Day 51: the first Spanish milestone, nothing else. Milestone.
        add(LAKE, 53, TaskState.DONE, firstTime); planned(53, READ, BUDGET)            // The return
        add(POTTERY, 55, TaskState.DONE, firstTime); planned(55, READ)                 // The re-return
        // Walk every day from 56 to 85: a personal best on 59, 7 days on 62, 30 on 85.
        (56..85).forEach { done(it, WALK) }
        // The week of day 63 has every pillar full by its Saturday: Conqueror. Clean sweeps on the way.
        listOf(63, 64, 65, 66, 67).forEach { add(WORK, it, TaskState.DONE, block, fixed = true) }
        done(63, READ); done(64, BUDGET, READ); done(65, MOM, CLEAN); done(66, DATE, READ); done(67, READ)
        // The rest of the run: a Read left planned keeps a walk from being a full day.
        ((56..85) - (63..67)).forEach { planned(it, READ) }

        val milestones = listOf(51, 72).mapIndexed { i, n ->
            Milestone(
                missionId = SPANISH, text = if (i == 0) "Order food in Spanish" else "Hold a 10-minute chat",
                priority = "MEDIUM", expectedCompletionDate = MyDate.fromLocalDate(day(n)), milestoneOrder = i + 1,
                status = TaskStatus.COMPLETED.value, progressValue = 1f, progressUnit = 1f, progressText = "",
                mileStartDate = MyDate.fromLocalDate(day(-1)), actualCompletionDate = MyDate.fromLocalDate(day(n)),
                completionTaskId = null,
            )
        }
        val plans = listOf(DayPlan(date = MyDate.fromLocalDate(day(39)).dateString, freeMinutes = 180, energy = Energy.LOW.name))
        // Quiet days with something logged: a mood sticker, or the full glass. Today too.
        val moods = listOf(5 to "happy", 11 to "sad", 13 to "angry", 17 to "dreamy", 20 to "bored", 26 to "confused", 86 to "happy")
            .map { (n, icon) -> day(n) to icon } + (today to "dreamy")
        val water = listOf(6, 19, 27, 87).map { day(it) } + today
        return Data(missions, milestones, tasks, plans, moods, water)
    }

    private val habit = Reason(ReasonCode.QUOTA, listOf("0", "1", "7"))
    private val walk = Reason(ReasonCode.STREAK, listOf("0"))
    private val block = Reason(ReasonCode.BLOCK, listOf("9:00", "5:00"))
    private val firstTime = Reason(ReasonCode.RHYTHM_FIRST, listOf("1 month"))

    private fun mission(
        id: Long,
        title: String,
        pillar: PillarType,
        kind: MissionKind,
        weight: Int,
        effort: Effort,
        start: LocalDate,
        count: Int = 1,
        period: Period = Period.WEEK,
        intervalCount: Int = 0,
        intervalUnit: Period = Period.WEEK,
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
        durationMinutes = 30,
        effort = effort.name,
        ifMissed = IfMissed.MOVE.name,
        count = count,
        period = period.name,
        intervalCount = intervalCount,
        intervalUnit = intervalUnit.name,
        weekDays = weekDays,
        startTime = startTime,
        endTime = endTime,
        startDate = MyDate.fromLocalDate(start),
        endDate = endDate?.let { MyDate.fromLocalDate(it) },
    )
}
