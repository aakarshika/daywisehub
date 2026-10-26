# Plan 5: Daily badges

**Goal:** Every finished day earns at least one badge, so the Metrics calendar always looks generous. A day with nothing done earns only "Recharging day": it sounds kind to a casual user, and a regular one learns it marks a gap, without the app ever saying "missed". Real rest gets its own badge ("Rested on purpose"), and blocks the evening check ticked don't count, so a work block alone never makes a weekday look active.

**Spec:** `docs/superpowers/specs/2026-10-25-daywisehub-redesign-design.md`, section 9.3 (added by this plan), with sections 5, 7 and 9.2. The approved design brief is the source for the badge list, their order and the sticker look.

**Architecture:** Badges are read-only and never stored. There's no table, no schema change and no write: nothing here takes `PlanRepository`'s lock.
- **The rules.** `domain/DayBadges.kt` is plain Kotlin: `Badge` (rank order, with a `BadgeTier`), `EarnedBadge(badge, reason: List<LinePart>)`, `BadgeInput` and `DayBadges.book(input)`. The book indexes tasks, plans and milestones once, then `on(date)` answers any day in its range, rarest first.
- **Metrics.** `MetricsStateBuilder` makes one book for the shown month's days (`MonthGrid.span`) and hands it to `MonthGrid.build`. `CalendarDay` gains `badges` and `title`; its description names the badges. `MonthCalendarCard` draws the rarest badge as a sticker on the ring, and `MetricsScreen` holds the badge dialog.
- **Diary.** `DiaryStateBuilder` makes a book for the page's day only. `DiaryUiState.badges` feeds a row under the evening day card.
- **Reads added.** `TodayTaskDao.observeFirstDate()` (where badges start, as it changes) and `MilestoneDao.observeIncludingArchived()` (so an archived finish line keeps its badges). Both are plain queries; `shared/schemas` doesn't change. `MetricsViewModel` now takes `PlanRepository` for `observePlans` (a plain read, for each day's energy).
- **The look.** `screen/metrics/badges/BadgeIcons.kt` draws 31 icons as SVG path data on a 24-unit grid (only `material-icons-core` is cached). `BadgeStickers.kt` has the sticker, the dialog and the diary row. Colors are in `AppColors.kt`.

## Files

| File | Change |
|---|---|
| `domain/DayBadges.kt` | New: badges, the input, the one-pass book |
| `domain/MonthGrid.kt` | `span(week)`; `build` takes the book; `CalendarDay.badges`, `.title`; description names badges |
| `db/data/todotask/TodayTaskDao.kt`, `repo/TodayTaskRepository.kt` | `observeFirstDate()` |
| `db/data/mission/milestone/MilestoneDao.kt`, `repo/MilestoneRepository.kt` | `observeIncludingArchived()` |
| `screen/metrics/MetricsState.kt` | `MetricsInput.plans`, `.firstDay`; milestones include archived; builds the book |
| `screen/metrics/MetricsViewModel.kt`, `di/DI.kt` | Takes `PlanRepository`; the window carries plans and the first day |
| `screen/metrics/MonthCalendar.kt` | The sticker on each day; a tap hands the day to the screen |
| `screen/metrics/MetricsScreen.kt` | A day with badges opens the dialog; "Open this day" opens the diary |
| `screen/metrics/badges/BadgeIcons.kt`, `BadgeStickers.kt` | New: icons, sticker, dialog, diary row |
| `screen/basicutils/AppColors.kt` | Sticker fills, rim and ink |
| `screen/diary/DiaryState.kt`, `DiaryViewModel.kt`, `DiaryScreen.kt` | `badges` on the page; `BadgeExtras`; the row under the day card |

`NavScreens` is unchanged: `openDay` already selects the date and opens the diary, which is what "Open this day" does.

## Decisions

The brief is followed as written except where noted.

