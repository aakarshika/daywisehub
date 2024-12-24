package com.example.todoapp.screen.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.getTopCalHeight
import com.example.todoapp.screen.metrics.bottomhighlights.Light_Yellowww
import kotlinx.datetime.LocalDate


@Composable
fun DiaryScreen(
    diaryViewModel: DiaryViewModel,
    selection: LocalDate,
    goToTomorrow: () -> Unit,
    goToYesterday: () -> Unit
) {
    val selectedTaskList: List<ComboTask>? by diaryViewModel.dayTasks.collectAsState(emptyList())
    val habitTasks: List<ComboTask>? by diaryViewModel.habitList.collectAsState(emptyList())

    val all : List<ComboTask> = (selectedTaskList?.toList() ?: listOf()) + (habitTasks?.toList() ?: listOf())
    val allTasks = all
        .sortedBy { it.missionFrequency?.isDailyHabit == true }
        .sortedBy {  it.todayTask?.taskPageTag!="TOP3" }

    LaunchedEffect(selection, Unit) {
        diaryViewModel.loadTaskDetails()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Light_Yellowww)
            .padding(top = getTopCalHeight())
    ) {
        val editingTaskMode = remember { mutableStateOf( "ViewItems") }
        val magicMode = remember { mutableStateOf("CHECK") }
        LazyColumn(modifier = Modifier) {
                MoodHeaderItem(
                    selection
                )
                WeatherHeaderItem()
                ExtraLines(1)
                TodoListItems(selection, allTasks,
                    editingTaskMode, magicMode, diaryViewModel)
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