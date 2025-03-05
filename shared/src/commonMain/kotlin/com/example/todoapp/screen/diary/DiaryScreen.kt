package com.example.todoapp.screen.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.data.mood.WaterIntake
import com.example.todoapp.getTopCalHeight
import com.example.todoapp.screen.diary.diaryitem.components.Blue80
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.Red80
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeViewModel
import com.example.todoapp.screen.metrics.bottomhighlights.Light_Yellowww
import com.example.todoapp.screen.missions.Orange80
import kotlinx.datetime.LocalDate


@Composable
fun DiaryScreen(
    diaryViewModel: DiaryViewModel,
    moodViewModel: MoodViewModel,
    waterIntakeViewModel: WaterIntakeViewModel,
    selection: LocalDate,
    goToTomorrow: () -> Unit,
    goToYesterday: () -> Unit,
    changeDate: (LocalDate) -> Unit
) {
    val selectedTaskList: List<ComboTask>? by diaryViewModel.dayTasks.collectAsState(emptyList())
    val habitTasks: List<ComboTask>? by diaryViewModel.habitList.collectAsState(emptyList())
    val todayMoodList:List<TodayMoodWithDetails>? by moodViewModel.todayMoodStatuses.collectAsState(emptyList())
    val waterIntake:WaterIntake? by waterIntakeViewModel.waterIntake.collectAsState(null)

    val all : List<ComboTask> = (selectedTaskList?.toList() ?: listOf()) + (habitTasks?.toList() ?: listOf())
    val allTasks = all
        .sortedBy { it.missionFrequency?.isDailyHabit == true }
        .sortedBy {  it.todayTask?.taskPageTag!="TOP3" }

    LaunchedEffect(selection, Unit) {
        diaryViewModel.loadTaskDetails()
        moodViewModel.loadDailyMoodStatus(selection)
        waterIntakeViewModel.loadWaterIntake(selection)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Light_Yellowww)
            .padding(top = 10.dp)
    ) {
        val editingTaskMode = remember { mutableStateOf( "ViewItems") }
        val magicMode = remember { mutableStateOf("CHECK") }
        LazyColumn(modifier = Modifier) {
            SundayHeaderItem(selection)
            DateHeaderItem(selection, changeDate = {
                changeDate(it)
            })
            HeadingText("MOOD", Red80)
            MoodHeaderItem(
                todayMoodList,
                selection,
                upsertMood = {newMood: TodayMood ->
                    moodViewModel.upsertTodayMood(newMood)
                }
            )
//          WeatherHeaderItem()
            HeadingText("TO DO LIST", Orange80)
            ExtraLines(1)
            TodoListItems(selection, allTasks,
                editingTaskMode, magicMode, diaryViewModel)
            HeadingText("WATER INTAKE", Blue80)
            WaterIntakeHeaderItem(waterIntake, selection, upsertWaterIntake = {
                waterIntakeViewModel.upsertWaterIntake(it)
            })
            TodoMenuItems(selection, diaryViewModel, editingTaskMode)
            ExtraLines(7)
            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
            item {
                Spacer(modifier = Modifier.height(500.dp))
            }
        }
    }

}
