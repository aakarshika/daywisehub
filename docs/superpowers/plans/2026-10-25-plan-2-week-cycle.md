# Plan 2: The Week Cycle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close each day and each week on their own. What a day left open moves or drops by its mission's "if a day goes sideways" answer. Each finished week is stored as a summary. Monday's diary shows a "Last week" card and up to two pace-offer stickies. The small diary catch-ups from Plans 1 and 3 are finished too. The database moves to version 6 and keeps real data from here on.

**Architecture:** New pure-Kotlin rules live in `domain`:
- `DayClose` decides what an unfinished item does.
- `Moves` finds where "Move it" goes. The repository and the diary both use it.
- `WeekSummaries` builds and reads the week rows.
- `PaceOffers` holds the triggers, the change each offer makes, and its words.

`PlanRepository.ensurePlanned` runs the close-out inside its existing `write` lock, before it builds today's list. It ticks fixed blocks on earlier days, resolves still-planned items, and stores missing week summaries. On the first open of a week it also makes that week's pace offers. The two new tables (`week_summary`, `pace_offer`) arrive through `@AutoMigration(from = 5, to = 6)` and are read through a lock-free `WeekRepository`. `DiaryStateBuilder` gains the Monday card, the offers and the evening details. The new composables live in `screen/diary/plan/WeekNotes.kt`.

**Tech Stack:** Kotlin Multiplatform (Kotlin 2.0.20, language level 1.9), Compose Multiplatform 1.7.1 (material3, foundation), Room KMP 2.7.0-alpha01 with KSP and the Room Gradle plugin, Koin 3.2, precompose 1.5.7, kotlinx-datetime.

**Spec:** `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`, sections 5 (with 5.1 and 5.2), 6.3, 6.4, 7.1, 9, 9.1, 10 and 14.

**Builds on:** Plan 1 (`docs/superpowers/plans/2026-10-25-plan-1-foundation-and-daily-list.md`) and Plan 3 (`docs/superpowers/plans/2026-10-25-plan-3-missions.md`), on branch `redesign/plan-3-missions` at `b095601` (Plan 3 with its review fixes and follow-ups).

**Ground rules for this plan**
- **No automated tests.** The owner tests by hand. Every task ends with a green build and a commit.
- **Build command.** `./gradlew` doesn't work here (the wrapper jar is missing). From the repo root run
  `/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60`
  and expect `BUILD SUCCESSFUL`. Tasks 1 and 9 also compile for iOS.
- **Paths** are relative to `shared/src/commonMain/kotlin/com/example/todoapp/` unless they start with `shared/` or `docs/`.
- **Commits** stage files by path, never `git add -A`. Task 1 commits the generated `shared/schemas/com.example.todoapp.db.AppDatabase/6.json`, and the review fixes after Task 8 regenerate it once, while version 6 is still unreleased. After that, a build must not change anything under `shared/schemas`. If one rewrites `4.json` or `5.json` (it has rewritten `4.json` before), restore it with `git checkout`.
- **Every task builds on its own, in order.** Each task was checked by applying Tasks 1..N, as written here, to a clean checkout of `b095601` and building.
- **Find/replace edits** quote the current text exactly. Each "Replace" block appears once in its file at that point.
- **Repository writes.** Every new write runs inside `PlanRepository`'s `write` helper: `NonCancellable` on `Dispatchers.Default`, behind a Mutex that isn't reentrant. Code inside a write calls only private helpers and lock-free repositories (`WeekRepository`, `TodayTaskRepository`, `MissionRepository.save`). It never calls another public `PlanRepository` function. Diary view models are cancelled when the screen goes away, so this also keeps answers and moves from being cut short.

## Database version 6

**What changes**
- Two new tables: `week_summary` (one row per finished week) and `pace_offer`.
- Two new columns, folded in by the review fixes after Task 8 while version 6 was still unreleased (no device had run it):
  - `mission.mission_pace_since`: nullable text, the day the pace or weight last changed.
  - `today_task.task_auto_ticked`: integer, `DEFAULT 0`, true for a block the evening check ticked.
- A unique index on `pace_offer` (`offer_week`, `offer_mission_id`), also from the review fixes: one offer per mission a week. It replaces the plain `offer_week` index, since its first column serves the same lookups.
- `@Database(version = 6, autoMigrations = [AutoMigration(from = 5, to = 6)])`.
- `5.json` is already exported and committed (since `f98520e`). `room { schemaDirectory("$projectDir/schemas") }` is already set in `shared/build.gradle.kts`. So the migration has its starting schema and needs no extra setup.
- Both platform `DB.kt` builders drop `fallbackToDestructiveMigration(true)`. In its place:
  - `fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4)`: pre-redesign databases are still wiped.
  - `fallbackToDestructiveMigrationOnDowngrade(true)`: installing an older build wipes too.
- From version 5, Room runs the auto migration. If a migration from 5 or later is ever missing, the app stops with an error instead of silently wiping real data.

**What was verified on Room KMP 2.7.0-alpha01**
1. **Runtime.** Read in the `room-runtime` sources for Android and for jvmNative/iOS:
   - Both builders call `validateAutoMigrations`, which adds the generated migrations to the migration container.
   - Both have `fallbackToDestructiveMigrationFrom(Boolean, vararg Int)` and `fallbackToDestructiveMigrationOnDowngrade(Boolean)`.
   - After migrating, Room checks the tables against the entities and fails loudly on a difference.
2. **Compiler.** KSP generates `AppDatabase_AutoMigration_5_6_Impl` for Android (`ksp/android/androidDebug`) and for iOS (`ksp/iosSimulatorArm64`). Each `AppDatabase_Impl.createAutoMigrations` returns it, and the two files' SQL is identical.
3. **SQL.** The generated statements were replayed on a real SQLite database, again after the review fixes:
   - The database was built from `5.json`, with one mission and one task row in it.
   - The two `ALTER TABLE … ADD COLUMN` statements come first, then the two new tables and their indices.
   - After the statements, every version 6 table and index matched `6.json`. The one difference is Room's own `DEFAULT NULL` on `mission_pace_since`, which Room's check ignores for a column whose entity has no default.
   - The other six version 5 tables were unchanged, the rows were kept (`mission_pace_since` null, `task_auto_ticked` 0), the new foreign key worked, and a second offer for the same mission and week was refused.
