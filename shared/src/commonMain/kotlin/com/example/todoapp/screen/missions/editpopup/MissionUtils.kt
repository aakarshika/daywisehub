package com.example.todoapp.screen.missions.editpopup

import androidx.compose.runtime.MutableState
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency


/**
 * Format ordinal numbers (1st, 2nd, 3rd, etc.)
 */
fun getOrdinal(index: Int): String {
    return when {
        index % 100 in 11..13 -> "${index}th"
        index % 10 == 1 -> "${index}st"
        index % 10 == 2 -> "${index}nd"
        index % 10 == 3 -> "${index}rd"
        else -> "${index}th"
    }
}

/**
 * Helper function to update mission frequency based on frequency set
 */
fun updateMissionFrequency(
    freqValue: Float,
    mission: Mission?,
    missionFrequency: MutableState<MissionFrequency?>
) {
    val freqSet = mission?.frequencySet ?: "COMFORTABLE"

    val newMissionFreq = calculateMissionFrequency(freqValue,freqSet)

    missionFrequency.value = newMissionFreq
}

/**
 * Helper function to update frequency slider value from detailed settings
 */
fun updateFrequencyFromDetail(
    mission: MutableState<Mission?>,
    missionFrequency: MutableState<MissionFrequency?>
) {
    val mf = missionFrequency.value ?: return
    val freqValue = getFrequencyValueFromDetail(mf)

    val freqSet = when {
        freqValue <= 0.25f -> "OCCASIONAL"
        freqValue <= 0.5f -> "COMFORTABLE"
        freqValue <= 0.75f -> "BALANCED"
        else -> "RIGOROUS"
    }

    mission.value = mission.value?.copy(
        frequencySetValue = freqValue,
        frequencySet = freqSet
    )
}

/**
 * Calculate a frequency value (0-1) from mission frequency details
 */
/**
 * Calculate a frequency value (0-1) from mission frequency details
 *
 * Converts a MissionFrequency object back to a 0-1 scale value where:
 * - YEARLY frequencies (1-11 times): 0-0.25 range
 * - MONTHLY frequencies (1-4 times): 0.25-0.5 range
 * - WEEKLY frequencies (1-7 times): 0.5-0.75 range
 * - DAILY frequencies (1-10 times): 0.75-1 range
 *
 * @param mf The MissionFrequency object to convert
 * @return A float value between 0 and 1 representing the frequency
 */
fun getFrequencyValueFromDetail(mf: MissionFrequency): Float {
    return when (mf.frequencyPeriod) {
        "YEARLY" -> {
            // Convert YEARLY (1-11 times) to 0-0.25 range
            val maxYearlyFreq = 11
            val normalizedFreq = minOf(mf.frequency, maxYearlyFreq)
            val percentage = (normalizedFreq - 1) / 10f
            0f + (percentage * 0.25f)
        }

        "MONTHLY" -> {
            // Convert MONTHLY (1-4 times) to 0.25-0.5 range
            val maxMonthlyFreq = 4
            val normalizedFreq = minOf(mf.frequency, maxMonthlyFreq)
            val percentage = (normalizedFreq - 1) / 3f
            0.25f + (percentage * 0.25f)
        }

        "WEEKLY" -> {
            // Convert WEEKLY (1-7 times) to 0.5-0.75 range
            val maxWeeklyFreq = 7
            val normalizedFreq = minOf(mf.frequency, maxWeeklyFreq)
            val percentage = (normalizedFreq - 1) / 6f
            0.5f + (percentage * 0.25f)
        }

        "DAILY" -> {
            // Convert DAILY (1-10 times) to 0.75-1 range
            val maxDailyFreq = 10
            val normalizedFreq = minOf(mf.frequency, maxDailyFreq)
            val percentage = (normalizedFreq - 1) / 9f
            0.75f + (percentage * 0.25f)
        }

        else -> {
            // Default fallback - mid-range of MONTHLY (0.375)
            0.375f
        }
    }
}

// Usage examples:
// val mf1 = MissionFrequency("OCCASIONAL", 1, "YEARLY", "TIMES", "Y", false, 1L)
// val value1 = getFrequencyValueFromDetail(mf1)  // Should return 0.0f
//
// val mf2 = MissionFrequency("OCCASIONAL", 6, "YEARLY", "TIMES", "Y", false, 1L)
// val value2 = getFrequencyValueFromDetail(mf2)  // Should return 0.125f
//
// val mf3 = MissionFrequency("COMFORTABLE", 1, "MONTHLY", "TIMES", "Y", false, 1L)
// val value3 = getFrequencyValueFromDetail(mf3)  // Should return 0.25f
//
// val mf4 = MissionFrequency("BALANCED", 4, "WEEKLY", "TIMES", "Y", false, 1L)
// val value4 = getFrequencyValueFromDetail(mf4)  // Should return 0.625f
//
// val mf5 = MissionFrequency("RIGOROUS", 10, "DAILY", "TIMES", "Y", true, 1L)
// val value5 = getFrequencyValueFromDetail(mf5)  // Should return 1.0f

