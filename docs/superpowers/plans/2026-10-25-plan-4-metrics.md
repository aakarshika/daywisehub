# Plan 4: Metrics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the "coming next" Metrics tab with the screen from spec 9. It shows one week at a time:
- four pillar rings;
- what next week adjusts;
- a week highlights card;
- pattern lines;
- a Monday-first month calendar whose day rings are split by points;
- one progress line per mission.

A seed option fills in five weeks of history, so all of it can be tried by hand straight away.

**Architecture:** The tab only reads. Nothing in this plan writes to the database or takes `PlanRepository`'s lock.
- **The view model.** `MetricsViewModel` combines all missions, a window of tasks, the stored week summaries (Plan 2's `WeekRepository.observeSummaries()`) and the milestones. It builds one `MetricsUiState` with `flowOn(Dispatchers.Default)`.
- **The rules.** Each section's rules are plain Kotlin in `domain`: `DayTasks`, `WeekScores`, `WeekHighlights`, `WeekAhead`, `Patterns`, `MonthGrid` and `MissionProgress`.
- **The builder.** `MetricsStateBuilder` (`screen/metrics/MetricsState.kt`) reads the inputs once and hands them to each of those.
- **The composables.** They live in `screen/metrics/`. `NavScreens` makes the view model with `remember`, keyed on the day (`CalDiaryViewModel.today`), and cancels it on dispose, like the other tabs.

There's no schema change.

**Tech Stack:** Kotlin Multiplatform (Kotlin 2.0.20, language level 1.9), Compose Multiplatform 1.7.1 (material3, foundation, Canvas), Room KMP 2.7.0-alpha01 (reads only), Koin 3.2, precompose 1.5.7, kotlinx-coroutines (`flatMapLatest`), kotlinx-datetime.

**Spec:** `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`, sections 5, 5.1, 7.1, 9 (the Metrics row) and 9.1. Mockup: `Metrics.dc.html` in the canvas project. Its numbers are examples.

**Builds on:**
- Plan 1 (`docs/superpowers/plans/2026-10-25-plan-1-foundation-and-daily-list.md`).
- Plan 3 (`docs/superpowers/plans/2026-10-25-plan-3-missions.md`).
- Plan 2 (`docs/superpowers/plans/2026-10-25-plan-2-week-cycle.md`), on branch `redesign/plan-2-week-cycle`. Plan 2 must be implemented first, with its review fixes.

**Ground rules for this plan**
- **No automated tests.** The owner tests by hand. Every task ends with a green build and a commit.
- **Build command.** `./gradlew` doesn't work here (the wrapper jar is missing). From the repo root run
  `/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60`
  and expect `BUILD SUCCESSFUL`. Task 9 also compiles for iOS.
- **Paths** are relative to `shared/src/commonMain/kotlin/com/example/todoapp/` unless they start with `shared/` or `docs/`.
- **Commits** stage files by path, never `git add -A`. No task changes the schema. If a build rewrites anything under `shared/schemas`, restore it with `git checkout` (builds have rewritten `4.json` before). This plan doesn't depend on the v6 identity hash, which Plan 2's review changes.
- **Every task builds on its own, in order.** Each task was checked this way: Plan 2's tasks were applied as written to a scratch checkout of `2be586e`, then Plan 4's Tasks 1..N on top, then a build. The code was then parsed back out of this document and compared with what compiled.
- **Find/replace edits** quote the current text exactly. Each "Replace" block appears once in its file at that point. Most of them edit files this plan creates. The only files from earlier plans that Tasks 1 to 9 edit are `di/DI.kt`, `screen/NavScreens.kt`, `screen/init/InitViewModel.kt` and the spec. None of the lines it quotes were changed by Plan 2. The review fixes after Task 8 edit a few more (see "Review fixes after Task 8").
- **Reads only.** The tab never writes. If a later change adds a write here, it goes through `PlanRepository`'s `write` helper (`NonCancellable` on `Dispatchers.Default`, behind its Mutex), as every other write does.

## What Plan 4 takes from Plan 2

Plan 4 uses only these Plan 2 pieces. If Plan 2's review renames one, adjust the line that uses it.

| Plan 2 symbol | Where | Used for |
|---|---|---|
| `WeekRepository.observeSummaries(): Flow<List<WeekSummary>>`, oldest first, and its `single { WeekRepository(get()) }` in `di/DI.kt` | `repo/WeekRepository.kt` | Finished weeks: rings, crowns, runs, how far back the arrows go |
| `WeekSummary` and its `balanced` column | `db/data/week/WeekSummary.kt` | The crown on a finished week |
| `WeekSummary.weekStartDate`, `.earned(pillar)`, `.target(pillar)` | `domain/WeekSummaries.kt` | Reading the pillar columns |
| `LinePart(text, pillar)` | `domain/PaceOffers.kt` | "What next week adjusts" lines with title chips |
| `PaceOffers.openedWeeks(tasks)` and `PaceOffers.full(mission, week, history)`, `internal` since the review fixes | `domain/PaceOffers.kt` | Away weeks (no items of its own), and "full weeks in a row" on mission lines |
| `WeekSummaries.balanced(earned, target)`, shared since the review fixes | `domain/WeekSummaries.kt` | This week's crown, by the stored rule |
| `ChipText(parts, fontSize, color, italic)` | `screen/diary/plan/WeekNotes.kt` | Drawing those chips (spec 7.1) |
| The close-out in `PlanRepository.ensurePlanned` | `repo/PlanRepository.kt` | It writes the summaries when the diary first opens in a new week, before the Metrics tab can be opened |

Plan 4 doesn't use `pace_offer`, `DiaryExtras`, `PillarProgress` or `WeekSummary.pillarsFull()`. Tasks 1 to 8 edit no file Plan 2 created. The review fixes after Task 8 do, keeping the diary as it was apart from two changes (M17, M18): `WeekStateBuilder.adjusted` now reads Metrics' `WeekAhead.carried`, and the Last week card's `HighlightRow` draws a quiet dot instead of the wand on "Nothing to catch up on.".

**Plan 2's review fixes.** Plan 4 was written against Plan 2 as it stands at `2be586e`. It fits the fixes in review:
1. **`WeekSummary.balanced` requires a total target above 0.** Plan 4 reads `summary.balanced` as the crown, with no check of its own. For this week, which has no row yet, `WeekScores.balanced` follows the same rule.
2. **Summaries of weeks the app wasn't opened show 0 points against today's targets.** Plan 4 calls a finished week with no items of its own an **away** week (`WeekScores.isAway`). That's Plan 2's rule for an opened week (its C4: items moved in don't count), shared as `PaceOffers.openedWeeks` since the review fixes. Away weeks aren't counted as light. See decision 5.
3. **`pace_offer.response` gains `WITHDRAWN`.** Plan 4 doesn't read `pace_offer`.
4. **`Mission.paceSince` and `TodayTask.autoTicked`** (new last parameters with defaults) and the unique index on `pace_offer`. Plan 4 builds `TodayTask` with named arguments only (in the seed) and never builds a `Mission`. It also doesn't read the v6 identity hash.
5. **`WeekStateBuilder.opensWeek` and the diary's "adjusted" lines change.** Metrics doesn't call `opensWeek`. Since the review fixes, the diary's adjusted lines and Metrics' finished weeks share one rule, `WeekAhead.carried` (decision 7).

## Decisions this plan makes

The spec leaves these open. Each is easy to change later.

1. **Archived missions.**
   - Their points count everywhere points are added up. The stored summaries already count them. This week's rings add points with `allMissions`, as the diary's bars do. Day rings and the weekday pattern look each task's mission up in `allMissions`.
   - They're left out of the mission lines and of "what next week adjusts". Archiving already removes their planned items from today on.
   - There's no archived list or restore yet (Plan 3 mentioned one).
2. **Which week a section follows.**
   - The rings, "what next week adjusts", the highlights card and the calendar follow the week on screen.
   - Patterns and mission lines are as of today, whatever week is shown.
3. **Moving between weeks.**
   - The left arrow goes back to the first stored week; the right arrow goes forward to this week. "Back to this week" shows whenever another week is on screen. Its room is kept on this week too, so the header doesn't jump.
   - A 44dp arrow fades and stops at either end. Steps are taken from the week asked for, not the one last built, so two quick taps move two weeks and never pass either end (changed after review).
   - A week older than the tasks read waits for its own window: the last state stays until the window arrives, so the week never shows "away" for a moment.
   - The title and dates read as one polite live region, with the title a heading. A date in another year than this week's carries its year: "Week of Dec 22, 2025", "Dec 22 to Dec 28, 2025".
   - The tab opens on this week each time. After midnight it starts again on the new day: `NavScreens` keys it on `CalDiaryViewModel.today`, which the diary's midnight check sets.
   - If building the state fails, the tab keeps what it showed and logs the error.
4. **Rings.**
   - A finished week shows its stored summary.
   - This week is worked out live, as the diary's bars are: points from all missions, targets from active ones. A past week without a row is also worked out live; that happens only for a moment, before the diary's close-out has run.
   - Each ring reads "full" or "light" at the 80% rule (`Points.isFull`).
   - A pillar with nothing due counts as full (spec 5), so it reads "0/0 full" with its ring drawn full.
   - Points past the target show as "+N extra" under the label.
   - Rings are 72dp at most and stay round on a narrow screen. At a large font size the numbers shrink to fit inside.
   - A screen reader hears "Health, 14 of 14 points, full" ("1 point" for one).
5. **Away weeks.** A finished week with no items of its own: the diary wasn't opened that week. Items moved in don't count, since a move can land in a week the app is then never opened in. That's Plan 2's rule for an opened week, shared as `PaceOffers.openedWeeks` and worked out once per build (changed after review).
   - Every ring reads "away" in grey, with "–" in place of its numbers and no arc. A screen reader hears "Health, away".
   - The card says "No points to count." and "The diary wasn't opened that week."
   - The calendar draws no rings in it, nor in the weeks before the first item.
   - It isn't counted in "balanced weeks out of the last 4".
   - It neither counts toward nor breaks a run (patterns, and "full weeks in a row" on mission lines).
   - Telling "away" from "not loaded" needs the week's tasks, so runs look back at most 51 weeks, inside the year of tasks the view model always reads.
6. **Crowns.**
   - A finished week's crown is its stored `WeekSummary.balanced`.
   - This week gets no crown on the calendar until it ends. The highlights card says "All 4 pillars full so far" with a crown icon. Its rule is the stored one, `WeekSummaries.balanced` (shared after review).
7. **What next week adjusts.** Only what the app actually knows, at most 3 lines. Each line names its mission on a chip, and the words around the chip never depend on the title (spec 7.1).
   - **One rule with the diary** (changed after review). `WeekAhead.carried` finds what a week carried into the week after: "move it" items that landed there, then every-so-often missions (and monthly habits) due that week and not done, short rhythms included (spec 5.1). One line per mission, at most 3. The diary's Last week card reads it too, so the two never disagree about a week; only the words differ.
   - A finished week, under "THE WEEK AFTER":
     - "⟨Guitar⟩ moved to Monday."
     - "⟨Call Grandma⟩ stayed due, so it moved into the week after."
   - This week, under "NEXT WEEK", the same rule words what's happening so far:
     - "⟨Guitar⟩ moved to Monday.": already carried into next week.
     - "⟨Call Grandma⟩ is due this week. If it waits, it moves into next week.", or "is still due. …" once its due date has passed.
   - This week only:
     - "⟨Plan winter trip⟩ comes due next week.": its last done date plus its interval falls next week. This is only shown for intervals longer than a week, since a weekly rhythm comes due every week.
     - "⟨New ideas⟩ is back next week.": its pause ends next week.
   - Otherwise "Nothing to catch up on."
   - The wand means the app adjusted something (spec 9.1), so only "moved to" and "stayed due" lines have it, here and on the diary's Last week card. Every other line has a quiet dot (changed after review).
   - Never a reason: the mockup's "Love was light. Next week makes room for ⟨Call sister⟩" isn't used, because the app doesn't know why.
8. **Highlights card.**
   - Points: each pillar is counted up to its target, and the rest shows as "+N extra", as on the "Last week" card. With nothing due it reads just "+N extra", as that card does, or "No points to count." when nothing was done either. An away week reads "No points to count."
   - Pillars full: "3 of 4 pillars full", or "All 4 pillars full · a balanced week" with a crown. This week, still open, says "so far" either way. A week with nothing due reads "Nothing was due that week." ("Nothing is due this week." for this one).
   - "2 balanced weeks out of the last 4": the 4 finished weeks ending with the week on screen. When this week is shown, it's the 4 weeks before it, as in the mockup. Away weeks aren't counted.
   - With fewer weeks, or none balanced: "1 balanced week so far", "No balanced week yet", "No balanced week in the last 3" or "No weeks to count yet."
9. **Patterns.** At most 3 lines, each shown only when the data holds it up:
   - "3 balanced weeks in a row.": 2 or more, back from last week.
   - "Health has been full 5 weeks running.": 3 or more weeks with something due, for at most 2 pillars. Only longer than the balanced run, which already says it.
   - A run that reaches the 51-week cap reads "for a year or more": "Health has been full for a year or more."
   - "Love mostly happens on Mondays.": in the last 8 weeks the pillar has 8 or more ticks (done or did-some), and more than half fall on one weekday, so "mostly" holds and no other day ties it. Only missions free to land on any day count: fixed blocks, daily missions and missions with preferred days are left out, since their days are set (changed after review). A weekday line keeps a slot, so run lines can't crowd it out.
   - When none hold: "Nothing stands out yet."
   - On any week but this one, a quiet "As of today" sits under the heading.
   - The mockup's "Your best weeks had 3 or more recharge breaks" is left out. It reads like a cause.
10. **Calendar.**
    - It shows the month of the shown week's Monday, in whole weeks from Monday to Sunday. Days of the next or previous month are greyed.
    - A day's ring is split by points per pillar, in pillar order, sized by points.
    - White is what didn't happen: skipped or moved items, the missing half of a did-some, and anything still planned on an earlier day. Today's planned items can still happen, so they aren't white.
    - A ring with points, or with nothing done (all white), has a 1dp edge outside and inside its band, about 3:1 on both tints (`DayRingEdge`). A past day with nothing on it is one thin plain line, still visible on the tinted row (changed after review).
    - Recharge has no slice. Days after today have no ring, nor do the days of an away week or of the weeks before the first item.
    - The shown week's row is tinted. The tints are flat, not the old gradient (spec 9.1). Today has an outline as well as a bold number.
    - Tapping a day opens it in the diary: `CalDiaryViewModel.selectDate`, then the Diary tab.
    - Each day cell is a button at least 44dp tall that reads, for example, "Tuesday, October 6. 8 points: Health 2, Wealth 6. 2 points didn't happen". Today with nothing yet reads "No points yet". A crown reads "Balanced week of Oct 12".
    - The card has the highlights card's inset.
11. **Mission lines.** One per active mission, as of today, in pillar order (Recharge last), with a pillar dot and the title as typed:

    | Mission | Line |
    |---|---|
    | Paused, not started, or past its end | "Paused until Dec 2", "Starts Nov 3", "Ended Oct 20" |
    | Fixed block | "4 of 5 this week", or "Nothing due this week" |
    | Recharge | "1 this week · about twice a week" |
    | Every so often | "Every 14 days on average · you aim for every 2 weeks" (2 or more ticks in the last year), "Last done 5 days ago · …", "Not done yet · …" |
    | Finish line with milestones (flag) | "Milestone 2 of 3 · due Dec 20", "All 3 milestones done" ("All 1 milestone done"). Milestone texts aren't shown. |
    | Every day | "26 of 30 days this month · best streak 14". Today counts once it's done. On the 1st: "Every day · best streak 14" |
    | N× a month | "1 of 2 this month" |
    | N× a week (habits, finish lines without milestones) | "2 of 3 this week", plus "· 2× a week for now" for a gentle start, or "· 4 full weeks in a row" |

    - **Full weeks in a row** use the pace offers' meaning of full (`PaceOffers.full`, changed after review): 80% of the mission's own points that week, a did-some earning half. The run stops at the mission's last pace change (`paceSince`), skips away weeks, and reads "· full every week for a year or more" at the 51-week cap.
    - Lines as of today leave out ticks dated after today: a daily habit's count and best streak, a rhythm's last done and average. "This week" counts keep the rest of the week.
    - Done days count only within the one-year look-back, so browsing an older month, which reads further back, doesn't change a line.
    - Titles are Medium weight, as on the Missions tab.
12. **How much is read.**
    - Tasks come from a year before today, or from 120 days before the shown month's first row if that's earlier, through the end of next week.
    - The window only moves when a week that old is shown: `distinctUntilChanged` before `flatMapLatest`.
    - Patterns and mission lines look back over that year, with one constant for both (`Patterns.LOOK_BACK_DAYS`): runs, best streaks and average gaps.
13. **A seed for trying it.** `SEED_METRICS_HISTORY` in the new `db/MetricsSample.kt` adds five weeks of history on top of SampleData's missions, and runs only with `SEED_SAMPLE_MISSIONS`. Both are off. The seed gives:
    - two balanced weeks;
    - a skipped block, a moved call and a skipped call, as white slices;
    - Monday calls. Since the review fixes they don't make a weekday line: Call Mom has a set day, so the pattern leaves it out;
    - a 2-week rhythm kept on time.
    The two pace offers in Plan 2's seed checklist (D) were checked to stay the same with both flags on.

## Keeping it fast

Months of tasks go through the builder on every change, so:
- **Off the main thread.** The state is built on `Dispatchers.Default` (`flowOn`), and only while the tab is open.
- **No date parsing per day.** `DayTasks` groups tasks by their stored `yyyy-MM-dd` text without reading any date. The calendar's 42 days and the pattern's 8 weeks are looked up by key, so nothing is O(days × tasks).
- **Each task's date is read once** for the mission lines, per mission.
- **`Points` runs per mission per week,** never per day. This week's points are added up from that week's tasks only.
- **Away weeks once.** The weeks with items of their own are worked out once per build (`PaceOffers.openedWeeks`) and handed to every section.

## File map

**Created**

| File | Responsibility |
|---|---|
| `screen/metrics/MetricsViewModel.kt` | Inputs, the task window, the week on screen |
| `screen/metrics/MetricsState.kt` | `MetricsInput`, `MetricsUiState` and the builder that reads everything once |
| `screen/metrics/MetricsScreen.kt` | The tab: header and arrows, the sections in order, ruled lines |
| `domain/DayTasks.kt` | Tasks by day, without reading dates |
| `domain/WeekScores.kt` | `PillarScore`, `WeekScore`, stored or live scores, the balanced rule, away weeks |
| `screen/metrics/PillarRings.kt` | The four rings |
| `domain/WeekHighlights.kt` | The highlights card's lines |
| `screen/metrics/WeekHighlightsCard.kt` | The card |
| `domain/WeekAhead.kt` | What next week adjusts |
| `screen/metrics/WeekAheadNotes.kt` | Its lines with chips: the wand on adjustments, a quiet dot otherwise |
| `domain/Patterns.kt` | The pattern lines |
| `domain/MonthGrid.kt` | The month's rows, day slices, white, crowns, descriptions |
| `screen/metrics/MonthCalendar.kt` | The calendar card |
| `domain/MissionProgress.kt` | One line per mission |
| `screen/metrics/MissionProgressRow.kt` | A mission row |
| `db/MetricsSample.kt` | The optional five-week seed |

**Edited:** `di/DI.kt`, `screen/NavScreens.kt`, `screen/init/InitViewModel.kt`, `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`.

**Edited by the review fixes after Task 8:** `domain/PaceOffers.kt`, `domain/WeekSummaries.kt`, `domain/MissionWords.kt`, `screen/metrics/CalDiaryViewModel.kt`, `screen/NavScreens.kt`, `screen/basicutils/AppColors.kt`, `screen/diary/WeekState.kt`, `screen/diary/plan/WeekNotes.kt`, `screen/diary/plan/PlanNotes.kt`.

**Deleted:** `screen/placeholder/ComingNextScreen.kt` (only the Metrics tab used it).

**Unchanged on purpose:**
- Every Plan 2 file in Tasks 1 to 9. See "What Plan 4 takes from Plan 2". The review fixes edit a few, keeping the diary as it was apart from M17 and M18.
- `domain/Points.kt`.
- The database: no new tables, columns or queries.

---

### Task 1: The Metrics tab with week navigation

**Files:**
- Create: `screen/metrics/MetricsScreen.kt`, `screen/metrics/MetricsState.kt`, `screen/metrics/MetricsViewModel.kt`
- Modify: `di/DI.kt`, `screen/NavScreens.kt`
- Delete: `screen/placeholder/ComingNextScreen.kt`

**Depends on:** Plan 2 (`WeekRepository` and its `single` in `di/DI.kt`).

Replaces the placeholder with the real tab. It shows only its header for now: the week's title and dates, arrows back and forward, and "Back to this week". The view model is written whole here, with every input the later tasks read. The state grows a field per task.

`MetricsViewModel` follows the other tabs:
- it's a Koin `factory`;
- `NavScreens` creates it with `remember` and cancels its scope on dispose;
- its "today" is fixed for its life.

The task window uses `flatMapLatest`, which needs `@OptIn(ExperimentalCoroutinesApi::class)`.

- [ ] **Step 1: Create `screen/metrics/MetricsScreen.kt`**

```kotlin
package com.example.todoapp.screen.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.diary.plan.SmallChip

/** Spec 9: the Metrics tab, one week at a time. */
@Composable
fun MetricsScreen(viewModel: MetricsViewModel) {
    val state by viewModel.state.collectAsState()
    val s = state

    Box(modifier = Modifier.fillMaxSize().background(Light_Yellowww)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (s != null) {
                item(key = "header") {
                    WeekHeader(s, onPrevious = viewModel::previousWeek, onNext = viewModel::nextWeek, onThisWeek = viewModel::backToThisWeek)
                }
            }
            item(key = "bottom-space") { Spacer(Modifier.height(120.dp)) }
        }
    }
}

/** "This week" and its dates, arrows a week back and forward, and "Back to this week" when away from it. */
@Composable
private fun WeekHeader(state: MetricsUiState, onPrevious: () -> Unit, onNext: () -> Unit, onThisWeek: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 30.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.title, fontSize = 30.sp, fontStyle = FontStyle.Italic, letterSpacing = 0.5.sp, color = InkGray,
                    modifier = Modifier.semantics { heading() }
                )
                Text(state.range, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, modifier = Modifier.padding(top = 4.dp))
            }
            WeekArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous week", enabled = state.canGoBack, onClick = onPrevious)
            WeekArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next week", enabled = state.canGoForward, onClick = onNext)
        }
        if (!state.isThisWeek) SmallChip("Back to this week", onClick = onThisWeek)
    }
}

/** A 44dp arrow button; faded and inactive at either end. */
@Composable
private fun WeekArrow(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) InkGray else InkGray.copy(alpha = 0.3f))
    }
}
```


- [ ] **Step 2: Create `screen/metrics/MetricsState.kt`**

```kotlin
package com.example.todoapp.screen.metrics

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.shortLabel
import com.example.todoapp.domain.weekStartDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Everything the Metrics tab reads. */
data class MetricsInput(
    val today: LocalDate,
    /** Monday of the week on screen. */
    val week: LocalDate,
    /** Active and archived missions. Archived ones still count for the points they earned. */
    val allMissions: List<Mission>,
    /** Tasks in MetricsViewModel's window: a year back from today, the shown week's month and its history, and next week. */
    val tasks: List<TodayTask>,
    /** Every finished week, oldest first. */
    val summaries: List<WeekSummary>,
    /** Milestones of active missions. */
    val milestones: List<Milestone>,
)

data class MetricsUiState(
    val weekStart: LocalDate,
    /** "This week", "Last week" or "Week of Oct 12". */
    val title: String,
    /** "Oct 26 to Nov 1" */
    val range: String,
    val isThisWeek: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
)

object MetricsStateBuilder {

    fun build(input: MetricsInput): MetricsUiState {
        val week = input.week
        val thisWeek = WeekMath.weekStart(input.today)
        // Stored weeks start at the week of the first task, so nothing comes before the first one.
        val earliest = input.summaries.firstOrNull()?.weekStartDate?.let { minOf(it, thisWeek) } ?: thisWeek
        return MetricsUiState(
            weekStart = week,
            title = title(week, thisWeek),
            range = "${week.shortLabel()} to ${week.plus(6, DateTimeUnit.DAY).shortLabel()}",
            isThisWeek = week == thisWeek,
            canGoBack = week > earliest,
            canGoForward = week < thisWeek,
        )
    }

    private fun title(week: LocalDate, thisWeek: LocalDate): String = when (week) {
        thisWeek -> "This week"
        thisWeek.minus(7, DateTimeUnit.DAY) -> "Last week"
        else -> "Week of ${week.shortLabel()}"
    }
}
```


- [ ] **Step 3: Create `screen/metrics/MetricsViewModel.kt`**

The window starts a year back. It moves earlier only when an older week's month needs it: the calendar's first row, plus `PlanRepository.HISTORY_DAYS` for due dates.

```kotlin
package com.example.todoapp.screen.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayTaskRepository
import com.example.todoapp.repo.WeekRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * The Metrics tab (spec 9). It only reads, so nothing here needs PlanRepository's write lock.
 * The tab makes a new one each time it opens and cancels it when it closes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MetricsViewModel(
    missionRepository: MissionRepository,
    todayTaskRepository: TodayTaskRepository,
    milestoneRepository: MilestoneRepository,
    weekRepository: WeekRepository,
) : ViewModel() {

    /** Fixed for this view model's life, as on the Missions tab. */
    private val today: LocalDate = MyDate.now().toLocalDate()
    private val thisWeek: LocalDate = WeekMath.weekStart(today)

    /** Monday of the week on screen. */
    private val week = MutableStateFlow(thisWeek)

    /**
     * Tasks from a year before today (or earlier, when an older week's month needs it) to the end of
     * next week. The window only moves when a week that old is shown.
     */
    private val tasks = week
        .map { firstDay(it) }
        .distinctUntilChanged()
        .flatMapLatest { from -> todayTaskRepository.observeBetween(from, thisWeek.plus(13, DateTimeUnit.DAY)) }

    /** Built off the main thread: months of tasks go through it on every change. */
    val state: StateFlow<MetricsUiState?> = combine(
        week,
        missionRepository.observeAll(),
        tasks,
        weekRepository.observeSummaries(),
        milestoneRepository.observeAll(),
    ) { week, missions, tasks, summaries, milestones ->
        MetricsStateBuilder.build(MetricsInput(today, week, missions, tasks, summaries, milestones))
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Steps from the week on screen, so two quick taps never go past the first stored week. */
    fun previousWeek() {
        val s = state.value ?: return
        if (s.canGoBack) week.value = s.weekStart.minus(7, DateTimeUnit.DAY)
    }

    fun nextWeek() {
        val s = state.value ?: return
        if (s.canGoForward) week.value = s.weekStart.plus(7, DateTimeUnit.DAY)
    }

    fun backToThisWeek() {
        week.value = thisWeek
    }

    /** A year back, or the history the week's month grid needs for due dates, whichever is earlier. */
    private fun firstDay(week: LocalDate): LocalDate {
        val gridStart = WeekMath.weekStart(LocalDate(week.year, week.month, 1))
        return minOf(today.minus(LOOK_BACK_DAYS, DateTimeUnit.DAY), gridStart.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY))
    }

    companion object {
        /** How far back patterns and per-mission lines look (best streaks, average gaps). */
        const val LOOK_BACK_DAYS = 365
    }
}
```


- [ ] **Step 4: `di/DI.kt`: import the metrics view model**

Replace:

```kotlin
import com.example.todoapp.screen.metrics.CalDiaryViewModel
```

with:

```kotlin
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.metrics.MetricsViewModel
```


- [ ] **Step 5: `di/DI.kt`: a fresh metrics view model each time the tab opens**

Replace:

```kotlin
        factory { (missionId: Long) -> MissionEditorViewModel(get(), get(), get(), get(), missionId) }
```

with:

```kotlin
        factory { (missionId: Long) -> MissionEditorViewModel(get(), get(), get(), get(), missionId) }
        factory { MetricsViewModel(get(), get(), get(), get()) }
```


- [ ] **Step 6: `screen/NavScreens.kt`: imports for the Metrics tab**

Replace:

```kotlin
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.missions.MissionsScreen
import com.example.todoapp.screen.missions.MissionsViewModel
import com.example.todoapp.screen.missions.editor.MissionEditorScreen
import com.example.todoapp.screen.missions.editor.MissionEditorViewModel
import com.example.todoapp.screen.placeholder.ComingNextScreen
```

with:

```kotlin
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.metrics.MetricsScreen
import com.example.todoapp.screen.metrics.MetricsViewModel
import com.example.todoapp.screen.missions.MissionsScreen
import com.example.todoapp.screen.missions.MissionsViewModel
import com.example.todoapp.screen.missions.editor.MissionEditorScreen
import com.example.todoapp.screen.missions.editor.MissionEditorViewModel
```


- [ ] **Step 7: `screen/NavScreens.kt`: the Metrics tab replaces the placeholder**

Replace:

```kotlin
                scene(route = MyScreen.Calendar.name) {
                    ComingNextScreen("Metrics come back after missions, with pillar rings and a week calendar.")
                }
```

with:

```kotlin
                scene(route = MyScreen.Calendar.name) {
                    val metricsViewModel = remember { KoinF.di!!.get<MetricsViewModel>() }
                    // Nothing owns this view model either, so stop its work when the tab leaves the screen.
                    DisposableEffect(metricsViewModel) {
                        onDispose { metricsViewModel.viewModelScope.cancel() }
                    }
                    MetricsScreen(metricsViewModel)
                }
```


- [ ] **Step 8: Delete `screen/placeholder/ComingNextScreen.kt`**

Nothing uses it once the Metrics tab is in. From the repo root:

```bash
git rm shared/src/commonMain/kotlin/com/example/todoapp/screen/placeholder/ComingNextScreen.kt
```


- [ ] **Step 9: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a changed file (builds have rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/screen/metrics/MetricsScreen.kt \
  $K/screen/metrics/MetricsState.kt \
  $K/screen/metrics/MetricsViewModel.kt \
  $K/di/DI.kt \
  $K/screen/NavScreens.kt
git commit -m "feat: the Metrics tab with week navigation"
```


---

### Task 2: Pillar rings

**Files:**
- Create: `domain/DayTasks.kt`, `domain/WeekScores.kt`, `screen/metrics/PillarRings.kt`
- Modify: `screen/metrics/MetricsState.kt`, `screen/metrics/MetricsScreen.kt`

**Depends on:** Task 1.

Four rings for the week on screen (decision 4). A finished week reads its stored summary. This week is worked out live, as the diary's bars are. A finished week without a single task is **away** (decision 5): its rings read "away", not "light".

`DayTasks` is the index every later section uses. `WeekScores.balanced` mirrors `WeekSummary.balanced`'s rule (every pillar full, and something due), for the week that has no row yet. The rings are drawn on a `Canvas`, each with one content description, for example "Health, 13 of 14 points, full".

- [ ] **Step 1: Create `domain/DayTasks.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Tasks grouped by day, keyed by their stored yyyy-MM-dd text. The Metrics tab looks days up here,
 * so a month of day cells never reads every task's date again.
 */
class DayTasks(tasks: List<TodayTask>) {

    private val byDay: Map<String, List<TodayTask>> = tasks.groupBy { it.taskDate.dateString }

    fun on(day: LocalDate): List<TodayTask> = byDay[MyDate.fromLocalDate(day).dateString].orEmpty()

    /** Tasks from [from] to [to], both included, in date order. */
    fun between(from: LocalDate, to: LocalDate): List<TodayTask> {
        val out = mutableListOf<TodayTask>()
        var day = from
        while (day <= to) {
            out += on(day)
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return out
    }
}
```


- [ ] **Step 2: Create `domain/WeekScores.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.week.WeekSummary
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** One pillar's points against its target for one week (spec 5). */
data class PillarScore(val pillar: PillarType, val earned: Int, val target: Int) {
    /** 80% of the target or more; nothing due counts as full. */
    val full: Boolean get() = Points.isFull(earned, target)
    /** Points past the target, shown as "+N extra" on this pillar only. */
    val extra: Int get() = (earned - target).coerceAtLeast(0)
    /** How much of the ring is drawn. A pillar with nothing due counts as full, so its ring is drawn full too. */
    val share: Float get() = if (target == 0) 1f else (earned.toFloat() / target).coerceIn(0f, 1f)
}

/** A week's pillar scores, as the Metrics tab shows them. */
data class WeekScore(
    val pillars: List<PillarScore>,
    /** The crown: stored as WeekSummary.balanced for a finished week, worked out the same way for this one. */
    val balanced: Boolean,
    /** The week is over. */
    val finished: Boolean,
    /**
     * A finished week without a single task: the diary wasn't opened that week. Its stored summary has
     * 0 points against targets, which isn't a miss, so it never counts as a light week.
     */
    val away: Boolean,
)

object WeekScores {

    /**
     * A finished week as stored at close-out, or a week without a row worked out live as the diary's
     * bars are: points from [allMissions] (archived too), targets from the active [missions]. Only this
     * week lacks a row normally; a past week can for a moment, before the diary's close-out runs.
     */
    fun forWeek(
        weekStart: LocalDate,
        thisWeek: LocalDate,
        summary: WeekSummary?,
        missions: List<Mission>,
        allMissions: List<Mission>,
        days: DayTasks,
        history: History,
    ): WeekScore {
        val finished = weekStart < thisWeek
        val away = finished && isAway(weekStart, days)
        if (summary != null) return WeekScore(stored(summary), summary.balanced, finished, away)
        val weekTasks = days.between(weekStart, weekStart.plus(6, DateTimeUnit.DAY))
        val pillars = PillarType.scoredPillars.map { pillar ->
            PillarScore(
                pillar = pillar,
                earned = Points.earnedInWeek(allMissions, weekTasks, pillar, weekStart),
                target = Points.weeklyTarget(missions, pillar, weekStart, history),
            )
        }
        return WeekScore(pillars, balanced(pillars), finished, away)
    }

    fun stored(summary: WeekSummary): List<PillarScore> =
        PillarType.scoredPillars.map { PillarScore(it, summary.earned(it), summary.target(it)) }

    /** WeekSummary.balanced's rule, for a week with no row yet: every pillar full (spec 5), and something was due. */
    fun balanced(pillars: List<PillarScore>): Boolean = pillars.all { it.full } && pillars.any { it.target > 0 }

    /** No task at all in the week starting [weekStart]: the diary wasn't opened in it. */
    fun isAway(weekStart: LocalDate, days: DayTasks): Boolean =
        days.between(weekStart, weekStart.plus(6, DateTimeUnit.DAY)).isEmpty()
}
```


- [ ] **Step 3: Create `screen/metrics/PillarRings.kt`**

```kotlin
package com.example.todoapp.screen.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.PillarScore
import com.example.todoapp.domain.WeekScore
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarStrong

private val RingText = Color(0xFF3A3A3A)

/**
 * Spec 9: the week's four pillar rings, each with its points of target and "full" or "light" at the
 * 80% rule. A week the diary wasn't opened in reads "away" instead, so its zeros don't look like misses.
 */
@Composable
fun PillarRings(week: WeekScore) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        week.pillars.forEach { score -> PillarRing(score, away = week.away, Modifier.weight(1f)) }
    }
}

@Composable
private fun PillarRing(score: PillarScore, away: Boolean, modifier: Modifier) {
    val state = when {
        away -> "away"
        score.full -> "full"
        else -> "light"
    }
    val extra = if (score.extra > 0) ", +${score.extra} extra" else ""
    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "${score.pillar.title}, ${score.earned} of ${score.target} points, $state$extra"
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(72.dp)) {
                val stroke = 9.dp.toPx()
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(Color.White, 0f, 360f, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke))
                if (score.share > 0f) {
                    drawArc(
                        pillarStrong(score.pillar), -90f, 360f * score.share, useCenter = false,
                        topLeft = topLeft, size = arc, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Text("${score.earned}/${score.target}", fontSize = 14.sp, color = RingText)
        }
        Text(score.pillar.title, fontSize = 13.sp, color = RingText, modifier = Modifier.padding(top = 4.dp))
        Text(state, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = if (score.full && !away) pillarInk(score.pillar) else InkGray)
        if (score.extra > 0) Text("+${score.extra} extra", fontSize = 11.sp, color = pillarInk(score.pillar))
    }
}
```


- [ ] **Step 4: `screen/metrics/MetricsState.kt`: imports for the rings**

Replace:

```kotlin
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.shortLabel
```

with:

```kotlin
import com.example.todoapp.db.data.week.WeekSummary
import com.example.todoapp.domain.DayTasks
import com.example.todoapp.domain.History
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.WeekScore
import com.example.todoapp.domain.WeekScores
import com.example.todoapp.domain.shortLabel
```


- [ ] **Step 5: `screen/metrics/MetricsState.kt`: the week's pillar scores in the state**

Replace:

```kotlin
    val canGoBack: Boolean,
    val canGoForward: Boolean,
)
```

with:

```kotlin
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    /** The week's pillar scores: stored for a finished week, live for this one. */
    val score: WeekScore,
)
```


- [ ] **Step 6: `screen/metrics/MetricsState.kt`: read the inputs once, then score the week**

Replace:

```kotlin
        val week = input.week
        val thisWeek = WeekMath.weekStart(input.today)
        // Stored weeks start at the week of the first task, so nothing comes before the first one.
        val earliest = input.summaries.firstOrNull()?.weekStartDate?.let { minOf(it, thisWeek) } ?: thisWeek
