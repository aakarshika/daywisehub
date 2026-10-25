# Plan 1: Foundation and the Daily List Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the old mission and task model with the pillar-and-points model, and make the diary build each day's list on its own: fixed blocks, picks with reason lines, "didn't fit", swap, lighter day, crowns, and the evening options.

**Architecture:** The database is wiped and rebuilt at version 5. Mission absorbs its old frequency and pillar-mapping tables. The rules (points, targets, picking) live in a pure-Kotlin `domain` package. `PlanRepository` runs those rules against Room and writes today's tasks. `DiaryViewModel` combines missions, tasks and the day plan into one `DiaryUiState` that the diary screen draws. Missions and Metrics tabs show a placeholder until Plans 3 and 4; sample missions from the mockups are seeded so the diary can be tried by hand.

**Tech Stack:** Kotlin Multiplatform (Kotlin 2.0.20, language level 1.9), Compose Multiplatform 1.7.1, Room KMP 2.7.0-alpha01 with KSP, Koin 3.2, precompose navigation, kotlinx-datetime, Kottie.

**Spec:** `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`

**Ground rules for this plan**
- **No automated tests.** The owner tests by hand. Each checkpoint is a successful build: `./gradlew :androidApp:assembleDebug` from the repo root, expected `BUILD SUCCESSFUL`.
- **Tasks 2 to 5 change the schema and remove the screens that depend on it,** so the project only compiles again at the end of Task 5. After that, every task ends with a green build.
- **Paths are relative to `shared/src/commonMain/kotlin/com/example/todoapp/`** unless they start with `shared/` or `docs/`.
- **Existing data is not kept.** Bumping the database to version 5 wipes it through `fallbackToDestructiveMigration(true)`, which is already set.

---

## File map

**Created**

| File | Responsibility |
|---|---|
| `domain/Types.kt` | Enums: `PillarType`, `MissionKind`, `Effort`, `IfMissed`, `Period`, `TaskState`, `Energy` |
| `domain/DateLabels.kt` | Weekday names, "Dec 20" labels, "2½ h" durations |
| `domain/WeekMath.kt` | Monday-based week start and end, days left |
| `domain/MissionRules.kt` | Typed accessors on `Mission`, intervals, live/scheduled checks |
| `domain/History.kt` | Last done, done counts, streaks from past tasks |
| `domain/Points.kt` | Points per state, weekly occurrences, pillar targets and earned points |
| `domain/ReasonLines.kt` | Reason codes, encoding, rendering to text |
| `domain/Planner.kt` | Builds a day: fixed blocks, ranked candidates, fitting, didn't-fit, swap |
| `db/data/plan/DayPlan.kt`, `DaySettings.kt`, `PlanDao.kt` | Per-day plan state and free-time defaults |
| `db/SampleData.kt` | Seeds the mockups' example missions and a little history |
| `repo/PlanRepository.kt` | Runs the planner, changes task states, moves, swaps, crowns |
| `screen/basicutils/Ordinal.kt` | `getOrdinal`, moved out of the deleted mission editor |
| `screen/placeholder/ComingNextScreen.kt` | Missions and Metrics placeholder |
| `screen/diary/DiaryState.kt` | `DiaryUiState` and the builder that derives it |
| `screen/diary/plan/PillarWeekStrip.kt` | "This week" bars per pillar |
| `screen/diary/plan/PlanSection.kt` | To-do rows, evening choices, chips, subline |
| `screen/diary/plan/PlanNotes.kt` | Didn't-fit sticky, morning buttons, highlights card |
| `screen/diary/plan/PlanDialogs.kt` | "Your day" dialog, add-to-today dialog |

**Rewritten:** `db/data/mission/Mission.kt`, `db/data/mission/MissionDao.kt`, `db/data/todotask/TodayTask.kt`, `db/data/todotask/TodayTaskDao.kt`, `db/data/mission/milestone/MilestoneDao.kt`, `db/data/mission/milestone/MilestoneWithDetails.kt`, `db/Database.kt`, `db/models/TaskStatus.kt`, `repo/MissionRepository.kt`, `repo/TodayTaskRepository.kt`, `repo/MilestoneRepository.kt`, `di/DI.kt`, `screen/NavScreens.kt`, `screen/init/InitViewModel.kt`, `screen/metrics/CalDiaryViewModel.kt`, `screen/diary/DiaryScreenList.kt`, `screen/diary/DiaryViewModel.kt`, `screen/diary/DiaryScreen.kt`.

**Edited:** `screen/basicutils/AppColors.kt`, `screen/diary/diaryitem/components/DiaryMilestoneSection.kt`, `screen/diary/diaryitem/components/NotesItem.kt`, `screen/diary/diaryitem/components/dateheader/DateHeader.kt`, `db/data/mood/TodayMoodDao.kt`, `db/data/mood/TodayMoodWithDetails.kt`, `shared/src/androidMain/kotlin/com/example/todoapp/db/DB.kt`, `shared/src/iosMain/kotlin/com/example/todoapp/db/DB.kt`.

**Deleted:** the old mission editor, planner, metrics screens, per-item diary widgets, and the pillar, frequency, reminder and sub-task tables (Task 2 lists every path).

---

### Task 1: Branch and park the in-progress frequency work

**Files:** none changed; git only.

- [ ] **Step 1: Stash the uncommitted frequency work (keeps `docs/` out of the stash)**

Run from the repo root:
```bash
git stash push -u -m "frequency WIP parked before redesign" -- shared flag.png flag-open.png
```
Expected: `Saved working directory and index state On develop: frequency WIP parked before redesign`

- [ ] **Step 2: Create the branch**

```bash
git switch -c redesign/plan-1-daily-list
git status --short
```
Expected: only `?? docs/` is listed.

- [ ] **Step 3: Commit the spec and this plan**

```bash
git add docs
git commit -m "docs: redesign spec and plan 1"
```

---

### Task 2: Remove the old screens and fix stray imports

**Files:** deletions listed below; create `screen/basicutils/Ordinal.kt`; edit `NotesItem.kt`, `TodayMoodDao.kt`, `TodayMoodWithDetails.kt`, `DateHeader.kt`.

- [ ] **Step 1: Delete the files that belong to the old model**

```bash
cd shared/src/commonMain/kotlin/com/example/todoapp
rm -rf screen/missions screen/diary/planning screen/metrics/bottomhighlights screen/metrics/metriccomponents
rm -f screen/metrics/MetricsScreen.kt screen/metrics/MetricsViewModel.kt
rm -f screen/diary/diaryitem/DiaryItem.kt screen/diary/diaryitem/HabitItemViewModel.kt
rm -f screen/diary/diaryitem/components/GeneralItem.kt screen/diary/diaryitem/components/GeneralItemCheckbox.kt \
      screen/diary/diaryitem/components/GeneralItemHabit.kt screen/diary/diaryitem/components/GeneralItemTop3Star.kt \
      screen/diary/diaryitem/components/GeneralItemTopButton.kt screen/diary/diaryitem/components/ItemProgressBar.kt \
      screen/diary/diaryitem/components/ItemStrikethrough.kt screen/diary/diaryitem/components/MenuItem.kt
rm -f screen/basicutils/components/FrequencyPeriodButton.kt screen/basicutils/components/MetricCalendarDayCell.kt \
      screen/basicutils/components/MissionCalendarDayCell.kt
rm -rf db/data/pillar db/data/mission/missionstuff
rm -f db/data/mission/MissionWithDetails.kt db/data/mission/MissionWithFewDetails.kt \
      db/data/todotask/TodayTaskWithDetails.kt db/data/todotask/TodayTaskReminder.kt db/data/todotask/SubTask.kt
cd -
```

- [ ] **Step 2: Move `getOrdinal` to a shared place**

Create `screen/basicutils/Ordinal.kt`:
```kotlin
package com.example.todoapp.screen.basicutils

/** 1 → "1st", 2 → "2nd", 11 → "11th", 23 → "23rd". */
fun getOrdinal(index: Int): String {
    return when {
        index % 100 in 11..13 -> "${index}th"
        index % 10 == 1 -> "${index}st"
        index % 10 == 2 -> "${index}nd"
        index % 10 == 3 -> "${index}rd"
        else -> "${index}th"
    }
}
```

In `screen/diary/diaryitem/components/dateheader/DateHeader.kt`, replace
```kotlin
import com.example.todoapp.screen.missions.editpopup.getOrdinal
```
with
```kotlin
import com.example.todoapp.screen.basicutils.getOrdinal
```

- [ ] **Step 3: Drop imports of deleted classes**

In `screen/diary/diaryitem/components/NotesItem.kt`, delete the line:
```kotlin
import com.example.todoapp.db.data.mission.Mission
```

In `db/data/mood/TodayMoodDao.kt`, delete these three lines:
```kotlin
import com.example.todoapp.db.data.todotask.SubTask
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskReminder
```

In `db/data/mood/TodayMoodWithDetails.kt`, delete these five lines:
```kotlin
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskReminder
```

No build yet; it compiles again at the end of Task 5.

---

### Task 3: Domain types and date helpers

**Files:** create `domain/Types.kt`, `domain/DateLabels.kt`, `domain/WeekMath.kt`.

- [ ] **Step 1: Create `domain/Types.kt`**

```kotlin
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
    val isOpen: Boolean get() = this != MOVED && this != SKIPPED
}

enum class Energy(val label: String) { LOW("Low"), NORMAL("Normal"), HIGH("High") }
```

- [ ] **Step 2: Create `domain/DateLabels.kt`**

```kotlin
package com.example.todoapp.domain

import kotlinx.datetime.LocalDate

private val shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "Monday" */
fun LocalDate.weekdayName(): String = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

/** "Dec 20" */
fun LocalDate.shortLabel(): String = "${shortMonths[monthNumber - 1]} $dayOfMonth"

/** 45 → "45 min", 90 → "1½ h", 120 → "2 h", 135 → "2 h 15 min" */
fun minutesLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    minutes % 60 == 30 -> "${minutes / 60}½ h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
```

- [ ] **Step 3: Create `domain/WeekMath.kt`**

```kotlin
package com.example.todoapp.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Weeks run Monday to Sunday. */
object WeekMath {
    fun weekStart(date: LocalDate): LocalDate = date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    fun weekEnd(date: LocalDate): LocalDate = weekStart(date).plus(6, DateTimeUnit.DAY)

    /** Monday → 7, Sunday → 1. */
    fun daysLeftIncludingToday(date: LocalDate): Int = 8 - date.dayOfWeek.isoDayNumber
}
```

---

### Task 4: New schema, DAOs and database version 5

**Files:** rewrite `db/data/mission/Mission.kt`, `db/data/mission/MissionDao.kt`, `db/data/todotask/TodayTask.kt`, `db/data/todotask/TodayTaskDao.kt`, `db/data/mission/milestone/MilestoneWithDetails.kt`, `db/data/mission/milestone/MilestoneDao.kt`, `db/Database.kt`, `db/models/TaskStatus.kt`; create `db/data/plan/DayPlan.kt`, `db/data/plan/DaySettings.kt`, `db/data/plan/PlanDao.kt`; edit both platform `DB.kt` files.

- [ ] **Step 1: Rewrite `db/data/mission/Mission.kt`**

```kotlin
package com.example.todoapp.db.data.mission

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.todoapp.db.models.MyDate

/**
 * One thing to make room for. Enum-backed columns store the enum name; read them through
 * the typed accessors in domain/MissionRules.kt.
 *
 * How it repeats depends on [kind]:
 * - HABIT, FINISH_LINE, RECHARGE: [count] per [period] (DAY means every day). [goalCount] is the pace a gentle start steps up to.
 * - RHYTHM: once every [intervalCount] [intervalUnit].
 * - BLOCK: on [weekDays] (ISO numbers, Monday = 1) from [startTime] to [endTime].
 * On habits, [weekDays] holds optional preferred days.
 */
@Entity(tableName = "mission")
data class Mission(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "mission_id") val missionId: Long = 0,
    @ColumnInfo(name = "mission_title") val title: String,
    @ColumnInfo(name = "mission_pillar") val pillar: String,
    @ColumnInfo(name = "mission_kind") val kind: String,
    @ColumnInfo(name = "mission_weight") val weight: Int,
    @ColumnInfo(name = "mission_minutes") val durationMinutes: Int,
    @ColumnInfo(name = "mission_effort") val effort: String,
    @ColumnInfo(name = "mission_if_missed") val ifMissed: String,
    @ColumnInfo(name = "mission_count") val count: Int = 1,
    @ColumnInfo(name = "mission_period") val period: String = "WEEK",
    @ColumnInfo(name = "mission_goal_count") val goalCount: Int? = null,
    @ColumnInfo(name = "mission_interval_count") val intervalCount: Int = 0,
    @ColumnInfo(name = "mission_interval_unit") val intervalUnit: String = "WEEK",
    @ColumnInfo(name = "mission_week_days") val weekDays: String = "",
    @ColumnInfo(name = "mission_start_time") val startTime: String? = null,
    @ColumnInfo(name = "mission_end_time") val endTime: String? = null,
    @ColumnInfo(name = "mission_start_date") val startDate: MyDate,
    @ColumnInfo(name = "mission_end_date") val endDate: MyDate? = null,
    @ColumnInfo(name = "mission_paused_until") val pausedUntil: MyDate? = null,
    @ColumnInfo(name = "mission_motivation") val motivation: String? = null,
    @ColumnInfo(name = "mission_active") val active: Boolean = true,
)
```

- [ ] **Step 2: Rewrite `db/data/mission/MissionDao.kt`**

