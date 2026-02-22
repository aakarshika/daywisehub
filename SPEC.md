# DayWiseHub — Project Spec & Cleanup Plan

> Living document. Last updated: 2026-02-22

---

## 1. What Is DayWiseHub?

DayWiseHub is a **Kotlin Multiplatform** (KMP) personal productivity and wellness app for **Android and iOS**. It helps users track their daily tasks (tied to long-term missions), mood, and water intake — all organized around a diary-style daily view.

**Core features:**
- **Daily Diary** — View and manage today's tasks, track mood, log water intake, take notes
- **Missions** — Define long-term goals with pillars (categories), milestones, and frequency schedules
- **Metrics / Calendar** — Visualize task completion over time on a month calendar
- **Planning** — Select which missions to work on today, activate/deactivate tasks

---

## 2. Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.0.20 |
| Shared UI | Compose Multiplatform | 1.6.6 |
| Navigation | Precompose | 1.5.7 |
| Database | AndroidX Room (KMP) | 2.7.0-alpha01 |
| DI | Koin | 3.2.0 |
| Async | Kotlinx Coroutines | (bundled) |
| Date/Time | kotlinx-datetime + kizitonwose-calendar | 2.6.1 |
| Logging | Kermit | 2.0.5 |
| Animations | Kottie (Lottie for KMP) | 2.0.1 |
| Build | Gradle (Kotlin DSL) | 8.9 |
| Android | compileSdk 34, minSdk 24, targetSdk 34 | — |
| iOS | iosArm64, iosX64, iosSimulatorArm64 | — |

---

## 3. Architecture

```
┌─────────────────────────────────────────────────┐
│                   Platform Entry                 │
│     Android: MainActivity → App()                │
│     iOS: MainViewController → App()              │
└──────────────────────┬──────────────────────────┘
                       │
              ┌────────▼────────┐
              │    App.kt       │
              │  Koin setup     │
              │  InitScreen /   │
              │  InitScreen2    │
              └────────┬────────┘
                       │ (after auth)
              ┌────────▼────────┐
              │   NavScreens    │
              │  PreCompose     │
              │  NavHost +      │
              │  BottomNavBar   │
              └────────┬────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
   ┌────▼───┐    ┌─────▼────┐   ┌────▼──────┐
   │ Diary  │    │ Missions │   │  Metrics  │
   │ Screen │    │ Screen   │   │  Screen   │
   └────┬───┘    └────┬─────┘   └────┬──────┘
        │             │              │
   ViewModels    ViewModels     ViewModels
        │             │              │
   Repositories  Repositories   Repositories
        │             │              │
        └──────── Room DAOs ─────────┘
                      │
                  AppDatabase
                  (SQLite)
```

**Pattern:** MVVM with Repository pattern. ViewModels hold reactive state (`SharedFlow`/`StateFlow`), Repositories wrap DAOs, DAOs define SQL queries.

**DI:** All 7 repositories are `single` scoped (one instance). Most ViewModels are `factory` scoped (new per use). `InitViewModel` and `CalDiaryViewModel` are `single` scoped.

---

## 4. Navigation Graph

### Routes (Precompose NavHost)

| Route | Screen | Description |
|---|---|---|
| `"Diary"` (initial) | `DiaryScreen` | Daily task list, mood, water, notes |
| `"Missions"` | `MissionsControllerScreen` | List of all missions, add/edit |
| `"Calendar"` | `MetricsScreen` + `DateHighlights` | Month calendar heatmap + date detail |

### Pre-Navigation Screens (conditional, before NavHost)

| Screen | Condition | Purpose |
|---|---|---|
| `InitScreen` | User not logged in | Username entry / signup |
| `InitScreen2` | User logged in | Loads user, wraps `NavScreens` |

### Boot Sequence

1. Platform entry → `App()` composable
2. `KoinF.setupKoin()` initializes DI
3. `InitScreen` checks `LoginStatus` table
4. On signup/existing login → `InitScreen2` loads user by ID
5. `NavScreens` renders bottom nav with 3 tabs