4. **Schema.** The exported `6.json` has `"identityHash": "b9b1d9a31de7e79cab75843c598556dc"`, the same in the Android and iOS `AppDatabase_Impl`. Task 1 as written here gave `912cb1183037244fa7d4b13b4959fb56`; the review fixes changed it.
5. **Transactions** (for the review fixes' `TodayTaskDao.move` and `takeBack`). A `@Transaction` suspend function with a body compiles, for Android and iOS alike, to `performInTransactionSuspending(__db) { super.move(…) }`. With `setDriver`, both platforms run that as an `IMMEDIATE` transaction on the writer connection, and the inner insert, update and delete reuse that connection. A DAO transaction needs no schema change.

## Spec 5.1, no debt: the targets already do it

Nothing in `domain/Points.kt` changes. The no-debt rules already follow from how targets are computed:
- **Points don't carry over.** `Points.earnedInWeek` adds up only tasks dated inside that week.
- **Next week's targets don't grow after a miss.** `Points.weeklyTarget` uses each mission's pace (`occurrencesInWeek`, a count capped at its live days), never what was done or missed before.
- **Weekly quotas start fresh.** The planner's QUOTA line counts what's done from this Monday, so "2 of 3" last week is "0 of 3" again. The spec says the planner then "favors it early in the week". There's no separate bonus for that: a quota habit is a candidate from Monday on, so it's picked early whenever it fits.
- **Due items stay due.** `Points.dueDate` reads the last time done before the week. So an every-so-often mission missed last week is overdue. `occurrencesInWeek` counts it in this week's target, and the planner ranks it as overdue.
- **"Has to be that day" and "skip it" drop.** The day close (Task 2) marks them skipped, worth 0, and no target reads skipped items.

So the week close-out only has to resolve what the days left open, then store a summary.

## Decisions this plan makes

The spec leaves these open. Each is easy to change later.

1. **When days close.** The close-out runs in `ensurePlanned`, on the first diary open of a new day (the diary also moves to the new day by itself after midnight). It runs again on every open, but it only touches what's still open, so a second run does nothing. The order is:
   1. Tick the fixed blocks on any earlier day that has a plan, not only days of this week.
   2. Resolve still-planned items.
   3. Store missing week summaries, and make pace offers on the first open of a week.
   4. Build today's list.

   It closes against today at the latest, never a day still to come. A failure in step 3 doesn't stop today's list: summaries and offers are worked out from what's stored, so the next open makes them. The failure is logged as a warning (changed after review).
2. **Unfinished items** follow `ifMissed`:
   - "Move it" uses the same rule as the evening's Move: the next day with room, from today on. Only days the mission is live on count. When it isn't live on any of the 7 days (it ends or is paused), the item is skipped (changed after review).
   - The copy and the moved mark are written in one transaction (`TodayTaskDao.move`), so a move is never half done (after review).
   - "It has to be that day" and "skip it" become skipped. So does any item whose mission is archived, paused or ended, and a fixed block left unticked in the evening check.
   - Three refinements, so moves never pile up. Items are resolved newest first. An item whose mission already has an open item from today on is skipped, so one catch-up is enough. An item more than 6 days old is skipped, so "Moved from Friday" is never weeks old.
   - A day that was never opened has no plan, so its blocks aren't ticked. It can still hold items moved into it, and those are resolved like any other (corrected after review).
3. **Unticking on an earlier day marks the item skipped,** not planned. An earlier day is closed, so an untick there doesn't come back as unfinished and get moved into today. Ticking a skipped item still works. After review:
   - Ticking a moved item takes the move back while its copy is still planned: the copy goes, and the item is done on its own day, in one transaction (`TodayTaskDao.takeBack`). Once the copy is ticked, skipped or moved on, the moved item can't be ticked. The row shows which: a box that takes the move back is less faded, and the row reads "Moved to Thursday · tick to take it back". A screen reader hears the other kind as "⟨title⟩, moved".
   - A skipped item keeps its crown, so ticking it again brings the crown back, unless the day has filled its 3 crowns meanwhile. Only a move takes it otherwise.
   - Adding a mission with the pencil on an earlier day adds it as done, with its points.
4. **Week summaries.** Every finished week from the week of the first task gets one row, keyed by its Monday.
   - Targets come from the active missions at close-out and stay fixed, so a pace change accepted later doesn't rewrite an old week.
   - Points count archived missions too, matching the diary's bars since the Plan 3 review.
   - A tick or untick later on a day of a closed week updates that week's points; its targets stay.
   - A week the app was never opened in still gets a row: 0 points against its targets.
   - A week with no target at all earns no crown (changed after review).
5. **Offers are made once a week:** on the first open of a new week, when last week's summary is written. They show on that day's diary (today's page, when today opens the week). An offer ignored that day doesn't show again later in the week. If its trigger still holds, it can come back the next Monday. After review:
   - Saving a new pace or weight in the editor withdraws that mission's unanswered offers this week (`WITHDRAWN`): they were judged on the old pace.
   - A finish line that ends before the end of this week gets no offer.
6. **Picking offers.** At most 2 a week, one per mission. Easing-off offers come first (lower, then lengthen), then stepping up (gentle start, then steady habit). Within a type, missions go in the order they were made. Any answer restarts that mission's run of weeks: only weeks from the answered offer's week on count. So "Keep 3×" asks again only after another 3 slipped weeks, and a step-up needs a fresh 3 or 4 full weeks. "Not yet" also holds that offer back 4 weeks. After review, offers judge only the current pace:
   - A change of kind, pace (count, period, goal, interval, days) or weight sets the mission's `paceSince` to that day, in the editor or by an accepted offer. Only weeks starting on or after it count, so a week changed partway through doesn't.
   - A week with no items of its own (the app wasn't opened) breaks a run, so a holiday doesn't read as three slipped weeks. Items moved into a week don't count, since a move can land in a week the app is then never opened in.
7. **Trigger words.**
   - **Slipped:** done less than half as often as it was due that week. A week it wasn't due (not started yet, paused) breaks the run.
   - **Full:** at least 80% of its own points that week, as for a pillar. The points are worked out from what was done, at today's weight, not read from the stored points (changed after review).
   - **Late:** done in a later week than the one it was due in. One still not done after its week has finished counts as late too.
   - **Free hours left:** this week's free time minus what its flexible missions need, at least one more session of the habit. Two step-ups in one week share it: the second needs room for its session after the first's.
8. **One step.**
   - **Lower:** every day becomes 6× a week, and N× a week becomes N−1×. A once-a-week habit isn't offered. Going to 1×, the button reads "Make it once a week", as the sentence does.
   - **Lengthen:** one more day, week or month, within the editor's limits (30 days, 12 weeks, 3 months).
   - **Gentle step-up:** one more a week, up to the goal. The goal is cleared once it's reached, so the card drops "up to" (changed after review: it went straight to the goal).
   - **Steady step-up:** +1 a week, up to 6×, for weekly habits that aren't starting gently.
   - Monthly habits get no offers yet.
9. **Pause a month** sets `pausedUntil` to the same date next month. Like archiving, it removes the mission's still-planned items from today on. Accepting or pausing then re-picks today's open items, as a mission save does, unless the day was confirmed with "Looks good". As with archiving, a failed re-pick doesn't undo the answer. After review:
   - A finish line whose end comes before the month is up has no "Pause a month".
   - An answer the sticky doesn't offer does nothing, and so does any answer once the mission is archived.
   - A "yes" that no longer changes anything leaves the offer unanswered.
10. **The "Last week" card** shows on the day that opens the week: Monday, or the first day the diary was opened that week. So a past Monday's page shows its own look back. It's hidden when last week had no target and no points. After review, a day that was never opened and isn't Monday doesn't open the week, so the card isn't repeated on it.
    - The points line caps each pillar at its target and shows the rest as "+N extra", because extra never fills another pillar (spec 5). When no pillar had a target it reads just "+N extra", not "0 of 0".
    - "What the app adjusted" lists up to 3 things: items "move it" carried from last week into this week, and every-so-often missions due last week and not done. Otherwise it says "Nothing to catch up on." A move that landed after this week isn't counted.
    - The adjusted lines read today's missions, so they show only on today's page. So does the strip's "A FRESH START": an earlier day, or a past week, isn't a fresh start. An earlier day's card has just the points and pillars lines (changed after review).
    - The mockup's "Love was light, so ⟨Call sister⟩ is set for Tuesday" isn't used. It claims a reason the app doesn't have.
11. **Look.** These follow the mockup:
    - An easing-off offer is a yellow sticky with the wand and "A SMALL CHANGE?".
    - A step-up is a blue sticky with the flag and "GOING WELL".
    - Buttons are the existing peach `PlanButton` (at least 44dp tall, a button to a screen reader).
    - Titles in these sentences sit on chips in their pillar's light color (spec 7.1). The words around a chip never depend on the title. After review each chip has a 1dp outline in the pillar's strong color, since a Life chip on the blue sticky had almost no contrast, and every line box is the same height so a chip on a one-line sentence isn't cramped (the same fix as Plan 3's tap-to-jump). The line height is merged into the theme's text style, which keeps its letter spacing.
    - After review, a sticky dims to 60% once answered, then fades, and the rest slide up. A sticky's tilt follows its offer, so the one left doesn't jump. Each sticky reads as one group to a screen reader, with its header and "LAST WEEK" as headings.
12. **The pencil** lists every live mission that isn't a fixed block and isn't on the day's list.
    - Groups follow the pillar order, Recharge last. Today's candidates come first in each group, with the reason the planner would give.
    - The rest say where they stand: "3 of 3 this week · 4 days left", "Last done 3 days ago · you aim for every 2 weeks", "Every day · 5-day streak", "You usually take 2 breaks a week."
    - Paused and not-yet-started missions are now left out. The list ignores the day's free time, energy and swapped-out missions on purpose: it is for adding by hand.
    - After review, when nothing can be added it says so without guessing why ("Nothing else to add. Fixed blocks, paused and not-yet-started missions aren't listed."), and the day's own list says "Tap the pencil to see what can be added". On another day the dialog reads "Add to this day". The pencil is a 44dp button, and a screen reader hears "Added" after a tap: the line for it is there from the start, empty, so its first change is announced. If the list can't load, a notice says so. The diary's notices are announced too.
13. **"Move to Thursday"** is worked out with the same `Moves` functions the move itself uses, so the chip and the result agree. From an earlier day's page it can read "Move to today". After review:
    - It reads "next Thursday" when the day is the same weekday as the page's day, a week on, so it isn't taken for the page's own day. The "Moved to" line reads the same way.
    - When the mission is live on none of the next 7 days, a move would skip it, so the chip is left out. "Skip today" is still there.
    - A page older than yesterday names no day: its planned items were settled long ago, and it reads only a week past itself.
    - The days to move to are cut out of the task list once, and only when a planned flexible item needs them.
    - An item carried to the weekday it came from reads "Moved from last Thursday", so it isn't taken for its own day. This is worked out when the line is shown, from the stored weekday and the item's date.
    - "Did some" and "Skip today" do nothing to an item that's already moved, so a fast second tap after "Move to …" doesn't count it twice.
14. **"Ticked for you"** shows on a done fixed block the evening check ticked (`TodayTask.autoTicked`). Any change by hand clears it, so a block unticked and ticked again by hand loses the label (changed after review: it read `plan.fixedTicked`, which a hand tick kept).
15. **The evening check at 18:00.** While today's diary stays open, the view model looks at the clock every 30 seconds and opens the evening check once, at 18:00. It looks every 30 seconds rather than waiting until 18:00 because timers pause while a phone sleeps. If you tap "Back to the plan" after that, it stays closed. Swap mode and the "change" dialog close when the evening check opens. After review the last wait is cut short, so it opens at 18:00 rather than up to 30 seconds late. Closing the check and then switching tabs or turning the phone opens it again: a new screen starts in the evening.
16. **A seed for trying offers.** `SampleData` stays off. Turned on, it now writes 3 gentle weeks of Guitar and a 21-day Gym streak. So the first open shows both kinds of offer, the same on any weekday.

## File map

**Created**

| File | Responsibility |
|---|---|
| `db/data/week/WeekSummary.kt` | One finished week: earned and target points per pillar, balanced flag |
| `db/data/week/PaceOffer.kt` | One offer: mission, type, week, response, snooze-until |
| `db/data/week/WeekDao.kt` | Queries for both tables |
| `shared/schemas/com.example.todoapp.db.AppDatabase/6.json` | Generated by the build in Task 1, committed |
| `domain/Moves.kt` | Room per day and the day "Move it" goes to |
| `domain/DayClose.kt` | What a still-planned item from an earlier day does |
| `domain/WeekSummaries.kt` | Building a summary, refreshing its points, reading its pillar columns |
| `repo/WeekRepository.kt` | Lock-free reads and writes of summaries and offers |
| `domain/PaceOffers.kt` | Offer types and answers, triggers, the change each makes, the sticky's words |
| `screen/diary/WeekState.kt` | `LastWeekState`, `OfferNote`, `DiaryExtras` and their builder |
| `screen/diary/plan/WeekNotes.kt` | Title chips in text, the Last week card, the offer sticky |

**Edited:** `db/Database.kt`, `db/data/mission/Mission.kt` and `db/data/todotask/TodayTask.kt` (review fixes), `domain/MissionDraft.kt` and `screen/missions/editor/MissionEditorViewModel.kt` (review fixes), `shared/src/androidMain/kotlin/com/example/todoapp/db/DB.kt`, `shared/src/iosMain/kotlin/com/example/todoapp/db/DB.kt`, `db/data/plan/PlanDao.kt`, `db/data/todotask/TodayTaskDao.kt`, `repo/TodayTaskRepository.kt`, `repo/PlanRepository.kt`, `di/DI.kt`, `db/SampleData.kt`, `domain/Planner.kt`, `screen/diary/DiaryState.kt`, `screen/diary/DiaryViewModel.kt`, `screen/diary/DiaryScreen.kt`, `screen/basicutils/AppColors.kt`, `screen/diary/plan/PlanNotes.kt`, `screen/diary/plan/PillarWeekStrip.kt`, `screen/diary/plan/PlanDialogs.kt`, `screen/diary/plan/PlanSection.kt`, `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`.

**Unchanged on purpose:**
- `domain/Points.kt`: the no-debt rules already hold.
- `repo/MissionRepository.kt`: an accepted offer saves through its lock-free `save`.
- The Missions screen: a paused mission already reads "Paused until …" there.

---

### Task 1: Database version 6 with an auto migration

**Files:**
- Create: `db/data/week/PaceOffer.kt`, `db/data/week/WeekDao.kt`, `db/data/week/WeekSummary.kt`
- Modify: `db/Database.kt`, `shared/src/androidMain/kotlin/com/example/todoapp/db/DB.kt`, `shared/src/iosMain/kotlin/com/example/todoapp/db/DB.kt`
- Generated, committed: `shared/schemas/com.example.todoapp.db.AppDatabase/6.json`

**Depends on:** nothing (start of the plan).

Adds the two week tables and moves the database to version 6 without losing data. Nothing reads or writes the tables yet. The decisions and checks behind this are in "Database version 6" above.

- [ ] **Step 1: Create `db/data/week/PaceOffer.kt`**

```kotlin
package com.example.todoapp.db.data.week

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoapp.db.data.mission.Mission

/**
 * A pace change offered on a week's first diary day (spec 5.2). [type] and [response] store
 * domain.OfferType and domain.OfferResponse names; [response] stays null until it's answered.
 * [week] and [snoozeUntil] are Mondays, yyyy-MM-dd. "Not yet" sets [snoozeUntil].
 */
@Entity(
    tableName = "pace_offer",
    foreignKeys = [ForeignKey(
        entity = Mission::class,
        parentColumns = ["mission_id"],
        childColumns = ["offer_mission_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["offer_mission_id"]), Index(value = ["offer_week"])]
)
data class PaceOffer(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "offer_id") val offerId: Long = 0,
    @ColumnInfo(name = "offer_mission_id") val missionId: Long,
    @ColumnInfo(name = "offer_type") val type: String,
    @ColumnInfo(name = "offer_week") val week: String,
    @ColumnInfo(name = "offer_response") val response: String? = null,
    @ColumnInfo(name = "offer_snooze_until") val snoozeUntil: String? = null,
)
```


- [ ] **Step 2: Create `db/data/week/WeekDao.kt`**

```kotlin
package com.example.todoapp.db.data.week

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WeekDao {

    @Upsert
    suspend fun upsertSummary(summary: WeekSummary)

    @Query("SELECT * FROM week_summary WHERE summary_week_start = :weekStart")
    suspend fun getSummary(weekStart: String): WeekSummary?

    @Query("SELECT * FROM week_summary WHERE summary_week_start = :weekStart")
    fun observeSummary(weekStart: String): Flow<WeekSummary?>

    /** Every stored week, oldest first. Plan 4's metrics read this. */
    @Query("SELECT * FROM week_summary ORDER BY summary_week_start")
    fun observeSummaries(): Flow<List<WeekSummary>>

    /** Week starts that already have a summary, from [from] on. Dates are yyyy-MM-dd, so text order is date order. */
    @Query("SELECT summary_week_start FROM week_summary WHERE summary_week_start >= :from")
    suspend fun getSummaryWeeksFrom(from: String): List<String>

    @Insert
    suspend fun insertOffers(offers: List<PaceOffer>)

    @Update
    suspend fun updateOffer(offer: PaceOffer)

    @Query("SELECT * FROM pace_offer WHERE offer_id = :offerId")
    suspend fun getOffer(offerId: Long): PaceOffer?

    @Query("SELECT * FROM pace_offer WHERE offer_week = :week ORDER BY offer_id")
    suspend fun getOffers(week: String): List<PaceOffer>

    @Query("SELECT * FROM pace_offer WHERE offer_week = :week ORDER BY offer_id")
    fun observeOffers(week: String): Flow<List<PaceOffer>>

    @Query("SELECT * FROM pace_offer ORDER BY offer_week, offer_id")
    suspend fun getAllOffers(): List<PaceOffer>
}
```


- [ ] **Step 3: Create `db/data/week/WeekSummary.kt`**

```kotlin
package com.example.todoapp.db.data.week

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One finished week (spec 5.1), stored the first time the diary opens in a later week.
 * [weekStart] is its Monday, yyyy-MM-dd. Earned points add up the week's tasks; targets are
 * computed at close-out, so a pace change accepted later doesn't rewrite an old week.
 * Read the pillar columns through domain/WeekSummaries.kt.
 */
@Entity(tableName = "week_summary")
data class WeekSummary(
    @PrimaryKey @ColumnInfo(name = "summary_week_start") val weekStart: String,
    @ColumnInfo(name = "summary_health_earned") val healthEarned: Int,
    @ColumnInfo(name = "summary_health_target") val healthTarget: Int,
    @ColumnInfo(name = "summary_wealth_earned") val wealthEarned: Int,
    @ColumnInfo(name = "summary_wealth_target") val wealthTarget: Int,
    @ColumnInfo(name = "summary_love_earned") val loveEarned: Int,
    @ColumnInfo(name = "summary_love_target") val loveTarget: Int,
    @ColumnInfo(name = "summary_life_earned") val lifeEarned: Int,
    @ColumnInfo(name = "summary_life_target") val lifeTarget: Int,
    /** Every pillar full (spec 5): the week gets a crown. */
    @ColumnInfo(name = "summary_balanced") val balanced: Boolean,
)
```


- [ ] **Step 4: `db/Database.kt`: Add the imports for the new tables and auto migrations**

Replace:

```kotlin
package com.example.todoapp.db

import androidx.room.Database
```

with:

```kotlin
package com.example.todoapp.db

import androidx.room.AutoMigration
import androidx.room.Database
```


- [ ] **Step 5: `db/Database.kt`: Import the new tables**

Replace:

```kotlin
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.db.data.water.WaterIntakeDao
```

with:

```kotlin
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.db.data.water.WaterIntakeDao
import com.example.todoapp.db.data.week.PaceOffer
import com.example.todoapp.db.data.week.WeekDao
import com.example.todoapp.db.data.week.WeekSummary
```


- [ ] **Step 6: `db/Database.kt`: Version 6 with the two week tables and an auto migration from 5**

Replace:

```kotlin
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
```

with:

```kotlin
/**
 * Version 5 replaced the old mission model; anything older is wiped (see the platform DB.kt files).
 * From version 5 on, data is kept: each new version adds an auto migration from the one before,
 * generated from the exported schemas in shared/schemas. Version 6 adds the week tables.
 */
@Database(
    entities = [
        Mission::class,
        Milestone::class,
        TodayTask::class,
        DayPlan::class,
        DaySettings::class,
        Mood::class,
        TodayMood::class,
        WaterIntake::class,
        WeekSummary::class,
        PaceOffer::class],
    version = 6,
    autoMigrations = [AutoMigration(from = 5, to = 6)]
)
```


- [ ] **Step 7: `db/Database.kt`: Expose the week DAO**

Replace:

```kotlin
    abstract fun getWaterIntakeDao(): WaterIntakeDao
}
```

with:

```kotlin
    abstract fun getWaterIntakeDao(): WaterIntakeDao
    abstract fun getWeekDao(): WeekDao
}
```


- [ ] **Step 8: `shared/src/androidMain/kotlin/com/example/todoapp/db/DB.kt`: Keep data from version 5 on**

Replace:

```kotlin
    )
        .fallbackToDestructiveMigration(true)
        .setDriver(BundledSQLiteDriver())
```

with:

```kotlin
    )
        // Real data is kept from version 5 on: 5 → 6 is an auto migration (Database.kt), and a missing
        // migration from 5 or later stops with an error instead of wiping. Databases from before the
        // redesign (1 to 4) are wiped, and so is one from a newer build when an older build is installed.
        .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4)
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .setDriver(BundledSQLiteDriver())
```


- [ ] **Step 9: `shared/src/iosMain/kotlin/com/example/todoapp/db/DB.kt`: Keep data from version 5 on**

Replace:

```kotlin
    )
        .fallbackToDestructiveMigration(true)
        .setDriver(BundledSQLiteDriver())
```

with:

```kotlin
    )
        // Real data is kept from version 5 on: 5 → 6 is an auto migration (Database.kt), and a missing
        // migration from 5 or later stops with an error instead of wiping. Databases from before the
        // redesign (1 to 4) are wiped, and so is one from a newer build when an older build is installed.
        .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4)
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .setDriver(BundledSQLiteDriver())
```


- [ ] **Step 10: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 11: Check the migration was generated, for Android and iOS**

Run from the repo root:

```bash
GEN=shared/build/generated/ksp
grep -c "CREATE TABLE IF NOT EXISTS" $GEN/android/androidDebug/kotlin/com/example/todoapp/db/AppDatabase_AutoMigration_5_6_Impl.kt
grep -c "AppDatabase_AutoMigration_5_6_Impl()" $GEN/android/androidDebug/kotlin/com/example/todoapp/db/AppDatabase_Impl.kt
grep '"identityHash"' shared/schemas/com.example.todoapp.db.AppDatabase/6.json
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :shared:kspKotlinIosSimulatorArm64 --console=plain 2>&1 | tail -3
grep -c "AppDatabase_AutoMigration_5_6_Impl()" $GEN/iosSimulatorArm64/iosSimulatorArm64Main/kotlin/com/example/todoapp/db/AppDatabase_Impl.kt
```

Expected, in order:
- `2`: the migration creates the two tables.
- `1`: Android's database registers it.
- `"identityHash": "912cb1183037244fa7d4b13b4959fb56",` for Task 1 as written here. The review fixes after Task 8 fold two columns and a unique index into version 6, which changes it to `"identityHash": "b9b1d9a31de7e79cab75843c598556dc",` (see "Database version 6").
- `BUILD SUCCESSFUL`
- `1`: iOS registers it too.

A different hash means an entity differs from this plan: fix the entity, not the JSON.

- [ ] **Step 12: Commit**

Task 1 adds `6.json`; nothing else under `shared/schemas` may change. Check, then stage by path:

```bash
git status --short shared/schemas        # expected: only ?? .../6.json
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/db/data/week/PaceOffer.kt \
  $K/db/data/week/WeekDao.kt \
  $K/db/data/week/WeekSummary.kt \
  $K/db/Database.kt \
  shared/src/androidMain/kotlin/com/example/todoapp/db/DB.kt \
  shared/src/iosMain/kotlin/com/example/todoapp/db/DB.kt \
  shared/schemas/com.example.todoapp.db.AppDatabase/6.json
git commit -m "feat: database version 6 with week summaries and pace offers, kept by an auto migration"
```


---

### Task 2: Closing earlier days

**Files:**
- Create: `domain/DayClose.kt`, `domain/Moves.kt`
- Modify: `db/data/plan/PlanDao.kt`, `db/data/todotask/TodayTaskDao.kt`, `repo/TodayTaskRepository.kt`, `repo/PlanRepository.kt`

**Depends on:** Task 1 (none of its types, only the order).

On the first open of a new day, `ensurePlanned` now closes what earlier days left open (decisions 1 to 3):
- Fixed blocks on any earlier day with a plan start ticked. Before, only days of this week were ticked; `getUntickedBefore` covers older weeks too.
- Items still planned there follow their mission's "if a day goes sideways" answer.

The evening's "Move it" moves through the same private `moveTask` helper, and the room per day moves to `domain/Moves.kt`, so Task 8 can name the target day on the chip with the same rule. Unticking on an earlier day now marks the item skipped.

- [ ] **Step 1: Create `domain/DayClose.kt`**

```kotlin
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
```


- [ ] **Step 2: Create `domain/Moves.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Where "Move it" sends a planned item (spec 6.4). The diary uses this to name the day on the chip,
 * and PlanRepository to do the move, so the two always agree.
 */
object Moves {

    /** Moves look at the 7 days after this one: the item's own day, or yesterday for anything older, so nothing lands in the past. */
    fun anchor(from: LocalDate, today: LocalDate): LocalDate = maxOf(from, today.minus(1, DateTimeUnit.DAY))

    /**
     * Free minutes left on each of the 7 days after [from], in date order: that day's free time
     * ([freeOn]) minus its open flexible items. Fixed blocks don't use free time.
     */
    fun roomByDay(
        from: LocalDate,
        minutesById: Map<Long, Int>,
        tasks: List<TodayTask>,
        freeOn: (LocalDate) -> Int,
    ): Map<LocalDate, Int> {
        val room = LinkedHashMap<LocalDate, Int>()
        for (i in 1..7) {
            val day = from.plus(i, DateTimeUnit.DAY)
            val planned = tasks
                .filter { it.date == day && !it.isFixedBlock && it.taskState.isOpen }
                .sumOf { minutesById[it.missionId] ?: 0 }
            room[day] = freeOn(day) - planned
        }
        return room
    }

    /**
     * The first day in [room] with time for [mission] that doesn't already have it open;
     * the first day after [after] when none has.
     */
    fun target(mission: Mission, after: LocalDate, tasks: List<TodayTask>, room: Map<LocalDate, Int>): LocalDate =
        room.entries.firstOrNull { (day, left) ->
            left >= mission.durationMinutes &&
                tasks.none { it.date == day && it.missionId == mission.missionId && it.taskState.isOpen }
        }?.key ?: after.plus(1, DateTimeUnit.DAY)
}
```


- [ ] **Step 3: `db/data/plan/PlanDao.kt`: Find earlier days whose fixed blocks haven't been ticked**

Replace:

```kotlin
    @Upsert
    suspend fun upsertPlan(plan: DayPlan)
```

with:

```kotlin
    @Upsert
    suspend fun upsertPlan(plan: DayPlan)

    /** Days before [date] that have a plan but whose fixed blocks haven't been ticked yet. */
    @Query("SELECT * FROM day_plan WHERE plan_date < :date AND plan_fixed_ticked = 0 ORDER BY plan_date")
    suspend fun getUntickedBefore(date: String): List<DayPlan>
```


- [ ] **Step 4: `db/data/todotask/TodayTaskDao.kt`: Find items still planned on earlier days, newest first**

Replace:

```kotlin
    @Query("SELECT * FROM today_task WHERE today_task_id = :taskId")
    suspend fun get(taskId: Long): TodayTask?
```

with:

```kotlin
    @Query("SELECT * FROM today_task WHERE today_task_id = :taskId")
    suspend fun get(taskId: Long): TodayTask?

    /** Items still planned on a day before [date], newest day first. */
    @Query("SELECT * FROM today_task WHERE task_date < :date AND task_state = 'PLANNED' ORDER BY task_date DESC, task_order")
    suspend fun getPlannedBefore(date: String): List<TodayTask>
```


- [ ] **Step 5: `repo/TodayTaskRepository.kt`: Expose it**

Replace:

```kotlin
    suspend fun get(taskId: Long): TodayTask? = todayTaskDao.get(taskId)
```

with:

```kotlin
    suspend fun get(taskId: Long): TodayTask? = todayTaskDao.get(taskId)

    suspend fun getPlannedBefore(date: LocalDate): List<TodayTask> = todayTaskDao.getPlannedBefore(key(date))
```


- [ ] **Step 6: `repo/PlanRepository.kt`: Import the close-out rules**

Replace:

```kotlin
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Energy
```

with:

```kotlin
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.DayClose
import com.example.todoapp.domain.Energy
```


- [ ] **Step 7: `repo/PlanRepository.kt`: Import Moves**

Replace:

```kotlin
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.PlanInput
```

with:

```kotlin
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.Moves
import com.example.todoapp.domain.PlanInput
```


- [ ] **Step 8: `repo/PlanRepository.kt`: Close earlier days before building today's list**

Replace:

```kotlin
    /** Builds the day's list the first time it's asked for; later calls do nothing. */
    suspend fun ensurePlanned(date: LocalDate) = write {
        // Fixed blocks start ticked, so earlier days this week score even if their evening check never opened.
        var day = WeekMath.weekStart(date)
        while (day < date) { tickBlocks(day); day = day.plus(1, DateTimeUnit.DAY) }
        if (planDao.getPlan(key(date)) != null) return@write
```

with:

```kotlin
    /**
     * Closes what earlier days left open, then builds the day's list the first time it's asked for.
     * Safe on every open: closing only touches what's still open, and a built day isn't rebuilt.
     */
    suspend fun ensurePlanned(date: LocalDate) = write {
        closePastDays(date)
        if (planDao.getPlan(key(date)) != null) return@write
```


- [ ] **Step 9: `repo/PlanRepository.kt`: Unticking on an earlier day marks the item skipped**

Replace:

```kotlin
    suspend fun toggleDone(taskId: Long) = write {
        val task = todayTaskRepository.get(taskId) ?: return@write
        updateState(task, if (task.taskState.countsAsDone) TaskState.PLANNED else TaskState.DONE)
    }
```

with:

```kotlin
    suspend fun toggleDone(taskId: Long) = write {
        val task = todayTaskRepository.get(taskId) ?: return@write
        // An earlier day is closed: unticking there skips the item, so the next open doesn't carry it forward again.
        val unticked = if (task.date < today()) TaskState.SKIPPED else TaskState.PLANNED
        updateState(task, if (task.taskState.countsAsDone) unticked else TaskState.DONE)
    }
```


- [ ] **Step 10: `repo/PlanRepository.kt`: Move through a shared helper**

Replace:

```kotlin
    /** Moves a planned pick to the next day with room, from today on (the first of those days if none in the week). */
    suspend fun moveToNextRoom(taskId: Long) = write {
        val task = todayTaskRepository.get(taskId) ?: return@write
        if (task.taskState != TaskState.PLANNED || task.isFixedBlock) return@write
        val mission = missionRepository.get(task.missionId) ?: return@write
        val from = task.date
        val after = maxOf(from, today().minus(1, DateTimeUnit.DAY))
        val upcoming = todayTaskRepository.getBetween(after.plus(1, DateTimeUnit.DAY), after.plus(7, DateTimeUnit.DAY))
        val target = roomByDay(after, tasks = upcoming).entries
            .firstOrNull { (day, room) ->
                room >= mission.durationMinutes &&
                    upcoming.none { it.date == day && it.missionId == mission.missionId && it.taskState.isOpen }
            }?.key
            ?: after.plus(1, DateTimeUnit.DAY)
        val order = (upcoming.filter { it.date == target }.maxOfOrNull { it.sortOrder } ?: -1) + 1
        todayTaskRepository.update(task.copy(state = TaskState.MOVED.name, pointsEarned = 0, crowned = false))
        todayTaskRepository.insert(
            newTask(mission, target, Reason(ReasonCode.MOVED, listOf(from.weekdayName())), fixed = false, order = order, movedFrom = from)
        )
    }
```

with:

```kotlin
    /** Moves a planned pick to the next day with room, from today on (the first of those days if none in the week). */
    suspend fun moveToNextRoom(taskId: Long) = write {
        val task = todayTaskRepository.get(taskId) ?: return@write
        if (task.taskState != TaskState.PLANNED || task.isFixedBlock) return@write
        val mission = missionRepository.get(task.missionId) ?: return@write
        moveTask(task, mission, today())
    }
```


- [ ] **Step 11: `repo/PlanRepository.kt`: The close-out of earlier days and the shared move**

Replace:

```kotlin
    private suspend fun currentPlan(date: LocalDate): DayPlan =
```

with:

```kotlin
    /**
     * Spec 6.4, at the first open of a new day: fixed blocks on earlier days start ticked, then items
     * still planned there follow their "if a day goes sideways" answer. A day that was never opened
     * has no plan and no items, so there's nothing to close. Call only inside [write].
     */
    private suspend fun closePastDays(today: LocalDate) {
        planDao.getUntickedBefore(key(today)).forEach { tickBlocks(MyDate(it.date).toLocalDate()) }
        resolveUnfinished(today)
    }

    /** Items still planned on earlier days follow "if a day goes sideways". Call only inside [write]. */
    private suspend fun resolveUnfinished(today: LocalDate) {
        val unfinished = todayTaskRepository.getPlannedBefore(today)
        if (unfinished.isEmpty()) return
        val missions = missionRepository.getActive().associateBy { it.missionId }
        // Newest first, so when one mission has several, the latest is the one carried forward.
        for (task in unfinished) {
            val mission = missions[task.missionId]
            val coming = todayTaskRepository.getBetween(today, today.plus(7, DateTimeUnit.DAY))
                .any { it.missionId == task.missionId && it.taskState.isOpen }
            if (mission != null && DayClose.outcome(task, mission, today, alreadyComing = coming) == DayClose.Outcome.MOVE) {
                moveTask(task, mission, today)
            } else {
                todayTaskRepository.update(task.copy(state = TaskState.SKIPPED.name, pointsEarned = 0, crowned = false))
            }
        }
    }

    /** Moves [task] to the next day with room after [Moves.anchor]. Call only inside [write]. */
    private suspend fun moveTask(task: TodayTask, mission: Mission, today: LocalDate) {
        val from = task.date
        val after = Moves.anchor(from, today)
        val upcoming = todayTaskRepository.getBetween(after.plus(1, DateTimeUnit.DAY), after.plus(7, DateTimeUnit.DAY))
        val target = Moves.target(mission, after, upcoming, roomByDay(after, tasks = upcoming))
        val order = (upcoming.filter { it.date == target }.maxOfOrNull { it.sortOrder } ?: -1) + 1
        todayTaskRepository.update(task.copy(state = TaskState.MOVED.name, pointsEarned = 0, crowned = false))
        todayTaskRepository.insert(
            newTask(mission, target, Reason(ReasonCode.MOVED, listOf(from.weekdayName())), fixed = false, order = order, movedFrom = from)
        )
    }

    private suspend fun currentPlan(date: LocalDate): DayPlan =
```


- [ ] **Step 12: `repo/PlanRepository.kt`: Room per day comes from Moves**

Replace:

```kotlin
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
```

with:

```kotlin
        val minutesById = (missions ?: missionRepository.getActive()).associate { it.missionId to it.durationMinutes }
        val days = (1..7).map { from.plus(it, DateTimeUnit.DAY) }
        val upcoming = tasks ?: todayTaskRepository.getBetween(days.first(), days.last())
        val free = days.associateWith { day -> planDao.getPlan(key(day))?.freeMinutes ?: defaultFreeMinutes(day) }
        return Moves.roomByDay(from, minutesById, upcoming, freeOn = { free[it] ?: 0 })
    }
```


- [ ] **Step 13: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 14: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (it has rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/domain/DayClose.kt \
  $K/domain/Moves.kt \
  $K/db/data/plan/PlanDao.kt \
  $K/db/data/todotask/TodayTaskDao.kt \
  $K/repo/TodayTaskRepository.kt \
  $K/repo/PlanRepository.kt
git commit -m "feat: close earlier days by each mission's sideways answer"
```


---

### Task 3: A summary for every finished week

**Files:**
- Create: `domain/WeekSummaries.kt`, `repo/WeekRepository.kt`
- Modify: `db/data/todotask/TodayTaskDao.kt`, `repo/TodayTaskRepository.kt`, `repo/PlanRepository.kt`, `di/DI.kt`

**Depends on:** Task 1 (`WeekSummary`, `WeekDao`), Task 2 (`closePastDays`).

After the days are closed, every finished week from the week of the first task gets a `WeekSummary`, once (decision 4). The summary's targets come from the active missions at that moment and then stay fixed. Its points count archived missions too, as the diary's bars have since the Plan 3 review. A later tick or untick on a closed week's day updates that week's points. `WeekRepository` is created whole here, with the offer queries Task 4 uses.

- [ ] **Step 1: Create `domain/WeekSummaries.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.db.models.MyDate
import kotlinx.datetime.LocalDate

val WeekSummary.weekStartDate: LocalDate get() = MyDate(weekStart).toLocalDate()

fun WeekSummary.earned(pillar: PillarType): Int = when (pillar) {
    PillarType.HEALTH -> healthEarned
    PillarType.WEALTH -> wealthEarned
    PillarType.LOVE -> loveEarned
    PillarType.LIFE -> lifeEarned
    PillarType.RECHARGE -> 0
}

fun WeekSummary.target(pillar: PillarType): Int = when (pillar) {
    PillarType.HEALTH -> healthTarget
    PillarType.WEALTH -> wealthTarget
    PillarType.LOVE -> loveTarget
    PillarType.LIFE -> lifeTarget
    PillarType.RECHARGE -> 0
}

/** Pillars at 80% of their target or more; a pillar with nothing due counts as full (spec 5). */
fun WeekSummary.pillarsFull(): Int = PillarType.scoredPillars.count { Points.isFull(earned(it), target(it)) }

/**
 * Spec 5.1. Nothing carries over between weeks: earned points are the week's own tasks, and each
 * week's target comes from the missions' paces, never from what was missed the week before.
 */
object WeekSummaries {

    /**
     * The finished week starting [weekStart]. Targets come from the active [missions] as they stand at
     * close-out; points from the week's tasks of [allMissions], archived too, as in the diary's bars.
     */
    fun summarize(weekStart: LocalDate, missions: List<Mission>, allMissions: List<Mission>, history: History): WeekSummary {
        val earned = PillarType.scoredPillars.associateWith { Points.earnedInWeek(allMissions, history.all, it, weekStart) }
        val target = PillarType.scoredPillars.associateWith { Points.weeklyTarget(missions, it, weekStart, history) }
        fun e(pillar: PillarType) = earned.getValue(pillar)
        fun t(pillar: PillarType) = target.getValue(pillar)
        return WeekSummary(
            weekStart = MyDate.fromLocalDate(weekStart).dateString,
            healthEarned = e(PillarType.HEALTH),
            healthTarget = t(PillarType.HEALTH),
            wealthEarned = e(PillarType.WEALTH),
            wealthTarget = t(PillarType.WEALTH),
            loveEarned = e(PillarType.LOVE),
            loveTarget = t(PillarType.LOVE),
            lifeEarned = e(PillarType.LIFE),
            lifeTarget = t(PillarType.LIFE),
            balanced = PillarType.scoredPillars.all { Points.isFull(e(it), t(it)) },
        )
    }

    /**
     * [summary] with its points added up again from [tasks] of [allMissions] (after a tick on one of its
     * days). The targets stay as stored.
     */
    fun withEarned(summary: WeekSummary, allMissions: List<Mission>, tasks: List<TodayTask>): WeekSummary {
        val start = summary.weekStartDate
        fun e(pillar: PillarType) = Points.earnedInWeek(allMissions, tasks, pillar, start)
        val updated = summary.copy(
            healthEarned = e(PillarType.HEALTH),
            wealthEarned = e(PillarType.WEALTH),
            loveEarned = e(PillarType.LOVE),
            lifeEarned = e(PillarType.LIFE),
        )
        return updated.copy(balanced = PillarType.scoredPillars.all { Points.isFull(updated.earned(it), updated.target(it)) })
    }
}
```


- [ ] **Step 2: Create `repo/WeekRepository.kt`**

```kotlin
package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.week.PaceOffer
import com.example.todoapp.db.data.week.WeekDao
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.db.models.MyDate
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Week summaries and pace offers. Nothing here locks: PlanRepository calls the writes inside its own
 * write, together with the task and mission changes they go with.
 */
class WeekRepository(private val database: AppDatabase) {

    private val weekDao: WeekDao by lazy { database.getWeekDao() }

    suspend fun getSummary(weekStart: LocalDate): WeekSummary? = weekDao.getSummary(key(weekStart))

    fun observeSummary(weekStart: LocalDate): Flow<WeekSummary?> = weekDao.observeSummary(key(weekStart))

    /** Every stored week, oldest first. */
    fun observeSummaries(): Flow<List<WeekSummary>> = weekDao.observeSummaries()

    /** Mondays from [from] on that already have a summary. */
    suspend fun summaryWeeksFrom(from: LocalDate): Set<LocalDate> =
        weekDao.getSummaryWeeksFrom(key(from)).map { MyDate(it).toLocalDate() }.toSet()

    suspend fun saveSummary(summary: WeekSummary) = weekDao.upsertSummary(summary)

    fun observeOffers(week: LocalDate): Flow<List<PaceOffer>> = weekDao.observeOffers(key(week))

    suspend fun getOffers(week: LocalDate): List<PaceOffer> = weekDao.getOffers(key(week))

    suspend fun getAllOffers(): List<PaceOffer> = weekDao.getAllOffers()

    suspend fun getOffer(offerId: Long): PaceOffer? = weekDao.getOffer(offerId)

    suspend fun insertOffers(offers: List<PaceOffer>) = weekDao.insertOffers(offers)

    suspend fun updateOffer(offer: PaceOffer) = weekDao.updateOffer(offer)

    private fun key(date: LocalDate): String = MyDate.fromLocalDate(date).dateString
}
```


- [ ] **Step 3: `db/data/todotask/TodayTaskDao.kt`: The earliest day with any item**

Replace:

```kotlin
    /** Items still planned on a day before [date], newest day first. */
```

with:

```kotlin
    /** The earliest day with any item, or null on an empty install. */
    @Query("SELECT MIN(task_date) FROM today_task")
    suspend fun getFirstDate(): String?

    /** Items still planned on a day before [date], newest day first. */
```


- [ ] **Step 4: `repo/TodayTaskRepository.kt`: Expose it**

Replace:

```kotlin
    suspend fun getPlannedBefore(date: LocalDate): List<TodayTask> = todayTaskDao.getPlannedBefore(key(date))
```

with:

```kotlin
    suspend fun getPlannedBefore(date: LocalDate): List<TodayTask> = todayTaskDao.getPlannedBefore(key(date))

    suspend fun firstDate(): LocalDate? = todayTaskDao.getFirstDate()?.let { MyDate(it).toLocalDate() }
```


- [ ] **Step 5: `repo/PlanRepository.kt`: Import the summary rules**

Replace:

```kotlin
import com.example.todoapp.domain.WeekMath
```

with:

```kotlin
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.WeekSummaries
```


- [ ] **Step 6: `repo/PlanRepository.kt`: Take the week repository**

Replace:

```kotlin
    private val milestoneRepository: MilestoneRepository,
) {
```

with:

```kotlin
    private val milestoneRepository: MilestoneRepository,
    private val weekRepository: WeekRepository,
) {
```


- [ ] **Step 7: `repo/PlanRepository.kt`: Close finished weeks too**

Replace:

```kotlin
        planDao.getUntickedBefore(key(today)).forEach { tickBlocks(MyDate(it.date).toLocalDate()) }
        resolveUnfinished(today)
    }
```

with:

```kotlin
        planDao.getUntickedBefore(key(today)).forEach { tickBlocks(MyDate(it.date).toLocalDate()) }
        resolveUnfinished(today)
        closeWeeks(today)
    }
```


- [ ] **Step 8: `repo/PlanRepository.kt`: Summaries for finished weeks**

Replace:

```kotlin
    private suspend fun currentPlan(date: LocalDate): DayPlan =
```

with:

```kotlin
    /**
     * Spec 5.1: every finished week since the first item gets a summary, once. Its points are what the
     * week's tasks earned, after the ticks and skips above. Call only inside [write].
     */
    private suspend fun closeWeeks(today: LocalDate) {
        val thisWeek = WeekMath.weekStart(today)
        val lastWeek = thisWeek.minus(7, DateTimeUnit.DAY)
        val first = todayTaskRepository.firstDate()?.let { WeekMath.weekStart(it) } ?: return
        if (first > lastWeek) return
        val stored = weekRepository.summaryWeeksFrom(first)
        val missing = generateSequence(first) { it.plus(7, DateTimeUnit.DAY) }
            .takeWhile { it <= lastWeek }
            .filter { it !in stored }
            .toList()
        if (missing.isEmpty()) return
        val missions = missionRepository.getActive()
        val allMissions = missionRepository.getAll()
        val from = missing.first().minus(HISTORY_DAYS, DateTimeUnit.DAY)
        val history = History(todayTaskRepository.getBetween(from, today.plus(7, DateTimeUnit.DAY)))
        missing.forEach { weekRepository.saveSummary(WeekSummaries.summarize(it, missions, allMissions, history)) }
    }

    private suspend fun currentPlan(date: LocalDate): DayPlan =
```


- [ ] **Step 9: `di/DI.kt`: Import the week repository**

Replace:

```kotlin
import com.example.todoapp.repo.TodayTaskRepository
import com.example.todoapp.repo.WaterIntakeRepository
```

with:

```kotlin
import com.example.todoapp.repo.TodayTaskRepository
import com.example.todoapp.repo.WaterIntakeRepository
import com.example.todoapp.repo.WeekRepository
```


- [ ] **Step 10: `di/DI.kt`: Provide it and pass it to the plan repository**

Replace:

```kotlin
        single { WaterIntakeRepository(get()) }
        single { PlanRepository(get(), get(), get(), get()) }
```

with:

```kotlin
        single { WaterIntakeRepository(get()) }
        single { WeekRepository(get()) }
        single { PlanRepository(get(), get(), get(), get(), get()) }
```


- [ ] **Step 11: `repo/PlanRepository.kt`: A change on a closed week's day updates its summary**

Replace:

```kotlin
    private suspend fun updateState(task: TodayTask, state: TaskState) {
        val mission = missionRepository.get(task.missionId) ?: return
        todayTaskRepository.update(
            task.copy(state = state.name, pointsEarned = Points.forTask(mission, state), crowned = task.crowned && state.isOpen)
        )
    }
```

with:

```kotlin
    private suspend fun updateState(task: TodayTask, state: TaskState) {
        val mission = missionRepository.get(task.missionId) ?: return
        todayTaskRepository.update(
            task.copy(state = state.name, pointsEarned = Points.forTask(mission, state), crowned = task.crowned && state.isOpen)
        )
        refreshClosedWeek(task.date)
    }

    /**
     * A tick or untick on a day of a finished week updates that week's stored points, so the
     * "Last week" card stays right. Its targets stay as they were at close-out. Call only inside [write].
     */
    private suspend fun refreshClosedWeek(date: LocalDate) {
        val weekStart = WeekMath.weekStart(date)
        if (weekStart >= WeekMath.weekStart(today())) return
        val summary = weekRepository.getSummary(weekStart) ?: return
        val tasks = todayTaskRepository.getBetween(weekStart, weekStart.plus(6, DateTimeUnit.DAY))
        weekRepository.saveSummary(WeekSummaries.withEarned(summary, missionRepository.getAll(), tasks))
    }
```


- [ ] **Step 12: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 13: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (it has rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/domain/WeekSummaries.kt \
  $K/repo/WeekRepository.kt \
  $K/db/data/todotask/TodayTaskDao.kt \
  $K/repo/TodayTaskRepository.kt \
  $K/repo/PlanRepository.kt \
  $K/di/DI.kt
git commit -m "feat: store a summary for each finished week"
```


---

### Task 4: Pace offers: rules, making them, answering them

**Files:**
- Create: `domain/PaceOffers.kt`
- Modify: `repo/PlanRepository.kt`, `db/SampleData.kt`

**Depends on:** Task 3 (`WeekRepository`, `closeWeeks`).

`PaceOffers` holds spec 5.2 as pure rules: the four triggers, the one-step change each offer makes, and the sticky's words (decisions 5 to 9). `closeWeeks` makes this week's offers when last week's summary is missing, which happens exactly on the first open of a new week. The offers are written before the summaries, so a close-out cut short is redone.

`answerOffer` records the answer. Only "yes" or "Pause a month" changes the mission, through `MissionRepository.save`, and then today's open picks are redone. A second tap on an answered offer does nothing.

The sample seed (still off) gains the history that shows both kinds of offer.

- [ ] **Step 1: Create `domain/PaceOffers.kt`**

Offer and answer names are stored in the database, so add new ones but never rename one.

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.week.PaceOffer
import com.example.todoapp.db.models.MyDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Spec 5.2, in the order offers are picked: easing off comes first. Stored by name: add types, never rename one. */
enum class OfferType {
    /** A habit or finish line got less than half its weekly count 3 weeks in a row: one step slower. */
    LOWER,
    /** An every-so-often mission ran late 3 times in a row: one step longer between times. */
    LENGTHEN,
    /** A gentle start had 3 full weeks: up to its goal pace. */
    STEP_UP_GENTLE,
    /** A weekly habit had 4 full weeks and this week has room: one more a week. */
    STEP_UP,
}

/** Stored by name in PaceOffer.response. */
enum class OfferResponse { ACCEPTED, KEPT, PAUSED, NOT_YET }

data class OfferFound(val mission: Mission, val type: OfferType)

/** A piece of a sentence. With [pillar] it's a mission title, drawn as a chip in that pillar's color (spec 7.1). */
data class LinePart(val text: String, val pillar: PillarType? = null)

/** What a pace-offer sticky says. Step-ups have no [pause], and their [keep] is "Not yet". */
data class OfferWords(
    val header: String,
    val goingWell: Boolean,
    val line: List<LinePart>,
    val accept: String,
    val keep: String,
    val pause: String?,
)

/** Spec 5.2: the app offers pace changes; only an accepted offer changes a mission. */
object PaceOffers {

    const val MAX_PER_WEEK = 2
    const val SLIP_WEEKS = 3
    const val LATE_TIMES = 3
    const val GENTLE_WEEKS = 3
    const val STEADY_WEEKS = 4
    /** "Not yet" holds the same offer back this long. */
    const val SNOOZE_WEEKS = 4
    /** History read to judge offers: enough for 3 late runs of a 3-month rhythm. */
    const val HISTORY_DAYS = 400
    /** "Every day" steps down to this many a week, and a weekly habit steps up to at most this many. */
    private const val MOST_PER_WEEK = 6

    /**
     * Offers for the week starting [weekStart], judged on finished weeks only: at most [MAX_PER_WEEK],
     * one per mission. [freeMinutesLeft] is this week's free time minus what its flexible missions need.
     * Weeks before a mission's last answered offer don't count, so after any answer the mission gets
     * a fresh run of weeks before it's asked again. "Not yet" also holds that offer back [SNOOZE_WEEKS].
     */
    fun find(
        weekStart: LocalDate,
        today: LocalDate,
        missions: List<Mission>,
        history: History,
        past: List<PaceOffer>,
        freeMinutesLeft: Int,
    ): List<OfferFound> {
        val found = mutableListOf<OfferFound>()
        for (type in OfferType.entries) {
            for (mission in missions) {
                if (found.size >= MAX_PER_WEEK) return found
                if (found.any { it.mission.missionId == mission.missionId }) continue
                if (!mission.isLiveOn(today) || accepted(mission, type) == null) continue
                if (snoozed(mission, type, past, weekStart)) continue
                val since = past
                    .filter { it.missionId == mission.missionId && it.response != null }
                    .maxOfOrNull { MyDate(it.week).toLocalDate() }
                if (triggers(mission, type, weekStart, history, since, freeMinutesLeft)) found += OfferFound(mission, type)
            }
        }
        return found
    }

    /** The mission after saying yes, or null when the offer can't apply to it (any more). */
    fun accepted(mission: Mission, type: OfferType): Mission? {
        val kind = mission.missionKind
        val counted = kind == MissionKind.HABIT || kind == MissionKind.FINISH_LINE
        val weekly = mission.periodUnit == Period.WEEK
        return when (type) {
            OfferType.LOWER -> when {
                !counted -> null
                mission.periodUnit == Period.DAY -> mission.copy(period = Period.WEEK.name, count = MOST_PER_WEEK)
                weekly && mission.count >= 2 -> mission.copy(count = mission.count - 1)
                else -> null
            }
            OfferType.LENGTHEN ->
                if (kind == MissionKind.RHYTHM && mission.intervalCount + 1 in MissionDrafts.intervalRange(mission.intervalPeriod)) {
                    mission.copy(intervalCount = mission.intervalCount + 1)
                } else null
            OfferType.STEP_UP_GENTLE -> {
                val goal = mission.goalCount
                if (counted && weekly && goal != null && goal > mission.count) mission.copy(count = goal, goalCount = null) else null
            }
            OfferType.STEP_UP -> {
                val gentle = (mission.goalCount ?: 0) > mission.count
                if (kind == MissionKind.HABIT && weekly && !gentle && mission.count < MOST_PER_WEEK) {
                    mission.copy(count = mission.count + 1, goalCount = null)
                } else null
            }
        }
    }

    /** "Pause a month": back on the same date next month. */
    fun paused(mission: Mission, today: LocalDate): Mission =
        mission.copy(pausedUntil = MyDate.fromLocalDate(today.plus(1, DateTimeUnit.MONTH)))

    /** The sticky's words, or null when the offer no longer applies. Built from stored fields; the title is only shown. */
    fun words(mission: Mission, type: OfferType): OfferWords? {
        val next = accepted(mission, type) ?: return null
        val title = LinePart(mission.title, mission.pillarType)
        return when (type) {
            OfferType.LOWER -> OfferWords(
                header = "A SMALL CHANGE?",
                goingWell = false,
                line = listOf(
                    title,
                    LinePart(
                        " has slipped $SLIP_WEEKS weeks in a row. That's fine. Want to make it ${weeklyPace(next.count)} " +
                            "for now? You can step back up anytime."
                    ),
                ),
                accept = "Make it ${next.count}×",
                keep = if (mission.periodUnit == Period.DAY) "Keep every day" else "Keep ${mission.count}×",
                pause = "Pause a month",
            )
            OfferType.LENGTHEN -> {
                val longer = MissionWords.intervalPhrase(next.intervalCount, next.intervalPeriod)
                OfferWords(
                    header = "A SMALL CHANGE?",
                    goingWell = false,
                    line = listOf(
                        title,
                        LinePart(" has slipped $LATE_TIMES times in a row. That's fine. Want to make it $longer for now? You can step back up anytime."),
                    ),
                    accept = longer.replaceFirstChar { it.uppercase() },
                    keep = "Keep ${MissionWords.intervalPhrase(mission.intervalCount, mission.intervalPeriod)}",
                    pause = "Pause a month",
                )
            }
            OfferType.STEP_UP_GENTLE -> OfferWords(
                header = "GOING WELL",
                goingWell = true,
                line = listOf(title, LinePart(" has gone well $GENTLE_WEEKS weeks running. Ready for ${weeklyPace(next.count)}?")),
                accept = "Step up",
                keep = "Not yet",
                pause = null,
            )
            OfferType.STEP_UP -> OfferWords(
                header = "GOING WELL",
                goingWell = true,
                line = listOf(title, LinePart(" has gone well $STEADY_WEEKS weeks running. Ready for ${weeklyPace(next.count)}?")),
                accept = "Step up",
                keep = "Not yet",
                pause = null,
            )
        }
    }

    private fun weeklyPace(count: Int): String = if (count == 1) "once a week" else "$count× a week"

    private fun snoozed(mission: Mission, type: OfferType, past: List<PaceOffer>, weekStart: LocalDate): Boolean =
        past.any { offer ->
            offer.missionId == mission.missionId && offer.type == type.name &&
                offer.snoozeUntil?.let { weekStart < MyDate(it).toLocalDate() } == true
        }

    private fun triggers(
        mission: Mission,
        type: OfferType,
        weekStart: LocalDate,
        history: History,
        since: LocalDate?,
        freeMinutesLeft: Int,
    ): Boolean {
        // The last n finished weeks, or null when some of them come before the last answer.
        fun lastWeeks(n: Int): List<LocalDate>? =
            (1..n).map { weekStart.minus(7 * it, DateTimeUnit.DAY) }.takeIf { weeks -> since == null || weeks.all { it >= since } }
        return when (type) {
            OfferType.LOWER -> lastWeeks(SLIP_WEEKS)?.all { slipped(mission, it, history) } ?: false
            OfferType.LENGTHEN -> ranLate(mission, weekStart, history, since)
            OfferType.STEP_UP_GENTLE -> lastWeeks(GENTLE_WEEKS)?.all { full(mission, it, history) } ?: false
            OfferType.STEP_UP -> freeMinutesLeft >= mission.durationMinutes &&
                (lastWeeks(STEADY_WEEKS)?.all { full(mission, it, history) } ?: false)
        }
    }

    /** Done less than half as often as it was due that week. A week it wasn't due doesn't count. */
    private fun slipped(mission: Mission, week: LocalDate, history: History): Boolean {
        val due = Points.occurrencesInWeek(mission, week, history)
        val done = history.doneCount(mission.missionId, week, week.plus(6, DateTimeUnit.DAY))
        return due > 0 && done * 2 < due
    }

    /** At 80% of its own points that week, the way a pillar counts as full. A week it wasn't due doesn't count. */
    private fun full(mission: Mission, week: LocalDate, history: History): Boolean {
        val due = Points.occurrencesInWeek(mission, week, history)
        if (due == 0) return false
        val end = week.plus(6, DateTimeUnit.DAY)
        val earned = history.tasksFor(mission.missionId).filter { it.date >= week && it.date <= end }.sumOf { it.pointsEarned }
        return Points.isFull(earned, mission.weight * due)
    }

    /**
     * The last [LATE_TIMES] times it came due (since [since]) were all done in a later week than the
     * due date's, counting one still not done when its week has already finished.
     */
    private fun ranLate(mission: Mission, weekStart: LocalDate, history: History, since: LocalDate?): Boolean {
        val interval = mission.intervalDays()
        val doneDays = history.tasksFor(mission.missionId)
            .filter { it.taskState.countsAsDone && it.date < weekStart }
            .map { it.date }.distinct().sorted()
        val start = mission.startDate.toLocalDate()
        // When the start is older than the history read, the first done day only sets the next due date.
        var due: LocalDate? = if (start >= weekStart.minus(HISTORY_DAYS, DateTimeUnit.DAY)) start else null
        val runs = mutableListOf<Pair<LocalDate, Boolean>>()
        for (day in doneDays) {
            due?.let { runs += it to (WeekMath.weekStart(day) > WeekMath.weekStart(it)) }
            due = day.plus(interval, DateTimeUnit.DAY)
        }
        due?.let { if (WeekMath.weekStart(it) < weekStart) runs += it to true }
        val recent = runs.filter { since == null || it.first >= since }.takeLast(LATE_TIMES)
        return recent.size == LATE_TIMES && recent.all { it.second }
    }
}
```


- [ ] **Step 2: `repo/PlanRepository.kt`: Import the offer table**

Replace:

```kotlin
import com.example.todoapp.db.data.todotask.TodayTask
```

with:

```kotlin
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.week.PaceOffer
```


- [ ] **Step 3: `repo/PlanRepository.kt`: Import the offer rules**

Replace:

```kotlin
import com.example.todoapp.domain.Moves
```

with:

```kotlin
import com.example.todoapp.domain.Moves
import com.example.todoapp.domain.OfferResponse
import com.example.todoapp.domain.OfferType
import com.example.todoapp.domain.PaceOffers
```


- [ ] **Step 4: `repo/PlanRepository.kt`: Import the week totals**

Replace:

```kotlin
import com.example.todoapp.domain.WeekMath
```

with:

```kotlin
import com.example.todoapp.domain.WeekLoad
import com.example.todoapp.domain.WeekMath
```


- [ ] **Step 5: `repo/PlanRepository.kt`: Answer a pace offer**

Replace:

```kotlin
    /** All writes: one at a time, finished even if the caller's screen goes away, and off the main thread. */
```

with:

```kotlin
    /**
     * Spec 5.2: the answer to a pace offer. Only "yes" or "pause a month" changes the mission, and then
     * today's open picks are redone, as after any mission save. A second tap on the same offer does nothing.
     */
    suspend fun answerOffer(offerId: Long, response: OfferResponse) = write {
        val offer = weekRepository.getOffer(offerId) ?: return@write
        if (offer.response != null) return@write
        val type = OfferType.entries.firstOrNull { it.name == offer.type } ?: return@write
        val week = MyDate(offer.week).toLocalDate()
        val snoozeUntil = if (response == OfferResponse.NOT_YET) week.plus(PaceOffers.SNOOZE_WEEKS * 7, DateTimeUnit.DAY) else null
        weekRepository.updateOffer(offer.copy(response = response.name, snoozeUntil = snoozeUntil?.let { key(it) }))
        val mission = missionRepository.get(offer.missionId) ?: return@write
        val today = today()
        val changed = when (response) {
            OfferResponse.ACCEPTED -> PaceOffers.accepted(mission, type)
            OfferResponse.PAUSED -> PaceOffers.paused(mission, today)
            OfferResponse.KEPT, OfferResponse.NOT_YET -> null
        } ?: return@write
        missionRepository.save(changed)
        // A paused mission leaves today's list and anything moved ahead, as when it's archived.
        if (response == OfferResponse.PAUSED) todayTaskRepository.deletePlannedFrom(changed.missionId, today)
        // As in archiveMission: a failed re-pick doesn't undo the answer.
        runCatching { refreshTodayIfOpen() }
    }

    /** All writes: one at a time, finished even if the caller's screen goes away, and off the main thread. */
```


- [ ] **Step 6: `repo/PlanRepository.kt`: The first open of a week also makes its pace offers**

Replace:

```kotlin
    /**
     * Spec 5.1: every finished week since the first item gets a summary, once. Its points are what the
     * week's tasks earned, after the ticks and skips above. Call only inside [write].
     */
```

with:

```kotlin
    /**
     * Spec 5.1: every finished week since the first item gets a summary, once. Its points are what the
     * week's tasks earned, after the ticks and skips above. When last week is among them, this is the
     * first open of a new week, which also makes this week's pace offers (spec 5.2). The offers are
     * written before the summaries, so a close-out cut short is redone next time. Call only inside [write].
     */
```


- [ ] **Step 7: `repo/PlanRepository.kt`: Read enough history for the offers and make them**

Replace:

```kotlin
        val from = missing.first().minus(HISTORY_DAYS, DateTimeUnit.DAY)
        val history = History(todayTaskRepository.getBetween(from, today.plus(7, DateTimeUnit.DAY)))
        missing.forEach { weekRepository.saveSummary(WeekSummaries.summarize(it, missions, allMissions, history)) }
    }