```kotlin
package com.example.todoapp.db.data.mission

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {

    /** Upsert, not REPLACE: REPLACE would delete the row and cascade-delete its tasks. */
    @Upsert
    suspend fun upsert(mission: Mission): Long

    @Query("SELECT * FROM mission WHERE mission_active = 1 ORDER BY mission_id")
    fun observeActive(): Flow<List<Mission>>

    @Query("SELECT * FROM mission WHERE mission_active = 1 ORDER BY mission_id")
    suspend fun getActive(): List<Mission>

    @Query("SELECT * FROM mission WHERE mission_id = :missionId")
    suspend fun get(missionId: Long): Mission?

    @Query("SELECT COUNT(*) FROM mission")
    suspend fun count(): Int
}
```

- [ ] **Step 3: Rewrite `db/data/todotask/TodayTask.kt`**

```kotlin
package com.example.todoapp.db.data.todotask

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.models.MyDate

/**
 * One mission on one day. [state] stores a TaskState name. [reasonCode] and [reasonArgs]
 * hold the reason line (domain/ReasonLines.kt), so it can be redrawn later.
 */
@Entity(
    tableName = "today_task",
    foreignKeys = [ForeignKey(
        entity = Mission::class,
        parentColumns = ["mission_id"],
        childColumns = ["task_mission_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["task_mission_id"]), Index(value = ["task_date"])]
)
data class TodayTask(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "today_task_id") val todayTaskId: Long = 0,
    @ColumnInfo(name = "task_mission_id") val missionId: Long,
    @ColumnInfo(name = "task_date") val taskDate: MyDate,
    @ColumnInfo(name = "task_state") val state: String = "PLANNED",
    @ColumnInfo(name = "task_points") val pointsEarned: Int = 0,
    @ColumnInfo(name = "task_fixed_block") val isFixedBlock: Boolean = false,
    @ColumnInfo(name = "task_crowned") val crowned: Boolean = false,
    @ColumnInfo(name = "task_moved_from") val movedFromDate: MyDate? = null,
    @ColumnInfo(name = "task_reason_code") val reasonCode: String,
    @ColumnInfo(name = "task_reason_args") val reasonArgs: String = "",
    @ColumnInfo(name = "task_order") val sortOrder: Int = 0,
)
```

- [ ] **Step 4: Rewrite `db/data/todotask/TodayTaskDao.kt`**

```kotlin
package com.example.todoapp.db.data.todotask

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodayTaskDao {

    @Insert
    suspend fun insert(task: TodayTask): Long

    @Insert
    suspend fun insertAll(tasks: List<TodayTask>)

    @Update
    suspend fun update(task: TodayTask)

    @Query("DELETE FROM today_task WHERE today_task_id = :taskId")
    suspend fun delete(taskId: Long)

    @Query("SELECT * FROM today_task WHERE today_task_id = :taskId")
    suspend fun get(taskId: Long): TodayTask?

    @Query("SELECT * FROM today_task WHERE task_date BETWEEN :from AND :to ORDER BY task_date, task_order")
    suspend fun getBetween(from: String, to: String): List<TodayTask>

    @Query("SELECT * FROM today_task WHERE task_date BETWEEN :from AND :to ORDER BY task_date, task_order")
    fun observeBetween(from: String, to: String): Flow<List<TodayTask>>
}
```

- [ ] **Step 5: Create `db/data/plan/DayPlan.kt`**

```kotlin
package com.example.todoapp.db.data.plan

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Today's planning state. A row exists once the day's list has been built. Dates are yyyy-MM-dd. */
@Entity(tableName = "day_plan")
data class DayPlan(
    @PrimaryKey @ColumnInfo(name = "plan_date") val date: String,
    @ColumnInfo(name = "plan_free_minutes") val freeMinutes: Int,
    @ColumnInfo(name = "plan_energy") val energy: String = "NORMAL",
    @ColumnInfo(name = "plan_confirmed") val confirmed: Boolean = false,
    @ColumnInfo(name = "plan_fixed_ticked") val fixedTicked: Boolean = false,
    @ColumnInfo(name = "plan_didnt_fit_mission") val didntFitMissionId: Long? = null,
    @ColumnInfo(name = "plan_didnt_fit_due") val didntFitDue: String? = null,
    @ColumnInfo(name = "plan_didnt_fit_room") val didntFitRoomDay: String? = null,
)
```

- [ ] **Step 6: Create `db/data/plan/DaySettings.kt`**

```kotlin
package com.example.todoapp.db.data.plan

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Free minutes the planner may fill on each weekday (ISO numbers, Monday = 1). */
@Entity(tableName = "day_settings")
data class DaySettings(
    @PrimaryKey @ColumnInfo(name = "settings_weekday") val weekday: Int,
    @ColumnInfo(name = "settings_free_minutes") val freeMinutes: Int,
) {
    companion object {
        fun defaultFor(weekday: Int): Int = if (weekday <= 5) 3 * 60 else 10 * 60

        val defaults: List<DaySettings> = (1..7).map { DaySettings(it, defaultFor(it)) }
    }
}
```

- [ ] **Step 7: Create `db/data/plan/PlanDao.kt`**

```kotlin
package com.example.todoapp.db.data.plan

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Query("SELECT * FROM day_plan WHERE plan_date = :date")
    suspend fun getPlan(date: String): DayPlan?

    @Query("SELECT * FROM day_plan WHERE plan_date = :date")
    fun observePlan(date: String): Flow<DayPlan?>

    @Upsert
    suspend fun upsertPlan(plan: DayPlan)

    @Query("SELECT * FROM day_settings ORDER BY settings_weekday")
    suspend fun getSettings(): List<DaySettings>

    @Upsert
    suspend fun upsertSettings(settings: List<DaySettings>)
}
```

- [ ] **Step 8: Rewrite `db/data/mission/milestone/MilestoneWithDetails.kt`**

```kotlin
package com.example.todoapp.db.data.mission.milestone

import androidx.room.Embedded
import com.example.todoapp.db.data.mission.Mission

data class MilestoneWithDetails(
    @Embedded val mission: Mission,
    @Embedded val milestone: Milestone
)
```

- [ ] **Step 9: Rewrite `db/data/mission/milestone/MilestoneDao.kt`**

```kotlin
package com.example.todoapp.db.data.mission.milestone

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneDao {

    @Query("""
        SELECT mls.*, m.* FROM milestone mls
        JOIN mission m ON m.mission_id = mls.mile_mission_id
        WHERE m.mission_id = :missionId
        ORDER BY mls.milestone_order
    """)
    fun observeForMission(missionId: Long): Flow<List<MilestoneWithDetails>>

    @Query("""
        SELECT mls.*, m.* FROM milestone mls
        JOIN mission m ON m.mission_id = mls.mile_mission_id
        WHERE mls.expected_completion_date >= :startDate
          AND mls.expected_completion_date <= :endDate
        ORDER BY mls.expected_completion_date ASC
    """)
    fun observeInRange(startDate: String, endDate: String): Flow<List<MilestoneWithDetails>>

    @Query("SELECT * FROM milestone")
    suspend fun getAll(): List<Milestone>

    @Update
    suspend fun updateMilestone(milestone: Milestone)
}
```

- [ ] **Step 10: Rewrite `db/models/TaskStatus.kt`**

```kotlin
package com.example.todoapp.db.models

/** Status values stored on milestones. Daily tasks use domain.TaskState instead. */
enum class TaskStatus(val value: String) {
    COMPLETED("COMPLETED"),
    IN_PROGRESS("IN_PROGRESS"),
    ADDED("ADDED");
}
```

- [ ] **Step 11: Rewrite `db/Database.kt`**

```kotlin
package com.example.todoapp.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionDao
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneDao
import com.example.todoapp.db.data.mood.Mood
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodDao
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.plan.DaySettings
import com.example.todoapp.db.data.plan.PlanDao
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskDao
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.db.data.water.WaterIntakeDao

// Version 5 replaces the old mission model. Builders use fallbackToDestructiveMigration, so older databases are wiped.
@Database(
    entities = [
        Mission::class,
        Milestone::class,
        TodayTask::class,
        DayPlan::class,
        DaySettings::class,
        Mood::class,
        TodayMood::class,
        WaterIntake::class],
    version = 5
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getMissionDao(): MissionDao
    abstract fun getMilestoneDao(): MilestoneDao
    abstract fun getTodayTaskDao(): TodayTaskDao
    abstract fun getPlanDao(): PlanDao
    abstract fun getTodayMoodDao(): TodayMoodDao
    abstract fun getWaterIntakeDao(): WaterIntakeDao
}
```

- [ ] **Step 12: Stop registering the removed migration**

In both `shared/src/androidMain/kotlin/com/example/todoapp/db/DB.kt` and `shared/src/iosMain/kotlin/com/example/todoapp/db/DB.kt`, delete the line:
```kotlin
        .addMigrations(MIGRATION_3_4)
```
Keep `.fallbackToDestructiveMigration(true)`.

---

### Task 5: Repositories, placeholder tabs, and the first green build

**Files:** rewrite `repo/MissionRepository.kt`, `repo/TodayTaskRepository.kt`, `repo/MilestoneRepository.kt`, `screen/metrics/CalDiaryViewModel.kt`, `screen/init/InitViewModel.kt`, `screen/diary/DiaryScreenList.kt`, `screen/diary/DiaryViewModel.kt` (interim), `screen/diary/DiaryScreen.kt` (interim), `screen/NavScreens.kt`, `di/DI.kt` (interim); create `screen/placeholder/ComingNextScreen.kt`; edit `screen/basicutils/AppColors.kt`, `screen/diary/diaryitem/components/DiaryMilestoneSection.kt`, and add `domain/MissionRules.kt` (needed by the milestone section).

- [ ] **Step 1: Rewrite `repo/MissionRepository.kt`**

```kotlin
package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionDao
import kotlinx.coroutines.flow.Flow

class MissionRepository(private val database: AppDatabase) {

    private val missionDao: MissionDao by lazy { database.getMissionDao() }

    fun observeActive(): Flow<List<Mission>> = missionDao.observeActive()

    suspend fun getActive(): List<Mission> = missionDao.getActive()

    suspend fun get(missionId: Long): Mission? = missionDao.get(missionId)

    suspend fun save(mission: Mission): Long = missionDao.upsert(mission)

    suspend fun count(): Int = missionDao.count()
}
```

- [ ] **Step 2: Rewrite `repo/TodayTaskRepository.kt`**

```kotlin
package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskDao
import com.example.todoapp.db.models.MyDate
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class TodayTaskRepository(private val database: AppDatabase) {

    private val todayTaskDao: TodayTaskDao by lazy { database.getTodayTaskDao() }

    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<TodayTask>> =
        todayTaskDao.observeBetween(key(from), key(to))

    suspend fun getBetween(from: LocalDate, to: LocalDate): List<TodayTask> =
        todayTaskDao.getBetween(key(from), key(to))

    suspend fun get(taskId: Long): TodayTask? = todayTaskDao.get(taskId)

    suspend fun insert(task: TodayTask): Long = todayTaskDao.insert(task)

    suspend fun insertAll(tasks: List<TodayTask>) = todayTaskDao.insertAll(tasks)

    suspend fun update(task: TodayTask) = todayTaskDao.update(task)

    suspend fun delete(taskId: Long) = todayTaskDao.delete(taskId)

    private fun key(date: LocalDate): String = MyDate.fromLocalDate(date).dateString
}
```

- [ ] **Step 3: Rewrite `repo/MilestoneRepository.kt`**

```kotlin
package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneDao
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.models.MyDate
import kotlinx.coroutines.flow.Flow

class MilestoneRepository(private val database: AppDatabase) {

    private val milestoneDao: MilestoneDao by lazy { database.getMilestoneDao() }

    fun observeForMission(missionId: Long): Flow<List<MilestoneWithDetails>> =
        milestoneDao.observeForMission(missionId)

    fun observeInRange(startDate: MyDate, endDate: MyDate): Flow<List<MilestoneWithDetails>> =
        milestoneDao.observeInRange(startDate.dateString, endDate.dateString)

    suspend fun getAll(): List<Milestone> = milestoneDao.getAll()

    suspend fun updateMilestone(milestone: Milestone) = milestoneDao.updateMilestone(milestone)
}
```

- [ ] **Step 4: Create `domain/MissionRules.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

val Mission.pillarType: PillarType get() = PillarType.valueOf(pillar)
val Mission.missionKind: MissionKind get() = MissionKind.valueOf(kind)
val Mission.effortLevel: Effort get() = Effort.valueOf(effort)
val Mission.ifMissedRule: IfMissed get() = IfMissed.valueOf(ifMissed)
val Mission.periodUnit: Period get() = Period.valueOf(period)
val Mission.intervalPeriod: Period get() = Period.valueOf(intervalUnit)
val Mission.scored: Boolean get() = pillarType.scored

/** ISO weekday numbers: fixed days for blocks, preferred days for habits. */
val Mission.daySet: Set<Int> get() = weekDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

/** Rhythms, and habits counted per month, repeat on an interval instead of a weekly count. */
val Mission.repeatsByInterval: Boolean
    get() = missionKind == MissionKind.RHYTHM ||
        (missionKind != MissionKind.BLOCK && periodUnit == Period.MONTH)

fun Mission.intervalDays(): Int = when {
    missionKind == MissionKind.RHYTHM -> intervalCount.coerceAtLeast(1) * intervalPeriod.days
    periodUnit == Period.MONTH -> (Period.MONTH.days / count.coerceAtLeast(1)).coerceAtLeast(1)
    else -> Period.WEEK.days
}

/** "week", "2 weeks", "3 months"; "15 days" for a twice-a-month habit. */
fun Mission.intervalLabel(): String {
    if (missionKind != MissionKind.RHYTHM) return "${intervalDays()} days"
    val unit = intervalPeriod.name.lowercase()
    return if (intervalCount <= 1) unit else "$intervalCount ${unit}s"
}

/** Started, not ended, not paused, not archived. */
fun Mission.isLiveOn(date: LocalDate): Boolean {
    if (!active) return false
    if (date < startDate.toLocalDate()) return false
    endDate?.let { if (date > it.toLocalDate()) return false }
    pausedUntil?.let { if (date < it.toLocalDate()) return false }
    return true
}

fun Mission.isScheduledOn(date: LocalDate): Boolean = date.dayOfWeek.isoDayNumber in daySet
```

