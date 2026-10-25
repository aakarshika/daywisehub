package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * Spec 6.4: anything still planned when its day is over follows its "if a day goes sideways"
 * answer. PlanRepository runs this before building today's list, on the first open of a new day.
 */
object DayClose {

    /** "Move it" carries an item from at most this many days back, so "Moved from Friday" is never weeks old. */
    const val CARRY_DAYS = 6

    enum class Outcome { MOVE, SKIP }

    /**
     * What happens to [task], still planned on a day before [today].
     * [alreadyComing] is true when the mission already has an open item from today on: one catch-up is enough.
     */
    fun outcome(task: TodayTask, mission: Mission?, today: LocalDate, alreadyComing: Boolean): Outcome = when {
        // Archived, paused or ended: nothing to catch up on.
        mission == null || !mission.isLiveOn(today) -> Outcome.SKIP
        // Fixed blocks start ticked; one still planned was unticked in the evening check.
        task.isFixedBlock -> Outcome.SKIP
        // "It has to be that day" and "skip it" just drop.
        mission.ifMissedRule != IfMissed.MOVE -> Outcome.SKIP
        task.date < today.minus(CARRY_DAYS, DateTimeUnit.DAY) -> Outcome.SKIP
        alreadyComing -> Outcome.SKIP
        else -> Outcome.MOVE
    }
}