```

with:

```kotlin
        val from = minOf(missing.first().minus(HISTORY_DAYS, DateTimeUnit.DAY), thisWeek.minus(PaceOffers.HISTORY_DAYS, DateTimeUnit.DAY))
        val history = History(todayTaskRepository.getBetween(from, today.plus(7, DateTimeUnit.DAY)))
        if (lastWeek in missing) makeOffers(thisWeek, today, missions, history)
        missing.forEach { weekRepository.saveSummary(WeekSummaries.summarize(it, missions, allMissions, history)) }
    }

    /** This week's pace offers, unless it already has some. Call only inside [write]. */
    private suspend fun makeOffers(thisWeek: LocalDate, today: LocalDate, missions: List<Mission>, history: History) {
        if (weekRepository.getOffers(thisWeek).isNotEmpty()) return
        val freeLeft = WeekLoad.freeMinutes(planDao.getSettings()) - WeekLoad.plannedMinutes(missions, thisWeek, history)
        val found = PaceOffers.find(thisWeek, today, missions, history, weekRepository.getAllOffers(), freeLeft)
        weekRepository.insertOffers(
            found.map { PaceOffer(missionId = it.mission.missionId, type = it.type.name, week = key(thisWeek)) }
        )
    }
```


- [ ] **Step 8: `db/SampleData.kt`: Import minus**

Replace:

```kotlin
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
```

with:

```kotlin
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
```


- [ ] **Step 9: `db/SampleData.kt`: Gym's streak covers the last 3 weeks, so it doesn't look slipped**

Replace:

```kotlin
            (1..9).forEach { add(done(ids.getValue("Gym"), today.subtractDays(it), points = 2, reason = gym)) }
