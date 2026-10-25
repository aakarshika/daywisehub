package com.example.todoapp.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoapp.app.MyScreen
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.diary.DiaryScreen
import com.example.todoapp.screen.diary.DiaryViewModel
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeViewModel
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.placeholder.ComingNextScreen
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

    val currentScreen = MyScreen.valueOf(
        backStackEntry?.route?.route ?: MyScreen.Diary.name
    )

    fun navigateTab(screen: MyScreen) {
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
                    ComingNextScreen("Missions are being rebuilt around your pillars. They come back in the next step.")
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
                    DiaryScreen(
                        diaryViewModel,
                        moodViewModel,
                        waterIntakeViewModel,
                        currentDate,
                        changeDate = { date -> calDateViewModel.selectDate(date) }
                    )
                }
                scene(route = MyScreen.Calendar.name) {
                    ComingNextScreen("Metrics come back after missions, with pillar rings and a week calendar.")
                }
            }
        }
    }
}
