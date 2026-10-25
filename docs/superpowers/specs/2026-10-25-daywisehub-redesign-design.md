# DayWiseHub redesign: a self-adjusting week across four pillars

Date: 2026-10-25
Status: draft for review
Mockups: https://claude.ai/artifact/XNa7TUV8E3nrtjjhx7eNZs (private canvas, 10 screens)

## 1. Goal

DayWiseHub should help one person keep many commitments alive at once (job, side project, gym, calls to relatives, learning guitar, a vacation every few months, a new episode to unwind) without the list becoming a burden.

The app already has the right bones: pillars, missions, a daily to-do list, a planner and a metrics calendar. What it lacks is a way to show the idea behind them. This redesign makes three things visible and calm:

1. **Balance.** Every mission feeds one pillar. Each pillar has a weekly target, and you can see how full each one is.
2. **Self-correction.** Anything missed is moved, slowed down or dropped without guilt. Next week adjusts on its own.
3. **Reward.** Points per pillar, a balanced-week crown, streaks and milestone flags, in the diary's existing hand-drawn style.

## 2. Core concepts

| Concept | Meaning |
|---|---|
| Pillar | One of four fixed life areas: Health, Wealth, Love, Life. Pillars are not user-editable in v1. |
| Mission | Something you want to make room for. Belongs to exactly one pillar (or to Recharge, which is not a pillar). |
| Mission kind | How the mission repeats: habit, finish line, fixed block, every so often, recharge (section 3). |
| Weight | How big a part of its pillar's week the mission is: small = 1 point, solid = 2, big = 3, earned each time it's done. |
| Week | Monday 00:00 to Sunday 23:59, local time. The only cycle the app adjusts on. |
| Pillar target | The sum of points of everything due in that pillar this week. Computed, never set by hand. |
| Free hours | Time per day outside fixed blocks that the planner may fill. Set per weekday, adjustable for today. |
| Recharge | Missions that protect mental health (a new episode). They earn no points and never count as missed. |

## 3. Mission kinds

The user never picks a kind from a technical list. It comes from question 3 of the creation sentence ("What kind of thing is it?").

| Kind | Creation answer | Repeats as | Examples | Question 4 asks |
|---|---|---|---|---|
| Habit | Something I keep doing | N times per day, week or month, forever | Gym every day, guitar 3× a week | "About how often?" |
| Finish line | Something with a finish line | N times per week until a date or until milestones are done | Learn a dance routine by spring | "By when? How will you know you're done?" (answers become milestones) |
| Fixed block | A fixed block in my day | Set weekdays and a time window | Job Mon–Fri 9–1, app Mon–Fri 2–5, housekeeping Saturdays | "Which days, and when?" |
| Every so often | Something I do every so often | Once every N days, weeks or months | Call Grandma every 2 weeks, plan a vacation every 3 months | "Every how long?" |
| Recharge | Something that recharges me | About N times a week, never required | New episode about 2× a week | "How often do you need it?" |

## 4. Creating a mission

There are three ways in, all from the + button on the Missions screen, which opens the ideas library (section 8):

1. **Quick add** an idea from the library.
2. **Edit and add** an idea, which opens the review screen pre-filled.
3. **Write your own.** You type a title, the app suggests matching ideas, and either fills the rest from one or walks through the nine questions.

### 4.1 The sentence

The creation form is a sentence written on the diary's ruled lines. Each answer fills a blank, highlighted in the pillar's color:

> I want to ⟨learn guitar⟩ for my ⟨Life⟩. It's something I ⟨keep doing⟩, about ⟨3 times a week⟩, ⟨30 min⟩ each, and it feels ⟨steady⟩. It's a ⟨big⟩ part of my Life week. If a day goes sideways, ⟨move it⟩.

| Step | Question | Answers |
|---|---|---|
| 1 | What do you want to make room for? | Free text (becomes the title) |
| 2 | Which part of your life does it feed? | Health, Wealth, Love, Life (skipped for Recharge) |
| 3 | What kind of thing is it? | The five kinds in section 3 |
| 4 | Depends on kind | See section 3 |
| 5 | Each time takes about… | 10, 15, 20, 30 or 45 min; 1, 1½, 2, 3 or 4 h; a full day |
| 6 | How does it feel to do? | Light, Steady, Draining |
| 7 | How big a part of your ⟨pillar⟩ week is it? | Small (1 pt), Solid (2 pts), Big (3 pts) (skipped for Recharge) |
| 8 | If a day goes sideways… | Move it to another day, It has to be that day, Skip it, no catch-up |
| 9 | It's new. Start gently? | Start at a lower pace and step up after 3 good weeks, or go straight to the full pace. Asked only for habits and finish lines with 2 or more sessions a week. |