- [ ] **Step 5: Add the pillar palette to `screen/basicutils/AppColors.kt`**

Replace
```kotlin
import androidx.compose.ui.graphics.Color
```
with
```kotlin
import androidx.compose.ui.graphics.Color
import com.example.todoapp.domain.PillarType
```

Replace the two old functions (from `fun getPillarColor(pillarName: String?): Color {` through the closing brace of `getDarkPillarColor`) with:
```kotlin
// Diary ink and surfaces used by the redesigned screens
val InkBlack = Color(0xFF1F1F1F)
val InkGray = Color(0xFF6E6E6E)
val PeachText = Color(0xFF7A4A2C)
val PeachInk = Color(0xFF4A2A18)
val TrackBeige = Color(0xFFEFE9DA)
val StickyYellow = Color(0xFFFDF0C6)
val CardCream = Color(0xFFFBF6E6)
val CrownGold = Color(0xFFD9A93A)
val LineBlueBorder = Color(0xFFCFE3F7)
val ChipBorder = Color(0xFFD9CDB8)

/** Headings, chips and highlights. */
fun pillarLight(pillar: PillarType): Color = when (pillar) {
    PillarType.HEALTH -> Color(0xFFF6D3FF)
    PillarType.WEALTH -> Color(0xFFFFD4B8)
    PillarType.LOVE -> Color(0xFFFFCBD2)
    PillarType.LIFE -> Color(0xFFD4EBFF)
    PillarType.RECHARGE -> Color(0xFFE2E2E2)
}

/** Checkboxes, rings, bars and strike-throughs. */
fun pillarStrong(pillar: PillarType): Color = when (pillar) {
    PillarType.HEALTH -> Color(0xFFE7B5F5)
    PillarType.WEALTH -> Color(0xFFF9B98F)
    PillarType.LOVE -> Color(0xFFF7A9B5)
    PillarType.LIFE -> Color(0xFFA9D1F5)
    PillarType.RECHARGE -> Color(0xFFC4C4C4)
}

/** Text on a pillar-light background. */
fun pillarInk(pillar: PillarType): Color = when (pillar) {
    PillarType.HEALTH -> Color(0xFF6B3D7A)
    PillarType.WEALTH -> Color(0xFF7A4A2C)
    PillarType.LOVE -> Color(0xFF7A3341)
    PillarType.LIFE -> Color(0xFF2F5677)
    PillarType.RECHARGE -> Color(0xFF555555)
}
```

- [ ] **Step 6: Point the milestone section at the new palette**

In `screen/diary/diaryitem/components/DiaryMilestoneSection.kt`, replace
```kotlin
import com.example.todoapp.screen.basicutils.getDarkPillarColor
```
with
```kotlin
import com.example.todoapp.domain.pillarType
import com.example.todoapp.screen.basicutils.pillarStrong
```
and replace
```kotlin
                    checkedColor = getDarkPillarColor(mwd.pillar?.pillarName),
```
with
```kotlin
                    checkedColor = pillarStrong(mwd.mission.pillarType),
```

- [ ] **Step 7: Rewrite `screen/metrics/CalDiaryViewModel.kt`**

```kotlin
package com.example.todoapp.screen.metrics

import androidx.lifecycle.ViewModel
import com.example.todoapp.db.models.MyDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

/** The date the diary is showing, shared across tabs. */
class CalDiaryViewModel : ViewModel() {

    private val _currentDate = MutableStateFlow(MyDate.now().toLocalDate())
    val currentDate: StateFlow<LocalDate> get() = _currentDate

    fun selectDate(selectedDate: LocalDate) {
        _currentDate.value = selectedDate
    }
}
```

- [ ] **Step 8: Rewrite `screen/init/InitViewModel.kt` (interim: moods only; Task 9 adds missions)**

```kotlin
package com.example.todoapp.screen.init

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.models.LOCAL_USER_ID
import com.example.todoapp.repo.TodayMoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InitViewModel(
    private val todayMoodRepository: TodayMoodRepository
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> get() = _isReady

    fun insertDefaultData() {
        viewModelScope.launch {
            todayMoodRepository.insertDefaultMoods(LOCAL_USER_ID)
            _isReady.value = true
        }
    }
}
```

- [ ] **Step 9: Create `screen/placeholder/ComingNextScreen.kt`**

```kotlin
package com.example.todoapp.screen.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.components.WriteText

/** Stands in for a tab that a later plan rebuilds. */
@Composable
fun ComingNextScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Light_Yellowww).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        WriteText(message, color = InkGray, fontSize = 18f)
    }
}
```

- [ ] **Step 10: Rewrite `screen/diary/DiaryScreenList.kt` (keeps the shared diary helpers, drops the old task list)**

```kotlin
package com.example.todoapp.screen.diary

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.diary.diaryitem.components.NotesItem
import com.example.todoapp.screen.diary.diaryitem.components.dateheader.DateHeader
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodHeader
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeHeader
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.water_daily_limit
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.block_b
import todoapp.shared.generated.resources.pencil

fun LazyListScope.MoodHeaderItem(todayMoodList: List<TodayMoodWithDetails>?,
                                 selection: LocalDate,
                                 upsertMood : (TodayMood) -> Unit) {
    item {
        MoodHeader(
            todayMoodList,
            selection,
            upsertMood
        )
    }
}

fun LazyListScope.DateHeaderItem(cDate: LocalDate,
                                 changeDate: (LocalDate) -> Unit) {
    item {
        DateHeader(cDate, changeDate = { changeDate ( it ) })
    }
}

fun LazyListScope.WaterIntakeHeaderItem(waterIntake: WaterIntake?,
                                        selection: LocalDate,
                                        upsertWaterIntake : (WaterIntake) -> Unit,
                                        waterIntakeCompleted : (Boolean) -> Unit) {
    item {
        WaterIntakeHeader(waterIntake?.waterIntakeProgress?:0.0, selection, upsertWaterIntake = {progress->
            upsertWaterIntake((waterIntake ?: WaterIntake(
                waterIntakeId = 0,
                waterIntakeProgress = progress,
                waterDate = MyDate.fromLocalDate(selection)
            )).copy(waterIntakeProgress = progress))
            waterIntakeCompleted(progress.toInt() == (water_daily_limit-1))
        })
    }
}

fun LazyListScope.Notes(waterIntake: WaterIntake?,
                        selection: LocalDate,
                        upsertWaterIntake : (WaterIntake) -> Unit) {
    item {
        val rowHeight = remember { mutableStateOf(52) }

        NotesItem(rowHeight.value,waterIntake, selection,textArranged = { lineCount ->
            rowHeight.value = 26*(lineCount+1)
        }, upsertWaterIntake = {n:String->
            upsertWaterIntake((waterIntake ?: WaterIntake(
                waterIntakeId = 0,
                notes = n,
                waterDate = MyDate.fromLocalDate(selection)
            )).copy(notes = n))
        })
    }
}

fun LazyListScope.ToDoListHeader(headingtext: String, color: Color, editClicked: () -> Unit) {
    item {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            HeadingBanner(color, headingtext)
            Box(modifier = Modifier.fillMaxWidth().padding(end = 5.dp).wrapContentHeight()) {
                Box(
                    modifier = Modifier.clickable { editClicked() }
                        .wrapContentSize()
                        .shadow(5.dp, CircleShape)
                        .clip(CircleShape)
                        .background(color)
                        .padding( 8.dp)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(Res.drawable.pencil),
                        contentDescription = "Add to today",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp).padding(start = 2.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

fun LazyListScope.HeadingText(headingtext: String, color: Color) {
    item {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            HeadingBanner(color, headingtext)
        }
    }
}

@Composable
private fun HeadingBanner(color: Color, headingtext: String) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.height(30.dp).fillMaxWidth()
                .padding(horizontal = 60.dp)
        ) {
            Image(
                painterResource(Res.drawable.block_b),
                contentDescription = "Section heading background",
                colorFilter = ColorFilter.tint(color),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
            )
            Text(
                text = headingtext,
                modifier = Modifier
                    .align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
```

- [ ] **Step 11: Rewrite `screen/diary/DiaryViewModel.kt` (interim: milestones only; Task 10 replaces it)**

```kotlin
package com.example.todoapp.screen.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.repo.MilestoneRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class DiaryViewModel(
    private val milestoneRepository: MilestoneRepository,
    private val currentDate: LocalDate,
) : ViewModel() {

    val milestones: StateFlow<List<MilestoneWithDetails>> = milestoneRepository
        .observeInRange(MyDate.fromLocalDate(currentDate).subtractDays(7), MyDate.fromLocalDate(currentDate).addDays(7))
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun updateMilestoneStatus(milestone: Milestone, achieved: Boolean, completionDate: LocalDate) {
        viewModelScope.launch {
            val updated = if (achieved) {
                milestone.copy(status = TaskStatus.COMPLETED.value, actualCompletionDate = MyDate.fromLocalDate(completionDate))
            } else {
                milestone.copy(status = TaskStatus.IN_PROGRESS.value, actualCompletionDate = null)
            }
            milestoneRepository.updateMilestone(updated)
        }
    }
}
```

- [ ] **Step 12: Rewrite `screen/diary/DiaryScreen.kt` (interim: no to-do list yet; Task 12 replaces it)**

```kotlin
package com.example.todoapp.screen.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.Orange80
import com.example.todoapp.screen.basicutils.Red80
import com.example.todoapp.screen.basicutils.Yellow180
import com.example.todoapp.screen.diary.diaryitem.components.DiaryMilestoneSection
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeViewModel
import kotlinx.datetime.LocalDate

@Composable
fun DiaryScreen(
    diaryViewModel: DiaryViewModel,
    moodViewModel: MoodViewModel,
    waterIntakeViewModel: WaterIntakeViewModel,
    selection: LocalDate,
    changeDate: (LocalDate) -> Unit
) {
    val milestones by diaryViewModel.milestones.collectAsState()
    val todayMoodList: List<TodayMoodWithDetails>? by moodViewModel.todayMoodStatuses.collectAsState(emptyList())
    val waterIntake: WaterIntake? by waterIntakeViewModel.waterIntake.collectAsState()

    LaunchedEffect(selection) {
        moodViewModel.loadDailyMoodStatus(selection)
        waterIntakeViewModel.loadWaterIntake(selection)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Light_Yellowww)) {
        DateHeaderItem(selection, changeDate = changeDate)
        HeadingText("MOOD", Red80)
        MoodHeaderItem(todayMoodList, selection, upsertMood = { moodViewModel.upsertTodayMood(it) })
        HeadingText("WATER INTAKE", Blue80)
        WaterIntakeHeaderItem(waterIntake, selection, upsertWaterIntake = { waterIntakeViewModel.upsertWaterIntake(it) }, waterIntakeCompleted = {})
        if (milestones.isNotEmpty()) {
            HeadingText("MILESTONE", Orange80)
            DiaryMilestoneSection(milestones = milestones, onMilestoneChecked = { mwd, achieved ->
                diaryViewModel.updateMilestoneStatus(mwd.milestone, achieved, selection)
            })
        }
        HeadingText("NOTES", Yellow180)
        Notes(waterIntake, selection, upsertWaterIntake = { waterIntakeViewModel.upsertWaterIntake(it) })
        item { Spacer(Modifier.height(120.dp)) }
    }
}
```

- [ ] **Step 13: Rewrite `screen/NavScreens.kt`**

