# Plan 3: Creating Missions and the Missions Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the owner create, edit and archive missions through the nine-question sentence with a before-saving week cost, see them on a Missions screen grouped by pillar, set free time per weekday, and start from an empty install instead of the sample seed.

**Architecture:** Pure-Kotlin rules in `domain` turn a `MissionDraft` into the sentence (`MissionSentence`), into a `Mission` row, and into the week's cost (`WeekCosts`, built on the existing `Points.occurrencesInWeek` / `weeklyTarget` and `DaySettings`). `MissionEditorViewModel` holds the draft, which questions are answered, and the step on screen; its screen is drawn inside the Missions tab. Writes go through repository helpers that hold a Mutex, can't be cancelled and run on `Dispatchers.Default`: mission + milestones in `MissionRepository`, free time, archive and "re-pick today" in `PlanRepository`. `MissionsViewModel` builds the pillar sections and sticky-note cards from stored data only.

**Tech Stack:** Kotlin Multiplatform (Kotlin 2.0.20, language level 1.9), Compose Multiplatform 1.7.1 (material3, foundation), Room KMP 2.7.0-alpha01 with KSP, Koin 3.2, precompose 1.5.7 (navigation and `BackHandler`), kotlinx-datetime.

**Spec:** `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`, sections 3, 4 (not the ideas library), 9, 9.1, 10, 11 and 14.

**Builds on:** Plan 1 (`docs/superpowers/plans/2026-10-25-plan-1-foundation-and-daily-list.md`), on branch `redesign/plan-3-missions`.

**Ground rules for this plan**
- **No automated tests.** The owner tests by hand. Every task ends with a green build and a commit.
- **Build command.** `./gradlew` doesn't work here (the wrapper jar is missing). From the repo root run
  `/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60`
  and expect `BUILD SUCCESSFUL`. Task 9 also runs `:shared:compileKotlinIosSimulatorArm64`.
- **Paths** are relative to `shared/src/commonMain/kotlin/com/example/todoapp/` unless they start with `shared/` or `docs/`.
- **Commits** stage files by path, never `git add -A`. If a build rewrites anything under `shared/schemas` (it has rewritten `4.json` before), restore it before committing.
- **No schema change.** Room stays at version 5: this plan adds DAO queries only, which don't touch the exported schema, so `5.json` doesn't change and no migration is needed. Plan 2's `@AutoMigration(from = 5, to = 6)` is unaffected by the order the two plans land in.
- **Every task builds on its own, in order.** Each task was checked by applying Tasks 1..N to a clean checkout of `f0a6dbf` and building.
- **Find/replace edits** quote the current text exactly; each "Replace" block appears once in its file.

## Decisions this plan makes

The spec leaves these open. Each is easy to change later.

