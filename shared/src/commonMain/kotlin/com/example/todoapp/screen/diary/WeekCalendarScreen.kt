package com.example.todoapp.screen.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.basicutils.components.WriteText
import com.example.todoapp.screen.basicutils.Orange40
import com.example.todoapp.screen.basicutils.Red80
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.YearMonth
import com.kizitonwose.calendar.core.atEndOfMonth
import com.kizitonwose.calendar.core.atStartOfMonth
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.minusYears
import com.kizitonwose.calendar.core.plusYears
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.circle_filled_a
import todoapp.shared.generated.resources.circle_filled_b


@Composable
fun WeekCalendarScreen(
    selectedDate: LocalDate,
    changeDate: (LocalDate) -> Unit,
    diaryViewMode: String,
    onViewModeChange: (String) -> Unit
) {
    val collapsedHeight = remember { mutableStateOf(20.dp) }

    Row(modifier = Modifier.fillMaxSize()) {
        CalendarArea(
            selectedDate = selectedDate,
            diaryViewMode = diaryViewMode,
            collapsedHeight = collapsedHeight,
            onDateClicked = { date -> changeDate(date) },
            changeToMonth = { onViewModeChange("month") },
            changeToWeek = { onViewModeChange("week") }
        )
    }
}

@Composable
private fun CalendarArea(
    selectedDate: LocalDate,
    collapsedHeight: MutableState<Dp>,
    onDateClicked: (LocalDate) -> Unit,
    changeToWeek: () -> Unit,
    changeToMonth: () -> Unit,
    diaryViewMode: String
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusYears(3) }
    val endMonth = remember { currentMonth.plusYears(3) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val daysOfWeekList = daysOfWeek(firstDayOfWeek = DayOfWeek.SUNDAY)

    var visibleMonthFirst by remember { mutableStateOf(LocalDate(selectedDate.year, selectedDate.month, 1)) }
    LaunchedEffect(diaryViewMode) {
        if (diaryViewMode == "month") {
            visibleMonthFirst = LocalDate(selectedDate.year, selectedDate.month, 1)
        }
    }

    val weekState = rememberWeekCalendarState(
        startDate = startMonth.atStartOfMonth(),
        endDate = endMonth.atEndOfMonth(),
        firstVisibleWeekDate = selectedDate,
        firstDayOfWeek = daysOfWeekList.first()
    )

    Box(modifier = Modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { collapsedHeight.value = it.height.dp }
        ) {
            if (diaryViewMode == "week") {
                WeekCalendar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, _ -> change.consume() },
                                onDragEnd = { changeToMonth() }
                            )
                        },
                    state = weekState,
                    calendarScrollPaged = false,
                    dayContent = { day ->
                        WeekDayGola(
                            day,
                            isSelected = selectedDate == day.date,
                            onDateClicked = { onDateClicked(it) }
                        )
                    }
                )
            }
        }

        if (diaryViewMode != "week") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .shadow(8.dp)
                    .zIndex(10f)
                    .background(Color.White)
            ) {
                ExpandedMonthCalendar(
                    visibleMonthFirst = visibleMonthFirst,
                    selectedDate = selectedDate,
                    firstDayOfWeek = firstDayOfWeek,
                    onMonthChange = { visibleMonthFirst = it },
                    onDateClicked = { date ->
                        onDateClicked(date)
                        changeToWeek()
                    }
                )
            }
        }
    }
}


@Composable
private fun ExpandedMonthCalendar(
    visibleMonthFirst: LocalDate,
    selectedDate: LocalDate,
    firstDayOfWeek: DayOfWeek,
    onMonthChange: (LocalDate) -> Unit,
    onDateClicked: (LocalDate) -> Unit
) {
    val weekOrder = remember(firstDayOfWeek) {
        val entries = DayOfWeek.entries
        val startIdx = entries.indexOf(firstDayOfWeek).coerceAtLeast(0)
        (0..6).map { entries[(startIdx + it) % 7] }
    }
    val monthName = visibleMonthFirst.month.name.lowercase().replaceFirstChar { it.uppercase() }
    val headerTitle = "$monthName ${visibleMonthFirst.year}"

    val leadingBlanks = weekOrder.indexOf(visibleMonthFirst.dayOfWeek).coerceAtLeast(0)
    val lastDayOfMonth = visibleMonthFirst.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
    val monthDays = (1..lastDayOfMonth).map { d -> LocalDate(visibleMonthFirst.year, visibleMonthFirst.month, d) }
    val totalCells = leadingBlanks + monthDays.size
    val trailingBlanks = (7 - (totalCells % 7)) % 7
    val gridDays: List<LocalDate?> = List(leadingBlanks) { null } + monthDays + List(trailingBlanks) { null }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    onMonthChange(visibleMonthFirst.minus(1, DateTimeUnit.MONTH))
                }
            ) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = Color.Gray
                )
            }
            Text(
                text = headerTitle,
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = {
                    onMonthChange(visibleMonthFirst.plus(1, DateTimeUnit.MONTH))
                }
            ) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = Color.Gray
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekOrder.forEach { dayOfWeek ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayOfWeek.name.take(1),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        gridDays.chunked(7).forEach { rowDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowDays.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            RoundButton(
                                date = date,
                                todayHighlight = date == selectedDate
                            ) {
                                onDateClicked(date)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekDayGola(
    day: WeekDay,
    isSelected: Boolean,
    onDateClicked: (LocalDate) -> Unit
){

    val date = day.date

    Box(modifier = Modifier.padding(2.dp)){
        RoundButton((date), todayHighlight = isSelected){
            onDateClicked(date)
        }
    }
}


@Composable
fun RoundButton(date: LocalDate, todayHighlight: Boolean = false,
                onClick: () -> Unit) {

    Box(
        modifier = Modifier.height(35.dp).width(25.dp)
            .clickable {
                onClick()
            }
    ) {
        Box(modifier = Modifier.size(25.dp).align(Alignment.BottomEnd)){
            Icon(
                painter = painterResource( Res.drawable.circle_filled_a),
                contentDescription = "circle_filled",
                tint = if(todayHighlight) Red80 else  Orange40,
                modifier = Modifier.fillMaxSize()
            )
            WriteText(
                text = ""+date.dayOfMonth,
                color = Color.Black,
                fontSize = 14f,
                modifier = Modifier.padding(top = 2.dp, start = 2.dp).align(Alignment.Center)
            )
        }
        Box(modifier = Modifier.size(16.dp).align(Alignment.TopStart)) {
            Icon(
                painter = painterResource(Res.drawable.circle_filled_b),
                contentDescription = "circle_filled",
                tint = if (todayHighlight) Red80 else Color.White,
                modifier = Modifier.fillMaxSize()
            )
            WriteText(
                text = date.dayOfWeek.name.substring(0, 1),
                fontSize = 10f,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
