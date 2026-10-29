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
     * The line under the months, over the counted items from [from] to [today]: "⟨Call mom⟩ and ⟨Gym⟩: done
     * 23 times, 4 in part" ("done once", "done twice"). Done is DONE and "in part" is PARTIAL, of any mission, scored or not. A
     * finished week that isn't in [opened] (PaceOffers.openedWeeks) is away and left out, as it has no rings.
     *
     * When every picked pillar and mission fits on 2 chips, each is named ("⟨Health⟩ and ⟨Gym⟩: …").
     * Otherwise 1 or 2 pillars are named and 3 or more counted ("3 pillars"), and missions are counted
     * whenever a pillar is picked too ("⟨Health⟩ and 2 missions: …"); picked alone, 3 or more are counted
     * ("3 missions"). Pillars come first. Null while the filter is off. [allMissions] include archived
     * ones, so a picked archived mission keeps its name.
     */
    fun summary(
        allMissions: List<Mission>,
        tasks: List<TodayTask>,
        from: LocalDate,
        today: LocalDate,
        opened: Set<LocalDate>,
    ): List<LinePart>? {
        if (!isOn) return null
        val byId = allMissions.associateBy { it.missionId }
        val thisWeek = WeekMath.weekStart(today)
        // yyyy-MM-dd: text order is date order, so only the counted items' dates are parsed.
        val fromKey = MyDate.fromLocalDate(from).dateString
        val toKey = MyDate.fromLocalDate(today).dateString
        var done = 0
        var inPart = 0
        for (task in tasks) {
            val key = task.taskDate.dateString
            if (key < fromKey || key > toKey) continue
            val state = task.taskState
            if (state != TaskState.DONE && state != TaskState.PARTIAL) continue
            val mission = byId[task.missionId] ?: continue
            if (!counts(mission)) continue
            val monday = WeekMath.weekStart(task.date)
            if (monday < thisWeek && WeekScores.isAway(monday, opened)) continue
            if (state == TaskState.DONE) done++ else inPart++
        }
        val pillarChips = PillarType.entries.filter { it in pillars }.map { chip(it) }
        val missionChips = allMissions.filter { it.missionId in missionIds }
            .sortedWith(compareBy({ it.pillarType.ordinal }, { it.title.lowercase() }))
            .map { chip(it) }
        val names = if (pillarChips.size + missionChips.size <= 2) pillarChips + missionChips else {
            // Past 2 chips, missions are counted: a pillar is picked too, or there are 3 or more of them.
            val missionNames = when (missionChips.size) {
                0 -> emptyList()
                1 -> words("1 mission")
                else -> words("${missionChips.size} missions")
            }
            (if (pillarChips.size <= 2) pillarChips else words("${pillarChips.size} pillars")) + missionNames
        }
        val counted = when {
            done == 0 && inPart == 0 -> "nothing done yet"
            done == 0 -> "done in part ${MissionWords.times(inPart)}"
            inPart == 0 -> "done ${MissionWords.times(done)}"
            else -> "done ${MissionWords.times(done)}, $inPart in part"
        }
        return joined(names) + LinePart(": $counted")
    }
}