1. **Recharge and step 2.** The kind is asked at step 3, after the pillar. Picking Recharge sets the pillar to Recharge and drops the pillar, weight and "sideways" clauses from the sentence. The step-2 answer is kept, so switching the kind back restores it. When editing a recharge mission, steps 2, 7 and 8 aren't asked.
2. **Questions a kind doesn't need.** A fixed block skips step 5 (its length is end minus start) and step 8 (always "it has to be that day"). Recharge skips step 8 (always "skip it").
3. **Gentle start** is offered for habits and finish lines counted per week, at 2 or more a week. The starting pace is half the goal, rounded up (3 → 2, 5 → 3, 7 → 4), which matches 5 of the 6 gentle ideas in `ideas.json`. A daily habit can't start gently, because `count`/`goalCount` can't express "fewer than every day". Stepping up is Plan 2's pace offer. Editing keeps a gentle start's current pace until the goal changes.
4. **Finish lines.** The pace step asks how often (spec section 3: N times per week), the end date in whole weeks (1 to 52 weeks out, 8 by default, the library's default), and up to 5 optional milestones. On save, milestone due dates are spread evenly from the mission's start date to its end date. The last one falls on the end date.
5. **Preferred days.** Weekly habits and finish lines get optional "days that suit it best". The planner already uses them.
6. **Fixed-block times** are set in half hours and stored as 24-hour `H:mm` (`"9:00"`, `"13:00"`). The diary shows them as stored ("9:00 to 13:00"). An old sample end time that is earlier than its start (`"9:00"` to `"1:00"`) is read as afternoon when edited.
7. **Every so often** runs 2–30 days, 1–12 weeks or 1–3 months. That keeps every interval inside the planner's 120-day history (`PlanRepository.HISTORY_DAYS`).
8. **Today re-picks after a change.** Saving or archiving a mission re-picks today's open items, as "change" on the diary does, unless the day was confirmed with "Looks good". Without this, missions made on a fresh install wouldn't show until tomorrow. On a confirmed day, use the pencil.
9. **Archive** also deletes the mission's still-planned items from today on. Points it already earned this week stay in the pillar bars (changed after review). Its rows leave the diary's lists, because those read active missions only. There's no restore yet.
10. **Icons.** "Every so often" uses `star_border`, not the mockup's wand, because spec 9.1 keeps the wand for "the app adjusted something". The gentle-start sticky keeps the wand.
11. **Titles.** The keyboard capitalizes the first word, so cards and the diary read "Learn guitar". The sentence shows the title as typed ("I want to Learn guitar").
12. **Where the editor lives.** It's drawn inside the Missions tab, not as a new route, so the tab bar stays and the Android back button walks the questions. Switching tabs mid-flow drops an unsaved draft. Editing opens on the before-saving screen, and tapping an answer asks that question again.

## File map

**Created**

| File | Responsibility |
|---|---|
| `domain/MissionWords.kt` | All wording: days, weights, pace phrases, hours, card cadence and status lines (stored data only) |
| `domain/MissionDraft.kt` | `EditorStep`, `MissionDraft` (answers → `Mission`), `MissionDrafts` (load, limits, gentle start, milestone dates) |
| `domain/MissionSentence.kt` | The sentence as tappable parts |
| `domain/WeekCost.kt` | `WeekLoad` (week totals) and `WeekCosts` (before/after per pillar, free hours, plain-words line) |
| `screen/missions/editor/MissionEditorViewModel.kt` | Draft, answered steps, navigation, save and archive |
| `screen/basicutils/components/MarkerHeading.kt` | `block_b` marker heading that wraps long questions |
| `screen/missions/editor/EditorParts.kt` | Ruled sentence with highlights, header, progress, answer cards, day toggles, steppers, footer |
| `screen/missions/editor/WeekCostView.kt` | Pillar bars with striped additions, free hours bar |
| `screen/missions/editor/SummaryStep.kt` | Before-saving screen and gentle-start sticky |
| `screen/missions/editor/StepContent.kt` | Questions 1–3 and 5–9 |
| `screen/missions/editor/PaceStep.kt` | Question 4 for each kind |
| `screen/missions/editor/MissionEditorScreen.kt` | The editor screen, Back, leave and archive dialogs |
| `screen/missions/MissionsState.kt` | Pillar sections and card rows |
| `screen/missions/MissionsViewModel.kt` | Missions state, which mission the editor is open on, free-time save |
| `screen/missions/FreeTimeDialog.kt` | Free time per weekday |
| `screen/missions/MissionsScreen.kt` | The Missions tab |

**Rewritten:** `repo/MissionRepository.kt`.

**Edited:** `db/data/mission/milestone/MilestoneDao.kt`, `db/data/plan/PlanDao.kt`, `db/data/todotask/TodayTaskDao.kt`, `repo/TodayTaskRepository.kt`, `repo/MilestoneRepository.kt`, `repo/PlanRepository.kt`, `di/DI.kt`, `screen/basicutils/AppColors.kt`, `screen/diary/plan/PlanNotes.kt`, `screen/NavScreens.kt`, `db/SampleData.kt`, `screen/diary/DiaryState.kt`, `screen/diary/plan/PlanSection.kt`, `screen/diary/plan/PlanDialogs.kt`, `screen/diary/DiaryScreen.kt`.

**Unchanged on purpose:** `domain/Points.kt`, `domain/Planner.kt`, `db/Database.kt` (still version 5), `screen/placeholder/ComingNextScreen.kt` (Metrics still uses it).

---

### Task 1: Mission words and drafts

**Files:**
- Create: `domain/MissionWords.kt`
- Create: `domain/MissionDraft.kt`

**Depends on:** Plan 1's `domain/*` (`History`, `Points`, `WeekMath`, `MissionRules`, `DateLabels`, `Types`), `db/data/mission/Mission.kt`, `db/data/mission/milestone/Milestone.kt`, `db/models/TaskStatus.kt`.

These are pure Kotlin. `MissionWords` holds every phrase the editor and the Missions screen show. The card lines read only stored fields (kind, pace, weight, days, times, start, end and pause dates, history and milestones), never the title (spec 7.1). `MissionDraft` holds the nine answers. It knows which questions apply to which kind, and how it saves as a `Mission`. Stored enum names are read with `enumOr`, never with a bare `valueOf`.

- [ ] **Step 1: Create `domain/MissionWords.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.models.TaskStatus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/**
 * Words for the mission sentence, the Missions screen and the week cost.
 * Everything here is built from stored fields only (spec 7.1): a title is never read for meaning.
 */
object MissionWords {

    /** Step 5's "a full day". */
    const val FULL_DAY_MINUTES = 480

    private val shortDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    /** ISO day number (Monday = 1) → "Mon". */
    fun shortDay(isoDay: Int): String = shortDays.getOrElse(isoDay - 1) { "" }

    /** "Mon to Fri", "Sat and Sun", "Mon, Wed and Fri", "Tue", "Every day". */
    fun daysLabel(days: Set<Int>): String {
        val sorted = days.filter { it in 1..7 }.sorted()
        return when {
            sorted.isEmpty() -> ""
            sorted.size == 7 -> "Every day"
            sorted.size == 1 -> shortDay(sorted.first())
            sorted.size >= 3 && sorted.last() - sorted.first() == sorted.size - 1 ->
                "${shortDay(sorted.first())} to ${shortDay(sorted.last())}"
            else -> sorted.dropLast(1).joinToString(", ") { shortDay(it) } + " and " + shortDay(sorted.last())
        }
    }

    fun weightWord(weight: Int): String = when {
        weight <= 1 -> "small"
        weight == 2 -> "solid"
        else -> "big"
    }

    fun effortWord(effort: Effort): String = when (effort) {
        Effort.LIGHT -> "light"
        Effort.STEADY -> "steady"
        Effort.DRAINING -> "draining"
    }

    fun ifMissedWords(rule: IfMissed): String = when (rule) {
        IfMissed.MOVE -> "move it"
        IfMissed.SAME_DAY -> "it has to be that day"
        IfMissed.SKIP -> "skip it"
    }

    /** The highlighted answer to step 3. */
    fun kindPhrase(kind: MissionKind): String = when (kind) {
        MissionKind.HABIT -> "something I keep doing"
        MissionKind.FINISH_LINE -> "something with a finish line"
        MissionKind.BLOCK -> "a fixed block in my day"
        MissionKind.RHYTHM -> "something I do every so often"
        MissionKind.RECHARGE -> "something that recharges me"
    }

    /** "30 min", "1½ h", "a full day". */
    fun durationWords(minutes: Int): String =
        if (minutes >= FULL_DAY_MINUTES) "a full day" else minutesLabel(minutes)

    /** "once", "twice", "3 times". */
    fun times(count: Int): String = when (count) {
        1 -> "once"
        2 -> "twice"
        else -> "$count times"
    }

    /** "about 3 times a week", "every day", "about twice a month". */
    fun countPhrase(count: Int, period: Period): String = when (period) {
        Period.DAY -> "every day"
        Period.WEEK -> "about ${times(count)} a week"
        Period.MONTH -> "about ${times(count)} a month"
    }

    /** "every week", "every 2 weeks", "every 3 months", "every 4 days". */
    fun intervalPhrase(count: Int, unit: Period): String {
        val name = unit.name.lowercase()
        return if (count <= 1) "every $name" else "every $count ${name}s"
    }

    /** "1 point", "6 points". */
    fun pointsPhrase(points: Int): String = if (points == 1) "1 point" else "$points points"

    /** 45 → "45 min", 60 → "1 hour", 90 → "1½ hours", 135 → "2 h 15 min". */
    fun hoursPhrase(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes == 60 -> "1 hour"
        minutes % 60 == 0 -> "${minutes / 60} hours"
        minutes % 60 == 30 -> "${minutes / 60}½ hours"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }

    /** Whole hours with a half: 1770 → "29½", 15 → "0", 30 → "½". */
    fun hoursNumber(minutes: Int): String {
        val whole = minutes.coerceAtLeast(0) / 60
        val half = minutes.coerceAtLeast(0) % 60 >= 30
        return when {
            whole == 0 && half -> "½"
            half -> "$whole½"
            else -> "$whole"
        }
    }

    /** The line under the free-hours bar. */
    fun spareLine(freeMinutes: Int, plannedMinutes: Int): String {
        val spare = freeMinutes - plannedMinutes
        return when {
            spare >= 5 * 60 -> "${hoursPhrase(spare)} to spare. Plenty of room."
            spare > 0 -> "${hoursPhrase(spare)} to spare."
            spare == 0 -> "That fills your free time exactly."
            else -> "${hoursPhrase(-spare)} more than your free time. Some of it will wait for days with room."
        }
    }

    /** Minutes after midnight → "9:00", "13:30". */
    fun clockLabel(minutes: Int): String = "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"

    /** "9:00", "09:00" or "13:30" → minutes after midnight; null when unreadable. */
    fun parseClock(text: String?): Int? {
        val parts = text?.trim()?.split(":") ?: return null
        if (parts.size != 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].take(2).toIntOrNull() ?: return null
        if (hours !in 0..24 || minutes !in 0..59) return null
        return hours * 60 + minutes
    }

    /** The pace line on a mission card: "3× a week, on Mon · big", "Every 2 weeks · solid". */
    fun cadence(mission: Mission): String {
        val weight = weightWord(mission.weight)
        return when (mission.missionKind) {
            MissionKind.BLOCK -> {
                val days = daysLabel(mission.daySet)
                val start = mission.startTime.orEmpty()
                val end = mission.endTime.orEmpty()
                val window = if (start.isBlank() || end.isBlank()) "" else ", $start to $end"
                "$days$window · $weight"
            }
            MissionKind.RECHARGE -> "About ${times(mission.count)} a week"
            MissionKind.RHYTHM ->
                intervalPhrase(mission.intervalCount, mission.intervalPeriod).replaceFirstChar { it.uppercase() } + " · $weight"
            else -> {
                val pace = when (mission.periodUnit) {
                    Period.DAY -> "Every day"
                    Period.WEEK -> if (mission.count == 1) "Once a week" else "${mission.count}× a week"
                    Period.MONTH -> if (mission.count == 1) "Once a month" else "${mission.count}× a month"
                }
                val goal = mission.goalCount?.takeIf { it > mission.count }?.let { "up to ${it}×" }
                val days = mission.daySet.takeIf { it.isNotEmpty() && mission.periodUnit == Period.WEEK }
                    ?.let { "on ${daysLabel(it)}" }
                listOfNotNull(pace, goal, days).joinToString(", ") + " · $weight"
            }
        }
    }

    /** The status line on a mission card, from stored data only. */
    fun status(mission: Mission, history: History, milestones: List<Milestone>, today: LocalDate): String {
        mission.pausedUntil?.toLocalDate()?.let { if (it > today) return "Paused until ${it.shortLabel()}" }
        val start = mission.startDate.toLocalDate()
        if (start > today) return "Starts ${start.shortLabel()}"
        mission.endDate?.toLocalDate()?.let { if (it < today) return "Ended ${it.shortLabel()}" }
        val kind = mission.missionKind
        if (kind == MissionKind.BLOCK) return "Fixed block"
        if (kind == MissionKind.RECHARGE) return "Never counts as missed"
        if (mission.repeatsByInterval) {
            val last = history.lastDoneBefore(mission.missionId, today.plus(1, DateTimeUnit.DAY))
            val due = last?.plus(mission.intervalDays(), DateTimeUnit.DAY) ?: start
            val days = today.daysUntil(due)
            return when {
                days < -1 -> "Due ${-days} days ago"
                days == -1 -> "Due yesterday"
                days == 0 -> "Due today"
                due <= WeekMath.weekEnd(today) -> "Due by ${due.weekdayName()}"
                else -> "Next around ${due.shortLabel()}"
            }
        }
        if (mission.periodUnit == Period.DAY) {
            val doneToday = history.doneCount(mission.missionId, today, today) > 0
            val streak = history.streakBefore(mission.missionId, today) + if (doneToday) 1 else 0
            return if (streak > 0) "$streak-day streak" else "No streak yet"
        }
        if (kind == MissionKind.FINISH_LINE) {
            val mine = milestones.filter { it.missionId == mission.missionId }.sortedBy { it.milestoneOrder }
            if (mine.isNotEmpty()) {
                val next = mine.indexOfFirst { it.status != TaskStatus.COMPLETED.value }
                return if (next < 0) "All ${mine.size} milestones done" else "Milestone ${next + 1} of ${mine.size}"
            }
        }
        val weekStart = WeekMath.weekStart(today)
        val target = Points.occurrencesInWeek(mission, weekStart, history)
        val done = history.doneCount(mission.missionId, weekStart, WeekMath.weekEnd(today))
        return if (target > 0 && done >= target) "Done for this week" else "$done of $target this week"
    }
}
```


- [ ] **Step 2: Create `domain/MissionDraft.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.models.MyDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/** The nine questions of spec section 4.1, in order. */
enum class EditorStep { TITLE, PILLAR, KIND, PACE, DURATION, EFFORT, WEIGHT, IF_MISSED, GENTLE }

/** One milestone line of a finish line. [milestoneId] is 0 until it's saved. */
data class MilestoneDraft(val milestoneId: Long = 0, val text: String = "")

/**
 * A mission being written or edited. [count] is the full pace; a gentle start saves a lower
 * count and keeps [count] as `goalCount`. Answers a kind doesn't use are ignored when saving.
 */
data class MissionDraft(
    val missionId: Long = 0,
    val title: String = "",
    /** Step 2. Kept while the kind is Recharge, so switching back restores it. */
    val pillarChoice: PillarType? = null,
    val kind: MissionKind? = null,
    val count: Int = 3,
    val period: Period = Period.WEEK,
    val gentle: Boolean = false,
    /** The current pace of a gentle start being edited; dropped when the goal changes. */
    val gentleCount: Int? = null,
    val intervalCount: Int = 2,
    val intervalUnit: Period = Period.WEEK,
    /** Fixed days for a block, optional preferred days for a weekly habit or finish line. */
    val weekDays: Set<Int> = emptySet(),
    val startMinute: Int = 9 * 60,
    val endMinute: Int = 12 * 60,
    val endDate: LocalDate? = null,
    val milestones: List<MilestoneDraft> = emptyList(),
    val durationMinutes: Int = 30,
    val effort: Effort = Effort.STEADY,
    val weight: Int = 2,
    val ifMissed: IfMissed = IfMissed.MOVE,
    val startDate: LocalDate,
    val pausedUntil: MyDate? = null,
    val motivation: String? = null,
) {
    val pillar: PillarType
        get() = if (kind == MissionKind.RECHARGE) PillarType.RECHARGE else pillarChoice ?: PillarType.LIFE

    /** Step 9 is asked only for habits and finish lines with 2 or more sessions a week. */
    val gentleEligible: Boolean
        get() = (kind == MissionKind.HABIT || kind == MissionKind.FINISH_LINE) && period == Period.WEEK && count >= 2

    val gentleOn: Boolean get() = gentle && gentleEligible

    /** The pace the planner uses now: lower while starting gently. */
    val currentCount: Int
        get() = if (gentleOn) gentleCount?.takeIf { it in 1 until count } ?: MissionDrafts.gentleStart(count) else count

    /** Weekly habits and finish lines may have preferred days; blocks must have days. */
    val usesDays: Boolean
        get() = kind == MissionKind.BLOCK ||
            ((kind == MissionKind.HABIT || kind == MissionKind.FINISH_LINE) && period == Period.WEEK)

    /** Recharge skips the pillar, weight and sideways questions; a block takes its length from its times. */
    fun applies(step: EditorStep): Boolean = when (step) {
        EditorStep.PILLAR, EditorStep.WEIGHT -> kind != MissionKind.RECHARGE
        EditorStep.DURATION -> kind != MissionKind.BLOCK
        EditorStep.IF_MISSED -> kind != MissionKind.BLOCK && kind != MissionKind.RECHARGE
        EditorStep.GENTLE -> kind == null || gentleEligible
        else -> true
    }

    val steps: List<EditorStep> get() = EditorStep.entries.filter { applies(it) }

    /** Fresh pace answers for a newly picked kind. Picking the same kind changes nothing. */
    fun withKind(newKind: MissionKind, today: LocalDate): MissionDraft {
        if (newKind == kind) return this
        val base = copy(kind = newKind, gentle = false, gentleCount = null, weekDays = emptySet())
        return when (newKind) {
            MissionKind.HABIT -> base.copy(count = 3, period = Period.WEEK)
            MissionKind.FINISH_LINE -> base.copy(
                count = 2,
                period = Period.WEEK,
                endDate = endDate ?: maxOf(today, startDate).plus(MissionDrafts.DEFAULT_FINISH_WEEKS * 7, DateTimeUnit.DAY),
            )
            MissionKind.BLOCK -> base.copy(weekDays = setOf(1, 2, 3, 4, 5), startMinute = 9 * 60, endMinute = 12 * 60)
            MissionKind.RHYTHM -> base.copy(intervalCount = 2, intervalUnit = Period.WEEK)
            MissionKind.RECHARGE -> base.copy(count = 2, period = Period.WEEK)
        }
    }

    /** The mission this draft saves as. */
    fun toMission(): Mission {
        val k = kind ?: MissionKind.HABIT
        val counted = k == MissionKind.HABIT || k == MissionKind.FINISH_LINE || k == MissionKind.RECHARGE
        val periodic = k == MissionKind.HABIT || k == MissionKind.FINISH_LINE
        return Mission(
            missionId = missionId,
            title = title.trim(),
            pillar = pillar.name,
            kind = k.name,
            weight = if (k == MissionKind.RECHARGE) 0 else weight,
            durationMinutes = if (k == MissionKind.BLOCK) endMinute - startMinute else durationMinutes,
            effort = effort.name,
            ifMissed = when (k) {
                MissionKind.BLOCK -> IfMissed.SAME_DAY
                MissionKind.RECHARGE -> IfMissed.SKIP
                else -> ifMissed
            }.name,
            count = if (counted) currentCount else 1,
            period = (if (periodic) period else Period.WEEK).name,
            goalCount = if (gentleOn) count else null,
            intervalCount = if (k == MissionKind.RHYTHM) intervalCount else 0,
            intervalUnit = (if (k == MissionKind.RHYTHM) intervalUnit else Period.WEEK).name,
            weekDays = if (usesDays) weekDays.sorted().joinToString(",") else "",
            startTime = if (k == MissionKind.BLOCK) MissionWords.clockLabel(startMinute) else null,
            endTime = if (k == MissionKind.BLOCK) MissionWords.clockLabel(endMinute) else null,
            startDate = MyDate.fromLocalDate(startDate),
            endDate = if (k == MissionKind.FINISH_LINE) endDate?.let { MyDate.fromLocalDate(it) } else null,
            pausedUntil = pausedUntil,
            motivation = motivation,
        )
    }
}

object MissionDrafts {

    const val DEFAULT_FINISH_WEEKS = 8
    const val MAX_FINISH_WEEKS = 52
    const val MAX_MILESTONES = 5
    const val MAX_TITLE_LENGTH = 60
    const val MAX_MILESTONE_LENGTH = 80
    /** Blocks move in half hours. */
    const val CLOCK_STEP = 30
    const val DAY_MINUTES = 24 * 60

    /** Step 5's choices; the last is "a full day". */
    val durationChoices = listOf(10, 15, 20, 30, 45, 60, 90, 120, 180, 240, MissionWords.FULL_DAY_MINUTES)

    /**
     * "Every so often" stops at 3 months: the planner reads 120 days of history
     * (PlanRepository.HISTORY_DAYS), so a longer rhythm would look never done.
     */
    fun intervalRange(unit: Period): IntRange = when (unit) {
        Period.DAY -> 2..30
        Period.WEEK -> 1..12
        Period.MONTH -> 1..3
    }

    /** About half the goal, rounded up: 3 → 2, 5 → 3, 7 → 4. */
    fun gentleStart(goal: Int): Int = ((goal + 1) / 2).coerceAtLeast(1)

    fun new(today: LocalDate): MissionDraft = MissionDraft(startDate = today)

    /** A stored mission as a draft. Stored names that don't match an answer fall back to a default. */
    fun from(mission: Mission, milestones: List<Milestone>): MissionDraft {
        val goal = mission.goalCount?.takeIf { it > mission.count }
        val unit = enumOr(mission.intervalUnit, Period.WEEK)
        val start = (MissionWords.parseClock(mission.startTime) ?: (9 * 60)).coerceIn(0, DAY_MINUTES - CLOCK_STEP)
        var end = MissionWords.parseClock(mission.endTime) ?: (start + mission.durationMinutes)
        // Early sample blocks wrote afternoons without a 12-hour offset ("9:00 to 1:00").
        if (end <= start && end + 12 * 60 > start) end += 12 * 60
        return MissionDraft(
            missionId = mission.missionId,
            title = mission.title,
            pillarChoice = enumOr<PillarType>(mission.pillar, PillarType.LIFE).takeIf { it.scored },
            kind = enumOr(mission.kind, MissionKind.HABIT),
            count = goal ?: mission.count.coerceAtLeast(1),
            period = enumOr(mission.period, Period.WEEK),
            gentle = goal != null,
            gentleCount = if (goal != null) mission.count else null,
            intervalCount = mission.intervalCount.coerceIn(intervalRange(unit)),
            intervalUnit = unit,
            weekDays = mission.weekDays.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.toSet(),
            startMinute = start,
            endMinute = end.coerceIn(start + CLOCK_STEP, DAY_MINUTES),
            endDate = mission.endDate?.toLocalDate(),
            milestones = milestones.sortedBy { it.milestoneOrder }.map { MilestoneDraft(it.milestoneId, it.text) },
            durationMinutes = mission.durationMinutes.coerceAtLeast(5),
            effort = enumOr(mission.effort, Effort.STEADY),
            weight = mission.weight.coerceIn(1, 3),
            ifMissed = enumOr(mission.ifMissed, IfMissed.MOVE),
            startDate = mission.startDate.toLocalDate(),
            pausedUntil = mission.pausedUntil,
            motivation = mission.motivation,
        )
    }

    /** Due dates for [count] milestones, spread evenly from [start] to [end]; the last one is the end date. */
    fun milestoneDates(start: LocalDate, end: LocalDate?, count: Int): List<LocalDate> {
        val last = end ?: start.plus(DEFAULT_FINISH_WEEKS * 7, DateTimeUnit.DAY)
        val days = start.daysUntil(last).coerceAtLeast(0)
        return (1..count).map { i -> start.plus(days * i / count, DateTimeUnit.DAY) }
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
```


- [ ] **Step 3: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

This plan changes no schema, so anything a build changed under `shared/schemas` is noise. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (usually 4.json)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/domain/MissionWords.kt \
  $K/domain/MissionDraft.kt
git commit -m "feat: mission words and drafts for the sentence flow"
```


---

### Task 2: The sentence and the week cost

**Files:**
- Create: `domain/MissionSentence.kt`
- Create: `domain/WeekCost.kt`

**Depends on:** Task 1.

The sentence is a list of parts. An answer part carries its `EditorStep`, so the screen can highlight it and jump back to it. The blank being asked is a `placeholder` part. The week cost compares the stored active missions ("before") with the same list where the draft is added, or replaces its old version ("after"). Both go through `Points.weeklyTarget` per pillar. Free time sums `occurrencesInWeek × durationMinutes` over everything but fixed blocks, against the seven `DaySettings`.

- [ ] **Step 1: Create `domain/MissionSentence.kt`**

```kotlin
package com.example.todoapp.domain

/** A piece of the mission sentence. An answer carries its [step]; [placeholder] is the blank being asked now. */
data class SentencePart(val text: String, val step: EditorStep? = null, val placeholder: Boolean = false)

/** Spec 4.1: "I want to ⟨learn guitar⟩ for my ⟨Life⟩. It's ⟨something I keep doing⟩, ⟨about 3 times a week⟩, …" */
object MissionSentence {

    /**
     * The answered part of the sentence before [current], ending in a blank for [current].
     * With [current] null (the before-saving screen) it's the whole sentence.
     * Gentle start isn't part of the sentence; it has its own sticky note.
     */
    fun parts(draft: MissionDraft, answered: Set<EditorStep>, current: EditorStep?): List<SentencePart> {
        val out = mutableListOf<SentencePart>()
        for (step in EditorStep.entries) {
            if (step == EditorStep.GENTLE || !draft.applies(step)) continue
            if (step == current) {
                out += SentencePart(lead(draft, step))
                out += SentencePart("", step, placeholder = true)
                return out
            }
            if (step !in answered) return out
            out += SentencePart(lead(draft, step))
            out += answer(draft, step)
        }
        out += SentencePart(".")
        return out
    }

    private fun lead(draft: MissionDraft, step: EditorStep): String = when (step) {
        EditorStep.TITLE -> "I want to "
        EditorStep.PILLAR -> " for my "
        EditorStep.KIND -> ". It's "
        EditorStep.PACE -> if (draft.kind == MissionKind.BLOCK) ", on " else ", "
        EditorStep.DURATION -> ", "
        EditorStep.EFFORT -> ", and it feels "
        EditorStep.WEIGHT -> ". It's a "
        EditorStep.IF_MISSED -> ". If a day goes sideways, "
        EditorStep.GENTLE -> ""
    }

    private fun answer(draft: MissionDraft, step: EditorStep): List<SentencePart> = when (step) {
        EditorStep.TITLE -> listOf(SentencePart(draft.title.trim(), step))
        EditorStep.PILLAR -> listOf(SentencePart(draft.pillar.title, step))
        EditorStep.KIND -> listOf(SentencePart(draft.kind?.let { MissionWords.kindPhrase(it) }.orEmpty(), step))
        EditorStep.PACE -> pace(draft)
        EditorStep.DURATION -> listOf(SentencePart(MissionWords.durationWords(draft.durationMinutes), step), SentencePart(" each"))
        EditorStep.EFFORT -> listOf(SentencePart(MissionWords.effortWord(draft.effort), step))
        EditorStep.WEIGHT -> listOf(
            SentencePart(MissionWords.weightWord(draft.weight), step),
            SentencePart(" part of my ${draft.pillar.title} week"),
        )
        EditorStep.IF_MISSED -> listOf(SentencePart(MissionWords.ifMissedWords(draft.ifMissed), step))
        EditorStep.GENTLE -> emptyList()
    }

    private fun pace(draft: MissionDraft): List<SentencePart> {
        val step = EditorStep.PACE
        return when (draft.kind) {
            MissionKind.BLOCK -> listOf(
                SentencePart(MissionWords.daysLabel(draft.weekDays), step),
                SentencePart(", "),
                SentencePart("${MissionWords.clockLabel(draft.startMinute)} to ${MissionWords.clockLabel(draft.endMinute)}", step),
            )
            MissionKind.RHYTHM -> listOf(
                SentencePart("about ${MissionWords.intervalPhrase(draft.intervalCount, draft.intervalUnit)}", step)
            )
            MissionKind.RECHARGE -> listOf(SentencePart(MissionWords.countPhrase(draft.count, Period.WEEK), step))
            null -> emptyList()
            else -> buildList {
                add(SentencePart(MissionWords.countPhrase(draft.count, draft.period), step))
                if (draft.usesDays && draft.weekDays.isNotEmpty()) {
                    add(SentencePart(", usually on "))
                    add(SentencePart(MissionWords.daysLabel(draft.weekDays), step))
                }
                if (draft.kind == MissionKind.FINISH_LINE) {
                    draft.endDate?.let { end ->
                        add(SentencePart(", until "))
                        add(SentencePart(end.shortLabel(), step))
                    }
                    val milestones = draft.milestones.count { it.text.isNotBlank() }
                    if (milestones > 0) {
                        add(SentencePart(", with "))
                        add(SentencePart(if (milestones == 1) "1 milestone" else "$milestones milestones", step))
                    }
                }
            }
        }
    }
}
```


- [ ] **Step 2: Create `domain/WeekCost.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.plan.DaySettings
import kotlinx.datetime.LocalDate

/** One pillar's target this week, without and with the mission being saved. */
data class PillarCost(val pillar: PillarType, val before: Int, val after: Int)

/** Spec 4.2: what a mission costs this week, shown before it's saved. */
data class WeekCost(
    val pillars: List<PillarCost>,
    /** Free minutes in the week, from the per-weekday settings. */
    val freeMinutes: Int,
    /** Free minutes this week's flexible missions use, without the draft. */
    val plannedBefore: Int,
    /** The same with the draft (replacing its old version when editing). */
    val plannedAfter: Int,
    /** "Adds 1 hour and 6 points a week while you start gently. …" */
    val line: String,
)

/** This week's totals across a set of missions. */
object WeekLoad {

    fun freeMinutes(settings: List<DaySettings>): Int =
        (1..7).sumOf { day -> settings.firstOrNull { it.weekday == day }?.freeMinutes ?: DaySettings.defaultFor(day) }

    /** Fixed blocks use their own hours, so only the rest counts against free time. Recharge counts. */
    fun plannedMinutes(missions: List<Mission>, weekStart: LocalDate, history: History): Int =
        missions.filter { it.missionKind != MissionKind.BLOCK }
            .sumOf { it.durationMinutes * Points.occurrencesInWeek(it, weekStart, history) }

    fun points(missions: List<Mission>, weekStart: LocalDate, history: History): Int =
        PillarType.scoredPillars.sumOf { Points.weeklyTarget(missions, it, weekStart, history) }
}

object WeekCosts {

    /** [missions] are the active missions as stored; an edited mission's old version is among them. */
    fun compute(
        draft: MissionDraft,
        missions: List<Mission>,
        history: History,
        settings: List<DaySettings>,
        today: LocalDate,
    ): WeekCost {
        val weekStart = WeekMath.weekStart(today)
        val mission = draft.toMission()
        val isNew = mission.missionId == 0L
        val after = missions.filter { isNew || it.missionId != mission.missionId } + mission
        return WeekCost(
            pillars = PillarType.scoredPillars.map { pillar ->
                PillarCost(
                    pillar,
                    before = Points.weeklyTarget(missions, pillar, weekStart, history),
                    after = Points.weeklyTarget(after, pillar, weekStart, history),
                )
            },
            freeMinutes = WeekLoad.freeMinutes(settings),
            plannedBefore = WeekLoad.plannedMinutes(missions, weekStart, history),
            plannedAfter = WeekLoad.plannedMinutes(after, weekStart, history),
            line = line(draft, Points.occurrencesInWeek(mission, weekStart, history), isNew),
        )
    }

    /** The plain-words line, about a usual week, plus a note when this week counts fewer times. */
    fun line(draft: MissionDraft, thisWeek: Int, isNew: Boolean): String {
        val mission = draft.toMission()
        val verb = if (isNew) "Adds" else "Comes to"
        val minutes = mission.durationMinutes
        val weight = mission.weight
        return when (draft.kind) {
            MissionKind.BLOCK -> {
                val days = draft.weekDays.size
                "$verb ${MissionWords.pointsPhrase(days * weight)} a week. Fixed blocks use their own hours, not your free time." +
                    weekNote(thisWeek, days)
            }
            MissionKind.RECHARGE ->
                "$verb about ${MissionWords.hoursPhrase(mission.count * minutes)} of free time a week. " +
                    "No points, and it never counts as missed."
            MissionKind.RHYTHM ->
                "$verb ${MissionWords.hoursPhrase(minutes)} and ${MissionWords.pointsPhrase(weight)} " +
                    "${MissionWords.intervalPhrase(draft.intervalCount, draft.intervalUnit)}." + dueNote(thisWeek)
            else -> when (draft.period) {
                Period.DAY ->
                    "$verb ${MissionWords.hoursPhrase(7 * minutes)} and ${MissionWords.pointsPhrase(7 * weight)} a week, one each day." +
                        weekNote(thisWeek, 7)
                Period.MONTH ->
                    "$verb ${MissionWords.hoursPhrase(minutes)} and ${MissionWords.pointsPhrase(weight)}, " +
                        "${MissionWords.times(mission.count)} a month." + dueNote(thisWeek)
                Period.WEEK -> {
                    val now = mission.count
                    val base = "$verb ${MissionWords.hoursPhrase(now * minutes)} and ${MissionWords.pointsPhrase(now * weight)} a week"
                    val gentle = if (draft.gentleOn) {
                        " while you start gently. That's ${MissionWords.pointsPhrase(draft.count * weight)} once you're at ${draft.count}×."
                    } else "."
                    base + gentle + weekNote(thisWeek, now)
                }
            }
        }
    }

    private fun weekNote(thisWeek: Int, full: Int): String = when {
        thisWeek >= full -> ""
        thisWeek == 0 -> " It doesn't come up this week."
        else -> " This week it's $thisWeek×."
    }

    private fun dueNote(thisWeek: Int): String = if (thisWeek == 0) " It isn't due this week." else ""
}
```


- [ ] **Step 3: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

This plan changes no schema, so anything a build changed under `shared/schemas` is noise. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (usually 4.json)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/domain/MissionSentence.kt \
  $K/domain/WeekCost.kt
git commit -m "feat: mission sentence and week cost"
```


---

### Task 3: Repository writes for missions, milestones, archive and free time

**Files:**
- Modify: `db/data/mission/milestone/MilestoneDao.kt`, `db/data/plan/PlanDao.kt`, `db/data/todotask/TodayTaskDao.kt`
- Modify: `repo/TodayTaskRepository.kt`, `repo/MilestoneRepository.kt`, `repo/PlanRepository.kt`
- Rewrite: `repo/MissionRepository.kt`

**Depends on:** Task 1 (`MilestoneDraft`, `MissionDrafts.milestoneDates`).

Every new write uses the Plan 1 pattern: `withContext(NonCancellable + Dispatchers.Default) { mutex.withLock { … } }`. The Mutex isn't reentrant, so code inside a write calls only lock-free helpers. That's why `MissionRepository.setInactive` doesn't lock, and why `archiveMission` calls `refreshTodayIfOpen()` rather than `refreshToday()`.

- [ ] **Step 1: `db/data/mission/milestone/MilestoneDao.kt`: Add the `Insert` import**

Replace:

```kotlin
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
```

with:

```kotlin
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
```


- [ ] **Step 2: `db/data/mission/milestone/MilestoneDao.kt`: Add the queries the editor and the Missions screen need**

Replace:

```kotlin
    @Query("SELECT * FROM milestone")
    suspend fun getAll(): List<Milestone>

    @Update
    suspend fun updateMilestone(milestone: Milestone)
}
```

with:

```kotlin
    @Query("SELECT * FROM milestone")
    suspend fun getAll(): List<Milestone>

    @Query("SELECT * FROM milestone ORDER BY mile_mission_id, milestone_order")
    fun observeAll(): Flow<List<Milestone>>

    @Query("SELECT * FROM milestone WHERE mile_mission_id = :missionId ORDER BY milestone_order")
    suspend fun getForMission(missionId: Long): List<Milestone>

    @Insert
    suspend fun insert(milestone: Milestone): Long

    @Update
    suspend fun updateMilestone(milestone: Milestone)

    @Query("DELETE FROM milestone WHERE milestone_id = :milestoneId")
    suspend fun delete(milestoneId: Long)
}
```


- [ ] **Step 3: `db/data/plan/PlanDao.kt`: Observe the per-weekday settings**

Replace:

```kotlin
    @Query("SELECT * FROM day_settings ORDER BY settings_weekday")
    suspend fun getSettings(): List<DaySettings>

```

with:

```kotlin
    @Query("SELECT * FROM day_settings ORDER BY settings_weekday")
    suspend fun getSettings(): List<DaySettings>

    @Query("SELECT * FROM day_settings ORDER BY settings_weekday")
    fun observeSettings(): Flow<List<DaySettings>>

```


- [ ] **Step 4: `db/data/todotask/TodayTaskDao.kt`: Delete a mission's still-planned items from a date on (used by archive)**

Replace:

```kotlin
    @Query("DELETE FROM today_task WHERE today_task_id = :taskId")
    suspend fun delete(taskId: Long)

```

with:

```kotlin
    @Query("DELETE FROM today_task WHERE today_task_id = :taskId")
    suspend fun delete(taskId: Long)

    /** A mission's still-planned items from [from] on. Dates are yyyy-MM-dd, so text order is date order. */
    @Query("DELETE FROM today_task WHERE task_mission_id = :missionId AND task_date >= :from AND task_state = 'PLANNED'")
    suspend fun deletePlannedFrom(missionId: Long, from: String)

```


- [ ] **Step 5: `repo/TodayTaskRepository.kt`: Expose it**

Replace:

```kotlin
    suspend fun delete(taskId: Long) = todayTaskDao.delete(taskId)

```

with:

```kotlin
    suspend fun delete(taskId: Long) = todayTaskDao.delete(taskId)

    suspend fun deletePlannedFrom(missionId: Long, from: LocalDate) = todayTaskDao.deletePlannedFrom(missionId, key(from))

```


- [ ] **Step 6: `repo/MilestoneRepository.kt`: Expose the new milestone reads**

Replace:

```kotlin
    suspend fun getAll(): List<Milestone> = milestoneDao.getAll()

```

with:

```kotlin
    suspend fun getAll(): List<Milestone> = milestoneDao.getAll()

    fun observeAll(): Flow<List<Milestone>> = milestoneDao.observeAll()

    suspend fun getForMission(missionId: Long): List<Milestone> = milestoneDao.getForMission(missionId)

```


- [ ] **Step 7: Rewrite `repo/MissionRepository.kt`**

`save` stays lock-free for `SampleData`. `saveWithMilestones` is the editor's write. `MissionRepository.save` already returns the id on update.

```kotlin
package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionDao
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneDao
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.domain.MilestoneDraft
import com.example.todoapp.domain.MissionDrafts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MissionRepository(private val database: AppDatabase) {

    private val missionDao: MissionDao by lazy { database.getMissionDao() }
    private val milestoneDao: MilestoneDao by lazy { database.getMilestoneDao() }
    private val mutex = Mutex()

    fun observeActive(): Flow<List<Mission>> = missionDao.observeActive()

    suspend fun getActive(): List<Mission> = missionDao.getActive()

    suspend fun get(missionId: Long): Mission? = missionDao.get(missionId)

    /** Inserts or updates. Returns the mission's id either way (Room's upsert gives -1 on update). Doesn't lock. */
    suspend fun save(mission: Mission): Long {
        val id = missionDao.upsert(mission)
        return if (id == -1L) mission.missionId else id
    }

    /**
     * Saves a mission from the editor with its milestones, their due dates spread evenly up to the
     * mission's end date. Saved milestones left out of [milestones] are deleted; a kept one keeps its
     * done state. Pass an empty list for anything but a finish line. Returns the mission's id.
     */
    suspend fun saveWithMilestones(mission: Mission, milestones: List<MilestoneDraft>): Long = write {
        val id = save(mission)
        val existing = milestoneDao.getForMission(id)
        val kept = milestones.map { it.copy(text = it.text.trim()) }.filter { it.text.isNotEmpty() }
        val keptIds = kept.map { it.milestoneId }.filter { it != 0L }.toSet()
        existing.filter { it.milestoneId !in keptIds }.forEach { milestoneDao.delete(it.milestoneId) }
        val dates = MissionDrafts.milestoneDates(mission.startDate.toLocalDate(), mission.endDate?.toLocalDate(), kept.size)
        kept.forEachIndexed { order, draft ->
            val due = MyDate.fromLocalDate(dates[order])
            val old = existing.firstOrNull { draft.milestoneId != 0L && it.milestoneId == draft.milestoneId }
            if (old != null) {
                milestoneDao.updateMilestone(old.copy(text = draft.text, milestoneOrder = order, expectedCompletionDate = due))
            } else {
                milestoneDao.insert(
                    Milestone(
                        missionId = id,
                        text = draft.text,
                        priority = "NORMAL",
                        expectedCompletionDate = due,
                        milestoneOrder = order,
                        status = TaskStatus.IN_PROGRESS.value,
                        progressValue = 0f,
                        progressUnit = 0f,
                        progressText = "",
                        mileStartDate = mission.startDate,
                        actualCompletionDate = null,
                        completionTaskId = null,
                    )
                )
            }
        }
        id
    }

    /** Archives a mission. Doesn't lock: PlanRepository.archiveMission calls it inside its own write. */
    suspend fun setInactive(missionId: Long) {
        missionDao.get(missionId)?.let { missionDao.upsert(it.copy(active = false)) }
    }

    suspend fun count(): Int = missionDao.count()

    /** Editor writes: one at a time, finished even if the screen goes away, and off the main thread. */
    private suspend fun <T> write(block: suspend () -> T): T =
        withContext(NonCancellable + Dispatchers.Default) { mutex.withLock { block() } }
}
```


- [ ] **Step 8: `repo/PlanRepository.kt`: Observe the settings**

Replace:

```kotlin
    fun observePlan(date: LocalDate): Flow<DayPlan?> = planDao.observePlan(key(date))

```

with:

```kotlin
    fun observePlan(date: LocalDate): Flow<DayPlan?> = planDao.observePlan(key(date))

    fun observeSettings(): Flow<List<DaySettings>> = planDao.observeSettings()

```


- [ ] **Step 9: `repo/PlanRepository.kt`: Add the three writes after `removeTask`. Each goes through `write`, so it holds the plan lock, can't be cancelled, and runs off the main thread**

Replace:

```kotlin
    suspend fun removeTask(taskId: Long) = write { todayTaskRepository.delete(taskId) }

```

with:

```kotlin
    suspend fun removeTask(taskId: Long) = write { todayTaskRepository.delete(taskId) }

    /** Free minutes per weekday (ISO numbers). Days already planned keep theirs; today changes from the diary. */
    suspend fun saveSettings(minutesByWeekday: Map<Int, Int>) = write {
        planDao.upsertSettings(
            (1..7).map { day ->
                DaySettings(day, (minutesByWeekday[day] ?: DaySettings.defaultFor(day)).coerceIn(0, MAX_FREE_MINUTES))
            }
        )
    }

    /** After a mission is saved: re-picks today's open items, unless the day was confirmed with "Looks good". */
    suspend fun refreshToday() = write { refreshTodayIfOpen() }

    /**
     * Archives a mission: it leaves the Missions screen and the planner, and its still-planned
     * items from today on are removed. Then today is re-picked as in [refreshToday].
     */
    suspend fun archiveMission(missionId: Long) = write {
        missionRepository.setInactive(missionId)
        todayTaskRepository.deletePlannedFrom(missionId, today())
        refreshTodayIfOpen()
    }

```


- [ ] **Step 10: `repo/PlanRepository.kt`: Add the lock-free helper just above `currentPlan` (the Mutex isn't reentrant, so `archiveMission` calls this rather than `refreshToday`)**

Replace:

```kotlin
    private suspend fun currentPlan(date: LocalDate): DayPlan =
```

with:

```kotlin
    /** Rebuilds today's open picks when today has a plan that isn't confirmed. Call only inside [write]. */
    private suspend fun refreshTodayIfOpen() {
        val date = today()
        val plan = planDao.getPlan(key(date)) ?: return
        if (!plan.confirmed) rebuild(date, plan)
    }

    private suspend fun currentPlan(date: LocalDate): DayPlan =
```


- [ ] **Step 11: `repo/PlanRepository.kt`: Add the free-time cap to the companion**

Replace:

```kotlin
        const val HISTORY_DAYS = 120
        const val MAX_CROWNS = 3
```

with:

```kotlin
        const val HISTORY_DAYS = 120
        const val MAX_CROWNS = 3
        /** Same cap as the diary's "Your day" dialog. */
        const val MAX_FREE_MINUTES = 16 * 60
```


- [ ] **Step 12: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 13: Commit**

This plan changes no schema, so anything a build changed under `shared/schemas` is noise. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (usually 4.json)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/db/data/mission/milestone/MilestoneDao.kt \
  $K/db/data/plan/PlanDao.kt \
  $K/db/data/todotask/TodayTaskDao.kt \
  $K/repo/TodayTaskRepository.kt \
  $K/repo/MilestoneRepository.kt \
  $K/repo/MissionRepository.kt \
  $K/repo/PlanRepository.kt
git commit -m "feat: repository writes for missions, milestones, archive and free time"
```


---

### Task 4: The editor view model

**Files:**
- Create: `screen/missions/editor/MissionEditorViewModel.kt`
- Modify: `di/DI.kt`

**Depends on:** Tasks 1–3.

How the flow moves:
- **A new mission** starts on `TITLE` with nothing answered. Each answer goes to the first question that applies and isn't answered yet. With none left, it opens the before-saving screen (`step == null`).
- **An existing mission** starts on the before-saving screen with every step answered. Tapping an answer (`goTo`) opens that one question.
- **A question that only now applies** is asked again, for example when a pace change makes gentle start possible, or a recharge mission becomes a habit. A new kind always re-asks the pace.
- **Back** returns to the before-saving screen from a question opened there. Otherwise it walks back through the questions. It returns `false` when the screen should close.
- **Taps are guarded.** `answer` ignores a tap for a question that isn't on screen. `save` and `archive` ignore a second tap while `saving`. Both writes finish under `NonCancellable`, even if the tab closes mid-save.

- [ ] **Step 1: Create `screen/missions/editor/MissionEditorViewModel.kt`**

```kotlin
package com.example.todoapp.screen.missions.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Effort
import com.example.todoapp.domain.EditorStep
import com.example.todoapp.domain.History
import com.example.todoapp.domain.IfMissed
import com.example.todoapp.domain.MilestoneDraft
import com.example.todoapp.domain.MissionDraft
import com.example.todoapp.domain.MissionDrafts
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.Period
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.WeekCost
import com.example.todoapp.domain.WeekCosts
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class EditorUiState(
    val isNew: Boolean,
    val draft: MissionDraft,
    val answered: Set<EditorStep>,
    /** The question on screen; null on the before-saving screen. */
    val step: EditorStep?,
    /** The question was opened from the before-saving screen, so Back returns there. */
    val fromSummary: Boolean = false,
    val saving: Boolean = false,
) {
    val steps: List<EditorStep> get() = draft.steps

    /** 1-based place of [step] for "3 of 9"; the step count on the before-saving screen. */
    val position: Int get() = step?.let { steps.indexOf(it) + 1 } ?: steps.size

    /** Every question that applies has an answer. */
    val complete: Boolean get() = draft.title.isNotBlank() && steps.all { it in answered }
}

/**
 * The nine-question sentence for a new mission ([missionId] 0) or an existing one.
 * A new mission walks the steps in order; an existing one opens on the before-saving screen,
 * and tapping an answer there asks that one question again.
 */
class MissionEditorViewModel(
    private val missionRepository: MissionRepository,
    todayTaskRepository: TodayTaskRepository,
    private val planRepository: PlanRepository,
    private val milestoneRepository: MilestoneRepository,
    private val missionId: Long,
) : ViewModel() {

    val today: LocalDate = MyDate.now().toLocalDate()
    private var initial: MissionDraft? = null

    private val _state = MutableStateFlow<EditorUiState?>(null)
    val state: StateFlow<EditorUiState?> = _state

    private val _finished = MutableStateFlow(false)
    /** True once saved or archived, or when the mission to edit is gone. The screen then closes. */
    val finished: StateFlow<Boolean> = _finished

    /** What the mission costs this week; worked out only on the before-saving screen. */
    val cost: StateFlow<WeekCost?> = combine(
        _state,
        missionRepository.observeActive(),
        todayTaskRepository.observeBetween(
            today.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY),
            today.plus(7, DateTimeUnit.DAY),
        ),
        planRepository.observeSettings(),
    ) { s, missions, tasks, settings ->
        if (s == null || s.step != null) null else WeekCosts.compute(s.draft, missions, History(tasks), settings, today)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val draft = if (missionId == 0L) {
                MissionDrafts.new(today)
            } else {
                val mission = missionRepository.get(missionId)
                if (mission == null || !mission.active) {
                    _finished.value = true
                    return@launch
                }
                MissionDrafts.from(mission, milestoneRepository.getForMission(missionId))
            }
            initial = draft
            _state.value = EditorUiState(
                isNew = missionId == 0L,
                draft = draft,
                answered = if (missionId == 0L) emptySet() else EditorStep.entries.toSet(),
                step = if (missionId == 0L) EditorStep.TITLE else null,
            )
        }
    }

    /** Worth asking before leaving: anything written for a new mission, any change to an existing one. */
    fun hasChanges(): Boolean {
        val s = _state.value ?: return false
        return if (s.isNew) s.draft.title.isNotBlank() || s.answered.isNotEmpty() else s.draft != initial
    }

    // Step 1. A blank title isn't an answer, so Back can't return to a sentence without one.
    fun setTitle(text: String) {
        change { it.copy(title = text.take(MissionDrafts.MAX_TITLE_LENGTH)) }
        if (text.isBlank()) _state.value?.let { _state.value = it.copy(answered = it.answered - EditorStep.TITLE) }
    }

    fun confirmTitle() {
        if (_state.value?.draft?.title?.isNotBlank() == true) answer(EditorStep.TITLE)
    }

    // Step 2
    fun pickPillar(pillar: PillarType) = answer(EditorStep.PILLAR) { it.copy(pillarChoice = pillar) }

    // Step 3: a different kind starts its pace over.
    fun pickKind(kind: MissionKind) {
        val s = _state.value ?: return
        if (s.step != EditorStep.KIND || s.saving) return
        if (s.draft.kind != kind) _state.value = s.copy(answered = s.answered - EditorStep.PACE - EditorStep.GENTLE)
        answer(EditorStep.KIND) { it.withKind(kind, today) }
    }

    // Step 4. Changes show at once; "Next" confirms them.
    fun setCount(count: Int, period: Period) = change {
        it.copy(
            count = count,
            period = period,
            gentleCount = null,
            weekDays = if (period == Period.WEEK) it.weekDays else emptySet(),
        )
    }

    fun toggleDay(day: Int) = change { d ->
        d.copy(weekDays = if (day in d.weekDays) d.weekDays - day else d.weekDays + day)
    }

    fun shiftStart(minutes: Int) = change { d ->
        val start = (d.startMinute + minutes).coerceIn(0, MissionDrafts.DAY_MINUTES - MissionDrafts.CLOCK_STEP)
        d.copy(startMinute = start, endMinute = d.endMinute.coerceAtLeast(start + MissionDrafts.CLOCK_STEP))
    }

    fun shiftEnd(minutes: Int) = change { d ->
        d.copy(endMinute = (d.endMinute + minutes).coerceIn(d.startMinute + MissionDrafts.CLOCK_STEP, MissionDrafts.DAY_MINUTES))
    }

    /** A finish line ends at least a week out and at most a year out; one already past can only move later. */
    fun shiftEndDate(weeks: Int) = change { d ->
        val earliest = maxOf(today, d.startDate).plus(7, DateTimeUnit.DAY)
        val latest = today.plus(MissionDrafts.MAX_FINISH_WEEKS * 7, DateTimeUnit.DAY)
        val current = d.endDate ?: earliest
        val moved = current.plus(weeks * 7, DateTimeUnit.DAY)
        d.copy(endDate = moved.coerceIn(minOf(earliest, current), maxOf(latest, current)))
    }

    fun addMilestone() = change { d ->
        if (d.milestones.size >= MissionDrafts.MAX_MILESTONES) d else d.copy(milestones = d.milestones + MilestoneDraft())
    }

    fun setMilestone(index: Int, text: String) = change { d ->
        d.copy(milestones = d.milestones.mapIndexed { i, m ->
            if (i == index) m.copy(text = text.take(MissionDrafts.MAX_MILESTONE_LENGTH)) else m
        })
    }

    fun removeMilestone(index: Int) = change { d -> d.copy(milestones = d.milestones.filterIndexed { i, _ -> i != index }) }

    fun setInterval(count: Int, unit: Period) = change {
        it.copy(intervalCount = count.coerceIn(MissionDrafts.intervalRange(unit)), intervalUnit = unit)
    }

    /** Recharge's pace is a single tap. */
    fun pickRechargeCount(count: Int) = answer(EditorStep.PACE) { it.copy(count = count, period = Period.WEEK) }

    /** "Next" on the pace step. A block needs at least one day. */
    fun confirmPace() {
        val d = _state.value?.draft ?: return
        if (d.kind == MissionKind.BLOCK && d.weekDays.isEmpty()) return
        answer(EditorStep.PACE)
    }

    // Steps 5 to 9
    fun pickDuration(minutes: Int) = answer(EditorStep.DURATION) { it.copy(durationMinutes = minutes) }
    fun pickEffort(effort: Effort) = answer(EditorStep.EFFORT) { it.copy(effort = effort) }
    fun pickWeight(weight: Int) = answer(EditorStep.WEIGHT) { it.copy(weight = weight) }
    fun pickIfMissed(rule: IfMissed) = answer(EditorStep.IF_MISSED) { it.copy(ifMissed = rule) }
    fun pickGentle(on: Boolean) = answer(EditorStep.GENTLE) { it.copy(gentle = on) }

    /** The gentle-start sticky on the before-saving screen. */
    fun setGentle(on: Boolean) {
        change { it.copy(gentle = on) }
        _state.value?.let { _state.value = it.copy(answered = it.answered + EditorStep.GENTLE) }
    }

    /** Tapping a highlighted answer asks that question again. */
    fun goTo(step: EditorStep) {
        val s = _state.value ?: return
        if (!s.saving && s.draft.applies(step)) _state.value = s.copy(step = step, fromSummary = true)
    }

    /** "Make it lighter" returns to the pace question. */
    fun makeLighter() = goTo(EditorStep.PACE)

    /**
     * One step back. A question opened from the before-saving screen returns there; otherwise Back walks
     * back through the questions. From the before-saving screen a new mission goes back to its last
     * question. False when there's nowhere left to go, so the screen should close.
     */
    fun back(): Boolean {
        val s = _state.value ?: return false
        if (s.saving) return true
        val steps = s.steps
        val step = s.step
        if (step == null) {
            if (!s.isNew) return false
            _state.value = s.copy(step = steps.last(), fromSummary = false)
            return true
        }
        if (s.fromSummary && steps.all { it in s.answered }) {
            _state.value = s.copy(step = null, fromSummary = false)
            return true
        }
        val index = steps.indexOf(step)
        if (index <= 0) return false
        _state.value = s.copy(step = steps[index - 1])
        return true
    }

    /** Saves once, then re-picks today if it isn't confirmed yet. Both finish even if the screen goes away. */
    fun save() {
        val s = _state.value ?: return
        if (s.saving || s.step != null || !s.complete) return
        _state.value = s.copy(saving = true)
        viewModelScope.launch {
            val savedId = withContext(NonCancellable) {
                val mission = s.draft.toMission()
                val milestones = if (s.draft.kind == MissionKind.FINISH_LINE) s.draft.milestones else emptyList()
                val id = missionRepository.saveWithMilestones(mission, milestones)
                planRepository.refreshToday()
                id
            }
            // The saved row now counts in the week, so the cost mustn't add the draft on top before the screen closes.
            _state.value = _state.value?.let { it.copy(draft = it.draft.copy(missionId = savedId)) }
            _finished.value = true
        }
    }

    fun archive() {
        val s = _state.value ?: return
        if (s.isNew || s.saving) return
        _state.value = s.copy(saving = true)
        viewModelScope.launch {
            planRepository.archiveMission(s.draft.missionId)
            _finished.value = true
        }
    }

    /** Applies a change to the draft. A question that only now applies is asked again. */
    private fun change(transform: (MissionDraft) -> MissionDraft) {
        val s = _state.value ?: return
        if (s.saving) return
        val draft = transform(s.draft)
        val newlyAsked = EditorStep.entries.filter { draft.applies(it) && !s.draft.applies(it) }.toSet()
        _state.value = s.copy(draft = draft, answered = s.answered - newlyAsked)
    }

    /** Records the answer to [step] (ignored if [step] isn't on screen) and moves to the first open question, or the before-saving screen. */
    private fun answer(step: EditorStep, transform: (MissionDraft) -> MissionDraft = { it }) {
        if (_state.value?.step != step) return
        change(transform)
        val s = _state.value ?: return
        if (s.saving) return
        val answered = s.answered + step
        val next = s.draft.steps.firstOrNull { it !in answered }
        _state.value = s.copy(answered = answered, step = next, fromSummary = s.fromSummary && next != null)
    }
}
```


- [ ] **Step 2: `di/DI.kt`: Import the editor view model**

Replace:

```kotlin
import com.example.todoapp.screen.metrics.CalDiaryViewModel

