package com.example.todoapp.screen.missions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionWithDetails
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneProgressData
import com.example.todoapp.db.data.mission.milestone.MilestoneWithFewDetails
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.basicblocks.FrequencyPeriodButton
import com.example.todoapp.screen.basicblocks.WriteText
import com.example.todoapp.screen.basicblocks.WritingTextField
import com.example.todoapp.screen.missions.calendar.blueprint.MissionBlueprintCalendarScreen

val Orange80 = Color(0xFFFFD4B8)
val Red80 = Color(0xFFFFCBD2)

fun Color.darken(amount: Float = 0.2f): Color {
    val red = (this.red * (1 - amount)).coerceIn(0f, 1f)
    val green = (this.green * (1 - amount)).coerceIn(0f, 1f)
    val blue = (this.blue * (1 - amount)).coerceIn(0f, 1f)

    return Color(red, green, blue, this.alpha)
}

@Composable
fun EditMissionPopup(
    selectedMission: MissionWithDetails,
    pillarOptions: List<Pillar>,
    pillarTotalValCount: Map<String?, Int>,
    onCloseClicked: () -> Unit,
    onSaveClicked: (MissionWithDetails) -> Unit
) {

    val mission = remember { mutableStateOf(selectedMission?.mission) }
    val missionFrequency = remember { mutableStateOf(selectedMission?.missionFrequency) }
    val pillarSelected = remember { mutableStateOf(selectedMission?.pillar) }
    val Pink180 = Color.White.copy(alpha = 0.45f)
    val MediumPillarColor = getPillarColor(pillarSelected.value?.pillarName)
    val DarkPillarColor = MediumPillarColor.darken()
    val milestones = remember { mutableStateOf(selectedMission?.milestones) }

    val expandFrequency = remember { mutableStateOf(false) }

    val pillarTotalInitial = pillarTotalValCount[pillarSelected.value?.pillarName]?.minus(
        if (mission.value?.importanceSet == "LOW") 1 else
            if (mission.value?.importanceSet == "MEDIUM") 2 else
                if (mission.value?.importanceSet == "HIGH") 3 else 0
    )?:0


    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandIn(),
        exit = fadeOut() + shrinkOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .padding(top = 10.dp)
                .height(575.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {

            Column(
                modifier = Modifier
                    .height(575.dp)
//                .shadow(10.dp, spotColor = Color.Black)
                    .background(color = Color.White)
//                        .align(Alignment.CenterEnd)
            ) {
                Row(
                    modifier = Modifier
                        .height(535.dp)
                        .background(color = MediumPillarColor.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Row {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                            ) {

                                Text(
                                    text = "TASK:",
                                    fontSize = 18.sp,
                                    color = Color.Black,
                                    modifier = Modifier.padding(top = 15.dp)
                                )

                                Box(modifier = Modifier.background(Pink180)) {
                                    WritingTextField(
                                        taskName = "${mission.value?.missionTitle}",
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .fillMaxWidth(),
                                        placeHolder = "TODO",
                                        valueChanged = { it ->
                                            mission.value =
                                                mission.value?.copy(missionTitle = it)
                                        },
                                    )
                                }

                                Row {
                                    Text(
                                        text = "IMPORTANCE:",
                                        color = Color.Black,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(modifier = Modifier.weight(1f)) {
                                        val pillarValAdded =
                                            if (mission.value?.importanceSet == "LOW")
                                                1 else if (mission.value?.importanceSet == "MEDIUM")
                                                2 else if (mission.value?.importanceSet == "HIGH") 3 else 0
                                        Column(
                                            modifier = Modifier
                                                .height(20.dp)
                                                .fillMaxWidth()
                                        ) {
                                            pillarOptions.forEach { pillar ->
                                                Row {
                                                    var pillarTotal =
                                                        pillarTotalValCount[pillar.pillarName] ?: 0
                                                    if (pillar.pillarName == pillarSelected.value?.pillarName) {
//                                                    pillarTotal = if(pillarTotalInitial>0) pillarTotalInitial else 0
                                                        repeat(pillarValAdded) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .height(5.dp)
                                                                    .width(10.dp)
                                                                    .background(
                                                                        getPillarColor(pillar.pillarName).darken()
                                                                    )
                                                            )
                                                        }
                                                    }
                                                    repeat(pillarTotal) {
                                                        Box(
                                                            modifier = Modifier
                                                                .height(5.dp)
                                                                .width(10.dp)
                                                                .background(Color.Gray.copy(alpha = 0.4f))
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .height(5.dp)
                                                                    .width(10.dp)
                                                                    .background(
                                                                        getPillarColor(pillar.pillarName).copy(
                                                                            alpha = 0.5f
                                                                        )
                                                                    )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .background(Pink180)
                                        .padding(3.dp)
                                ) {

                                    FrequencyPeriodButton(
                                        text = "LOW",
                                        modifier = Modifier
                                            .wrapContentSize()
                                            .background(if (mission.value?.importanceSet == "LOW") Orange80 else Color.Transparent)
                                            .clickable {
                                                mission.value = mission.value?.copy(
                                                    importanceValue = 0.25f,
                                                    importanceSet = "LOW"
                                                )
                                            }
                                    )
                                    FrequencyPeriodButton(
                                        text = "MEDIUM",
                                        modifier = Modifier
                                            .wrapContentSize()
                                            .background(if (mission.value?.importanceSet == "MEDIUM") Orange80 else Color.Transparent)
                                            .clickable {
                                                mission.value = mission.value?.copy(
                                                    importanceValue = 0.50f,
                                                    importanceSet = "MEDIUM"
                                                )
                                            }
                                    )
                                    FrequencyPeriodButton(
                                        text = "HIGH",
                                        modifier = Modifier
                                            .wrapContentSize()
                                            .background(if (mission.value?.importanceSet == "HIGH") Orange80 else Color.Transparent)
                                            .clickable {
                                                mission.value = mission.value?.copy(
                                                    importanceValue = 0.70f,
                                                    importanceSet = "HIGH"
                                                )
                                            }
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .weight(0.4f)
                            ) {

                                var currentIndex by remember { mutableStateOf(0) }


                                // xPILLAR__ HEALTH
                                FrequencyPeriodButton(
                                    text = "${pillarSelected.value?.pillarName}",
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .height(40.dp)
                                        .background(Orange80)
                                        .clickable {
                                            val currentIndex =
                                                pillarOptions.indexOf(pillarSelected.value)
                                            val nextIndex =
                                                (currentIndex + 1) % pillarOptions.size
                                            pillarSelected.value =
                                                pillarOptions[nextIndex].copy()
                                        }
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Pink180)
                                        .height(115.dp)
                                        .fillMaxWidth()
                                ) {
                                    Column {
                                        Icon(
                                            Icons.Default.Face,
                                            "asd",
                                            tint = DarkPillarColor,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .align(Alignment.CenterHorizontally)
                                        )
                                        Text(
                                            "\"${pillarSelected.value?.pillarDescription}\"",
                                            fontSize = 13.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(40.dp)
                                                .align(Alignment.CenterHorizontally)
                                        )
                                        //4 dots that highlight based on pillar chosen
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                                .weight(1f)
                                                .align(Alignment.CenterHorizontally)
                                        ) {
                                            Row(modifier = Modifier.align(Alignment.BottomCenter)) {
                                                pillarOptions.forEach { pill ->
                                                    Icon(
                                                        Icons.Default.AddCircle,
                                                        "asd",
                                                        tint = getPillarColor(pill.pillarName).darken(),
                                                        modifier = Modifier
                                                            .size(15.dp)
                                                            .padding(if (pillarSelected.value?.pillarId == pill.pillarId) 1.dp else 3.dp)
                                                            .clickable {
                                                                pillarSelected.value = pill
                                                            }
                                                    )
                                                }
                                            }
                                        }

                                    }

                                }
                            }
                        }



                        fun frequencyUpdate(): String {

                            val freqSet = if (mission.value?.frequencySetValue ?: 0f in 0f..0.25f) {
                                "OCCASIONAL"
                            } else if (mission.value?.frequencySetValue ?: 0f in 0.25f..0.5f) {
                                "COMFORTABLE"
                            } else if (mission.value?.frequencySetValue ?: 0f in 0.5f..0.75f) {
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
                                    frequencyUnit = "TIMES",
                                    isDailyHabit = false,
                                    active = "Y",
                                    missionId = 0L
                                )
                            } else if (freqSet == "BALANCED") {
                                MissionFrequency(
                                    frequencySetName = freqSet,
                                    frequency = 3,
                                    frequencyPeriod = "WEEKLY",
                                    isDailyHabit = false,
                                    frequencyUnit = "TIMES",
                                    active = "Y",
                                    missionId = 0L
                                )
                            } else {
                                MissionFrequency(
                                    frequencySetName = freqSet,
                                    frequency = 1,
                                    frequencyPeriod = "DAILY",
                                    isDailyHabit = true,
                                    frequencyUnit = "TIMES",
                                    active = "Y",
                                    missionId = 0L
                                )
                            }

                            missionFrequency.value = missionFreq
                            return freqSet

                        }
                        // Frequency Set
                        Row {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.9f)
                            ) {
                                Text(text = "FREQUENCY:", color = Color.Black)

                            }

                            Icon(
                                if (expandFrequency.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                "asdsfgd",
                                tint = Color.Black,
                                modifier = Modifier
                                    .clickable {
                                        expandFrequency.value = !expandFrequency.value
                                    }
                                    .size(24.dp)
                                    .weight(0.1f)
                            )
                        }

                        if (expandFrequency.value) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width((7 * 22).dp)
                                        .height(150.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.End)
                                    ) {
                                        MissionBlueprintCalendarScreen(mission, pillarSelected)
                                    }
                                }
                                Column{
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.End)
                                    ) {

                                        FrequencyPeriodButton(
                                            text = if(missionFrequency.value?.isDailyHabit==true) "DAILY HABIT" else "MISSION",
                                            modifier = Modifier
                                                .height(40.dp)
                                                .background(Orange80)
                                                .clickable {
                                                    missionFrequency.value= missionFrequency.value?.copy(
                                                        isDailyHabit = !(missionFrequency.value?.isDailyHabit?:false)
                                                    )
                                                }
                                        )
                                    }
                                }
                            }
                        }
                        FrequencySlider(
                            mission = mission,
                            pillar = pillarSelected.value,
                            onValueChange = { ff ->
                                mission.value = mission.value?.copy(
                                    frequencySetValue = ff
                                )

                                val freqSet = frequencyUpdate()
                                mission.value = mission.value?.copy(
                                    frequencySet = freqSet
                                )
                            }
                        )

                        if (expandFrequency.value) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Pink180)
                                    .padding(3.dp)
                            )
                            {
                                WriteText(text = "Approx.  ", color = Color.Black)
                                WritingTextField(
                                    taskName = "${missionFrequency.value?.frequency.toString()}",
                                    valueChanged = { ff ->
                                        missionFrequency.value =
                                            missionFrequency.value?.copy(frequency = ff.toInt())
                                        val missionFreqSetValue =
                                            getReverseFrequencyValue(missionFrequency.value!!)

                                        val freqSet = if (missionFreqSetValue in 0f..0.25f) {
                                            "OCCASIONAL"
                                        } else if (missionFreqSetValue in 0.25f..0.5f) {
                                            "COMFORTABLE"
                                        } else if (missionFreqSetValue in 0.5f..0.75f) {
                                            "BALANCED"
                                        } else {
                                            "RIGOROUS"
                                        }
                                        mission.value = mission.value?.copy(
                                            frequencySetValue = missionFreqSetValue,
                                            frequencySet = freqSet
                                        )
                                    },
                                    placeHolder = "How many?",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                )


                                val frequencyUnits = listOf("TIMES", "HOURS")
                                val frequencyPeriods = listOf("MONTHLY", "WEEKLY", "DAILY")

                                FrequencyPeriodButton(
                                    text = "${missionFrequency.value?.frequencyUnit}",
                                    modifier = Modifier
                                        .wrapContentSize()
                                        .background(DarkPillarColor)
                                        .clickable {
                                            val currentIndex =
                                                frequencyUnits.indexOf(missionFrequency.value?.frequencyUnit)
                                            val nextIndex =
                                                (currentIndex + 1) % frequencyUnits.size
                                            missionFrequency.value =
                                                missionFrequency.value?.copy(frequencyUnit = frequencyUnits[nextIndex])
                                            val missionFreqSetValue =
                                                getReverseFrequencyValue(missionFrequency.value!!)

                                            val freqSet =
                                                if (missionFreqSetValue in 0f..0.25f) {
                                                    "OCCASIONAL"
                                                } else if (missionFreqSetValue in 0.25f..0.5f) {
                                                    "COMFORTABLE"
                                                } else if (missionFreqSetValue in 0.5f..0.75f) {
                                                    "BALANCED"
                                                } else {
                                                    "RIGOROUS"
                                                }
                                            mission.value = mission.value?.copy(
                                                frequencySetValue = missionFreqSetValue,
                                                frequencySet = freqSet
                                            )
                                        }
                                )

                                FrequencyPeriodButton(
                                    text = "${missionFrequency.value?.frequencyPeriod}",
                                    modifier = Modifier
                                        .wrapContentSize()
                                        .background(DarkPillarColor)
                                        .clickable {
                                            val currentIndex =
                                                frequencyPeriods.indexOf(missionFrequency.value?.frequencyPeriod)
                                            val nextIndex =
                                                (currentIndex + 1) % frequencyPeriods.size
                                            missionFrequency.value =
                                                missionFrequency.value?.copy(frequencyPeriod = frequencyPeriods[nextIndex])
                                            val missionFreqSetValue =
                                                getReverseFrequencyValue(missionFrequency.value!!)
                                            mission.value = mission.value?.copy(
                                                frequencySetValue = missionFreqSetValue
                                            )

                                            val freqSet =
                                                if (missionFreqSetValue in 0f..0.25f) {
                                                    "OCCASIONAL"
                                                } else if (missionFreqSetValue in 0.25f..0.5f) {
                                                    "COMFORTABLE"
                                                } else if (missionFreqSetValue in 0.5f..0.75f) {
                                                    "BALANCED"
                                                } else {
                                                    "RIGOROUS"
                                                }
                                            mission.value = mission.value?.copy(
                                                frequencySet = freqSet
                                            )
                                        }
                                )
                            }
                        } else {

                            Row(
                                modifier = Modifier
                                    .background(Pink180)
                                    .padding(3.dp)
                            )
                            {
                                WriteText(
                                    text = "Approx. ${missionFrequency.value?.frequency.toString()} ${missionFrequency.value?.frequencyUnit}" +
                                            " ${missionFrequency.value?.frequencyPeriod}",
                                    color = Color.Black,
                                    modifier = Modifier.fillMaxWidth()
                                )

                            }
                        }
                        Row {
                            Text(
                                text = "MILESTONES:",
                                color = Color.Black,
                                modifier = Modifier.weight(1f).padding(top=5.dp)
                            )

                        }
                        // Milestones List
                        milestones.value?.forEachIndexed { index, milestoneWithDetails ->
                            val mile = milestoneWithDetails.milestone
                            val milestoneProgressData = milestoneWithDetails.milestoneProgressData

                            Column(modifier = Modifier) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(0.9f)) {
                                        Box(modifier = Modifier.background(Pink180)) {
                                            WritingTextField(
                                                taskName = mile.text,
                                                valueChanged = {
                                                    milestones.value =
                                                        milestones.value?.mapIndexed { i, m ->
                                                            if (i == index)
                                                                MilestoneWithFewDetails(
                                                                    milestone = mile.copy(text = it),
                                                                    milestoneProgressData = milestoneProgressData
                                                                )
                                                            else milestones.value!![i]
                                                        }
                                                },
                                                placeHolder = "Looking forward to",
                                                modifier = Modifier.wrapContentSize()

                                            )
                                        }
                                        Row {
                                            WriteText(" due on ")

                                            Box(modifier = Modifier.background(Pink180)) {
                                                WritingTextField(
                                                    taskName = mile.expectedCompletionDate.dateString,
                                                    valueChanged = { xx ->
                                                        milestones.value =
                                                            milestones.value?.mapIndexed { i, m ->
                                                                if (i == index)
                                                                    MilestoneWithFewDetails(
                                                                        milestone = mile.copy(
                                                                            expectedCompletionDate = MyDate(
                                                                                xx
                                                                            )
                                                                        ),
                                                                        milestoneProgressData = milestoneProgressData
                                                                    )
                                                                else milestones.value!![i]
                                                            }
                                                    },
                                                    placeHolder = "Expected to complete on",
                                                    modifier = Modifier.wrapContentSize()

                                                )
                                            }
                                        }
                                    }
                                    Box(modifier = Modifier.weight(0.1f)) {
                                        // Remove Milestone Button
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove Milestone",
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .align(Alignment.CenterEnd)
                                                    .clickable {
//                                                    milestones.value.removeAt(index)
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Milestone",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {

                                        val newMilestone = Milestone(
                                            text = "My ${getOrdinal((milestones.value?.size ?: 0) + 1)} Milestone",
                                            priority = "HIGH",
                                            expectedCompletionDate = MyDate("2022-01-01"),
                                            milestoneOrder = milestones.value?.size ?: 1,
                                            status = "ACTIVE",
                                            missionId = mission.value?.missionId ?: 0L
                                        )
                                        val milestoneProgressData = MilestoneProgressData(
                                            progressValue = 0f,
                                            progressUnit = 0f,
                                            progressText = "",
                                            mileStartDate = MyDate("2022-01-01"),
                                            actualCompletionDate = null,
                                            completionTaskId = null,
                                            milestoneId = 0L
                                        )
                                        milestones.value = milestones.value?.plus(
                                            MilestoneWithFewDetails(
                                                milestone = newMilestone,
                                                milestoneProgressData = milestoneProgressData
                                            )
                                        )

                                    }
                            )
                        }
                        Row {
//                                Button(onClick = onCloseClicked) {
//                                    Text("Close")
//                                }
                        }

                        Row {
                            Spacer(modifier = Modifier.height(60.dp))
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = MediumPillarColor)
                        .padding(bottom = 10.dp, end = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Bottom)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            modifier = Modifier
                                .size(25.dp)
                                .align(BottomEnd)
                                .clickable { onCloseClicked() },
                            contentDescription = "cancel Adding"
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Bottom)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            modifier = Modifier
                                .size(25.dp)
                                .align(BottomEnd)
                                .clickable {
                                    pillarSelected.value?.let { it2 ->
                                        missionFrequency.value?.let { it3 ->
                                            mission.value?.let { it4 ->
                                                MissionWithDetails(
                                                    mission = it4,
                                                    missionFrequency = it3,
                                                    pillar = it2,
                                                    milestones = milestones.value
                                                )
                                            }
                                        }
                                    }
                                    ?.let {
                                        onSaveClicked(
                                            it
                                        )
                                    }
                                },
                            contentDescription = "Done Adding"
                        )
                    }
                }

            }
        }
    }
}

