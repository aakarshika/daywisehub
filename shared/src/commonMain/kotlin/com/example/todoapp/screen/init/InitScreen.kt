package com.example.todoapp.screen.init

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun InitScreen(
    initViewModel: InitViewModel ,
    onInitSuccess: (userId: Long) -> Unit
) {
    val loginStatus by initViewModel.loginStatus.collectAsState(null)

    LaunchedEffect(Unit) {
        initViewModel.loadLoginState()
    }

    LaunchedEffect(loginStatus) {
        if (loginStatus?.isLoggedIn == true && loginStatus?.userId !=null) {
           onInitSuccess(loginStatus!!.userId)
        }
    }
    var username by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Sign Up", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            ////////////////////////////////
            initViewModel.signup(username)
            ////////////////////////////////
        }) {

            Text("Sign Up")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (loginStatus?.isLoggedIn == true && loginStatus?.userId !=null) {
            Text(text = "SUCCESS", color = Color.Green)
        } else{
            Text(text = "waiting", color = Color.Green)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {}) {
            Text("Already have an account? Restore from Backup")
        }
    }
}