```

with:

```kotlin
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.missions.editor.MissionEditorViewModel

```


- [ ] **Step 3: `di/DI.kt`: Register it as a factory keyed by mission id (0 for a new mission)**

Replace:

```kotlin
        factory { (day: LocalDate) -> WaterIntakeViewModel(get()) }

```

with:

```kotlin
        factory { (day: LocalDate) -> WaterIntakeViewModel(get()) }
        factory { (missionId: Long) -> MissionEditorViewModel(get(), get(), get(), get(), missionId) }

```


- [ ] **Step 4: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

This plan changes no schema, so anything a build changed under `shared/schemas` is noise. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (usually 4.json)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/screen/missions/editor/MissionEditorViewModel.kt \
  $K/di/DI.kt
git commit -m "feat: mission editor view model"
```


---

### Task 5: Editor building blocks

**Files:**
- Create: `screen/basicutils/components/MarkerHeading.kt`
- Create: `screen/missions/editor/EditorParts.kt`
- Modify: `screen/basicutils/AppColors.kt`, `screen/diary/plan/PlanNotes.kt`

**Depends on:** Tasks 1–2. Reuses Plan 1's `SmallChip`, `StickyNote`, palette and `block_b`.

`SentenceText` draws the ruled lines and marker highlights itself from the `TextLayoutResult`. Span backgrounds would fill the whole 44sp line box, so it doesn't use them. It finds taps with `getOffsetForPosition` and string annotations. Every tap target here is 44dp or more. A sentence line is 44sp tall, so each highlighted answer is a 44dp row.