---

## 5. Database Schema

### Entities (13 tables)

| Entity | Table | Purpose | FK Parents |
|---|---|---|---|
| `User` | `users` | User account | — |
| `LoginStatus` | `login_status` | Auth state persistence | — |
| `Mission` | `mission` | Long-term goal | — |
| `Pillar` | `pillar` | Category (Health, Wealth, Love, Life) | — |
| `MissionPillarMapping` | `mission_pillar_mapping` | M:N mission↔pillar | Mission, Pillar |
| `MissionFrequency` | `mission_frequency` | How often a mission recurs | Mission (CASCADE) |
| `Milestone` | `milestone` | Sub-goal within a mission | Mission |
| `TodayTask` | `today_task` | Daily task instance from a mission | Mission |
| `TodayTaskReminder` | `today_task_reminder` | Reminder for a task | TodayTask |
| `SubTask` | `sub_task` | Checklist item within a task | TodayTask |
| `Mood` | `mood` | Mood type definition | — |
| `TodayMood` | `today_mood` | User's mood for a date | Mood (CASCADE) |
| `WaterIntake` | `water_intake` | Water intake for a date | — |

### Entity Relationships

```
User
 ├── Mission (via user_id)
 │    ├── MissionFrequency (CASCADE DELETE)
 │    ├── MissionPillarMapping → Pillar
 │    ├── Milestone
 │    └── TodayTask
 │         ├── TodayTaskReminder
 │         └── SubTask
 ├── Mood
 │    └── TodayMood (CASCADE DELETE)
 └── WaterIntake
```

---

## 6. ViewModel → Repository → DAO Dependency Map

```
InitViewModel
 ├── UserRepository.insertUser
 ├── LoginRepository.insertLoginStatus, getLoginStatus
 ├── MissionRepository.insertDefaultPillars
 └── TodayMoodRepository.insertDefaultMoods

UserViewModel
 └── UserRepository.getUserValueByUserId

DiaryViewModel
 └── TodayTaskRepository.getAllTasksForDate1, updateStatus, insertFullTask,
     updateTodayTask, addRandomMissionForDay

HabitItemViewModel
 └── TodayTaskRepository.getTaskProgressForPastAround

MoodViewModel
 └── TodayMoodRepository.getAllMoods, getTodayMood, upsertTodayMood

WaterIntakeViewModel
 └── WaterIntakeRepository.getWaterIntake, upsertWaterIntake

PlanningViewModel
 ├── MissionRepository.getMissions
 └── TodayTaskRepository.getTasksPastMonth, addMissionForDay,
     deleteTaskForToday, updateTag, activateTaskForToday

MetricsViewModel
 └── TodayTaskRepository.getAllTasks

CalDiaryViewModel
 └── MissionRepository.getAllMissionIds

BottomHighlightsViewModel
 └── TodayTaskRepository.getAllTasksForDate2

MissionsControllerViewModel
 └── MissionRepository.getAllMissionIds, insertFullMission, updateMission,
     updatePillarMapping, updateMissionFrequency, updateMilestones

MissionItemViewModel
 └── MissionRepository.getMission, getPillar, getMissionFrequency, getMilestones

DayMissionProgressViewModel
 ├── MilestoneRepository.getAllMilestonesForMission
 └── TodayTaskRepository.getAllTasksForMission
```

---

## 7. File Inventory & Status

### Files to DELETE (dead code)

| File | Lines | Reason |
|---|---|---|
| `screen/metrics/CalDiaryScreen.kt` | 428 | 100% commented out |
| `screen/missions/EditMissionPopup.kt` | 922 | Replaced by `OptimizedEditMissionPopup` + subcomponents; never called |
| `screen/missions/calendar/progress/MissionProgressCalendarb.kt` | 234 | Orphaned duplicate with typo name; never called |

