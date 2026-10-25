package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Where "Move it" sends a planned item (spec 6.4). The diary uses this to name the day on the chip,
 * and PlanRepository to do the move, so the two always agree.
 */
object Moves {

    /** Moves look at the 7 days after this one: the item's own day, or yesterday for anything older, so nothing lands in the past. */
    fun anchor(from: LocalDate, today: LocalDate): LocalDate = maxOf(from, today.minus(1, DateTimeUnit.DAY))

    /**
     * Free minutes left on each of the 7 days after [from], in date order: that day's free time
     * ([freeOn]) minus its open flexible items. Fixed blocks don't use free time.
     */
    fun roomByDay(
        from: LocalDate,
        minutesById: Map<Long, Int>,
        tasks: List<TodayTask>,
        freeOn: (LocalDate) -> Int,
    ): Map<LocalDate, Int> {
        val room = LinkedHashMap<LocalDate, Int>()
        for (i in 1..7) {
            val day = from.plus(i, DateTimeUnit.DAY)
            val planned = tasks
                .filter { it.date == day && !it.isFixedBlock && it.taskState.isOpen }
                .sumOf { minutesById[it.missionId] ?: 0 }
            room[day] = freeOn(day) - planned
        }
        return room
    }

    /**
     * The first day in [room] the mission is live on, with time for it, that doesn't already have it
     * open; the first live day when none has. Null when the mission isn't live on any of those days
     * (it ends or is paused), so the item is skipped instead.
     */
    fun target(mission: Mission, tasks: List<TodayTask>, room: Map<LocalDate, Int>): LocalDate? {
        val live = room.keys.filter { mission.isLiveOn(it) }
        return live.firstOrNull { day ->
            room.getValue(day) >= mission.durationMinutes &&
                tasks.none { it.date == day && it.missionId == mission.missionId && it.taskState.isOpen }
        } ?: live.firstOrNull()
    }

    /**
     * The item a move made from [task]: same mission, moved from [task]'s day, on a later day.
     * A move lands at most 7 days past [task]'s day or today, whichever is later, so [tasks] should reach that far.
     */
    fun copyOf(task: TodayTask, tasks: List<TodayTask>): TodayTask? =
        tasks.filter { it.missionId == task.missionId && it.movedFromDate == task.taskDate && it.date > task.date }
            .maxByOrNull { it.todayTaskId }

    /**
     * Ticking moved [task] takes the move back while its copy is still planned (PlanRepository.toggleDone).
     * Once the copy is ticked, skipped or moved on, the moved item stays as it is.
     */
    fun canTakeBack(task: TodayTask, tasks: List<TodayTask>): Boolean =
        task.taskState == TaskState.MOVED && copyOf(task, tasks)?.taskState == TaskState.PLANNED
}