- [ ] **Step 1: Create `screen/basicutils/components/MarkerHeading.kt`**

The diary's `HeadingBanner` is fixed at 30dp and would clip a long question like "HOW BIG A PART OF YOUR HEALTH WEEK IS IT?". This one sizes the swipe to its text.

```kotlin
package com.example.todoapp.screen.basicutils.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.block_b

/** A heading on the block_b marker swipe, sized to its text so long questions wrap instead of clipping. */
@Composable
fun MarkerHeading(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 34.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.heightIn(min = 32.dp), contentAlignment = Alignment.Center) {
            Image(
                painterResource(Res.drawable.block_b),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
            )
            Text(
                text,
                fontSize = 15.sp,
                letterSpacing = 1.sp,
                color = Color(0xFF2A2A2A),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
            )
        }
    }
}
```


- [ ] **Step 2: `screen/basicutils/AppColors.kt`: Add the answer-card border from the mockups**

Replace:

```kotlin
val ChipBorder = Color(0xFFD9CDB8)

```

with:

```kotlin
val ChipBorder = Color(0xFFD9CDB8)
val CardBorder = Color(0xFFEDE6D6)

```


- [ ] **Step 3: `screen/diary/plan/PlanNotes.kt`: Share the peach button with the editor (dark brown text on peach, 44dp tall)**

Replace:

```kotlin
@Composable
private fun PlanButton(text: String, filled: Boolean, onClick: () -> Unit) {
```

with:

```kotlin
/** The peach button: filled with dark text, or outlined. Also used by the mission editor. */
@Composable
internal fun PlanButton(text: String, filled: Boolean, onClick: () -> Unit) {
```


- [ ] **Step 4: Create `screen/missions/editor/EditorParts.kt`**

