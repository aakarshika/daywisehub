package com.example.todoapp.domain

enum class PillarType(val title: String, val description: String, val scored: Boolean) {
    HEALTH("Health", "Take care of your body. It is the only one you have!", true),
    WEALTH("Wealth", "Money isn't everything, but it is some things..", true),
    LOVE("Love", "We all need love from our family, friends, neighbours, and internet trolls..", true),
    LIFE("Life", "Meditate, Create, Dream, Build, Destroy..", true),
    RECHARGE("Recharge", "Rest that keeps everything else possible.", false);

    companion object {
        val scoredPillars: List<PillarType> = entries.filter { it.scored }
    }
}

enum class MissionKind { HABIT, FINISH_LINE, BLOCK, RHYTHM, RECHARGE }

enum class Effort { LIGHT, STEADY, DRAINING }

enum class IfMissed { MOVE, SAME_DAY, SKIP }

enum class Period(val days: Int) { DAY(1), WEEK(7), MONTH(30) }

enum class TaskState {
    PLANNED, DONE, PARTIAL, MOVED, SKIPPED;

    val countsAsDone: Boolean get() = this == DONE || this == PARTIAL
    /** Still on that day's list: true for planned, done and did-some; false once moved away or skipped. */
    val isOpen: Boolean get() = this != MOVED && this != SKIPPED
}

enum class Energy(val label: String) { LOW("Low"), NORMAL("Normal"), HIGH("High") }
