package com.example.todoapp.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.init.UserViewModel
import com.example.todoapp.screen.init.InitScreen
import com.example.todoapp.screen.init.InitScreen2
import com.example.todoapp.screen.init.InitViewModel
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

        var isInitialized = remember { mutableStateOf(false) }
        var userId = remember { mutableStateOf(-2L) }

        Scaffold(
            topBar = {}
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
            if(isInitialized.value){
                InitScreen2(
                    userViewModel = KoinF.di?.get<UserViewModel>()!!,
                    userId = userId.value
                )
            } else {
                InitScreen(
                    initViewModel = KoinF.di?.get<InitViewModel>()!!,
                    onInitSuccess = { userid->
                        userId.value = userid
                        isInitialized.value = true
                    }
                )
            }
            }
        }
    }
}