```kotlin
package com.example.todoapp.screen.missions.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.EditorStep
import com.example.todoapp.domain.MissionDraft
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.SentencePart
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.CardBorder
import com.example.todoapp.screen.basicutils.ChipBorder
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Orange180
import com.example.todoapp.screen.basicutils.Orange80
import com.example.todoapp.screen.basicutils.PeachInk
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.TrackBeige
import com.example.todoapp.screen.basicutils.Yellow80
import com.example.todoapp.screen.basicutils.pillarLight
import com.example.todoapp.screen.diary.plan.PlanButton
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs

private const val STEP_TAG = "step"
private const val BLANK_TAG = "blank"
private const val SENTENCE_FONT = 22
private const val SENTENCE_LINE = 44

/** Tilts for stacked cards, as on the mockups. */
internal val cardTilts = listOf(-0.7f, 0.6f, -0.4f, 0.7f, -0.5f)

/** The pillar's light color once a pillar is chosen; a neutral marker before. */
fun sentenceHighlight(draft: MissionDraft): Color =
    if (draft.kind == MissionKind.RECHARGE || draft.pillarChoice != null) pillarLight(draft.pillar) else Yellow80

/**
 * The mission sentence on ruled diary lines. Answers get a marker highlight; the blank being asked
 * gets a dashed line. With [onTap], tapping an answer reports its step. Lines are 44 tall, so each
 * highlighted answer is a 44dp target.
 */
@Composable
fun SentenceText(
    parts: List<SentencePart>,
    highlight: Color,
    modifier: Modifier = Modifier,
    onTap: ((EditorStep) -> Unit)? = null,
) {
    val text = remember(parts) {
        buildAnnotatedString {
            parts.forEach { part ->
                val step = part.step
                when {
                    part.placeholder -> {
                        pushStringAnnotation(BLANK_TAG, step?.name.orEmpty())
                        append(" ".repeat(12))
                        pop()
                    }
                    step != null -> {
                        pushStringAnnotation(STEP_TAG, step.name)
                        append(part.text)
                        pop()
                    }
                    else -> append(part.text)
                }
            }
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentOnTap by rememberUpdatedState(onTap)
    val tapModifier = if (onTap == null) Modifier else Modifier.pointerInput(text) {
        detectTapGestures { position ->
            val l = layout ?: return@detectTapGestures
            val offset = l.getOffsetForPosition(position)
            val name = text.getStringAnnotations(STEP_TAG, offset, offset).firstOrNull()?.item
            EditorStep.entries.firstOrNull { it.name == name }?.let { currentOnTap?.invoke(it) }
        }
    }
    Text(
        text,
        fontSize = SENTENCE_FONT.sp,
        lineHeight = SENTENCE_LINE.sp,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Light,
        letterSpacing = 0.4.sp,
        color = InkBlack,
        onTextLayout = { layout = it },
        modifier = modifier.fillMaxWidth()
            .heightIn(min = (SENTENCE_LINE * 3).dp)
            .drawBehind {
                val l = layout ?: return@drawBehind
                val stroke = 1.dp.toPx()
                var y = 0f
                for (line in 0 until l.lineCount) {
                    y = l.getLineBottom(line)
                    drawLine(Blue80, Offset(0f, y), Offset(size.width, y), stroke)
                }
                val step = SENTENCE_LINE.sp.toPx()
                y += step
                while (y <= size.height) {
                    drawLine(Blue80, Offset(0f, y), Offset(size.width, y), stroke)
                    y += step
                }
                val fontPx = SENTENCE_FONT.sp.toPx()
                text.getStringAnnotations(STEP_TAG, 0, text.length).forEach { marker(l, it.start, it.end, highlight, fontPx) }
                text.getStringAnnotations(BLANK_TAG, 0, text.length).forEach { blank(l, it.start, it.end) }
            }
            .then(tapModifier)
    )
}

/** A marker swipe behind [start, end), line by line, around the letters rather than the whole line box. */
private fun DrawScope.marker(l: TextLayoutResult, start: Int, end: Int, color: Color, fontPx: Float) {
    if (end <= start) return
    val pad = 3.dp.toPx()
    for (line in l.getLineForOffset(start)..l.getLineForOffset(end - 1)) {
        val from = maxOf(start, l.getLineStart(line))
        val to = minOf(end, l.getLineEnd(line, visibleEnd = true))
        if (to <= from) continue
        val left = l.getBoundingBox(from).left
        val right = l.getBoundingBox(to - 1).right
        val baseline = l.getLineBaseline(line)
        drawRoundRect(
            color,
            topLeft = Offset(minOf(left, right) - pad, baseline - fontPx * 0.8f),
            size = Size(abs(right - left) + 2 * pad, fontPx * 1.05f),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
    }
}

/** The dashed blank for the question being asked. It never wraps: it's made of no-break spaces. */
private fun DrawScope.blank(l: TextLayoutResult, start: Int, end: Int) {
    if (end <= start) return
    val line = l.getLineForOffset(start)
    val left = l.getBoundingBox(start).left
    val right = l.getBoundingBox(end - 1).right
    val y = l.getLineBaseline(line) + 4.dp.toPx()
    drawLine(
        Orange180, Offset(minOf(left, right), y), Offset(maxOf(left, right), y),
        strokeWidth = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))
    )
}

/** Close, "New mission", "3 of 9". */
@Composable
fun EditorHeader(title: String, progress: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clickable(role = Role.Button) { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = InkGray)
        }
        Text(
            title,
            fontSize = 20.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(progress, fontSize = 13.sp, color = InkGray, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 44.dp))
    }
}

/** One segment per question that applies; filled up to [done]. */
@Composable
fun StepProgress(total: Int, done: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(total) { i ->
            Box(
                modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (i < done) Orange180 else TrackBeige)
            )
        }
    }
}

/** A tilted white answer card, as on the kind step of the mockups. */
@Composable
fun ChoiceCard(
    title: String,
    example: String?,
    selected: Boolean,
    tilt: Float,
    onClick: () -> Unit,
    icon: DrawableResource? = null,
    accent: Color? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .rotate(tilt)
            .shadow(3.dp, shape)
            .clip(shape)
            .background(Color.White)
            .border(if (selected) 2.dp else 1.dp, if (selected) Orange180 else CardBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (accent != null) {
            Box(modifier = Modifier.size(width = 6.dp, height = 34.dp).clip(RoundedCornerShape(3.dp)).background(accent))
            Spacer(Modifier.width(14.dp))
        }
        if (icon != null) {
            Image(
                painterResource(icon), contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFF6B6B6B)), modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, color = InkBlack)
            if (example != null) Text(example, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray)
        }
    }
}

/** Small grey label above a group of controls. */
@Composable
fun FieldLabel(text: String) {
    Text(
        text, fontSize = 13.sp, color = InkGray,
        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 4.dp)
    )
}

/** Monday-to-Sunday toggles, 44dp each. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayToggles(selected: Set<Int>, onToggle: (Int) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (1..7).forEach { day ->
            val on = day in selected
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .border(1.dp, if (on) Orange180 else ChipBorder, CircleShape)
                    .background(if (on) Orange80 else Color.Transparent)
                    .clickable(role = Role.Checkbox) { onToggle(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(MissionWords.shortDay(day), fontSize = 12.sp, color = if (on) PeachInk else Color(0xFF4A4A4A))
            }
        }
    }
}

/** "From  (−)  9:00  (+)". */
@Composable
fun Stepper(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = InkGray, modifier = Modifier.width(64.dp))
        StepButton("−", "$label earlier or less", minusEnabled, onMinus)
        Text(
            value, fontSize = 18.sp, fontStyle = FontStyle.Italic, color = InkBlack, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        StepButton("+", "$label later or more", plusEnabled, onPlus)
    }
}

@Composable
private fun StepButton(symbol: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape)
            .border(1.5.dp, if (enabled) Orange180 else ChipBorder, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 20.sp, color = if (enabled) PeachText else ChipBorder)
    }
}

/** Back, the reassurance line, and Next when the step needs one and it can go on. */
@Composable
fun StepFooter(onBack: () -> Unit, onNext: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.heightIn(min = 44.dp).clickable(role = Role.Button) { onBack() }.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Back", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PeachText)
        }
        Text(
            "You can change any of this later.",
            fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        if (onNext != null) PlanButton("Next", filled = true, onClick = onNext)
    }
}
```


- [ ] **Step 5: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

This plan changes no schema, so anything a build changed under `shared/schemas` is noise. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (usually 4.json)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/screen/basicutils/components/MarkerHeading.kt \
  $K/screen/missions/editor/EditorParts.kt \
  $K/screen/basicutils/AppColors.kt \
  $K/screen/diary/plan/PlanNotes.kt
git commit -m "feat: editor parts: ruled sentence, answer cards, steppers"
```


---

### Task 6: The before-saving screen

**Files:**
- Create: `screen/missions/editor/WeekCostView.kt`
- Create: `screen/missions/editor/SummaryStep.kt`

**Depends on:** Tasks 2, 4 and 5.

Spec 4.2 in order:
1. The full sentence. Tapping a highlight jumps to that step.
2. The gentle-start sticky, when it applies, with both choices.
3. "YOUR WEEK": one bar per pillar on a shared scale, with the new points striped, and "6 → 12" when a target changes.
4. The plain-words line.
5. Free hours planned against free hours available, with the mission's share striped.
6. Make it lighter (back to the pace question) and Save mission.

An existing mission also gets "Archive this mission". `SummaryStep` emits several children, so call it inside a `Column`.

- [ ] **Step 1: Create `screen/missions/editor/WeekCostView.kt`**

```kotlin
package com.example.todoapp.screen.missions.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.WeekCost
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Orange180
import com.example.todoapp.screen.basicutils.TrackBeige
import com.example.todoapp.screen.basicutils.pillarStrong

/** Spec 4.2: each pillar's target this week with the new points striped, the plain-words line, and free hours. */
@Composable
fun WeekCostView(cost: WeekCost, draftPillar: PillarType) {
    val scale = cost.pillars.maxOf { maxOf(it.before, it.after) }.coerceAtLeast(1)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        cost.pillars.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.pillar.title, fontSize = 14.sp, color = Color(0xFF3A3A3A), modifier = Modifier.width(64.dp))
                CostBar(p.before, p.after, scale, pillarStrong(p.pillar), pillarStrong(p.pillar), Modifier.weight(1f))
                Text(
                    if (p.before == p.after) "${p.after}" else "${p.before} → ${p.after}",
                    fontSize = 13.sp, color = Color(0xFF4A4A4A), textAlign = TextAlign.End,
                    modifier = Modifier.width(64.dp)
                )
            }
        }
    }
    Text(
        cost.line,
        fontSize = 14.sp, fontStyle = FontStyle.Italic, lineHeight = 20.sp, color = Color(0xFF3A3A3A),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)
    )
    Column(modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Free hours planned", fontSize = 13.sp, color = Color(0xFF4A4A4A))
            Text(
                "${MissionWords.hoursNumber(cost.plannedAfter)} of ${MissionWords.hoursNumber(cost.freeMinutes)}",
                fontSize = 13.sp, color = Color(0xFF4A4A4A)
            )
        }
        Spacer(Modifier.height(6.dp))
        CostBar(
            cost.plannedBefore, cost.plannedAfter, maxOf(cost.freeMinutes, cost.plannedAfter, 1),
            Orange180, pillarStrong(draftPillar), Modifier.fillMaxWidth()
        )
        Text(
            MissionWords.spareLine(cost.freeMinutes, cost.plannedAfter),
            fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/** [before] in solid [color]; anything [after] adds on top, striped in [stripe]. */
@Composable
private fun CostBar(before: Int, after: Int, scale: Int, color: Color, stripe: Color, modifier: Modifier) {
    Box(
        modifier = modifier.height(12.dp).clip(RoundedCornerShape(6.dp)).background(TrackBeige)
            .drawBehind {
                val unit = size.width / scale
                val solid = minOf(before, after) * unit
                if (solid > 0f) drawRect(color, size = Size(solid, size.height))
                val added = (after - before).coerceAtLeast(0) * unit
                if (added > 0f) {
                    clipRect(left = solid, top = 0f, right = solid + added, bottom = size.height) {
                        drawRect(stripe.copy(alpha = 0.35f), topLeft = Offset(solid, 0f), size = Size(added, size.height))
                        val gap = 8.dp.toPx()
                        var x = solid - size.height
                        while (x < solid + added) {
                            drawLine(stripe, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 3.dp.toPx())
                            x += gap
                        }
                    }
                }
            }
    )
}
```


- [ ] **Step 2: Create `screen/missions/editor/SummaryStep.kt`**

```kotlin
package com.example.todoapp.screen.missions.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.EditorStep
import com.example.todoapp.domain.MissionDraft
import com.example.todoapp.domain.MissionSentence
import com.example.todoapp.domain.WeekCost
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.components.MarkerHeading
import com.example.todoapp.screen.diary.plan.PlanButton
import com.example.todoapp.screen.diary.plan.StickyNote
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.wand

/** Spec 4.2, before saving: the whole sentence, gentle start, the week's cost, then Make it lighter and Save. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryStep(
    state: EditorUiState,
    cost: WeekCost?,
    onTap: (EditorStep) -> Unit,
    onGentle: (Boolean) -> Unit,
    onLighter: () -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit,
) {
    val draft = state.draft
    SentenceText(
        MissionSentence.parts(draft, state.answered, current = null),
        highlight = sentenceHighlight(draft),
        onTap = onTap,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp)
    )
    Text(
        "Tap any highlighted words to change them.",
        fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp)
    )
    if (draft.gentleEligible) GentleNote(draft, state.isNew, onGentle)
    MarkerHeading("YOUR WEEK", Blue80, Modifier.padding(top = 22.dp))
    if (cost != null) WeekCostView(cost, draft.pillar)
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlanButton("Make it lighter", filled = false, onClick = onLighter)
        PlanButton(if (state.saving) "Saving…" else "Save mission", filled = true, onClick = onSave)
    }
    if (!state.isNew) {
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.heightIn(min = 44.dp).clickable(role = Role.Button) { onArchive() }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Archive this mission", fontSize = 14.sp, color = PeachText, textDecoration = TextDecoration.Underline)
            }
        }
    }
}

/** The gentle-start sticky: shows the choice and lets it flip without leaving the screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GentleNote(draft: MissionDraft, isNew: Boolean, onGentle: (Boolean) -> Unit) {
    StickyNote(rotation = -1f) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(Res.drawable.wand), contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(0xFF6A5A2A)), modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (isNew) "IT'S NEW. START GENTLY?" else "START GENTLY?",
                    fontSize = 11.sp, letterSpacing = 1.2.sp, color = Color(0xFF6A5A2A)
                )
            }
            Text(
                if (draft.gentleOn) {
                    "${draft.currentCount}× a week to begin, then ${draft.count}× after 3 good weeks."
                } else {
                    "Straight to ${draft.count}× a week from the start."
                },
                fontSize = 16.sp, fontStyle = FontStyle.Italic, lineHeight = 23.sp, color = InkBlack,
                modifier = Modifier.padding(top = 6.dp)
            )
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlanButton("Start gently", filled = draft.gentleOn, onClick = { onGentle(true) })
                PlanButton("Go straight to ${draft.count}×", filled = !draft.gentleOn, onClick = { onGentle(false) })
            }
        }
    }
}
```


- [ ] **Step 3: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

This plan changes no schema, so anything a build changed under `shared/schemas` is noise. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (usually 4.json)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/screen/missions/editor/WeekCostView.kt \
  $K/screen/missions/editor/SummaryStep.kt
git commit -m "feat: before-saving screen with the week's cost"
```


---

### Task 7: The nine questions and the editor screen

**Files:**
- Create: `screen/missions/editor/StepContent.kt`
- Create: `screen/missions/editor/PaceStep.kt`
- Create: `screen/missions/editor/MissionEditorScreen.kt`

**Depends on:** Tasks 4–6.

How each question answers:
- **One tap:** most questions (pillar, kind, length, effort, weight, sideways, gentle start, recharge's pace) answer on a tap and move on.
- **Next:** the title and the other paces have more than one control, so they confirm with Next. Next appears only when the step can go on: a title is typed, or a block has at least one day.
- **Shown as picked:** a choice shows as picked only once its question is answered, so a new mission doesn't look pre-filled. Pace controls always show their current value, because Next confirms them.
- **Leaving:** the ✕ and Back ask before dropping anything: words written for a new mission, or a change to an existing one.

- [ ] **Step 1: Create `screen/missions/editor/StepContent.kt`**

```kotlin
package com.example.todoapp.screen.missions.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.EditorStep
import com.example.todoapp.domain.Effort
import com.example.todoapp.domain.IfMissed
import com.example.todoapp.domain.MissionDrafts
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.pillarStrong
import com.example.todoapp.screen.diary.plan.SmallChip
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.check_square
import todoapp.shared.generated.resources.flag_open
import todoapp.shared.generated.resources.heart_filled
import todoapp.shared.generated.resources.metrics_calendar
import todoapp.shared.generated.resources.star_border

/** The question written on the marker heading for [step]. */
fun questionFor(step: EditorStep, state: EditorUiState): String = when (step) {
    EditorStep.TITLE -> "WHAT DO YOU WANT TO MAKE ROOM FOR?"
    EditorStep.PILLAR -> "WHICH PART OF YOUR LIFE DOES IT FEED?"
    EditorStep.KIND -> "WHAT KIND OF THING IS IT?"
    EditorStep.PACE -> when (state.draft.kind) {
        MissionKind.FINISH_LINE -> "BY WHEN? HOW WILL YOU KNOW YOU'RE DONE?"
        MissionKind.BLOCK -> "WHICH DAYS, AND WHEN?"
        MissionKind.RHYTHM -> "EVERY HOW LONG?"
        MissionKind.RECHARGE -> "HOW OFTEN DO YOU NEED IT?"
        else -> "ABOUT HOW OFTEN?"
    }
    EditorStep.DURATION -> "EACH TIME TAKES ABOUT…"
    EditorStep.EFFORT -> "HOW DOES IT FEEL TO DO?"
    EditorStep.WEIGHT -> "HOW BIG A PART OF YOUR ${state.draft.pillar.title.uppercase()} WEEK IS IT?"
    EditorStep.IF_MISSED -> "IF A DAY GOES SIDEWAYS…"
    EditorStep.GENTLE -> "IT'S NEW. START GENTLY?"
}

/** The answers for [step]. A choice shows as picked only once its question has an answer. */
@Composable
fun StepQuestion(step: EditorStep, state: EditorUiState, viewModel: MissionEditorViewModel) {
    val draft = state.draft
    val answered = step in state.answered
    when (step) {
        EditorStep.TITLE -> TitleField(draft.title, viewModel::setTitle, viewModel::confirmTitle)
        EditorStep.PILLAR -> PillarType.scoredPillars.forEachIndexed { i, pillar ->
            ChoiceCard(
                pillar.title, pillarExample(pillar), answered && draft.pillarChoice == pillar, cardTilts[i % cardTilts.size],
                onClick = { viewModel.pickPillar(pillar) }, accent = pillarStrong(pillar)
            )
        }
        EditorStep.KIND -> MissionKind.entries.forEachIndexed { i, kind ->
            val (title, example, icon) = when (kind) {
                MissionKind.HABIT -> Triple("Something I keep doing", "like the gym, or guitar", Res.drawable.check_square)
                MissionKind.FINISH_LINE -> Triple("Something with a finish line", "like a dance routine by spring", Res.drawable.flag_open)
                MissionKind.BLOCK -> Triple("A fixed block in my day", "like your job, on weekdays", Res.drawable.metrics_calendar)
                MissionKind.RHYTHM -> Triple("Something I do every so often", "like calling Grandma, or a trip every 3 months", Res.drawable.star_border)
                MissionKind.RECHARGE -> Triple("Something that recharges me", "like a new episode. No points, never missed.", Res.drawable.heart_filled)
            }
            ChoiceCard(
                title, example, answered && draft.kind == kind, cardTilts[i % cardTilts.size],
                onClick = { viewModel.pickKind(kind) }, icon = icon
            )
        }
        EditorStep.PACE -> PaceStep(state, viewModel)
        EditorStep.DURATION -> DurationChoices(draft.durationMinutes, answered, viewModel::pickDuration)
        EditorStep.EFFORT -> Effort.entries.forEachIndexed { i, effort ->
            val example = when (effort) {
                Effort.LIGHT -> "easy to fit in, even on a tired day"
                Effort.STEADY -> "takes some focus"
                Effort.DRAINING -> "needs a good day. Left out on low-energy days."
            }
            ChoiceCard(
                MissionWords.effortWord(effort).replaceFirstChar { it.uppercase() }, example,
                answered && draft.effort == effort, cardTilts[i % cardTilts.size], onClick = { viewModel.pickEffort(effort) }
            )
        }
        EditorStep.WEIGHT -> (1..3).forEach { weight ->
            ChoiceCard(
                MissionWords.weightWord(weight).replaceFirstChar { it.uppercase() },
                "${MissionWords.pointsPhrase(weight)} each time",
                answered && draft.weight == weight, cardTilts[(weight - 1) % cardTilts.size],
                onClick = { viewModel.pickWeight(weight) }, accent = pillarStrong(draft.pillar)
            )
        }
        EditorStep.IF_MISSED -> IfMissed.entries.forEachIndexed { i, rule ->
            val (title, example) = when (rule) {
                IfMissed.MOVE -> "Move it to another day" to "it goes to the next day with room"
                IfMissed.SAME_DAY -> "It has to be that day" to "if it doesn't happen, it simply drops"
                IfMissed.SKIP -> "Skip it, no catch-up" to "nothing to catch up on"
            }
            ChoiceCard(title, example, answered && draft.ifMissed == rule, cardTilts[i % cardTilts.size], onClick = { viewModel.pickIfMissed(rule) })
        }
        EditorStep.GENTLE -> {
            val start = MissionDrafts.gentleStart(draft.count)
            ChoiceCard(
                "Start at $start× a week", "and step up to ${draft.count}× after 3 good weeks",
                answered && draft.gentle, cardTilts[0], onClick = { viewModel.pickGentle(true) }
            )
            ChoiceCard(
                "Go straight to ${draft.count}× a week", "the full pace from the start",
                answered && !draft.gentle, cardTilts[1], onClick = { viewModel.pickGentle(false) }
            )
        }
    }
}

private fun pillarExample(pillar: PillarType): String = when (pillar) {
    PillarType.HEALTH -> "like the gym, sleep or a walk"
    PillarType.WEALTH -> "like your job, a side project or money"
    PillarType.LOVE -> "like family, friends or a partner"
    PillarType.LIFE -> "like learning, making things or travel"
    PillarType.RECHARGE -> "rest that keeps the rest going"
}

/** Step 1, written on a ruled line. */
@Composable
private fun TitleField(title: String, onChange: (String) -> Unit, onNext: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        value = title,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(
            fontSize = 22.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light,
            letterSpacing = 0.4.sp, color = InkBlack
        ),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        cursorBrush = SolidColor(PeachText),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp).focusRequester(focus),
        decorationBox = { inner ->
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).drawBehind {
                    drawLine(Blue80, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                },
                contentAlignment = Alignment.CenterStart
            ) {
                if (title.isEmpty()) {
                    Text(
                        "Learn guitar", fontSize = 22.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light,
                        color = InkGray.copy(alpha = 0.6f)
                    )
                }
                inner()
            }
        }
    )
}

