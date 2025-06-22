package com.example.todoapp.screen.missions

import androidx.compose.runtime.Composable

import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionWithDetails
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.missions.editpopup.OptimizedEditMissionPopup
import kotlin.random.Random

val pillars = listOf(
    Pillar(
        pillarId = 1L,
        pillarUserId = 1L,
        pillarDescription = "Take care of your body. It is the only one you have!",
        pillarBenefits = "Proper attention to physical wellness at least 3-5 times a week is recommended for our body",
        pillarName = "HEALTH",
        uiData = "health_icon",
        pillarFrequency = 4,
        pillarFrequencyPeriod = "WEEKLY",
        pillarFrequencyUnit = "TIMES",
        pillarRelativeImportance = 10
    ),
    Pillar(
        pillarId = 2L,
        pillarUserId = 1L,
        pillarDescription = "Money isn't everything, but it is some things..",
        pillarBenefits = "Improve your skills, stay connected, and constantly walk towards your dreams",
        pillarName = "WEALTH",
        uiData = "wealth_icon",
        pillarFrequency = 3,
        pillarFrequencyPeriod = "WEEKLY",
        pillarFrequencyUnit = "TIMES",
        pillarRelativeImportance = 7
    ),
    Pillar(
        pillarId = 3L,
        pillarUserId = 1L,
        pillarDescription = "We all need love from our family, friends, neighbours, and internet trolls..",
        pillarBenefits = "Keep your social life alive by making it a priority",
        pillarName = "LOVE",
        uiData = "love_icon",
        pillarFrequency = 1,
        pillarFrequencyPeriod = "DAILY",
        pillarFrequencyUnit = "TIMES",
        pillarRelativeImportance = 10
    ),
    Pillar(
        pillarId = 4L,
        pillarUserId = 1L,
        pillarDescription = "Meditate, Create, Dream, Build, Destroy..",
        pillarBenefits = "Mental Peace needs attention. Multi-dimensional growth is needed to feel sustained and alive",
        pillarName = "LIFE",
        uiData = "life_icon",
        pillarFrequency = 4,
        pillarFrequencyPeriod = "WEEKLY",
        pillarFrequencyUnit = "TIMES",
        pillarRelativeImportance = 8
    )
)

val defaultMissionWithDetails: MissionWithDetails = MissionWithDetails(
    mission = Mission(
        missionId = 0L,
        userId = 1L,
        missionTitle = "TODO",
        importanceValue = 0.52f,
        importanceSet = "MEDIUM",
        frequencySetValue = 0.7f,
        frequencySet = "RIGOROUS",
        progressIndicator = "",
        missionMotivation = "",
        progressInitValue = 0f,
        progressInitText = "",
        progressUnit = "",  // 2 glasses of water
        startDate = MyDate("2024-" +
                listOf("10","11","12","09").random() + "-" +
                listOf("01","02","03","04","05","06","07","12","15","15","17","21","23","25","29",).random()
        ),
        endDate = null,
        missionHiddenType = "General"
    ),
    missionFrequency = MissionFrequency(
        missionId = 0L,
        frequencySetName = "RIGOROUS",
        frequency = 2,
        frequencyPeriod = "DAILY",
        frequencyUnit = "TIMES",
        isDailyHabit = true,
        active = "true"
    ),
    pillar = pillars[0]
)