**Total dead file lines: 1,584**

### Dead Code Within Live Files

| File | Dead Item | Type |
|---|---|---|
| `App.kt` | `TaskScreen` enum | Unused enum |
| `App.kt` | `AppBar` composable | Defined, never called |
| `App.kt` | `Text("Hello, World!")` | Visible behind scaffold content |
| `InitViewModel.kt` | `LoginStatusUiState`, `LoggedInUserUiState` data classes | Declared, never used |
| `DiaryViewModel.kt` | `habitList` SharedFlow | Observed but never emitted to (loading code commented out) |
| `DiaryViewModel.kt` | `updateTaskTag()` | Never called from any screen |
| `MoodViewModel.kt` | `allMoods` SharedFlow | Loaded in init but never observed by any screen |
| `MoodViewModel.kt` | `c1Date` constructor param | Never used (functions take explicit date) |
| `WaterIntakeViewModel.kt` | `c1Date` constructor param | Never used (functions take explicit date) |
| `CalDiaryViewModel.kt` | `missionIds` SharedFlow | Never observed |
| `CalDiaryViewModel.kt` | `loadMissionIds()` | Never called |
| `CalDiaryViewModel.kt` | `selectNextDate()`, `selectPreviousDate()` | Wired as lambdas but never invoked from UI |
| `CalDiaryViewModel.kt` | 8+ unused imports | Remnants of old design |
| `DiaryScreenList.kt` | `WeatherHeaderItem`, `TodoMenuItems`, `ExtraLines` | Defined, never called |
| `WeekCalendarScreen.kt` | `WeekCalendarScreen`, `RoundButton` | Never called from navigation or any composable |
| `WeekDayHeader.kt` | `WeekDayHeader` | Caller is commented out |
| `basicutils/cal/CalUtils.kt` | `NavigationIcon` + all 6 `rememberFirst*` composables | Never called from any file |
| `missions/editpopup/ImportanceSection.kt` | `ImportanceSection`, `ImportanceButton` | Caller in `OptimizedEditMissionPopup` is commented out |
| `basicutils/components/CustomHabitButton.kt` | `CustomHabitButton` | Never called from any file |
| `basicutils/components/PlanningLineText.kt` | `PlanningLineText` | Never called from any file |

### Dead DAO Methods (never called by any repository)

| DAO | Method |
|---|---|
| `MissionDao` | `upsertMilestones(List<Milestone>)` |
| `MilestoneDao` | `upsertMilestone`, `getMilestoneById`, `getMilestonesByMissionId` |
| `TodayTaskDao` | `getAllTodayTasks`, `getTodayTaskReminder`, `getTodayTaskWithDetails(taskId)`, `getSubTasks`, `insertSubTasks` |

### Dead Repository Methods (never called by any ViewModel)

| Repository | Method |
|---|---|
| `UserRepository` | `getUserByUserId`, `getUserByUsername`, `getUserByUsernameAndPassword`, `deleteUser` |
| `MissionRepository` | `getAllPillars` |
| `MilestoneRepository` | `getAllMilestonesForDate`, `getAllMilestonesForDateAndMission`, `getAllMilestones` |
| `TodayTaskRepository` | `getHabitMissions` (caller is commented out) |

### Unused Resources (25 of ~60 files — 42%)

`block_a.png`, `diary_handmade.png`, `banner_style.png`, `banner.png`, `circle_a.png`, `circle_b.png`, `square_check_a.png`, `square_check_b.png`, `diary_edit.png`, `diary.png`, `surprised.png`, `happiness.png`, `angry.png`, `afraid.png`, `disgusted.png`, `crown_filled_a.png`, `crown_fill_a.png`, `crown_filled_c.png`, `square_filled_b.png`, `square_filled_c.png`, `square.png`, `square_c.png`, `heart_border.png`, `ribbon_dotted.png`, `confetti-animation.json`

### Empty / Stub Files

