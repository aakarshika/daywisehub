package com.example.todoapp.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.globalViewModels.UserViewModel
import com.example.todoapp.screen.init.InitScreen
import com.example.todoapp.screen.init.InitScreen2
import com.example.todoapp.screen.init.InitViewModel
import moe.tlaster.precompose.PreComposeApp

enum class TaskScreen{
    TaskList,
    TaskItem
}

enum class MyScreen{
    Missions,
    Diary,
    Calendar
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    currentScreen: MyScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { currentScreen.name },
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "back"
                    )
                }
            }
        })
}

@Composable
fun App(){

    PreComposeApp {
        val modifier = Modifier
        KoinF.setupKoin()

        val initViewModel = KoinF.di?.get<InitViewModel>()
        val userViewModel = KoinF.di?.get<UserViewModel>()

        var isInitialized = remember { mutableStateOf(false) }
        var userId = remember { mutableStateOf(-2L) }

        Scaffold(
            topBar = {
            }
        ) { paddingValues ->
            val coroutineScope = rememberCoroutineScope()
            if(isInitialized.value){
                InitScreen2(
                    userViewModel = userViewModel!!,
                    userId = userId.value
                )

            } else {
                InitScreen(
                    initViewModel = initViewModel!!,
                    onInitSuccess = { userid->
                        userId.value = userid
                        isInitialized.value = true
                    }
                )
            }
        }
    }
}

