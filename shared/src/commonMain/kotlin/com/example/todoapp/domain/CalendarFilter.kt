package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import kotlinx.datetime.LocalDate

/**
 * Spec 9.4: what the All months screen counts. An item counts when its mission's pillar is picked or its
 * mission is picked. With nothing picked the filter is off and everything counts, so the months look as
 * on the Metrics calendar.
 */
data class CalendarFilter(val pillars: Set<PillarType> = emptySet(), val missionIds: Set<Long> = emptySet()) {

    /** Something is picked: day rings count items, and badges and crowns hide. */
    val isOn: Boolean get() = pillars.isNotEmpty() || missionIds.isNotEmpty()

    fun counts(mission: Mission): Boolean = !isOn || mission.pillarType in pillars || mission.missionId in missionIds

    /**
     * The line under the filters, over the counted items from [from] to [to]: "⟨Call mom⟩ and ⟨Gym⟩: done
     * 23 times, 4 in part". Done is DONE and "in part" is PARTIAL, of any mission, scored or not. One or
     * two pillars or missions sit on chips, and three or more are counted ("3 missions"); with both
     * picked, pillars come first ("⟨Health⟩ and 2 missions: …"). Null while the filter is off.
     * [allMissions] include archived ones, so a picked archived mission keeps its name.
     */
    fun summary(allMissions: List<Mission>, tasks: List<TodayTask>, from: LocalDate, to: LocalDate): List<LinePart>? {
        if (!isOn) return null
        val byId = allMissions.associateBy { it.missionId }
        // yyyy-MM-dd: text order is date order, so no task's date is parsed.
        val fromKey = MyDate.fromLocalDate(from).dateString
        val toKey = MyDate.fromLocalDate(to).dateString
        var done = 0
        var inPart = 0
        for (task in tasks) {
            val key = task.taskDate.dateString
            if (key < fromKey || key > toKey) continue
            val mission = byId[task.missionId] ?: continue
            if (!counts(mission)) continue
            when (task.taskState) {
                TaskState.DONE -> done++
                TaskState.PARTIAL -> inPart++
                else -> Unit
            }
        }
        val names = named(
            PillarType.entries.filter { it in pillars }.map { chip(it) }, "pillars"
        ) + named(
            allMissions.filter { it.missionId in missionIds }
                .sortedWith(compareBy({ it.pillarType.ordinal }, { it.title.lowercase() }))
                .map { chip(it) },
            "missions"
        )
        // Only picked missions that are no longer stored: nothing names them.
        val subject = joined(names).ifEmpty { words("Picked missions") }
        val counted = when {
            done == 0 && inPart == 0 -> "nothing done yet"
            done == 0 -> "done in part ${times(inPart)}"
            inPart == 0 -> "done ${times(done)}"
            else -> "done ${times(done)}, $inPart in part"
        }
        return subject + LinePart(": $counted")
    }

    private companion object {
        /** One or two on chips; three or more counted instead, so the line stays short. */
        fun named(chips: List<LinePart>, plural: String): List<LinePart> =
            if (chips.size <= 2) chips else listOf(LinePart("${chips.size} $plural"))

        fun times(n: Int): String = if (n == 1) "1 time" else "$n times"
    }
}