```

with:

```kotlin
        val week = input.week
        val thisWeek = WeekMath.weekStart(input.today)
        // Read once here; every section below looks days and weeks up instead of re-reading dates.
        val missions = input.allMissions.filter { it.active }
        val days = DayTasks(input.tasks)
        val history = History(input.tasks)
        val byWeek = input.summaries.associateBy { it.weekStartDate }
        // Stored weeks start at the week of the first task, so nothing comes before the first one.
        val earliest = input.summaries.firstOrNull()?.weekStartDate?.let { minOf(it, thisWeek) } ?: thisWeek
        val score = WeekScores.forWeek(week, thisWeek, byWeek[week], missions, input.allMissions, days, history)
```


- [ ] **Step 7: `screen/metrics/MetricsState.kt`: pass the scores on**

Replace:

```kotlin
            canGoBack = week > earliest,
            canGoForward = week < thisWeek,
        )
```

with:

```kotlin
            canGoBack = week > earliest,
            canGoForward = week < thisWeek,
            score = score,
        )
```


- [ ] **Step 8: `screen/metrics/MetricsScreen.kt`: the rings under the header**

Replace:

```kotlin
                    WeekHeader(s, onPrevious = viewModel::previousWeek, onNext = viewModel::nextWeek, onThisWeek = viewModel::backToThisWeek)
                }
