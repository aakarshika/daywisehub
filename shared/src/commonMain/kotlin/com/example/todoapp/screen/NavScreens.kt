package com.example.todoapp.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.unit.dp
import com.example.todoapp.app.MyScreen
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.metrics.MetricsScreen
import com.example.todoapp.screen.missions.MissionsControllerScreen
import com.example.todoapp.screen.missions.MissionsControllerViewModel
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.rememberNavigator
import moe.tlaster.precompose.navigation.transition.NavTransition



@Composable
fun NavScreens() {
    val navigator = rememberNavigator()
    val modifier = Modifier
    val backStackEntry by navigator.currentEntry.collectAsState(null)


    // Get the name of the current screen
    val currentScreen = MyScreen.valueOf(
        backStackEntry?.route?.route ?: MyScreen.Missions.name
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == MyScreen.Missions,
                    onClick = { navigator.navigate(MyScreen.Missions.name) },
                    label = { Text("Missions") },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Missions") }
                )
                NavigationBarItem(
                    selected = currentScreen == MyScreen.Diary,
                    onClick = { navigator.navigate(MyScreen.Diary.name) },
                    label = { Text("Diary") },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Diary") }
                )
                NavigationBarItem(
                    selected = currentScreen == MyScreen.Calendar,
                    onClick = { navigator.navigate(MyScreen.Calendar.name) },
                    label = { Text("Metrics") },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Metrics") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(10.dp)) {
            NavHost(
                navigator = navigator,
                navTransition = NavTransition(),
                initialRoute = MyScreen.Missions.name
            ) {
                scene(
                    route = MyScreen.Missions.name,
                    navTransition = NavTransition()
                ) {
                    MissionsControllerScreen(KoinF.di?.get<MissionsControllerViewModel>()!!)
                }
                scene(
                    route = MyScreen.Diary.name,
                    navTransition = NavTransition()
                ) {
                    DiaryScreen()
                }
                scene(
                    route = MyScreen.Calendar.name,
                    navTransition = NavTransition()
                ) {
                    MetricsScreen()
                }
            }
        }
    }
}



@Composable
fun DiaryScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Diary Screen")
    }
}