View models are created once per date with `remember`, so the diary doesn't build a new one on every recomposition.
```kotlin
package com.example.todoapp.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoapp.app.MyScreen
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.diary.DiaryScreen
import com.example.todoapp.screen.diary.DiaryViewModel
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeViewModel
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.placeholder.ComingNextScreen
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.NavOptions
import moe.tlaster.precompose.navigation.PopUpTo
import moe.tlaster.precompose.navigation.rememberNavigator
import org.jetbrains.compose.resources.painterResource
import org.koin.core.parameter.parametersOf
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.diary_heart_closed
import todoapp.shared.generated.resources.metrics_calendar
import todoapp.shared.generated.resources.target

@Composable
fun NavScreens(calDateViewModel: CalDiaryViewModel) {
    val navigator = rememberNavigator()
    val backStackEntry by navigator.currentEntry.collectAsState(null)
    val currentDate by calDateViewModel.currentDate.collectAsState()

    val currentScreen = MyScreen.valueOf(
        backStackEntry?.route?.route ?: MyScreen.Diary.name
    )

    fun navigateTab(screen: MyScreen) {
        navigator.navigate(
            screen.name,
            NavOptions(
                launchSingleTop = true,
                popUpTo = PopUpTo(MyScreen.Diary.name, inclusive = false)
            )
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == MyScreen.Diary,
                    onClick = { if (currentScreen != MyScreen.Diary) navigateTab(MyScreen.Diary) },
                    label = { Text("Diary") },
                    icon = { Icon(painterResource(Res.drawable.diary_heart_closed), tint = Color.Gray,
                        contentDescription = "Diary", modifier = Modifier.size(30.dp)) }
                )
                NavigationBarItem(
                    selected = currentScreen == MyScreen.Missions,
                    onClick = { if (currentScreen != MyScreen.Missions) navigateTab(MyScreen.Missions) },
                    label = { Text("Missions") },
                    icon = { Icon(painterResource(Res.drawable.target), tint = Color.Gray,
                        contentDescription = "Missions", modifier = Modifier.size(30.dp)) }
                )
                NavigationBarItem(
                    selected = currentScreen == MyScreen.Calendar,
                    onClick = { if (currentScreen != MyScreen.Calendar) navigateTab(MyScreen.Calendar) },
                    label = { Text("Metrics") },
                    icon = { Icon(painterResource(Res.drawable.metrics_calendar), tint = Color.Gray,
                        contentDescription = "Metrics", modifier = Modifier.size(30.dp)) }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navigator = navigator,
                initialRoute = MyScreen.Diary.name
            ) {
                scene(route = MyScreen.Missions.name) {
                    ComingNextScreen("Missions are being rebuilt around your pillars. They come back in the next step.")
                }
                scene(route = MyScreen.Diary.name) {
                    val diaryViewModel = remember(currentDate) {
                        KoinF.di!!.get<DiaryViewModel> { parametersOf(currentDate) }
                    }
                    val moodViewModel = remember(currentDate) {
                        KoinF.di!!.get<MoodViewModel> { parametersOf(currentDate) }
                    }
                    val waterIntakeViewModel = remember(currentDate) {
                        KoinF.di!!.get<WaterIntakeViewModel> { parametersOf(currentDate) }
                    }
                    DiaryScreen(
                        diaryViewModel,
                        moodViewModel,
                        waterIntakeViewModel,
                        currentDate,
                        changeDate = { date -> calDateViewModel.selectDate(date) }
                    )
                }
                scene(route = MyScreen.Calendar.name) {
                    ComingNextScreen("Metrics come back after missions, with pillar rings and a week calendar.")
                }
            }
        }
    }
}
```

- [ ] **Step 14: Rewrite `di/DI.kt` (interim)**

```kotlin
package com.example.todoapp.di

import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.TodayMoodRepository
import com.example.todoapp.repo.TodayTaskRepository
import com.example.todoapp.repo.WaterIntakeRepository
import com.example.todoapp.screen.diary.DiaryViewModel
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeViewModel
import com.example.todoapp.screen.init.InitViewModel
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import kotlinx.datetime.LocalDate
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

expect fun platformModule(): Module

fun vmModule(): Module {
    return module {
        // Repositories
        single { MissionRepository(get()) }
        single { MilestoneRepository(get()) }
        single { TodayTaskRepository(get()) }
        single { TodayMoodRepository(get()) }
        single { WaterIntakeRepository(get()) }

        // ViewModels
        single { InitViewModel(get()) }
        single { CalDiaryViewModel() }
        factory { (day: LocalDate) -> DiaryViewModel(get(), day) }
        factory { (day: LocalDate) -> MoodViewModel(get()) }
        factory { (day: LocalDate) -> WaterIntakeViewModel(get()) }
    }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(platformModule(), vmModule())
    }

object KoinF {
    var di: Koin? = null

    fun setupKoin(appDeclaration: KoinAppDeclaration = {}) {
        if (di == null) {
            di = initKoin(appDeclaration).koin
        }
    }
}
```

- [ ] **Step 15: Build (first checkpoint)**

Run: `./gradlew :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL`. If a file you didn't touch fails to compile, it is still importing something deleted in Task 2: remove that import or symbol and rebuild.

- [ ] **Step 16: Commit**

```bash
git add -A shared
git commit -m "feat: new mission schema (db v5), remove old mission, planner and metrics screens"
```

---

### Task 6: History and points

**Files:** create `domain/History.kt`, `domain/Points.kt`.

- [ ] **Step 1: Create `domain/History.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.todotask.TodayTask
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

val TodayTask.taskState: TaskState get() = TaskState.valueOf(state)
val TodayTask.date: LocalDate get() = taskDate.toLocalDate()

/** Read-only view over past and upcoming tasks. */
class History(val all: List<TodayTask>) {

    private val byMission: Map<Long, List<TodayTask>> = all.groupBy { it.missionId }

    fun tasksFor(missionId: Long): List<TodayTask> = byMission[missionId].orEmpty()

    fun lastDoneBefore(missionId: Long, before: LocalDate): LocalDate? =
        tasksFor(missionId)
            .filter { it.taskState.countsAsDone && it.date < before }
            .maxOfOrNull { it.date }

    fun doneCount(missionId: Long, from: LocalDate, to: LocalDate): Int =
        tasksFor(missionId).count { it.taskState.countsAsDone && it.date >= from && it.date <= to }

    /** Was it on that day's list (and not moved away or skipped)? */
    fun pickedOn(missionId: Long, date: LocalDate): Boolean =
        tasksFor(missionId).any { it.date == date && it.taskState.isOpen }

    /** Consecutive done days ending the day before [today]. */
    fun streakBefore(missionId: Long, today: LocalDate): Int {
        val doneDays = tasksFor(missionId).filter { it.taskState.countsAsDone }.map { it.date }.toSet()
        var day = today.minus(1, DateTimeUnit.DAY)
        var streak = 0
        while (day in doneDays) {
            streak++
            day = day.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }
}
```

- [ ] **Step 2: Create `domain/Points.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Spec section 5. */
object Points {

    /** A pillar counts as full at 80% of its target. */
    const val FULL_SHARE = 0.8

    fun forState(weight: Int, state: TaskState): Int = when (state) {
        TaskState.DONE -> weight
        TaskState.PARTIAL -> (weight + 1) / 2
        else -> 0
    }

    /**
     * Due date of an interval mission, judged only from what was done before [weekStart],
     * so the week's target doesn't change when it gets done mid-week.
     */
    fun dueDate(mission: Mission, history: History, weekStart: LocalDate): LocalDate {
        val last = history.lastDoneBefore(mission.missionId, weekStart)
        return last?.plus(mission.intervalDays(), DateTimeUnit.DAY) ?: mission.startDate.toLocalDate()
    }

    /** How many times the mission is due in the week starting [weekStart]. */
    fun occurrencesInWeek(mission: Mission, weekStart: LocalDate, history: History): Int {
        val days = (0..6).map { weekStart.plus(it, DateTimeUnit.DAY) }
        val liveDays = days.filter { mission.isLiveOn(it) }
        if (liveDays.isEmpty()) return 0
        return when {
            mission.missionKind == MissionKind.BLOCK -> liveDays.count { mission.isScheduledOn(it) }
            mission.repeatsByInterval -> if (dueDate(mission, history, weekStart) <= days.last()) 1 else 0
            mission.periodUnit == Period.DAY -> liveDays.size
            else -> mission.count
        }
    }

    fun weeklyTarget(missions: List<Mission>, pillar: PillarType, weekStart: LocalDate, history: History): Int =
        missions
            .filter { it.pillarType == pillar && it.scored }
            .sumOf { it.weight * occurrencesInWeek(it, weekStart, history) }

    fun earnedInWeek(missions: List<Mission>, tasks: List<TodayTask>, pillar: PillarType, weekStart: LocalDate): Int {
        val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
        val ids = missions.filter { it.pillarType == pillar }.map { it.missionId }.toSet()
        return tasks
            .filter { it.missionId in ids && it.date >= weekStart && it.date <= weekEnd }
            .sumOf { it.pointsEarned }
    }

    fun isFull(earned: Int, target: Int): Boolean = target == 0 || earned >= target * FULL_SHARE
}
```

- [ ] **Step 3: Build**

Run: `./gradlew :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add -A shared
git commit -m "feat: task history and weekly points rules"
```

---

### Task 7: Reason lines

**Files:** create `domain/ReasonLines.kt`.

- [ ] **Step 1: Create `domain/ReasonLines.kt`**

```kotlin
package com.example.todoapp.domain

/** Spec section 7. Lines are built only from stored data, never from what a title means. */
enum class ReasonCode {
    STREAK, QUOTA, RHYTHM_DUE, RHYTHM_FIRST, RHYTHM_OVERDUE, MOVED, MILESTONE,
    RECHARGE_HEAVY, RECHARGE_USUAL, BLOCK, PILLAR_LIGHT, ADDED
}

data class Reason(val code: ReasonCode, val args: List<String> = emptyList()) {

    fun encodeArgs(): String = args.joinToString(SEPARATOR)

    companion object {
        const val SEPARATOR = "|"

        fun decode(code: String, args: String): Reason =
            Reason(ReasonCode.valueOf(code), if (args.isEmpty()) emptyList() else args.split(SEPARATOR))
    }
}

object ReasonLines {

    fun render(reason: Reason): String {
        val a = reason.args
        return when (reason.code) {
            ReasonCode.STREAK ->
                if (a[0].toInt() > 0) "Every day · ${a[0]}-day streak" else "Every day"
            ReasonCode.QUOTA ->
                "${a[0]} of ${a[1]} this week · " + if (a[2] == "1") "last day" else "${a[2]} days left"
            ReasonCode.RHYTHM_DUE ->
                (if (a[0] == "1") "Last done yesterday" else "Last done ${a[0]} days ago") + " · you aim for every ${a[1]}"
            ReasonCode.RHYTHM_FIRST -> "First one · you aim for every ${a[0]}"
            ReasonCode.RHYTHM_OVERDUE -> if (a[0] == "1") "Due yesterday" else "Due ${a[0]} days ago"
            ReasonCode.MOVED -> "Moved from ${a[0]}"
            ReasonCode.MILESTONE -> "Milestone ${a[0]} of ${a[1]} · due ${a[2]}"
            ReasonCode.RECHARGE_HEAVY -> "${a[0]} hours of fixed blocks today. This counts too."
            ReasonCode.RECHARGE_USUAL -> "You usually take ${a[0]} breaks a week."
            ReasonCode.BLOCK -> "${a[0]} to ${a[1]}"
            ReasonCode.PILLAR_LIGHT -> "${a[0]} is light this week"
            ReasonCode.ADDED -> "Added by you"
        }
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add -A shared
git commit -m "feat: reason line codes and rendering"
```

---

### Task 8: The planner

**Files:** create `domain/Planner.kt`.