```

with:

```kotlin
                    WeekHeader(s, onPrevious = viewModel::previousWeek, onNext = viewModel::nextWeek, onThisWeek = viewModel::backToThisWeek)
                }
                item(key = "rings") { PillarRings(s.score) }
```


- [ ] **Step 9: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a changed file (builds have rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/domain/DayTasks.kt \
  $K/domain/WeekScores.kt \
  $K/screen/metrics/PillarRings.kt \
  $K/screen/metrics/MetricsState.kt \
  $K/screen/metrics/MetricsScreen.kt
git commit -m "feat: pillar rings on the Metrics tab"
```


---

### Task 3: The week highlights card

**Files:**
- Create: `domain/WeekHighlights.kt`, `screen/metrics/WeekHighlightsCard.kt`
- Modify: `screen/metrics/MetricsState.kt`, `screen/metrics/MetricsScreen.kt`

**Depends on:** Task 2 (`WeekScore`, `WeekScores.isAway`, `DayTasks`).

The diary's highlights style, for the week on screen (decision 8). It has three lines: points, pillars full, and balanced weeks out of the last 4. The card keeps its own one-line row composable. It doesn't use Plan 2's `internal` `HighlightRow`, so the diary's file stays untouched.

- [ ] **Step 1: Create `domain/WeekHighlights.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.week.WeekSummary
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** The week highlights card on the Metrics tab (spec 9), in the diary's highlights style. */
data class WeekHighlights(
    /** "THIS WEEK", "LAST WEEK" or "WEEK OF OCT 12" */
    val header: String,
    /** "54 of 62 points", with " · +4 extra" when a pillar went past its target. */
    val points: String,
    /** "3 of 4 pillars full", "All 4 pillars full so far", or "All 4 pillars full · a balanced week" once it's over. */
    val pillars: String,
    /** Shows the crown on the pillars line. */
    val balanced: Boolean,
    /** "2 balanced weeks out of the last 4" */
    val balancedWeeks: String,
)

object WeekHighlightsBuilder {

    /** The crown line looks at this many finished weeks: up to the week on screen, or before this week. */
    const val LOOK_BACK_WEEKS = 4

    /** [byWeek] holds the finished weeks by their Monday; [week] is the one on screen. */
    fun build(
        header: String,
        week: LocalDate,
        thisWeek: LocalDate,
        score: WeekScore,
        byWeek: Map<LocalDate, WeekSummary>,
        days: DayTasks,
    ): WeekHighlights {
        val pillarCount = score.pillars.size
        // Finished weeks only, since this week can still change, so this week looks at the 4 before it.
        // Away weeks aren't counted either way.
        val last = if (week < thisWeek) week else week.minus(7, DateTimeUnit.DAY)
        val counted = (0 until LOOK_BACK_WEEKS)
            .map { last.minus(7 * it, DateTimeUnit.DAY) }
            .filter { !WeekScores.isAway(it, days) }
            .mapNotNull { byWeek[it] }
        return WeekHighlights(
            header = header,
            points = points(score.pillars),
            pillars = when {
                score.away -> "The diary wasn't opened that week."
                !score.balanced -> "${score.pillars.count { it.full }} of $pillarCount pillars full"
                score.finished -> "All $pillarCount pillars full · a balanced week"
                else -> "All $pillarCount pillars full so far"
            },
            balanced = score.balanced && !score.away,
            balancedWeeks = balancedWeeks(counted.count { it.balanced }, counted.size),
        )
    }

    /** Each pillar counts up to its target; anything past it is extra, which never fills another pillar (spec 5). */
    fun points(scores: List<PillarScore>): String {
        val target = scores.sumOf { it.target }
        val earned = scores.sumOf { it.earned }
        if (target == 0) return MissionWords.pointsPhrase(earned)
        val counted = scores.sumOf { minOf(it.earned, it.target) }
        val extra = earned - counted
        return "$counted of ${MissionWords.pointsPhrase(target)}" + if (extra > 0) " · +$extra extra" else ""
    }

    private fun balancedWeeks(balanced: Int, weeks: Int): String = when {
        weeks == 0 -> "No weeks to count yet."
        weeks == 1 -> if (balanced == 1) "1 balanced week so far" else "No balanced week yet"
        else -> "$balanced balanced ${if (balanced == 1) "week" else "weeks"} out of the last $weeks"
    }
}
```