/**
 * Represents a mission frequency configuration
 */
data class MissionFrequency(
    val frequencySetName: String,
    val frequency: Int,
    val frequencyPeriod: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val frequencyUnit: String = "TIMES",
    val active: String = "Y",
    val isDailyHabit: Boolean = false,
    val missionId: Long = 0L
)

/**
 * Calculates MissionFrequency object based on an input value between 0 and 1
 *
 * - OCCASIONAL (0-0.25): 1-11 times yearly
 * - COMFORTABLE (0.25-0.5): 1-4 times monthly
 * - BALANCED (0.5-0.75): 1-7 times weekly
 * - RIGOROUS (0.75-1): 1-10 times daily
 *
 * @param freqValue Input frequency value from 0 to 1
 * @param freqSet Name of the frequency set
 * @param mission Optional mission object to extract missionId from
 * @return A MissionFrequency object with the calculated values
 */
fun calculateMissionFrequency(
    freqValue: Float,
    freqSet: String,
    mission: Mission? = null
): MissionFrequency {
    // Ensure the value is within bounds
    val clampedValue = freqValue.coerceIn(0f, 1f)

    return when {
        // OCCASIONAL: 0-0.25 range
        clampedValue < 0.25f -> {
            // Map 0-0.25 to 1-11 times yearly
            val timesPerYear = (1 + (clampedValue / 0.25f * 10)).toInt()

            MissionFrequency(
                frequencySetName = freqSet,
                frequency = timesPerYear,
                frequencyPeriod = "YEARLY",
                frequencyUnit = "TIMES",
                active = "Y",
                isDailyHabit = false,
                missionId = mission?.missionId ?: 0L
            )
        }

        // COMFORTABLE: 0.25-0.5 range
        clampedValue < 0.5f -> {
            // Map 0.25-0.5 to 1-4 times monthly
            val timesPerMonth = (1 + ((clampedValue - 0.25f) / 0.25f * 3)).toInt()

            MissionFrequency(
                frequencySetName = freqSet,
                frequency = timesPerMonth,
                frequencyPeriod = "MONTHLY",
                frequencyUnit = "TIMES",
                active = "Y",
                isDailyHabit = false,
                missionId = mission?.missionId ?: 0L
            )
        }

        // BALANCED: 0.5-0.75 range
        clampedValue < 0.75f -> {
            // Map 0.5-0.75 to 1-7 times weekly
            val timesPerWeek = (1 + ((clampedValue - 0.5f) / 0.25f * 6)).toInt()

            MissionFrequency(
                frequencySetName = freqSet,
                frequency = timesPerWeek,
                frequencyPeriod = "WEEKLY",
                frequencyUnit = "TIMES",
                active = "Y",
                isDailyHabit = false,
                missionId = mission?.missionId ?: 0L
            )
        }

        // RIGOROUS: 0.75-1 range
        else -> {
            // Map 0.75-1 to 1-10 times daily
            val timesPerDay = (1 + ((clampedValue - 0.75f) / 0.25f * 9)).toInt()

            MissionFrequency(
                frequencySetName = freqSet,
                frequency = timesPerDay,
                frequencyPeriod = "DAILY",
                frequencyUnit = "TIMES",
                active = "Y",
                isDailyHabit = true, // Only set to true for daily habits
                missionId = mission?.missionId ?: 0L
            )
        }
    }
}

// Usage examples:
// calculateMissionFrequency(0.0f, "OCCASIONAL", myMission)   // 1 time yearly
// calculateMissionFrequency(0.125f, "OCCASIONAL", myMission) // 6 times yearly
// calculateMissionFrequency(0.25f, "COMFORTABLE", myMission) // 1 time monthly
// calculateMissionFrequency(0.375f, "COMFORTABLE", myMission) // 2 times monthly
// calculateMissionFrequency(0.5f, "BALANCED", myMission)   // 1 time weekly
// calculateMissionFrequency(0.625f, "BALANCED", myMission) // 4 times weekly
// calculateMissionFrequency(0.75f, "RIGOROUS", myMission)  // 1 time daily
// calculateMissionFrequency(0.875f, "RIGOROUS", myMission) // 5 times daily
// calculateMissionFrequency(1.0f, "RIGOROUS", myMission)   // 10 times daily