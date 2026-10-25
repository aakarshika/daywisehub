package com.example.todoapp.screen.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.components.WriteText

/** Stands in for a tab that a later plan rebuilds. */
@Composable
fun ComingNextScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Light_Yellowww).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        WriteText(message, color = InkGray, fontSize = 18f)
    }
}