```

with:

```kotlin
            (1..21).forEach { add(done(ids.getValue("Gym"), today.subtractDays(it), points = 2, reason = gym)) }
```


- [ ] **Step 10: `db/SampleData.kt`: Three good gentle weeks of guitar, so the first open shows both kinds of pace offer**

Replace:

```kotlin
                day = day.plus(1, DateTimeUnit.DAY)
            }
        }
        todayTaskRepository.insertAll(history)
```

with:

```kotlin
                day = day.plus(1, DateTimeUnit.DAY)
            }
            // Guitar at its gentle 2× for the last 3 weeks, so the first open offers to step it up (spec 5.2).
            // "New ideas" has no history, so it shows as slipped and gets the other offer.
            val thisWeek = WeekMath.weekStart(todayDate)
            val guitar = Reason(ReasonCode.QUOTA, listOf("0", "2", "7"))
            for (weeksBack in 1..3) {
                val monday = thisWeek.minus(7 * weeksBack, DateTimeUnit.DAY)
                listOf(0, 3).forEach { offset ->
                    add(done(ids.getValue("Guitar"), MyDate.fromLocalDate(monday.plus(offset, DateTimeUnit.DAY)), points = 3, reason = guitar))
                }
            }
        }
        todayTaskRepository.insertAll(history)
```


- [ ] **Step 11: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 12: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (it has rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/domain/PaceOffers.kt \
  $K/repo/PlanRepository.kt \
  $K/db/SampleData.kt
git commit -m "feat: pace offers made at the start of a week and answered through the plan repository"
```


---

### Task 5: Diary state for Monday

