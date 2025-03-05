package com.example.todoapp.screen.missions.calendar.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mission.MissionWithDetails
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.missions.MissionItemViewModel
import com.example.todoapp.screen.missions.darken
import com.example.todoapp.screen.missions.getPillarColor
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.ExperimentalCalendarApi
import com.kizitonwose.calendar.core.YearMonth
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.minusYears
import com.kizitonwose.calendar.core.plusYears
import kotlinx.datetime.number
import org.koin.core.parameter.parametersOf


@Composable
fun MissionProgressCalendarb(missionn: MissionWithDetails) {

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Column {
            CalendarArea(selectedMission = missionn)
        }
    }
}

@Composable
private fun CalendarArea(
    selectedMission: MissionWithDetails?
) {

    val currentMonth = remember { YearMonth.now() }
    val currentYear = remember { currentMonth.year }

    val startMonth = remember { currentMonth.minusYears(3) } // Adjust as needed
    val endMonth = remember { currentMonth.plusYears(3) } // Adjust as needed
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() } // Available from the library

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek,
    )
    VerticalCalendar(
        modifier = Modifier.fillMaxWidth(),
        state = state,
        calendarScrollPaged = false,
        dayContent = { day ->
            val date = MyDate.fromLocalDate(day.date)
            DayMission(day,
                KoinF.di?.get<DayMissionProgressViewModel> { parametersOf(date,selectedMission?.mission?.missionId) }!!,
                selectedMission
            )
        },
        monthHeader = { month ->
        },
        monthBody = { month, content ->
            Box(
                modifier = Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = if (month.yearMonth.month.number % 2 == 0) listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.25f)
                        ) else listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.40f)
                        )
                    )
                )
            ) {
                content() // Render the provided content!
            }
        },
        monthContainer = { month, container ->
            Column(modifier = Modifier.fillMaxWidth()) { // Wrap month in a Column
                MonthHeader2(month) // Display month header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    container() // Render the month content
                }
            }
        }
    )
}
@Composable
fun MonthHeader2(month: CalendarMonth) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${month.yearMonth.month.name} ${month.yearMonth.year}",
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Left,
        )
    }
}
@Composable
private fun DayMission(
    day: CalendarDay,
    viewModel: DayMissionProgressViewModel,
    selectedMission: MissionWithDetails?,
) {

    Box(modifier = Modifier.wrapContentSize()) {
        LaunchedEffect(Unit) {
            viewModel.loadMileDetails()
        }
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .size(20.dp)
                    .testTag("MonthDay")
                    .clip(CircleShape)
                    .background(
                        color =  Color.Transparent
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center)
                ) {
                    val miles: List<MilestoneWithDetails>? by viewModel.milestones.collectAsState(emptyList())

                    if(miles != null && miles!!.size>0){
                        Icon(Icons.Default.Settings,
                            "sfgh",
                            modifier = Modifier.size(70.dp),
                            tint =
                        when (day.position) {
                            DayPosition.MonthDate ->
                                if (miles!!.get(0).milestoneWithDetails.milestone.status == "COMPLETED") {
                                    getPillarColor(pillarName = selectedMission?.pillar?.pillarName).darken(0.2f)
                                } else Gray
                            DayPosition.InDate, DayPosition.OutDate -> Color.Transparent})
//                    } else if(dayTasks != null && dayTasks.size>0) {
//                        Canvas(
//                            modifier = Modifier
//                                .size(78.dp)
//                        ) {
//                            val dayTask = dayTasks.get(0)
//                            val dayTaskFreq = dayTask?.mission?.frequencySetValue ?: 1f
//                            val strokeWidth = 24f // Thickness of the ring
//                            val fillWidth = 50f // Thickness of the ring
//                            val startAngle = -90f // Start from the top
//                            val sweepAngle: Float = 360f / dayTaskFreq
//                            var currentAngle = startAngle
//                            dayTask.let { task ->
//                                if (task?.mission?.frequencyPeriod == "DAILY" && task.task.taskStatus != TaskStatus.COMPLETED) {
//                                    val dayTaskFreqDone = task.taskUiData.stampPoints.size
//                                }
//                                drawArc(
//                                    color = when (day.position) {
//                                        DayPosition.MonthDate ->
//                                            if (task?.task?.taskStatus == TaskStatus.COMPLETED) {
//                                                getPillarColor(pillarName = task.mission.pillarName).darken()
//                                            } else if (task != null) {
//                                                Gray
//                                            } else Color.Transparent
//
//                                        DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
//                                    },
//                                    startAngle = currentAngle,
//                                    sweepAngle = sweepAngle,
//                                    useCenter = false,
//                                    style = Stroke(width = fillWidth),
//                                    size = Size(size.width, size.height)
//                                )
//                                currentAngle += sweepAngle
//                            }
//                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                    ) {
                        val textColor = when (day.position) {
                            DayPosition.MonthDate -> Color.Black
                            DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
                        }
                        Text(
                            modifier = Modifier
                                .align(Alignment.Center),
                            text = day.date.dayOfMonth.toString(),
                            color = textColor,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
    }
}