Weight and effort are separate on purpose. Points come from weight; duration and effort decide whether the mission fits into a given day.

### 4.2 Before saving: what it costs your week

The last screen shows the full sentence, then:

- Each pillar's weekly target as a bar, with this mission's points as a striped segment ("Life 6 → 12").
- One line in plain words: "Guitar adds 1 hour and 6 points a week while you start gently. That's 9 points once you're at 3×."
- Free hours planned this week versus available ("29 of 35"), with the new mission striped.
- Buttons: Make it lighter, Save mission.

"Make it lighter" returns to the sentence with the pace blank selected.

## 5. Points model

| Rule | Decision |
|---|---|
| Points per time | Weight: small 1, solid 2, big 3. Recharge 0. |
| Pillar weekly target | Sum of points of every occurrence due in that pillar this week. |
| Fixed blocks | Earn points like any mission. They start ticked; the evening check lets you untick. |
| Slower than weekly | Counts only in the week it's due (Love can be 4 one week and 6 the next). |
| Faster than weekly intervals | Count once per interval (every 2 days counts 4 times in a week). |
| Short weeks | A weekly count is capped at the days the mission is live that week, so a mission starting Saturday can still be full. |
| Gentle start | The target uses the current pace, not the goal pace. |
| Pillar is "full" | At 80% of its target or more. A pillar with a target of 0 this week counts as full. |
| Balanced week | Every pillar full. Shown as a crown on the week. |
| Doing extra | Shown as "+N extra" in that pillar only. Extra never fills another pillar or next week. |
| Did some | Half the points, rounded up (a big mission done partly earns 2). |
| Finish-line milestones | No extra points. A milestone shows a flag and a celebration. |

### 5.1 End of week: no debt

At Monday 00:00 the week closes:

- **Points don't carry over** and next week's targets don't grow because of a miss.
- **Weekly quotas start fresh.** Guitar done 2 of 3 times is simply 3 again next week, and the planner favors it early in the week.
- **Due items stay due until done.** A missed every-so-often mission (Call Grandma) moves into next week's target.
- **"Has to be that day" and "skip it" items just drop.**
- A week summary is stored (per-pillar points and targets, balanced or not) for metrics and Monday's "Last week" card.

### 5.2 Pace changes

The app offers changes; it never makes them on its own.

| Trigger | Offer | Options |
|---|---|---|
| A habit or finish line got less than half its weekly count 3 weeks in a row | "⟨New ideas⟩ has slipped 3 weeks in a row. That's fine. Want to make it 2× a week for now? You can step back up anytime." | Lower by one step, keep it, pause a month |
| An every-so-often mission was late 3 times in a row | Same wording, lengthening the interval one step (2 weeks → 3 weeks) | Lengthen, keep, pause a month |
| A gently started mission had 3 full weeks | "⟨Guitar⟩ has gone well 3 weeks running. Ready for 3× a week?" | Step up, not yet |
| Any habit had 4 full weeks and the week still has free hours | "⟨Dance class⟩ has gone well 4 weeks running. Ready for 2 a week?" | Step up, not yet |

At most two offers appear per week, on Monday's diary, as sticky notes. "Not yet" suppresses the same offer for 4 weeks. Only an accepted offer changes the target.

## 6. Planner rules

The separate planner (the red, yellow and green "Critically lagging" list) is removed. The diary's to-do list arrives already picked each morning.

### 6.1 Inputs

- **Free hours per weekday,** in settings. Defaults: 3 h on weekdays, 10 h on Saturdays and Sundays (35 h a week).
- **Today's free hours and energy** (low, normal, high), adjustable from the diary's "change" link.
- **This week's progress** per mission and per pillar.
- **Moved items** targeted at today.

### 6.2 Building today's list

The list is built the first time the diary opens on a new day.