1. **Done by hand** is a DONE item that the evening check didn't tick (`autoTicked` false). A block ticked by hand counts. For the fallback, a did-some also counts as something done, so a day with only a did-some earns "Did some" rather than "Recharging day" (otherwise "Did some" could only ever appear beside a tick). Every other badge counts DONE only unless it says so.
2. **Runs and personal best count did-some days (changed from the brief).** The brief says "done by hand". The app's streak everywhere (the STREAK reason line "Every day · N-day streak", the Missions status and the Metrics best streak) counts did-somes, so a "7-day run" that ignored them would disagree with the streak the diary just showed.
3. **The return and the re-return.**
   - They apply to missions the planner repeats on an interval of 28 days or more (`repeatsByInterval` and `intervalDays() >= 28`): rhythms of 4 weeks or more, and habits once a month. A habit twice a month is a 15-day cycle by the planner's own rule, so it doesn't qualify (the brief lists "a habit with period MONTH"). Recharge never counts as missed, so it has no return.
   - The gap runs from the last time it was done or done in part, as its due date does, else from its start. Cycles missed = `gap / cycle − 1`.
   - When neither the last time nor the start is inside the 120-day look-back, the gap is known only to be longer than 120 days. That is still certainly a re-return when even 121 days misses 2 cycles (cycles up to 40 days); otherwise no badge, since it can't be told.
