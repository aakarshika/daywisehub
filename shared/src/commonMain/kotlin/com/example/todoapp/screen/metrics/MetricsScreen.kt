package com.example.todoapp.screen.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.screen.basicutils.components.MetricCalendarDayCell
import com.example.todoapp.screen.metrics.metriccomponents.TaskTypeDropdown
import com.example.todoapp.screen.metrics.metriccomponents.options
import com.example.todoapp.screen.basicutils.CalendarGradientA
import com.example.todoapp.screen.basicutils.CalendarGradientB
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.YearMonth
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.minusYears
import com.kizitonwose.calendar.core.plusYears
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number


@Composable
fun MetricsScreen(
    metricsViewModel: MetricsViewModel,
    currentDate: LocalDate,
    changeDate: (LocalDate) -> Unit
) {
    val allTasks: List<TodayTaskWithFewDetails>? by metricsViewModel.allTasks.collectAsState(emptyList())
    val allMilestones: List<MilestoneWithDetails>? by metricsViewModel.allMilestones.collectAsState(emptyList())
    var dateWiseTasks by remember { mutableStateOf<Map<MyDate, List<TodayTaskWithFewDetails>>>(mapOf()) }
    var dateWiseMilestones by remember { mutableStateOf<Map<String, List<MilestoneWithDetails>>>(mapOf()) }
    var selectionOptions by remember { mutableStateOf<List<String>>(options) }
    dateWiseTasks =
        allTasks?.sortedBy { it.pillar?.pillarId }
            ?.sortedBy { it.todayTask.taskStatus == TaskStatus.COMPLETED.value }
            ?.groupBy { task -> task.todayTask.taskDate }
            ?: mapOf()
    dateWiseMilestones =
        allMilestones?.mapNotNull { m ->
            m.milestone.actualCompletionDate?.dateString?.let { it to m }
        }?.groupBy({ it.first }, { it.second })
            ?: mapOf()

    LaunchedEffect(Unit) {
        metricsViewModel.loadAllTasks()
        metricsViewModel.loadAllMilestones()
    }

    Column {
        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()){
            TaskTypeDropdown(
                optionsSelected = { selectedPillars ->
                    selectionOptions = selectedPillars
                }
            )

        }
        Box(modifier = Modifier.fillMaxSize()) {
            CalendarArea(
                selectedDate = currentDate,
                dateWiseTasks = dateWiseTasks,
                dateWiseMilestones = dateWiseMilestones,
                selectionOptions = selectionOptions,
                onDateClicked = { date ->
                    changeDate(date)
                },
            )
        }
    }
}

@Composable
private fun CalendarArea(
    selectedDate: LocalDate,
    dateWiseTasks: Map<MyDate, List<TodayTaskWithFewDetails>?>? = null,
    dateWiseMilestones: Map<String, List<MilestoneWithDetails>>? = null,
    selectionOptions: List<String>,
    onDateClicked: (LocalDate) -> Unit,
) {

    val currentMonth = remember { YearMonth.now() }

    val startMonth = remember { currentMonth.minusYears(3) }
    val endMonth = remember { currentMonth.plusYears(3) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.SUNDAY)
    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )
    Box {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            VerticalCalendar(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                state = state,
                calendarScrollPaged = false,
                dayContent = { day ->
                    val dateString = MyDate.fromLocalDate(day.date).dateString
                    MetricCalendarDayCell(
                        day = day,
                        tasks = dateWiseTasks?.get(MyDate.fromLocalDate(day.date)) ?: listOf(),
                        selectionOptions = selectionOptions,
                        isSelected = selectedDate == day.date,
                        onDateClicked = onDateClicked,
                        milestonesForDate = dateWiseMilestones?.get(dateString)
                    )
                },
                monthBody = { month, content ->
                    Box(
                        modifier = Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = if (month.yearMonth.month.number % 2 == 0)
                                    listOf(CalendarGradientA, CalendarGradientB)
                                else
                                    listOf(CalendarGradientB, CalendarGradientA)
                            )
                        )
                    ) {
                        content()
                    }
                },
                monthContainer = { month, container ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MonthHeader(month)
                        Box(modifier = Modifier.fillMaxWidth()) { container() }
                    }
                }
            )
        }
    }
}

@Composable
private fun MonthHeader(month: CalendarMonth) {
    Row(modifier = Modifier.fillMaxWidth().background(color =
    if (month.yearMonth.month.number % 2 == 0) CalendarGradientA else CalendarGradientB)) {
        Text(
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            text = "${month.yearMonth.month.name} ${month.yearMonth.year}",
        )
    }
}
