package com.example.todoapp.screen.metrics.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val columnWeights = listOf(1f, 5f, 2f)
val columnNames = listOf("Reminder", "", "Top", "Habit")
@Composable
fun PlannerHeadingItems(doStuff: (String)-> Unit ) {
    Box(modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 20.dp)){
        Row(modifier = Modifier.fillMaxWidth()
        ){
            columnWeights.forEachIndexed { i, item->
                Box(
                    modifier = Modifier.wrapContentHeight().fillMaxWidth()
                        .weight(columnWeights[i]).clickable {
                            doStuff(columnNames[i])
                        }
                ){
                    Text(
                        text = columnNames[i],
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

    }

}