4. **Milestones don't make a day count.** "Done by hand" reads items only, as the brief defines it, so a finished day whose only act was ticking a milestone gets "Recharging day", and today shows nothing until an item is done. This keeps a day's badges the same before and after midnight.
5. **Archived missions keep their badges.** Their items count everywhere, as their points do, and their milestones are read too (the new `observeIncludingArchived`). Mission lines still read only active missions' milestones.
6. **The first day** comes from a live `MIN(task_date)`. Adding an item on an earlier day with the pencil moves it back.
7. **The look-back and the crown count are inputs.** `BadgeInput.lookBackDays` and `.maxCrowns` take `PlanRepository.HISTORY_DAYS` and `PlanRepository.MAX_CROWNS` at both call sites, so `domain` doesn't import `repo`.
8. **Literal readings kept.** "Halfway" and "Full enough" both show at 80% or more. "We'll count it" and "Clean sweep" count every pick, Recharge picks included (only the percent badges are limited to scored pillars). "First one" shows beside "Hat trick".
9. **Several missions in one badge.** One or two sit on chips ("Did ⟨Run⟩ and ⟨Swim⟩ after a move"); three or more are counted ("Did 3 things after a move"). A return or personal best names the mission with the longest gap or run.
10. **Week badges.** "Pillar filled" names every pillar that first filled that day ("Health and Life full for the week"). A pillar with a target of 0 is full from the start, so it never "fills". "Week maker" reads "…, a balanced week" on a finished week and "… so far" on this one. Both need something done by hand that day (the fallback).
11. **Personal best** fires when the run is exactly one day longer than the longest earlier run in the look-back. A record run that sits right at the 120-day edge can shift by a day as the window slides; both screens use the same window, so they still agree.
12. **The diary row** follows the day card: no card, no row. A page with no plan and no items (a day the app wasn't opened) shows no card, so its "Recharging day" shows only on the calendar. Each sticker's line is its content description rather than a tap, the simpler accessible choice the brief offered; the calendar's dialog shows the lines on screen.
13. **Sizes.** Calendar sticker 20dp (icon 14dp), offset 12.5dp down and right from the cell's centre so it sits on the ring's lower right, clear of the day number; the cell stays 44dp. Dialog 32dp, diary 36dp. Tilt is −7° to 7°, from the date and the badge.
14. **Contrast.** The ink `#3B3226` is 6.3:1 on the lilac fill (the darkest) and 9:1 or more on the others.

## Keeping it fast

- One pass over the tasks indexes them by day and records done days per mission and the days with something done by hand. Tasks outside `[from − look-back, end of to's week]` are skipped by comparing their stored yyyy-MM-dd text, without parsing.
- Each task's date is parsed once. Plans outside the range are skipped the same way.
- Week totals and targets are worked out once per week, the first time one of its days is asked for. A finished week's target is read from its summary, so the live `Points.weeklyTarget` runs only for this week.
- Runs and quiet days walk at most 120 days per daily habit done that day, with set lookups.
- The calendar builds one book per emission for its 42 days. The diary builds one for its single day.

## Manual hand-test checklist

### Before you start
- Install over the previous build without clearing storage: there's no schema change.
- **The seed.** Set `SEED_SAMPLE_MISSIONS = true` (`db/SampleData.kt`) and `SEED_METRICS_HISTORY = true` (`db/MetricsSample.kt`), clear storage, open the app on the Diary, then tap Metrics. Don't commit the flags.
  - "N weeks back" counts from this week, as in Plan 4's checklist. "1 week back" is last week.
  - **Two seed quirks.** The seed's past work blocks are stored without "Ticked for you", so they count as ticked by hand: seeded weekdays look busier than real ones. Most seeded items are stored as "Added by you", so "Bonus round" is common in the seeded weeks.
- A screen reader (TalkBack or VoiceOver) for the checks marked "Screen reader".

### A. Recharging day (seed)
1. Step back about 7 to 12 weeks. Every day from 88 days back to 42 days back has a grey-lilac moon sticker. Most of them are in away weeks, with no ring; the weeks holding the ticks 89 and 41 days back have thin plain rings. The dialog says "Recharging day" and "A quiet day".
2. 89 days back (Plan winter trip's tick) has a peach "Full enough" sticker. Days before it have no sticker.
3. Future days never have a sticker.

### B. Today, and blocks ticked for you (seed, on a weekday, before ticking anything)
1. Today has no sticker, and tapping it opens the diary at once.
2. In the diary, open the evening check. The blocks show "Ticked for you", and there's no "TODAY SO FAR" row: blocks the app ticked don't count. Today still has no sticker on Metrics.
3. Tick Gym. "TODAY SO FAR" appears under the day card with "First one" ("1 done") and "Two in a row" (Gym was done yesterday), and today's cell on Metrics gets a sticker.

### C. The calendar sticker and the dialog
1. Each day with badges wears its rarest one as a small round sticker on the ring's lower right. It doesn't cover the day number, and it shows on away weeks' days too, which have no ring.
2. Fills: cream (everyday), peach (good day), lilac (rare), gold (rarest), and grey-lilac only for "Recharging day". Each sticker has a light rim, a soft shadow and a slight tilt that differs from day to day.
3. Tap a day with a sticker. A dialog titled with the day ("Tuesday, Oct 20", with the year when it isn't this week's year) lists every badge, rarest first, each with a 32dp sticker, its name and its line. Titles sit on chips in their pillar's color.
4. "Open this day" opens that date in the diary. "Close" and tapping outside close the dialog and stay on Metrics.
5. Tap a day without badges (a future day): the diary opens at once, as before.
6. Screen reader: a day reads its points, then its badges ("…. Badges: Clean sweep, All four, …"), and its action is "Show badges" ("Open in the diary" without badges). The dialog title is a heading, and each badge reads as one item: "Clean sweep, All 3 picks done".

### D. The diary row
1. Open any seeded past weekday's page. Under the day card there's a "BADGES" row: 36dp stickers with their names underneath, rarest first, wrapping onto a second line when needed.
2. Today's row reads "TODAY SO FAR" and shows only once something is done (B.3).
3. The row matches the calendar's dialog for the same day, badge for badge.
4. Screen reader: "BADGES" is a heading, and each sticker reads its name and line ("Hat trick. 4 done in one day.").

### E. Specific badges (seed)
Some days move with the weekday you seeded on, because Call Grandma's three ticks (13, 27 and 41 days back) land on different weekdays.
1. **Hat trick.** Tuesday 1 week back lists "4 done in one day" (Job, App, Gym, Call sister) and "Clean sweep": "Both picks done". Its top sticker is "Pillar filled": "Love full for the week".
2. **Clean sweep.** Every seeded Monday (1 to 5 weeks back) lists "All 3 picks done" (Gym, Call Mom, Guitar; 4 on a Monday Call Grandma also landed on), with "All four" and "Strong start".
3. **Back at it.** 41 days back (Call Grandma, the first tick after the moons) lists "Back after 47 quiet days". Seeded on a Monday, Tuesday or Wednesday, the Monday 5 weeks back also lists "Back after 5 quiet days" (4 on a Tuesday, 3 on a Wednesday).
4. **7-day run.** Wednesday 4 weeks back has a lilac flame on top: "⟨Gym⟩ 7 days running". Gym's run starts the Thursday 5 weeks back, after the seed's one gap.
5. **30-day run.** Friday 1 week back has a gold flame in a ring: "⟨Gym⟩ 30 days running".
6. **Week maker and Pillar filled.** On the crowned weeks (2 and 4 back), the gold big crown reads "All 4 pillars full, a balanced week". It's on Saturday, when Dance class fills Life and Gym fills Health; seeded on a Saturday, Call Grandma's tick lands on those Sundays and the crown moves there with Love. Saturday lists "Pillar filled" naming Health and Life, and Thursday's names Wealth (with Love on some seeds).
7. **Second chance.** Wednesday 3 weeks back: "Did ⟨Call sister⟩ after a move".
8. **Heavy lifting and Weekend warrior.** Saturdays 1 to 4 weeks back: "Did ⟨Dance class⟩, which feels draining", and "2 done on a Saturday" (3 where Call Grandma landed that day).

### F. Personal best and We'll count it (seed, by hand)
1. In the diary, open the Monday 4 weeks back and untick Gym (it becomes skipped).
2. That Monday now lists "We'll count it": "2 of 3 done", and no "Clean sweep".
3. Saturday 4 weeks back now has the gold trophy on top: "⟨Gym⟩ 5 days running, past its best of 4" (the earlier run is Thursday to Sunday 5 weeks back).
4. The 7-day run moves to the Monday 3 weeks back. Last Friday loses its 30-day run, which moves to this Wednesday once that's past.
5. Tick Gym again on that Monday to put the seed back.

### G. Other badges (by hand)
1. **Did some.** In the evening check, "Did some" on a pick: "Did some of ⟨it⟩".
2. **Rested on purpose.** Add New episode with the pencil and tick it: "Made time for ⟨New episode⟩".
3. **Crowned and All three crowns.** Crown 3 picks and tick them all: "3 crowned and done" and "All 3 crowned done". With one crowned: "⟨it⟩ was crowned and done".
4. **Gentle win.** Tap "Lighter day", then tick at least 80% of the picks' points: for example "A low-energy day, 4 of 5 points".
5. **Milestone and Finish line.** Tick a finish line's milestone in the diary: "Milestone 1 of 3 for ⟨it⟩". Ticking its last one adds the gold checkered flag: "⟨it⟩ · All 3 milestones done". Archive that mission: the day keeps both badges. A day whose only act was ticking a milestone stays "Recharging day" (decision 4).
6. Nothing anywhere uses "missed", "failed" or an exclamation mark, and singulars read right ("1 done", "1 of 1 point").

### H. Date travel
Change the device date as in Plan 2's checklist (Android: turn off automatic time; the iOS simulator follows the Mac's clock), reopen the app, and only move forward. Do this after the seed checks above: it shifts which days are "N weeks back".
1. **A block alone doesn't count.** On a weekday, open today's evening check so the blocks are ticked for you, and tick nothing else. Move the date one day ahead and reopen. That day has the moon on Metrics, and its diary page shows its card with a "BADGES" row holding only "Recharging day". On that page, untick a block and tick it again (now by hand): the row becomes "First one" ("1 done").
2. **The return.** Tick a once-a-month habit, move the date about 60 days on and tick it again: "⟨it⟩ done for the first time in 60 days". After 90 days or more it's "The re-return".

### I. Agreement and speed
1. For any day in the last 120 days, the diary row and the calendar dialog list the same badges.
2. With the seed, Metrics opens and steps weeks without a visible pause, and ticking in the diary feels as quick as before.

### J. iOS
`:shared:compileKotlinIosSimulatorArm64` and `:shared:linkDebugFrameworkIosSimulatorArm64` pass. On the simulator, the stickers and the dialog look as on Android.

## What comes next
- A seed day with a real "Ticked for you" block, and a seeded personal best, would let A–F run without hand steps.
- If the owner prefers a tap to reveal a sticker's line in the diary, the row can grow a reason line under it.
- Monthly habits twice a month could get returns on a 30-day cycle, if wanted (decision 3).
