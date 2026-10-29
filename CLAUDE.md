# Inbalance

This is a Kotlin Multiplatform app (Compose Multiplatform, Room KMP, Koin). Shared code is in `shared/src/commonMain/kotlin/com/example/todoapp/`.

## Development mode: speed over quality

The app is in its dev phase. Ship fast; the owner tests by hand.

- Build it directly. Make small changes yourself, and use at most one agent per independent feature.
- Don't use reviewer agents or review-fix rounds unless the owner asks for them.
- Don't write specs or plans before building. Afterwards, give a short list of what changed and what to try.
- Don't write automated tests.
- Skip polish: accessibility extras, KDoc wording, small speed-ups and rare edge cases.
- Ask only when a choice really changes what gets built. Otherwise pick and say what you picked.
- Start a fresh session for each batch of asks, to keep the context small.

## References

- `docs/old-app-screenshots/` holds screenshots of the app before the redesign: the Metrics month scroll with the pillar filter, the diary and its swipe progress lines, the planning mode, and the old mission editor.

## Build

- Gradle: `./gradlew`, which uses Gradle 8.14.6.
- After each change, run `:androidApp:assembleDebug` only.
- Run iOS once at the end of a batch, or when asked, with `:shared:compileKotlinIosSimulatorArm64` (or `:shared:linkDebugFrameworkIosSimulatorArm64`).

## Rules that still hold

- Never push, and never touch `git stash`. `stash@{0}` is the owner's old work.
- `PlanRepository` writes go through its private `write {}`, which takes a non-reentrant mutex. A public write must never call another public write.
- Room schema changes need a version bump with an `AutoMigration`. Data from version 5 on is kept.
