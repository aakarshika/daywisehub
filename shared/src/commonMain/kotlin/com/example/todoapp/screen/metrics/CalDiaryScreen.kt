package com.example.todoapp.screen.metrics

import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.di.KoinF
import com.example.todoapp.getScreenHeight
import com.example.todoapp.getScreenWidth
import com.example.todoapp.getTopCalHeight
import com.example.todoapp.screen.missions.Orange80
import com.example.todoapp.screen.missions.Red80
import com.example.todoapp.screen.missions.calendar.progress.DayMissionProgressViewModel
import com.example.todoapp.screen.missions.darken
import com.example.todoapp.screen.missions.getPillarColor
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.ExperimentalCalendarApi
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.YearMonth
import com.kizitonwose.calendar.core.atEndOfMonth
import com.kizitonwose.calendar.core.atStartOfMonth
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.minusYears
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusYears
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.number
import org.koin.core.parameter.parametersOf


@Composable
fun CalDiaryScreen(mode: String, calDateViewModel: CalDiaryViewModel,
                   onExpandClicked: () -> Unit,
                   onCollapseClicked: () -> Unit
) {

    val screenHeightDp = getScreenHeight()
    val screenWidthDp = getScreenWidth()
    val topCalHeight = getTopCalHeight()

    var diaryViewMode by remember { mutableStateOf(mode) }
    var dateFilterMode by remember { mutableStateOf("NORMAL") }

    val diaryHeight by animateDpAsState(
        targetValue = if (diaryViewMode == "DIARY_MODE") (screenHeightDp - topCalHeight) else (screenHeightDp - 500.dp)
    )
    val diaryWidth by animateDpAsState(
        targetValue = if (diaryViewMode == "DIARY_MODE") screenWidthDp else (screenWidthDp - 100.dp)
    )
    val calHeight by animateDpAsState(
        targetValue = if (diaryViewMode == "DIARY_MODE") 100.dp else screenHeightDp
    )
    val diaryAlpha by animateFloatAsState(
        targetValue = if (diaryViewMode == "DIARY_MODE") 1f else 0f
    )

    val currentDate by calDateViewModel.currentDate.collectAsState()
    val missions by calDateViewModel.missionIds.collectAsState(emptyList())

    LaunchedEffect(Unit) {
        calDateViewModel.loadMissionIds()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CalendarArea(
            selectedDate = currentDate,
            diaryViewMode = diaryViewMode,
            expandAlpha = diaryAlpha,
            onDateClicked = { date -> calDateViewModel.selectDate(date) },
            onCollapseClicked = onCollapseClicked,
            onExpandClicked = onExpandClicked
        )

        Box(
            modifier = Modifier
                .height(diaryHeight)
                .width(diaryWidth)
                .align(Alignment.BottomCenter)
        ) {
            Box(modifier = Modifier.alpha(diaryAlpha)) {
                DiaryScreen(
                    selection = currentDate,
                    diaryViewModel = KoinF.di?.get<DiaryViewModel> {
                        parametersOf(currentDate, missions.size)
                    } ?: error("DiaryViewModel not found")
                )
            }
            Box(modifier = Modifier.alpha(1f - diaryAlpha)) {
                BottomHighlightsSpace(
                    selection = currentDate,
                    bottomHighlightsViewModel = KoinF.di?.get<BottomHighlightsViewModel> {
                        parametersOf(currentDate)
                    } ?: error("BottomHighlightsViewModel not found"),
                    onExpandClicked = onExpandClicked
                )
            }
        }
    }
}