**Files:**
- Create: `screen/diary/WeekState.kt`
- Modify: `db/data/plan/PlanDao.kt`, `repo/PlanRepository.kt`, `screen/diary/DiaryState.kt`, `screen/diary/DiaryViewModel.kt`, `di/DI.kt`

**Depends on:** Task 3 (`WeekRepository`, `WeekSummaries`), Task 4 (`PaceOffers`, `OfferResponse`, `answerOffer`).

The diary state learns the "Last week" card, the offers and the fresh-start label. Nothing draws them yet; Task 6 does.
- `DiaryExtras` carries the plans of the week so far, last week's summary and this week's offers. `DiaryStateBuilder.build` takes `today` and the extras instead of one plan.
- `WeekStateBuilder.opensWeek` decides which page shows the card (decision 10). Offers show only on today's page, when today opens the week.
- The view model already combines five flows, the most the typed `combine` takes, so the new ones are combined into `extras` first.

- [ ] **Step 1: Create `screen/diary/WeekState.kt`**

```kotlin
package com.example.todoapp.screen.diary

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.week.PaceOffer
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.History
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.OfferType
import com.example.todoapp.domain.OfferWords
import com.example.todoapp.domain.PaceOffers
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.Points
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.date
import com.example.todoapp.domain.earned
import com.example.todoapp.domain.isLiveOn
import com.example.todoapp.domain.pillarType
import com.example.todoapp.domain.pillarsFull
import com.example.todoapp.domain.repeatsByInterval
import com.example.todoapp.domain.shortLabel
import com.example.todoapp.domain.target
import com.example.todoapp.domain.weekStartDate
import com.example.todoapp.domain.weekdayName
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Monday's "Last week" card (spec 9), from last week's stored summary. */
data class LastWeekState(
    /** "Oct 26 to Nov 1" */
    val range: String,
    /** "54 of 62 points", with " · +4 extra" when a pillar went past its target. */
    val points: String,
    /** "3 of 4 pillars full", or "All 4 pillars full · a balanced week". */
    val pillars: String,
    val balanced: Boolean,
    /** What the app adjusted, each line naming its mission on a chip. Never empty. */
    val adjusted: List<List<LinePart>>,
)

/** One pace-offer sticky (spec 5.2). */
data class OfferNote(val offerId: Long, val words: OfferWords)

/** What the diary reads besides missions and tasks. */
data class DiaryExtras(
    /** This day's plan and those of the week's earlier days. */
    val plans: List<DayPlan>,
    /** The summary of the week before this day's week. */
    val lastWeek: WeekSummary?,
    /** Offers made for the week that has today in it. */
    val offers: List<PaceOffer>,
)

object WeekStateBuilder {

    const val MAX_ADJUSTED = 3

    /**
     * True when [date] opens its week in the diary: no earlier day of that week has a plan, because the
     * diary wasn't opened on it. Monday always opens its week. The "Last week" card shows on such days.
     */
    fun opensWeek(date: LocalDate, plans: List<DayPlan>): Boolean {
        val weekStart = WeekMath.weekStart(date)
        return plans.none { MyDate(it.date).toLocalDate().let { day -> day >= weekStart && day < date } }
    }

    /** Null without a summary, or when last week had nothing due and nothing done. */
    fun lastWeek(summary: WeekSummary?, missions: List<Mission>, tasks: List<TodayTask>): LastWeekState? {
        summary ?: return null
        val pillars = PillarType.scoredPillars
        val target = pillars.sumOf { summary.target(it) }
        val earned = pillars.sumOf { summary.earned(it) }
        if (target == 0 && earned == 0) return null
        // Extra never fills another pillar (spec 5), so it's shown on its own.
        val counted = pillars.sumOf { minOf(summary.earned(it), summary.target(it)) }
        val extra = earned - counted
        val start = summary.weekStartDate
        return LastWeekState(
            range = "${start.shortLabel()} to ${start.plus(6, DateTimeUnit.DAY).shortLabel()}",
            points = "$counted of ${MissionWords.pointsPhrase(target)}" + if (extra > 0) " · +$extra extra" else "",
            pillars = if (summary.balanced) {
                "All ${pillars.size} pillars full · a balanced week"
            } else {
                "${summary.pillarsFull()} of ${pillars.size} pillars full"
            },
            balanced = summary.balanced,
            adjusted = adjusted(start, missions, tasks),
        )
    }

    /** This week's unanswered offers whose mission is still active and can still take the change. */
    fun offers(offers: List<PaceOffer>, missions: List<Mission>): List<OfferNote> {
        val byId = missions.associateBy { it.missionId }
        return offers.filter { it.response == null }.mapNotNull { offer ->
            val mission = byId[offer.missionId] ?: return@mapNotNull null
            val type = OfferType.entries.firstOrNull { it.name == offer.type } ?: return@mapNotNull null
            PaceOffers.words(mission, type)?.let { OfferNote(offer.offerId, it) }
        }
    }

    /**
     * What the app adjusted, from stored data only: items "move it" carried from last week into this
     * week, and every-so-often missions that were due last week and stay due (spec 5.1).
     */
    private fun adjusted(lastStart: LocalDate, missions: List<Mission>, tasks: List<TodayTask>): List<List<LinePart>> {
        val lastEnd = lastStart.plus(6, DateTimeUnit.DAY)
        val thisStart = lastStart.plus(7, DateTimeUnit.DAY)
        val byId = missions.associateBy { it.missionId }
        val history = History(tasks)
        val named = mutableSetOf<Long>()
        val lines = mutableListOf<List<LinePart>>()
        tasks
            .filter { task ->
                val from = task.movedFromDate?.toLocalDate()
                from != null && from >= lastStart && from <= lastEnd && task.date >= thisStart
            }
            .sortedBy { it.date }
            .forEach { task ->
                val mission = byId[task.missionId] ?: return@forEach
                if (named.add(mission.missionId)) lines += listOf(chip(mission), LinePart(" moved to ${task.date.weekdayName()}."))
            }
        missions
            .filter { it.repeatsByInterval && it.isLiveOn(thisStart) && it.missionId !in named }
            .filter {
                Points.occurrencesInWeek(it, lastStart, history) > 0 && history.doneCount(it.missionId, lastStart, lastEnd) == 0
            }
            .forEach { lines += listOf(chip(it), LinePart(" is still due, so it moved into this week.")) }
        return lines.take(MAX_ADJUSTED).ifEmpty { listOf(listOf(LinePart("Nothing to catch up on."))) }
    }

    private fun chip(mission: Mission) = LinePart(mission.title, mission.pillarType)
}
```


- [ ] **Step 2: `db/data/plan/PlanDao.kt`: Observe the plans of a range of days**

Replace:

```kotlin
    @Query("SELECT * FROM day_plan WHERE plan_date = :date")
    fun observePlan(date: String): Flow<DayPlan?>
```

with:

```kotlin
    @Query("SELECT * FROM day_plan WHERE plan_date = :date")
    fun observePlan(date: String): Flow<DayPlan?>

    @Query("SELECT * FROM day_plan WHERE plan_date BETWEEN :from AND :to ORDER BY plan_date")
    fun observePlansBetween(from: String, to: String): Flow<List<DayPlan>>
```


- [ ] **Step 3: `repo/PlanRepository.kt`: Expose it**

Replace:

```kotlin
    fun observePlan(date: LocalDate): Flow<DayPlan?> = planDao.observePlan(key(date))
```

with:

```kotlin
    fun observePlan(date: LocalDate): Flow<DayPlan?> = planDao.observePlan(key(date))

    fun observePlans(from: LocalDate, to: LocalDate): Flow<List<DayPlan>> = planDao.observePlansBetween(key(from), key(to))
```


- [ ] **Step 4: `screen/diary/DiaryState.kt`: Monday's card and offers in the diary state**

Replace:

```kotlin
    /** False on an empty install: the list then points to the Missions tab. */
    val hasMissions: Boolean,
) {
```

with:

```kotlin
    /** False on an empty install: the list then points to the Missions tab. */
    val hasMissions: Boolean,
    /** Shown on the day that opens the week (WeekStateBuilder.opensWeek). */
    val lastWeek: LastWeekState?,
    /** Pace offers, on today's page when it opens the week. */
    val offers: List<OfferNote>,
    /** The week has just begun: the strip reads "THIS WEEK · A FRESH START". */
    val freshStart: Boolean,
) {
```


- [ ] **Step 5: `screen/diary/DiaryState.kt`: Build from the week's plans and the week extras**

Replace:

```kotlin
    /**
     * [missions] are the active ones; everything is built from them except points earned, which use
     * [allMissions] (archived too) so a mission archived mid-week keeps the points it already earned.
     */
    fun build(
        date: LocalDate,
        missions: List<Mission>,
        allMissions: List<Mission>,
        tasks: List<TodayTask>,
        plan: DayPlan?,
    ): DiaryUiState {
        val byId = missions.associateBy { it.missionId }
```

with:

```kotlin
    /**
     * [missions] are the active ones; everything is built from them except points earned, which use
     * [allMissions] (archived too) so a mission archived mid-week keeps the points it already earned.
     */
    fun build(
        date: LocalDate,
        today: LocalDate,
        missions: List<Mission>,
        allMissions: List<Mission>,
        tasks: List<TodayTask>,
        extras: DiaryExtras,
    ): DiaryUiState {
        val plan = extras.plans.firstOrNull { it.date == MyDate.fromLocalDate(date).dateString }
        val opensWeek = WeekStateBuilder.opensWeek(date, extras.plans)
        val lastWeek = if (opensWeek) WeekStateBuilder.lastWeek(extras.lastWeek, missions, tasks) else null
        val byId = missions.associateBy { it.missionId }
```


- [ ] **Step 6: `screen/diary/DiaryState.kt`: Fill the new fields**

Replace:

```kotlin
            donePicks = picks.count { it.state.countsAsDone },
            hasMissions = missions.isNotEmpty(),
        )
```

with:

```kotlin
            donePicks = picks.count { it.state.countsAsDone },
            hasMissions = missions.isNotEmpty(),
            lastWeek = lastWeek,
            offers = if (date == today && opensWeek) WeekStateBuilder.offers(extras.offers, missions) else emptyList(),
            freshStart = lastWeek != null && pillars.all { it.earned == 0 },
        )
```


- [ ] **Step 7: `screen/diary/DiaryViewModel.kt`: Import the offer answers**

Replace:

```kotlin
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.TaskState
```

with:

```kotlin
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.OfferResponse
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.WeekMath
```


- [ ] **Step 8: `screen/diary/DiaryViewModel.kt`: Import the week repository**

Replace:

```kotlin
import com.example.todoapp.repo.TodayTaskRepository
```

with:

```kotlin
import com.example.todoapp.repo.TodayTaskRepository
import com.example.todoapp.repo.WeekRepository
```


- [ ] **Step 9: `screen/diary/DiaryViewModel.kt`: Take the week repository**

Replace:

```kotlin
    private val milestoneRepository: MilestoneRepository,
    private val currentDate: LocalDate,
) : ViewModel() {

    private val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val isToday: Boolean = currentDate == now.date
```

with:

```kotlin
    private val milestoneRepository: MilestoneRepository,
    weekRepository: WeekRepository,
    private val currentDate: LocalDate,
) : ViewModel() {

    private val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    private val today: LocalDate = now.date
    val isToday: Boolean = currentDate == today
```


- [ ] **Step 10: `screen/diary/DiaryViewModel.kt`: Read the week's plans, last week's summary and this week's offers**

Replace:

```kotlin
    /** Archived missions come in too, only so their points earned this week still count in the bars. */
    val state: StateFlow<DiaryUiState?> = combine(
        missionRepository.observeActive(),
        missionRepository.observeAll(),
        todayTaskRepository.observeBetween(
            currentDate.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY),
            currentDate.plus(7, DateTimeUnit.DAY),
        ),
        planRepository.observePlan(currentDate),
        planReady,
    ) { missions, allMissions, tasks, plan, ready ->
        if (ready) DiaryStateBuilder.build(currentDate, missions = missions, allMissions = allMissions, tasks, plan) else null
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)
```

with:

```kotlin
    private val extras = combine(
        // This day's plan and the week's earlier ones, which decide whether this day opens the week.
        planRepository.observePlans(WeekMath.weekStart(currentDate), currentDate),
        weekRepository.observeSummary(WeekMath.weekStart(currentDate).minus(7, DateTimeUnit.DAY)),
        weekRepository.observeOffers(WeekMath.weekStart(today)),
    ) { plans, summary, offers -> DiaryExtras(plans, summary, offers) }

    /** Archived missions come in too, only so their points earned this week still count in the bars. */
    val state: StateFlow<DiaryUiState?> = combine(
        missionRepository.observeActive(),
        missionRepository.observeAll(),
        todayTaskRepository.observeBetween(
            currentDate.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY),
            currentDate.plus(7, DateTimeUnit.DAY),
        ),
        extras,
        planReady,
    ) { missions, allMissions, tasks, extras, ready ->
        if (ready) DiaryStateBuilder.build(currentDate, today, missions, allMissions, tasks, extras) else null
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)
```


- [ ] **Step 11: `screen/diary/DiaryViewModel.kt`: Answer a pace offer**

Replace:

```kotlin
    fun confirm() = act { planRepository.confirm(currentDate) }
```

with:

```kotlin
    fun confirm() = act { planRepository.confirm(currentDate) }
    /** The write can't be cancelled (PlanRepository.write), and a second answer to the same offer does nothing. */
    fun answerOffer(offerId: Long, response: OfferResponse) = act { planRepository.answerOffer(offerId, response) }
```


- [ ] **Step 12: `di/DI.kt`: Pass the week repository to the diary**

Replace:

```kotlin
        factory { (day: LocalDate) -> DiaryViewModel(get(), get(), get(), get(), day) }
```

with:

```kotlin
        factory { (day: LocalDate) -> DiaryViewModel(get(), get(), get(), get(), get(), day) }
```


- [ ] **Step 13: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 14: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (it has rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/screen/diary/WeekState.kt \
  $K/db/data/plan/PlanDao.kt \
  $K/repo/PlanRepository.kt \
  $K/screen/diary/DiaryState.kt \
  $K/screen/diary/DiaryViewModel.kt \
  $K/di/DI.kt
git commit -m "feat: diary state for the last-week card and pace offers"
```


---

### Task 6: The Last week card and offer stickies on the diary

**Files:**
- Create: `screen/diary/plan/WeekNotes.kt`
- Modify: `screen/basicutils/AppColors.kt`, `screen/diary/plan/PlanNotes.kt`, `screen/diary/plan/PillarWeekStrip.kt`, `screen/diary/DiaryScreen.kt`

**Depends on:** Task 5 (`LastWeekState`, `OfferNote`, `DiaryUiState.lastWeek`, `offers`, `freshStart`, `DiaryViewModel.answerOffer`).

Draws Monday's diary (spec 9 and the `DiaryMonday` mockup), directly under the week strip:
- The strip's label: "THIS WEEK · A FRESH START" while nothing's earned yet.
- The "Last week" card, in the evening highlights style.
- Up to two tilted offer stickies.

`ChipText` draws mission titles on rounded chips in their pillar's light color (spec 7.1). The new `HighlightRow` is the highlights card's row, now shared with the Last week card. Each sticky takes one answer: buttons tapped again before it goes away do nothing.

- [ ] **Step 1: Create `screen/diary/plan/WeekNotes.kt`**

```kotlin
package com.example.todoapp.screen.diary.plan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.OfferResponse
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.CardCream
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.StickyBlue
import com.example.todoapp.screen.basicutils.StickyYellow
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarLight
import com.example.todoapp.screen.diary.LastWeekState
import com.example.todoapp.screen.diary.OfferNote
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.crown_a
import todoapp.shared.generated.resources.flag_open
import todoapp.shared.generated.resources.heart_filled
import todoapp.shared.generated.resources.star_filled
import todoapp.shared.generated.resources.wand
import kotlin.math.abs

private const val CHIP_TAG = "chip"

/**
 * A sentence whose mission titles sit on chips in their pillar's light color (spec 7.1), so any title
 * reads fine whatever the words around it. No-break spaces pad the chip inside.
 */
@Composable
fun ChipText(parts: List<LinePart>, fontSize: TextUnit, color: Color, italic: Boolean = false, modifier: Modifier = Modifier) {
    val text = remember(parts) {
        buildAnnotatedString {
            parts.forEach { part ->
                val pillar = part.pillar
                if (pillar == null) {
                    append(part.text)
                } else {
                    pushStringAnnotation(CHIP_TAG, pillar.name)
                    withStyle(SpanStyle(color = pillarInk(pillar), fontStyle = FontStyle.Normal)) { append(" ${part.text} ") }
                    pop()
                }
            }
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text,
        fontSize = fontSize,
        lineHeight = fontSize * 1.6f,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        color = color,
        onTextLayout = { layout = it },
        modifier = modifier.drawBehind {
            val l = layout ?: return@drawBehind
            val inset = 2.dp.toPx()
            text.getStringAnnotations(CHIP_TAG, 0, text.length).forEach { chip ->
                val pillar = PillarType.entries.firstOrNull { it.name == chip.item } ?: return@forEach
                for (line in l.getLineForOffset(chip.start)..l.getLineForOffset(chip.end - 1)) {
                    val from = maxOf(chip.start, l.getLineStart(line))
                    val to = minOf(chip.end, l.getLineEnd(line))
                    if (to <= from) continue
                    val left = l.getBoundingBox(from).left
                    val right = l.getBoundingBox(to - 1).right
                    drawRoundRect(
                        pillarLight(pillar),
                        topLeft = Offset(minOf(left, right), l.getLineTop(line) + inset),
                        size = Size(abs(right - left), l.getLineBottom(line) - l.getLineTop(line) - 2 * inset),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                    )
                }
            }
        }
    )
}

/** Monday's look back, in the style of the evening highlights card (spec 9). */
@Composable
fun LastWeekCard(state: LastWeekState) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(CardCream, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "LAST WEEK",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp, color = Color(0xFF3A3A3A),
                modifier = Modifier.weight(1f)
            )
            Text(state.range, fontSize = 13.sp, color = InkGray)
        }
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(Color(0xFFDDD6C2)))
        HighlightRow(Res.drawable.star_filled) { Text(state.points, fontSize = 15.sp, color = InkBlack) }
        HighlightRow(if (state.balanced) Res.drawable.crown_a else Res.drawable.heart_filled) {
            Text(state.pillars, fontSize = 15.sp, color = InkBlack)
        }
        // The wand always means "the app adjusted something" (spec 9.1).
        state.adjusted.forEach { line -> HighlightRow(Res.drawable.wand) { ChipText(line, fontSize = 15.sp, color = InkBlack) } }
    }
}

