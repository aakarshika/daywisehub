---
name: DayWiseHub Code Cleanup
overview: "A phased cleanup of the DayWiseHub codebase: delete ~2,500+ lines of dead code, fix critical silent bugs, remove unused resources, standardize naming/style, and improve architecture — all without changing visible app behavior."
todos:
  - id: phase1-delete-dead-files
    content: "Phase 1: Delete 3 entirely dead files (CalDiaryScreen.kt, EditMissionPopup.kt, MissionProgressCalendarb.kt)"
    status: completed
  - id: phase2-delete-orphaned
    content: "Phase 2: Delete orphaned files (WeekCalendarScreen.kt, WeekDayHeader.kt, CalUtils.kt, CustomHabitButton.kt, PlanningLineText.kt, empty API stubs, stale schema)"
    status: completed
  - id: phase3-dead-code-live-files
    content: "Phase 3: Remove dead code from live files (unused enums, data classes, ViewModels state, DAO/repo methods, commented-out blocks), update DI and call sites"
    status: completed
  - id: phase4-unused-resources
    content: "Phase 4: Delete 25 unused drawable/animation resources"
    status: completed
  - id: phase5-naming-style
    content: "Phase 5: Fix naming (double spaces, deleteMilestonesss, package mismatch), replace nonsense contentDescriptions, remove debug loggers, clean commented code"
    status: completed
  - id: phase6-bug-fixes
    content: "Phase 6: Fix silent bugs (MyDate zero-padding, @Insert REPLACE, getTasksPastMonth SQL, SubTask PK, migration fallback, paddingValues, TodayMood index)"
    status: completed
  - id: phase7-architecture
    content: "Phase 7: Architectural cleanup (encapsulate ViewModel state, eliminate UserInitManager, move DAO business logic to repo, fix back-stack, deduplicate Koin init, fix redundant milestone delete)"
    status: completed
  - id: phase8-consolidate
    content: "Phase 8: Consolidate hardcoded values (string enums for statuses/types/pillars, centralize colors into AppColors.kt, extract magic dimensions)"
    status: completed
isProject: false
---

# DayWiseHub Code Cleanup Plan

Each phase is an independent, self-contained commit. Phases are ordered so that earlier phases make later ones easier, and each phase compiles and runs correctly on its own.

---

## Phase 1: Delete Dead Files (3 files, ~1,584 lines)

Delete files that are entirely dead — never called, fully replaced, or 100% commented out.

- **Delete** `[shared/.../screen/metrics/CalDiaryScreen.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/metrics/CalDiaryScreen.kt)` — 428 lines, 100% commented out
- **Delete** `[shared/.../screen/missions/EditMissionPopup.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/missions/EditMissionPopup.kt)` — 922 lines, fully replaced by `OptimizedEditMissionPopup` + subcomponents
- **Delete** `[shared/.../screen/missions/calendar/progress/MissionProgressCalendarb.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/missions/calendar/progress/MissionProgressCalendarb.kt)` — 234 lines, orphaned duplicate with typo name

---

## Phase 2: Delete Orphaned Files (~600 lines)

Delete files containing composables/utilities that are defined but never called from any reachable code path.

- **Delete** `[shared/.../screen/diary/WeekCalendarScreen.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/diary/WeekCalendarScreen.kt)` — 298 lines, `WeekCalendarScreen` + `RoundButton` never called
- **Delete** `[shared/.../screen/diary/diaryitem/components/dateheader/WeekDayHeader.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/diary/diaryitem/components/dateheader/WeekDayHeader.kt)` — 29 lines, only caller is commented out
- **Delete** `[shared/.../basicutils/cal/CalUtils.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/basicutils/cal/CalUtils.kt)` — 261 lines, `NavigationIcon` + 6 `rememberFirst`* functions, none called
- **Delete** `[shared/.../basicutils/components/CustomHabitButton.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/basicutils/components/CustomHabitButton.kt)` — never called
- **Delete** `[shared/.../basicutils/components/PlanningLineText.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/basicutils/components/PlanningLineText.kt)` — never called
- **Delete** `[shared/.../api/OpenWeatherApi.kt](shared/src/commonMain/kotlin/com/example/todoapp/api/OpenWeatherApi.kt)` and `[WeatherModels.kt](shared/src/commonMain/kotlin/com/example/todoapp/api/WeatherModels.kt)` — empty class bodies, no implementation
- **Delete** stale schema JSON at `shared/schemas/com.example.todoapp.AppDatabase/1.json` (old prototype with a single `TodoEntity` table)
- **Remove** any imports referencing deleted files from surviving files (e.g., `WeekCalendarScreen` import in `NavScreens.kt`)

