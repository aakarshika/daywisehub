package com.example.todoapp.screen.diary

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import com.example.todoapp.getScreenHeight
import com.example.todoapp.getTopCalHeight
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.Red80
import com.example.todoapp.screen.metrics.CalendarGradientA
import com.example.todoapp.screen.metrics.CalendarGradientB
import com.example.todoapp.screen.metrics.WeekDayGola
import com.example.todoapp.screen.missions.Orange80
import com.example.todoapp.screen.missions.calendar.progress.MonthHeader
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
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
import kotlinx.datetime.number


@Composable
fun WeekCalendarScreen(selectedDate: LocalDate, changeDate: (LocalDate) -> Unit) {

    var diaryViewMode by remember { mutableStateOf("week") }

    Row(modifier = Modifier.fillMaxSize()) {
        CalendarArea(
            selectedDate = selectedDate,
            diaryViewMode = diaryViewMode,
            onDateClicked = { date ->
                Logger.e("DATE CLICKED $date cal area")

                changeDate(date)
            },
            changeToMonth = {diaryViewMode = "month"},
            changeToWeek = {diaryViewMode = "week"}
        )
    }
}

@Composable
private fun CalendarArea(
    selectedDate: LocalDate,
    onDateClicked: (LocalDate) -> Unit,
    changeToWeek: () -> Unit,
    changeToMonth: () -> Unit,
    diaryViewMode: String
) {

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
        ) {
            if (diaryViewMode == "week") {
                //week calendar
                WeekCalendar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(getTopCalHeight())
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                },
                                onDragEnd = { changeToMonth() }
                            )
                        },
                    state = weekState,
                    calendarScrollPaged = true,
                    dayContent = { day ->
                        WeekDayGola(
                            day,
                            isSelected = selectedDate == day.date,
                            onDateClicked = {
                                Logger.e("DATE CLICKED $it")
                                onDateClicked(it)
                            }
                        )
                    }
                )
            }
            else {
                //month calendar
                HorizontalCalendar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    state = state,
                    calendarScrollPaged = true,
                    dayContent = { day ->
                        MonthDay(
                            day,
                            isSelected = selectedDate == day.date,
                            onDateClicked = {
                                onDateClicked(it)
                                changeToWeek()
                            }
                        )
                    }
                )
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
    Box(modifier = Modifier.wrapContentSize()) {

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .size(30.dp)
                .testTag("MonthDay")
                .padding(2.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    color = when (day.position) {
                        DayPosition.MonthDate -> if (isSelected) Red80 else Orange80
                        DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
                    }
                )
                .clickable(
                    enabled = day.position == DayPosition.MonthDate,
                    onClick = { onDateClicked(day.date) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            val textColor = when (day.position) {
                DayPosition.MonthDate -> if (isSelected) Color.Gray else Color.Black
                DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
            }
            Text(
                text = day.date.dayOfMonth.toString(),
                color = textColor,
                fontSize = 14.sp,
            )
        }

    }
}

