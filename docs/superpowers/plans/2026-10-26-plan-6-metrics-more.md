# Plan 6: More Metrics, and swipe to fill

**Goal:** Six asks from the owner's first look at Plans 4 and 5:
- Metrics opens at the top.
- A full-screen "All months" calendar, filtered by pillars or missions.
- Stickers sit over the day number.
- More patterns.
- Rings close in a light shade once they're close enough.
- The old swipe-to-fill on diary items is back.

**Spec:** `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`. This plan adds 6.5 and 9.4. It also changes 5 ("Did some"), 9.2 (opens at the top, closed rings, patterns, closed day rings), 9.3 (sticker placement) and 10 (version 7).

**Owner's answers:**
- Points don't follow the swipe's percent. 1–99% is a did-some (half), 100% is done.
- Rings close at 80%, or at all but 2 once there are 6 or more items.
- All four new kinds of pattern go in.
- "Recharging day" stays small in the corner, so a quiet day keeps its number.

**Architecture:**
- Rules stay in `domain/` as plain Kotlin, read by the screens' state builders.
- Only Task 5 writes. It adds one column, through `PlanRepository`'s lock.
- The All months screen reads through its own view model, made only while it's open.

## Files

Paths under `shared/src/commonMain/kotlin/com/example/todoapp/`.

| Task | File | Change |
|---|---|---|
| 1 | `screen/metrics/MetricsScreen.kt` | The list appears only once the state is built, so it opens at the top |
| 1 | `screen/metrics/MonthCalendar.kt` | Earned badges sit as a 26dp sticker in the ring's middle, with no number; "Recharging day" stays 20dp on the lower right |
| 2 | `domain/Points.kt` | `closes(earned, target, items, done)` and its two constants |
| 2 | `domain/WeekScores.kt` | `PillarScore.items`, `.done`, `.closed`, `.nearly` |
| 2 | `domain/MonthGrid.kt` | `CalendarDay.missed` (per pillar), `.closed`; the description says "ring closed" |
| 2 | `screen/metrics/PillarRings.kt`, `MonthCalendar.kt` | Light remainder on closed rings; "nearly" |
| 3 | `domain/Patterns.kt` | Lines become `List<LinePart>`; six new kinds of line; no line cap (the screen folds them) |
| 3 | `domain/MissionProgress.kt`, `domain/DayBadges.kt` | Share the mission full-week run; `DayBadgeBook.counts(date)` |
| 3 | `screen/metrics/MetricsState.kt`, `MetricsScreen.kt` | Patterns as chip lines; the first 4, then "More patterns" |
| 4 | `domain/CalendarFilter.kt` | New: the filter, the item rule and the summary line |
| 4 | `domain/MonthGrid.kt` | `build` takes a selected week (or none) and a filter |
| 4 | `screen/metrics/allmonths/AllMonthsViewModel.kt`, `AllMonthsScreen.kt`, `MissionFilterDialog.kt` | New |
| 4 | `screen/metrics/MonthCalendar.kt`, `MetricsScreen.kt`, `di/DI.kt` | "All months" chip; the caption is optional; the overlay; the factory |
| 5 | `db/data/todotask/TodayTask.kt`, `db/Database.kt`, `shared/schemas/.../7.json` | `progress`; version 7 with `AutoMigration(6, 7)` |
| 5 | `domain/TaskProgress.kt` | New: the shown percent and the state a percent saves as |
| 5 | `repo/PlanRepository.kt` | `setProgress`; every state write keeps `progress` in step |
| 5 | `screen/diary/DiaryViewModel.kt`, `DiaryState.kt`, `DiaryScreen.kt`, `plan/PlanSection.kt` | `PlanRow.progress`; the drag, the line, the percent, the semantics |

## Decisions

