package com.example.todoapp.screen.missions

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.plan.DaySettings
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.domain.History
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.Points
import com.example.todoapp.domain.WeekLoad
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.missionKind
import com.example.todoapp.domain.pillarType
import kotlinx.datetime.LocalDate

/** One sticky-note card. [cadence] and [status] come from stored fields only. */
data class MissionCardRow(
    val missionId: Long,
    val title: String,
    val pillar: PillarType,
    val cadence: String,
    val status: String,
    val finishLine: Boolean,
    val tilt: Float,
)

/** A pillar's (or Recharge's) section: "HEALTH · 14 THIS WEEK" and its cards. */
data class PillarGroup(val pillar: PillarType, val heading: String, val cards: List<MissionCardRow>)

data class MissionsUiState(
    /** "This week: 60 points planned · 29 of 35 free hours" */
    val summary: String,
    val groups: List<PillarGroup>,
    /** Free minutes per ISO weekday, for the settings dialog. */
    val freeMinutes: Map<Int, Int>,
    val weeklyFreeMinutes: Int,
    val isEmpty: Boolean,
)

object MissionsStateBuilder {

    private val tilts = listOf(-1.6f, 1.2f, 0.8f, -1.1f, 1.5f, -0.7f)

    fun build(
        today: LocalDate,
        missions: List<Mission>,
        tasks: List<TodayTask>,
        milestones: List<Milestone>,
        settings: List<DaySettings>,
    ): MissionsUiState {
        val history = History(tasks)
        val weekStart = WeekMath.weekStart(today)
        val groups = PillarType.entries.map { pillar ->
            val heading = if (pillar.scored) {
                "${pillar.title.uppercase()} · ${Points.weeklyTarget(missions, pillar, weekStart, history)} THIS WEEK"
            } else {
                "${pillar.title.uppercase()} · NO POINTS"
            }
            val cards = missions.filter { it.pillarType == pillar }.map { mission ->
                MissionCardRow(
                    missionId = mission.missionId,
                    title = mission.title,
                    pillar = pillar,
                    cadence = MissionWords.cadence(mission),
                    status = MissionWords.status(mission, history, milestones, today),
                    finishLine = mission.missionKind == MissionKind.FINISH_LINE,
                    tilt = tilts[(mission.missionId % tilts.size).toInt()],
                )
            }
            PillarGroup(pillar, heading, cards)
        }
        val free = WeekLoad.freeMinutes(settings)
        val planned = WeekLoad.plannedMinutes(missions, weekStart, history)
        val points = WeekLoad.points(missions, weekStart, history)
        return MissionsUiState(
            summary = "This week: ${MissionWords.pointsPhrase(points)} planned · " +
                "${MissionWords.hoursNumber(planned)} of ${MissionWords.hoursNumber(free)} free hours",
            groups = groups,
            freeMinutes = (1..7).associateWith { day ->
                settings.firstOrNull { it.weekday == day }?.freeMinutes ?: DaySettings.defaultFor(day)
            },
            weeklyFreeMinutes = free,
            isEmpty = missions.isEmpty(),
        )
    }
}