- [ ] **Step 2: Create `screen/metrics/WeekHighlightsCard.kt`**

```kotlin
package com.example.todoapp.screen.metrics

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.WeekHighlights
import com.example.todoapp.screen.basicutils.CardCream
import com.example.todoapp.screen.basicutils.InkBlack
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.crown_a
import todoapp.shared.generated.resources.heart_filled
import todoapp.shared.generated.resources.metrics_calendar
import todoapp.shared.generated.resources.star_filled

/** The week's highlights, in the style of the diary's evening card. Never interprets titles. */
@Composable
fun WeekHighlightsCard(state: WeekHighlights) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(CardCream, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.header,
                fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp, color = Color(0xFF3A3A3A),
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Image(
                painterResource(Res.drawable.metrics_calendar), contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFF6B6B6B)), modifier = Modifier.size(30.dp)
            )
        }
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(Color(0xFFDDD6C2)))
        HighlightLine(Res.drawable.star_filled, state.points)
        HighlightLine(if (state.balanced) Res.drawable.crown_a else Res.drawable.heart_filled, state.pillars)
        HighlightLine(Res.drawable.crown_a, state.balancedWeeks)
    }
}

/** One line of the card: an icon, then its text, as on the diary's highlights. */
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


- [ ] **Step 3: `screen/metrics/MetricsState.kt`: import the highlights**

Replace:

```kotlin
import com.example.todoapp.domain.History
import com.example.todoapp.domain.WeekMath
```

with:

```kotlin
import com.example.todoapp.domain.History
import com.example.todoapp.domain.WeekHighlights
import com.example.todoapp.domain.WeekHighlightsBuilder
import com.example.todoapp.domain.WeekMath
```


- [ ] **Step 4: `screen/metrics/MetricsState.kt`: the highlights card in the state**

Replace:

```kotlin
    /** The week's pillar scores: stored for a finished week, live for this one. */
    val score: WeekScore,
)
```

with:

```kotlin
    /** The week's pillar scores: stored for a finished week, live for this one. */
    val score: WeekScore,
    val highlights: WeekHighlights,
)
```


- [ ] **Step 5: `screen/metrics/MetricsState.kt`: build the highlights from the same scores**

Replace:

```kotlin
        val score = WeekScores.forWeek(week, thisWeek, byWeek[week], missions, input.allMissions, days, history)
        return MetricsUiState(
            weekStart = week,
            title = title(week, thisWeek),
```

with:

```kotlin
        val score = WeekScores.forWeek(week, thisWeek, byWeek[week], missions, input.allMissions, days, history)
        val title = title(week, thisWeek)
        return MetricsUiState(
            weekStart = week,
            title = title,
```


- [ ] **Step 6: `screen/metrics/MetricsState.kt`: pass the highlights on**

Replace:

```kotlin
            score = score,
        )
```

with:

```kotlin
            score = score,
            highlights = WeekHighlightsBuilder.build(title.uppercase(), week, thisWeek, score, byWeek, days),
        )
```


- [ ] **Step 7: `screen/metrics/MetricsScreen.kt`: the highlights card under the rings**

Replace:

```kotlin
                item(key = "rings") { PillarRings(s.score) }
```

with:

```kotlin
                item(key = "rings") { PillarRings(s.score) }
                item(key = "highlights") { WeekHighlightsCard(s.highlights) }
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
git checkout -- shared/schemas            # only if the line above listed a changed file (builds have rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/domain/WeekHighlights.kt \
  $K/screen/metrics/WeekHighlightsCard.kt \
  $K/screen/metrics/MetricsState.kt \
  $K/screen/metrics/MetricsScreen.kt
git commit -m "feat: week highlights card on the Metrics tab"
```


---

### Task 4: What next week adjusts

**Files:**
- Create: `domain/WeekAhead.kt`, `screen/metrics/WeekAheadNotes.kt`
- Modify: `screen/metrics/MetricsState.kt`, `screen/metrics/MetricsScreen.kt`

**Depends on:** Task 2 (`DayTasks`); Plan 2 (`LinePart`, `ChipText`).

Wand lines between the rings and the card, as in the mockup (decision 7). They're built only from stored data:
- tasks moved into next week;
- rhythms still due;
- for this week, longer rhythms coming due and pauses ending.

Titles sit on chips in their pillar's light color, and the words never depend on the title (spec 7.1). The heading reads "NEXT WEEK", or "THE WEEK AFTER" for a finished week.

- [ ] **Step 1: Create `domain/WeekAhead.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * "What next week adjusts" on the Metrics tab (spec 9), from stored data only. Each line names its
 * mission on a chip (spec 7.1), and the words around the chip never depend on the title. Nothing here
 * guesses why a week went the way it did: there's no "Love was light, so…".
 */
object WeekAhead {

    const val MAX_LINES = 4

    /**
     * Lines for the week starting [week]. A finished week lists what was carried into the week after
     * it. This week also lists longer rhythms coming due and pauses ending next week. [missions] are the
     * active ones, so an archived mission never shows here.
     */
    fun lines(week: LocalDate, thisWeek: LocalDate, missions: List<Mission>, days: DayTasks, history: History): List<List<LinePart>> {
        val weekEnd = week.plus(6, DateTimeUnit.DAY)
        val next = week.plus(7, DateTimeUnit.DAY)
        val nextEnd = next.plus(6, DateTimeUnit.DAY)
        val finished = week < thisWeek
        val byId = missions.associateBy { it.missionId }
        val named = mutableSetOf<Long>()
        val lines = mutableListOf<List<LinePart>>()
        fun add(mission: Mission, words: String) {
            if (named.add(mission.missionId)) lines += listOf(LinePart(mission.title, mission.pillarType), LinePart(words))
        }
        // "Move it" items already carried from this week into the next (spec 6.4).
        days.between(next, nextEnd)
            .filter { task -> task.movedFromDate?.toLocalDate()?.let { it >= week && it <= weekEnd } == true }
            .forEach { task -> byId[task.missionId]?.let { add(it, " moved to ${task.date.weekdayName()}.") } }
        // Every-so-often missions stay due until they're done (spec 5.1). Shorter rhythms are due every week anyway.
        val rhythms = missions.filter { it.repeatsByInterval && it.intervalDays() >= Period.WEEK.days && it.isLiveOn(next) }
        rhythms
            .filter { Points.occurrencesInWeek(it, week, history) > 0 && history.doneCount(it.missionId, week, weekEnd) == 0 }
            .forEach {
                add(it, if (finished) " stayed due, so it moved into the next week." else " is still due. If it waits, it moves into next week.")
            }
        if (!finished) {
            // A weekly rhythm comes due every week, so only longer ones are worth a line.
            rhythms
                .filter { it.intervalDays() > Period.WEEK.days && Points.dueDate(it, history, next) in next..nextEnd }
                .forEach { add(it, " comes due next week.") }
            missions
                .filter { mission -> mission.pausedUntil?.toLocalDate()?.let { it in next..nextEnd } == true }
                .forEach { add(it, " is back next week.") }
        }
        return lines.take(MAX_LINES).ifEmpty { listOf(listOf(LinePart("Nothing to catch up on."))) }
    }
}
```


- [ ] **Step 2: Create `screen/metrics/WeekAheadNotes.kt`**

```kotlin
package com.example.todoapp.screen.metrics

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.LinePart
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.diary.plan.ChipText
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.wand

/** What next week adjusts: one wand line each, titles on pillar chips (spec 7.1). */
@Composable
fun WeekAheadNotes(title: String, lines: List<List<LinePart>>) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp)) {
        Text(title, fontSize = 11.sp, letterSpacing = 1.2.sp, color = InkGray, modifier = Modifier.semantics { heading() })
        lines.forEach { line ->
            Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                // The wand always means "the app adjusted something" (spec 9.1).
                Image(
                    painterResource(Res.drawable.wand), contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(0xFF6B6B6B)), modifier = Modifier.padding(top = 3.dp).size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                ChipText(line, fontSize = 15.sp, color = Color(0xFF2A2A2A), italic = true)
            }
        }
    }
}
```


- [ ] **Step 3: `screen/metrics/MetricsState.kt`: imports for next week**

Replace:

```kotlin
import com.example.todoapp.domain.History
import com.example.todoapp.domain.WeekHighlights
```

with:

```kotlin
import com.example.todoapp.domain.History
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.WeekAhead
import com.example.todoapp.domain.WeekHighlights
```


- [ ] **Step 4: `screen/metrics/MetricsState.kt`: next week's lines in the state**

Replace:

```kotlin
    val score: WeekScore,
    val highlights: WeekHighlights,
)
```

with:

```kotlin
    val score: WeekScore,
    val highlights: WeekHighlights,
    /** "NEXT WEEK", or "THE WEEK AFTER" for a finished week. */
    val aheadTitle: String,
    /** What next week adjusts, each line naming its mission on a chip. Never empty. */
    val ahead: List<List<LinePart>>,
)
```


- [ ] **Step 5: `screen/metrics/MetricsState.kt`: pass next week's lines on**

Replace:

```kotlin
            highlights = WeekHighlightsBuilder.build(title.uppercase(), week, thisWeek, score, byWeek, days),
        )
```

with:

```kotlin
            highlights = WeekHighlightsBuilder.build(title.uppercase(), week, thisWeek, score, byWeek, days),
            aheadTitle = if (week < thisWeek) "THE WEEK AFTER" else "NEXT WEEK",
            ahead = WeekAhead.lines(week, thisWeek, missions, days, history),
        )
```


- [ ] **Step 6: `screen/metrics/MetricsScreen.kt`: next week between the rings and the card, as in the mockup**

Replace:

```kotlin
                item(key = "rings") { PillarRings(s.score) }
```

with:

```kotlin
                item(key = "rings") { PillarRings(s.score) }
                item(key = "week-ahead") { WeekAheadNotes(s.aheadTitle, s.ahead) }
```


- [ ] **Step 7: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a changed file (builds have rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/domain/WeekAhead.kt \
  $K/screen/metrics/WeekAheadNotes.kt \
  $K/screen/metrics/MetricsState.kt \
  $K/screen/metrics/MetricsScreen.kt
git commit -m "feat: what next week adjusts, on the Metrics tab"
```


---

### Task 5: Pattern lines

**Files:**
- Create: `domain/Patterns.kt`
- Modify: `screen/metrics/MetricsState.kt`, `screen/metrics/MetricsScreen.kt`

**Depends on:** Task 2 (`DayTasks`, `WeekScores.isAway`).

A few plain counts, each shown only when the data holds it up (decision 9). They come from stored weeks and ticks only, never from titles. They're as of today, whichever week is on screen. They're written on ruled lines under a yellow PATTERNS marker heading, in the diary's light italic.