1. **Fixed blocks** for today go under "Already in your day." They don't use free hours.
2. **Candidates** are every other active mission that is eligible today:
   - Habits with a daily pace: always.
   - Habits with N per week: if this week's count (capped at its live days) is below N. A habit is held back for a preferred day still ahead this week unless it's needed today.
   - Finish lines: like habits, plus any milestone due within 7 days. Days left count to the finish line's end date when it ends this week.
   - Every so often, and habits counted per month: if due this week or overdue (due date = last done + interval). Intervals shorter than a week are only picked once actually due.
   - Recharge: if under its usual count this week, or if today has 6 or more hours of fixed blocks.
   - Anything moved to today is already on the list and uses its time first.
3. **Each candidate gets an urgency score.** Highest first:
   1. Moved to today.
   2. Overdue every-so-often missions.
   3. A daily streak that would break today.
   4. Weekly quotas where the remaining count divided by the remaining days is 1 or more, habits on one of their preferred days, and recharge on a heavy day.
   5. Finish lines with a milestone due within 7 days.
   6. Everything else.
   
   Within a score, the pillar furthest behind this week comes first, then the earliest due date, then the bigger weight.
4. **Fill:** take candidates in score order; a candidate that doesn't fit what's left is skipped and the next one is tried.
   - On a low-energy day, skip "draining" missions and use only 60% of free hours.
   - Don't pick the same weekly-quota mission two days in a row within a week, unless it's needed or preferred today.
   - Recharge picks count toward the time.
5. **Didn't fit:** the highest-scoring candidate with a due date this week that wasn't picked appears as a sticky note naming the next day with room ("Due by Dec 20. Saturday has room.").

### 6.3 Changing today's list

- **Swap one:** replaces a picked item with the next-best candidate that fits what's left of the day and today's energy, preferring the same pillar. If nothing fits, the item stays. Items moved in from another day can't be swapped, and something swapped out isn't offered again that day.
- **Lighter day:** sets energy to low and rebuilds the list.
- **Pencil button:** opens a manual list of all candidates grouped by pillar, each with its reason line.
- **Crowns:** you can crown up to 3 picks as today's priorities. This replaces the existing TOP3 tag; the crown icon and the "priorities done" highlight stay.

### 6.4 The evening check

It opens when the diary is first opened after 6 pm, and can be reached anytime from the to-do list.

- Fixed blocks start ticked ("Ticked for you").
- An unticked flexible item offers: Did some (half points), Move to ⟨next day with room⟩, Skip today. A skipped item can be ticked again if the skip was a mistake.
- Anything still unresolved at midnight follows its "if a day goes sideways" answer automatically.
- The day closes with the diary's highlights card: points today, how many pillars got something, and crowned items.

## 7. Reason lines and wording

### 7.1 The rule

**The app never reads meaning into a mission's title.** It doesn't know "Call Grandma" is a phone call. Reason lines are built only from stored data: kind, pace, last-done date, streak, this period's count, moved-from date, milestones, due date and today's fixed-block hours. The title is shown in bold above the reason, so the reason never repeats it.

When a title appears inside a sentence, it's shown as a chip in its pillar's color ("Love was light, so ⟨Call sister⟩ is set for Tuesday"). The chip doesn't have to fit the grammar, so any title the user types reads fine.

### 7.2 Templates

| Code | Situation | Line |
|---|---|---|
| STREAK | Daily habit | Every day · {n}-day streak |
| QUOTA | Behind on a weekly or monthly count | {done} of {target} this week · {days} days left |
| RHYTHM_DUE | Every so often, coming due | Last done {n} days ago · you aim for every {interval} |
| RHYTHM_OVERDUE | Every so often, overdue | Due {n} days ago |
| MOVED | Moved from another day | Moved from {weekday} |
| MILESTONE | Finish line | Milestone {i} of {n} · due {date} |
| RECHARGE_HEAVY | Recharge on a heavy day | {h} hours of fixed blocks today. This counts too. |
| RECHARGE_USUAL | Recharge otherwise | You usually take {n} breaks a week. |
| BLOCK | Fixed block | {start} to {end} |
| NO_FIT | Didn't fit today | Due by {date}. {weekday} has room. (Overdue: Overdue since {date}.) |
| PILLAR_LIGHT | Pillar well under target (fallback) | {pillar} is light this week |
| RHYTHM_FIRST | Every so often, never done yet | First one · you aim for every {interval} |
| ADDED | Added by hand from the pencil | Added by you |

