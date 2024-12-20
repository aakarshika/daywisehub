package com.example.todoapp.screen.metrics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp



@Composable
fun ListHeader(text:String,modifier : Modifier) {
    Box(modifier = modifier
        .fillMaxWidth()
        .wrapContentHeight()) {
        Box(modifier = Modifier.fillMaxWidth()
            .align(Alignment.Center)
            .padding(end = 20.dp, start = 20.dp)
            .height(1.dp)
            .background(Light_Mint)) {
        }
        Box(modifier = Modifier.fillMaxWidth()
            .padding(start = 50.dp, end = 50.dp)
            .background(Orange80)) {

            Text(
                text = text,
                modifier = Modifier
                    .align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
val Orange80 = Color(0xFFFFD4B8)
val Light_Mint =  Color(0xFFA3CEFC)