- [ ] **Step 1: Create `domain/Patterns.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.week.WeekSummary
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Pattern lines on the Metrics tab (spec 9): a few plain counts over stored weeks and ticks. A line
 * shows only when the data holds it up, and nothing reads meaning into a title (spec 7.1).
 */
object Patterns {

    const val MAX_LINES = 3
    /** Full weeks in a row before a pillar gets a line… */
    const val FULL_RUN = 3
    /** …and at most this many pillars get one, so other kinds of line still fit. */
    const val MAX_RUN_LINES = 2
    /** Balanced weeks in a row before they get a line. */
    const val BALANCED_RUN = 2
    /** The weekday line looks at this many weeks, this one included… */
    const val WEEKDAY_WEEKS = 8
    /** …and needs this many ticks in the pillar. */
    const val WEEKDAY_MIN_DONE = 6
    /**
     * Runs look back at most this far. That stays inside MetricsViewModel's year of tasks, which is what
     * tells an away week (no tasks) from a week that simply wasn't loaded.
     */
    const val MAX_RUN_WEEKS = 51

    private val pluralDays = listOf("Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays", "Sundays")

    /**
     * As of [today], whatever week is on screen. [byWeek] holds the finished weeks by their Monday.
     * Ticks of archived missions count too: they happened.
     */
    fun lines(today: LocalDate, byWeek: Map<LocalDate, WeekSummary>, allMissions: List<Mission>, days: DayTasks): List<String> {
        val thisWeek = WeekMath.weekStart(today)
        val lines = mutableListOf<String>()
        val balancedRun = runBack(thisWeek, byWeek, days) { it.balanced }
        if (balancedRun >= BALANCED_RUN) lines += "$balancedRun balanced weeks in a row."
        // A pillar with nothing due isn't a full week here. A balanced run already covers a shorter pillar run.
        PillarType.scoredPillars
            .map { pillar ->
                pillar to runBack(thisWeek, byWeek, days) { it.target(pillar) > 0 && Points.isFull(it.earned(pillar), it.target(pillar)) }
            }
            .filter { (_, run) -> run >= FULL_RUN && run > balancedRun }
            .sortedByDescending { it.second }
            .take(MAX_RUN_LINES)
            .forEach { (pillar, run) -> lines += "${pillar.title} has been full $run weeks running." }
        lines += weekdayLines(today, thisWeek, allMissions, days)
        return lines.take(MAX_LINES)
    }

    /**
     * Finished weeks in a row, back from last week, that pass [test]. A week without a row ends the run.
     * An away week (WeekScores.isAway) neither counts nor ends it: its zeros aren't misses.
     */
    private fun runBack(thisWeek: LocalDate, byWeek: Map<LocalDate, WeekSummary>, days: DayTasks, test: (WeekSummary) -> Boolean): Int {
        var week = thisWeek.minus(7, DateTimeUnit.DAY)
        var run = 0
        repeat(MAX_RUN_WEEKS) {
            val summary = byWeek[week] ?: return run
            if (!WeekScores.isAway(week, days)) {
                if (!test(summary)) return run
                run++
            }
            week = week.minus(7, DateTimeUnit.DAY)
        }
        return run
    }

    /**
     * "Love mostly happens on Sundays": the pillar has enough ticks in the last [WEEKDAY_WEEKS] weeks,
     * and one weekday has at least 40% of them and more than any other day. Fixed blocks are left out,
     * since their days are set.
     */
    private fun weekdayLines(today: LocalDate, thisWeek: LocalDate, allMissions: List<Mission>, days: DayTasks): List<String> {
        val byId = allMissions.associateBy { it.missionId }
        val counts = mutableMapOf<PillarType, IntArray>()
        var day = thisWeek.minus(7 * (WEEKDAY_WEEKS - 1), DateTimeUnit.DAY)
        while (day <= today) {
            val weekday = day.dayOfWeek.isoDayNumber - 1
            days.on(day).forEach { task ->
                val mission = byId[task.missionId]
                if (mission != null && mission.scored && !task.isFixedBlock && task.taskState.countsAsDone) {
                    counts.getOrPut(mission.pillarType) { IntArray(7) }[weekday]++
                }
            }
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return PillarType.scoredPillars
            .mapNotNull { pillar ->
                val byDay = counts[pillar] ?: return@mapNotNull null
                val total = byDay.sum()
                val ranked = byDay.sortedArrayDescending()
                val top = ranked[0]
                if (total < WEEKDAY_MIN_DONE || top * 5 < total * 2 || top == ranked[1]) return@mapNotNull null
                Triple(pillar, byDay.indexOf(top), top.toFloat() / total)
            }
            .sortedByDescending { it.third }
            .map { (pillar, weekday, _) -> "${pillar.title} mostly happens on ${pluralDays[weekday]}." }
    }
}
```


- [ ] **Step 2: `screen/metrics/MetricsState.kt`: import the patterns**

Replace:

```kotlin
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.WeekAhead
```

with:

```kotlin
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.Patterns
import com.example.todoapp.domain.WeekAhead
```


- [ ] **Step 3: `screen/metrics/MetricsState.kt`: pattern lines in the state**

Replace:

```kotlin
    /** What next week adjusts, each line naming its mission on a chip. Never empty. */
    val ahead: List<List<LinePart>>,
)
```

with:

```kotlin
    /** What next week adjusts, each line naming its mission on a chip. Never empty. */
    val ahead: List<List<LinePart>>,
    /** As of today, whichever week is on screen. Empty until the data holds a pattern up. */
    val patterns: List<String>,
)
```


- [ ] **Step 4: `screen/metrics/MetricsState.kt`: pass the patterns on**

Replace:

```kotlin
            ahead = WeekAhead.lines(week, thisWeek, missions, days, history),
        )
```

with:

```kotlin
            ahead = WeekAhead.lines(week, thisWeek, missions, days, history),
            patterns = Patterns.lines(input.today, byWeek, input.allMissions, days),
        )
```


- [ ] **Step 5: `screen/metrics/MetricsScreen.kt`: imports for the patterns**

Replace:

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
```

with:

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
```


- [ ] **Step 6: `screen/metrics/MetricsScreen.kt`: more imports for the patterns**

Replace:

```kotlin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
```

with:

```kotlin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
```


- [ ] **Step 7: `screen/metrics/MetricsScreen.kt`: and the rest**

Replace:

```kotlin
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
```

with:

```kotlin
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.Yellow180
import com.example.todoapp.screen.basicutils.components.MarkerHeading
```


- [ ] **Step 8: `screen/metrics/MetricsScreen.kt`: the patterns under the highlights card**

Replace:

```kotlin
                item(key = "highlights") { WeekHighlightsCard(s.highlights) }
```

with:

```kotlin
                item(key = "highlights") { WeekHighlightsCard(s.highlights) }
                item(key = "patterns-heading") {
                    MarkerHeading("PATTERNS", Yellow180, Modifier.padding(top = 24.dp, bottom = 4.dp))
                }
                if (s.patterns.isEmpty()) {
                    item(key = "patterns-none") { RuledLine("Patterns show up after a few weeks.", color = InkGray) }
                }
                s.patterns.forEachIndexed { i, line -> item(key = "pattern-$i") { RuledLine(line) } }
```


- [ ] **Step 9: `screen/metrics/MetricsScreen.kt`: a line written on the diary's ruled paper**

Replace:

```kotlin
        Icon(icon, contentDescription = label, tint = if (enabled) InkGray else InkGray.copy(alpha = 0.3f))
    }
}
```

with:

```kotlin
        Icon(icon, contentDescription = label, tint = if (enabled) InkGray else InkGray.copy(alpha = 0.3f))
    }
}

/** One line in the diary's hand, on a ruled line (spec 9.1). */
@Composable
private fun RuledLine(text: String, color: Color = InkBlack) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).heightIn(min = 38.dp)
            .drawBehind {
                val y = size.height - 1.dp.toPx()
                drawLine(Blue80, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text, fontSize = 17.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light, letterSpacing = 0.3.sp,
            color = color, modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}
```


- [ ] **Step 10: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 11: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a changed file (builds have rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/domain/Patterns.kt \
  $K/screen/metrics/MetricsState.kt \
  $K/screen/metrics/MetricsScreen.kt
git commit -m "feat: pattern lines on the Metrics tab"
```


---

### Task 6: The Monday-first month calendar

**Files:**
- Create: `domain/MonthGrid.kt`, `screen/metrics/MonthCalendar.kt`
- Modify: `screen/metrics/MetricsState.kt`, `screen/metrics/MetricsScreen.kt`, `screen/NavScreens.kt`

**Depends on:** Tasks 1 and 2 (`MetricsScreen`, `DayTasks`).

The month of the shown week, Monday first (decision 10):
- Each day's ring is split by points per pillar, with white for what didn't happen.
- Each balanced finished week has a gold crown.
- The week on screen is tinted.

Tapping a day opens that date in the diary. `NavScreens` passes `openDay`, which calls the shared `CalDiaryViewModel.selectDate` and then switches to the Diary tab. The diary scene's `remember(currentDate)` then makes a view model for that date.

Every day comes from one `DayTasks` lookup, built off the main thread. Each cell is 44dp tall: with 8dp + 4dp side padding and a 20dp crown column, a 360dp-wide phone still gives each of the seven days about 45dp of width. The weekday letters are hidden from screen readers, since each day reads its own name.

- [ ] **Step 1: Create `domain/MonthGrid.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.week.WeekSummary
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** One pillar's points on one day. */
data class DaySlice(val pillar: PillarType, val points: Int)

/** One day of the month calendar. */
data class CalendarDay(
    val date: LocalDate,
    /** False for the days of the first and last rows that belong to the next or previous month. */
    val inMonth: Boolean,
    val isToday: Boolean,
    /** Later than today: no ring yet. */
    val isFuture: Boolean,
    /** Points earned per pillar that day, in pillar order, without empty ones. */
    val slices: List<DaySlice>,
    /**
     * Points that didn't happen: skipped or moved items, the missing half of a did-some, and anything still
     * planned on an earlier day. Today's planned items can still happen, so they aren't counted.
     */
    val notDone: Int,
    /** What a screen reader says for the day. */
    val description: String,
)

data class CalendarWeek(val weekStart: LocalDate, val days: List<CalendarDay>, val crowned: Boolean, val selected: Boolean)

data class MonthCalendar(
    /** "OCTOBER 2026" */
    val title: String,
    val weeks: List<CalendarWeek>,
)

/** Spec 9: the Monday-first month calendar on the Metrics tab. */
object MonthGrid {

    /**
     * The month of [week]'s Monday, as rows of whole weeks from Monday to Sunday. [allMissions] include
     * archived ones, so every tick keeps its pillar. Only finished weeks get a crown. Each task is read once,
     * through [days].
     */
    fun build(
        week: LocalDate,
        today: LocalDate,
        allMissions: List<Mission>,
        days: DayTasks,
        byWeek: Map<LocalDate, WeekSummary>,
    ): MonthCalendar {
        val first = LocalDate(week.year, week.month, 1)
        val last = first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        val thisWeek = WeekMath.weekStart(today)
        val byId = allMissions.associateBy { it.missionId }
        val weeks = mutableListOf<CalendarWeek>()
        var monday = WeekMath.weekStart(first)
        while (monday <= last) {
            val summary = byWeek[monday]
            weeks += CalendarWeek(
                weekStart = monday,
                days = (0..6).map { day(monday.plus(it, DateTimeUnit.DAY), first.month, today, byId, days) },
                crowned = monday < thisWeek && summary?.balanced == true,
                selected = monday == week,
            )
            monday = monday.plus(7, DateTimeUnit.DAY)
        }
        return MonthCalendar("${first.month.name} ${first.year}", weeks)
    }

    private fun day(date: LocalDate, month: Month, today: LocalDate, byId: Map<Long, Mission>, days: DayTasks): CalendarDay {
        val earned = mutableMapOf<PillarType, Int>()
        var notDone = 0
        if (date <= today) {
            for (task in days.on(date)) {
                val mission = byId[task.missionId] ?: continue
                // Recharge never scores, so it has no slice.
                if (!mission.scored) continue
                val pillar = mission.pillarType
                when (task.taskState) {
                    TaskState.DONE -> earned[pillar] = (earned[pillar] ?: 0) + task.pointsEarned
                    TaskState.PARTIAL -> {
                        earned[pillar] = (earned[pillar] ?: 0) + task.pointsEarned
                        notDone += (mission.weight - task.pointsEarned).coerceAtLeast(0)
                    }
                    TaskState.SKIPPED, TaskState.MOVED -> notDone += mission.weight
                    TaskState.PLANNED -> if (date < today) notDone += mission.weight
                }
            }
        }
        val slices = PillarType.scoredPillars.mapNotNull { pillar -> earned[pillar]?.takeIf { it > 0 }?.let { DaySlice(pillar, it) } }
        return CalendarDay(
            date = date,
            inMonth = date.month == month,
            isToday = date == today,
            isFuture = date > today,
            slices = slices,
            notDone = notDone,
            description = describe(date, today, slices, notDone),
        )
    }

    /** "Monday, October 26. 10 points: Health 2, Wealth 6, Love 2. 3 points didn't happen" */
    private fun describe(date: LocalDate, today: LocalDate, slices: List<DaySlice>, notDone: Int): String {
        val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val name = "${date.weekdayName()}, $month ${date.dayOfMonth}"
        val parts = mutableListOf(if (date == today) "Today, $name" else name)
        if (date <= today) {
            val total = slices.sumOf { it.points }
            parts += if (total == 0) "No points" else {
                "${MissionWords.pointsPhrase(total)}: " + slices.joinToString(", ") { "${it.pillar.title} ${it.points}" }
            }
            if (notDone > 0) parts += "${MissionWords.pointsPhrase(notDone)} didn't happen"
        }
        return parts.joinToString(". ")
    }
}
```


- [ ] **Step 2: Create `screen/metrics/MonthCalendar.kt`**

```kotlin
package com.example.todoapp.screen.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.CalendarDay
import com.example.todoapp.domain.MonthCalendar
import com.example.todoapp.screen.basicutils.CrownGold
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.pillarStrong
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.crown_a

// Flat tints instead of the old gradient (spec 9.1).
private val CalendarTint = Color(0xFFF4F0FA)
private val SelectedWeekTint = Color(0xFFE9E1F6)
private val EmptyRing = Color(0xFFE4DDEC)
private val DayInk = Color(0xFF2A2A2A)
private val CrownColumn = 20.dp
private val weekLetters = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Spec 9: the Monday-first month. Each day's ring is split by points per pillar, with white for what
 * didn't happen, and each balanced week has a crown. The week on screen is tinted. Tapping a day opens
 * it in the diary.
 */
@Composable
fun MonthCalendarCard(calendar: MonthCalendar, openDay: (LocalDate) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp)
            .background(CalendarTint, RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 10.dp)
    ) {
        Text(
            calendar.title, fontSize = 13.sp, letterSpacing = 1.sp, color = Color(0xFF4A4A4A), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() }
        )
        // The letters are for the eye; each day tells a screen reader its own name.
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp).clearAndSetSemantics {}) {
            Spacer(Modifier.width(CrownColumn))
            weekLetters.forEach { letter ->
                Text(letter, fontSize = 11.sp, color = InkGray, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        calendar.weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    .background(if (week.selected) SelectedWeekTint else CalendarTint, RoundedCornerShape(10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(CrownColumn), contentAlignment = Alignment.Center) {
                    if (week.crowned) {
                        Image(
                            painterResource(Res.drawable.crown_a), contentDescription = "Balanced week",
                            colorFilter = ColorFilter.tint(CrownGold), modifier = Modifier.size(16.dp)
                        )
                    }
                }
                week.days.forEach { day -> DayCell(day, Modifier.weight(1f)) { openDay(day.date) } }
            }
        }
        Text(
            "Ring slices are sized by points. White is what didn't happen.",
            fontSize = 12.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
    }
}

/** A 44dp tap target with the day's ring and number. */
@Composable
private fun DayCell(day: CalendarDay, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.height(44.dp)
            .clickable(role = Role.Button, onClickLabel = "Open in the diary", onClick = onClick)
            .semantics { contentDescription = day.description },
        contentAlignment = Alignment.Center
    ) {
        if (!day.isFuture) DayRing(day)
        Text(
            "${day.date.dayOfMonth}",
            fontSize = 13.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.inMonth) DayInk else InkGray.copy(alpha = 0.6f),
            modifier = Modifier.clearAndSetSemantics {}
        )
    }
}