@Composable
private fun CalendarArea(
    selectedDate: LocalDate,
    onDateClicked: (LocalDate) -> Unit,
    onCollapseClicked: () -> Unit,
    onExpandClicked: () -> Unit,
    diaryViewMode: String,
    expandAlpha: Float
) {

    val screenHeightDp = getScreenHeight()
    val calBigHeight by animateDpAsState(
        targetValue = if (diaryViewMode == "DIARY_MODE") 0.dp else screenHeightDp
    )
    val calWeekHeight by animateDpAsState(
        targetValue = if (diaryViewMode == "DIARY_MODE") 100.dp else 0.dp
    )

    val currentDate = remember { LocalDate.now() }
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
    val weekState = rememberWeekCalendarState(
        startDate = startMonth.atStartOfMonth(),
        endDate = endMonth.atEndOfMonth(),
        firstVisibleWeekDate = currentDate,
        firstDayOfWeek = daysOfWeek.first()
    )

    Box {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .alpha(expandAlpha)
        ) {
            WeekCalendar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(calWeekHeight)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                            },
                            onDragEnd = { onCollapseClicked() }
                        )
                    },
                state = weekState,
                calendarScrollPaged = false,
                dayContent = { day ->
                    WeekDay(
                        day,
                        isSelected = selectedDate == day.date,
                        onDateClicked
                    )
                }
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .alpha(1f - expandAlpha)
        ) {
            VerticalCalendar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(calBigHeight),
                state = state,
                calendarScrollPaged = false,
                dayContent = { day ->
                    MonthDay(
                        day,
                        isSelected = selectedDate == day.date,
                        onDateClicked
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
fun WeekDay(
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

    Box(modifier = Modifier.padding(5.dp)){
        if(isSelected && date.daysUntil(LocalDate.now())==0){
            RoundBox(""+date.dayOfMonth+date.month.name, todayHighlight = true){
                onDateClicked(date)
            }
        } else if(isSelected){
            RoundButton(date.dayOfMonth, todayHighlight = true){
                onDateClicked(date)
            }
        } else if(date.daysUntil(LocalDate.now())==0){
            RoundBox(""+date.dayOfMonth+date.month.name, todayHighlight = false){
                onDateClicked(date)
            }
        } else {
            RoundButton(date.dayOfMonth, todayHighlight = false){
                onDateClicked(date)
            }
        }
    }
}

@Composable
private fun MonthDay(
    day: CalendarDay,
    isSelected: Boolean,
    onDateClicked: (LocalDate) -> Unit
) {
//    val allTasks =  listOf() .sortedBy { t-> t.mission.pillarName }.sortedBy { t-> t.task.taskStatus }
//    val allTasksCompleted = allTasks.filter { t->  t.task.taskStatus==TaskStatus.COMPLETED }
//    val top3Tasks = allTasks.filter { t-> t.taskUiData.taskUi.taskPageTag == "TOP3" }
//    val top3TasksCompleted = allTasks.filter { t-> t.taskUiData.taskUi.taskPageTag == "TOP3" && t.task.taskStatus==TaskStatus.COMPLETED }


    Box(modifier = Modifier.wrapContentSize()) {

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .size(80.dp)
                .testTag("MonthDay")
                .padding(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    color = when (day.position) {
                        DayPosition.MonthDate -> if (isSelected) Color.White else Color.Transparent
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
//            Box(
//                modifier = Modifier
//                    .size(80.dp)
//                    .align(Alignment.Center)
//            ) {
//
//                Canvas(
//                    modifier = Modifier
//                        .size(80.dp)
//                ) {
//
//                    val strokeWidth = 24f // Thickness of the ring
//                    val startAngle = -90f // Start from the top
//                    val sweepAngle = 360f / allTasks.size
//
//                    var currentAngle = startAngle
//                    allTasks.forEach { task ->
//                        drawArc(
//                            color = if (task.task.taskStatus == TaskStatus.COMPLETED) {
//                                getPillarColor(pillarName = task.mission.pillarName).darken()
//                            } else Color.White,
//                            startAngle = currentAngle,
//                            sweepAngle = sweepAngle,
//                            useCenter = false,
//                            style = Stroke(width = strokeWidth),
//                            size = Size(size.width, size.height)
//                        )
//                        currentAngle += sweepAngle
//                    }
//                }
//            }
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

        Row(modifier = Modifier.align(Alignment.TopCenter).padding(top = 55.dp)) {
//            if (allTasks.size > 0 && allTasks.size == allTasksCompleted.size) {
//                Icon(
//                    Icons.Default.ThumbUp,
//                    modifier = Modifier.size(12.dp).padding(top = 2.dp),
//                    contentDescription = "AllCompleted",
//                    tint = Color.DarkGray
//                )
//            }
//            if (top3Tasks.size == 3 && top3Tasks.size == top3TasksCompleted.size) {
//                Icon(
//                    Icons.Default.Star,
//                    modifier = Modifier.size(14.dp),
//                    contentDescription = "top3Completed",
//                    tint = Color.DarkGray
//                )
//            }
        }
    }
}
/*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CalendarTop(
    modifier: Modifier = Modifier,
    daysOfWeek: List<DayOfWeek>,
//    selection: LocalDate,
    close: () -> Unit,
    clearDates: () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                for (dayOfWeek in daysOfWeek) {
                    Text(
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray,
                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        fontSize = 15.sp,
                    )
                }
            }
        }
        HorizontalDivider()
    }
}



@RequiresApi(Build.VERSION_CODES.O)
@RequiresApi(Build.VERSION_CODES.O)
*/



@Composable
fun RoundBox(dateS: String,
             todayHighlight: Boolean = false,
             onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = if(todayHighlight) Red80 else  Orange80), // Orange color
        modifier = Modifier.height(50.dp).wrapContentWidth() // Adjust the size as needed
    ) {
        Text(
            text = dateS,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
@Composable
fun RoundButton(number: Int, todayHighlight: Boolean = false,
                onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = if(todayHighlight) Red80 else  Orange80), // Orange color
        modifier = Modifier.height(50.dp).wrapContentWidth() // Adjust the size as needed
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
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


val CalendarGradientA = Color(0xFFFFFBF0)
val CalendarGradientB = Color(0xFFFCECE7)

