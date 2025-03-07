package com.example.todoapp.screen.diary.diaryitem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.getScreenWidth
import com.example.todoapp.screen.basicutils.components.WriteText
import com.example.todoapp.screen.diary.ComboTask
import com.example.todoapp.screen.missions.getPillarColor
import kotlinx.datetime.LocalDate


val totalProgressWidth = 500f

@Composable
fun ItemProgressBar(
    rowHeight: Int,
    selection: LocalDate,
    ct: ComboTask,
    editingMode:String,
    onTaskProgressUpdated: (Float) -> Unit,
) {
    val isTaskCompleted = ct.todayTask?.taskStatus == "COMPLETED"
    var taskProgress = remember { mutableStateOf(0f) }

    LaunchedEffect(selection) {
        taskProgress.value = when {
            isTaskCompleted -> totalProgressWidth
            ct.todayTask?.taskProgressVal != null -> ct.todayTask.taskProgressVal * totalProgressWidth
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
            pillarColor = getPillarColor(ct.pillar?.pillarName)
        )

        ProgressBarMetrics(
            taskProgress = taskProgress.value,
            missionFrequency = ct.missionFrequency
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
    pillarColor: Color
) {
    val paddingStart = 40.dp
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
        )
    }
}

@Composable
fun ProgressBarMetrics(
    taskProgress: Float,
    missionFrequency: MissionFrequency?
) {
    val completedFrequency = ((taskProgress / totalProgressWidth) * (missionFrequency?.frequency ?: 1)).toFloat()
    val displayedValue = (completedFrequency * 10).toInt() / 10f
    val totalFrequency = (missionFrequency?.frequency ?: 1).toFloat()

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, end = 5.dp).wrapContentHeight(),
        horizontalArrangement = Arrangement.End
    ) {
        WriteText(
            text = "${if (displayedValue % 1f == 0f) displayedValue.toInt() else displayedValue}/${totalFrequency.toInt()}",
            fontSize = 13f,
            color = Color.Black
        )
        WriteText(
            text = " ${missionFrequency?.frequencyUnit?:1}",
            fontSize = 9f,
            color = Color.Gray
        )
    }
}