- [ ] **Step 1: Create `domain/Planner.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.TaskStatus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class PlanInput(
    val date: LocalDate,
    val missions: List<Mission>,
    val milestones: List<Milestone>,
    /** Tasks from about 120 days back through a week ahead, including today's. */
    val history: History,
    val todayTasks: List<TodayTask>,
    val freeMinutes: Int,
    val energy: Energy,
    /** Free minutes left on a later day. */
    val roomOn: (LocalDate) -> Int,
)

data class Pick(val mission: Mission, val reason: Reason, val isFixedBlock: Boolean)

data class DidntFit(val missionId: Long, val due: LocalDate, val roomDay: LocalDate?)

data class PlanResult(val picks: List<Pick>, val didntFit: DidntFit?)

/** Spec section 6.2. */
object Planner {

    const val LOW_ENERGY_SHARE = 0.6
    const val HEAVY_DAY_MINUTES = 6 * 60

    private data class Candidate(
        val mission: Mission,
        /** Lower is more urgent: 2 overdue, 3 streak at risk, 4 quota due or preferred day, 5 milestone soon, 6 the rest. */
        val tier: Int,
        /** How far behind the mission's pillar is this week, 0 to 1. Recharge uses -1 so it sorts last. */
        val gap: Double,
        val reason: Reason,
        /** Set for things with a due date; only these can show as "didn't fit". */
        val due: LocalDate?,
    )

    fun plan(input: PlanInput): PlanResult {
        val fixed = fixedBlocks(input)
        val picks = mutableListOf<Pick>()
        val skipped = mutableListOf<Candidate>()
        var left = budget(input)
        for (candidate in candidates(input, fixed)) {
            val mission = candidate.mission
            val tooHard = input.energy == Energy.LOW && mission.effortLevel == Effort.DRAINING
            if (!tooHard && mission.durationMinutes <= left) {
                picks += Pick(mission, candidate.reason, isFixedBlock = false)
                left -= mission.durationMinutes
            } else {
                skipped += candidate
            }
        }
        val didntFit = skipped.firstOrNull { it.due != null }?.let { candidate ->
            DidntFit(candidate.mission.missionId, candidate.due!!, nextRoomDay(input, candidate.mission.durationMinutes))
        }
        return PlanResult(fixed + picks, didntFit)
    }

    /** The best candidate not already on today's list, for "Swap one". */
    fun nextCandidate(input: PlanInput, exclude: Set<Long>): Pick? =
        candidates(input, fixedBlocks(input))
            .firstOrNull { it.mission.missionId !in exclude }
            ?.let { Pick(it.mission, it.reason, isFixedBlock = false) }

    /** Free minutes the flexible picks may use, after anything already on today's list. */
    fun budget(input: PlanInput): Int {
        val share = if (input.energy == Energy.LOW) LOW_ENERGY_SHARE else 1.0
        val minutesById = input.missions.associate { it.missionId to it.durationMinutes }
        val alreadyPlanned = input.todayTasks
            .filter { !it.isFixedBlock && it.taskState.isOpen }
            .sumOf { minutesById[it.missionId] ?: 0 }
        return (input.freeMinutes * share).toInt() - alreadyPlanned
    }

    private fun fixedBlocks(input: PlanInput): List<Pick> = input.missions
        .filter { it.missionKind == MissionKind.BLOCK && it.isLiveOn(input.date) && it.isScheduledOn(input.date) }
        .filter { mission -> input.todayTasks.none { it.missionId == mission.missionId } }
        .map { Pick(it, Reason(ReasonCode.BLOCK, listOf(it.startTime.orEmpty(), it.endTime.orEmpty())), isFixedBlock = true) }

    private fun candidates(input: PlanInput, fixed: List<Pick>): List<Candidate> {
        val weekStart = WeekMath.weekStart(input.date)
        val weekEnd = WeekMath.weekEnd(input.date)
        val minutesById = input.missions.associate { it.missionId to it.durationMinutes }
        val fixedMinutesToday = fixed.sumOf { it.mission.durationMinutes } +
            input.todayTasks.filter { it.isFixedBlock }.sumOf { minutesById[it.missionId] ?: 0 }
        val gaps = PillarType.scoredPillars.associateWith { pillarGap(input, it, weekStart) }
        return input.missions
            .filter { it.missionKind != MissionKind.BLOCK && it.isLiveOn(input.date) }
            .filter { mission -> input.todayTasks.none { it.missionId == mission.missionId } }
            .mapNotNull { candidateFor(it, input, weekStart, weekEnd, fixedMinutesToday, gaps[it.pillarType] ?: 0.0) }
            .sortedWith(compareBy<Candidate> { it.tier }.thenByDescending { it.gap }.thenBy { it.mission.missionId })
    }

    private fun candidateFor(
        mission: Mission,
        input: PlanInput,
        weekStart: LocalDate,
        weekEnd: LocalDate,
        fixedMinutesToday: Int,
        gap: Double,
    ): Candidate? {
        val date = input.date
        val history = input.history
        val yesterday = date.minus(1, DateTimeUnit.DAY)

        if (mission.missionKind == MissionKind.RECHARGE) {
            val done = history.doneCount(mission.missionId, weekStart, yesterday)
            return when {
                fixedMinutesToday >= HEAVY_DAY_MINUTES -> Candidate(
                    mission, 6, -1.0, Reason(ReasonCode.RECHARGE_HEAVY, listOf((fixedMinutesToday / 60).toString())), null
                )
                done < mission.count -> Candidate(
                    mission, 6, -1.0, Reason(ReasonCode.RECHARGE_USUAL, listOf(mission.count.toString())), null
                )
                else -> null
            }
        }

        if (mission.repeatsByInterval) {
            val last = history.lastDoneBefore(mission.missionId, date)
            val due = last?.plus(mission.intervalDays(), DateTimeUnit.DAY) ?: mission.startDate.toLocalDate()
            if (due > weekEnd) return null
            val label = mission.intervalLabel()
            return when {
                due < date -> Candidate(mission, 2, gap, Reason(ReasonCode.RHYTHM_OVERDUE, listOf(due.daysUntil(date).toString())), due)
                last == null -> Candidate(mission, 6, gap, Reason(ReasonCode.RHYTHM_FIRST, listOf(label)), due)
                else -> Candidate(mission, 6, gap, Reason(ReasonCode.RHYTHM_DUE, listOf(last.daysUntil(date).toString(), label)), due)
            }
        }

        if (mission.periodUnit == Period.DAY) {
            val streak = history.streakBefore(mission.missionId, date)
            return Candidate(mission, if (streak > 0) 3 else 6, gap, Reason(ReasonCode.STREAK, listOf(streak.toString())), null)
        }

        // Weekly count: habits and finish lines.
        val done = history.doneCount(mission.missionId, weekStart, yesterday)
        val remaining = mission.count - done
        if (remaining <= 0) return null
        val daysLeft = WeekMath.daysLeftIncludingToday(date)
        val urgent = remaining >= daysLeft
        val preferred = mission.isScheduledOn(date)
        // Spread the week out: not two days in a row unless it's needed or today is a preferred day.
        if (!urgent && !preferred && history.pickedOn(mission.missionId, yesterday)) return null
        val milestone = nextMilestone(mission, input.milestones, date)
        val tier = when {
            urgent || preferred -> 4
            milestone != null -> 5
            else -> 6
        }
        val reason = milestone?.first
            ?: Reason(ReasonCode.QUOTA, listOf(done.toString(), mission.count.toString(), daysLeft.toString()))
        return Candidate(mission, tier, gap, reason, milestone?.second)
    }

    /** The next open milestone if it's due within 7 days, with its reason and date. */
    private fun nextMilestone(mission: Mission, milestones: List<Milestone>, date: LocalDate): Pair<Reason, LocalDate>? {
        val mine = milestones.filter { it.missionId == mission.missionId }.sortedBy { it.milestoneOrder }
        val nextIndex = mine.indexOfFirst { it.status != TaskStatus.COMPLETED.value }
        if (nextIndex < 0) return null
        val dueDate = mine[nextIndex].expectedCompletionDate.toLocalDate()
        if (date.daysUntil(dueDate) > 7) return null
        val reason = Reason(ReasonCode.MILESTONE, listOf((nextIndex + 1).toString(), mine.size.toString(), dueDate.shortLabel()))
        return reason to dueDate
    }

    private fun pillarGap(input: PlanInput, pillar: PillarType, weekStart: LocalDate): Double {
        val target = Points.weeklyTarget(input.missions, pillar, weekStart, input.history)
        if (target == 0) return 0.0
        val earned = Points.earnedInWeek(input.missions, input.history.all, pillar, weekStart)
        return (1.0 - earned.toDouble() / target).coerceAtLeast(0.0)
    }

    private fun nextRoomDay(input: PlanInput, minutes: Int): LocalDate? =
        (1..7).map { input.date.plus(it, DateTimeUnit.DAY) }.firstOrNull { input.roomOn(it) >= minutes }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add -A shared
git commit -m "feat: planner that builds a day from pillars, cadence and free time"
```

---

### Task 9: Plan repository, sample data and startup

**Files:** create `repo/PlanRepository.kt`, `db/SampleData.kt`; rewrite `screen/init/InitViewModel.kt`; edit `di/DI.kt`.

- [ ] **Step 1: Create `repo/PlanRepository.kt`**

All writes go through one mutex, so two screens asking at once can't build the same day twice.
```kotlin
package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.plan.DaySettings
import com.example.todoapp.db.data.plan.PlanDao
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.History
import com.example.todoapp.domain.PlanInput
import com.example.todoapp.domain.Planner
import com.example.todoapp.domain.Points
import com.example.todoapp.domain.Reason
import com.example.todoapp.domain.ReasonCode
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.date
import com.example.todoapp.domain.taskState
import com.example.todoapp.domain.weekdayName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class PlanRepository(
    private val database: AppDatabase,
    private val missionRepository: MissionRepository,
    private val todayTaskRepository: TodayTaskRepository,
    private val milestoneRepository: MilestoneRepository,
) {
    private val planDao: PlanDao by lazy { database.getPlanDao() }
    private val mutex = Mutex()

    fun observePlan(date: LocalDate): Flow<DayPlan?> = planDao.observePlan(key(date))

    suspend fun ensureDefaults() {
        if (planDao.getSettings().isEmpty()) planDao.upsertSettings(DaySettings.defaults)
    }

    /** Builds the day's list the first time it's asked for; later calls do nothing. */
    suspend fun ensurePlanned(date: LocalDate) {
        mutex.withLock {
            if (planDao.getPlan(key(date)) != null) return@withLock
            val plan = DayPlan(date = key(date), freeMinutes = defaultFreeMinutes(date))
            planDao.upsertPlan(plan)
            fill(date, plan)
        }
    }

    /** New free time or energy for the day: keeps what's done, crowned, moved in or added by hand, and re-picks the rest. */
    suspend fun changeDay(date: LocalDate, freeMinutes: Int, energy: Energy) {
        mutex.withLock {
            val current = planDao.getPlan(key(date)) ?: DayPlan(date = key(date), freeMinutes = defaultFreeMinutes(date))
            val plan = current.copy(freeMinutes = freeMinutes, energy = energy.name)
            planDao.upsertPlan(plan)
            rebuild(date, plan)
        }
    }

    suspend fun lighterDay(date: LocalDate) {
        val freeMinutes = planDao.getPlan(key(date))?.freeMinutes ?: defaultFreeMinutes(date)
        changeDay(date, freeMinutes, Energy.LOW)
    }

    suspend fun confirm(date: LocalDate) {
        mutex.withLock {
            planDao.getPlan(key(date))?.let { planDao.upsertPlan(it.copy(confirmed = true)) }
        }
    }

    /** Evening check: fixed blocks start ticked, once per day, so an untick sticks. */
    suspend fun tickFixedBlocks(date: LocalDate) {
        mutex.withLock {
            val plan = planDao.getPlan(key(date)) ?: return@withLock
            if (plan.fixedTicked) return@withLock
            todayTaskRepository.getBetween(date, date)
                .filter { it.isFixedBlock && it.taskState == TaskState.PLANNED }
                .forEach { updateState(it, TaskState.DONE) }
            planDao.upsertPlan(plan.copy(fixedTicked = true))
        }
    }

    suspend fun setState(taskId: Long, state: TaskState) {
        mutex.withLock {
            todayTaskRepository.get(taskId)?.let { updateState(it, state) }
        }
    }

    suspend fun toggleDone(taskId: Long) {
        mutex.withLock {
            val task = todayTaskRepository.get(taskId) ?: return@withLock
            updateState(task, if (task.taskState.countsAsDone) TaskState.PLANNED else TaskState.DONE)
        }
    }

    suspend fun toggleCrown(taskId: Long) {
        mutex.withLock {
            val task = todayTaskRepository.get(taskId) ?: return@withLock
            if (!task.crowned) {
                val crownedToday = todayTaskRepository.getBetween(task.date, task.date).count { it.crowned }
                if (crownedToday >= MAX_CROWNS) return@withLock
            }
            todayTaskRepository.update(task.copy(crowned = !task.crowned))
        }
    }

    /** Moves an open task to the next day with room (tomorrow if none in the next week). */
    suspend fun moveToNextRoom(taskId: Long) {
        mutex.withLock {
            val task = todayTaskRepository.get(taskId) ?: return@withLock
            val mission = missionRepository.get(task.missionId) ?: return@withLock
            val from = task.date
            val target = roomByDay(from).entries.firstOrNull { it.value >= mission.durationMinutes }?.key
                ?: from.plus(1, DateTimeUnit.DAY)
            todayTaskRepository.update(task.copy(state = TaskState.MOVED.name, pointsEarned = 0))
            todayTaskRepository.insert(
                newTask(mission, target, Reason(ReasonCode.MOVED, listOf(from.weekdayName())), fixed = false, order = 0, movedFrom = from)
            )
        }
    }

    suspend fun swap(taskId: Long) {
        mutex.withLock {
            val task = todayTaskRepository.get(taskId) ?: return@withLock
            val date = task.date
            val plan = planDao.getPlan(key(date)) ?: return@withLock
            todayTaskRepository.delete(taskId)
            val next = Planner.nextCandidate(input(date, plan), exclude = setOf(task.missionId)) ?: return@withLock
            todayTaskRepository.insert(newTask(next.mission, date, next.reason, fixed = false, order = task.sortOrder))
        }
    }

    suspend fun addMission(date: LocalDate, missionId: Long) {
        mutex.withLock {
            val mission = missionRepository.get(missionId) ?: return@withLock
            val order = todayTaskRepository.getBetween(date, date).size
            todayTaskRepository.insert(newTask(mission, date, Reason(ReasonCode.ADDED), fixed = false, order = order))
        }
    }

    suspend fun removeTask(taskId: Long) {
        mutex.withLock { todayTaskRepository.delete(taskId) }
    }

    private suspend fun updateState(task: TodayTask, state: TaskState) {
        val mission = missionRepository.get(task.missionId) ?: return
        todayTaskRepository.update(task.copy(state = state.name, pointsEarned = Points.forState(mission.weight, state)))
    }

    private suspend fun rebuild(date: LocalDate, plan: DayPlan) {
        todayTaskRepository.getBetween(date, date)
            .filter {
                it.taskState == TaskState.PLANNED && !it.isFixedBlock && !it.crowned &&
                    it.movedFromDate == null && it.reasonCode != ReasonCode.ADDED.name
            }
            .forEach { todayTaskRepository.delete(it.todayTaskId) }
        fill(date, plan)
    }

    private suspend fun fill(date: LocalDate, plan: DayPlan) {
        val input = input(date, plan)
        val result = Planner.plan(input)
        val firstOrder = input.todayTasks.size
        todayTaskRepository.insertAll(
            result.picks.mapIndexed { i, pick -> newTask(pick.mission, date, pick.reason, pick.isFixedBlock, firstOrder + i) }
        )
        planDao.upsertPlan(
            plan.copy(
                didntFitMissionId = result.didntFit?.missionId,
                didntFitDue = result.didntFit?.due?.let { key(it) },
                didntFitRoomDay = result.didntFit?.roomDay?.let { key(it) },
            )
        )
    }

    private suspend fun input(date: LocalDate, plan: DayPlan): PlanInput {
        val missions = missionRepository.getActive()
        val tasks = todayTaskRepository.getBetween(date.minus(HISTORY_DAYS, DateTimeUnit.DAY), date.plus(7, DateTimeUnit.DAY))
        val room = roomByDay(date, missions, tasks)
        return PlanInput(
            date = date,
            missions = missions,
            milestones = milestoneRepository.getAll(),
            history = History(tasks),
            todayTasks = tasks.filter { it.date == date },
            freeMinutes = plan.freeMinutes,
            energy = Energy.valueOf(plan.energy),
            roomOn = { day -> room[day] ?: 0 },
        )
    }

    /** Free minutes left on each of the 7 days after [from], in date order. */
    private suspend fun roomByDay(
        from: LocalDate,
        missions: List<Mission>? = null,
        tasks: List<TodayTask>? = null,
    ): Map<LocalDate, Int> {
        val minutesById = (missions ?: missionRepository.getActive()).associate { it.missionId to it.durationMinutes }
        val days = (1..7).map { from.plus(it, DateTimeUnit.DAY) }
        val upcoming = tasks ?: todayTaskRepository.getBetween(days.first(), days.last())
        val room = LinkedHashMap<LocalDate, Int>()
        for (day in days) {
            val free = planDao.getPlan(key(day))?.freeMinutes ?: defaultFreeMinutes(day)
            val planned = upcoming
                .filter { it.date == day && !it.isFixedBlock && it.taskState.isOpen }
                .sumOf { minutesById[it.missionId] ?: 0 }
            room[day] = free - planned
        }
        return room
    }

    private suspend fun defaultFreeMinutes(date: LocalDate): Int {
        val weekday = date.dayOfWeek.isoDayNumber
        return planDao.getSettings().firstOrNull { it.weekday == weekday }?.freeMinutes
            ?: DaySettings.defaultFor(weekday)
    }

    private fun newTask(
        mission: Mission,
        date: LocalDate,
        reason: Reason,
        fixed: Boolean,
        order: Int,
        movedFrom: LocalDate? = null,
    ) = TodayTask(
        missionId = mission.missionId,
        taskDate = MyDate.fromLocalDate(date),
        isFixedBlock = fixed,
        reasonCode = reason.code.name,
        reasonArgs = reason.encodeArgs(),
        sortOrder = order,
        movedFromDate = movedFrom?.let { MyDate.fromLocalDate(it) },
    )

    private fun key(date: LocalDate): String = MyDate.fromLocalDate(date).dateString

    companion object {
        const val HISTORY_DAYS = 120
        const val MAX_CROWNS = 3
    }
}
```