---

## Phase 3: Remove Dead Code from Live Files

Strip dead declarations, unused variables, and commented-out blocks from files that are otherwise active.

### In `[App.kt](shared/src/commonMain/kotlin/com/example/todoapp/app/App.kt)`:

- Remove `TaskScreen` enum (never referenced)
- Remove `AppBar` composable (never called)
- Remove `Text("Hello, World!")` (renders behind scaffold)
- Remove unused `modifier` and `coroutineScope` variable assignments

### In `[InitViewModel.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/init/InitViewModel.kt)`:

- Remove `LoginStatusUiState` and `LoggedInUserUiState` data classes (never used)

### In `[CalDiaryViewModel.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/metrics/CalDiaryViewModel.kt)`:

- Remove `missionIds` SharedFlow and `loadMissionIds()` (never observed, never called)
- Remove `selectNextDate()` and `selectPreviousDate()` (wired as lambdas but never invoked in UI)
- Remove 8+ unused imports (MilestoneRepository, TodayTaskRepository, etc.)

### In `[DiaryViewModel.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/diary/DiaryViewModel.kt)`:

- Remove `habitList` SharedFlow (observed but never emitted to; loading code is commented out)
- Remove `updateTaskTag()` (never called from any screen)
- Remove commented-out habit loading block (lines ~34-40)

### In `[MoodViewModel.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/diary/diaryitem/components/diarymood/MoodViewModel.kt)`:

- Remove `allMoods` SharedFlow + its init-block loading (never observed by any screen)
- Remove unused `c1Date` constructor parameter

### In `[WaterIntakeViewModel.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/diary/diaryitem/components/waterintake/WaterIntakeViewModel.kt)`:

- Remove unused `c1Date` constructor parameter

### In `[DiaryScreenList.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/diary/DiaryScreenList.kt)`:

- Remove `WeatherHeaderItem`, `TodoMenuItems`, `ExtraLines` (defined but never called)
- Remove commented-out `SundayHeaderItem`, `ListHeader` sections

### In `[NavScreens.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/NavScreens.kt)`:

- Remove `goToTomorrow`/`goToYesterday` lambdas and their wiring to `CalDiaryViewModel` (since the target functions are being removed)
- Remove commented-out ViewModel instantiation

### In various DAO files — remove dead methods:

- `MissionDao`: remove `upsertMilestones(List<Milestone>)`
- `MilestoneDao`: remove `upsertMilestone`, `getMilestoneById`, `getMilestonesByMissionId`
- `TodayTaskDao`: remove `getAllTodayTasks`, `getTodayTaskReminder`, `getTodayTaskWithDetails(taskId)`, `getSubTasks`, `insertSubTasks`

### In various Repository files — remove dead methods:

- `UserRepository`: remove `getUserByUserId`, `getUserByUsername`, `getUserByUsernameAndPassword`, `deleteUser`
- `MissionRepository`: remove `getAllPillars`
- `MilestoneRepository`: remove `getAllMilestonesForDate`, `getAllMilestonesForDateAndMission`, `getAllMilestones`
- `TodayTaskRepository`: remove `getHabitMissions`

### In `[missions/editpopup/ImportanceSection.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/missions/editpopup/ImportanceSection.kt)`:

- Delete this file entirely — `ImportanceSection` call is commented out in `OptimizedEditMissionPopup.kt`, making it orphaned

### In `[MainActivity.kt](androidApp/src/main/java/com/example/todoapp/android/MainActivity.kt)`:

- Remove `GreetingView` and `DefaultPreview` composables (unused test code in production)