/** Step 5: the spec's choices, plus a saved length that isn't one of them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationChoices(current: Int, answered: Boolean, onPick: (Int) -> Unit) {
    val standard = MissionDrafts.durationChoices
    val choices = if (current in standard || current > MissionWords.FULL_DAY_MINUTES) standard else (standard + current).sorted()
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        choices.forEach { minutes ->
            SmallChip(
                MissionWords.durationWords(minutes).replaceFirstChar { it.uppercase() },
                selected = answered && minutes == current
            ) { onPick(minutes) }
        }
    }
}
```


- [ ] **Step 2: Create `screen/missions/editor/PaceStep.kt`**

```kotlin
package com.example.todoapp.screen.missions.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.EditorStep
import com.example.todoapp.domain.MissionDraft
import com.example.todoapp.domain.MissionDrafts
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.Period
import com.example.todoapp.domain.shortLabel
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.diary.plan.SmallChip
import kotlinx.datetime.daysUntil

/** Step 4. What it asks depends on the kind (spec section 3). */
@Composable
fun PaceStep(state: EditorUiState, viewModel: MissionEditorViewModel) {
    val draft = state.draft
    when (draft.kind) {
        MissionKind.BLOCK -> BlockPace(draft, viewModel)
        MissionKind.RHYTHM -> RhythmPace(draft, viewModel)
        MissionKind.RECHARGE -> RechargePace(draft, EditorStep.PACE in state.answered, viewModel)
        MissionKind.FINISH_LINE -> {
            CountPace(draft, viewModel, monthly = false)
            FinishPace(draft, viewModel)
        }
        else -> CountPace(draft, viewModel, monthly = true)
    }
}

/** How often, for habits and finish lines, with optional preferred days on a weekly count. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountPace(draft: MissionDraft, viewModel: MissionEditorViewModel, monthly: Boolean) {
    FieldLabel("How often")
    FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        SmallChip("Every day", selected = draft.period == Period.DAY) { viewModel.setCount(1, Period.DAY) }
        (1..6).forEach { n ->
            SmallChip(if (n == 1) "Once a week" else "$n× a week", selected = draft.period == Period.WEEK && draft.count == n) {
                viewModel.setCount(n, Period.WEEK)
            }
        }
        if (monthly) {
            (1..3).forEach { n ->
                SmallChip(if (n == 1) "Once a month" else "$n× a month", selected = draft.period == Period.MONTH && draft.count == n) {
                    viewModel.setCount(n, Period.MONTH)
                }
            }
        }
    }
    if (draft.usesDays) {
        FieldLabel("Any days that suit it best? (optional)")
        DayToggles(draft.weekDays, viewModel::toggleDay)
    }
}

/** By when, and the milestones that show you're done. */
@Composable
private fun FinishPace(draft: MissionDraft, viewModel: MissionEditorViewModel) {
    val end = draft.endDate
    FieldLabel("By when")
    if (end != null) {
        val weeks = (viewModel.today.daysUntil(end) + 3) / 7
        val away = when {
            weeks <= 0 -> "already passed"
            weeks == 1 -> "in 1 week"
            else -> "in $weeks weeks"
        }
        Stepper(
            "Ends", "${end.shortLabel()} · $away",
            onMinus = { viewModel.shiftEndDate(-1) }, onPlus = { viewModel.shiftEndDate(1) }
        )
    }
    FieldLabel("How will you know you're done? (optional)")
    val dates = MissionDrafts.milestoneDates(draft.startDate, draft.endDate, draft.milestones.size)
    draft.milestones.forEachIndexed { i, milestone ->
        MilestoneRow(
            text = milestone.text,
            placeholder = "Milestone ${i + 1}",
            due = dates.getOrNull(i)?.shortLabel().orEmpty(),
            onChange = { viewModel.setMilestone(i, it) },
            onRemove = { viewModel.removeMilestone(i) }
        )
    }
    if (draft.milestones.size < MissionDrafts.MAX_MILESTONES) {
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            SmallChip("+ Add a milestone") { viewModel.addMilestone() }
        }
    }
}

@Composable
private fun MilestoneRow(text: String, placeholder: String, due: String, onChange: (String) -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = text,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 17.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light, color = InkBlack),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            cursorBrush = SolidColor(PeachText),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).drawBehind {
                        drawLine(Blue80, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                    },
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) Text(placeholder, fontSize = 17.sp, fontStyle = FontStyle.Italic, color = InkGray.copy(alpha = 0.6f))
                    inner()
                }
            }
        )
        Text(due, fontSize = 12.sp, color = InkGray, modifier = Modifier.padding(horizontal = 8.dp))
        Box(
            modifier = Modifier.size(44.dp).clickable(role = Role.Button, onClickLabel = "Remove milestone") { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove milestone", tint = InkGray, modifier = Modifier.size(18.dp))
        }
    }
}

/** Which days and from when to when, in half hours. */
@Composable
private fun BlockPace(draft: MissionDraft, viewModel: MissionEditorViewModel) {
    val step = MissionDrafts.CLOCK_STEP
    FieldLabel("Which days")
    DayToggles(draft.weekDays, viewModel::toggleDay)
    FieldLabel("When")
    Stepper(
        "From", MissionWords.clockLabel(draft.startMinute),
        onMinus = { viewModel.shiftStart(-step) }, onPlus = { viewModel.shiftStart(step) },
        minusEnabled = draft.startMinute > 0, plusEnabled = draft.startMinute < MissionDrafts.DAY_MINUTES - step
    )
    Stepper(
        "To", MissionWords.clockLabel(draft.endMinute),
        onMinus = { viewModel.shiftEnd(-step) }, onPlus = { viewModel.shiftEnd(step) },
        minusEnabled = draft.endMinute > draft.startMinute + step, plusEnabled = draft.endMinute < MissionDrafts.DAY_MINUTES
    )
    Text(
        if (draft.weekDays.isEmpty()) "Pick at least one day."
        else "${MissionWords.hoursPhrase(draft.endMinute - draft.startMinute)} each time. Fixed blocks don't use your free hours.",
        fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)
    )
}

/** Once every N days, weeks or months, up to 3 months. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RhythmPace(draft: MissionDraft, viewModel: MissionEditorViewModel) {
    val range = MissionDrafts.intervalRange(draft.intervalUnit)
    val unitName = draft.intervalUnit.name.lowercase()
    Stepper(
        "Every", if (draft.intervalCount == 1) "1 $unitName" else "${draft.intervalCount} ${unitName}s",
        onMinus = { viewModel.setInterval(draft.intervalCount - 1, draft.intervalUnit) },
        onPlus = { viewModel.setInterval(draft.intervalCount + 1, draft.intervalUnit) },
        minusEnabled = draft.intervalCount > range.first, plusEnabled = draft.intervalCount < range.last
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        listOf(Period.DAY to "Days", Period.WEEK to "Weeks", Period.MONTH to "Months").forEach { (unit, label) ->
            SmallChip(label, selected = draft.intervalUnit == unit) { viewModel.setInterval(draft.intervalCount, unit) }
        }
    }
    Text(
        "Up to every 3 months for now.",
        fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)
    )
}

/** About N times a week; one tap answers. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RechargePace(draft: MissionDraft, answered: Boolean, viewModel: MissionEditorViewModel) {
    FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        (1..7).forEach { n ->
            SmallChip(if (n == 1) "About once a week" else "About $n× a week", selected = answered && draft.count == n) {
                viewModel.pickRechargeCount(n)
            }
        }
    }
}
```


- [ ] **Step 3: Create `screen/missions/editor/MissionEditorScreen.kt`**

```kotlin
package com.example.todoapp.screen.missions.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.domain.EditorStep
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.MissionSentence
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.Orange80
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.components.MarkerHeading
import moe.tlaster.precompose.navigation.BackHandler

/** The nine-question sentence and the before-saving screen, for a new or an existing mission. */
@Composable
fun MissionEditorScreen(viewModel: MissionEditorViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val cost by viewModel.cost.collectAsState()
    val finished by viewModel.finished.collectAsState()
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }

    LaunchedEffect(finished) { if (finished) onClose() }

    fun leave() {
        if (viewModel.hasChanges()) confirmLeave = true else onClose()
    }
    BackHandler { if (!viewModel.back()) leave() }

    val s = state
    Column(modifier = Modifier.fillMaxSize().background(Light_Yellowww)) {
        if (s != null) {
            val step = s.step
            EditorHeader(
                title = if (s.isNew) "New mission" else "Edit mission",
                progress = if (step == null) "Ready" else "${s.position} of ${s.steps.size}",
                onClose = { leave() }
            )
            StepProgress(total = s.steps.size, done = s.position)
            // Each question starts at the top.
            val scroll = key(step) { rememberScrollState() }
            Column(modifier = Modifier.weight(1f).verticalScroll(scroll)) {
                if (step == null) {
                    SummaryStep(
                        state = s,
                        cost = cost,
                        onTap = viewModel::goTo,
                        onGentle = viewModel::setGentle,
                        onLighter = viewModel::makeLighter,
                        onSave = viewModel::save,
                        onArchive = { confirmArchive = true },
                    )
                } else {
                    SentenceText(
                        MissionSentence.parts(s.draft, s.answered, step),
                        highlight = sentenceHighlight(s.draft),
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp)
                    )
                    MarkerHeading(questionFor(step, s), Orange80, Modifier.padding(top = 18.dp, bottom = 8.dp))
                    StepQuestion(step, s, viewModel)
                    StepFooter(onBack = { if (!viewModel.back()) leave() }, onNext = nextAction(step, s, viewModel))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    val buttonColors = ButtonDefaults.textButtonColors(contentColor = PeachText)
    val isNew = s?.isNew ?: true
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; onClose() }, colors = buttonColors) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }, colors = buttonColors) {
                    Text(if (isNew) "Keep writing" else "Keep editing")
                }
            },
            title = { Text(if (isNew) "Leave this mission?" else "Leave without saving?") },
            text = { Text(if (isNew) "What you've written so far won't be kept." else "Your changes won't be kept.") },
            containerColor = Light_Yellowww
        )
    }
    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            confirmButton = {
                TextButton(onClick = { confirmArchive = false; viewModel.archive() }, colors = buttonColors) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = { confirmArchive = false }, colors = buttonColors) { Text("Keep it") }
            },
            title = { Text("Archive this mission?") },
            text = {
                Text(
                    "It leaves your missions and the diary, and stops counting toward your week. " +
                        "Anything still planned for it from today on comes off the list. There's no way to bring it back yet."
                )
            },
            containerColor = Light_Yellowww
        )
    }
}

/** Steps with more than one control confirm with Next; the rest answer with a single tap. */
private fun nextAction(step: EditorStep, state: EditorUiState, viewModel: MissionEditorViewModel): (() -> Unit)? = when (step) {
    EditorStep.TITLE -> if (state.draft.title.isNotBlank()) viewModel::confirmTitle else null
    EditorStep.PACE -> when (state.draft.kind) {
        MissionKind.RECHARGE -> null
        MissionKind.BLOCK -> if (state.draft.weekDays.isNotEmpty()) viewModel::confirmPace else null
        else -> viewModel::confirmPace
    }
    else -> null
}
```


- [ ] **Step 4: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

This plan changes no schema, so anything a build changed under `shared/schemas` is noise. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (usually 4.json)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/screen/missions/editor/StepContent.kt \
  $K/screen/missions/editor/PaceStep.kt \
  $K/screen/missions/editor/MissionEditorScreen.kt
git commit -m "feat: the nine questions and the editor screen"
```