fun getReverseFrequencyValue(mf: MissionFrequency): Float {
    var period = mf.frequencyPeriod
    var unit = mf.frequencyUnit
    var freqDaily: Float = 0.5f
    var freq = mf.frequency
    when (period){
        "MONTHLY" -> {
            freqDaily = freq / 28.0f
        }
        "WEEKLY" -> {
            freqDaily = freq / 7.0f
        }
        "DAILY" -> {
        }
    }
    when (unit){
        "TIMES" -> {
            freqDaily = freqDaily
        }
        "HOURS" -> {
            freqDaily = freqDaily
        }
    }

    if (freqDaily > 1){
        freqDaily = 0.99f
    }
    return freqDaily
}

fun getOrdinal(index: Int): String {
    return when {
        index % 100 in 11..13 -> "${index}th"
        index % 10 == 1 -> "${index}st"
        index % 10 == 2 -> "${index}nd"
        index % 10 == 3 -> "${index}rd"
        else -> "${index}th"
    }
}


@Composable
fun FrequencySlider(
    mission: MutableState<Mission?>,
    pillar: Pillar?,
    onValueChange: (Float) -> Unit
) {
    val points = listOf(0f, 0.125f, 0.25f, 0.375f, 0.5f, 0.625f, 0.75f, 0.875f, 1f) // Points with labels
    val mainLabels = listOf("A", "B", "C", "D", "E") // Main points
    val intermediateLabels = listOf("OCCASIONAL", "COMFORTABLE", "BALANCED", "RIGOROUS") // Intermediate points

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.45f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Draw labels for all points
            points.forEachIndexed { index, point ->
                Box(modifier = Modifier.background(if(mission.value?.frequencySetValue?: 0f >= point-0.125 && mission.value?.frequencySetValue?: 0f < point+0.124)
                    getPillarColor(pillar?.pillarName).darken()
                else Color.Transparent)) {
                    Text(
                        text = when (index) {
                            0 -> ""
                            2 -> ""
                            4 -> ""
                            6 -> ""
                            8 -> ""
                            else -> intermediateLabels[(index - 1) / 2]
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = if (index == 0) 0.dp else 4.dp)
                    )
                }
            }
        }
        Slider(
            value = mission.value?.frequencySetValue ?: 0f,
            onValueChange = { onValueChange(it) },
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = getPillarColor(pillar?.pillarName),             // Custom color for the thumb (ball)
                activeTrackColor = getPillarColor(pillar?.pillarName).darken(),     // Color of the active track
                inactiveTrackColor = Color.White.copy(alpha = 0.45f),   // Color of the inactive track
            )
        )
    }
}