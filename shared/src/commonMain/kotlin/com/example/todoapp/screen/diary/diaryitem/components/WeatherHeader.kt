package com.example.todoapp.screen.diary.diaryitem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.example.todoapp.screen.missions.Blue180
import com.example.todoapp.screen.missions.Blue80


@Composable
fun WeatherHeader() {

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)) {
        Row {
            Column(
                modifier = Modifier.weight(2f)
                    .padding(start = 10.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.End).fillMaxHeight()
                ){
                    listOf(1,2,3,4,5,6).forEach {
                        Icon(
                            tint = Blue180,
                            painter = rememberVectorPainter(Icons.Default.Face),
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterVertically),
                            contentDescription = "mood-icon-smile"
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .weight(1f)
                        .padding(2.dp)
                        .background(Blue80)
                )
                Box(
                    modifier = Modifier.fillMaxSize()
                        .weight(1.5f)
                        .padding(0.dp)
                        .background(Blue80)
                ){
                    Text(
                        text = "WEATHER",
                        modifier = Modifier
                            .align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize()
                        .weight(1f)
                        .padding(2.dp)
                        .background(Blue80)
                )
            }
        }
    }
}