@Composable
fun AddMissionPopup(
    pillarOptions: List<Pillar>,
    pillarTotalValCount: Map<String?, Int>,
    onCloseClicked: () -> Unit,
    onSaveClicked: (MissionWithDetails) -> Unit
) {

//    val randomFloat1 = Random.nextFloat()
    val missionFrequency: MissionFrequency = getMissionFrequency(0.75f)
//    val randomFloat2 = Random.nextFloat()
    val missionImportance: String = getMissionImportance(0.5f)

    val newMission = MissionWithDetails(
        mission = defaultMissionWithDetails.mission?.copy(
            missionTitle = "",
            frequencySetValue = 0.75f,
            frequencySet = missionFrequency.frequencySetName?:"",
            importanceValue = 0.5f,
            importanceSet = missionImportance,
            startDate = MyDate.now()
        ),
        missionFrequency = missionFrequency,
        pillar = pillars.random()
    )
//    val randomFloat1 = Random.nextFloat()
//    val missionFrequency: MissionFrequency = getMissionFrequency(randomFloat1)
//    val randomFloat2 = Random.nextFloat()
//    val missionImportance: String = getMissionImportance(randomFloat2)
//
//    val newMission = MissionWithDetails(
//        mission = defaultMissionWithDetails.mission?.copy(
//            missionTitle = listOf("Gym",
//                "Swimming",
//                "Running",
//                "Marathon Prep",
//                "Drinking Water",
//                "Breathing",
//                "Stupid office",
//                "Brush Teeth",
//                "Make Bed",
//                "Stock Classes",
//                "App Dev",
//                "AWS Certification",
//                "India Job",
//                "LinkedIn",
//                "Akku Kissy",
//                "Dinner Date",
//                "Call Relatives",
//                "Unboxing Video",
//                "Monday Pooja",
//                "Shaadi Album",
//                "Horse Riding",
//                "Meditation",
//                "Yoga",
//                "Mummy Call",
//                "Akku Kissy",
//                "Cooking").random(),
//            frequencySetValue = randomFloat1,
//            frequencySet = missionFrequency.frequencySetName?:"",
//            importanceValue = randomFloat2,
//            importanceSet = missionImportance,
//            startDate = MyDate("2024-" +
//                    listOf("10","11","12","9").random() + "-" +
//                    listOf("1","2","3","4","5","6","7","12","15","15","17","21","23","25","29",).random()
//            )
//        ),
//        missionFrequency = missionFrequency,
//        pillar = pillars.random()
//    )
    OptimizedEditMissionPopup(
        selectedMission = newMission,
        pillarOptions = pillarOptions,
        pillarTotalValCount = pillarTotalValCount,
        onCloseClicked = onCloseClicked,
        onSaveClicked = onSaveClicked
    )
}


private fun getMissionFrequency(frequencySetValue: Float): MissionFrequency {
    val freqSet = if (frequencySetValue in 0f..0.25f) {
        "OCCASIONAL"
    } else if (frequencySetValue in 0.25f..0.5f) {
        "COMFORTABLE"
    } else if (frequencySetValue in 0.5f..0.75f) {
        "BALANCED"
    } else {
        "RIGOROUS"
    }
    val missionFreq: MissionFrequency = if (freqSet == "OCCASIONAL") {
        MissionFrequency(
            frequencySetName = freqSet,
            frequency = 2,
            frequencyPeriod = "MONTHLY",
            frequencyUnit = "TIMES",
            active = "Y",
            isDailyHabit = false,
            missionId = 0L
        )
    } else if (freqSet == "COMFORTABLE") {
        MissionFrequency(
            frequencySetName = freqSet,
            frequency = 1,
            frequencyPeriod = "WEEKLY",
            isDailyHabit = false,
            frequencyUnit = "TIMES",
            active = "Y",
            missionId = 0L
        )
    } else if (freqSet == "BALANCED") {
        MissionFrequency(
            frequencySetName = freqSet,
            frequency = 3,
            frequencyPeriod = "WEEKLY",
            frequencyUnit = "TIMES",
            isDailyHabit = false,
            active = "Y",
            missionId = 0L
        )
    } else {
        MissionFrequency(
            frequencySetName = freqSet,
            frequency = 1,
            frequencyPeriod = "DAILY",
            frequencyUnit = "TIMES",
            active = "Y",
            isDailyHabit = true,
            missionId = 0L
        )
    }

    return missionFreq
}
private fun getMissionImportance(importanceSetValue: Float): String {
    val imp =  if (importanceSetValue in 0f..0.33f) {
        "LOW"
    } else if (importanceSetValue in 0.33f..0.66f) {
        "MEDIUM"
    } else {
        "HIGH"
    }
    return imp
}
