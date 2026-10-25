package com.example.todoapp.screen.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.Orange80
import com.example.todoapp.screen.basicutils.Red80
import com.example.todoapp.screen.basicutils.Yellow180
import com.example.todoapp.screen.diary.diaryitem.components.DiaryMilestoneSection
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeViewModel
import kotlinx.datetime.LocalDate

@Composable
fun DiaryScreen(
    diaryViewModel: DiaryViewModel,
    moodViewModel: MoodViewModel,
    waterIntakeViewModel: WaterIntakeViewModel,
    selection: LocalDate,
    changeDate: (LocalDate) -> Unit
) {
    val milestones by diaryViewModel.milestones.collectAsState()
    val todayMoodList: List<TodayMoodWithDetails>? by moodViewModel.todayMoodStatuses.collectAsState(emptyList())
    val waterIntake: WaterIntake? by waterIntakeViewModel.waterIntake.collectAsState()

    LaunchedEffect(selection) {
        moodViewModel.loadDailyMoodStatus(selection)
        waterIntakeViewModel.loadWaterIntake(selection)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Light_Yellowww)) {
        DateHeaderItem(selection, changeDate = changeDate)
        HeadingText("MOOD", Red80)
        MoodHeaderItem(todayMoodList, selection, upsertMood = { moodViewModel.upsertTodayMood(it) })
        HeadingText("WATER INTAKE", Blue80)
        WaterIntakeHeaderItem(waterIntake, selection, upsertWaterIntake = { waterIntakeViewModel.upsertWaterIntake(it) }, waterIntakeCompleted = {})
        if (milestones.isNotEmpty()) {
            HeadingText("MILESTONE", Orange80)
            DiaryMilestoneSection(milestones = milestones, onMilestoneChecked = { mwd, achieved ->
                diaryViewModel.updateMilestoneStatus(mwd.milestone, achieved, selection)
            })
        }
        HeadingText("NOTES", Yellow180)
        Notes(waterIntake, selection, upsertWaterIntake = { waterIntakeViewModel.upsertWaterIntake(it) })
        item { Spacer(Modifier.height(120.dp)) }
    }
}