| File | Content |
|---|---|
| `api/OpenWeatherApi.kt` | Empty class body |
| `api/WeatherModels.kt` | Empty class body |

---

## 8. Bugs & Data Integrity Issues

### P0 — Critical

| Issue | Location | Impact |
|---|---|---|
| **`MyDate` produces non-zero-padded dates** | `db/models/MyDate.kt:22,26` | `"2026-2-5"` instead of `"2026-02-05"`. All SQL `BETWEEN` clauses and string date comparisons produce **wrong results** for single-digit months/days. |
| **`@Insert(REPLACE)` used as update** | `MissionDao.updateMission`, `TodayTaskDao.updateTodayTask` | `REPLACE` deletes then re-inserts. Triggers `ON DELETE CASCADE` on child tables — updating a mission silently deletes its `MissionFrequency`. |
| **No database migration strategy** | `db/Database.kt` | Version 1, no migrations defined, no `fallbackToDestructiveMigration()`. Any schema change crashes existing installs. |
| **Hardcoded password** | `InitViewModel.kt:38` | `password = "password123"` on every signup. Security issue even for local-only data. |
| **Double Koin initialization** | `MainActivity.kt` + `App.kt:65` | `setupKoin()` called in both; the one in `App()` is redundant on Android and could fail on iOS if call order changes. |

### P1 — High

| Issue | Location | Impact |
|---|---|---|
| **`getTasksPastMonth()` fetches ALL tasks** | `TodayTaskDao` | SQL has no date filter despite the name. O(n) unbounded. |
| **Missing indexes** on `task_date`, `water_date`, `tm_date`, `tm_mood_id` | Various entities | Slow queries as data grows. |
| **Inconsistent FK ON DELETE policy** | All entities | `MissionFrequency` cascades, but `Milestone`, `TodayTask`, `SubTask`, `TodayTaskReminder`, `MissionPillarMapping` do not. Deleting a mission orphans most child rows. |
| **`SubTask` PK defaults to `-1`** | `SubTask` entity | `autoGenerate = true` treats `0` as auto-gen. `-1` is inserted literally, causing potential conflicts. |
| **Redundant index on PK** | `TodayMood` | `indices = [Index("tm_id")]` — PK already indexed. Should be on `tm_mood_id` (the FK). |
| **`paddingValues` ignored** | `App.kt:74` | Scaffold provides padding but content doesn't use it. Content renders behind system bars. |
| **No back-stack management** | `NavScreens.kt` | Bottom nav `navigate()` calls grow the back stack infinitely instead of popping to root. |

### P2 — Medium

| Issue | Location | Impact |
|---|---|---|
| **Business logic in DAOs** | `TodayTaskDao.addMissionForDay`, `addRandomMission`, `prepareTaskObject` | Architecture violation — should be in repository. |
| **Mutable state exposed publicly** | 10+ ViewModels | `MutableSharedFlow` exposed directly; screens could accidentally emit. Should expose read-only `SharedFlow`. |
| **`UserInitManager` global singleton** | `UserViewModel.kt` | Mutable global state breaks reactive pattern. |
| **`getAllMoods()` return type misleading** | `TodayMoodDao` | Returns `List<TodayMoodWithDetails>` but SQL only queries `mood` table — `todayMood` field is always null. |
| **Double milestone delete** | `MissionRepository.updateMilestones` | Calls `deleteMilestones` then `updateMilestones` which internally deletes again. |
| **Package declaration mismatch** | `WaterIntake.kt`, `WaterIntakeDao.kt` | Declare `package ...data.mood` but live in `data/water/` directory. |

---

## 9. Code Smells & Inconsistencies

### Naming

