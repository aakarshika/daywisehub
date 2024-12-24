package com.example.todoapp.screen.metrics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.getScreenWidth
import com.example.todoapp.screen.basicblocks.CustomCheckbox
import com.example.todoapp.screen.basicblocks.DiaryLineText
import com.example.todoapp.screen.basicblocks.WriteText
import com.example.todoapp.screen.metrics.cal.clickable
import com.example.todoapp.screen.missions.getPillarColor
import kotlinx.datetime.LocalDate
import kotlin.random.Random

@Composable
fun ItemProgressBar(
    rowHeight: Int,
    selection: LocalDate,
    todoTask: TodayTask?,
    pillar: Pillar,
    mission: Mission,
    missionFrequency: MissionFrequency,
    editingMode: String,
    onTaskProgressUpdated: (Float) -> Unit,
) {
    val totalProgressWidth = 500f
    val isTaskCompleted = todoTask?.taskStatus == "COMPLETED"
    var taskProgress = remember { mutableStateOf(0f) }

    LaunchedEffect(selection) {
        taskProgress.value = when {
            isTaskCompleted -> totalProgressWidth
            todoTask?.taskProgressVal != null -> todoTask.taskProgressVal * totalProgressWidth
            else -> 0f
        }
    }

    Box(modifier = Modifier.fillMaxWidth().height(rowHeight.dp)) {
        ProgressBarWithDrag(
            totalProgressWidth = totalProgressWidth,
            taskProgress = taskProgress,
            isTaskCompleted = isTaskCompleted,
            onTaskProgressUpdated = onTaskProgressUpdated
        )

        ProgressBarForeground(
            rowHeight = rowHeight,
            taskProgress = taskProgress.value,
            totalProgressWidth = totalProgressWidth,
            pillarColor = getPillarColor(pillar.pillarName)
        )

        ProgressBarMetrics(
            taskProgress = taskProgress.value,
            totalProgressWidth = totalProgressWidth,
            missionFrequency = missionFrequency
        )
    }
}

@Composable
fun ProgressBarWithDrag(
    totalProgressWidth: Float,
    taskProgress: MutableState<Float>,
    isTaskCompleted: Boolean,
    onTaskProgressUpdated: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        onTaskProgressUpdated(taskProgress.value / totalProgressWidth)
                    }
                ) { change, dragAmount ->
                    if (!isTaskCompleted) {
                        change.consume()
                        taskProgress.value = (taskProgress.value + dragAmount.x).coerceIn(0f, totalProgressWidth)
                    }
                }
            }
    )
}

@Composable
fun ProgressBarForeground(
    rowHeight: Int,
    taskProgress: Float,
    totalProgressWidth: Float,
    pillarColor: Color
) {
    val paddingStart = 48.dp
    Row(
        modifier = Modifier.padding(top = (rowHeight - 25).dp, start = paddingStart),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .height(3.dp)
                .width(((taskProgress / totalProgressWidth) * (getScreenWidth().value - paddingStart.value)).dp)
                .background(pillarColor)
        )
        Box(
            modifier = Modifier
                .height(2.dp)
                .weight(1f)
                .background(Color.Transparent)
        )
    }
}

@Composable
fun ProgressBarMetrics(
    taskProgress: Float,
    totalProgressWidth: Float,
    missionFrequency: MissionFrequency
) {
    val completedFrequency = ((taskProgress / totalProgressWidth) * (missionFrequency.frequency ?: 1)).toFloat()
    val displayedValue = (completedFrequency * 10).toInt() / 10f
    val totalFrequency = (missionFrequency.frequency ?: 1).toFloat()

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        WriteText(
            text = "${if (displayedValue % 1f == 0f) displayedValue.toInt() else displayedValue}/${totalFrequency.toInt()}",
            fontSize = 13f,
            color = Color.Black
        )
        WriteText(
            text = " ${missionFrequency.frequencyUnit}",
            fontSize = 9f,
            color = Color.Gray
        )
    }
}

