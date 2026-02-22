package com.example.todoapp.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoapp.app.MyScreen
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.diary.DiaryScreen
import com.example.todoapp.screen.diary.DiaryViewModel
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.metrics.MetricsScreen
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeViewModel
import com.example.todoapp.screen.metrics.MetricsViewModel
import com.example.todoapp.screen.metrics.bottomhighlights.BottomHighlightsViewModel
import com.example.todoapp.screen.metrics.bottomhighlights.DateHighlights
import com.example.todoapp.screen.missions.MissionsControllerScreen
import com.example.todoapp.screen.missions.MissionsControllerViewModel
import kotlinx.datetime.LocalDate
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
                    onClick = {
                        if (currentScreen != MyScreen.Diary) {
                            navigateTab(MyScreen.Diary)
                        }
                    },
                    label = { Text("Diary") },
                    icon = { Icon(painterResource(Res.drawable.diary_heart_closed), tint = Color.Gray,
                        contentDescription = "Diary", modifier = Modifier.size(30.dp)) }
                )
                NavigationBarItem(
                    selected = currentScreen == MyScreen.Missions,
                    onClick = {
                        if(currentScreen != MyScreen.Missions) {
                            navigateTab(MyScreen.Missions)
                        }
                    },
                    label = { Text("Missions") },
                    icon = { Icon(painterResource(Res.drawable.target),tint = Color.Gray,modifier = Modifier.size(30.dp), contentDescription = "Missions") }
                )
                NavigationBarItem(
                    selected = currentScreen == MyScreen.Calendar,
                    onClick = {
                        if (currentScreen != MyScreen.Calendar) {
                            navigateTab(MyScreen.Calendar)
                        }
                    },
                    label = { Text("Metrics") },
                    icon = { Icon(painterResource(Res.drawable.metrics_calendar),tint = Color.Gray,modifier = Modifier.size(30.dp), contentDescription = "Metrics") }
                )
            }
        }
    ) { innerPadding ->

        Box(modifier = Modifier) {
            NavHost(
                navigator = navigator,
                initialRoute = MyScreen.Diary.name
            ) {
                scene(
                    route = MyScreen.Missions.name
                ) {
                    MissionsControllerScreen(KoinF.di?.get<MissionsControllerViewModel>()!!)
                }
                scene(route = MyScreen.Diary.name) {
                    Scaffold(
                        topBar = {}
                    ){
                        DiaryScreen(
                            KoinF.di?.get<DiaryViewModel> {
                                parametersOf(currentDate)
                            } ?: error("DiaryViewModel not found"),
                            KoinF.di?.get<MoodViewModel> { parametersOf(currentDate) }!!,
                            KoinF.di?.get<WaterIntakeViewModel> { parametersOf(currentDate) }!!,
                            currentDate,
                            changeDate={ date:LocalDate ->
                                calDateViewModel.selectDate(date)
                            }
                        )
                    }
                }
                scene(
                    route = MyScreen.Calendar.name
                ) {
                    Scaffold(
                        bottomBar = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth().wrapContentHeight()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentSize()
                                        .align(Alignment.BottomCenter)
                                ) {
                                    DateHighlights(
                                        KoinF.di?.get<BottomHighlightsViewModel> {
                                            parametersOf(currentDate)
                                        } ?: error("BottomHighlightsViewModel not found"),
                                        currentDate,
                                        goToDiary = {
                                            navigateTab(MyScreen.Diary)
                                        }
                                    )
                                }
                            }
                        }
                    ) {
                        MetricsScreen(
                            KoinF.di?.get<MetricsViewModel>()!!,
                            currentDate,
                            changeDate = { date: LocalDate ->
                                calDateViewModel.selectDate(date)
                            }
                        )
                    }
                }
            }
        }
    }
}