1. **Opening at the top.** The cause was the list's keyed bottom spacer. Before the state is built, it was the only item, so when the cards arrived above it, the list kept it in view. Now the list is drawn only once the state exists. A new view model (after midnight, or on reopening the tab) starts at the top too.
2. **Sticker sizes.** In the calendar cell, 26dp centred for any earned badge (the icon scales with it), with the day number hidden and still in the description. "Recharging day" stays 20dp at `offset(11.dp, 11.dp)` with the number showing. Today keeps its outline either way.
3. **The close rule is one function.** `Points.closes(earned, target, items, done)` is `(target > 0 && isFull(earned, target)) || (items >= 6 && done >= items - 2)`.
   - **Items** are rows that didn't move away (`state != MOVED`), of scored missions, in that pillar and week (or on that day). For a pillar's week, items are the larger of those rows and the times its active missions were due that week at their current pace (`Points.occurrencesInWeek`), so a ring can't close on the few rows that exist mid-week.
   - **Done** is `countsAsDone`.
   - A week's rows are its stored rows, finished or not.
   - An away week never closes.
   - A pillar with target 0 is already drawn full, so it isn't affected.
4. **"Nearly"** is `closed && !full`. A pillar ring's state word is "away", "full", "nearly" or "light", and its description uses the same word.
5. **Closed day rings** are for finished days only (`date < today`, with a ring). The rule runs over the day's earned points against earned plus missed, less what moved away (moved items went to another day, so they don't hold the day back), and its scored items. `notDone` stays as the total. `missed` splits it by pillar, so a closed ring draws each pillar's missed part in its light color, in pillar order after the earned slices. An open ring keeps white.
6. **Patterns.**
   - **Lines.** Each line is a `List<LinePart>`; titles sit on chips (spec 7.1). Order, rules and wording are as in spec 9.2.
   - **Families.**
     - **Missions going well:** reuses the mission lines' full-week run (`PaceOffers.full`, since `paceSince`, away weeks skipped). It's shared rather than copied: the mission lines' run is the source.
     - **Strong** and **picking up:** each is one line naming every pillar that qualifies ("⟨Life⟩ and ⟨Health⟩ have been strong this past month."). Strong leaves out a pillar that has a run line, and is dropped entirely while the balanced run is 3 or more, since every pillar would then be strong.
     - **Best week:** "in N weeks" counts calendar weeks from the nearest earlier week with as many points or more to last week, so away weeks inside the span are included. It needs 4 or more counted weeks beaten. When the walk reaches the run cap, the older stored weeks are checked too; it reads "yet" only when no stored week has as many points.
     - **Badge run:** uses `DayBadgeBook.counts(date)`, which is true when the day has something done by hand, a did-some, or a milestone (the fallback's own test), so no badges are worked out for it. The Metrics book's range grows to cover `today − HISTORY_DAYS` up to today. A run at the look-back's edge just stops there.
   - **Folding.** The tab shows the first 4 lines and a "More patterns" chip that shows the rest ("Fewer patterns" folds them again). The weekday line no longer keeps a reserved slot, since nothing is dropped.
7. **The All months filter.** `CalendarFilter(pillars, missionIds)`. An item counts when its mission's pillar is picked or its mission is picked; both empty means the filter is off.
   - **With a filter on,** a day's ring is sized by items: one slice per counted item done or done in part, in its pillar's strong color (Recharge too), with white for counted items that didn't happen (skipped, or left planned on an earlier day). There's no closing, no badges and no crown.
   - **The summary** covers every month shown, away weeks left out: "⟨A⟩ and ⟨B⟩: done 23 times, 4 in part". Every pick is named on a chip while that makes two chips at most. Otherwise 3 or more pillars are counted ("3 pillars"), missions beside a picked pillar are counted ("⟨Health⟩ and 2 missions: …"), and missions alone are named up to 2. With nothing done: "⟨A⟩: nothing done yet". With no filter: "Everything counts." It and the caption are the list's first items, so they sit just under this month; only the back arrow and the chips are pinned.
8. **All months data.**
   - **Reads.** Tasks and plans from the first stored day's month grid, less `HISTORY_DAYS` (for due dates and badges), to the end of this week.
   - **Months.** From the first stored day's month to this month, built once per change on `Dispatchers.Default` and listed newest first in a `reverseLayout` list, so it opens on this month at the bottom. The tab bar is the Scaffold's own bar, outside the content, so the list needs only a small bottom padding. The unfiltered months, the indexes and the badge book are built per data change; a filter toggle only reruns the filtered grid.
   - **Badges.** One badge book covers the whole span.
   - **Lifetime.** The view model is made when the screen opens and cancelled when it closes, as the Metrics one is.
9. **Back** uses precompose's `BackHandler`, as the mission editor does. "Open this day" and a day tap with a filter on both go to the diary, which leaves Metrics and so closes the overlay.
10. **Progress values.**
    - **Column.** `TodayTask.progress` is an `Int` from 0 to 100, `@ColumnInfo(name = "task_progress", defaultValue = "0")`.
    - **Shown percent.** DONE shows 100. PARTIAL shows its `progress` when it's 1–99, else 50, which covers rows saved before plan 6. Anything else shows 0.
    - **Saving.** `PlanRepository.setProgress(taskId, percent)` rounds to 5% steps and ignores a MOVED item. At 100 it saves DONE; at 1–99, PARTIAL with that progress; at 0, PLANNED, or SKIPPED on an earlier day (as unticking does). Points come from the state, as now.
    - **Every other write.** Each state change sets progress to match: DONE 100, PARTIAL 50 unless given, anything else 0. This covers `withState` and any direct `copy(state = …)`: ticking, the evening check's ticks, moves, skips, take-backs and adding on an earlier day.
11. **The drag.**
    - **Gesture.** `detectHorizontalDragGestures` on the whole row, off for MOVED items and in edit (swap) mode. The vertical list scroll and the long-press crown are unaffected.
    - **Line.** It starts at the title's left edge (after the 44dp box and padding) and spans the rest of the row's width.
    - **While dragging,** the points pill shows "40%". Letting go commits only if the value changed. A did-some keeps its line, with "Did some · 40%" on the subline.
    - **Screen readers** get `progressBarRangeInfo` (19 steps) and `setProgress`, so the value can be set without dragging.

## Tasks

Each task ends with `:androidApp:assembleDebug` and `:shared:compileKotlinIosSimulatorArm64` passing, then one commit. Gradle is `/Users/aakarshika/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle`, because `./gradlew` is broken. There are no automated tests: the owner tests by hand.

### Task 1: Opens at the top; stickers over the number
- `MetricsScreen`: render the `LazyColumn`, its bottom spacer included, only when the state isn't null. Give it a `rememberLazyListState()` keyed on the view model.
- `MonthCalendar.DayCell`: per decision 2. Check how `BadgeSticker` sizes its icon; it should scale with the sticker's size.
- Update the cell's KDoc. Commit: `fix: Metrics opens at the top; stickers sit over the day number`.

### Task 2: Closed rings
- Decisions 3–5.
- `PillarRings`: draw the strong arc for `share` (capped at 1). When closed and `share < 1`, draw the rest of the circle from the arc's end in `pillarLight`. The state word and the description follow decision 4.
- `DayRing`: draw `missed` slices in `pillarLight` instead of white when the day is closed.
- The calendar caption reads: "Ring slices are sized by points. White is what didn't happen, or a light shade once the day was close enough."
- Commit: `feat: rings close in a light shade once they're close enough`.

### Task 3: More patterns
- Decision 6, with spec 9.2's Patterns list as the source for each rule and line.
- Change `MetricsUiState.patterns` to `List<List<LinePart>>`. Render each with `ChipText` on the ruled line.
- Remove `MAX_LINES` and keep per-family caps: 2 pillar run lines, 1 line each for missions, strong, picking up, weekends, best week and the badge run, and 2 weekday lines.
- Commit: `feat: more patterns: missions going well, strong pillars, weekends, best week, badge runs`.

### Task 4: All months
- Decisions 7–9 and spec 9.4.
- `MonthGrid.build` takes `selected: LocalDate?` in place of comparing to the shown week, and `filter: CalendarFilter?`.
- `MonthCalendarCard` takes an optional `onAllMonths` (the chip in the title row; tapping the title does the same) and an optional caption.
- `AllMonthsScreen` has three parts:
  - the top bar, with a back arrow "Back to Metrics" and "ALL MONTHS";
  - the filter chips, pinned (`SmallChip` with `selected`); the summary line and the caption are the months list's first items, so they sit just under this month;
  - the months.
- `MissionFilterDialog`: missions grouped by pillar in pillar order, active first, then archived marked "archived". Each row is a 44dp checkbox row, with "Clear" and "Done".
- Commit: `feat: All months: every month on one screen, filtered by pillars or missions`.

### Task 5: Swipe to fill
- Decisions 10–11 and spec 6.5.
- Version 7. Build once so Room writes `shared/schemas/com.example.todoapp.db.AppDatabase/7.json`, and commit it.
- Grep for every write of a task's state (`withState`, `copy(state`, `TaskState.DONE.name` and so on) and keep `progress` in step.
- `DiaryViewModel.setProgress` goes through the same guarded `act {}` as the other actions.
- Commit: `feat: swipe across an item to record how much got done (db v7)`.

## Manual hand-test checklist

### Before you start
- Install over the Plan 5 build without clearing storage. The app moves to version 7 on its own, and everything stays.
- For the pattern and calendar checks, use the seed as in Plan 5 (`SEED_SAMPLE_MISSIONS` and `SEED_METRICS_HISTORY` on, storage cleared). Don't commit the flags.
- Keep a screen reader handy for the checks marked "Screen reader".

### A. Opening at the top
1. Open Metrics from the Diary. It shows the week's header at the top, not the bottom.
2. Scroll down, go to Diary, come back. It opens at the top again.

### B. Stickers
1. A past day with an earned badge shows one 26dp sticker in the middle of its ring, and no number. The ring shows around it.
2. A quiet day ("Recharging day") shows its number, with a small moon sticker on the lower right.
3. Later days show their numbers. Today shows its number until it earns a badge, then wears the sticker. Its outline stays either way.
4. Days from the month before or after keep their faded number; any badge is a small faded sticker in the corner.
5. Screen reader: a day still reads its date, points and badges.

### C. Closed rings
1. Pillar ring at 80% or more: strong arc, then the rest of the ring in the light color. It reads "full". At 100% it's all strong.
2. A finished week where a pillar had 6 or more items due, all but 2 done, under 80% of its points: the ring closes in the light color and reads "nearly". 5 due with 3 done: it doesn't close. Mid-week, the ring doesn't close on the few items done so far: "all but 2" counts what's due this week.
3. An away week's rings stay empty, with "–".
4. A finished day with most of its points done: the missed part shows in light pillar colors instead of white. A day below the rule keeps white. Today never closes. A day where everything left was done and one item moved away closes.
5. Screen reader: the ring says "nearly" where it shows; a closed day's description includes "Ring closed.", before its badges.

### D. Patterns
1. Only lines the data holds up appear, in spec 9.2's order. With more than 4, "More patterns" shows the rest and "Fewer patterns" folds them.
2. Missions and pillars sit on chips in their pillar's color.
3. With the seed, Gym (ticked every day) reads "⟨Gym⟩ is going well.", or "… are going well." with another. A mission becomes eligible once it's been full 3 finished weeks running since its start or last pace change. A fixed block never does.
4. "N weekends in a row with 2 or more things done." shows once 2 or more weekends in a row each had 2 or more things done by hand. The seed already gives several, so expect a number above 2 there.
5. "N days running with a badge besides Recharging day." shows once 5 or more days in a row, up to today (or yesterday), had something done by hand. With the seed's daily Gym, N is about a month.
6. An empty install reads "Nothing stands out yet."

### E. All months
1. The calendar card has an "All months" chip. It, and the month title, open a full screen with this month at the bottom and older months above, back to the first month with data.
2. Days look as on the Metrics calendar: rings, closed rings, crowns, stickers. No week is tinted.
3. The back arrow (and Android's system back) returns to Metrics, where you left it.
4. On opening, this month shows whole at the bottom, with "Everything counts." and the caption under it. Pick Health: rings show only Health items, sized by items, and stickers and crowns go. The line reads "⟨Health⟩: done N times, M in part".
5. Pick Recharge: Recharge items show as grey slices.
6. Missions → pick two from different pillars → Done. The chip reads "2 missions", the rings show only those missions' items, and the line names both. With Health on too, Health's items count as well and the line reads "⟨Health⟩ and 2 missions: …".
7. At the largest font size, the chips wrap but the months still have most of the screen. On the Metrics card the month title and the "All months" chip don't overlap.
8. An archived mission is in the list, marked "archived", and its old days show.
9. With a filter on, tapping a day opens it in the diary. With none, a day with badges opens the badge list.
10. Clear every filter: stickers, crowns and point-sized rings come back.

### F. Swipe to fill
1. Drag right across a planned item. A thin line in its pillar color runs along the row's bottom line and ends under your finger, and the points pill shows the percent in 5% steps. One stroke from the title to the row's right end reaches 100%.
2. Let go at 40%. The subline reads "Did some · 40%", the pill "+1 of 2" (for a 2-point mission), and the line stays. The box stays open, with no strike-through.
3. Drag it on to the end. It ticks, strikes through and earns full points. The line goes.
4. Drag a ticked item back to 0. It's unticked (on an earlier day, skipped). Drag back up to 30% and it's a did-some again.
5. The evening check's "Did some" shows the line at 50% ("Did some · 50%"). Tapping a did-some's box ticks it. Ticked and unticked rows show no line; a screen reader reads them as 100% and 0%.
6. A moved item doesn't drag. In swap mode, rows don't drag.
7. A fixed block "Ticked for you", dragged to 60%: "Ticked for you" goes, and it's a did-some.
8. The list still scrolls up and down normally, and long-press still crowns. A short sideways slip (under about 24dp) saves nothing. A press held still before sliding crowns instead of dragging.
9. Week bars, rings and badges count a dragged did-some as half points.
10. Screen reader: the row reads as one item with its percent. TalkBack adjusts it up and down in 5% steps; on both, the "5% more" and "5% less" actions work, and "Crown" crowns it. VoiceOver reads the percent too.

## Review fixes

Each task had its own review, and the fixes changed some of the decisions above. The spec and the checklist now say what was built.

| Commit | What it fixed |
|---|---|
| `55c459c` | **Tasks 1–2.** <br>• A pillar's "all but 2" counts the times its missions were due that week when that's more than the rows, so a ring doesn't close mid-week or in a week opened on a few days only.<br>• Days from the next or previous month keep their faded number, with a faded corner sticker.<br>• Moved items stay out of a day's close rule.<br>• "Ring closed." is its own sentence.<br>• A closed ring's strong arc has square ends, so the light part shows near 100%.<br>• `PillarScore` has no silent defaults. |
| `66679ed` | **Task 5.**<br>• The line ends under the finger, so one stroke fills the row.<br>• A did-some looks open (no tick, no strike-through), and tapping its box ticks it.<br>• VoiceOver hears the percent, and "5% more", "5% less" and "Crown" actions work on both platforms.<br>• A retry after a failed save isn't dropped.<br>• The row is one screen-reader item in every mode. |
| `7f47d91` | **Task 3.**<br>• Each mission's full-week run is worked out once per build, and `PaceOffers.full` reads only its week through a per-mission index in `History`.<br>• "Strong" looks at the last 6 weeks and "picking up" at the last 8, so a monthly pillar can't read "strong this past month" from a season of weeks.<br>• One line per pillar.<br>• A missing summary row ends the walk.<br>• "Going well" needs the mission live today.<br>• "Best yet" checks every stored week.<br>• Mission runs stop at the mission's start and the first stored week; before, a two-week-old install read "full every week for a year or more".<br>• Shared wording helpers are in `domain/LineWords.kt`.<br>• One failed build no longer freezes Metrics or All months. |
| `972c52b` | **Task 4.**<br>• Only the back arrow and the chips are pinned; the summary and caption sit under this month, and the bottom padding is 16dp, so this month fits on opening.<br>• New summary naming rule (decision 7), with "Everything counts." when no filter is on.<br>• A filter toggle reuses the unfiltered months and the badge book.<br>• Away weeks stay out of the summary.<br>• The Metrics card's title is centred.<br>• Spoken dates carry the year when it isn't this year.<br>• Action chips aren't presented as selectable. |
| `3ea1120` | **Final review.**<br>• A block done in part by hand earns "Did some", so `on()` and `counts()` agree, and a day like that is never "Recharging day".<br>• The Metrics card's title and "All months" chip can't overlap.<br>• Opening All months is announced, and "More patterns" says whether it's expanded.<br>• A sideways slip under 24dp saves nothing.<br>• Spoken dates follow the titles' year rule.<br>• Seeds store progress; Metrics' bottom spacer is 24dp. |

Left for the hand test:
- A sideways slip of more than 24dp during a scroll still records a drag.
- A press held still before sliding crowns instead.
- The summary's live region is only read while it's on screen.