---

### Task 8: The Missions screen, free time and wiring

**Files:**
- Create: `screen/missions/MissionsState.kt`, `screen/missions/MissionsViewModel.kt`, `screen/missions/FreeTimeDialog.kt`, `screen/missions/MissionsScreen.kt`
- Modify: `di/DI.kt`, `screen/NavScreens.kt`

**Depends on:** Tasks 1–7.

What the tab shows:
- **Header:** "This week: N points planned · X of Y free hours", and a "Free time · Y h a week" chip that opens the weekday dialog.
- **Sections:** one per pillar under its marker heading ("HEALTH · 14 THIS WEEK"), plus "RECHARGE · NO POINTS". Each holds its missions as tilted sticky notes in two columns: title, cadence ("2× a week, up to 3× · big") and a status line from stored data ("1 of 2 this week", "Due by Thursday", "Milestone 1 of 3", "Fixed block").
- **Empty install:** the sections give way to one sticky note.
- **+** opens the sentence flow. The ideas library is a later pass. A card opens the same flow on its mission.
- **Keys:** the `LazyColumn` items are keyed by pillar and mission id.

- [ ] **Step 1: Create `screen/missions/MissionsState.kt`**

```kotlin
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
```


- [ ] **Step 2: Create `screen/missions/MissionsViewModel.kt`**

```kotlin
package com.example.todoapp.screen.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Which mission the editor is open on; 0 for a new one. */
data class EditorTarget(val missionId: Long)

class MissionsViewModel(
    missionRepository: MissionRepository,
    todayTaskRepository: TodayTaskRepository,
    private val planRepository: PlanRepository,
    milestoneRepository: MilestoneRepository,
) : ViewModel() {

    /** Fixed for this view model's life; the tab makes a new one each time it opens. */
    private val today: LocalDate = MyDate.now().toLocalDate()

    val state: StateFlow<MissionsUiState?> = combine(
        missionRepository.observeActive(),
        todayTaskRepository.observeBetween(
            today.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY),
            today.plus(7, DateTimeUnit.DAY),
        ),
        milestoneRepository.observeAll(),
        planRepository.observeSettings(),
    ) { missions, tasks, milestones, settings ->
        MissionsStateBuilder.build(today, missions, tasks, milestones, settings)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _editor = MutableStateFlow<EditorTarget?>(null)
    /** Non-null while the sentence flow is open. */
    val editor: StateFlow<EditorTarget?> = _editor

    /** A second tap while the editor is opening does nothing. */
    fun openNew() {
        if (_editor.value == null) _editor.value = EditorTarget(0L)
    }

    fun openEdit(missionId: Long) {
        if (_editor.value == null) _editor.value = EditorTarget(missionId)
    }

    fun closeEditor() {
        _editor.value = null
    }

    /** The write itself can't be cancelled (PlanRepository.write), so leaving the tab doesn't lose it. */
    fun saveFreeTime(minutesByWeekday: Map<Int, Int>) {
        viewModelScope.launch { planRepository.saveSettings(minutesByWeekday) }
    }
}
```


- [ ] **Step 3: Create `screen/missions/FreeTimeDialog.kt`**

```kotlin
package com.example.todoapp.screen.missions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.plan.DaySettings
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.minutesLabel
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.PeachText

/** Free time per weekday (DaySettings), in half hours. */
@Composable
fun FreeTimeDialog(minutesByWeekday: Map<Int, Int>, onSave: (Map<Int, Int>) -> Unit, onDismiss: () -> Unit) {
    var minutes by remember { mutableStateOf(minutesByWeekday) }
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = PeachText)
    val step = 30
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(minutes) }, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp)) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp)) { Text("Cancel") }
        },
        title = { Text("Free time per day") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Time outside fixed blocks that the diary may fill. Days already planned keep theirs; change today from the diary.",
                    fontSize = 13.sp, color = InkGray
                )
                (1..7).forEach { day ->
                    val value = minutes[day] ?: DaySettings.defaultFor(day)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(MissionWords.shortDay(day), fontSize = 15.sp, modifier = Modifier.width(44.dp))
                        TextButton(
                            onClick = { minutes = minutes + (day to (value - step).coerceAtLeast(0)) },
                            enabled = value > 0, colors = buttonColors, modifier = Modifier.heightIn(min = 44.dp)
                        ) { Text("−30") }
                        Text(minutesLabel(value), fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { minutes = minutes + (day to (value + step).coerceAtMost(PlanRepository.MAX_FREE_MINUTES)) },
                            enabled = value < PlanRepository.MAX_FREE_MINUTES, colors = buttonColors,
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) { Text("+30") }
                    }
                }
                Text(
                    "${MissionWords.hoursPhrase((1..7).sumOf { minutes[it] ?: DaySettings.defaultFor(it) })} a week",
                    fontSize = 14.sp, color = InkGray, modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        containerColor = Light_Yellowww
    )
}
```


- [ ] **Step 4: Create `screen/missions/MissionsScreen.kt`**

```kotlin
package com.example.todoapp.screen.missions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.Orange80
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.components.MarkerHeading
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarLight
import com.example.todoapp.screen.diary.plan.SmallChip
import com.example.todoapp.screen.diary.plan.StickyNote
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.flag_open
import todoapp.shared.generated.resources.plus_sign

/** Spec 9: missions per pillar under marker headings, as tilted sticky notes. + opens the sentence flow. */
@Composable
fun MissionsScreen(viewModel: MissionsViewModel) {
    val state by viewModel.state.collectAsState()
    var showFreeTime by remember { mutableStateOf(false) }
    val s = state

    Box(modifier = Modifier.fillMaxSize().background(Light_Yellowww)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "title") {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 30.dp)) {
                    Text("Missions", fontSize = 30.sp, fontStyle = FontStyle.Italic, letterSpacing = 0.5.sp, color = InkGray)
                    if (s != null) {
                        Text(s.summary, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, modifier = Modifier.padding(top = 4.dp))
                        SmallChip("Free time · ${MissionWords.hoursNumber(s.weeklyFreeMinutes)} h a week") { showFreeTime = true }
                    }
                }
            }
            if (s != null && s.isEmpty) {
                item(key = "empty") {
                    StickyNote(rotation = -1.2f) {
                        Text(
                            "Nothing here yet. Tap + and write down one thing you want to make room for. " +
                                "Each morning the diary picks from your missions.",
                            fontSize = 16.sp, fontStyle = FontStyle.Italic, lineHeight = 23.sp, color = InkBlack
                        )
                    }
                }
            }
            if (s != null && !s.isEmpty) {
                s.groups.forEach { group ->
                    item(key = "heading-${group.pillar.name}") {
                        MarkerHeading(group.heading, pillarLight(group.pillar), Modifier.padding(top = 24.dp, bottom = 8.dp))
                    }
                    if (group.cards.isEmpty()) {
                        item(key = "none-${group.pillar.name}") {
                            Text(
                                "Nothing here yet.",
                                fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                        }
                    }
                    group.cards.chunked(2).forEach { row ->
                        item(key = "cards-${row.first().missionId}") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                row.forEach { card ->
                                    MissionCard(card, Modifier.weight(1f)) { viewModel.openEdit(card.missionId) }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item(key = "bottom-space") { Spacer(Modifier.height(120.dp)) }
        }
        AddButton(Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 20.dp)) { viewModel.openNew() }
    }

    if (showFreeTime && s != null) {
        FreeTimeDialog(
            minutesByWeekday = s.freeMinutes,
            onSave = {
                showFreeTime = false
                viewModel.saveFreeTime(it)
            },
            onDismiss = { showFreeTime = false }
        )
    }
}

/** A tilted sticky note: pillar band, title, pace and weight, and a status line. */
@Composable
private fun MissionCard(card: MissionCardRow, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier
            .rotate(card.tilt)
            .shadow(6.dp, shape)
            .clip(shape)
            .background(Color.White)
            .clickable(role = Role.Button, onClickLabel = "Edit ${card.title}") { onClick() }
            .heightIn(min = 92.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(pillarLight(card.pillar)))
        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(card.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = InkBlack, modifier = Modifier.weight(1f))
                if (card.finishLine) {
                    Image(
                        painterResource(Res.drawable.flag_open), contentDescription = "Finish line",
                        colorFilter = ColorFilter.tint(pillarInk(card.pillar)), modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(card.cadence, fontSize = 12.sp, lineHeight = 16.sp, color = InkGray, modifier = Modifier.padding(top = 4.dp))
            Text(
                card.status, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = pillarInk(card.pillar),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun AddButton(modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier.size(56.dp).shadow(6.dp, shape).clip(shape).background(Orange80)
            .clickable(role = Role.Button, onClickLabel = "Add a mission") { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(Res.drawable.plus_sign), contentDescription = "Add a mission",
            colorFilter = ColorFilter.tint(PeachText), modifier = Modifier.size(22.dp)
        )
    }
}
```


- [ ] **Step 5: `di/DI.kt`: Import the Missions view model**

Replace:

```kotlin
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.missions.editor.MissionEditorViewModel

```

with:

```kotlin
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.missions.MissionsViewModel
import com.example.todoapp.screen.missions.editor.MissionEditorViewModel

```


- [ ] **Step 6: `di/DI.kt`: Register it**

Replace:

```kotlin
        factory { (missionId: Long) -> MissionEditorViewModel(get(), get(), get(), get(), missionId) }

```

with:

```kotlin
        factory { MissionsViewModel(get(), get(), get(), get()) }
        factory { (missionId: Long) -> MissionEditorViewModel(get(), get(), get(), get(), missionId) }

```


- [ ] **Step 7: `screen/NavScreens.kt`: Add the imports (`ComingNextScreen` stays: Metrics still uses it)**

Replace:

```kotlin
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.placeholder.ComingNextScreen

```

with:

```kotlin
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.missions.MissionsScreen
import com.example.todoapp.screen.missions.MissionsViewModel
import com.example.todoapp.screen.missions.editor.MissionEditorScreen
import com.example.todoapp.screen.missions.editor.MissionEditorViewModel
import com.example.todoapp.screen.placeholder.ComingNextScreen

```


- [ ] **Step 8: `screen/NavScreens.kt`: Replace the Missions placeholder scene. Like the diary's view models, these are made with `remember` and cancelled on dispose; every write they start is non-cancellable inside the repositories**

Replace:

```kotlin
                scene(route = MyScreen.Missions.name) {
                    ComingNextScreen("Missions are being rebuilt around your pillars. They come back in the next step.")
                }

```

with:

```kotlin
                scene(route = MyScreen.Missions.name) {
                    val missionsViewModel = remember { KoinF.di!!.get<MissionsViewModel>() }
                    DisposableEffect(missionsViewModel) {
                        onDispose { missionsViewModel.viewModelScope.cancel() }
                    }
                    val editor by missionsViewModel.editor.collectAsState()
                    val target = editor
                    if (target == null) {
                        MissionsScreen(missionsViewModel)
                    } else {
                        // A fresh editor for each opening; its saves can't be cancelled, so closing mid-save is safe.
                        val editorViewModel = remember(target) {
                            KoinF.di!!.get<MissionEditorViewModel> { parametersOf(target.missionId) }
                        }
                        DisposableEffect(editorViewModel) {
                            onDispose { editorViewModel.viewModelScope.cancel() }
                        }
                        MissionEditorScreen(editorViewModel, onClose = { missionsViewModel.closeEditor() })
                    }
                }

```


- [ ] **Step 9: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10: Commit**

This plan changes no schema, so anything a build changed under `shared/schemas` is noise. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (usually 4.json)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/screen/missions/MissionsState.kt \
  $K/screen/missions/MissionsViewModel.kt \
  $K/screen/missions/FreeTimeDialog.kt \
  $K/screen/missions/MissionsScreen.kt \
  $K/di/DI.kt \
  $K/screen/NavScreens.kt
git commit -m "feat: Missions screen with pillar sections, free time and the editor"
```


---

### Task 9: Empty install: seed off, diary points to Missions

**Files:**
- Modify: `db/SampleData.kt`, `screen/diary/DiaryState.kt`, `screen/diary/plan/PlanSection.kt`, `screen/diary/plan/PlanDialogs.kt`, `screen/diary/DiaryScreen.kt`, `screen/NavScreens.kt`

**Depends on:** Task 8 (`NavScreens` already shows the Missions tab).

With the seed off, a fresh install has no missions:
- The to-do list says so and offers a "Go to Missions" chip.
- The "Picked for you" line and the Swap one / Lighter day / Looks good buttons stay hidden until a mission exists.
- `InitViewModel` still checks `SEED_SAMPLE_MISSIONS`, so flipping the constant back seeds an empty database as before.

- [ ] **Step 1: `db/SampleData.kt`: Turn the seed off**

Replace:

```kotlin
/** Seeds the mockups' example missions while missions can't be created in the app. Plan 3 turns this off. */
const val SEED_SAMPLE_MISSIONS = true
```

with:

```kotlin
/**
 * Seeds the mockups' example missions on an empty database. Off since missions can be created in the app;
 * turn it on to try the diary with history without typing missions in.
 */
const val SEED_SAMPLE_MISSIONS = false
```


- [ ] **Step 2: `screen/diary/DiaryState.kt`: Add `hasMissions` to `DiaryUiState`**

Replace:

```kotlin
    /** Picks done or did-some. */
    val donePicks: Int,
) {
```

with:

```kotlin
    /** Picks done or did-some. */
    val donePicks: Int,
    /** False on an empty install: the list then points to the Missions tab. */
    val hasMissions: Boolean,
) {
```


- [ ] **Step 3: `screen/diary/DiaryState.kt`: Set it in `DiaryStateBuilder.build`**

Replace:

```kotlin
            donePicks = picks.count { it.state.countsAsDone },
        )
```

with:

```kotlin
            donePicks = picks.count { it.state.countsAsDone },
            hasMissions = missions.isNotEmpty(),
        )