/** A pace offer as a tilted sticky (spec 5.2): easing off on yellow with the wand, stepping up on blue with the flag. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaceOfferNote(note: OfferNote, rotation: Float, onAnswer: (Long, OfferResponse) -> Unit) {
    val words = note.words
    // One answer per offer: a second tap before the note goes away does nothing.
    var answered by remember(note.offerId) { mutableStateOf(false) }
    val answer = { response: OfferResponse ->
        if (!answered) {
            answered = true
            onAnswer(note.offerId, response)
        }
    }
    val ink = if (words.goingWell) pillarInk(PillarType.LIFE) else Color(0xFF6A5A2A)
    StickyNote(rotation = rotation, color = if (words.goingWell) StickyBlue else StickyYellow) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(if (words.goingWell) Res.drawable.flag_open else Res.drawable.wand), contentDescription = null,
                    colorFilter = ColorFilter.tint(ink), modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(words.header, fontSize = 11.sp, letterSpacing = 1.2.sp, color = ink)
            }
            ChipText(words.line, fontSize = 16.sp, color = InkBlack, italic = true, modifier = Modifier.padding(top = 6.dp))
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlanButton(words.accept, filled = true, onClick = { answer(OfferResponse.ACCEPTED) })
                PlanButton(
                    words.keep, filled = false,
                    onClick = { answer(if (words.goingWell) OfferResponse.NOT_YET else OfferResponse.KEPT) }
                )
                words.pause?.let { PlanButton(it, filled = false, onClick = { answer(OfferResponse.PAUSED) }) }
            }
        }
    }
}
```


- [ ] **Step 2: `screen/basicutils/AppColors.kt`: A blue sticky for offers that are going well**

Replace:

```kotlin
val StickyYellow = Color(0xFFFDF0C6)
```

with:

```kotlin
val StickyYellow = Color(0xFFFDF0C6)
val StickyBlue = Color(0xFFE3F1FF)
```


- [ ] **Step 3: `screen/diary/plan/PlanNotes.kt`: Stickies can take another color**

Replace:

```kotlin
/** Slightly tilted sticky note, like the mission cards. */
@Composable
fun StickyNote(rotation: Float, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 12.dp)
            .rotate(rotation)
            .shadow(6.dp, RoundedCornerShape(4.dp))
            .background(StickyYellow, RoundedCornerShape(4.dp))
```

with:

```kotlin
/** Slightly tilted sticky note, like the mission cards. */
@Composable
fun StickyNote(rotation: Float, color: Color = StickyYellow, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 12.dp)
            .rotate(rotation)
            .shadow(6.dp, RoundedCornerShape(4.dp))
            .background(color, RoundedCornerShape(4.dp))
```


- [ ] **Step 4: `screen/diary/plan/PlanNotes.kt`: Share the highlights row with the Last week card**

Replace:

```kotlin
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

with:

```kotlin
@Composable
private fun HighlightLine(icon: DrawableResource, text: String) {
    HighlightRow(icon) { Text(text, fontSize = 15.sp, color = InkBlack) }
}

/** One line of a highlights card: an icon, then [content]. */
@Composable
internal fun HighlightRow(icon: DrawableResource, content: @Composable () -> Unit) {
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
        content()
    }
}
```


- [ ] **Step 5: `screen/diary/plan/PillarWeekStrip.kt`: A fresh start label**

Replace:

```kotlin
/** "This week" bars under the date: points earned against each pillar's target. */
@Composable
fun PillarWeekStrip(pillars: List<PillarProgress>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("THIS WEEK", fontSize = 11.sp, letterSpacing = 1.2.sp, color = InkGray)
```

with:

```kotlin
/** "This week" bars under the date: points earned against each pillar's target. */
@Composable
fun PillarWeekStrip(pillars: List<PillarProgress>, freshStart: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(if (freshStart) "THIS WEEK · A FRESH START" else "THIS WEEK", fontSize = 11.sp, letterSpacing = 1.2.sp, color = InkGray)
```


- [ ] **Step 6: `screen/diary/DiaryScreen.kt`: Import the week notes**

Replace:

```kotlin
import com.example.todoapp.screen.diary.plan.DidntFitNote
```

with:

```kotlin
import com.example.todoapp.screen.diary.plan.DidntFitNote
import com.example.todoapp.screen.diary.plan.LastWeekCard
import com.example.todoapp.screen.diary.plan.PaceOfferNote
```


- [ ] **Step 7: `screen/diary/DiaryScreen.kt`: Last week and the pace offers under the week strip**

Replace:

```kotlin
            state?.let { s -> item(key = "week-strip") { PillarWeekStrip(s.pillars) } }
```

with:

```kotlin
            state?.let { s ->
                item(key = "week-strip") { PillarWeekStrip(s.pillars, freshStart = s.freshStart) }
                s.lastWeek?.let { card -> item(key = "last-week") { LastWeekCard(card) } }
                s.offers.forEachIndexed { i, note ->
                    item(key = "offer-${note.offerId}") {
                        PaceOfferNote(note, rotation = if (i % 2 == 0) -1.4f else 1.1f, onAnswer = diaryViewModel::answerOffer)
                    }
                }
            }
```


