package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.reminder.ReminderSettings
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

/** One local notification: shown [at], and tapping it opens the diary on [openDate]. [id] is stable per kind and day. */
data class Reminder(val id: String, val at: LocalDateTime, val title: String, val body: String, val openDate: LocalDate)

/**
 * The notifications to have scheduled, worked out from what's stored now. Tomorrow is planned the night before:
 * the evening check asks to tick today and plan tomorrow; if tomorrow still has no "Looks good", the night one
 * lists its picks; if it still has none by morning, the morning one asks to plan the day. A planned day gets a
 * plain "your day" in the morning instead. Opted-in fixed blocks get a "starts now".
 *
 * The text is fixed when it's scheduled, so it's redone on every change (ReminderSync). Only the next
 * [DAYS_AHEAD] days get daily ones: if the app isn't opened for a while, it goes quiet instead of nagging.
 */
object Reminders {

    const val DAYS_AHEAD = 2
    const val BLOCK_DAYS = 7
    private const val LISTED = 4

    fun build(
        now: LocalDateTime,
        settings: ReminderSettings,
        missions: List<Mission>,
        tasks: List<TodayTask>,
        plans: List<DayPlan>,
    ): List<Reminder> {
        val byId = missions.associateBy { it.missionId }
        val confirmed = plans.filter { it.confirmed }.mapTo(mutableSetOf()) { it.date }
        val today = now.date
        val out = mutableListOf<Reminder>()

        fun planned(day: LocalDate) = tasks
            .filter { it.date == day && it.taskState == TaskState.PLANNED && byId.containsKey(it.missionId) }
            // Every-so-often ones first: they're the easiest to forget.
            .sortedWith(compareBy({ byId[it.missionId]?.missionKind != MissionKind.RHYTHM }, { !it.isFixedBlock }, { it.sortOrder }))
        fun isSet(day: LocalDate) = MyDate.fromLocalDate(day).dateString in confirmed
        fun at(day: LocalDate, minute: Int) = LocalDateTime(day, LocalTime(minute / 60 % 24, minute % 60))
        fun add(id: String, at: LocalDateTime, title: String, body: String, open: LocalDate) {
            if (at > now) out += Reminder(id, at, title, body, open)
        }

        for (offset in 0..DAYS_AHEAD) {
            val day = today.plus(offset, DateTimeUnit.DAY)
            val next = day.plus(1, DateTimeUnit.DAY)

            if (settings.morningOn) {
                val items = planned(day)
                if (items.isNotEmpty()) {
                    val newWeek = settings.weekOn && day.dayOfWeek == DayOfWeek.MONDAY
                    if (isSet(day)) {
                        add(
                            "morning-$day", at(day, settings.morningMinute),
                            (if (newWeek) "New week · " else "") + "Your day: ${things(items.size)}",
                            listed(items, byId), day,
                        )
                    } else {
                        add(
                            "morning-$day", at(day, settings.morningMinute),
                            (if (newWeek) "New week · " else "") + "Plan today",
                            "${items.size} picked: ${inline(items, byId)}. Swap what doesn't fit, then Looks good.", day,
                        )
                    }
                }
            }

            if (settings.eveningOn) {
                val open = planned(day).count { !it.isFixedBlock }
                val tomorrowSet = isSet(next)
                if (open > 0 || !tomorrowSet) {
                    val title = if (settings.weekOn && day.dayOfWeek == DayOfWeek.SUNDAY) "Week's done · plan Monday" else "Evening check"
                    val body = when {
                        open > 0 && !tomorrowSet -> "${things(open)} still open. Tick what you did, then plan tomorrow."
                        open > 0 -> "${things(open)} still open. Tick what you did."
                        else -> "Today's done. Now plan tomorrow: ${things(planned(next).size)} picked so far."
                    }
                    add("evening-$day", at(day, settings.eveningMinute), title, body, day)
                }
            }

            if (settings.nightOn && !isSet(next)) {
                val items = planned(next)
                val body = if (items.isEmpty()) "Nothing picked yet. Tap to plan it."
                else listed(items, byId) + "\nTap to swap or say Looks good."
                add("night-$day", at(day, settings.nightMinute), "Tomorrow isn't planned yet", body, next)
            }
        }

        val blockIds = settings.blockIds
        if (blockIds.isNotEmpty()) {
            val last = today.plus(BLOCK_DAYS, DateTimeUnit.DAY)
            tasks.filter { it.isFixedBlock && it.missionId in blockIds && it.taskState == TaskState.PLANNED && it.date <= last }
                .forEach { task ->
                    val mission = byId[task.missionId] ?: return@forEach
                    val start = MissionWords.parseClock(mission.startTime) ?: return@forEach
                    val until = mission.endTime?.let { "Until $it." } ?: ""
                    add("block-${task.date}-${mission.missionId}", at(task.date, start), "${mission.title} starts now", until, task.date)
                }
        }
        return out.sortedBy { it.at }
    }

    private fun things(n: Int) = if (n == 1) "1 thing" else "$n things"

    private fun title(task: TodayTask, byId: Map<Long, Mission>) = byId[task.missionId]?.title.orEmpty()

    private fun inline(items: List<TodayTask>, byId: Map<Long, Mission>): String {
        val names = items.take(3).joinToString(", ") { title(it, byId) }
        return if (items.size > 3) "$names +${items.size - 3} more" else names
    }

    private fun listed(items: List<TodayTask>, byId: Map<Long, Mission>): String {
        val lines = items.take(LISTED).map { "• " + title(it, byId) }
        return (if (items.size > LISTED) lines + "+${items.size - LISTED} more" else lines).joinToString("\n")
    }
}