```


- [ ] **Step 4: `screen/diary/plan/PlanSection.kt`: Add a callback to `PlanSection`**

Replace:

```kotlin
    actions: PlanRowActions,
    onToggleEvening: () -> Unit,
) {
```

with:

```kotlin
    actions: PlanRowActions,
    onToggleEvening: () -> Unit,
    onOpenMissions: () -> Unit,
) {
```


- [ ] **Step 5: `screen/diary/plan/PlanSection.kt`: Point an empty install to the Missions tab, and show the crown hint only when there's something to crown**

Replace:

```kotlin
        if (state.picks.isEmpty()) {
            Text(
                if (isToday) "Nothing picked yet. Tap the pencil to add something." else "Nothing was planned for this day.",
                fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }
        state.picks.forEach { PlanRowItem(it, evening, editMode, actions) }
        if (isToday && !evening) {
```

with:

```kotlin
        if (state.picks.isEmpty()) {
            Text(
                when {
                    !state.hasMissions -> "No missions yet. Add a few on the Missions tab, and each morning the diary picks from them."
                    isToday -> "Nothing picked yet. Tap the pencil to add something."
                    else -> "Nothing was planned for this day."
                },
                fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            if (!state.hasMissions) SmallChip("Go to Missions") { onOpenMissions() }
        }
        state.picks.forEach { PlanRowItem(it, evening, editMode, actions) }
        if (isToday && !evening && state.picks.isNotEmpty()) {
```


- [ ] **Step 6: `screen/diary/plan/PlanDialogs.kt`: Make the pencil's empty message honest on an empty install**

Replace:

```kotlin
            if (available.isEmpty()) {
                Text("Everything is already on today's list.")
```

with:

```kotlin
            if (available.isEmpty()) {
                Text(if (missions.isEmpty()) "No missions yet. Add some on the Missions tab." else "Everything is already on today's list.")
```


- [ ] **Step 7: `screen/diary/DiaryScreen.kt`: Add the parameter**

Replace:

```kotlin
    selection: LocalDate,
    changeDate: (LocalDate) -> Unit
) {
```

with:

```kotlin
    selection: LocalDate,
    changeDate: (LocalDate) -> Unit,
    openMissions: () -> Unit,
) {
```


- [ ] **Step 8: `screen/diary/DiaryScreen.kt`: Hide "Picked for you · … free" when there's nothing to pick from**

Replace:

```kotlin
                if (s.hasPlan && diaryViewModel.isToday && !evening) {
                    item(key = "plan-subline") { PlanSubline(s, onChange = { showCapacity = true }) }
                }
```

with:

```kotlin
                if (s.hasPlan && s.hasMissions && diaryViewModel.isToday && !evening) {
                    item(key = "plan-subline") { PlanSubline(s, onChange = { showCapacity = true }) }
                }
```


- [ ] **Step 9: `screen/diary/DiaryScreen.kt`: Pass the callback to `PlanSection`**

Replace:

```kotlin
                        onToggleEvening = {
                            editMode = false
                            diaryViewModel.toggleEveningCheck()
                        }
                    )
```

with:

```kotlin
                        onToggleEvening = {
                            editMode = false
                            diaryViewModel.toggleEveningCheck()
                        },
                        onOpenMissions = openMissions
                    )
```


- [ ] **Step 10: `screen/diary/DiaryScreen.kt`: Hide Swap one, Lighter day and Looks good when there are no missions**

Replace:

```kotlin
                if (diaryViewModel.isToday && !evening) {
                    item(key = "plan-actions") {
```

with:

```kotlin
                if (diaryViewModel.isToday && !evening && s.hasMissions) {
                    item(key = "plan-actions") {
```


- [ ] **Step 11: `screen/NavScreens.kt`: Let the diary open the Missions tab**

Replace:

```kotlin
                        currentDate,
                        changeDate = { date -> calDateViewModel.selectDate(date) }
                    )
```

with:

```kotlin
                        currentDate,
                        changeDate = { date -> calDateViewModel.selectDate(date) },
                        openMissions = { navigateTab(MyScreen.Missions) }
                    )
```


- [ ] **Step 12: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 13: Compile for iOS**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :shared:compileKotlinIosSimulatorArm64 --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 14: Commit**

This plan changes no schema, so anything a build changed under `shared/schemas` is noise. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (usually 4.json)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/db/SampleData.kt \
  $K/screen/diary/DiaryState.kt \
  $K/screen/diary/plan/PlanSection.kt \
  $K/screen/diary/plan/PlanDialogs.kt \
  $K/screen/diary/DiaryScreen.kt \
  $K/screen/NavScreens.kt
git commit -m "feat: turn off sample missions; an empty diary points to Missions"
```


- [ ] **Step 15: Hand over for manual testing**

Tell the owner the build is ready and point them to the checklist below.

---

## Manual hand-test checklist

**Before you start.** The seed is now off. An install that still has Plan 1's sample missions keeps them; they open and edit like any other mission. To see an empty install, clear the app's storage (Android: App info → Storage → Clear storage) or delete the app from the iOS simulator. Free time starts at the defaults: 3 h on weekdays, 10 h at the weekend, 35 h a week.

**About the numbers.** Counts assume Monday to Friday. Near the end of the week fewer days are left, so this week's numbers shrink. The plain-words line then adds "This week it's N×.", or "It doesn't come up this week." for a block on a weekend.

**When to run it.**
- **Best on a weekday morning, before 18:00.** Steps A.2, B.7–B.12, C.5–C.6, F and I assume a Monday.
- **After 18:00 the Diary opens on the evening check.** The label reads TODAY, and there's no "Picked for you" line, no Swap one, no Lighter day and no Looks good. Tap "Back to the plan" to get the morning view. You'll need to tap it again each time you return to the Diary.
- **On a Saturday or Sunday:** today's free time is 10 h, a Mon–Fri block doesn't come up, and weekly habits count only the days left (once, on a Sunday).
- **Don't rotate the phone mid-edit.** Rotating recreates the screen and drops an unsaved draft.

### A. Empty install
1. Diary → TO DO LIST says "No missions yet. Add a few on the Missions tab, and each morning the diary picks from them." with a "Go to Missions" chip.
2. There's no "Picked for you · …" line, no Swap one / Lighter day / Looks good, and no "Hold a title to crown it." The pillar strip reads 0/0. (After 18:00, tap "Back to the plan" first.)
3. The pencil says "No missions yet. Add some on the Missions tab."
4. "Go to Missions" switches to the Missions tab. It shows "Missions", "This week: 0 points planned · 0 of 35 free hours", a "Free time · 35 h a week" chip, and a sticky note starting "Nothing here yet. Tap +". No pillar sections yet. A peach + button sits bottom right.

### B. A habit with a gentle start
1. Tap +. Check the header and the opening state:
   - The header reads ✕ / "New mission" / "1 of 9", with 1 of 9 segments filled.
   - The sentence "I want to" ends in a dashed blank on blue ruled lines.
   - The keyboard opens on a ruled line with the placeholder "Learn guitar". "Next" appears only once something is typed.
2. Type "Learn guitar" and tap Next (or the keyboard's Next).
3. "WHICH PART OF YOUR LIFE DOES IT FEED?" shows four tilted cards with color stripes, and nothing is picked yet. Tap Life. The sentence reads "I want to Learn guitar for my Life. It's ___" with the answers in light blue (yellow before the pillar was picked). The header reads "3 of 9".
4. "WHAT KIND OF THING IS IT?" shows five cards with icons and examples. The every-so-often card shows a star, not a wand. Tap "Something I keep doing".
5. "ABOUT HOW OFTEN?" shows chips from "Every day" to "6× a week", then "Once a month" to "3× a month", with "3× a week" selected. Below them, "Any days that suit it best? (optional)" with Mon to Sun circles. Tap Next.
6. Answer the next questions:
   - "EACH TIME TAKES ABOUT…": tap "30 min".
   - "HOW DOES IT FEEL TO DO?": tap Steady.
   - "HOW BIG A PART OF YOUR LIFE WEEK IS IT?": tap Big ("3 points each time").
   - "IF A DAY GOES SIDEWAYS…": tap "Move it to another day".
   - "IT'S NEW. START GENTLY?": tap "Start at 2× a week".
7. The before-saving screen. Check:
   - The header reads "Ready" and every segment is filled.
   - The sentence: "I want to Learn guitar for my Life. It's something I keep doing, about 3 times a week, 30 min each, and it feels steady. It's a big part of my Life week. If a day goes sideways, move it."
   - "Tap any highlighted words to change them." sits under it.
   - The sticky "IT'S NEW. START GENTLY?" with a wand reads "2× a week to begin, then 3× after 3 good weeks.", and "Start gently" is filled.
   - "YOUR WEEK": the Life bar reads "0 → 6" with a striped segment; the other pillars read 0.
   - The line: "Adds 1 hour and 6 points a week while you start gently. That's 9 points once you're at 3×."
   - "Free hours planned 1 of 35" with a thin stripe, then "34 hours to spare. Plenty of room."
8. Tap "Go straight to 3×". The sticky now reads "Straight to 3× a week from the start." The line becomes "Adds 1½ hours and 9 points a week.", and Life reads "0 → 9". Tap "Start gently" again.
9. Tap "Make it lighter". The pace question opens, saying "4 of 9". Pick "2× a week" and tap Next. You're back on the summary, which now shows:
   - the sticky: "1× a week to begin, then 2× after 3 good weeks."
   - the line: "Adds 30 min and 3 points a week while you start gently. That's 6 points once you're at 2×."
10. Tap the highlighted "about twice a week" and pick "3× a week", then tap Next. You land straight back on the summary. Press the Android back button on any question opened from the summary: it returns to the summary.
11. Tap "Save mission". You're back on the list. Check:
    - The header reads "This week: 6 points planned · 1 of 35 free hours".
    - The sections read "HEALTH · 0 THIS WEEK", "WEALTH · 0 THIS WEEK" and "LOVE · 0 THIS WEEK", each with "Nothing here yet.", then "LIFE · 6 THIS WEEK" and "RECHARGE · NO POINTS".
    - A tilted card "Learn guitar" reads "2× a week, up to 3× · big" and "0 of 2 this week".
12. On the Diary, before tapping Looks good, "Learn guitar" is under PICKED FOR TODAY. It reads "0 of 2 this week · N days left", with "30 min" and "+3". "Picked for you · 30 min of your 3 h free" and the morning buttons are back.

### C. A fixed block
1. Tap +, type "Work", and pick Wealth, then "A fixed block in my day". The pace question says "4 of 6": a block skips the length and the "sideways" questions.
2. "WHICH DAYS, AND WHEN?" shows Mon to Fri picked, "From 9:00" and "To 12:00", with "3 hours each time. Fixed blocks don't use your free hours."
3. Untick all five days. Next disappears and the line reads "Pick at least one day." Tick them again.
4. Raise "To" to 13:00. The line now says "4 hours each time". Check the limits:
   - "−" on To stops half an hour after From.
   - Moving From past To pushes To along.
5. Tap Next. Effort is "5 of 6" (pick Steady) and weight is "6 of 6" (pick Big). Then the summary:
   - The sentence: "I want to Work for my Wealth. It's a fixed block in my day, on Mon to Fri, 9:00 to 13:00, and it feels steady. It's a big part of my Wealth week."
   - The line: "Adds 15 points a week. Fixed blocks use their own hours, not your free time." (on a Monday).
   - The free-hours bar gains no stripe.
6. Save. The card reads "Mon to Fri, 9:00 to 13:00 · big" and "Fixed block". On a weekday the diary lists "Work" under ALREADY IN YOUR DAY, reading "9:00 to 13:00".

### D. Every so often, capped at 3 months
1. Tap +, type "Call Grandma", and pick Love, then "Something I do every so often". "EVERY HOW LONG?" shows "Every 2 weeks" with Days / Weeks / Months chips and "Up to every 3 months for now."
2. Test the limits:
   - Tap Months: it reads "2 months". "+" gives "3 months", then turns grey.
   - Tap Days: it reads "3 days", and "−" stops at "2 days".
   - Weeks go from 1 to 12.
3. Set "2 weeks" and tap Next. Then pick "15 min", Light, Solid and "Move it to another day". There's no gentle-start question.
4. The summary line reads "Adds 15 min and 2 points every 2 weeks.", and Love reads "0 → 2". Save.
5. The card reads "Every 2 weeks · solid" and "Due today". On the diary it reads "First one · you aim for every 2 weeks".

### E. Recharge
1. Tap +, type "Watch a new episode", and pick any pillar, then "Something that recharges me". The pace question says "3 of 5", and the sentence drops "for my …".
2. "HOW OFTEN DO YOU NEED IT?" shows chips from "About once a week" to "About 7× a week". Tap "About 2× a week" and it moves on. Pick "45 min", then Light.
3. The summary sentence reads "I want to Watch a new episode. It's something that recharges me, about twice a week, 45 min each, and it feels light." with grey highlights.
4. The line reads "Adds about 1½ hours of free time a week. No points, and it never counts as missed." The pillar bars don't change, and the free-hours bar gains a grey stripe.
5. Save. Under "RECHARGE · NO POINTS" the card reads "About twice a week" and "Never counts as missed".

### F. A finish line with milestones
1. Tap +, type "Learn a dance routine", and pick Life, then "Something with a finish line".
2. "BY WHEN? HOW WILL YOU KNOW YOU'RE DONE?" shows:
   - how-often chips with no monthly ones, and "2× a week" selected
   - optional days
   - "Ends ‹date 8 weeks out› · in 8 weeks", with − and +
3. Check the end-date limits: "−" stops at "in 1 week" and "+" stops at 52 weeks. Leave it at 8 weeks.
4. Tap "+ Add a milestone" three times. Rows read "Milestone 1" to "Milestone 3", each with a date, and the last date is the end date. Type three short milestones.
5. Check the milestone controls: a fifth row hides the chip, and × removes a row.
6. Tap Next. Then pick "1 h", Draining, Big, "It has to be that day", and "Go straight to 2× a week".
7. The sentence reads "…It's something with a finish line, about twice a week, until ‹date›, with 3 milestones, 1 h each, and it feels draining. It's a big part of my Life week. If a day goes sideways, it has to be that day."
8. Save. The card shows a flag and reads "2× a week · big" and "Milestone 1 of 3".

### G. Editing and archiving
1. Tap the "Learn guitar" card. It opens on the summary, with:
   - the header "Edit mission" / "Ready"
   - the sticky headed "START GENTLY?"
   - an underlined "Archive this mission" at the bottom
2. Tap "30 min". The question opens ("5 of 9", with 30 min picked). Tap "45 min" and you're back on the summary:
   - The line: "Comes to 1½ hours and 6 points a week while you start gently. That's 9 points once you're at 3×."
   - Life still reads "6", and the free-hours bar has a stripe for the extra half hour.
3. Tap ✕. The dialog asks "Leave without saving?" / "Your changes won't be kept.". Keep editing stays; Leave closes, and the card is unchanged.
4. Open the dance card and tap "with 3 milestones". Remove the second milestone, rename the first, tap Next, then Save. The card reads "Milestone 1 of 2", and reopening shows the two milestones with the new name.
5. Open "Call Grandma" and tap "Archive this mission". The dialog reads "Archive this mission?" with Archive and Keep it, and Keep it does nothing. Archive closes the editor, the card is gone, LOVE reads "0 THIS WEEK" with "Nothing here yet.", and the diary no longer lists Call Grandma.
6. Start a new mission, type a title, and press back on the first question. The dialog asks "Leave this mission?" with Leave and Keep writing. On any later question, back goes to the previous one.

### H. Free time per weekday
1. Tap "Free time · 35 h a week". "Free time per day" lists Mon to Sun with −30 / +30 and "35 hours a week". "−30" turns grey at 0 min, and "+30" at 16 h.
2. Lower Saturday to 8 h. The total reads "33 hours a week". Save. The chip reads "Free time · 33 h a week", and the header ends "of 33 free hours". The summary's free-hours line uses 33 too.
3. Today's diary keeps its free time ("change" still adjusts today). Cancel in the dialog changes nothing.

### I. A confirmed day is left alone
1. On the diary, tap Looks good. Then create another mission.
2. It isn't added to today's list (the pencil can add it). It's picked from tomorrow.
3. Before Looks good, a save re-picks today's open items. Done, crowned, moved-in and added-by-hand items stay.

### J. Look and feel
- The sentence is in light italic on blue ruled lines, with marker highlights in the pillar's light color and a dashed blank for the question being asked.
- Next, Save mission and the filled sticky button are peach with dark brown text; Make it lighter is outlined.
- Every tap target is at least 44dp: cards, chips, day circles, steppers, Back, ✕, the archive link and the + button.
- Switching to the Diary tab mid-flow and back shows the list, and the unsaved draft is gone (decision 12).

### K. iOS
`:shared:compileKotlinIosSimulatorArm64` passes. If you run the simulator, there's no system back gesture for the flow. Use Back and ✕.

---

## What comes next (other plans)

- **Plan 2, week cycle:**
  - Pace offers step a gentle start up from `count` to `goalCount` after 3 good weeks. The cards already say "up to 3×", and the editor keeps a gentle start's current pace while its goal is unchanged.
  - "Pause a month" (spec 5.2) writes `pausedUntil`. The editor carries it through saves and the cards show "Paused until …", but there's no pause control yet.
  - Midnight resolution follows "if a day goes sideways". Blocks are saved as `SAME_DAY` and recharge as `SKIP`.
  - The Room 6 bump for `WeekSummary` and `PaceOffer`. Plan 3 changed no schema, so it doesn't affect that migration.
- **Plan 4, metrics:**
  - Decide how archived missions count in history. The diary and the week bars read active missions only, so archiving hides a mission's past ticks.
  - Metrics may want archived points kept, and maybe an archived list with a restore.
- **Next pass, ideas library:**
  - + should open the library (spec 4 and 8); today it opens the sentence directly. `MissionDrafts.from` already turns a mission into a pre-filled draft, which the review screen (8.4) can build on.
  - `ideas.json` has 6 rhythms longer than 3 months (6, 12 and 24 months). Cap them at 3 months when importing, as the editor does, or first widen `PlanRepository.HISTORY_DAYS` past the longest interval.
  - Its block times are `"09:00"`/`"17:00"`. `MissionWords.parseClock` reads them, and a save rewrites them as `"9:00"`/`"17:00"`.
  - `ideaId` on `Mission` is a schema change, so it comes after Plan 2's version 6.
- **Small follow-ups, not scheduled:**
  - The Missions tab's "today" stays fixed while the tab is open across midnight.
  - Free-time changes skip days already planned.
  - A mission saved after Looks good waits for tomorrow or the pencil (decision 8).
  - The spec's section 9 still says + "opens the library", and section 4 doesn't record decisions 1–7. Update the spec when the library lands.
