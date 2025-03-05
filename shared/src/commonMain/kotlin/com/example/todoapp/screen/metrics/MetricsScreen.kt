package com.example.todoapp.screen.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.basicutils.components.WriteText
import com.example.todoapp.screen.metrics.metriccomponents.TaskTypeDropdown
import com.example.todoapp.screen.metrics.metriccomponents.options
import com.example.todoapp.screen.missions.Orange80
import com.example.todoapp.screen.missions.Red80
import com.example.todoapp.screen.missions.getPillarColor
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.WeekDay
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
    var dateWiseTasks by remember { mutableStateOf<Map<MyDate, List<TodayTaskWithFewDetails>>>(mapOf()) }
    var selectionOptions by remember { mutableStateOf<List<String>>(options) }
     dateWiseTasks=
        allTasks?.groupBy { task ->  task.todayTask.taskDate }
           ?: mapOf()

    LaunchedEffect( Unit) {
        metricsViewModel.loadAllTasks()
    }

    Column {
        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()){
            TaskTypeDropdown(
                optionsSelected = { selectedPillars ->
                    Logger.e("TaskTypeDropdown     $selectedPillars")

//                    metricsViewModel.loadTasks(selectedPillars)
                    selectionOptions = selectedPillars

                    Logger.e("dateWiseTasks     $dateWiseTasks")
                }
            )

        }
        Box(modifier = Modifier.fillMaxSize()) {
            CalendarArea(
                selectedDate = currentDate,
                dateWiseTasks = dateWiseTasks,
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
    selectionOptions: List<String> ,
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
                    // The key parameter forces recomposition when dateWiseTasks changes
                    key(day.date, dateWiseTasks) {
                        MonthDay(
                            day,
                            dateWiseTasks?.get(MyDate.fromLocalDate(day.date)) ?: listOf(),
                            selectionOptions = selectionOptions,
                            isSelected = selectedDate == day.date,
                            onDateClicked
                        )
                    }
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
private fun MonthDay(
    day: CalendarDay,
    ttasks: List<TodayTaskWithFewDetails>?,
    selectionOptions: List<String>,
    isSelected: Boolean,
    onDateClicked: (LocalDate) -> Unit
) {
    val todayTasks: List<TodayTaskWithFewDetails> = ttasks?.filter { selectionOptions.contains(it.pillar?.pillarName)}?: listOf()
    Box(modifier = Modifier.wrapContentSize()) {

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .size(80.dp)
                .testTag("MonthDay")
                .padding(if (isSelected) 5.dp else 10.dp)
                .clip(RoundedCornerShape(if (isSelected) 15.dp else 20.dp))
                .background(
                    color = when (day.position) {
                        DayPosition.MonthDate -> if (todayTasks.size>0 && todayTasks.all { it.todayTask.taskStatus=="COMPLETED" }) Red80 else if (isSelected) Color.White else Color.Transparent
                        DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
                    }
                )
                // Disable clicks on inDates/outDates
                .clickable(
                    enabled = day.position == DayPosition.MonthDate,
                    onClick = { onDateClicked(day.date) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center)
            ) {
                if (todayTasks != null) {
                    Canvas(
                        modifier = Modifier
                            .size(80.dp)
                    ) {

                        val strokeWidth = 24f // Thickness of the ring
                        val startAngle = -90f // Start from the top
                        val sweepAngle = 360f / todayTasks.size

                        var currentAngle = startAngle
                        todayTasks.forEach { task ->
                            drawArc(
                                color = if (task.todayTask.taskStatus == "COMPLETED") {
                                    getPillarColor(pillarName = task.pillar?.pillarName)
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
            val textColor = when (day.position) {
                DayPosition.MonthDate -> if (isSelected) Color.Gray else Color.Black
                DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
            }
            Text(
                text = day.date.dayOfMonth.toString(),
                color = textColor,
                fontSize = 16.sp,
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

@Composable
fun WeekDayGola(
    day: WeekDay,
    isSelected: Boolean,
    onDateClicked: (LocalDate) -> Unit
){

//    val dayTasks: List<TodayTaskWithDetails>,
    val dayMilestones: List<MilestoneWithDetails>

//    val allTasks = (dayTasks?: listOf() ).sortedBy { t-> t.mission.pillarName }.sortedBy { t-> t.task.taskStatus }
//    val allTasksCompleted = allTasks.filter { t->  t.task.taskStatus==TaskStatus.COMPLETED }
//    val top3Tasks = allTasks.filter { t-> t.taskUiData.taskUi.taskPageTag == "TOP3" }
//    val top3TasksCompleted = allTasks.filter { t-> t.taskUiData.taskUi.taskPageTag == "TOP3" && t.task.taskStatus==TaskStatus.COMPLETED }

    val date = day.date

    Box(modifier = Modifier.padding(2.dp)){
        RoundButton((date.dayOfWeek.name).substring(0,1), todayHighlight = isSelected){
            onDateClicked(date)
        }
    }
}
val CalendarGradientA = Color(0xFFFFFBF0)
val CalendarGradientB = Color(0xFFFCECE7)


@Composable
fun RoundButton(number: String, todayHighlight: Boolean = false,
                onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(25.dp)
            .clip(CircleShape)
            .background(if(todayHighlight) Red80 else  Orange80)
            .clickable {
                onClick()
            }
    ) {
        WriteText(
            text = number,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