| Issue | Location |
|---|---|
| File named `MissionProgressCalendarb.kt` (trailing `b`) | `screen/missions/calendar/progress/` |
| Function named `deleteMilestonesss` (triple `s`) | `MissionDao` |
| Double space in `package  com.example.todoapp.screen.init` | `InitViewModel.kt`, `InitScreen.kt`, `InitScreen2.kt` |
| Nonsense `contentDescription` strings: `"sdfgf"`, `"sdfgh"`, `"asd"`, `"asdsfgd"`, etc. | Throughout UI files |
| App ID still `com.example.todoapp.android` | `androidApp/build.gradle.kts` |

### Duplicate Code

| Duplication | Files |
|---|---|
| `Color.darken()` extension | `EditMissionPopup.kt` (dead) AND `MissionUtils.kt` |
| `getOrdinal()` | `EditMissionPopup.kt` (dead) AND `MissionUtils.kt` |
| Frequency calculation functions | `EditMissionPopup.kt` (dead) AND `MissionUtils.kt` AND `AddMissionPopup.kt` |
| `MonthDay` composable | `WeekCalendarScreen.kt` AND `MetricsScreen.kt` |
| `CalendarArea` composable | `WeekCalendarScreen.kt` AND `MetricsScreen.kt` |
| `MonthHeader` composable | `MissionProgressCalendar.kt` AND `MissionProgressCalendarb.kt` |
| `Red80` color constant — **different values** | `MoodHeader.kt` (`0xFFFFAFBD`) vs `EditMissionPopup.kt` (`0xFFFFCBD2`) |
| `DiaryLineText`, `NotesLineText`, `PlanningLineText` | Three near-identical styled text composables |
| `loadMissionIds()` | `MissionsControllerViewModel` AND `CalDiaryViewModel` |
| `updateTag()` | `DiaryViewModel` AND `PlanningViewModel` |

### Hardcoded Values Needing Extraction

| Category | Examples |
|---|---|
| **Status strings** | `"COMPLETED"` (20+ usages), `"TOP3"`, `"CHECK"`, `"EDIT"`, `"IDLE"` |
| **Pillar names** | `"HEALTH"`, `"WEALTH"`, `"LOVE"`, `"LIFE"` |
| **Importance levels** | `"LOW"`, `"MEDIUM"`, `"HIGH"` |
| **Frequency labels** | `"OCCASIONAL"`, `"COMFORTABLE"`, `"BALANCED"`, `"RIGOROUS"` |
| **Period labels** | `"DAILY"`, `"WEEKLY"`, `"MONTHLY"` |
| **Hardcoded dates** | `"2025-3-13"`, `"2025-1-1"` in EditMissionPopup |
| **44 inline `Color(0x...)` usages** | Should be a centralized theme/colors file |
| **Magic number dimensions** | `500.dp`, `255.dp`, `535.dp`, `82.dp`, etc. |

### Inconsistent Patterns

| Pattern | Inconsistency |
|---|---|
| **ViewModel injection** | Some injected as composable parameters, others grabbed inline via `KoinF.di?.get<>()!!` |
| **State types** | Some use `StateFlow`, others `SharedFlow`, no clear convention |
| **State exposure** | Some ViewModels expose private `_state` / public `state`, most expose `MutableSharedFlow` directly |
| **Init loading** | Some ViewModels load data in `init`, most require manual `loadX()` calls |
| **Local vs ViewModel state** | No clear rule for what goes in `remember {}` vs ViewModel |

---

## 10. Stale / Legacy Artifacts

| Artifact | Location | Note |
|---|---|---|
| Old Room schema JSON | `shared/schemas/com.example.todoapp.AppDatabase/1.json` | Contains a single `TodoEntity` table from prototype. Stale. |
| `GreetingView` / `DefaultPreview` | `MainActivity.kt` | Unused test composables in production code |
| `Text("Hello, World!")` | `App.kt:61-63` | Debug text rendering behind scaffold |
| `modifier` variable | `App.kt:64` | Assigned, never used |
| `coroutineScope` variable | `App.kt:75` | Assigned, never used |

---

## 11. Cleanup Plan — Prioritized

### Phase 1: Delete Dead Code (Low Risk, High Impact)

