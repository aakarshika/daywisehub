package com.example.todoapp.db.sample

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.TodayTaskRepository

/**
 * Dev-only sample data. Never runs on a normal start: the Android debug build runs it when launched with
 * a `sample` extra (see scripts/seed-sample.sh), and the iOS debug build with `-sample <name>`, and only on a database with no missions yet.
 */
enum class SampleSeed {
    /** About 13 weeks that show every day badge on the Metrics calendar. */
    BADGES,
    /** The mockups' example missions with a few weeks of history. */
    MISSIONS,
    /** MISSIONS plus five more weeks of history for the Metrics tab. */
    METRICS,
    /** A tidy five weeks for the store screenshots (see ScreenshotSample). */
    SCREENSHOTS;

    suspend fun run(database: AppDatabase, missionRepository: MissionRepository, todayTaskRepository: TodayTaskRepository) {
        when (this) {
            BADGES -> BadgeSample.seed(database)
            SCREENSHOTS -> ScreenshotSample.seed(database)
            MISSIONS -> SampleData.seed(missionRepository, todayTaskRepository)
            METRICS -> {
                SampleData.seed(missionRepository, todayTaskRepository)
                MetricsSample.seed(missionRepository, todayTaskRepository)
            }
        }
    }

    companion object {
        fun from(name: String?): SampleSeed? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}
