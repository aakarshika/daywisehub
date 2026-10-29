package com.example.todoapp.domain

import com.example.todoapp.db.data.todotask.TodayTask
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

/**
 * Spec 6.5: how much of an item got done, as a percent in [STEP] steps. The state still decides the
 * points (spec 5): a did-some earns half whatever its percent, and only 100% earns them all.
 */
object TaskProgress {

    const val STEP = 5
    const val FULL = 100
    /** A did-some with no percent of its own: the evening check's "Did some", or one saved before plan 6. */
    const val HALF = 50

    /** The percent [task] shows. */
    fun shown(task: TodayTask): Int = shown(task.taskState, task.progress)

    /** Done shows 100, a did-some its stored percent (50 when it has none) and anything else 0. */
    fun shown(state: TaskState, progress: Int): Int = when (state) {
        TaskState.DONE -> FULL
        TaskState.PARTIAL -> if (progress in 1 until FULL) progress else HALF
        else -> 0
    }

    /** [percent] to the nearest step, within 0 to 100. */
    fun rounded(percent: Float): Int = ((percent / STEP).roundToInt() * STEP).coerceIn(0, FULL)

    /**
     * The state a rounded [percent] saves as, for an item on [date]: 100 ticks it, 1–99 is a did-some, and 0
     * unticks it. An earlier day is closed, so 0 skips the item there, as unticking does.
     */
    fun stateFor(percent: Int, date: LocalDate, today: LocalDate): TaskState = when {
        percent >= FULL -> TaskState.DONE
        percent > 0 -> TaskState.PARTIAL
        date < today -> TaskState.SKIPPED
        else -> TaskState.PLANNED
    }

    /** The progress stored with [state]: done 100, a did-some [given] (50 when it's not 1–99) and anything else 0. */
    fun stored(state: TaskState, given: Int? = null): Int = when (state) {
        TaskState.DONE -> FULL
        TaskState.PARTIAL -> given?.takeIf { it in 1 until FULL } ?: HALF
        else -> 0
    }
}