- [ ] **Step 2: Create `db/SampleData.kt`**

```kotlin
package com.example.todoapp.db

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Effort
import com.example.todoapp.domain.IfMissed
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.Period
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.ReasonCode
import com.example.todoapp.domain.TaskState
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.TodayTaskRepository

/** Seeds the mockups' example missions while missions can't be created in the app. Plan 3 turns this off. */
const val SEED_SAMPLE_MISSIONS = true

object SampleData {

    suspend fun seed(missionRepository: MissionRepository, todayTaskRepository: TodayTaskRepository) {
        val today = MyDate.now()
        val ids = missions(start = today.subtractDays(120), today = today)
            .associate { it.title to missionRepository.save(it) }
        val history = buildList {
            (1..9).forEach { add(done(ids.getValue("Gym"), today.subtractDays(it), points = 2)) }
            add(done(ids.getValue("Call Grandma"), today.subtractDays(13), points = 2))
            add(done(ids.getValue("Plan winter trip"), today.subtractDays(89), points = 3))
        }
        todayTaskRepository.insertAll(history)
    }

    private fun done(missionId: Long, date: MyDate, points: Int) = TodayTask(
        missionId = missionId,
        taskDate = date,
        state = TaskState.DONE.name,
        pointsEarned = points,
        reasonCode = ReasonCode.ADDED.name,
    )

    private fun missions(start: MyDate, today: MyDate): List<Mission> = listOf(
        mission("Job", PillarType.WEALTH, MissionKind.BLOCK, 3, 240, Effort.STEADY, IfMissed.SAME_DAY, start,
            weekDays = "1,2,3,4,5", startTime = "9:00", endTime = "1:00"),
        mission("App", PillarType.WEALTH, MissionKind.BLOCK, 3, 180, Effort.STEADY, IfMissed.SAME_DAY, start,
            weekDays = "1,2,3,4,5", startTime = "2:00", endTime = "5:00"),
        mission("Gym", PillarType.HEALTH, MissionKind.HABIT, 2, 45, Effort.STEADY, IfMissed.MOVE, start,
            period = Period.DAY),
        mission("Call Mom", PillarType.LOVE, MissionKind.HABIT, 2, 15, Effort.LIGHT, IfMissed.MOVE, start,
            weekDays = "1"),
        mission("Call sister", PillarType.LOVE, MissionKind.HABIT, 2, 15, Effort.LIGHT, IfMissed.MOVE, start,
            weekDays = "2"),
        mission("Call Grandma", PillarType.LOVE, MissionKind.RHYTHM, 2, 15, Effort.LIGHT, IfMissed.MOVE, start,
            intervalCount = 2, intervalUnit = Period.WEEK),
        mission("Guitar", PillarType.LIFE, MissionKind.HABIT, 3, 30, Effort.STEADY, IfMissed.MOVE, start,
            count = 2, goalCount = 3),
        mission("Dance class", PillarType.LIFE, MissionKind.FINISH_LINE, 3, 60, Effort.DRAINING, IfMissed.SAME_DAY, start,
            endDate = today.addDays(56)),
        mission("New ideas", PillarType.LIFE, MissionKind.HABIT, 1, 30, Effort.LIGHT, IfMissed.SKIP, start,
            count = 3),
        mission("Plan winter trip", PillarType.LIFE, MissionKind.RHYTHM, 3, 120, Effort.STEADY, IfMissed.MOVE, start,
            intervalCount = 3, intervalUnit = Period.MONTH),
        mission("New episode", PillarType.RECHARGE, MissionKind.RECHARGE, 0, 45, Effort.LIGHT, IfMissed.SKIP, start,
            count = 2),
    )

    private fun mission(
        title: String,
        pillar: PillarType,
        kind: MissionKind,
        weight: Int,
        minutes: Int,
        effort: Effort,
        ifMissed: IfMissed,
        start: MyDate,
        count: Int = 1,
        period: Period = Period.WEEK,
        goalCount: Int? = null,
        intervalCount: Int = 0,
        intervalUnit: Period = Period.WEEK,
        weekDays: String = "",
        startTime: String? = null,
        endTime: String? = null,
        endDate: MyDate? = null,
    ) = Mission(
        title = title,
        pillar = pillar.name,
        kind = kind.name,
        weight = weight,
        durationMinutes = minutes,
        effort = effort.name,
        ifMissed = ifMissed.name,
        count = count,
        period = period.name,
        goalCount = goalCount,
        intervalCount = intervalCount,
        intervalUnit = intervalUnit.name,
        weekDays = weekDays,
        startTime = startTime,
        endTime = endTime,
        startDate = start,
        endDate = endDate,
    )
}
```

- [ ] **Step 3: Rewrite `screen/init/InitViewModel.kt`**

```kotlin
package com.example.todoapp.screen.init

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.SEED_SAMPLE_MISSIONS
import com.example.todoapp.db.SampleData
import com.example.todoapp.db.models.LOCAL_USER_ID
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayMoodRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InitViewModel(
    private val missionRepository: MissionRepository,
    private val todayTaskRepository: TodayTaskRepository,
    private val planRepository: PlanRepository,
    private val todayMoodRepository: TodayMoodRepository,
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> get() = _isReady

    fun insertDefaultData() {
        viewModelScope.launch {
            todayMoodRepository.insertDefaultMoods(LOCAL_USER_ID)
            planRepository.ensureDefaults()
            if (SEED_SAMPLE_MISSIONS && missionRepository.count() == 0) {
                SampleData.seed(missionRepository, todayTaskRepository)
            }
            _isReady.value = true
        }
    }
}
```

- [ ] **Step 4: Register the plan repository and the new init dependencies in `di/DI.kt`**

Add the import:
```kotlin
import com.example.todoapp.repo.PlanRepository
```
Replace
```kotlin
        single { WaterIntakeRepository(get()) }

        // ViewModels
        single { InitViewModel(get()) }
```
with
```kotlin
        single { WaterIntakeRepository(get()) }
        single { PlanRepository(get(), get(), get(), get()) }

        // ViewModels
        single { InitViewModel(get(), get(), get(), get()) }
```

- [ ] **Step 5: Build**

Run: `./gradlew :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add -A shared
git commit -m "feat: plan repository, day settings and sample missions"
```

---

### Task 10: Diary state and view model

**Files:** create `screen/diary/DiaryState.kt`; rewrite `screen/diary/DiaryViewModel.kt`; edit `di/DI.kt`.

- [ ] **Step 1: Create `screen/diary/DiaryState.kt`**

```kotlin
package com.example.todoapp.screen.diary

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.History
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.Points
import com.example.todoapp.domain.Reason
import com.example.todoapp.domain.ReasonLines
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.date
import com.example.todoapp.domain.pillarType
import com.example.todoapp.domain.shortLabel
import com.example.todoapp.domain.taskState
import com.example.todoapp.domain.weekdayName
import kotlinx.datetime.LocalDate

data class PillarProgress(val pillar: PillarType, val earned: Int, val target: Int) {
    val share: Float get() = if (target == 0) 0f else (earned.toFloat() / target).coerceAtMost(1f)
}

data class PlanRow(
    val taskId: Long,
    val title: String,
    val pillar: PillarType,
    val minutes: Int,
    /** The mission's weight; 0 for recharge. */
    val points: Int,
    val reason: String,
    val state: TaskState,
    val crowned: Boolean,
    val isFixedBlock: Boolean,
    /** Weekday it was moved to, when [state] is MOVED. */
    val movedTo: String?,
)

data class DidntFitRow(val title: String, val text: String)

data class DiaryUiState(
    val pillars: List<PillarProgress>,
    val fixed: List<PlanRow>,
    val picks: List<PlanRow>,
    val didntFit: DidntFitRow?,
    val hasPlan: Boolean,
    val freeMinutes: Int,
    val energy: Energy,
    val plannedMinutes: Int,
    val confirmed: Boolean,
    val pointsToday: Int,
    val pillarsTouched: Int,
    val crownedDone: Int,
    val crownedTotal: Int,
    val todayMissionIds: Set<Long>,
) {
    /** Every open pick resolved as done or did-some; drives the confetti. */
    val allPicksDone: Boolean
        get() = picks.any { it.state.isOpen } && picks.filter { it.state.isOpen }.all { it.state.countsAsDone }
}

object DiaryStateBuilder {

    fun build(date: LocalDate, missions: List<Mission>, tasks: List<TodayTask>, plan: DayPlan?): DiaryUiState {
        val byId = missions.associateBy { it.missionId }
        val history = History(tasks)
        val weekStart = WeekMath.weekStart(date)
        val pillars = PillarType.scoredPillars.map { pillar ->
            PillarProgress(
                pillar = pillar,
                earned = Points.earnedInWeek(missions, tasks, pillar, weekStart),
                target = Points.weeklyTarget(missions, pillar, weekStart, history),
            )
        }
        val todayTasks = tasks.filter { it.date == date }.sortedBy { it.sortOrder }
        val rows = todayTasks.mapNotNull { task -> byId[task.missionId]?.let { toRow(task, it, tasks) } }
        val open = rows.filter { it.state.isOpen }
        val crowned = rows.filter { it.crowned }
        return DiaryUiState(
            pillars = pillars,
            fixed = rows.filter { it.isFixedBlock },
            picks = rows.filter { !it.isFixedBlock },
            didntFit = didntFit(plan, byId, todayTasks),
            hasPlan = plan != null,
            freeMinutes = plan?.freeMinutes ?: 0,
            energy = plan?.let { Energy.valueOf(it.energy) } ?: Energy.NORMAL,
            plannedMinutes = open.filter { !it.isFixedBlock }.sumOf { it.minutes },
            confirmed = plan?.confirmed ?: true,
            pointsToday = todayTasks.sumOf { it.pointsEarned },
            pillarsTouched = rows.filter { it.state.countsAsDone && it.pillar.scored }.map { it.pillar }.distinct().size,
            crownedDone = crowned.count { it.state.countsAsDone },
            crownedTotal = crowned.size,
            todayMissionIds = todayTasks.map { it.missionId }.toSet(),
        )
    }

    private fun toRow(task: TodayTask, mission: Mission, tasks: List<TodayTask>): PlanRow {
        val movedTo = if (task.taskState == TaskState.MOVED) {
            tasks.firstOrNull { it.missionId == task.missionId && it.movedFromDate == task.taskDate }?.date?.weekdayName()
        } else null
        return PlanRow(
            taskId = task.todayTaskId,
            title = mission.title,
            pillar = mission.pillarType,
            minutes = mission.durationMinutes,
            points = mission.weight,
            reason = ReasonLines.render(Reason.decode(task.reasonCode, task.reasonArgs)),
            state = task.taskState,
            crowned = task.crowned,
            isFixedBlock = task.isFixedBlock,
            movedTo = movedTo,
        )
    }

    private fun didntFit(plan: DayPlan?, byId: Map<Long, Mission>, todayTasks: List<TodayTask>): DidntFitRow? {
        val missionId = plan?.didntFitMissionId ?: return null
        if (todayTasks.any { it.missionId == missionId }) return null
        val mission = byId[missionId] ?: return null
        val due = plan.didntFitDue?.let { MyDate(it).toLocalDate() } ?: return null
        val room = plan.didntFitRoomDay?.let { MyDate(it).toLocalDate() }
        val text = "Due by ${due.shortLabel()}." + (room?.let { " ${it.weekdayName()} has room." } ?: "")
        return DidntFitRow(mission.title, text)
    }
}
```

- [ ] **Step 2: Rewrite `screen/diary/DiaryViewModel.kt`**