Singular and edge forms: "last day" instead of "1 days left", "Last done yesterday", "Due yesterday", "Every day" when there's no streak yet, "You usually take one break a week.", and "Fixed block" when a block has no times. An unreadable stored reason renders as an empty line rather than an error.

`TodayTask` stores the code and its arguments, so the line can be redrawn in any language later.

### 7.3 Words to use and avoid

Use "moved," "slipped," "light," "not today? no problem," and "nothing to catch up on." Never use "lagging," "failed," "missed your habit," "critically," or exclamation marks in system copy.

## 8. Ideas library

**Deferred to the next pass.** This section is the agreed design. The first pass ships without the library: + on Missions opens the nine-question sentence directly, and `ideaId` is not added yet.

### 8.1 Content

The library is a curated, read-only list bundled with the app at `shared/src/commonMain/composeResources/files/ideas.json`. Version 1 has 119 ideas in 14 themes and is expected to grow to hundreds, so the screen relies on search, filters and themes rather than scrolling alone.

Themes, in display order: Move your body, Look after yourself, Calm your mind, Rest and recharge, Family, Friends, Partner and dating, Out in the world, Learn something, Make and play, Work and career, Money, Home and errands, Explore and travel.

Each idea has:

| Field | Meaning | Example |
|---|---|---|
| `id` | Stable kebab-case id, stored on missions as `ideaId` | `call-relative` |
| `title` | Shown as is. May hold one `{slot}` blank. | `Call a relative`, `Meet {who} for coffee` |
| `blank` | Only with a slot: `label` for the inline field, plus `examples` | `{ "label": "which language?", "examples": ["Spanish", "French"] }` |
| `theme` | One of the theme ids | `family` |
| `pillar` | HEALTH, WEALTH, LOVE, LIFE, or RECHARGE | `LOVE` |
| `kind` | HABIT, FINISH_LINE, FIXED_BLOCK, EVERY_SO_OFTEN, RECHARGE | `EVERY_SO_OFTEN` |
| `pace` | Shaped by kind, using the same names as `Mission`'s pace fields (section 10): `count` + `period` (+ `goalCount` for a gentle start), `intervalCount` + `intervalUnit`, or `weekDays` + `startTime` + `endTime` | `{ "intervalCount": 1, "intervalUnit": "MONTH" }` |
| `durationMinutes` | One of the step 5 choices | `20` |
| `effort` | LIGHT, STEADY, DRAINING | `LIGHT` |
| `weight` | 1–3, or 0 for recharge | `1` |
| `finish` | Finish lines only: default `weeks` and suggested `milestones` | `{ "weeks": 8, "milestones": ["Pick a routine", "…"] }` |
| `keywords` | Single lowercase words that should find this idea | `["phone", "ring", "family"]` |
| `groups` | Optional labelled keyword sets, used to explain a match | `{ "relative": ["uncle", "aunt", "cousin"] }` |

A gentle start is expressed by `goalCount` being higher than `count`. Six ideas use it: run, wind down, meditate, practice guitar, learn an instrument and learn a language.

A saved mission keeps an `ideaId` back to its source idea. Custom missions are never added to the library.

#### Curation rules for adding ideas

- **Mellow and common.** Something most people would recognize wanting, not an optimization project. Net-worth tracking, insurance reviews and retirement planning were left out for this reason.
- **An activity, not a task or a feeling.** "Debugging," "meetings" and "stress management" aren't missions. "Do a focus block," "Clear your inbox" and "Meditate" are.
- **Sub-steps become keywords, not ideas.** Flights, hotels, itineraries and packing are keywords of "Plan a getaway," not ideas of their own.
- **No duplicates of what the app already tracks.** Water intake has its own section, so there's no "drink water" idea.
- **Breadth over depth.** A new idea should add a kind of life the list doesn't cover yet, rather than a variant of an existing one. The exception is when the rhythm genuinely differs: "Take guitar lessons" (weekly class) and "Practice guitar" (at home, gentle start) are both kept.
- **Keep personal names out of titles.** Use a blank instead ("Check in with {who}").

### 8.2 Browsing

The screen has:

- A search box.
- A "Write your own" sticky note.
- Pillar filter chips: All, Health, Wealth, Love, Life, Recharge.
- Themed sections under marker headings.

Each row shows the title, a pillar chip and a one-line preset ("Every month · 20 min · small"), plus Edit and +.

