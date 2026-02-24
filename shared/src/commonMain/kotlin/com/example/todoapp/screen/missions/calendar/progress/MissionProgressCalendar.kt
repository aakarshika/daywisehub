package com.example.todoapp.screen.missions.calendar.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.MissionWithDetails
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.screen.basicutils.CalendarGradientA
import com.example.todoapp.screen.basicutils.CalendarGradientB
import com.example.todoapp.screen.basicutils.DARKGREEN180
import com.example.todoapp.screen.basicutils.Yellow180
import com.example.todoapp.screen.basicutils.Yellow80
import com.example.todoapp.screen.basicutils.components.MissionCalendarDayCell
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.YearMonth
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.minusYears
import com.kizitonwose.calendar.core.plusYears
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.number


@Composable
fun MissionProgressCalendar(
    viewModel: DayMissionProgressViewModel,
    missionn: MissionWithDetails) {
    val miles: List<MilestoneWithDetails>? by viewModel.milestones.collectAsState(initial = null)
    val tasks: List<TodayTaskWithFewDetails>? by viewModel.tasks.collectAsState(initial = null)

    val dateWiseMiles = remember(miles) {
        miles?.sortedBy { it.pillar?.pillarId }
            ?.groupBy { mile -> mile.milestone.expectedCompletionDate.dateString }
            ?: emptyMap<String, List<MilestoneWithDetails>>()
    }

    val dateWiseTasks = remember(tasks) {
        tasks
            ?.sortedWith(
                compareBy<TodayTaskWithFewDetails> { it.todayTask.taskStatus != TaskStatus.COMPLETED.value }
                    .thenBy { it.pillar?.pillarId }
            )
            ?.groupBy { it.todayTask.taskDate.dateString }
            ?: emptyMap<String, List<TodayTaskWithFewDetails>>()
    }

    LaunchedEffect(Unit) {
        viewModel.loadMileDetails()
        viewModel.loadTaskDetails()
    }
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Column {
            key(dateWiseMiles, dateWiseTasks) {
                CalendarArea(
                    miles = dateWiseMiles,
                    tasks = dateWiseTasks,
                    selectedMission = missionn
                )
            }
        }
    }
}

@Composable
private fun CalendarArea(
    miles: Map<String,List<MilestoneWithDetails>?>?,
    tasks: Map<String,List<TodayTaskWithFewDetails>?>?,
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

    //some more calculation - for every saturday - lastdayofweek - i need to get the number tasks done in that week.


    VerticalCalendar(
        modifier = Modifier.fillMaxWidth(),
        state = state,
        calendarScrollPaged = false,
        dayContent = { day ->
            val date = MyDate.fromLocalDate(day.date)
            val dayTasks = tasks?.get(date.dateString).orEmpty()

            // Weekly Task List (Last 7 Days from Saturday) for weekly progress bar
            val weeklyTaskList = if (day.date.dayOfWeek == DayOfWeek.SATURDAY) {
                (1..7).flatMap { n ->
                    tasks?.get(date.subtractDays(n).dateString).orEmpty()
                }
            } else {
                emptyList()
            }

            // VerticalCalendar gives each day an equal slot (1/7 of row width). Per-day width
            // is not supported, so Modifier.width() here has no effect; all cells stay same width.
            Box(modifier = Modifier) {
                if (day.date.dayOfWeek == DayOfWeek.SATURDAY) {
                    val weeklycompleted = weeklyTaskList.filter { t -> t.todayTask.taskStatus == TaskStatus.COMPLETED.value }
                    val freq = (selectedMission?.missionFrequency?.frequency ?: 1)
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(5.dp)
                            .align(Alignment.CenterEnd)
                            .background(
                                color = if (selectedMission?.missionFrequency?.frequencyPeriod == "WEEKLY")
                                    when {
                                        weeklycompleted.size >= freq -> DARKGREEN180
                                        weeklycompleted.isNotEmpty() -> Yellow180
                                        weeklyTaskList.isNotEmpty() -> Yellow80
                                        else -> Color.Transparent
                                    }
                                else Color.Transparent
                            )
                    )
                }
                MissionCalendarDayCell(
                    day = day,
                    tasks = dayTasks,
                    onDateClicked = {},
                    milestonesForDate = miles?.get(date.dateString)
                )
            }
        },
        monthHeader = { month ->
        },
        monthBody = { month, content ->
            Box {
                content()
            }
        },
        monthContainer = { month, container ->
            Column(modifier = Modifier.fillMaxWidth()) {
                val monthlyTaskList = tasks?.filterKeys { dateString ->
                    MyDate(dateString).toLocalDate().month == month.yearMonth.month
                }?.values?.flatMap { it ?: listOf() } ?: listOf()
                val weeklycompleted = monthlyTaskList.filter { t -> t.todayTask.taskStatus == TaskStatus.COMPLETED.value }
                val freq = (selectedMission?.missionFrequency?.frequency ?: 1)

                MonthHeader(month)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    container() // Render the month content
                }
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .fillMaxWidth()
                        .background(
                            color =
                            if(selectedMission?.missionFrequency?.frequencyPeriod == "MONTHLY")
                                if(weeklycompleted.size >= freq) DARKGREEN180
                                else if(weeklycompleted.size > 0 ) Yellow180
                                else if(monthlyTaskList.isNotEmpty()) Yellow80
                                else Color.Transparent
                            else Color.Transparent
                        )
                )
            }
        }
    )
}
@Composable
fun MonthHeader(month: CalendarMonth) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${month.yearMonth.month.name} ${month.yearMonth.year}",
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Left,
        )
    }
}