```kotlin
package com.example.todoapp.screen.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.TaskState
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class DiaryViewModel(
    missionRepository: MissionRepository,
    todayTaskRepository: TodayTaskRepository,
    private val planRepository: PlanRepository,
    private val milestoneRepository: MilestoneRepository,
    private val currentDate: LocalDate,
) : ViewModel() {

    val isToday: Boolean = currentDate == MyDate.now().toLocalDate()

    /** Past days, and today from 18:00, show the evening check. */
    val isEvening: Boolean
        get() {
            val today = MyDate.now().toLocalDate()
            if (currentDate < today) return true
            if (currentDate > today) return false
            return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour >= EVENING_HOUR
        }

    val state: StateFlow<DiaryUiState?> = combine(
        missionRepository.observeActive(),
        todayTaskRepository.observeBetween(
            currentDate.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY),
            currentDate.plus(7, DateTimeUnit.DAY),
        ),
        planRepository.observePlan(currentDate),
    ) { missions, tasks, plan ->
        DiaryStateBuilder.build(currentDate, missions, tasks, plan)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val missions: StateFlow<List<Mission>> = missionRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val milestones: StateFlow<List<MilestoneWithDetails>> = milestoneRepository
        .observeInRange(MyDate.fromLocalDate(currentDate).subtractDays(7), MyDate.fromLocalDate(currentDate).addDays(7))
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            if (isToday) {
                planRepository.ensurePlanned(currentDate)
                if (isEvening) planRepository.tickFixedBlocks(currentDate)
            }
        }
    }

    fun toggleDone(taskId: Long) = act { planRepository.toggleDone(taskId) }
    fun didSome(taskId: Long) = act { planRepository.setState(taskId, TaskState.PARTIAL) }
    fun moveTask(taskId: Long) = act { planRepository.moveToNextRoom(taskId) }
    fun skipTask(taskId: Long) = act { planRepository.setState(taskId, TaskState.SKIPPED) }
    fun toggleCrown(taskId: Long) = act { planRepository.toggleCrown(taskId) }
    fun swap(taskId: Long) = act { planRepository.swap(taskId) }
    fun removeTask(taskId: Long) = act { planRepository.removeTask(taskId) }
    fun addMission(missionId: Long) = act { planRepository.addMission(currentDate, missionId) }
    fun lighterDay() = act { planRepository.lighterDay(currentDate) }
    fun changeDay(freeMinutes: Int, energy: Energy) = act { planRepository.changeDay(currentDate, freeMinutes, energy) }
    fun confirm() = act { planRepository.confirm(currentDate) }

    fun updateMilestoneStatus(milestone: Milestone, achieved: Boolean, completionDate: LocalDate) = act {
        val updated = if (achieved) {
            milestone.copy(status = TaskStatus.COMPLETED.value, actualCompletionDate = MyDate.fromLocalDate(completionDate))
        } else {
            milestone.copy(status = TaskStatus.IN_PROGRESS.value, actualCompletionDate = null)
        }
        milestoneRepository.updateMilestone(updated)
    }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        const val EVENING_HOUR = 18
    }
}
```

- [ ] **Step 3: Update the diary factory in `di/DI.kt`**

Replace
```kotlin
        factory { (day: LocalDate) -> DiaryViewModel(get(), day) }
```
with
```kotlin
        factory { (day: LocalDate) -> DiaryViewModel(get(), get(), get(), get(), day) }
```

- [ ] **Step 4: Build**

Run: `./gradlew :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL` (the interim `DiaryScreen` still only reads `milestones`, which the new view model keeps).

- [ ] **Step 5: Commit**

```bash
git add -A shared
git commit -m "feat: diary state built from missions, tasks and the day plan"
```

---

### Task 11: Diary plan components

**Files:** create `screen/diary/plan/PillarWeekStrip.kt`, `screen/diary/plan/PlanSection.kt`, `screen/diary/plan/PlanNotes.kt`, `screen/diary/plan/PlanDialogs.kt`.

- [ ] **Step 1: Create `screen/diary/plan/PillarWeekStrip.kt`**

```kotlin
package com.example.todoapp.screen.diary.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.TrackBeige
import com.example.todoapp.screen.basicutils.pillarStrong
import com.example.todoapp.screen.diary.PillarProgress

/** "This week" bars under the date: points earned against each pillar's target. */
@Composable
fun PillarWeekStrip(pillars: List<PillarProgress>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("THIS WEEK", fontSize = 11.sp, letterSpacing = 1.2.sp, color = InkGray)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pillars.forEach { progress ->
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(progress.pillar.title, fontSize = 12.sp, color = Color(0xFF4A4A4A))
                        Text("${progress.earned}/${progress.target}", fontSize = 12.sp, color = Color(0xFF4A4A4A))
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(7.dp)
                            .clip(RoundedCornerShape(4.dp)).background(TrackBeige)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(progress.share).fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp)).background(pillarStrong(progress.pillar))
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Create `screen/diary/plan/PlanSection.kt`**

```kotlin
package com.example.todoapp.screen.diary.plan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.minutesLabel
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.ChipBorder
import com.example.todoapp.screen.basicutils.CrownGold
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.LineBlueBorder
import com.example.todoapp.screen.basicutils.Orange180
import com.example.todoapp.screen.basicutils.Orange80
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarLight
import com.example.todoapp.screen.basicutils.pillarStrong
import com.example.todoapp.screen.diary.DiaryUiState
import com.example.todoapp.screen.diary.PlanRow
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.check_square
import todoapp.shared.generated.resources.crown_a
import todoapp.shared.generated.resources.square_b

class PlanRowActions(
    val onToggle: (Long) -> Unit,
    val onCrown: (Long) -> Unit,
    val onSwap: (Long) -> Unit,
    val onRemove: (Long) -> Unit,
    val onDidSome: (Long) -> Unit,
    val onMove: (Long) -> Unit,
    val onSkip: (Long) -> Unit,
)

/** "Picked for you · 2 h 15 min of your 2½ h free" and a link to change the day. */
@Composable
fun PlanSubline(state: DiaryUiState, onChange: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val energy = if (state.energy == Energy.LOW) " · low energy" else ""
        Text(
            "Picked for you · ${minutesLabel(state.plannedMinutes)} of your ${minutesLabel(state.freeMinutes)} free$energy",
            fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.Center
        )
        Text(
            "change",
            fontSize = 13.sp, fontStyle = FontStyle.Italic, color = PeachText,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { onChange() }.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PlanSection(state: DiaryUiState, evening: Boolean, editMode: Boolean, actions: PlanRowActions) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(1.dp, LineBlueBorder, RoundedCornerShape(8.dp))
            .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 10.dp)
    ) {
        if (state.fixed.isNotEmpty()) {
            GroupLabel("ALREADY IN YOUR DAY")
            state.fixed.forEach { PlanRowItem(it, evening, editMode, actions) }
        }
        GroupLabel(if (evening) "TODAY" else "PICKED FOR TODAY")
        if (state.picks.isEmpty()) {
            Text(
                "Nothing picked yet. Tap the pencil to add something.",
                fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }
        state.picks.forEach { PlanRowItem(it, evening, editMode, actions) }
        if (!evening) {
            Text(
                "Hold a title to crown it.",
                fontSize = 11.sp, color = InkGray, modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(text, fontSize = 11.sp, letterSpacing = 1.2.sp, color = InkGray, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanRowItem(row: PlanRow, evening: Boolean, editMode: Boolean, actions: PlanRowActions) {
    val done = row.state.countsAsDone
    val closed = !row.state.isOpen
    val strong = pillarStrong(row.pillar)
    Row(
        modifier = Modifier.fillMaxWidth()
            .drawBehind {
                drawLine(Blue80, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            }
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.size(width = 36.dp, height = 30.dp)) {
            Image(
                painter = painterResource(if (done) Res.drawable.check_square else Res.drawable.square_b),
                contentDescription = if (done) "Untick ${row.title}" else "Tick ${row.title}",
                colorFilter = ColorFilter.tint(strong),
                modifier = Modifier.align(Alignment.CenterStart).size(24.dp)
                    .clickable(enabled = !closed) { actions.onToggle(row.taskId) }
            )
            if (row.crowned) {
                Image(
                    painter = painterResource(Res.drawable.crown_a),
                    contentDescription = "Crowned",
                    colorFilter = ColorFilter.tint(CrownGold),
                    modifier = Modifier.align(Alignment.TopEnd).size(width = 18.dp, height = 14.dp)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f)
                .pointerInput(row.taskId) { detectTapGestures(onLongPress = { actions.onCrown(row.taskId) }) }
        ) {
            Text(
                row.title,
                fontSize = 20.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light, letterSpacing = 0.6.sp,
                color = if (closed) InkGray else InkBlack,
                modifier = Modifier.drawWithContent {
                    drawContent()
                    if (done) {
                        val y = size.height * 0.58f
                        drawLine(strong, Offset(0f, y), Offset(size.width, y), strokeWidth = 3.dp.toPx())
                    }
                }
            )
            Text(subline(row), fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, lineHeight = 17.sp)
            if (evening && row.state == TaskState.PLANNED && !row.isFixedBlock) {
                FlowRow(modifier = Modifier.padding(top = 4.dp)) {
                    val half = (row.points + 1) / 2
                    SmallChip(if (row.pillar.scored) "Did some · +$half" else "Did some") { actions.onDidSome(row.taskId) }
                    SmallChip("Move it") { actions.onMove(row.taskId) }
                    SmallChip("Skip this week") { actions.onSkip(row.taskId) }
                }
            }
            if (editMode && row.state == TaskState.PLANNED && !row.isFixedBlock) {
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    SmallChip("Swap") { actions.onSwap(row.taskId) }
                    SmallChip("Remove") { actions.onRemove(row.taskId) }
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp, top = 3.dp)) {
            Text(minutesLabel(row.minutes), fontSize = 11.sp, color = InkGray)
            Spacer(Modifier.height(5.dp))
            Text(
                if (row.pillar.scored) "+${row.points}" else "recharge",
                fontSize = 12.sp, fontWeight = FontWeight.Medium, color = pillarInk(row.pillar),
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(pillarLight(row.pillar))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

private fun subline(row: PlanRow): String = when (row.state) {
    TaskState.MOVED -> "Moved to ${row.movedTo ?: "another day"}"
    TaskState.SKIPPED -> "Skipped this week"
    else -> row.reason
}

/** Small rounded choice used for evening options, swap mode and the energy picker. */
@Composable
fun SmallChip(text: String, selected: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 13.sp,
        color = Color(0xFF4A4A4A),
        modifier = Modifier.padding(top = 6.dp, end = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, if (selected) Orange180 else ChipBorder, RoundedCornerShape(16.dp))
            .background(if (selected) Orange80 else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}
```

- [ ] **Step 3: Create `screen/diary/plan/PlanNotes.kt`**

```kotlin
package com.example.todoapp.screen.diary.plan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.CardCream
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.Orange180
import com.example.todoapp.screen.basicutils.PeachInk
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.StickyYellow
import com.example.todoapp.screen.diary.DiaryUiState
import com.example.todoapp.screen.diary.DidntFitRow
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.crown_a
import todoapp.shared.generated.resources.diary_heart_closed
import todoapp.shared.generated.resources.flag_open
import todoapp.shared.generated.resources.heart_filled
import todoapp.shared.generated.resources.star_filled

/** Slightly tilted sticky note, like the mission cards. */
@Composable
fun StickyNote(rotation: Float, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 12.dp)
            .rotate(rotation)
            .shadow(6.dp, RoundedCornerShape(4.dp))
            .background(StickyYellow, RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) { content() }
}

@Composable
fun DidntFitNote(row: DidntFitRow) {
    StickyNote(rotation = -1.2f) {
        Row(verticalAlignment = Alignment.Top) {
            Image(
                painterResource(Res.drawable.flag_open), contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFFB8692E)), modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("DIDN'T FIT TODAY", fontSize = 11.sp, letterSpacing = 1.2.sp, color = Color(0xFF6A5A2A))
                Text(row.title, fontSize = 19.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light, color = InkBlack)
                Text(row.text, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = Color(0xFF5F5A48))
            }
        }
    }
}

/** Morning buttons: Swap one, Lighter day, Looks good. */
@Composable
fun PlanActions(editMode: Boolean, onEdit: () -> Unit, onLighter: () -> Unit, onLooksGood: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        PlanButton(if (editMode) "Done swapping" else "Swap one", filled = false, onClick = onEdit)
        PlanButton("Lighter day", filled = false, onClick = onLighter)
        PlanButton("Looks good", filled = true, onClick = onLooksGood)
    }
}

@Composable
private fun PlanButton(text: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(if (filled) Modifier.background(Orange180) else Modifier.border(1.5.dp, Orange180, RoundedCornerShape(10.dp)))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (filled) PeachInk else PeachText)
    }
}

/** The evening card, in the style of the old metrics highlights. Never interprets titles. */
@Composable
fun DayHighlightsCard(state: DiaryUiState, date: LocalDate) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(CardCream, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${date.dayOfMonth} ${date.month.name}",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3A3A3A),
                modifier = Modifier.weight(1f)
            )
            Image(
                painterResource(Res.drawable.diary_heart_closed), contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFF6B6B6B)), modifier = Modifier.size(30.dp)
            )
        }
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(Color(0xFFDDD6C2)))
        HighlightLine(Res.drawable.star_filled, "+${state.pointsToday} points today")
        HighlightLine(
            Res.drawable.heart_filled,
            "${state.pillarsTouched} of ${PillarType.scoredPillars.size} pillars got something."
        )
        HighlightLine(
            Res.drawable.crown_a,
            if (state.crownedTotal == 0) "No crowns today." else "${state.crownedDone} of ${state.crownedTotal} crowned done."
        )
    }
}