- **+ (quick add)** saves the idea as is. A toast shows the cost: "Added. +7 Health points a week. Undo."
- **On an idea with a blank,** + opens the row inline: type the blank, pick how often from three chips, then Add. "More options" opens the review screen.
- **Edit** opens the review screen pre-filled.
- **Ideas already added** show a check and "Added."

### 8.3 Write your own: matching

As the title is typed (after 2 characters, with a 150 ms debounce), the top 4 ideas appear under the sentence.

1. Lowercase the input, split it into words and drop stop words (a, an, the, my, your, to, for, with, and, of, in, on, at, go, do, get, have, take, some, more, i, want, every, day).
2. Score each idea: 2 points for every typed word found in its title, keywords or groups.
3. While typing, if the last word matches no word anywhere in the library, it gets 1 point wherever it is the start of a word ("meditat" finds "Meditate"). A finished word like "clean" never prefix-matches "cleaning" elsewhere.
4. Show the top 4 ideas scoring above 0.
   - The top one is tagged "Best match" if it leads the second by at least 2 points, or is the only result.
   - When it matched through a group, the card explains why ("Uncle counts as a relative").

Checked against version 1 of the bank:

| Typed | Best match |
|---|---|
| call uncle raj | Call a relative ("Uncle counts as a relative") |
| aws cert study | Study for {exam} |
| learn spanish | Learn {language} |
| mom | Call your parents ("Mom counts as a parent") |
| guitar class | Take guitar lessons |
| netflix | Watch a new episode |
| pottery | Make something by hand |
4. Picking an idea keeps the typed title and borrows everything else, then opens the review screen.

"Answer step by step instead" continues with the nine questions.

All matching runs on the phone. There's no network or AI call.

### 8.4 Review screen

The same sentence, fully filled:

- **Your own words:** solid highlight.
- **Borrowed from the idea:** lighter highlight with a dotted underline, under a legend: "suggested, tap to change."
- **Tapping a value** opens that step's choices inline under the sentence.

A sticky note names the idea that was used. The week-cost bars and Save mission follow, plus Edit step by step.

## 9. Screens

These match the mockup canvas. All screens keep the bottom tab bar: Diary, Missions, Metrics.