### Update DI module `[DI.kt](shared/src/commonMain/kotlin/com/example/todoapp/di/DI.kt)`:

- Remove constructor parameters that were removed from ViewModels (e.g., `c1Date` from `MoodViewModel`, `WaterIntakeViewModel`)
- Clean up any dangling references to deleted classes

### Update screens that observe removed state:

- `DiaryScreen` — remove observation of `habitList` from `DiaryViewModel`
- `DiaryScreen` — remove `goToTomorrow`/`goToYesterday` parameters if no longer passed
- Ensure all call sites compile cleanly after removals

---

## Phase 4: Remove Unused Resources

Delete 25 unused drawable/animation resources (~42% of all resources). These are image and animation files never referenced in code:

`block_a.png`, `diary_handmade.png`, `banner_style.png`, `banner.png`, `circle_a.png`, `circle_b.png`, `square_check_a.png`, `square_check_b.png`, `diary_edit.png`, `diary.png`, `surprised.png`, `happiness.png`, `angry.png`, `afraid.png`, `disgusted.png`, `crown_filled_a.png`, `crown_fill_a.png`, `crown_filled_c.png`, `square_filled_b.png`, `square_filled_c.png`, `square.png`, `square_c.png`, `heart_border.png`, `ribbon_dotted.png`, `confetti-animation.json`

Resources live in `shared/src/commonMain/composeResources/` and `androidApp/src/main/res/drawable-*/`. Need to verify each path before deleting.

---

## Phase 5: Fix Naming, Style, and Nonsense Strings

All cosmetic — no logic changes.

- Fix `package  com.example.todoapp.screen.init` (double space) in `InitViewModel.kt`, `InitScreen.kt`, `InitScreen2.kt`
- Fix `deleteMilestonesss` method name to `deleteMilestones` (and update its one internal caller)
- Fix `WaterIntake.kt` / `WaterIntakeDao.kt` package declaration from `...data.mood` to `...data.water`
- Replace all nonsense `contentDescription` strings (`"sdfgf"`, `"sdfgh"`, `"asd"`, `"asdsfgd"`, `"sfgh"`, `"pevhj j"`, `"dfg"`) with meaningful descriptions (e.g., `"Task icon"`, `"Mission image"`)
- Remove all debug Logger statements (30+ instances) — e.g., `Logger.e("editiiiiinnnnnggggg")`, `Logger.e("Is Priority?")`, `Logger.e("daysBetween ...")`. If some are genuinely useful, gate them behind a constant like `const val DEBUG_LOGGING = false`
- Remove commented-out code blocks in: `OptimizedEditMissionPopup.kt`, `MissionsControllerScreen.kt`, `AddMissionPopup.kt`, `MissionProgressCalendar.kt`, `DiaryScreen.kt`, `PlanningList.kt`, `PlanningViewModel.kt`

---

## Phase 6: Fix Silent Bugs

These fix behavior that is currently **silently wrong**. Each is a targeted, isolated fix.

### 6a. Fix `MyDate` zero-padding (`[MyDate.kt](shared/src/commonMain/kotlin/com/example/todoapp/db/models/MyDate.kt)`)

Current `now()` and `fromLocalDate()` produce `"2026-2-5"`. Fix to produce `"2026-02-05"`:

```kotlin
fun fromLocalDate(date: LocalDate): MyDate {
    val m = date.monthNumber.toString().padStart(2, '0')
    val d = date.dayOfMonth.toString().padStart(2, '0')
    return MyDate(dateString = "${date.year}-$m-$d")
}
```

### 6b. Replace `@Insert(REPLACE)` with `@Update` (`[MissionDao](shared/src/commonMain/kotlin/com/example/todoapp/db/data/mission/MissionDao.kt)`, `[TodayTaskDao](shared/src/commonMain/kotlin/com/example/todoapp/db/data/todotask/TodayTaskDao.kt)`)

Change `updateMission` and `updateTodayTask` from `@Insert(onConflict = REPLACE)` to `@Update` to prevent cascade-deleting child rows.

### 6c. Fix `getTasksPastMonth()` SQL (`[TodayTaskDao](shared/src/commonMain/kotlin/com/example/todoapp/db/data/todotask/TodayTaskDao.kt)`)

