package com.example.todoapp.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import com.example.todoapp.app.MyScreen
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.diary.DiaryScreen
import com.example.todoapp.screen.diary.DiaryViewModel
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeViewModel
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.metrics.MetricsScreen
import com.example.todoapp.screen.metrics.MetricsViewModel
import com.example.todoapp.screen.missions.MissionsScreen
import com.example.todoapp.screen.missions.MissionsViewModel
import com.example.todoapp.screen.missions.editor.MissionEditorScreen
import com.example.todoapp.screen.missions.editor.MissionEditorViewModel
import kotlinx.coroutines.cancel
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.NavOptions
import moe.tlaster.precompose.navigation.PopUpTo
import moe.tlaster.precompose.navigation.rememberNavigator
import org.jetbrains.compose.resources.painterResource
import org.koin.core.parameter.parametersOf
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.diary_heart_closed
import todoapp.shared.generated.resources.metrics_calendar
import todoapp.shared.generated.resources.target

@Composable
fun NavScreens(calDateViewModel: CalDiaryViewModel) {
    val navigator = rememberNavigator()
    val backStackEntry by navigator.currentEntry.collectAsState(null)
    val currentDate by calDateViewModel.currentDate.collectAsState()
    val today by calDateViewModel.today.collectAsState()

    val currentScreen = MyScreen.valueOf(
        backStackEntry?.route?.route ?: MyScreen.Diary.name
    )

    // Timers pause in the background, so the day is looked at again as soon as the app is back in front.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { calDateViewModel.checkDate() }

    fun navigateTab(screen: MyScreen) {
        // A tab opens on the current day, without waiting for the next timed check.
        calDateViewModel.checkDate()
        navigator.navigate(
            screen.name,
            NavOptions(
                launchSingleTop = true,
                popUpTo = PopUpTo(MyScreen.Diary.name, inclusive = false)
            )
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == MyScreen.Diary,
                    onClick = { if (currentScreen != MyScreen.Diary) navigateTab(MyScreen.Diary) },
                    label = { Text("Diary") },
                    icon = { Icon(painterResource(Res.drawable.diary_heart_closed), tint = Color.Gray,
                        contentDescription = "Diary", modifier = Modifier.size(30.dp)) }
                )
                NavigationBarItem(
                    selected = currentScreen == MyScreen.Missions,
                    onClick = { if (currentScreen != MyScreen.Missions) navigateTab(MyScreen.Missions) },
                    label = { Text("Missions") },
                    icon = { Icon(painterResource(Res.drawable.target), tint = Color.Gray,
                        contentDescription = "Missions", modifier = Modifier.size(30.dp)) }
                )
                NavigationBarItem(
                    selected = currentScreen == MyScreen.Calendar,
                    onClick = { if (currentScreen != MyScreen.Calendar) navigateTab(MyScreen.Calendar) },
                    label = { Text("Metrics") },
                    icon = { Icon(painterResource(Res.drawable.metrics_calendar), tint = Color.Gray,
                        contentDescription = "Metrics", modifier = Modifier.size(30.dp)) }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navigator = navigator,
                initialRoute = MyScreen.Diary.name
            ) {
                scene(route = MyScreen.Missions.name) {
                    // A new one after midnight, so its "today" follows the date, as the Metrics tab's does.
                    var missionsDay by remember { mutableStateOf(today) }
                    val missionsViewModel = remember(missionsDay) { KoinF.di!!.get<MissionsViewModel>() }
                    DisposableEffect(missionsViewModel) {
                        onDispose { missionsViewModel.viewModelScope.cancel() }
                    }
                    // Held here, so the list keeps its place while the editor is open.
                    val missionsList = rememberLazyListState()
                    val editor by missionsViewModel.editor.collectAsState()
                    val target = editor
                    // The open editor lives in the view model, so a new day waits for it to close rather than
                    // dropping a draft. Each opening makes a fresh editor, which reads the date then.
                    LaunchedEffect(today, target) { if (target == null) missionsDay = today }
                    if (target == null) {
                        MissionsScreen(missionsViewModel, missionsList)
                    } else {
                        // A fresh editor for each opening; its saves can't be cancelled, so closing mid-save is safe.
                        val editorViewModel = remember(target) {
                            KoinF.di!!.get<MissionEditorViewModel> { parametersOf(target.missionId) }
                        }
                        DisposableEffect(editorViewModel) {
                            onDispose { editorViewModel.viewModelScope.cancel() }
                        }
                        MissionEditorScreen(editorViewModel, onClose = { missionsViewModel.closeEditor() })
                    }
                }
                scene(route = MyScreen.Diary.name) {
                    val diaryViewModel = remember(currentDate) {
                        KoinF.di!!.get<DiaryViewModel> { parametersOf(currentDate) }
                    }
                    val moodViewModel = remember(currentDate) {
                        KoinF.di!!.get<MoodViewModel> { parametersOf(currentDate) }
                    }
                    val waterIntakeViewModel = remember(currentDate) {
                        KoinF.di!!.get<WaterIntakeViewModel> { parametersOf(currentDate) }
                    }
                    // Nothing owns these view models, so stop their work when the date changes or the diary leaves the screen.
                    DisposableEffect(diaryViewModel, moodViewModel, waterIntakeViewModel) {
                        onDispose {
                            diaryViewModel.viewModelScope.cancel()
                            moodViewModel.viewModelScope.cancel()
                            waterIntakeViewModel.viewModelScope.cancel()
                        }
                    }
                    DiaryScreen(
                        diaryViewModel,
                        moodViewModel,
                        waterIntakeViewModel,
                        currentDate,
                        changeDate = { date -> calDateViewModel.selectDate(date) },
                        openMissions = { navigateTab(MyScreen.Missions) }
                    )
                }
                scene(route = MyScreen.Calendar.name) {
                    // A new one after midnight, so its "today" and "this week" follow the date.
                    val metricsViewModel = remember(today) { KoinF.di!!.get<MetricsViewModel>() }
                    // Nothing owns this view model either, so stop its work when the date changes or the tab leaves the screen.
                    DisposableEffect(metricsViewModel) {
                        onDispose { metricsViewModel.viewModelScope.cancel() }
                    }
                    MetricsScreen(metricsViewModel, openDay = { date ->
                        // The diary shows the date the shared CalDiaryViewModel holds.
                        calDateViewModel.selectDate(date)
                        navigateTab(MyScreen.Diary)
                    })
                }
            }
        }
    }
}