| Screen | Contents |
|---|---|
| Diary, morning | Date and week strip. "This week" bar per pillar. Mood row. To-do list with "Already in your day" (fixed blocks), "Picked for today" (reason line, duration, points chip, crowns) and "Didn't fit today" sticky. Buttons: Swap one, Lighter day, Looks good. |
| Diary, evening | Updated pillar bars. Ticked items struck through in pillar color. Did some / Move / Skip on unticked items. Highlights card (points today, pillars touched, crowned items). |
| Diary, Monday | "Last week" card (points, pillars full, what the app adjusted). Up to two pace-offer sticky notes. Today's list. |
| New mission, kind | Sentence so far, then five kind cards, each with an example. Progress shows 9 steps. |
| New mission, before saving | Full sentence, gentle-start sticky, week-cost bars, free hours, Make it lighter, Save mission. |
| Missions | Per pillar: marker heading with this week's target, then tilted sticky notes showing pace, weight and status. Recharge group with "no points." + opens the library. |
| Metrics | Four pillar rings (points of target, full or light), what next week adjusts, the week highlights card, pattern lines, a Monday-first calendar (day rings split by points per pillar, white for what didn't happen, crown on balanced weeks), and one progress line per mission. |
| Ideas library | Section 8.2. |
| Write your own | Title inside the sentence, matched ideas, step-by-step fallback. |
| Review what we filled in | Section 8.4. |

### 9.1 Visual language

These are kept from the current app and extended:

- **Paper, ruled lines and type.** Paper background `#FCF9ED`, ruled lines `#D4EBFF`, and light italic Roboto for anything written "in the diary."
- **Section headings** sit on the `block_b` marker swipe tinted per section.
- **Icons are the existing hand-drawn ones.** The check-square, crown, faces, glasses, flags, star, heart and tab icons are reused. The wand now always means "the app adjusted something for you."
- **Prompts and anything that didn't fit are sticky notes,** slightly tilted with a soft shadow, matching today's mission cards.
- **Pillar colors:**

| Pillar | Light (headings, chips) | Strong (checks, rings, bars) | Ink (text on light) |
|---|---|---|---|
| Health | `#F6D3FF` | `#E7B5F5` | `#6B3D7A` |
| Wealth | `#FFD4B8` | `#F9B98F` | `#7A4A2C` |
| Love | `#FFCBD2` | `#F7A9B5` | `#7A3341` |
| Life | `#D4EBFF` | `#A9D1F5` | `#2F5677` |
| Recharge | `#E2E2E2` | `#C4C4C4` | `#555555` |

The light colors are the current `getPillarColor` values. The strong colors replace `getDarkPillarColor`, which is too faint for rings and checkboxes.

These change from the current app:

- **Button text on peach** (`#FCBE98`) is dark brown `#4A2A18` instead of white, so it's readable.
- **Unticked checkboxes** show a faint outline in pillar color.
- **The metrics calendar** starts on Monday to match the week cycle and uses flat tints instead of a gradient.
- **Tap targets** are at least 44 px.

## 10. Data model changes

The database is Room, currently version 4. **Existing data is not kept:** the new schema replaces the old one and the database is wiped (destructive migration). There is no data migration.

| Entity | Change |
|---|---|
| `Mission` | One table holds everything: title, `pillar` (enum name, including RECHARGE), `kind`, `weight` (0–3), `durationMinutes`, `effort`, `ifMissed`, pace fields (`count` + `period`, `goalCount`, `intervalCount` + `intervalUnit`, `weekDays`, `startTime`, `endTime`), `startDate`, `endDate`, `pausedUntil`, `motivation`, `active`. The old `mission_frequency`, `pillar` and `mission_pillar_mapping` tables are gone; pillars are a fixed enum in code. `ideaId` comes with the ideas library. |
| `TodayTask` | `state` (PLANNED, DONE, PARTIAL, MOVED, SKIPPED), `pointsEarned`, `isFixedBlock`, `crowned` (replaces the TOP3 tag), `movedFromDate`, `reasonCode`, `reasonArgs`, `sortOrder`. The reminder and sub-task tables are dropped. |
| `Milestone` | Unchanged. |
| New `DaySettings` | Free minutes per weekday. The evening check time is a constant (18:00) for now. |
| New `DayPlan` | Date, free minutes, energy, confirmed, fixed-blocks-ticked, and the day's didn't-fit pick. The row is written last, so a day exists only once its list is built. |
| New `WeekSummary` | Added in plan 2 (week cycle): week start, per-pillar earned and target points, balanced flag. |
| New `PaceOffer` | Added in plan 2: mission, offer type, week shown, response, snooze-until date. |

## 11. Build order

This pass, in order:

1. **Data and points engine:** the new schema (wiping the old database), points and target calculation, week close-out, pace-offer triggers. Pure Kotlin in `commonMain`.
2. **Planner and diary:** the list-building rules, reason lines, evening check, Monday cards; the diary screens. Removes `Planner`, `PlanningList` and the red/yellow/green categorization.
3. **Creating missions:** the nine-question sentence flow and the week-cost preview. Replaces `AddMissionPopup` and `OptimizedEditMissionPopup`.
4. **Missions and metrics screens:** the pillar-grouped missions list, rings, the Monday-first calendar with point-sized day rings, patterns, per-mission lines.

Next pass: the ideas library (section 8), write-your-own matching and the review screen.

## 12. Testing

The owner tests by hand on a device or emulator. The plans include no automated tests; each task ends with the project compiling.

## 13. Not in scope for v1

- Notifications and reminders. `TodayTaskReminder` is untouched.
- Placing flexible missions at clock times. Only fixed blocks have times.
- User-editable or extra pillars.
- Accounts, sync and sharing.
- AI-generated wording or matching.
- Changes to mood, water intake or notes beyond keeping them on the diary page.
- The optional "Each time is a… (call, session, class)" question. `progressUnit` stays unused for now.

## 14. Defaults chosen in this spec

Each of these is easy to change later:

| Default | Value |
|---|---|
| Free hours | 3 h on weekdays, 10 h on Saturdays and Sundays (35 h a week); today's value can be changed from the diary |
| Evening check | Opens after 18:00 |
| Low-energy day | Uses 60% of free hours and skips draining missions |
| Recharge suggested | On days with 6 h or more of fixed blocks |
| Pace offers | At most 2 a week; "Not yet" snoozes for 4 weeks |
| Matching | Starts after 2 characters with a 150 ms debounce, shows the top 4, best match needs a 2-point lead (one more matching word) |
