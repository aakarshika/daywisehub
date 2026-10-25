package com.example.todoapp.db

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Effort
import com.example.todoapp.domain.IfMissed
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.Period
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.Reason
import com.example.todoapp.domain.ReasonCode
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/**
 * Seeds the mockups' example missions on an empty database. Off since missions can be created in the app;
 * turn it on to try the diary with history without typing missions in.
 */
const val SEED_SAMPLE_MISSIONS = false

object SampleData {

    suspend fun seed(missionRepository: MissionRepository, todayTaskRepository: TodayTaskRepository) {
        val today = MyDate.now()
        val missions = missions(start = today.subtractDays(120), today = today)
        val ids = missions.associate { it.title to missionRepository.save(it) }
        val history = buildList {
            val gym = Reason(ReasonCode.STREAK, listOf("0"))
            (1..9).forEach { add(done(ids.getValue("Gym"), today.subtractDays(it), points = 2, reason = gym)) }
            add(done(ids.getValue("Call Grandma"), today.subtractDays(13), points = 2,
                reason = Reason(ReasonCode.RHYTHM_FIRST, listOf("2 weeks"))))
            add(done(ids.getValue("Plan winter trip"), today.subtractDays(89), points = 3,
                reason = Reason(ReasonCode.RHYTHM_FIRST, listOf("3 months"))))
            // This week's earlier work blocks, already ticked as fixed blocks start out.
            val blocks = missions.filter { it.title == "Job" || it.title == "App" }
            val todayDate = today.toLocalDate()
            var day = WeekMath.weekStart(todayDate)
            while (day < todayDate) {
                if (day.dayOfWeek.isoDayNumber <= 5) {
                    blocks.forEach { block ->
                        add(done(ids.getValue(block.title), MyDate.fromLocalDate(day), points = 3,
                            reason = Reason(ReasonCode.BLOCK, listOf(block.startTime.orEmpty(), block.endTime.orEmpty())),
                            isFixedBlock = true))
                    }
                }
                day = day.plus(1, DateTimeUnit.DAY)
            }
        }
        todayTaskRepository.insertAll(history)
    }

    private fun done(missionId: Long, date: MyDate, points: Int, reason: Reason, isFixedBlock: Boolean = false) = TodayTask(
        missionId = missionId,
        taskDate = date,
        state = TaskState.DONE.name,
        pointsEarned = points,
        isFixedBlock = isFixedBlock,
        reasonCode = reason.code.name,
        reasonArgs = reason.encodeArgs(),
    )

    private fun missions(start: MyDate, today: MyDate): List<Mission> = listOf(
        mission("Job", PillarType.WEALTH, MissionKind.BLOCK, 3, 240, Effort.STEADY, IfMissed.SAME_DAY, start,
            weekDays = "1,2,3,4,5", startTime = "9:00", endTime = "1:00"),
        mission("App", PillarType.WEALTH, MissionKind.BLOCK, 3, 180, Effort.STEADY, IfMissed.SAME_DAY, start,
            weekDays = "1,2,3,4,5", startTime = "2:00", endTime = "5:00"),
        mission("Gym", PillarType.HEALTH, MissionKind.HABIT, 2, 45, Effort.STEADY, IfMissed.MOVE, start,
            period = Period.DAY),
        mission("Call Mom", PillarType.LOVE, MissionKind.HABIT, 2, 15, Effort.LIGHT, IfMissed.MOVE, start,
            weekDays = "1"),
        mission("Call sister", PillarType.LOVE, MissionKind.HABIT, 2, 15, Effort.LIGHT, IfMissed.MOVE, start,
            weekDays = "2"),
        mission("Call Grandma", PillarType.LOVE, MissionKind.RHYTHM, 2, 15, Effort.LIGHT, IfMissed.MOVE, start,
            intervalCount = 2, intervalUnit = Period.WEEK),
        mission("Guitar", PillarType.LIFE, MissionKind.HABIT, 3, 30, Effort.STEADY, IfMissed.MOVE, start,
            count = 2, goalCount = 3),
        mission("Dance class", PillarType.LIFE, MissionKind.FINISH_LINE, 3, 60, Effort.DRAINING, IfMissed.SAME_DAY, start,
            endDate = today.addDays(56)),
        mission("New ideas", PillarType.LIFE, MissionKind.HABIT, 1, 30, Effort.LIGHT, IfMissed.SKIP, start,
            count = 3),
        mission("Plan winter trip", PillarType.LIFE, MissionKind.RHYTHM, 3, 120, Effort.STEADY, IfMissed.MOVE, start,
            intervalCount = 3, intervalUnit = Period.MONTH),
        mission("New episode", PillarType.RECHARGE, MissionKind.RECHARGE, 0, 45, Effort.LIGHT, IfMissed.SKIP, start,
            count = 2),
    )

    private fun mission(
        title: String,
        pillar: PillarType,
        kind: MissionKind,
        weight: Int,
        minutes: Int,
        effort: Effort,
        ifMissed: IfMissed,
        start: MyDate,
        count: Int = 1,
        period: Period = Period.WEEK,
        goalCount: Int? = null,
        intervalCount: Int = 0,
        intervalUnit: Period = Period.WEEK,
        weekDays: String = "",
        startTime: String? = null,
        endTime: String? = null,
        endDate: MyDate? = null,
    ) = Mission(
        title = title,
        pillar = pillar.name,
        kind = kind.name,
        weight = weight,
        durationMinutes = minutes,
        effort = effort.name,
        ifMissed = ifMissed.name,
        count = count,
        period = period.name,
        goalCount = goalCount,
        intervalCount = intervalCount,
        intervalUnit = intervalUnit.name,
        weekDays = weekDays,
        startTime = startTime,
        endTime = endTime,
        startDate = start,
        endDate = endDate,
    )
}
