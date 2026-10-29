package com.example.todoapp.db.sample

import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Reason
import com.example.todoapp.domain.ReasonCode
import com.example.todoapp.domain.TaskProgress
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.weekdayName
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Five more weeks of history for the Metrics tab, on top of SampleData's missions, to try the rings,
 * crowns, patterns and calendar without weeks of ticking.
 */
object MetricsSample {

    /** Weeks written before this one. */
    private const val WEEKS = 5
    /** SampleData already ticked Gym on each of the last 21 days. */
    private const val SAMPLE_GYM_DAYS = 21

    suspend fun seed(missionRepository: MissionRepository, todayTaskRepository: TodayTaskRepository) {
        val ids = missionRepository.getAll().associate { it.title to it.missionId }
        todayTaskRepository.insertAll(history(ids, MyDate.now().toLocalDate()))
    }

    /**
     * The tasks, given SampleData's mission ids by title. Days SampleData already wrote are left alone.
     * Weeks 2 and 4 back come out balanced; weeks 1, 3 and 5 have Life (and in week 5 Love) light.
     */
    fun history(ids: Map<String, Long>, today: LocalDate): List<TodayTask> {
        val tasks = mutableListOf<TodayTask?>()
        val thisWeek = WeekMath.weekStart(today)
        fun task(title: String, date: LocalDate, state: TaskState, points: Int, reason: Reason, fixed: Boolean = false, movedFrom: LocalDate? = null) =
            ids[title]?.let { id ->
                TodayTask(
                    missionId = id,
                    taskDate = MyDate.fromLocalDate(date),
                    state = state.name,
                    progress = TaskProgress.stored(state),
                    pointsEarned = points,
                    isFixedBlock = fixed,
                    movedFromDate = movedFrom?.let { MyDate.fromLocalDate(it) },
                    reasonCode = reason.code.name,
                    reasonArgs = reason.encodeArgs(),
                )
            }
        fun done(title: String, date: LocalDate, points: Int, reason: Reason = Reason(ReasonCode.ADDED)) =
            task(title, date, TaskState.DONE, points, reason)
        for (back in 1..WEEKS) {
            val monday = thisWeek.minus(7 * back, DateTimeUnit.DAY)
            fun day(offset: Int) = monday.plus(offset, DateTimeUnit.DAY)
            // Work blocks Monday to Friday, ticked. One App block 3 weeks back is skipped: a white slice.
            val job = Reason(ReasonCode.BLOCK, listOf("9:00", "1:00"))
            val app = Reason(ReasonCode.BLOCK, listOf("2:00", "5:00"))
            for (offset in 0..4) {
                tasks += task("Job", day(offset), TaskState.DONE, 3, job, fixed = true)
                val skipped = back == 3 && offset == 2
                tasks += task("App", day(offset), if (skipped) TaskState.SKIPPED else TaskState.DONE, if (skipped) 0 else 3, app, fixed = true)
            }
            // Gym on every day SampleData didn't write, with one gap 5 weeks back.
            for (offset in 0..6) {
                val date = day(offset)
                if (date.daysUntil(today) > SAMPLE_GYM_DAYS && !(back == WEEKS && offset == 2)) {
                    tasks += done("Gym", date, 2, Reason(ReasonCode.STREAK, listOf("0")))
                }
            }
            // Call Mom on Mondays; Call sister on a different day each week.
            tasks += done("Call Mom", day(0), 2)
            when (back) {
                1 -> tasks += done("Call sister", day(1), 2)
                2 -> tasks += done("Call sister", day(3), 2)
                3 -> {
                    // Moved from Tuesday and done on Wednesday.
                    tasks += task("Call sister", day(1), TaskState.MOVED, 0, Reason(ReasonCode.ADDED))
                    tasks += task("Call sister", day(2), TaskState.DONE, 2, Reason(ReasonCode.MOVED, listOf(day(1).weekdayName())), movedFrom = day(1))
                }
                4 -> tasks += done("Call sister", day(4), 2)
                else -> tasks += task("Call sister", day(1), TaskState.SKIPPED, 0, Reason(ReasonCode.ADDED))
            }
            // Guitar at its gentle 2× (SampleData wrote the last 3 weeks), Dance class on Saturdays, New ideas twice.
            if (back > 3) {
                tasks += done("Guitar", day(0), 3)
                tasks += done("Guitar", day(3), 3)
            }
            if (back < WEEKS) tasks += done("Dance class", day(5), 3)
            if (back == 2 || back == 4) tasks += done("New ideas", day(2), 1)
        }
        // Call Grandma every 2 weeks before SampleData's call 13 days ago.
        tasks += done("Call Grandma", today.minus(27, DateTimeUnit.DAY), 2)
        tasks += done("Call Grandma", today.minus(41, DateTimeUnit.DAY), 2)
        return tasks.filterNotNull()
    }
}