1. Delete `CalDiaryScreen.kt` (428 lines, 100% commented out)
2. Delete `EditMissionPopup.kt` (922 lines, fully replaced)
3. Delete `MissionProgressCalendarb.kt` (234 lines, orphaned duplicate)
4. Remove `TaskScreen` enum, `AppBar` composable, `Text("Hello, World!")`, unused variables from `App.kt`
5. Remove `LoginStatusUiState`, `LoggedInUserUiState` from `InitViewModel.kt`
6. Remove `WeatherHeaderItem`, `TodoMenuItems`, `ExtraLines` from `DiaryScreenList.kt`
7. Remove dead DAO methods (9 methods across 3 DAOs)
8. Remove dead repository methods (9 methods across 4 repos)
9. Delete 25 unused image/animation resources
10. Delete stale schema JSON (`com.example.todoapp.AppDatabase/1.json`)
11. Delete empty stub files `OpenWeatherApi.kt`, `WeatherModels.kt` (or keep if planned)

### Phase 2: Fix Bugs (High Risk, Essential)

1. Fix `MyDate` to produce zero-padded ISO dates (`%02d` format)
2. Replace `@Insert(REPLACE)` with `@Update` where used for updates
3. Add `fallbackToDestructiveMigration()` (or real migrations) to database builder
4. Fix `getTasksPastMonth()` SQL to actually filter by date range
5. Fix `SubTask` PK default from `-1` to `0`
6. Fix `paddingValues` not being applied in `App.kt` Scaffold
7. Remove hardcoded `password = "password123"`

### Phase 3: Clean Up Naming & Style (Low Risk)

1. Fix `package  com.example.todoapp...` double-space (3 files)
2. Replace all nonsense `contentDescription` strings with meaningful ones
3. Fix `deleteMilestonesss` naming
4. Fix `WaterIntake`/`WaterIntakeDao` package declaration (`.mood` → `.water`)
5. Remove all debug Logger statements (30+ instances) or gate behind `BuildConfig.DEBUG`

### Phase 4: Architectural Improvements (Medium Risk)

1. Make all ViewModel `MutableSharedFlow`/`MutableStateFlow` private; expose read-only
2. Remove unused constructor params (`c1Date` in `MoodViewModel`, `WaterIntakeViewModel`)
3. Move business logic out of `TodayTaskDao` into repository
4. Eliminate `UserInitManager` singleton; use proper reactive state
5. Consolidate all color constants into a single `AppColors` object
6. Extract string constants (`"COMPLETED"`, `"TOP3"`, etc.) into enums or sealed classes
7. Fix bottom nav back-stack management (single-top, pop-up-to)
8. Add missing database indexes (`task_date`, `water_date`, `tm_date`, `tm_mood_id`)
9. Standardize FK `ON DELETE` policy across all child tables

### Phase 5: Consolidation & Deduplication (Medium Risk)

1. Merge `DiaryLineText`, `NotesLineText`, `PlanningLineText` into a single parameterized component
2. Extract shared `MonthDay` and `CalendarArea` composables
3. Remove `CalDiaryViewModel` dead methods; consider if it should just be a shared state holder
4. Unify Koin initialization — remove the `KoinF.setupKoin()` call in `App.kt`
5. Clean up or remove the orphaned `WeekCalendarScreen.kt`, `WeekDayHeader.kt`, `CalUtils.kt`, `ImportanceSection.kt`, `CustomHabitButton.kt`, `PlanningLineText.kt`

---

## 12. Things to Keep In Mind for Later (Not Blocking)

These are not cleanup items but will be important when preparing for production:

- App ID needs to change from `com.example.todoapp.android` to a real package name
- No signing configuration exists (keystore, signing config in gradle)
- R8/ProGuard not enabled for release builds
- No CI/CD pipeline
- No automated tests
- No privacy policy or Play Store assets
- `versionCode` is 1, `versionName` is "1.0" — need a versioning strategy
