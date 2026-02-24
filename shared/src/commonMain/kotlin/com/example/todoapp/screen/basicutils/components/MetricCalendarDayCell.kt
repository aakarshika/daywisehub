package com.example.todoapp.screen.basicutils.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.db.models.TaskType
import com.example.todoapp.screen.basicutils.Yellow180
import com.example.todoapp.screen.basicutils.getDarkPillarColor
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.crown_a
import todoapp.shared.generated.resources.crown_background_a
import todoapp.shared.generated.resources.flag
import todoapp.shared.generated.resources.flag_open

/**
 * Calendar day cell for the metrics screen.
 * Shows tasks filtered by pillar selection, milestones, and top-3 crown when applicable.
 */
@Composable
fun MetricCalendarDayCell(
    day: CalendarDay,
    tasks: List<TodayTaskWithFewDetails>,
    selectionOptions: List<String>,
    isSelected: Boolean,
    onDateClicked: (LocalDate) -> Unit,
    milestonesForDate: List<MilestoneWithDetails>? = null,
    cellSize: Dp = 82.dp,
) {
    val dayString = MyDate.fromLocalDate(day.date).dateString
    val todayTasks: List<TodayTaskWithFewDetails> = when {
        tasks.isEmpty() -> emptyList()
        else -> tasks.filter { selectionOptions.contains(it.pillar?.pillarName) }
    }
    val t3 = todayTasks.filter { it.todayTask.taskPageTag == TaskType.TOP3.value }
    val top3completed = t3.isNotEmpty() && t3.all { it.todayTask.taskStatus == TaskStatus.COMPLETED.value }
    val hasMilestone = !milestonesForDate.isNullOrEmpty() &&
            milestonesForDate.any { it.milestone.expectedCompletionDate.dateString == dayString && it.milestone.status != "COMPLETED" }
    val hasCompletedMilestone = !milestonesForDate.isNullOrEmpty() &&
            milestonesForDate.any { m ->
                (m.milestone.expectedCompletionDate.dateString == dayString && m.milestone.status == "COMPLETED") ||
                m.milestone.actualCompletionDate?.dateString == dayString
            }

    val paddingOuter = if (isSelected) 5.dp else 10.dp
    val cornerRadius = if (isSelected) 15.dp else 20.dp
    val innerSize = 80.dp
    val canvasSize = 78.dp
    val strokeWidth = 24f
    val crownSize = if (isSelected) 28.dp else 25.dp
    val crownPaddingStart = if (isSelected) 1.dp else 5.dp
    val crownPaddingTop = if (isSelected) 0.dp else 2.dp
    val fontSize = 16.sp
    val flagSize = 15.dp

    Box(modifier = Modifier.wrapContentSize()) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .size(cellSize)
                .testTag("MonthDay")
                .padding(paddingOuter)
                .clip(RoundedCornerShape(cornerRadius))
                .border(
                    1.dp,
                    if (dayString == MyDate.now().dateString && !hasCompletedMilestone && !hasMilestone) Yellow180 else Color.Transparent,
                    RoundedCornerShape(cornerRadius)
                )
                .clickable(
                    enabled = day.position == DayPosition.MonthDate,
                    onClick = { onDateClicked(day.date) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(innerSize)
                    .align(Alignment.Center)
            ) {
                if (todayTasks.isNotEmpty()) {
                    Canvas(
                        modifier = Modifier
                            .size(canvasSize)
                            .align(Alignment.Center)
                    ) {
                        val startAngle = -90f
                        val sweepAngle = 360f / todayTasks.size
                        var currentAngle = startAngle
                        todayTasks.forEach { task ->
                            drawArc(
                                color = if (task.todayTask.taskStatus == TaskStatus.COMPLETED.value) {
                                    getDarkPillarColor(pillarName = task.pillar?.pillarName)
                                } else Color.White,
                                startAngle = currentAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth),
                                size = Size(size.width, size.height)
                            )
                            currentAngle += sweepAngle
                        }
                    }
                }
            }
        }

        if (top3completed) {
            Box(
                modifier = Modifier
                    .padding(start = crownPaddingStart, top = crownPaddingTop)
                    .size(crownSize)
                    .align(Alignment.TopStart)
            ) {
                Image(
                    painterResource(Res.drawable.crown_background_a),
                    contentDescription = "Crown background",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.fillMaxSize()
                )
                Image(
                    painterResource(Res.drawable.crown_a),
                    contentDescription = "Crown icon",
                    colorFilter = ColorFilter.tint(Yellow180),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (hasCompletedMilestone && day.position == DayPosition.MonthDate) {
            Box(
                modifier = Modifier
                    .size(flagSize)
                    .align(Alignment.TopEnd)
            ) {
                Image(
                    painterResource(Res.drawable.flag),
                    contentDescription = "MilestoneC",
                    colorFilter = ColorFilter.tint(getDarkPillarColor(pillarName = milestonesForDate?.firstOrNull()?.pillar?.pillarName)),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (hasMilestone && day.position == DayPosition.MonthDate) {
            Box(
                modifier = Modifier
                    .size(flagSize)
                    .align(Alignment.TopEnd)
            ) {
                Image(
                    painterResource(Res.drawable.flag_open),
                    contentDescription = "MilestoneI",
                    colorFilter = ColorFilter.tint(getDarkPillarColor(pillarName = milestonesForDate?.firstOrNull()?.pillar?.pillarName)),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        val textColor = when (day.position) {
            DayPosition.MonthDate -> if (isSelected) Color.Gray else Color.Black
            DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
            else -> Color.Transparent
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxSize().align(Alignment.Center),
                text = day.date.dayOfMonth.toString(),
                color = textColor,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
            )
        }
    }
}