@Composable
private fun HighlightLine(icon: DrawableResource, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .background(Color(0xFFFDFAF1), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(icon), contentDescription = null,
            colorFilter = ColorFilter.tint(Color(0xFF3A3A3A)), modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(text, fontSize = 15.sp, color = InkBlack)
    }
}
```

- [ ] **Step 4: Create `screen/diary/plan/PlanDialogs.kt`**

```kotlin
package com.example.todoapp.screen.diary.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.minutesLabel
import com.example.todoapp.domain.missionKind
import com.example.todoapp.domain.pillarType
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.pillarStrong

/** "change" on the diary: today's free time and energy. Saving re-picks the open items. */
@Composable
fun DayCapacityDialog(freeMinutes: Int, energy: Energy, onSave: (Int, Energy) -> Unit, onDismiss: () -> Unit) {
    var minutes by remember { mutableStateOf(freeMinutes) }
    var level by remember { mutableStateOf(energy) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(minutes, level) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Your day") },
        text = {
            Column {
                Text("Free time today", fontSize = 14.sp, color = InkGray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { minutes = (minutes - 30).coerceAtLeast(0) }) { Text("−30 min") }
                    Text(minutesLabel(minutes), fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    TextButton(onClick = { minutes = (minutes + 30).coerceAtMost(16 * 60) }) { Text("+30 min") }
                }
                Spacer(Modifier.height(12.dp))
                Text("Energy", fontSize = 14.sp, color = InkGray)
                Row {
                    Energy.entries.forEach { e -> SmallChip(e.label, selected = e == level) { level = e } }
                }
            }
        },
        containerColor = Light_Yellowww
    )
}

/** The pencil on TO DO LIST: add any mission that isn't on today's list yet. */
@Composable
fun AddMissionDialog(missions: List<Mission>, todayMissionIds: Set<Long>, onAdd: (Long) -> Unit, onDismiss: () -> Unit) {
    val available = missions
        .filter { it.missionKind != MissionKind.BLOCK && it.missionId !in todayMissionIds }
        .sortedBy { it.pillarType.ordinal }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Add to today") },
        text = {
            if (available.isEmpty()) {
                Text("Everything is already on today's list.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(available, key = { it.missionId }) { mission ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onAdd(mission.missionId) }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(pillarStrong(mission.pillarType)))
                            Spacer(Modifier.width(10.dp))
                            Text(mission.title, fontSize = 17.sp, fontStyle = FontStyle.Italic, modifier = Modifier.weight(1f))
                            Text(minutesLabel(mission.durationMinutes), fontSize = 12.sp, color = InkGray)
                        }
                    }
                }
            }
        },
        containerColor = Light_Yellowww
    )
}
```

- [ ] **Step 5: Build**

Run: `./gradlew :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add -A shared
git commit -m "feat: diary plan components (week strip, rows, notes, dialogs)"
```

---

### Task 12: Wire the plan into the diary

**Files:** rewrite `screen/diary/DiaryScreen.kt`.

- [ ] **Step 1: Rewrite `screen/diary/DiaryScreen.kt`**

```kotlin
package com.example.todoapp.screen.diary

import KottieAnimation
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.Orange80
import com.example.todoapp.screen.basicutils.Red80
import com.example.todoapp.screen.basicutils.Yellow180
import com.example.todoapp.screen.diary.diaryitem.components.DiaryMilestoneSection
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeViewModel
import com.example.todoapp.screen.diary.plan.AddMissionDialog
import com.example.todoapp.screen.diary.plan.DayCapacityDialog
import com.example.todoapp.screen.diary.plan.DayHighlightsCard
import com.example.todoapp.screen.diary.plan.DidntFitNote
import com.example.todoapp.screen.diary.plan.PillarWeekStrip
import com.example.todoapp.screen.diary.plan.PlanActions
import com.example.todoapp.screen.diary.plan.PlanRowActions
import com.example.todoapp.screen.diary.plan.PlanSection
import com.example.todoapp.screen.diary.plan.PlanSubline
import kotlinx.datetime.LocalDate
import kottieAnimationState.KottieAnimationState
import kottieComposition.KottieCompositionSpec
import kottieComposition.animateKottieCompositionAsState
import kottieComposition.rememberKottieComposition
import org.jetbrains.compose.resources.ExperimentalResourceApi
import todoapp.shared.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DiaryScreen(
    diaryViewModel: DiaryViewModel,
    moodViewModel: MoodViewModel,
    waterIntakeViewModel: WaterIntakeViewModel,
    selection: LocalDate,
    changeDate: (LocalDate) -> Unit
) {
    val state by diaryViewModel.state.collectAsState()
    val missions by diaryViewModel.missions.collectAsState()
    val milestones by diaryViewModel.milestones.collectAsState()
    val todayMoodList: List<TodayMoodWithDetails>? by moodViewModel.todayMoodStatuses.collectAsState(emptyList())
    val waterIntake: WaterIntake? by waterIntakeViewModel.waterIntake.collectAsState()

    val evening = diaryViewModel.isEvening
    var editMode by remember(selection) { mutableStateOf(false) }
    var showCapacity by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }

    var confettiJson by remember { mutableStateOf("") }
    var waterJson by remember { mutableStateOf("") }
    var confettiPlaying by remember { mutableStateOf(false) }
    var waterPlaying by remember { mutableStateOf(false) }
    var wasAllDone by remember(selection) { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(selection) {
        moodViewModel.loadDailyMoodStatus(selection)
        waterIntakeViewModel.loadWaterIntake(selection)
    }
    LaunchedEffect(Unit) {
        confettiJson = Res.readBytes("files/confetti-big-animation.json").decodeToString()
        waterJson = Res.readBytes("files/water-bubbles-animation.json").decodeToString()
    }
    // Confetti when the last open pick gets done in this session, not when opening a finished day.
    LaunchedEffect(state?.allPicksDone) {
        val allDone = state?.allPicksDone ?: return@LaunchedEffect
        if (wasAllDone == false && allDone) confettiPlaying = true
        wasAllDone = allDone
    }

    val confetti = rememberKottieComposition(spec = KottieCompositionSpec.File(confettiJson))
    val confettiState by animateKottieCompositionAsState(composition = confetti, speed = 2.5f, isPlaying = confettiPlaying)
    val water = rememberKottieComposition(spec = KottieCompositionSpec.File(waterJson))
    val waterState by animateKottieCompositionAsState(composition = water, speed = 2.5f, isPlaying = waterPlaying)
    LaunchedEffect(confettiState.isCompleted) { if (confettiState.isCompleted) confettiPlaying = false }
    LaunchedEffect(waterState.isCompleted) { if (waterState.isCompleted) waterPlaying = false }

    val actions = remember(diaryViewModel) {
        PlanRowActions(
            onToggle = diaryViewModel::toggleDone,
            onCrown = diaryViewModel::toggleCrown,
            onSwap = diaryViewModel::swap,
            onRemove = diaryViewModel::removeTask,
            onDidSome = diaryViewModel::didSome,
            onMove = diaryViewModel::moveTask,
            onSkip = diaryViewModel::skipTask,
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Light_Yellowww)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            DateHeaderItem(selection, changeDate = changeDate)
            state?.let { s -> item { PillarWeekStrip(s.pillars) } }
            HeadingText("MOOD", Red80)
            MoodHeaderItem(todayMoodList, selection, upsertMood = { moodViewModel.upsertTodayMood(it) })
            ToDoListHeader("TO DO LIST", Orange80, editClicked = { showAdd = true })
            state?.let { s ->
                if (s.hasPlan) item { PlanSubline(s, onChange = { showCapacity = true }) }
                item { PlanSection(s, evening, editMode, actions) }
                s.didntFit?.let { row -> item { DidntFitNote(row) } }
                if (diaryViewModel.isToday && !evening && !s.confirmed) {
                    item {
                        PlanActions(
                            editMode = editMode,
                            onEdit = { editMode = !editMode },
                            onLighter = { diaryViewModel.lighterDay() },
                            onLooksGood = {
                                editMode = false
                                diaryViewModel.confirm()
                            }
                        )
                    }
                }
                if (evening) item { DayHighlightsCard(s, selection) }
            }
            HeadingText("WATER INTAKE", Blue80)
            WaterIntakeHeaderItem(
                waterIntake, selection,
                upsertWaterIntake = { waterIntakeViewModel.upsertWaterIntake(it) },
                waterIntakeCompleted = { waterPlaying = it }
            )
            if (milestones.isNotEmpty()) {
                HeadingText("MILESTONE", Orange80)
                DiaryMilestoneSection(milestones = milestones, onMilestoneChecked = { mwd, achieved ->
                    diaryViewModel.updateMilestoneStatus(mwd.milestone, achieved, selection)
                })
            }
            HeadingText("NOTES", Yellow180)
            Notes(waterIntake, selection, upsertWaterIntake = { waterIntakeViewModel.upsertWaterIntake(it) })
            item { Spacer(Modifier.height(120.dp)) }
        }
        if (confettiPlaying) AnimationConfetti(confetti, confettiState)
        if (waterPlaying) AnimationWater(water, waterState)
    }

    state?.let { s ->
        if (showCapacity) {
            DayCapacityDialog(
                freeMinutes = s.freeMinutes,
                energy = s.energy,
                onSave = { minutes, energy ->
                    showCapacity = false
                    diaryViewModel.changeDay(minutes, energy)
                },
                onDismiss = { showCapacity = false }
            )
        }
        if (showAdd) {
            AddMissionDialog(
                missions = missions,
                todayMissionIds = s.todayMissionIds,
                onAdd = { diaryViewModel.addMission(it) },
                onDismiss = { showAdd = false }
            )
        }
    }
}

@Composable
private fun AnimationWater(composition: Any?, animationState: KottieAnimationState) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent), contentAlignment = Alignment.Center) {
        KottieAnimation(
            composition = composition,
            progress = { animationState.progress },
            modifier = Modifier.fillMaxSize().background(Color.Transparent)
        )
    }
}

@Composable
private fun AnimationConfetti(composition: Any?, animationState: KottieAnimationState) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent), contentAlignment = Alignment.Center) {
        KottieAnimation(
            composition = composition,
            progress = { animationState.progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add -A shared
git commit -m "feat: diary builds and shows today's list with reasons, points and evening check"
```

- [ ] **Step 4: Hand over for manual testing**

Tell the owner the build is ready, and that the first launch wipes the old database and seeds the sample missions. On a weekday before 18:00 the diary should show:
- **Already in your day:** Job (9:00 to 1:00) and App (2:00 to 5:00).
- **Picked for today:** about 3 h of picks, led by Gym ("Every day · 9-day streak"), then Call Grandma ("Last done 13 days ago · you aim for every 2 weeks"), Guitar, New ideas and New episode (recharge, because the blocks total 7 h).
- **Didn't fit today:** Plan winter trip, with a weekend day named as having room.

The owner checks the rest: ticking, long-press crowns, Swap one, Lighter day, "change", the pencil, and the evening options (by opening a past day or after 18:00).

---

### Task 13: Bring the spec in line with what was built

**Files:** modify `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`.

- [ ] **Step 1: Replace the four data-model rows in section 10**

Replace the rows that start `| \`Mission\` |`, `| \`MissionFrequency\` |`, `| \`Pillar\` |` and `| \`TodayTask\` |` with:
```markdown
| `Mission` | One table holds everything: title, `pillar` (enum name, including RECHARGE), `kind`, `weight` (0–3), `durationMinutes`, `effort`, `ifMissed`, pace fields (`count` + `period`, `goalCount`, `intervalCount` + `intervalUnit`, `weekDays`, `startTime`, `endTime`), `startDate`, `endDate`, `pausedUntil`, `motivation`, `active`. The old `mission_frequency`, `pillar` and `mission_pillar_mapping` tables are gone; pillars are a fixed enum in code. `ideaId` comes with the ideas library. |
| `TodayTask` | `state` (PLANNED, DONE, PARTIAL, MOVED, SKIPPED), `pointsEarned`, `isFixedBlock`, `crowned` (replaces the TOP3 tag), `movedFromDate`, `reasonCode`, `reasonArgs`, `sortOrder`. The reminder and sub-task tables are dropped. |
```

- [ ] **Step 2: Note the plan-2 tables and the two extra reason codes**

In the same table, replace the rows starting `| New \`WeekSummary\` |` and `| New \`PaceOffer\` |` with:
```markdown
| New `WeekSummary` | Added in plan 2 (week cycle): week start, per-pillar earned and target points, balanced flag. |
| New `PaceOffer` | Added in plan 2: mission, offer type, week shown, response, snooze-until date. |
```

In section 7.2, add two rows to the end of the templates table:
```markdown
| RHYTHM_FIRST | Every so often, never done yet | First one · you aim for every {interval} |
| ADDED | Added by hand from the pencil | Added by you |
```

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md
git commit -m "docs: sync redesign spec with plan 1 schema"
```

---

## What comes next (separate plans)

- **Plan 2, week cycle:**
  - Monday close-out with the no-debt rule and a stored `WeekSummary`.
  - The "Last week" card.
  - Pace offers as sticky notes (slipping, gentle start, going well).
  - Resolving unresolved items at midnight by their "if a day goes sideways" answer.
  - Small catch-ups from the Plan 1 review: the pencil's list grouped by pillar with each candidate's reason line, and "+N extra" on a pillar's bar when you do more than planned.
- **Plan 3, creating missions:**
  - The nine-question sentence and the before-saving week cost.
  - The Missions screen grouped by pillar, with free-hours settings per weekday.
  - Turns `SEED_SAMPLE_MISSIONS` off.
- **Plan 4, metrics:**
  - Pillar rings, patterns and the Monday-first calendar with point-sized day rings.
  - One progress line per mission.
- **Next pass:** the ideas library, write-your-own matching and the review screen.
