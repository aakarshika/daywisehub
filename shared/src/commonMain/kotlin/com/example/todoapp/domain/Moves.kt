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
     * The first day in [room] with time for [mission] that doesn't already have it open;
     * the first day after [after] when none has.
     */
    fun target(mission: Mission, after: LocalDate, tasks: List<TodayTask>, room: Map<LocalDate, Int>): LocalDate =
        room.entries.firstOrNull { (day, left) ->
            left >= mission.durationMinutes &&
                tasks.none { it.date == day && it.missionId == mission.missionId && it.taskState.isOpen }
        }?.key ?: after.plus(1, DateTimeUnit.DAY)
}
