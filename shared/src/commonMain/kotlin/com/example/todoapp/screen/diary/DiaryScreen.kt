package com.example.todoapp.screen.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.Red80
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeViewModel
import com.example.todoapp.screen.diary.planning.PlanningList
import com.example.todoapp.screen.diary.planning.PlanningViewModel
import com.example.todoapp.screen.metrics.bottomhighlights.Light_Yellowww
import com.example.todoapp.screen.missions.Blue80
import com.example.todoapp.screen.missions.Orange80
import com.example.todoapp.screen.missions.Yellow180
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
    val planningMode = remember { mutableStateOf(false) }

    val all : List<ComboTask> = (selectedTaskList?.toList() ?: listOf()) //+ (habitTasks?.toList() ?: listOf())

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
            .wrapContentSize()
            .background(Light_Yellowww)
            .padding(top = 10.dp)
    ) {
        if (allTasks.isNotEmpty() && !planningMode.value) {
            val editingTaskMode = remember { mutableStateOf("ViewItems") }
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
                    upsertMood = { newMood: TodayMood ->
                        moodViewModel.upsertTodayMood(newMood)
                    }
                )
//          WeatherHeaderItem()
                HeadingText("TO DO LIST", Orange80)
                ExtraLines(1)
                TodoListItems(
                    selection, allTasks,
                    editingTaskMode, magicMode, diaryViewModel
                )
                HeadingText("WATER INTAKE", Blue80)
                WaterIntakeHeaderItem(waterIntake, selection, upsertWaterIntake = {
                    waterIntakeViewModel.upsertWaterIntake(it)
                })
                HeadingText("NOTES", Yellow180)
                Notes(waterIntake, selection, upsertWaterIntake = {
                    waterIntakeViewModel.upsertWaterIntake(it)
                })
                TodoMenuItems(selection, diaryViewModel, editingTaskMode)
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.size(30.dp).background(Color.Green)
                        .clickable {
                            planningMode.value = true
                        })
                }
                item {
                    Spacer(modifier = Modifier.height(500.dp))
                }
            }
        } else {
            LazyColumn(modifier = Modifier) {
                SundayHeaderItem(selection)
                DateHeaderItem(selection, changeDate = {
                    changeDate(it)
                })
//                item{
//                }
//                item {
//                    Spacer(modifier = Modifier.height(500.dp))
//                }
            }

            Planner(
                planningMode,
                todaysDate = selection,
                back = {
                    planningMode.value = false
                },
                done = {
                    planningMode.value = false
                }
            )
        }
    }
}

@Composable
fun Planner(planningMode: MutableState<Boolean>,todaysDate: LocalDate, back : () -> Unit, done : () -> Unit) {
    Box {
        if (!planningMode.value) {
            Box(
                modifier = Modifier
                    .size(500.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Text("No tasks added yet")
                        Box(
                            modifier = Modifier
                                .clickable {
                                    planningMode.value = true
                                }
                                .wrapContentSize()
                                .background(Blue80)
                                .padding(15.dp)
                        ) {
                            Text("Let's plan your day")
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Let's pla",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
//                            IconButton(
//                                onClick = { back() },
//                                modifier = Modifier
//                                    .size(36.dp)
//                                    .background(
//                                        color = MaterialTheme.colorScheme.surfaceVariant,
//                                        shape = CircleShape
//                                    )
//                            ) {
//                                Icon(
//                                    Icons.Default.Close,
//                                    contentDescription = "Close",
//                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                            }

                            Button(
                                onClick = { done() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Done")
                            }
                        }
                    }

                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Box(
                        modifier = Modifier
                            .wrapContentHeight()
                    ) {
                        PlanningList(
                            todaysDate,
                            KoinF.di?.get<PlanningViewModel>()!!
                        )
                    }
                }
            }
        }
    }
}