/** Slices in pillar order from the top, sized by points, then white for what didn't happen. */
@Composable
private fun DayRing(day: CalendarDay) {
    Canvas(modifier = Modifier.size(38.dp)) {
        val stroke = 6.dp.toPx()
        val topLeft = Offset(stroke / 2, stroke / 2)
        val arc = Size(size.width - stroke, size.height - stroke)
        val total = day.slices.sumOf { it.points } + day.notDone
        if (total == 0) {
            drawArc(EmptyRing, 0f, 360f, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke))
            return@Canvas
        }
        var start = -90f
        day.slices.forEach { slice ->
            val sweep = 360f * slice.points / total
            drawArc(pillarStrong(slice.pillar), start, sweep, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke))
            start += sweep
        }
        if (day.notDone > 0) {
            drawArc(Color.White, start, 360f * day.notDone / total, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke))
        }
    }
}
```


- [ ] **Step 3: `screen/metrics/MetricsState.kt`: import the calendar**

Replace:

```kotlin
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.Patterns
```

with:

```kotlin
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.MonthCalendar
import com.example.todoapp.domain.MonthGrid
import com.example.todoapp.domain.Patterns
```


- [ ] **Step 4: `screen/metrics/MetricsState.kt`: the calendar in the state**

Replace:

```kotlin
    /** As of today, whichever week is on screen. Empty until the data holds a pattern up. */
    val patterns: List<String>,
)
```

with:

```kotlin
    /** As of today, whichever week is on screen. Empty until the data holds a pattern up. */
    val patterns: List<String>,
    /** The month of the week on screen. */
    val calendar: MonthCalendar,
)
```


- [ ] **Step 5: `screen/metrics/MetricsState.kt`: pass the calendar on**

Replace:

```kotlin
            patterns = Patterns.lines(input.today, byWeek, input.allMissions, days),
        )
```

with:

```kotlin
            patterns = Patterns.lines(input.today, byWeek, input.allMissions, days),
            calendar = MonthGrid.build(week, input.today, input.allMissions, days, byWeek),
        )