- [ ] **Step 8: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (it has rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/screen/diary/plan/WeekNotes.kt \
  $K/screen/basicutils/AppColors.kt \
  $K/screen/diary/plan/PlanNotes.kt \
  $K/screen/diary/plan/PillarWeekStrip.kt \
  $K/screen/diary/DiaryScreen.kt
git commit -m "feat: last-week card and pace-offer stickies on Monday's diary"
```


---

### Task 7: The pencil's list, grouped by pillar with reason lines

**Files:**
- Modify: `domain/Planner.kt`, `repo/PlanRepository.kt`, `screen/diary/DiaryState.kt`, `screen/diary/DiaryViewModel.kt`, `screen/diary/plan/PlanDialogs.kt`, `screen/diary/DiaryScreen.kt`

**Depends on:** Task 5 (`DiaryStateBuilder.build`'s current shape, the view model's `extras`).

Spec 6.3: the pencil opens "a manual list of all candidates grouped by pillar, each with its reason line" (decision 12).
- `Planner.options` returns today's candidates in pick order, then every other live mission with a line that says where it stands.
- The view model loads the list when the pencil is tapped. A second tap while it loads does nothing.
- The dialog groups the list by pillar, under small pillar headings, in keyed rows at least 44dp tall.
- The diary's own copy of the active missions is no longer read, so it's removed.

- [ ] **Step 1: `domain/Planner.kt`: The pencil's list: every live mission not on the day, with its reason line**

Replace:

```kotlin
    /** Free minutes the flexible picks may use, after anything already on today's list. */
```

with:

```kotlin
    /**
     * Spec 6.3, the pencil: every live mission that isn't a fixed block and isn't on the day's list yet,
     * each with its reason line. Today's candidates come first, in pick order; the rest say where they stand.
     */
    fun options(input: PlanInput): List<Pick> {
        val ranked = candidates(input, fixedBlocks(input)).map { Pick(it.mission, it.reason, isFixedBlock = false) }
        val rankedIds = ranked.map { it.mission.missionId }.toSet()
        val rest = input.missions
            .filter { it.missionKind != MissionKind.BLOCK && it.isLiveOn(input.date) && it.missionId !in rankedIds }
            .filter { mission -> input.todayTasks.none { it.missionId == mission.missionId } }
            .map { Pick(it, standing(it, input), isFixedBlock = false) }
        return ranked + rest
    }

    /** The reason line for a mission that isn't a candidate today: this week's count, or the last time it was done. */
    private fun standing(mission: Mission, input: PlanInput): Reason {
        val date = input.date
        val history = input.history
        return when {
            mission.missionKind == MissionKind.RECHARGE -> Reason(ReasonCode.RECHARGE_USUAL, listOf(mission.count.toString()))
            mission.repeatsByInterval -> {
                val last = history.lastDoneBefore(mission.missionId, date)
                if (last == null) {
                    Reason(ReasonCode.RHYTHM_FIRST, listOf(mission.intervalLabel()))
                } else {
                    Reason(ReasonCode.RHYTHM_DUE, listOf(last.daysUntil(date).toString(), mission.intervalLabel()))
                }
            }
            mission.periodUnit == Period.DAY ->
                Reason(ReasonCode.STREAK, listOf(history.streakBefore(mission.missionId, date).toString()))
            else -> {
                val weekStart = WeekMath.weekStart(date)
                val weekEnd = WeekMath.weekEnd(date)
                val target = Points.occurrencesInWeek(mission, weekStart, history)
                val done = history.doneCount(mission.missionId, weekStart, date.minus(1, DateTimeUnit.DAY))
                val lastDay = mission.endDate?.toLocalDate()?.let { minOf(it, weekEnd) } ?: weekEnd
                Reason(ReasonCode.QUOTA, listOf(done.toString(), target.toString(), (date.daysUntil(lastDay) + 1).toString()))
            }
        }
    }

    /** Free minutes the flexible picks may use, after anything already on today's list. */
```


- [ ] **Step 2: `repo/PlanRepository.kt`: Import Pick**

Replace:

```kotlin
import com.example.todoapp.domain.PaceOffers
```

with:

```kotlin
import com.example.todoapp.domain.PaceOffers
import com.example.todoapp.domain.Pick
```


- [ ] **Step 3: `repo/PlanRepository.kt`: The pencil's list for a day**

Replace:

```kotlin
    /** Moves a planned pick to the next day with room, from today on (the first of those days if none in the week). */
```

with:

```kotlin
    /** Spec 6.3, the pencil: every live mission not on the day's list, with its reason line. Only reads, so it doesn't lock. */
    suspend fun addOptions(date: LocalDate): List<Pick> = withContext(Dispatchers.Default) {
        Planner.options(input(date, currentPlan(date)))
    }

    /** Moves a planned pick to the next day with room, from today on (the first of those days if none in the week). */
```


- [ ] **Step 4: `screen/diary/DiaryState.kt`: Import Pick**

Replace:

```kotlin
import com.example.todoapp.domain.PillarType
```

with:

```kotlin
import com.example.todoapp.domain.Pick
import com.example.todoapp.domain.PillarType
```


- [ ] **Step 5: `screen/diary/DiaryState.kt`: One line of the pencil's list**

Replace:

```kotlin
data class DidntFitRow(val title: String, val text: String)
```

with:

```kotlin
data class DidntFitRow(val title: String, val text: String)

/** One mission in the pencil's list, with its reason line. */
data class AddOption(val missionId: Long, val title: String, val pillar: PillarType, val minutes: Int, val reason: String) {
    companion object {
        fun from(pick: Pick) = AddOption(
            missionId = pick.mission.missionId,
            title = pick.mission.title,
            pillar = pick.mission.pillarType,
            minutes = pick.mission.durationMinutes,
            reason = ReasonLines.render(pick.reason),
        )
    }
}
```


- [ ] **Step 6: `screen/diary/DiaryViewModel.kt`: Import Job**

Replace:

```kotlin
import kotlinx.coroutines.Dispatchers
```

with:

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
```


- [ ] **Step 7: `screen/diary/DiaryViewModel.kt`: Hold the pencil's list while its dialog is open**

Replace:

```kotlin
    /** False until today's plan is built, so the list doesn't flicker through half-built states. */
```

with:

```kotlin
    private val _addOptions = MutableStateFlow<List<AddOption>?>(null)
    /** The pencil's list while its dialog is open; null when it's closed. */
    val addOptions: StateFlow<List<AddOption>?> = _addOptions
    private var addLoading: Job? = null

    /** False until today's plan is built, so the list doesn't flicker through half-built states. */
```


- [ ] **Step 8: `screen/diary/DiaryViewModel.kt`: Open and close the pencil's list**

Replace:

```kotlin
    fun toggleEveningCheck() {
```

with:

```kotlin
    /** Opens the pencil's list. A second tap while it loads or shows does nothing. */
    fun openAdd() {
        if (_addOptions.value != null || addLoading?.isActive == true) return
        _notice.value = null
        addLoading = viewModelScope.launch {
            _addOptions.value = planRepository.addOptions(currentDate).map { AddOption.from(it) }
        }
    }

    fun closeAdd() {
        addLoading?.cancel()
        _addOptions.value = null
    }

    fun toggleEveningCheck() {
```


- [ ] **Step 9: `screen/diary/DiaryViewModel.kt`: Drop the Mission import**

Replace:

```kotlin
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
```

with:

```kotlin
import com.example.todoapp.db.data.mission.milestone.Milestone
```


- [ ] **Step 10: `screen/diary/DiaryViewModel.kt`: The screen no longer reads the mission list, so stop watching it**

Replace:

```kotlin
    val missions: StateFlow<List<Mission>> = missionRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val milestones
```

with:

```kotlin
    val milestones
```


- [ ] **Step 11: `screen/diary/plan/PlanDialogs.kt`: Imports for the grouped list**

Replace:

```kotlin
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
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.pillarStrong
```

with:

```kotlin
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.minutesLabel
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarStrong
import com.example.todoapp.screen.diary.AddOption
```


- [ ] **Step 12: `screen/diary/plan/PlanDialogs.kt`: The list grouped by pillar, each mission with its reason line**

Replace:

```kotlin
/** The pencil on TO DO LIST: add any mission that isn't on today's list yet. */
@Composable
fun AddMissionDialog(missions: List<Mission>, todayMissionIds: Set<Long>, onAdd: (Long) -> Unit, onDismiss: () -> Unit) {
    val available = missions
        .filter { it.missionKind != MissionKind.BLOCK && it.missionId !in todayMissionIds }
        .sortedBy { it.pillarType.ordinal }
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = PeachText)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss, colors = buttonColors) { Text("Done") } },
        title = { Text("Add to today") },
        text = {
            if (available.isEmpty()) {
                Text(if (missions.isEmpty()) "No missions yet. Add some on the Missions tab." else "Everything is already on today's list.")
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

with:

```kotlin
/**
 * The pencil on TO DO LIST (spec 6.3): every mission that isn't on the day's list yet, grouped by
 * pillar, each with its reason line. Today's candidates come first in each group.
 */
@Composable
fun AddMissionDialog(
    options: List<AddOption>,
    hasMissions: Boolean,
    todayMissionIds: Set<Long>,
    onAdd: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val available = options.filter { it.missionId !in todayMissionIds }
    val groups = PillarType.entries
        .map { pillar -> pillar to available.filter { it.pillar == pillar } }
        .filter { (_, rows) -> rows.isNotEmpty() }
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = PeachText)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss, colors = buttonColors) { Text("Done") } },
        title = { Text("Add to today") },
        text = {
            if (available.isEmpty()) {
                Text(if (!hasMissions) "No missions yet. Add some on the Missions tab." else "Everything is already on today's list.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    groups.forEach { (pillar, rows) ->
                        item(key = "pillar-${pillar.name}") {
                            Text(
                                pillar.title.uppercase(),
                                fontSize = 11.sp, letterSpacing = 1.2.sp, color = pillarInk(pillar),
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }
                        items(rows, key = { it.missionId }) { option ->
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                                    .clickable(role = Role.Button) { onAdd(option.missionId) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(pillarStrong(option.pillar)))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(option.title, fontSize = 17.sp, fontStyle = FontStyle.Italic)
                                    if (option.reason.isNotEmpty()) {
                                        Text(option.reason, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = InkGray)
                                    }
                                }
                                Text(minutesLabel(option.minutes), fontSize = 12.sp, color = InkGray, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        containerColor = Light_Yellowww
    )
}
```


- [ ] **Step 13: `screen/diary/DiaryScreen.kt`: Collect the pencil's list instead of the missions**

Replace:

```kotlin
    val missions by diaryViewModel.missions.collectAsState()
```

with:

```kotlin
    val addOptions by diaryViewModel.addOptions.collectAsState()
```


- [ ] **Step 14: `screen/diary/DiaryScreen.kt`: The dialog shows while the list is loaded, so drop its flag**

Replace:

```kotlin
    var showCapacity by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
```

with:

```kotlin
    var showCapacity by remember { mutableStateOf(false) }
```


- [ ] **Step 15: `screen/diary/DiaryScreen.kt`: The pencil loads the list**

Replace:

```kotlin
            ToDoListHeader("TO DO LIST", Orange80, editClicked = { showAdd = true })
```

with:

```kotlin
            ToDoListHeader("TO DO LIST", Orange80, editClicked = { diaryViewModel.openAdd() })
```


- [ ] **Step 16: `screen/diary/DiaryScreen.kt`: Show the grouped list**

Replace:

```kotlin
        if (showAdd) {
            AddMissionDialog(
                missions = missions,
                todayMissionIds = s.todayMissionIds,
                onAdd = { diaryViewModel.addMission(it) },
                onDismiss = { showAdd = false }
            )
```

with:

```kotlin
        addOptions?.let { options ->
            AddMissionDialog(
                options = options,
                hasMissions = s.hasMissions,
                todayMissionIds = s.todayMissionIds,
                onAdd = { diaryViewModel.addMission(it) },
                onDismiss = { diaryViewModel.closeAdd() }
            )
```


- [ ] **Step 17: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 18: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (it has rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/domain/Planner.kt \
  $K/repo/PlanRepository.kt \
  $K/screen/diary/DiaryState.kt \
  $K/screen/diary/DiaryViewModel.kt \
  $K/screen/diary/plan/PlanDialogs.kt \
  $K/screen/diary/DiaryScreen.kt
git commit -m "feat: the pencil's list grouped by pillar with reason lines"
```


---

### Task 8: Evening details: +N extra, Move to ⟨day⟩, Ticked for you, 18:00

**Files:**
- Modify: `screen/diary/WeekState.kt`, `screen/diary/DiaryState.kt`, `screen/diary/DiaryViewModel.kt`, `screen/diary/plan/PlanSection.kt`, `screen/diary/plan/PillarWeekStrip.kt`, `screen/diary/DiaryScreen.kt`

**Depends on:** Task 2 (`Moves`), Task 5 (`DiaryExtras`, `WeekState.kt`), Task 6 (`PillarWeekStrip`'s fresh-start label).

The catch-ups from the Plan 1 and Plan 3 reviews:
- **"+N extra"** under a pillar's bar when it goes past its target (spec 5).
- **"Move to Thursday"** on the evening chip (spec 6.4), found with the same `Moves` functions the move uses. For that, the diary now reads the plans and items of the week ahead and the free-time settings.
- **"· ticked for you"** on fixed blocks the evening check ticked (spec 6.4).
- **The evening check opens by itself at 18:00** while today's diary stays open (decision 15). Swap mode closes when it does.

- [ ] **Step 1: `screen/diary/WeekState.kt`: Import DaySettings**

Replace:

```kotlin
import com.example.todoapp.db.data.plan.DayPlan
```

with:

```kotlin
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.plan.DaySettings
```


- [ ] **Step 2: `screen/diary/WeekState.kt`: The extras also carry the days ahead and the free-time settings**

Replace:

```kotlin
data class DiaryExtras(
    /** This day's plan and those of the week's earlier days. */
    val plans: List<DayPlan>,
```

with:

```kotlin
data class DiaryExtras(
    /** Plans from the start of this day's week to a week after today, for "opens the week" and room ahead. */
    val plans: List<DayPlan>,
    /** Free minutes per weekday, for days ahead that have no plan yet. */
    val settings: List<DaySettings>,
```


- [ ] **Step 3: `screen/diary/DiaryState.kt`: Imports for the move chip**

Replace:

```kotlin
import com.example.todoapp.db.data.plan.DayPlan
```

with:

```kotlin
import com.example.todoapp.db.data.plan.DayPlan
import com.example.todoapp.db.data.plan.DaySettings
```


- [ ] **Step 4: `screen/diary/DiaryState.kt`: Import Moves**

Replace:

```kotlin
import com.example.todoapp.domain.History
```

with:

```kotlin
import com.example.todoapp.domain.History
import com.example.todoapp.domain.Moves
```


- [ ] **Step 5: `screen/diary/DiaryState.kt`: Import isoDayNumber**

Replace:

```kotlin
import kotlinx.datetime.LocalDate
```

with:

```kotlin
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
```


- [ ] **Step 6: `screen/diary/DiaryState.kt`: Points past a pillar's target**

Replace:

```kotlin
data class PillarProgress(val pillar: PillarType, val earned: Int, val target: Int) {
    val share: Float get() = if (target == 0) 0f else (earned.toFloat() / target).coerceAtMost(1f)
}
```

with:

```kotlin
data class PillarProgress(val pillar: PillarType, val earned: Int, val target: Int) {
    val share: Float get() = if (target == 0) 0f else (earned.toFloat() / target).coerceAtMost(1f)
    /** Points past the target, shown as "+N extra" on this pillar only (spec 5). */
    val extra: Int get() = (earned - target).coerceAtLeast(0)
}
```


- [ ] **Step 7: `screen/diary/DiaryState.kt`: Rows know where Move it goes and whether the evening check ticked them**

Replace:

```kotlin
    /** Moved in from another day; it can't be swapped. */
    val movedIn: Boolean,
)
```

with:

```kotlin
    /** Moved in from another day; it can't be swapped. */
    val movedIn: Boolean,
    /** Where "Move it" would send it: a weekday, or "today" from an earlier day. Set on planned flexible rows. */
    val moveTo: String?,
    /** A fixed block the evening check ticked (spec 6.4: "Ticked for you"). */
    val tickedForYou: Boolean,
)
```


- [ ] **Step 8: `screen/diary/DiaryState.kt`: Work out each planned row's move day**

Replace:

```kotlin
        val rows = todayTasks.mapNotNull { task -> byId[task.missionId]?.let { toRow(task, it, tasks) } }
```

with:

```kotlin
        // "Move it" names its day, found the way PlanRepository.moveToNextRoom finds it.
        val after = Moves.anchor(date, today)
        val room = Moves.roomByDay(after, missions.associate { it.missionId to it.durationMinutes }, tasks) { day -> freeOn(day, extras) }
        val rows = todayTasks.mapNotNull { task ->
            val mission = byId[task.missionId] ?: return@mapNotNull null
            val moveTo = if (task.taskState == TaskState.PLANNED && !task.isFixedBlock) {
                Moves.target(mission, after, tasks, room).let { if (it == today) "today" else it.weekdayName() }
            } else null
            val ticked = task.isFixedBlock && task.taskState == TaskState.DONE && plan?.fixedTicked == true
            toRow(task, mission, tasks, moveTo, ticked)
        }
```


- [ ] **Step 9: `screen/diary/DiaryState.kt`: Fill the new row fields**

Replace:

```kotlin
    private fun toRow(task: TodayTask, mission: Mission, tasks: List<TodayTask>): PlanRow {
```

with:

```kotlin
    /** Free minutes on [day]: its plan's if it has one, else its weekday's setting, as in PlanRepository. */
    private fun freeOn(day: LocalDate, extras: DiaryExtras): Int {
        val key = MyDate.fromLocalDate(day).dateString
        extras.plans.firstOrNull { it.date == key }?.let { return it.freeMinutes }
        val weekday = day.dayOfWeek.isoDayNumber
        return extras.settings.firstOrNull { it.weekday == weekday }?.freeMinutes ?: DaySettings.defaultFor(weekday)
    }

    private fun toRow(task: TodayTask, mission: Mission, tasks: List<TodayTask>, moveTo: String?, tickedForYou: Boolean): PlanRow {
```


- [ ] **Step 10: `screen/diary/DiaryState.kt`: Pass them through**

Replace:

```kotlin
            movedTo = movedTo,
            movedIn = task.movedFromDate != null,
        )
```

with:

```kotlin
            movedTo = movedTo,
            movedIn = task.movedFromDate != null,
            moveTo = moveTo,
            tickedForYou = tickedForYou,
        )
```


- [ ] **Step 11: `screen/diary/DiaryViewModel.kt`: Import delay**

Replace:

```kotlin
import kotlinx.coroutines.Job
```

with:

```kotlin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
```


- [ ] **Step 12: `screen/diary/DiaryViewModel.kt`: The evening check now also opens on its own**

Replace:

```kotlin
    private val _eveningOpen = MutableStateFlow(isEvening)
    /** Today's list can switch to the evening check at any time; past days always show it. */
```

with:

```kotlin
    private val _eveningOpen = MutableStateFlow(isEvening)
    /** Today's list switches to the evening check at 18:00, or anytime by hand; past days always show it. */
```


- [ ] **Step 13: `screen/diary/DiaryViewModel.kt`: Read the days ahead and the free-time settings too**

Replace:

```kotlin
    private val extras = combine(
        // This day's plan and the week's earlier ones, which decide whether this day opens the week.
        planRepository.observePlans(WeekMath.weekStart(currentDate), currentDate),
        weekRepository.observeSummary(WeekMath.weekStart(currentDate).minus(7, DateTimeUnit.DAY)),
        weekRepository.observeOffers(WeekMath.weekStart(today)),
    ) { plans, summary, offers -> DiaryExtras(plans, summary, offers) }
```

with:

```kotlin
    /** The last day the diary looks at: a week past this day or today, whichever is later, for room to move into. */
    private val lookAhead = maxOf(currentDate, today).plus(7, DateTimeUnit.DAY)

    private val extras = combine(
        // The week's earlier plans decide whether this day opens the week; later ones give the room ahead.
        planRepository.observePlans(WeekMath.weekStart(currentDate), lookAhead),
        planRepository.observeSettings(),
        weekRepository.observeSummary(WeekMath.weekStart(currentDate).minus(7, DateTimeUnit.DAY)),
        weekRepository.observeOffers(WeekMath.weekStart(today)),
    ) { plans, settings, summary, offers -> DiaryExtras(plans, settings, summary, offers) }
```


- [ ] **Step 14: `screen/diary/DiaryViewModel.kt`: Tasks up to the same day**

Replace:

```kotlin
        todayTaskRepository.observeBetween(
            currentDate.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY),
            currentDate.plus(7, DateTimeUnit.DAY),
        ),
        extras,
```

with:

```kotlin
        todayTaskRepository.observeBetween(currentDate.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY), lookAhead),
        extras,
```


- [ ] **Step 15: `screen/diary/DiaryViewModel.kt`: Start the 18:00 watch**

Replace:

```kotlin
            } finally {
                planReady.value = true
            }
        }
    }
```

with:

```kotlin
            } finally {
                planReady.value = true
            }
        }
        if (isToday && !isEvening) viewModelScope.launch { openEveningAtSix() }
    }

    /**
     * Opens the evening check at 18:00 while today's diary stays on screen. It checks the clock every
     * half minute rather than sleeping until 18:00, because a sleeping phone pauses timers.
     */
    private suspend fun openEveningAtSix() {
        while (true) {
            delay(EVENING_CHECK_MILLIS)
            val clock = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            // After midnight CalDiaryViewModel moves the diary to the new day, with a new view model.
            if (clock.date != currentDate) return
            if (clock.hour >= EVENING_HOUR) {
                if (!_eveningOpen.value) {
                    _eveningOpen.value = true
                    act { planRepository.tickFixedBlocks(currentDate) }
                }
                return
            }
        }
    }
```


- [ ] **Step 16: `screen/diary/DiaryViewModel.kt`: How often to look at the clock**

Replace:

```kotlin
    companion object {
        const val EVENING_HOUR = 18
    }
```

with:

```kotlin
    companion object {
        const val EVENING_HOUR = 18
        private const val EVENING_CHECK_MILLIS = 30_000L
    }
```


- [ ] **Step 17: `screen/diary/plan/PlanSection.kt`: Move it names its day**

Replace:

```kotlin
                    SmallChip("Move it") { actions.onMove(row.taskId) }
```

with:

```kotlin
                    SmallChip(row.moveTo?.let { "Move to $it" } ?: "Move it") { actions.onMove(row.taskId) }
```


- [ ] **Step 18: `screen/diary/plan/PlanSection.kt`: Ticked for you**

Replace:

```kotlin
    TaskState.PARTIAL -> "Did some · half points"
    else -> row.reason
}
```

with:

```kotlin
    TaskState.PARTIAL -> "Did some · half points"
    else -> if (row.tickedForYou) "${row.reason} · ticked for you" else row.reason
}
```


- [ ] **Step 19: `screen/diary/plan/PillarWeekStrip.kt`: Import the pillar ink**

Replace:

```kotlin
import com.example.todoapp.screen.basicutils.TrackBeige
```

with:

```kotlin
import com.example.todoapp.screen.basicutils.TrackBeige
import com.example.todoapp.screen.basicutils.pillarInk
```


- [ ] **Step 20: `screen/diary/plan/PillarWeekStrip.kt`: +N extra under a bar**

Replace:

```kotlin
                        Box(
                            modifier = Modifier.fillMaxWidth(progress.share).fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp)).background(pillarStrong(progress.pillar))
                        )
                    }
```

with:

```kotlin
                        Box(
                            modifier = Modifier.fillMaxWidth(progress.share).fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp)).background(pillarStrong(progress.pillar))
                        )
                    }
                    if (progress.extra > 0) {
                        Text(
                            "+${progress.extra} extra",
                            fontSize = 11.sp, color = pillarInk(progress.pillar),
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
```


- [ ] **Step 21: `screen/diary/DiaryScreen.kt`: Leave swap mode when the evening check opens on its own**

Replace:

```kotlin
    var editMode by remember(selection) { mutableStateOf(false) }
```

with:

```kotlin
    var editMode by remember(selection) { mutableStateOf(false) }
    LaunchedEffect(evening) { if (evening) editMode = false }
```


- [ ] **Step 22: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 23: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (it has rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/screen/diary/WeekState.kt \
  $K/screen/diary/DiaryState.kt \
  $K/screen/diary/DiaryViewModel.kt \
  $K/screen/diary/plan/PlanSection.kt \
  $K/screen/diary/plan/PillarWeekStrip.kt \
  $K/screen/diary/DiaryScreen.kt
git commit -m "feat: +N extra, move-to day, ticked for you, and the evening check at 18:00"
```


---

### Review fixes after Task 8

Each task's review asked for fixes. They were made after Task 8 and before Task 9 (the F-items below), and one more batch after Task 9 (the C-items). The decisions above and the checklist below already describe the result; Task 9's spec text includes them.

- `ea1a889`: the final version 6 (`Mission.paceSince`, `TodayTask.autoTicked`, one offer per mission a week) and the version 7 rules in `Database.kt`. F1, F23 (the column), F24, F41 (the column).
- `c250707`: the close-out and moves. F2 (the repository), F3 to F13, F41 (the repository).
- `149e7a3`: pace offers judge only the current pace. F21 to F23, F25 to F29.
- `d868871`: plan notes for the three commits above.
- `b8f011b`: the Last week card, the stickies and the diary state. F2 (`canTakeBack`), F14 to F20, F30, F31, F34, F35, F38, F41 (the diary), F42, F45, F46, F48, F50.
- `7b74fa1`: the diary rows, the pencil list and the evening details. F2 (the box), F32, F33, F36, F37, F39, F40, F43, F47, F49.
- `cea1e27`: plan notes for the two diary commits, with F44 as a known follow-up.
- After Task 9:
  - `5a70919`: one transaction for each move and take-back (`TodayTaskDao.move`, `TodayTaskDao.takeBack`), no second count after a fast "Move", crowns on reopened items, opened weeks from their own items, and a logged close-out failure. C1 to C5.
  - `6975f8a`: the take-back cue, "Moved from last ⟨weekday⟩", the announced notices, stable list keys, the chip text style and the diary state's single passes. C8, C9, C11 to C14.
  - The commit with this section: plan and checklist corrections and spec 7.2 (Task 9 Step 8). C6, C7, C10, C15.

---

### Task 9: iOS compile, spec sync and hand-off

**Files:**
- Modify: `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`

**Depends on:** Tasks 1 to 8.

Compiles the shared module for iOS, brings the spec in line with what was built, and hands the build over for testing.

- [ ] **Step 1: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Compile for iOS**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :shared:compileKotlinIosSimulatorArm64 --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`: Section 5.2: how offers were built**

Replace:

```markdown
At most two offers appear per week, on Monday's diary, as sticky notes. "Not yet" suppresses the same offer for 4 weeks. Only an accepted offer changes the target.
```

with:

```markdown
At most two offers appear per week, on Monday's diary, as sticky notes. "Not yet" suppresses the same offer for 4 weeks. Only an accepted offer changes the target.

As built in plan 2:
- Offers are made once, on the first open of a week, and show on that day's diary. Offers to ease off come first. A finish line that ends before the end of that week gets none.
- Offers judge only the current pace. Any answer starts that mission's run of weeks again, so "Keep it" asks again only after another 3 slipped weeks. So does a change of pace or weight, in the editor or by an accepted offer: only weeks from that day on count. A week the app wasn't opened in breaks a run.
- "Slipped" means done less than half as often as it was due that week. "Full" means 80% of its own points at today's weight, as for a pillar. "Late" means done in a later week than the one it was due in. Two step-ups in one week share its free hours.
- Lowering goes from every day to 6× a week, or from N× to N−1× (a once-a-week habit isn't offered). Lengthening adds one day, week or month, within the editor's limits. A gentle start steps up one a week, toward its goal. Monthly habits get no offers yet.
- "Pause a month" sets the mission's pause date a month out and takes it off today's list. A finish line that ends within the month isn't offered a pause.
- Saving a new pace or weight in the editor withdraws that mission's unanswered offers for the week.
```


- [ ] **Step 4: `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`: Section 6.4: how midnight resolution was built**

Replace:

```markdown
- Anything still unresolved at midnight follows its "if a day goes sideways" answer automatically.
```

with:

```markdown
- Anything still unresolved at midnight follows its "if a day goes sideways" answer automatically. As built, this runs on the first open of a new day. "Move it" carries an item up to 6 days, only to a day the mission is live on, and only when the mission has nothing else coming; otherwise the item is skipped. Unticking an item on an earlier day marks it skipped. Ticking a moved item takes the move back while its copy is still planned. Adding an item on an earlier day adds it as done.
- "Ticked for you" stays on a fixed block only until it's changed by hand.
- The Move chip names the day it will use ("Move to Thursday", or "next Thursday" for the same weekday a week on). It is left out when the mission can't go anywhere in the coming week, and "Skip today" stays.
```


- [ ] **Step 5: `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`: Section 10: data is kept from version 5 on**

Replace:

```markdown
The database is Room, currently version 4. **Existing data is not kept:** the new schema replaces the old one and the database is wiped (destructive migration). There is no data migration.
```

with:

```markdown
The database is Room, version 6. Version 5 replaced the old schema and wiped anything older (destructive migration), with no data migration. **From version 5 on, data is kept.** Version 6 (plan 2) adds the two week tables, `Mission.paceSince` and `TodayTask.autoTicked` through `@AutoMigration(from = 5, to = 6)`, and each later version adds an auto migration from the one before. Installing an older build over a newer database wipes it.
```


- [ ] **Step 6: `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`: Section 10: the week tables as built**

Replace:

```markdown
| New `WeekSummary` | Added in plan 2 (week cycle): week start, per-pillar earned and target points, balanced flag. |
| New `PaceOffer` | Added in plan 2: mission, offer type, week shown, response, snooze-until date. |
```

with:

```markdown
| New `WeekSummary` | Added in plan 2 (week cycle): one row per finished week, keyed by its Monday, with earned and target points for each of the four pillars and a balanced flag (every pillar full, with something due). Targets are fixed at close-out. Points include archived missions and update if a day of that week is ticked later. |
| New `PaceOffer` | Added in plan 2: mission, offer type (`LOWER`, `LENGTHEN`, `STEP_UP_GENTLE`, `STEP_UP`), week shown, response (`ACCEPTED`, `KEPT`, `PAUSED`, `NOT_YET`, `WITHDRAWN` when the mission's pace was edited first, or none yet), snooze-until date. At most one per mission a week (a unique index). |
```


- [ ] **Step 7: `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`: Section 10: the two new columns**

Replace:

```markdown
| `Mission` | One table holds everything: title, `pillar` (enum name, including RECHARGE), `kind`, `weight` (0–3), `durationMinutes`, `effort`, `ifMissed`, pace fields (`count` + `period`, `goalCount`, `intervalCount` + `intervalUnit`, `weekDays`, `startTime`, `endTime`), `startDate`, `endDate`, `pausedUntil`, `motivation`, `active`. The old `mission_frequency`, `pillar` and `mission_pillar_mapping` tables are gone; pillars are a fixed enum in code. `ideaId` comes with the ideas library. |
| `TodayTask` | `state` (PLANNED, DONE, PARTIAL, MOVED, SKIPPED), `pointsEarned`, `isFixedBlock`, `crowned` (replaces the TOP3 tag), `movedFromDate`, `reasonCode`, `reasonArgs`, `sortOrder`. The reminder and sub-task tables are dropped. |
```

with:

```markdown
| `Mission` | One table holds everything: title, `pillar` (enum name, including RECHARGE), `kind`, `weight` (0–3), `durationMinutes`, `effort`, `ifMissed`, pace fields (`count` + `period`, `goalCount`, `intervalCount` + `intervalUnit`, `weekDays`, `startTime`, `endTime`), `startDate`, `endDate`, `pausedUntil`, `motivation`, `active`, and `paceSince` (plan 2: the day the pace or weight last changed; pace offers judge only the weeks from then on). The old `mission_frequency`, `pillar` and `mission_pillar_mapping` tables are gone; pillars are a fixed enum in code. `ideaId` comes with the ideas library. |
| `TodayTask` | `state` (PLANNED, DONE, PARTIAL, MOVED, SKIPPED), `pointsEarned`, `isFixedBlock`, `crowned` (replaces the TOP3 tag), `movedFromDate`, `reasonCode`, `reasonArgs`, `sortOrder`, and `autoTicked` (plan 2: a fixed block the evening check ticked, cleared by any change by hand). The reminder and sub-task tables are dropped. |
```


- [ ] **Step 8: `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`: Section 7.2: a move to the same weekday**

Replace:

```markdown
Singular and edge forms: "last day" instead of "1 days left", "Last done yesterday", "Due yesterday", "Every day" when there's no streak yet, "You usually take one break a week.", and "Fixed block" when a block has no times.
```

with:

```markdown
Singular and edge forms: "last day" instead of "1 days left", "Last done yesterday", "Due yesterday", "Every day" when there's no streak yet, "You usually take one break a week.", "Fixed block" when a block has no times, and "Moved from last {weekday}" when an item lands on the weekday it came from.
```


- [ ] **Step 9: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a file (it has rewritten 4.json before)
git add docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md
git commit -m "docs: sync the redesign spec with plan 2"
```


- [ ] **Step 10: Hand over for manual testing**

Tell the owner the build is ready and point them to the checklist below.

---

## Manual hand-test checklist

### Before you start

**Install over the Plan 3 build, without clearing storage.** Section A checks that your missions and history survive the move to version 6. If a Plan 2 build from before the review fixes was ever installed, clear storage first: its version 6 isn't the final one, so Room would stop on launch.

**Moving the date.** Most of these checks need a new day or a new week. Change the date on the device instead of waiting:
- **Android:** Settings → System → Date & time → turn off "Set time automatically", then set the date (and the time for section H). Then swipe the app away and open it again. While it's open, the diary also moves to the new day within a minute, but reopening is quicker and certain.
- **iOS simulator:** it follows the Mac's clock. Go to System Settings → General → Date & Time, turn off "Set time and date automatically", set the date, then relaunch the app.
- **Only move forward.** Moving back leaves plans, moved items and week summaries dated "in the future", which then look odd. When you're done, clear the app's storage and turn automatic time back on.
- **"Opens the week"** means Monday, or the first day of the week you open the diary on. The "Last week" card shows on that day's page, and the offers only on that day, while it's today. A day you never opened doesn't open the week, so the card isn't repeated there.

### A. The upgrade keeps your data
1. Note a few missions and today's ticks, then install this build over the old one.
2. The app opens, and the Missions tab and today's list are as before. If it stops on launch, send the error. "Migration didn't properly handle" means the device's database wasn't the committed version 5. Clearing storage gets past it, at the cost of that data.
3. A fresh install (storage cleared) opens on the empty diary, as in Plan 3's checklist.

### B. Closing a day
Do this on a Tuesday to Saturday, so tomorrow is in the same week.
1. Make or pick four missions:
   - a weekly habit with "Move it to another day" (say "Learn guitar", 3× a week)
   - a finish line with "It has to be that day"
   - a habit with "Skip it, no catch-up"
   - a fixed block for today
2. Get the three flexible ones onto today's list (the pencil adds any that weren't picked). Tick nothing and don't open the evening check.
3. Move the date to tomorrow and reopen. Today's list shows "Learn guitar" with "Moved from ‹yesterday's weekday›". The planner doesn't add a second one.
4. Tap yesterday in the week strip. Check:
   - The block is struck through and reads "9:00 to 13:00 · ticked for you" (its own times).
   - "Learn guitar" reads "Moved to ‹today's weekday›".
   - The other two read "Skipped today" and earn nothing.
   - There are no Did some / Move / Skip chips on yesterday's page.
5. On yesterday's page, tap a skipped item's box: it ticks and earns its points. Tap again: it goes back to "Skipped today", not to planned.
6. Switch to Missions and back to the Diary, or reopen the app. Nothing changes a second time.
7. Leave "Learn guitar" unticked today, then move the date 2 days ahead. Check:
   - It's carried once more, from today's date.
   - The day you skipped over shows "Nothing was planned for this day." Nothing comes from it.
8. Leave a "Move it" item unticked, then move the date 8 days ahead. On its own day it reads "Skipped today". The new day's list has no "Moved from …" for it (6 days at most).
9. Taking a move back. Leave two short "Move it" items unticked (so both fit today), move the date to tomorrow and reopen. Both are on today's list as "Moved from …". Tick one of the two copies today. Then on yesterday's page:
   - Before ticking: the moved item whose copy is still unticked reads "Moved to ‹today's weekday› · tick to take it back", and its box is less faded than the other's.
   - Tick it. It ticks and earns its points, and its "Moved from …" copy leaves today's list.
   - The moved item whose copy you ticked can't be ticked: its box is off, and it still reads "Moved to ‹today's weekday›", with nothing after it. A screen reader calls its box "‹title›, moved", and the other one's (before you tick it) "Take back the move of …".
   - Untick the one you took back: it reads "Skipped today", and no copy comes back.

### C. Monday: the Last week card
1. Earlier in a week (Tuesday to Sunday), set up:
   - Some missions, with a few things ticked.
   - One "Move it" item left unticked.
   - A new every-so-often mission, "every week", with "Skip it, no catch-up". It's due straight away ("First one · you aim for every week"). Leave it unticked.
   Note the four pillar bars.
2. Move the date to the next Monday, before 18:00, and reopen.
3. Under the strip, which reads "THIS WEEK · A FRESH START" until something is ticked, sits the card:
   - "LAST WEEK", with last week's dates on the right ("Oct 26 to Nov 1").
   - Star: "N of M points". Each pillar counts up to its target, out of all targets. Points past a target show as " · +K extra". Without extra, N is the sum of the bars you noted, plus any fixed blocks the close-out ticked.
   - Heart: "X of 4 pillars full". If every pillar reached 80%, a crown and "All 4 pillars full · a balanced week".
   - Wand: "⟨Learn guitar⟩ moved to Monday." and "⟨‹the every-week mission›⟩ is still due, so it moved into this week." Titles sit on chips in their pillar's color.
4. Today's list has the carried item ("Moved from …"), and the every-week mission reads "Due N days ago".
5. Move the date to Tuesday. Tuesday's page has no card; Monday's page (week strip) still shows it, with the points and pillars lines only: no wand lines, and its strip reads "THIS WEEK", not "A FRESH START". A later day of that week that you never opened has no card either.
6. Go to last week's Friday and tick something you'd skipped, then back to Monday. The card's points went up; its targets didn't change.
7. A week with nothing to report reads "Nothing to catch up on." A week you didn't open on Monday shows the card on the first day you did open.
8. A week where no pillar had a target but you ticked something (an every-so-often mission added with the pencil) reads "+N extra" on the star line, not "0 of 0".
9. That same week gets no crown: with nothing due it isn't a balanced week, so the line has the heart and no "a balanced week".

### D. Pace offers, quickly, with the seed
1. In `db/SampleData.kt` set `SEED_SAMPLE_MISSIONS = true` (don't commit this), build, clear the app's storage, and open the app.
2. Under the Last week card are two stickies:
   - **Yellow, wand, "A SMALL CHANGE?":** "⟨New ideas⟩ has slipped 3 weeks in a row. That's fine. Want to make it 2× a week for now? You can step back up anytime." Buttons: "Make it 2×" (filled), "Keep 3×", "Pause a month".
   - **Blue, flag, "GOING WELL":** "⟨Guitar⟩ has gone well 3 weeks running. Ready for 3× a week?" Buttons: "Step up" (filled), "Not yet".
   - Both titles are on light blue Life chips.
   - The card's last line reads "Nothing to catch up on.", next to a small dot rather than the wand (plan 4 keeps the wand for lines where the app adjusted something)
3. Tap "Not yet" on Guitar. The sticky goes, and Missions still shows Guitar as "2× a week, up to 3× · big".
4. Tap "Make it 2×" twice, fast. The sticky goes, and Missions shows New ideas as "2× a week · small", lowered once, not to 1×. Today's list was re-picked, unless you'd tapped Looks good.
5. Reopen the app: no offers come back today.
6. Move the date to next Monday. No offer for Guitar (snoozed) or New ideas (its run of weeks restarted).
7. Clear storage again (the seed runs again) and try the other buttons:
   - "Pause a month": New ideas isn't on today's list any more, and its card reads "Paused until ‹a month from today›".
   - "Step up" on Guitar: the card reads "3× a week · big".
   - "Keep 3×": nothing changes.
8. Clear storage once more. On Missions, edit New ideas from 3× to 4× a week and save. Back on the diary its sticky is gone (withdrawn); Guitar's is still there. Move the date to next Monday: no offer for New ideas, because its run of weeks starts again from the edit.
9. Turn the seed back off when done.

### E. Pace offers by hand (slower, optional)
- **Slipped.** Make a habit at 3× a week, going straight to 3× rather than starting gently, and never tick it. Open the app at least once in each of the next two weeks too, moving the date a week at a time: a week the app isn't opened in breaks the run, so a holiday doesn't read as slipped. Each of those opens needs a list that isn't empty: an item moved in from another day doesn't count as the week's own. On the Monday three weeks after the week you made it, the yellow "has slipped 3 weeks in a row" sticky offers 2×. A habit at 2× a week offers "Make it once a week".
- **Ran late (every so often).** Count weeks from the one you start in, week 0:
  1. Week 0, Monday: make one "every week" with "Move it". Don't tick it that week.
  2. Week 1, Tuesday: tick it (late once).
  3. Week 3, Wednesday: tick it (late twice).
  4. Week 5, Monday: a sticky says it "has slipped 3 times in a row" and offers "every 2 weeks". The buttons read "Every 2 weeks", "Keep every week", "Pause a month".
- **Going well (steady).** Make a habit at once a week, without starting gently. Tick it in each of 4 weeks in a row, moving the date a week at a time. The next Monday: "has gone well 4 weeks running. Ready for 2× a week?" It's only offered if the week's free time has room for one more.
- **Going well (gentle).** Make a habit at 2× a week and start gently (1× a week). Tick it once in each of 3 weeks. The next Monday: "Ready for 2× a week?" "Step up" makes it "2× a week". A gentle start steps one a week at a time: one at 4× that started at 2× offers 3× first, and keeps "up to 4×".
- **Finish lines.** One that ends before the end of this week gets no offer. One that ends within a month has no "Pause a month" on its sticky.

### F. The pencil
1. Tap the pencil. "Add to today" lists missions under small pillar headings (HEALTH, WEALTH, LOVE, LIFE, RECHARGE), in that order. Each has its reason line and its length. Examples:
   - "2 of 3 this week · 4 days left"
   - "First one · you aim for every 2 weeks"
   - "Last done 3 days ago · you aim for every 2 weeks"
   - "Every day · 5-day streak"
   - "You usually take 2 breaks a week."
2. Fixed blocks, paused missions and anything already on today's list aren't listed.
3. Tap a mission: it joins today's list as "Added by you" and leaves the dialog. Tapping twice fast adds it once. "Done" closes the dialog.
4. On an empty install it says "No missions yet. Add some on the Missions tab."
5. Go to an earlier day's page and add a mission with the pencil. The dialog reads "Add to this day". The mission joins that day's list already ticked, as "Added by you", and earns its points. If that day was last week, Monday's Last week card counts the points too.
6. Nothing left to add:
   1. With only fixed blocks, paused or not-yet-started missions, the dialog reads "Nothing else to add. Fixed blocks, paused and not-yet-started missions aren't listed." The list behind it reads "Nothing picked yet. Tap the pencil to see what can be added."
   2. With missions that can be added, add every one of them, one by one. After the last, the dialog reads "Everything is already on today's list." ("this day's list" on another day).
7. After a tap the dialog says "Added" (then "Added · 2 so far"), and a screen reader says it too, from the first tap on. The list doesn't jump when "Added" first shows. The pillar names are headings.

### G. +N extra
Find a pillar whose bar reads 0/0 this week, for instance one whose only mission is every-so-often and not due. Add that mission with the pencil and tick it. The bar reads "2/0", with "+2 extra" under it in the pillar's ink. The other bars don't change. Before that, a pillar with nothing due reads "0/0" with a full bar, not an empty one.

### H. The evening check
1. Set the time to 17:59 on a weekday, open today's diary and leave it on screen. Tap "Swap one" so swap mode is on. At 18:00 the list switches to the evening check by itself:
   - The label reads TODAY.
   - Swap mode is off. The same goes for the "change" dialog if you had it open.
   - Fixed blocks are struck through and read "‹times› · ticked for you".
2. Each unticked pick offers "Did some · +N", "Move to ‹tomorrow's weekday›" and "Skip today". Tap "Move to …": the row reads "Moved to ‹that same weekday›". On another pick, tap "Move to …" and then "Did some" straight away: it's moved, and today earns nothing for it.
3. In Missions → Free time, set tomorrow's weekday to 0 min and save. Back on the diary, the chip names the day after tomorrow.
4. "Back to the plan" still switches back by hand, and it stays there after 18:00.
5. Untick a block in the evening check, then open the next day. That block's day shows it as "Skipped today".
6. Untick another block and tick it again by hand: it's done, and it no longer reads "ticked for you".
7. The Move chip's day. In Missions → Free time set every weekday except today's to 0 min. The chip reads "Move to next ‹today's weekday›", and after tapping it the row reads "Moved to next ‹that weekday›". Put the free times back afterwards. When you later move the date a week ahead, the copy on that day reads "Moved from last ‹that weekday›".
8. A mission that can't move. Make a finish line with the earliest end date the editor allows, then move the date forward to its last day. Add it to the list if it isn't there. In the evening check it offers "Did some" and "Skip today" only, with no Move chip.
9. Close the evening check with "Back to the plan", then switch tabs and come back: it shows the evening check again. This is known (see the follow-ups at the end).
10. Crowns on skipped items. Crown a pick (hold its title), then tap "Skip today" on it in the evening check. Tick its box: it's done and still crowned. Untick it and skip it again, then crown three other picks. Tick it now: it's done, but its crown is gone, since the day already has three.

### I. Look and feel
- The stickies tilt and have a soft shadow. All their buttons are at least 44dp tall and peach (filled or outlined). Answer one of two: it dims, then fades, and the rest slide up. The other keeps its tilt.
- The chips' text is readable on every pillar color. Each chip has a thin outline in its pillar's color, and a chip on a one-line sentence has room above and below its text.
- The pencil is easy to hit, and a screen reader calls it "Add to today" ("Add to this day" on another day).
- With TalkBack or VoiceOver, a sticky reads as one group, and "LAST WEEK" and the sticky headers are headings.
- The dialog's "Done" is dark peach.
- Nothing in the new copy uses "missed", "failed", "lagging" or an exclamation mark.

### J. iOS
`:shared:compileKotlinIosSimulatorArm64` passes (Task 9). If you run the simulator, change the Mac's date to move days, as above.

---

## What comes next (other plans)

- **Plan 4, metrics:**
  - **Finished weeks:** `WeekRepository.observeSummaries()` gives every finished week, oldest first. Read a row with `WeekSummary.earned(pillar)`, `.target(pillar)`, `.pillarsFull()`, `.balanced` (the crown on the calendar's week) and `.weekStartDate`.
  - **This week:** the current week has no row until it ends. Build it live, as `DiaryStateBuilder` does (`Points.earnedInWeek` with all missions, `Points.weeklyTarget` with the active ones).
  - **Stored targets:** fixed at close-out. Stored points include archived missions and update when a closed week's day is ticked. Rows start at the week of the first task.
  - **Gap weeks:** a week the app wasn't opened in still gets a row at the next open, with targets from the missions as they are then. It shows 0 earned, so it looks like a missed week. Tell it apart by having no `TodayTask` rows, if the metrics need to.
  - **Read only:** read summaries and offers through `WeekRepository`. Only `PlanRepository`'s close-out writes them, inside its lock.
  - **"What next week adjusts":** the material is in `pace_offer` (type, week, response, snooze) and in `WeekStateBuilder.adjusted`, which finds carried items and still-due rhythms. Move that function into `domain` if the metrics screen needs it too.
  - **Offer responses:** an offer from a past week whose response is still null was ignored. `WITHDRAWN` means the mission's pace or weight was edited before it was answered. `Mission.paceSince` is the day the pace last changed.
  - **Day rings:** add up `TodayTask.pointsEarned` per day and pillar. SKIPPED and MOVED items are "what didn't happen".
- **Next pass, ideas library:** `ideaId` on `Mission` is version 7. Add `AutoMigration(from = 6, to = 7)`, and commit `7.json` beside `6.json`. Keep the destructive fallback limited to versions 1 to 4. Follow the version 7 rules in `db/Database.kt`'s KDoc: a nullable `ideaId` (or one with a default), and `6.json` left as it is.
- **Small follow-ups, not scheduled:**
  - Monthly habits get no pace offers.
  - An offer ignored on its day isn't shown again that week.
  - The Missions tab doesn't show offers.
  - Installing an older build wipes the data (`fallbackToDestructiveMigrationOnDowngrade`).
  - Earlier days' skipped items read "Skipped today"; "Skipped" would read better there.
  - Easing-off stickies have no "Not now". One that's ignored lapses, and can come back next Monday.
  - If you close the evening check and then switch tabs or turn the phone, it opens again, because the new screen starts in the evening.
  - "Moved to" in the Last week card's adjusted lines includes moves tapped in the evening check.
  - A page older than yesterday doesn't name where "Move it" would go. Its "Moved to" line reads "another day" if the copy landed more than a week after that page's day.
