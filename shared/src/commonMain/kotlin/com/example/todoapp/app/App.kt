package com.example.todoapp.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.sample.SampleSeed
import com.example.todoapp.di.KoinF
import com.example.todoapp.reminders.ReminderSync
import com.example.todoapp.screen.NavScreens
import com.example.todoapp.screen.init.InitViewModel
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.widget.WidgetSync
import moe.tlaster.precompose.PreComposeApp

enum class MyScreen{
    Missions,
    Diary,
    Calendar
}

/**
 * [sample] is dev-only: the Android debug build passes it from a launch extra.
 * [devTools] shows the dev clock; debug builds on both platforms pass it.
 */
@Composable
fun App(sample: SampleSeed? = null, devTools: Boolean = false){
    PreComposeApp {
        KoinF.setupKoin()

        val initViewModel = KoinF.di?.get<InitViewModel>()!!
        val isReady by initViewModel.isReady.collectAsState()
        val startOnMissions by initViewModel.startOnMissions.collectAsState()

        LaunchedEffect(Unit) {
            initViewModel.insertDefaultData(sample)
        }

        Scaffold(
            topBar = {}
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
            if(isReady){
                LaunchedEffect(Unit) { KoinF.di?.get<ReminderSync>()?.start() }
                // Widgets follow the app's changes while it runs; each platform says how to reload them.
                LaunchedEffect(Unit) { KoinF.di?.get<WidgetSync>()?.start(this) }
                NavScreens(KoinF.di?.get<CalDiaryViewModel>()!!, startOnMissions)
                if (devTools) DevClockPill(Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
            }
            }
        }
    }
}