```


- [ ] **Step 6: `screen/metrics/MetricsScreen.kt`: import LocalDate for the day taps**

Replace:

```kotlin
import com.example.todoapp.screen.diary.plan.SmallChip
```

with:

```kotlin
import com.example.todoapp.screen.diary.plan.SmallChip
import kotlinx.datetime.LocalDate
```


- [ ] **Step 7: `screen/metrics/MetricsScreen.kt`: a day tap opens the diary**

Replace:

```kotlin
/** Spec 9: the Metrics tab, one week at a time. */
@Composable
fun MetricsScreen(viewModel: MetricsViewModel) {
```

with:

```kotlin
/** Spec 9: the Metrics tab, one week at a time. [openDay] shows a date in the diary. */
@Composable
fun MetricsScreen(viewModel: MetricsViewModel, openDay: (LocalDate) -> Unit) {
```


- [ ] **Step 8: `screen/metrics/MetricsScreen.kt`: the calendar under the patterns**

Replace:

```kotlin
                s.patterns.forEachIndexed { i, line -> item(key = "pattern-$i") { RuledLine(line) } }
```

with:

```kotlin
                s.patterns.forEachIndexed { i, line -> item(key = "pattern-$i") { RuledLine(line) } }
                item(key = "calendar") { MonthCalendarCard(s.calendar, openDay) }
```


- [ ] **Step 9: `screen/NavScreens.kt`: a tapped day opens that date in the diary**

Replace:

```kotlin
                    MetricsScreen(metricsViewModel)
```

with:

```kotlin
                    MetricsScreen(metricsViewModel, openDay = { date ->
                        // The diary shows the date the shared CalDiaryViewModel holds.
                        calDateViewModel.selectDate(date)
                        navigateTab(MyScreen.Diary)
                    })
```


- [ ] **Step 10: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 11: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a changed file (builds have rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/domain/MonthGrid.kt \
  $K/screen/metrics/MonthCalendar.kt \
  $K/screen/metrics/MetricsState.kt \
  $K/screen/metrics/MetricsScreen.kt \
  $K/screen/NavScreens.kt
git commit -m "feat: Monday-first month calendar with day rings"
```


---

### Task 7: One progress line per mission

**Files:**
- Create: `domain/MissionProgress.kt`, `screen/metrics/MissionProgressRow.kt`
- Modify: `screen/metrics/MetricsState.kt`, `screen/metrics/MetricsScreen.kt`

**Depends on:** Task 2 (`DayTasks`, `WeekScores.isAway`).

Under a lilac MISSIONS marker heading, one row per active mission (decisions 1 and 11). Each row has a dot in the pillar's strong color, the title as typed, and a line built from kind, pace, done days and milestones. Finish lines have the flag. Archived missions aren't listed. A screen reader reads each row as one item.

- [ ] **Step 1: Create `domain/MissionProgress.kt`**

```kotlin
package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.models.TaskStatus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.roundToInt

/** One mission's line on the Metrics tab (spec 9). */
data class MissionLine(
    val missionId: Long,
    val title: String,
    val pillar: PillarType,
    val finishLine: Boolean,
    /** "26 of 30 days this month · best streak 14", "Every 12 days on average · you aim for every 2 weeks" */
    val detail: String,
)

/**
 * One progress line per active mission, as of today, from stored data only (spec 7.1): kind, pace,
 * done days, milestones. Archived missions are left out; their points still count in the weeks they
 * were earned in.
 */
object MissionProgress {

    /** "K full weeks in a row" looks back at most this far, inside MetricsViewModel's year of tasks. */
    const val MAX_RUN_WEEKS = 51

    fun lines(
        today: LocalDate,
        missions: List<Mission>,
        history: History,
        milestones: List<Milestone>,
        days: DayTasks,
    ): List<MissionLine> {
        val thisWeek = WeekMath.weekStart(today)
        val milestonesById = milestones.groupBy { it.missionId }
        return missions
            .sortedWith(compareBy({ it.pillarType.ordinal }, { it.missionId }))
            .map { mission ->
                // The mission's done days, each date read once.
                val done = history.tasksFor(mission.missionId)
                    .filter { it.taskState.countsAsDone }
                    .map { it.date }
                    .distinct()
                    .sorted()
                MissionLine(
                    missionId = mission.missionId,
                    title = mission.title,
                    pillar = mission.pillarType,
                    finishLine = mission.missionKind == MissionKind.FINISH_LINE,
                    detail = detail(mission, done, today, thisWeek, history, milestonesById[mission.missionId].orEmpty(), days),
                )
            }
    }

    private fun detail(
        mission: Mission,
        done: List<LocalDate>,
        today: LocalDate,
        thisWeek: LocalDate,
        history: History,
        milestones: List<Milestone>,
        days: DayTasks,
    ): String {
        notLive(mission, today)?.let { return it }
        val weekEnd = thisWeek.plus(6, DateTimeUnit.DAY)
        return when {
            mission.missionKind == MissionKind.BLOCK ->
                "${count(done, thisWeek, weekEnd)} of ${Points.occurrencesInWeek(mission, thisWeek, history)} this week"
            mission.missionKind == MissionKind.RECHARGE ->
                "${count(done, thisWeek, weekEnd)} this week · about ${MissionWords.times(mission.count)} a week"
            mission.missionKind == MissionKind.RHYTHM -> rhythm(mission, done, today)
            mission.missionKind == MissionKind.FINISH_LINE && milestones.isNotEmpty() -> milestone(milestones)
            mission.periodUnit == Period.DAY -> daily(mission, done, today)
            mission.periodUnit == Period.MONTH ->
                "${count(done, LocalDate(today.year, today.month, 1), today)} of ${mission.count} this month"
            else -> weekly(mission, done, thisWeek, history, days)
        }
    }

    /** Paused, not started yet, or past its end date. */
    private fun notLive(mission: Mission, today: LocalDate): String? {
        mission.pausedUntil?.toLocalDate()?.let { if (it > today) return "Paused until ${it.shortLabel()}" }
        val start = mission.startDate.toLocalDate()
        if (start > today) return "Starts ${start.shortLabel()}"
        mission.endDate?.toLocalDate()?.let { if (it < today) return "Ended ${it.shortLabel()}" }
        return null
    }

    /** "26 of 30 days this month · best streak 14". Today counts once it's done, so a morning isn't a day missed. */
    private fun daily(mission: Mission, done: List<LocalDate>, today: LocalDate): String {
        val from = maxOf(LocalDate(today.year, today.month, 1), mission.startDate.toLocalDate())
        val through = if (done.lastOrNull() == today) today else today.minus(1, DateTimeUnit.DAY)
        val dayCount = if (through < from) 0 else from.daysUntil(through) + 1
        val best = bestStreak(done)
        val streak = if (best >= 2) " · best streak $best" else ""
        if (dayCount == 0) return "Every day$streak"
        return "${count(done, from, through)} of $dayCount ${if (dayCount == 1) "day" else "days"} this month$streak"
    }

    /** "Every 12 days on average · you aim for every 2 weeks" */
    private fun rhythm(mission: Mission, done: List<LocalDate>, today: LocalDate): String {
        val aim = "you aim for every ${mission.intervalLabel()}"
        return when (done.size) {
            0 -> "Not done yet · $aim"
            1 -> {
                val ago = done.last().daysUntil(today)
                val last = when (ago) {
                    0 -> "Last done today"
                    1 -> "Last done yesterday"
                    else -> "Last done $ago days ago"
                }
                "$last · $aim"
            }
            else -> {
                val average = (done.first().daysUntil(done.last()).toFloat() / (done.size - 1)).roundToInt()
                "Every ${if (average == 1) "day" else "$average days"} on average · $aim"
            }
        }
    }

    /** "Milestone 2 of 3 · due Dec 20", as the MILESTONE reason line puts it. Milestone texts aren't shown. */
    private fun milestone(milestones: List<Milestone>): String {
        val mine = milestones.sortedBy { it.milestoneOrder }
        val next = mine.indexOfFirst { it.status != TaskStatus.COMPLETED.value }
        if (next < 0) return "All ${mine.size} milestones done"
        return "Milestone ${next + 1} of ${mine.size} · due ${mine[next].expectedCompletionDate.toLocalDate().shortLabel()}"
    }

    /** "2 of 3 this week", then the gentle pace or a run of full weeks. */
    private fun weekly(mission: Mission, done: List<LocalDate>, thisWeek: LocalDate, history: History, days: DayTasks): String {
        val target = Points.occurrencesInWeek(mission, thisWeek, history)
        if (target == 0) return "Nothing due this week"
        val base = "${count(done, thisWeek, thisWeek.plus(6, DateTimeUnit.DAY))} of $target this week"
        val goal = mission.goalCount
        if (goal != null && goal > mission.count) {
            return "$base · ${if (mission.count == 1) "once a week" else "${mission.count}× a week"} for now"
        }
        val run = fullWeeks(mission, done, thisWeek, history, days)
        return if (run >= 2) "$base · $run full weeks in a row" else base
    }

    /**
     * Finished weeks in a row, back from last week, in which it was done as often as it was due. A week
     * it wasn't due ends the run; an away week (no tasks at all) neither counts nor ends it.
     */
    private fun fullWeeks(mission: Mission, done: List<LocalDate>, thisWeek: LocalDate, history: History, days: DayTasks): Int {
        var week = thisWeek.minus(7, DateTimeUnit.DAY)
        var run = 0
        repeat(MAX_RUN_WEEKS) {
            if (!WeekScores.isAway(week, days)) {
                val due = Points.occurrencesInWeek(mission, week, history)
                if (due == 0 || count(done, week, week.plus(6, DateTimeUnit.DAY)) < due) return run
                run++
            }
            week = week.minus(7, DateTimeUnit.DAY)
        }
        return run
    }

    /** Longest run of consecutive done days in [done], which is sorted and has no repeats. */
    private fun bestStreak(done: List<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in done) {
            run = if (previous != null && previous.plus(1, DateTimeUnit.DAY) == day) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }

    private fun count(done: List<LocalDate>, from: LocalDate, to: LocalDate): Int = done.count { it >= from && it <= to }
}
```


- [ ] **Step 2: Create `screen/metrics/MissionProgressRow.kt`**

```kotlin
package com.example.todoapp.screen.metrics

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.MissionLine
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarStrong
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.flag_open

/** One mission: a pillar dot, the title as typed, and its progress line. A screen reader reads it as one item. */
@Composable
fun MissionProgressRow(line: MissionLine) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .heightIn(min = 44.dp)
            .drawBehind {
                val y = size.height - 1.dp.toPx()
                drawLine(Blue80, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 4.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(10.dp).background(pillarStrong(line.pillar), CircleShape)
                .semantics { contentDescription = line.pillar.title }
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(line.title, fontSize = 15.sp, color = InkBlack)
            Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (line.finishLine) {
                    Image(
                        painterResource(Res.drawable.flag_open), contentDescription = "Finish line",
                        colorFilter = ColorFilter.tint(pillarInk(line.pillar)), modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(line.detail, fontSize = 13.sp, color = Color(0xFF5A5A5A))
            }
        }
    }
}
```


- [ ] **Step 3: `screen/metrics/MetricsState.kt`: import the mission lines**

Replace:

```kotlin
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.MonthCalendar
```

with:

```kotlin
import com.example.todoapp.domain.LinePart
import com.example.todoapp.domain.MissionLine
import com.example.todoapp.domain.MissionProgress
import com.example.todoapp.domain.MonthCalendar
```


- [ ] **Step 4: `screen/metrics/MetricsState.kt`: mission lines in the state**

Replace:

```kotlin
    /** The month of the week on screen. */
    val calendar: MonthCalendar,
)
```

with:

```kotlin
    /** The month of the week on screen. */
    val calendar: MonthCalendar,
    /** One per active mission, as of today, in pillar order. */
    val missions: List<MissionLine>,
)
```


- [ ] **Step 5: `screen/metrics/MetricsState.kt`: pass the mission lines on**

Replace:

```kotlin
            calendar = MonthGrid.build(week, input.today, input.allMissions, days, byWeek),
        )
```

with:

```kotlin
            calendar = MonthGrid.build(week, input.today, input.allMissions, days, byWeek),
            missions = MissionProgress.lines(input.today, missions, history, input.milestones, days),
        )
```


- [ ] **Step 6: `screen/metrics/MetricsScreen.kt`: the mission lines under the calendar**

Replace:

```kotlin
                item(key = "calendar") { MonthCalendarCard(s.calendar, openDay) }
```

with:

```kotlin
                item(key = "calendar") { MonthCalendarCard(s.calendar, openDay) }
                item(key = "missions-heading") {
                    MarkerHeading("MISSIONS", MissionsMarker, Modifier.padding(top = 24.dp, bottom = 4.dp))
                }
                if (s.missions.isEmpty()) {
                    item(key = "missions-none") { RuledLine("Missions you add show their progress here.", color = InkGray) }
                }
                s.missions.forEach { line -> item(key = "mission-${line.missionId}") { MissionProgressRow(line) } }
```


- [ ] **Step 7: `screen/metrics/MetricsScreen.kt`: the marker color of the missions heading, from the mockup**

Replace:

```kotlin
/** Spec 9: the Metrics tab, one week at a time. [openDay] shows a date in the diary. */
```

with:

```kotlin
private val MissionsMarker = Color(0xFFE8DEF8)

/** Spec 9: the Metrics tab, one week at a time. [openDay] shows a date in the diary. */
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
git checkout -- shared/schemas            # only if the line above listed a changed file (builds have rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/domain/MissionProgress.kt \
  $K/screen/metrics/MissionProgressRow.kt \
  $K/screen/metrics/MetricsState.kt \
  $K/screen/metrics/MetricsScreen.kt
git commit -m "feat: one progress line per mission on the Metrics tab"
```


---

### Task 8: A seed with five weeks of history

**Files:**
- Create: `db/MetricsSample.kt`
- Modify: `screen/init/InitViewModel.kt`

**Depends on:** None in this plan. It uses SampleData's mission titles (Plan 1, extended by Plan 2).

For hand testing (decision 13). With `SEED_SAMPLE_MISSIONS` and the new `SEED_METRICS_HISTORY` both on, an empty database gets SampleData's missions and history, plus five more weeks. Both stay off in the commit.

What the five weeks hold:
- Work blocks Monday to Friday, ticked. One App block is skipped, 3 weeks back.
- Gym on every day SampleData didn't write, with a gap 5 weeks back.
- Call Mom on Mondays. Call sister on a different day each week; it's moved 3 weeks back and skipped 5 weeks back.
- Guitar at its gentle 2× in weeks 4 and 5 back. Dance class on Saturdays. New ideas twice.
- Call Grandma every 2 weeks before SampleData's call.

Weeks 2 and 4 back come out balanced.

The seed is written relative to the day it runs. The checklist says which numbers depend on that weekday. Its `TodayTask`s use named arguments only, so a new column with a default (Plan 2's review adds `autoTicked`) doesn't affect it.

- [ ] **Step 1: Create `db/MetricsSample.kt`**

```kotlin
package com.example.todoapp.db

import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Reason
import com.example.todoapp.domain.ReasonCode
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.weekdayName
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Five more weeks of history for the Metrics tab, on top of SampleData's missions. It runs only with
 * SEED_SAMPLE_MISSIONS on an empty database. Turn both on to try the rings, crowns, patterns and calendar
 * without weeks of ticking. Leave it off in a release.
 */
const val SEED_METRICS_HISTORY = false

object MetricsSample {

    /** Weeks written before this one. */
    private const val WEEKS = 5
    /** SampleData already ticked Gym on each of the last 21 days. */
    private const val SAMPLE_GYM_DAYS = 21

    suspend fun seed(missionRepository: MissionRepository, todayTaskRepository: TodayTaskRepository) {
        val ids = missionRepository.getAll().associate { it.title to it.missionId }
        todayTaskRepository.insertAll(history(ids, MyDate.now().toLocalDate()))
    }

    /**
     * The tasks, given SampleData's mission ids by title. Days SampleData already wrote are left alone.
     * Weeks 2 and 4 back come out balanced; weeks 1, 3 and 5 have Life (and in week 5 Love) light.
     */
    fun history(ids: Map<String, Long>, today: LocalDate): List<TodayTask> {
        val tasks = mutableListOf<TodayTask?>()
        val thisWeek = WeekMath.weekStart(today)
        fun task(title: String, date: LocalDate, state: TaskState, points: Int, reason: Reason, fixed: Boolean = false, movedFrom: LocalDate? = null) =
            ids[title]?.let { id ->
                TodayTask(
                    missionId = id,
                    taskDate = MyDate.fromLocalDate(date),
                    state = state.name,
                    pointsEarned = points,
                    isFixedBlock = fixed,
                    movedFromDate = movedFrom?.let { MyDate.fromLocalDate(it) },
                    reasonCode = reason.code.name,
                    reasonArgs = reason.encodeArgs(),
                )
            }
        fun done(title: String, date: LocalDate, points: Int, reason: Reason = Reason(ReasonCode.ADDED)) =
            task(title, date, TaskState.DONE, points, reason)
        for (back in 1..WEEKS) {
            val monday = thisWeek.minus(7 * back, DateTimeUnit.DAY)
            fun day(offset: Int) = monday.plus(offset, DateTimeUnit.DAY)
            // Work blocks Monday to Friday, ticked. One App block 3 weeks back is skipped: a white slice.
            val job = Reason(ReasonCode.BLOCK, listOf("9:00", "1:00"))
            val app = Reason(ReasonCode.BLOCK, listOf("2:00", "5:00"))
            for (offset in 0..4) {
                tasks += task("Job", day(offset), TaskState.DONE, 3, job, fixed = true)
                val skipped = back == 3 && offset == 2
                tasks += task("App", day(offset), if (skipped) TaskState.SKIPPED else TaskState.DONE, if (skipped) 0 else 3, app, fixed = true)
            }
            // Gym on every day SampleData didn't write, with one gap 5 weeks back.
            for (offset in 0..6) {
                val date = day(offset)
                if (date.daysUntil(today) > SAMPLE_GYM_DAYS && !(back == WEEKS && offset == 2)) {
                    tasks += done("Gym", date, 2, Reason(ReasonCode.STREAK, listOf("0")))
                }
            }
            // Call Mom on Mondays; Call sister on a different day each week.
            tasks += done("Call Mom", day(0), 2)
            when (back) {
                1 -> tasks += done("Call sister", day(1), 2)
                2 -> tasks += done("Call sister", day(3), 2)
                3 -> {
                    // Moved from Tuesday and done on Wednesday.
                    tasks += task("Call sister", day(1), TaskState.MOVED, 0, Reason(ReasonCode.ADDED))
                    tasks += task("Call sister", day(2), TaskState.DONE, 2, Reason(ReasonCode.MOVED, listOf(day(1).weekdayName())), movedFrom = day(1))
                }
                4 -> tasks += done("Call sister", day(4), 2)
                else -> tasks += task("Call sister", day(1), TaskState.SKIPPED, 0, Reason(ReasonCode.ADDED))
            }
            // Guitar at its gentle 2× (SampleData wrote the last 3 weeks), Dance class on Saturdays, New ideas twice.
            if (back > 3) {
                tasks += done("Guitar", day(0), 3)
                tasks += done("Guitar", day(3), 3)
            }
            if (back < WEEKS) tasks += done("Dance class", day(5), 3)
            if (back == 2 || back == 4) tasks += done("New ideas", day(2), 1)
        }
        // Call Grandma every 2 weeks before SampleData's call 13 days ago.
        tasks += done("Call Grandma", today.minus(27, DateTimeUnit.DAY), 2)
        tasks += done("Call Grandma", today.minus(41, DateTimeUnit.DAY), 2)
        return tasks.filterNotNull()
    }
}
```


- [ ] **Step 2: `screen/init/InitViewModel.kt`: import the metrics seed**

Replace:

```kotlin
import com.example.todoapp.db.SEED_SAMPLE_MISSIONS
import com.example.todoapp.db.SampleData
```

with:

```kotlin
import com.example.todoapp.db.MetricsSample
import com.example.todoapp.db.SEED_METRICS_HISTORY
import com.example.todoapp.db.SEED_SAMPLE_MISSIONS
import com.example.todoapp.db.SampleData
```


- [ ] **Step 3: `screen/init/InitViewModel.kt`: the metrics weeks go in right after the sample missions**

Replace:

```kotlin
            if (SEED_SAMPLE_MISSIONS && missionRepository.count() == 0) {
                SampleData.seed(missionRepository, todayTaskRepository)
            }
```

with:

```kotlin
            if (SEED_SAMPLE_MISSIONS && missionRepository.count() == 0) {
                SampleData.seed(missionRepository, todayTaskRepository)
                if (SEED_METRICS_HISTORY) MetricsSample.seed(missionRepository, todayTaskRepository)
            }
```


- [ ] **Step 4: Build**

Run from the repo root:

```bash
/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle :androidApp:assembleDebug --console=plain 2>&1 | tail -60
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a changed file (builds have rewritten 4.json before)
K=shared/src/commonMain/kotlin/com/example/todoapp
git add $K/db/MetricsSample.kt \
  $K/screen/init/InitViewModel.kt
git commit -m "feat: optional seed with five weeks of metrics history"
```


---

### Review fixes after Task 8

Each task's review asked for fixes (M1 to M35). They were made after Task 8 and before Task 9. The decisions above, Task 9's spec text and the checklist below already describe the result. The checklist's seed numbers were then re-derived for every weekday by running the seed, the close-out's summaries and `MetricsStateBuilder` on the JVM, in a throwaway test that wasn't kept.

- `50197a8`: week loading, midnight and away weeks. M1 to M15, M25, and M35 for the rings, the highlights card and `AppColors`.
- `2275fba`: one rule for what a finished week carried (`WeekAhead.carried`, shared with the diary's Last week card), the wand only on adjustments, and stricter patterns. M16 to M24.
- `0022821`: calendar ring edges, no rings in away weeks, and mission lines as of today. M26 to M34, and M35 for the calendar and mission rows.
- The commit with this section: these notes, Task 9's spec text and the checklist.

Files from earlier plans that the fixes edit. The diary stays as it was apart from M17 and M18:
- `domain/PaceOffers.kt`: `openedWeeks`, Plan 2's opened-week rule, now shared; `full` is now `internal`.
- `domain/WeekSummaries.kt`: `balanced(earned, target)`, now shared.
- `domain/MissionWords.kt`: "All 1 milestone done" on the Missions tab too.
- `screen/metrics/CalDiaryViewModel.kt` and `screen/NavScreens.kt`: `today` from the midnight check, and the Metrics view model keyed on it.
- `screen/basicutils/AppColors.kt`: the Metrics tab's colors.
- `screen/diary/WeekState.kt`: `adjusted` reads `WeekAhead.carried`, with the same inclusion rules and cap of 3 as before, and each line says whether it gets the wand.
- `screen/diary/plan/WeekNotes.kt` and `screen/diary/plan/PlanNotes.kt`: `HighlightRow` draws a quiet dot when it has no icon, used for "Nothing to catch up on.".

Left as they were, by choice:
- An away week still lists what it carried, as the diary does.
- Moves that move twice, and past weeks read with today's missions and limits.
- One ruled line per pattern, not one per wrapped line.
- Pastel arcs follow the spec's color table; the numbers carry the data.
- Weights at the time of a tick, remembering the browsed week across tabs, and the diary's words for a future day.
- How far back the arrows go still depends on the stored weeks.

---

### Task 9: iOS compile, spec sync and hand-off

**Files:**
- Modify: `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`

**Depends on:** Tasks 1 to 8.

Compiles the shared module for iOS, records in the spec how the Metrics tab was built, and hands the build over for testing.

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

- [ ] **Step 3: `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`: Section 9.2: the Metrics tab as built**

Replace:

```markdown
- **Tap targets** are at least 44 px.
```

with:

```markdown
- **Tap targets** are at least 44 px.

### 9.2 Metrics, as built in plan 4

- **One week at a time.** Arrows step back to the first stored week and forward to this week, and "Back to this week" returns. The tab opens on this week, and starts again after midnight. A date in another year than this week's carries its year.
- **Rings.** A finished week shows its stored summary. This week is worked out live, as the diary's bars are. Each ring reads "full" or "light" at the 80% rule, and a pillar with nothing due is drawn full. A finished week without an item of its own (items moved in don't count) reads "away", with no numbers and no arc: the diary wasn't opened, so its zeros aren't misses.
- **What next week adjusts.** Only what the app knows, at most 3 lines: items "move it" carried into the week after, and every-so-often missions due and not done, by the same rule as the diary's "Last week" card. For this week it also lists longer rhythms coming due and pauses ending next week. Titles sit on chips. Only a line about something the app adjusted has the wand; the others, here and on the "Last week" card, have a quiet dot. With nothing to list it says "Nothing to catch up on."
- **Highlights card.** Points (each pillar counted up to its target, the rest as "+N extra"), pillars full ("so far" for this week, "Nothing was due that week." when nothing was), and balanced weeks out of the last 4 finished weeks. Away weeks aren't counted.
- **Patterns.** As of today, at most 3 lines, each shown only when the data holds it up:
  - balanced weeks in a row (2 or more);
  - a pillar full 3 or more weeks running (at most 2 pillars), or "for a year or more";
  - the weekday a pillar's ticks mostly land on: 8 or more ticks in 8 weeks, more than half of them on one day, from missions free to land on any day (not fixed blocks, daily missions or missions with set days). It always keeps a slot.
  Away weeks neither count nor break a run. With none: "Nothing stands out yet."
- **Calendar.** The month of the shown week's Monday, Monday first. Day rings are split by points per pillar, inside a thin edge. White is skipped or moved items, the missing half of a did-some, and anything left planned on an earlier day. A day with nothing on it is one plain thin ring; away weeks have no rings. Only finished weeks get a crown. Today has an outline. Tapping a day opens it in the diary.
- **Mission lines.** One per active mission, as of today: a daily habit's days this month and best streak, a rhythm's average gap against its aim, a finish line's next milestone, a weekly count with a gentle pace or a run of full weeks (80% of its own points, as for pace offers, since its last pace change). Archived missions aren't listed, but their points still count in every week and day ring.
```


- [ ] **Step 4: Commit**

Nothing under `shared/schemas` may change in this task. Check, restore if needed, then stage by path:

```bash
git status --short shared/schemas        # expected: nothing
git checkout -- shared/schemas            # only if the line above listed a changed file (builds have rewritten 4.json before)
git add docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md
git commit -m "docs: sync the redesign spec with plan 4"
```


- [ ] **Step 5: Hand over for manual testing**

Tell the owner the build is ready and point them to the checklist below.

---

## Manual hand-test checklist

### Before you start

- **Install over the Plan 2 build without clearing storage.** Plan 4 changes no schema, so missions, ticks and stored weeks stay. Open the Diary first: on the first open of a new week, it writes the summaries the Metrics tab reads.
- **A device that ever ran a Plan 2 build from before its review fixes must clear storage first,** as Plan 2's checklist says: that build's version 6 isn't the final one, so Room would stop on launch.
- **Several weeks of data quickly (the seed).**
  1. In `db/SampleData.kt` set `SEED_SAMPLE_MISSIONS = true`, and in `db/MetricsSample.kt` set `SEED_METRICS_HISTORY = true`. Don't commit these.
  2. Build, clear the app's storage, and open the app. It opens on the Diary, which writes about 13 stored weeks and plans today.
  3. Then tap Metrics.
  4. Turn both flags off when done.
  - Numbers below are for the morning right after seeding, before you tick anything. Some depend on the weekday you seed on; these are marked "(on a Sunday: …)". They were re-derived after the review fixes by running the seed for every weekday.
  - "1 week back" is last week.
- **Moving the date.** For sections C and H, change the device date, as in Plan 2's checklist:
  - **Android:** Settings → System → Date & time → turn off automatic time.
  - **iOS simulator:** it follows the Mac's clock.
  - Reopen the app after changing it, and only move forward.
- **A screen reader** (TalkBack or VoiceOver) is needed for the accessibility checks marked "Screen reader".

### A. The tab and its week arrows
1. Metrics opens on "This week" with this week's dates ("Oct 26 to Nov 1"). The right arrow is faded and does nothing. There's room under the dates for "Back to this week", so the page doesn't jump when the chip comes and goes.
2. Tap the left arrow: "Last week", then "Week of …", with each week's dates. A "Back to this week" chip appears; tapping it returns.
3. Keep going back until the left arrow fades. The first week is the one with SampleData's Plan winter trip tick: 13 weeks back (12 on a Saturday or Sunday).
4. Tap the left arrow twice quickly: it moves two weeks. Tap the arrows quickly several times: it never goes past the first week or this week.
5. Switch to Missions or Diary and back. Metrics opens on this week again.
6. Leave Metrics open across midnight (set the time to 23:59 and wait). Within a few seconds of midnight it starts again on this week as of the new day. Leaving the app before midnight and coming back after it does the same at once.
7. With a long-used install, a week in another year shows its year: "Week of Dec 22, 2025", "Dec 22 to Dec 28, 2025".
8. Screen reader: the arrows read "Previous week" and "Next week" (and say when they're unavailable). The title and dates read as one heading, and are read again when the week changes.

### B. Rings and the highlights card (seed)
1. **This week.** Four rings with live numbers. Tick Gym in the diary and come back: Health goes up by 2.
2. **Past weeks.** Step back and compare:

| Week | Rings | Card |
|---|---|---|
| 1 back | Health 14/14, Wealth 30/30, Love 4/4 (on a Sunday: 6/6), all full; Life 9/12 light | "57 of 60 points" (Sunday: "59 of 62"), "3 of 4 pillars full", "2 balanced weeks out of the last 4" |
| 2 back | All four full, Life 10/12 | "60 of 62 points" (Sunday: "58 of 60"); a crown and "All 4 pillars full · a balanced week" |
| 3 back | Wealth 27/30 full; Life 9/12 light | "54 of 60 points" (Sunday: "56 of 62"), "3 of 4 pillars full", "1 balanced week out of the last 4" (Sunday: "…of the last 3") |
| 4 back | All four full | Crown, "a balanced week", "1 balanced week out of the last 3" (Sunday: "…of the last 2") |
| 5 back | Health 12/14 full, Wealth full, Love and Life light | "2 of 4 pillars full", "No balanced week in the last 2" (Sunday: "No balanced week yet") |

3. **This week's card.** "THIS WEEK" with the calendar icon. The star line reads "N of M points", the heart line "X of 4 pillars full so far", and the crown line "2 balanced weeks out of the last 4".
4. **Away weeks.** Step back past week 5 to a week with no ticks. On Monday to Saturday, week 6 back still has a Call Grandma tick, so go one further.
   - Every ring reads "away" in grey, with "–" in place of its numbers and no colored arc.
   - The card's star line says "No points to count." and its heart line "The diary wasn't opened that week.", with no crown.
   - Its crown line reads "No weeks to count yet." once only away weeks are left in its 4.
5. **"full" and "light".** A ring reads "full" in its pillar's ink at 80% or more, otherwise "light" in grey. If a pillar passes its target (tick an extra item with the pencil), "+N extra" appears under it.
6. **A pillar with nothing due** (optional: pause or archive every Love mission). Its ring is drawn full and reads "0/0 full". With nothing due in any pillar, the card reads "Nothing is due this week.", and its star line "+N extra" once something is ticked.
7. **Screen reader:** a ring reads as one item, for example "Health, 14 of 14 points, full" or, on an away week, "Love, away".
8. **Large text.** At the largest font size the rings stay round and their numbers fit inside.

### C. What next week adjusts
1. **This week, Monday to Saturday.** "NEXT WEEK" with two lines, each with a quiet dot, not the wand (nothing has been adjusted yet):
   - "⟨Call Grandma⟩ is due this week. If it waits, it moves into next week."
   - "⟨Plan winter trip⟩ is due this week. If it waits, it moves into next week."
   On a Sunday both read "… comes due next week." instead, also with a dot. Titles sit on chips in their pillar's color.
2. Tick Call Grandma in the diary (the pencil adds it if it isn't on today's list), then come back. Its line is gone.
3. Seeded on Monday to Thursday, move the date two days ahead and reopen. Plan winter trip's due date has passed, so its line reads "… is still due. If it waits, it moves into next week."
4. **Past weeks** read "THE WEEK AFTER". For the seeded weeks the line is "Nothing to catch up on.", with a dot. For an away week before Call Grandma's first tick it reads "⟨Call Grandma⟩ stayed due, so it moved into the week after.", with the wand.
5. **"moved to".** On a Saturday or Sunday evening, use the evening check's "Move to …" on an unticked pick, so it lands on next Monday. Back on Metrics: "⟨it⟩ moved to Monday.", with the wand. On that Monday, the diary's Last week card and Metrics' "Last week" name the same missions: one rule makes both.
6. **The diary's Last week card.** Its "Nothing to catch up on." has a quiet dot instead of the wand; "moved to" and "is still due" lines keep the wand.
7. **"is back next week"** (optional, with Plan 2's seed offers). Answer "Pause a month" on New ideas. Move the date forward to the week before the pause ends. "⟨New ideas⟩ is back next week.", with a dot.
8. No line ever gives a reason ("… was light, so …").

### D. Patterns (seed)
1. Under PATTERNS, on ruled lines, the same on every week:
   - "Health has been full 5 weeks running."
   - "Wealth has been full 5 weeks running."
2. There's no "balanced weeks in a row" line: last week wasn't balanced. There's no weekday line either. Call Mom and Call sister have set days, Gym is daily and the work blocks are fixed, so they're left out. That leaves Love only Call Grandma's 3 ticks, and Life's 16 ticks spread over four days (Guitar's Mondays and Thursdays, Dance class, New ideas), none with more than half.
3. On any week but this one, a quiet "As of today" sits under the heading.
4. A fresh install reads "Nothing stands out yet."

### E. The calendar (seed)
1. **Layout.**
   - Month title ("OCTOBER 2026"), then M T W T F S S, Monday first.
   - Whole weeks, with the next or previous month's days greyed. Days after today have only their number. Today's number is bold, and today has an outline.
   - The shown week's row is tinted. Stepping weeks moves the tint, and at a month boundary the calendar changes month.
2. **Crowns.** Weeks 2 and 4 back have a gold crown in the left column. Step back a week or two if they fall in the previous month.
3. **White slices.**
   - 3 weeks back, Tuesday: a white slice (Call sister moved).
   - 3 weeks back, Wednesday: white (the skipped App block), plus a Love slice (Call sister done there).
   - 5 weeks back, Tuesday: white (Call sister skipped).
   - Other days are fully colored, split by pillar.
4. **Edges.** Every ring with points, or with only white, has a thin darker edge outside and inside, so white reads as part of the ring on either tint. On Monday to Saturday, week 6 back's empty days are one thin plain line each, visible on the tinted row too. The away weeks before it have no rings.
5. **By hand.** "Did some" on today's item shows half its slice plus white, once the day is over. A ticked New episode adds no slice.
6. **Taps.** Tap any day: the Diary opens on that date. Days are easy to hit on a small phone (each cell is at least 44dp tall, and taller with large text).
7. **Screen reader.**
   - A day reads, for example, "Tuesday, October 6. 8 points: Health 2, Wealth 6. 2 points didn't happen", and "Open in the diary" as its action. Today, before anything is ticked, reads "Today, ‹weekday›, ‹date›. No points yet".
   - A crown reads "Balanced week of Oct 12", with its own week's Monday.
   - The weekday letters aren't read.

### F. Mission lines (seed)
1. **In order** (Health, Wealth, Love, Life, Recharge), each with a pillar dot:
   - Gym: "N of N days this month · best streak S". S is 32 on a Monday seed and one more for each later weekday. On the 1st of a month: "Every day · best streak S".
   - Job and App: "D of 5 this week", D being the weekdays before today.
   - Call Mom: "0 of 1 this week · 5 full weeks in a row".
   - Call sister: "0 of 1 this week · 4 full weeks in a row".
   - Call Grandma: "Every 14 days on average · you aim for every 2 weeks".
   - Guitar: "0 of 2 this week · 2× a week for now".
   - Dance class, with the flag: "0 of 1 this week · 4 full weeks in a row".
   - New ideas: "0 of 3 this week".
   - Plan winter trip: "Last done 89 days ago · you aim for every 3 months".
   - New episode: "0 this week · about twice a week".
2. Tick things in the diary. The "this week" counts go up, and Gym's month count and streak include today once it's ticked.
3. **A finish line with milestones** (write one in Missions). "Milestone 1 of 3 · due ‹date›". After ticking all of them in the diary: "All 3 milestones done". With a single milestone: "All 1 milestone done", here and on the Missions tab.
4. **Archive a mission** from its editor.
   - Its line goes.
   - Its earlier points stay: past weeks' rings, this week's ring and the day rings don't change.
5. **Screen reader:** a row reads as one item: pillar, title, line.
6. **Full weeks** mean 80% of the mission's own points, as for pace offers: a week with only a did-some of Call Mom isn't full. Changing a mission's pace or weight in the editor starts its run again from that day.

### G. Look and feel
- The paper background, the marker headings (PATTERNS yellow, MISSIONS lilac) and the highlights card match the diary's evening card.
- The calendar uses flat tints, not a gradient.
- Nothing in the new copy uses "missed", "failed", "lagging" or an exclamation mark.
- No title is ever put into a sentence except on a chip.
- Mission titles on the rows are Medium weight, as on the Missions tab. The calendar card lines up with the highlights card.

### H. Away weeks without the seed (date travel)
1. Use the app on some days of one week. Make sure no "Move it" item lands in the next week.
2. Move the date two weeks ahead and open the app. The diary writes a summary for the skipped week.
3. On Metrics, step back to the skipped week:
   - Every ring reads "away", with "–" and no arc.
   - The card says "No points to count." and "The diary wasn't opened that week."
   - There's no crown on the calendar, and its days have no rings.
   - The crown line on the following weeks leaves it out.
   - A pattern run, or a mission's "full weeks in a row", continues across it rather than ending.
4. The skipped week is now last week, so today's diary has a Last week card for it that says the same: "No points to count." and "The diary wasn't opened that week.", with no crown. Its adjusted lines stay.
5. A finished week with nothing due (Plan 2's C8 sets one up) reads "Nothing was due that week." on both the Metrics card and the diary's Last week card.

### I. Speed
- With the seed (about four months of ticks), the tab opens without a visible pause, and stepping weeks feels instant.
- Step back past 8 months (with a long-used install): the calendar still has rings, since the task window moves back with the week.

### J. iOS
`:shared:compileKotlinIosSimulatorArm64` passes (Task 9). To run the simulator with other dates, change the Mac's date.

---

## What comes next (ideas library pass)

- **The ideas library** (spec 8): write-your-own matching and the review screen. + on Missions should open the library instead of the sentence (Plan 3's note).
- **`ideaId` on `Mission` is the next schema version: 7.** Version 6 is Plan 2's, including its review additions (`paceSince`, `autoTicked`, the `pace_offer` index). Add `AutoMigration(from = 6, to = 7)` and commit `7.json` beside `6.json`. Keep the destructive fallback limited to versions 1 to 4. Plan 4 changed no schema, so it adds nothing to that migration.
- **Long rhythms from `ideas.json`** (6, 12 and 24 months): cap them at 3 months when importing, as Plans 1 and 3 note. If they're allowed instead, widen these windows past the longest interval:
  - `PlanRepository.HISTORY_DAYS`, which Metrics reads too;
  - `Patterns.LOOK_BACK_DAYS` (a year), which bounds a rhythm's average gap, a best streak and every run.
- **Small follow-ups, not scheduled:**
  - There's no archived-missions list or restore. Archived missions only lose their Metrics line; their points stay.
  - The seed shows no weekday line since the review fixes: its Monday calls have a set day. A seeded mission without set days, ticked mostly on one weekday, would show one.
  - The calendar has no month arrows of its own; it follows the shown week.
  - Mission rows don't open the editor when tapped.
  - The weekday pattern counts did-some like done.