Add a date filter — currently `SELECT tt.* FROM today_task tt` with no `WHERE` clause. Should filter to last 30-60 days.

### 6d. Fix `SubTask` PK default

Change `val taskSubWorkId: Long = -1L` to `val taskSubWorkId: Long = 0L` so Room's `autoGenerate` works correctly.

### 6e. Add `fallbackToDestructiveMigration()` to database builder

In the platform-specific database builder (Android and iOS), add destructive migration fallback so schema changes don't crash the app. (A proper migration strategy is a future task.)

### 6f. Fix `paddingValues` in `App.kt`

Apply the scaffold's `paddingValues` to the content so it doesn't render behind system bars.

### 6g. Fix redundant `TodayMood` index

Change from index on PK `tm_id` (redundant) to index on FK `tm_mood_id` (useful).

---

## Phase 7: Architectural Cleanup

### 7a. Encapsulate ViewModel state

In all 13 ViewModels, make `MutableSharedFlow`/`MutableStateFlow` properties `private` and expose read-only `SharedFlow`/`StateFlow`. Standard pattern:

```kotlin
private val _dayTasks = MutableSharedFlow<List<ComboTask>?>(...)
val dayTasks: SharedFlow<List<ComboTask>?> = _dayTasks
```

### 7b. Eliminate `UserInitManager` singleton (`[UserViewModel.kt](shared/src/commonMain/kotlin/com/example/todoapp/screen/init/UserViewModel.kt)`)

Move `userId` and `username` into `CalDiaryViewModel` (already a singleton holding global state) or into the existing reactive `User` flow. Update all call sites that reference `UserInitManager.getUserId()`.

### 7c. Move business logic out of `TodayTaskDao`

Move `addMissionForDay`, `addRandomMission`, and `prepareTaskObject` from `TodayTaskDao` into `TodayTaskRepository`. The DAO should only contain `@Query`/`@Insert`/`@Update`/`@Delete` annotated methods.

### 7d. Fix bottom nav back-stack

In `NavScreens.kt`, add `launchSingleTop = true` and `popUpTo` behavior to `navigator.navigate()` calls so the back stack doesn't grow infinitely.

### 7e. Remove duplicate Koin initialization

Remove `KoinF.setupKoin()` from `App.kt` — it's already called in `MainActivity.onCreate()` (Android) and should be called in the iOS entry point. The `App()` composable should not initialize DI.

### 7f. Fix redundant milestone delete

In `MissionRepository.updateMilestones()`, remove the first `deleteMilestones()` call — the internal `updateMilestones` already does a delete before re-insert.

---

## Phase 8: Consolidate Hardcoded Values

### 8a. Extract string constants into enums

Create a `shared/.../db/models/TaskStatus.kt` with:

```kotlin
enum class TaskStatus(val value: String) {
    COMPLETED("COMPLETED"), IN_PROGRESS("IN_PROGRESS"), ...
}
```

Similarly for `TaskType` (TOP3, CHECK, etc.), `PillarName` (HEALTH, WEALTH, LOVE, LIFE), `ImportanceLevel`, `FrequencySet`, `FrequencyPeriod`.

Replace all 60+ scattered string literals with enum references.

### 8b. Centralize colors into `AppColors.kt`

Create `shared/.../screen/basicutils/AppColors.kt` to hold all color constants. Remove duplicates (e.g., two different `Red80` definitions). Replace all 44 inline `Color(0x...)` usages with named references.

### 8c. Extract magic number dimensions

Create constants for repeated dimension values (`500.dp` spacers, calendar cell sizes like `82.dp`, etc.) in a shared `Dimensions.kt` or within the relevant screen files.

---

## Build Verification Strategy

After each phase:

1. Run `./gradlew :shared:compileKotlinIosArm64` (shared module compiles)
2. Run `./gradlew :androidApp:assembleDebug` (Android app builds)
3. Manual smoke test: launch app, navigate all 3 tabs, add a task, edit a mission

Each phase is one commit with a descriptive message, so any phase can be reverted independently if something breaks.