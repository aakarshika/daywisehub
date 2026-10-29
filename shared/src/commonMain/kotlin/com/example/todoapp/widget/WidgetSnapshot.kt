package com.example.todoapp.widget

import com.example.todoapp.domain.Badge
import com.example.todoapp.domain.PillarType

/**
 * Everything the home and lock screen widgets show, as of one moment. Plain values only (dates are
 * yyyy-MM-dd strings), so Swift and the Android app read it without the date library.
 */
data class WidgetSnapshot(
    val dateKey: String,
    /** "Tuesday, 28 Oct" */
    val dateTitle: String,
    /** "Tue 28" */
    val shortDate: String,
    /** "9:40 PM" */
    val timeLabel: String,
    val mode: TodayMode,
    val dayType: DayType,
    val pillars: List<WidgetPillar>,
    /** "Life needs you" or "In balance". */
    val verdict: String,
    /** The pillar the verdict names; null when in balance. */
    val verdictPillar: PillarType?,
    /** The see-saw's tilt in degrees; positive dips the right (Love and Life). */
    val tilt: Float,
    /** Today's items still on the list (done, did-some and planned), in the diary's order. */
    val tasks: List<WidgetTask>,
    val doneCount: Int,
    val totalCount: Int,
    val water: Int,
    val waterGoal: Int,
    /** The faces the widgets offer, in the order they show. */
    val moods: List<WidgetMood>,
    val moodSet: Boolean,
    /** Today and the six days after it. */
    val weekAhead: List<WidgetDay>,
    /** "Thursday is light", or null when no day ahead is lighter. */
    val lightLine: String?,
    /** "Move “Read 20 pages” there?" when the planner has a mission that didn't fit today and room that day. */
    val suggestion: String?,
    /** The day [lightLine] names, which a tap opens. */
    val lightDateKey: String?,
    /** This week, Monday to Sunday. */
    val weekStickers: List<WidgetSticker>,
    /** "3-day run · keep it up" */
    val streakLine: String,
    val month: WidgetMonth,
) {
    /** Items still to do, for Next up. */
    val openTasks: List<WidgetTask> get() = tasks.filter { !it.done }
}

/** The Today widget's face: it switches by itself (docs/widgets/mockups.html, widget 1). */
enum class TodayMode { BALANCE, ALL_DONE, REST, EVENING, EMPTY }

/** How full a day is: plenty planned, a little, or nothing scored. */
enum class DayType(val label: String) { BIG("Big"), SLOW("Slow"), REST("Rest") }

data class WidgetPillar(
    val pillar: PillarType,
    val earned: Int,
    val target: Int,
    /** 0 to 1; a pillar with nothing due reads full. */
    val share: Float,
    /** The friend's size: 0 when nothing is due this week, as on the diary's strip. */
    val grown: Float,
    val extra: Boolean,
)

data class WidgetTask(
    val taskId: Long,
    val title: String,
    val pillar: PillarType,
    /** Done or did-some. */
    val done: Boolean,
    /** 0 to 100. */
    val progress: Int,
    val partial: Boolean,
    /** "10:30" for a fixed block, else "20 min". */
    val timeLabel: String,
)

/** A mood face, by the diary's icon name ("happy"). */
data class WidgetMood(val icon: String, val badge: Badge, val active: Boolean)

data class WidgetDay(
    val dateKey: String,
    /** "M" */
    val letter: String,
    val dayType: DayType,
    /** Every item that day done; only today or earlier. */
    val done: Boolean,
    val isToday: Boolean,
    /** The day [WidgetSnapshot.lightLine] names. */
    val light: Boolean,
)

/** A day of the week's sticker row: its rarest badge, or null; [future] days show an empty slot. */
data class WidgetSticker(val dateKey: String, val badge: Badge?, val future: Boolean)

data class WidgetMonth(
    /** "October" */
    val title: String,
    /** Balanced weeks this month. */
    val crowns: Int,
    val weeks: List<WidgetMonthWeek>,
)

data class WidgetMonthWeek(val crowned: Boolean, val days: List<WidgetMonthDay>)

data class WidgetMonthDay(
    val dateKey: String,
    val day: Int,
    val inMonth: Boolean,
    val isToday: Boolean,
    val badge: Badge?,
)
