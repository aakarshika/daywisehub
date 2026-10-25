package com.example.todoapp.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.NavScreens
import com.example.todoapp.screen.init.InitViewModel
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import moe.tlaster.precompose.PreComposeApp

enum class MyScreen{
    Missions,
    Diary,
    Calendar
}

@Composable
fun App(){
    PreComposeApp {
        KoinF.setupKoin()

        val initViewModel = KoinF.di?.get<InitViewModel>()!!
        val isReady by initViewModel.isReady.collectAsState()

        LaunchedEffect(Unit) {
            initViewModel.insertDefaultData()
        }

        Scaffold(
            topBar = {}
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
            if(isReady){
                NavScreens(KoinF.di?.get<CalDiaryViewModel>()!!)
            }
            }
        }
    }
}
