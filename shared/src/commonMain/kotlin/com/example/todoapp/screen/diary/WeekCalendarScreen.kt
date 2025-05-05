package com.example.todoapp.screen.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import co.touchlab.kermit.Logger
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.basicutils.components.WriteText
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.Red80
import com.example.todoapp.screen.missions.Orange40
import com.example.todoapp.screen.missions.Orange80
import com.example.todoapp.screen.missions.calendar.progress.MonthHeader
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
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
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.circle_filled_a
import todoapp.shared.generated.resources.circle_filled_b


@Composable
fun WeekCalendarScreen(selectedDate: LocalDate, changeDate: (LocalDate) -> Unit) {

    var diaryViewMode by remember { mutableStateOf("week") }
    val collapsedHeight = remember { mutableStateOf(20.dp) }

    Row(modifier = Modifier.fillMaxSize()) {
        CalendarArea(
            selectedDate = selectedDate,
            diaryViewMode = diaryViewMode,
            collapsedHeight = collapsedHeight,
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
        firstVisibleWeekDate = selectedDate,
        firstDayOfWeek = daysOfWeek.first()
    )


    Box(modifier = Modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged {
                    collapsedHeight.value = it.height.dp
                }
        ) {
            if (diaryViewMode == "week") {
                //week calendar
                WeekCalendar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
//                        .height(getTopCalHeight())
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                },
                                onDragEnd = { changeToMonth() }
                            )
                        },
                    state = weekState,
                    calendarScrollPaged = false,
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

        }

        if(diaryViewMode!="week") {
            Box(modifier = Modifier
//                .height(collapsedHeight.value)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .shadow(8.dp)
                        .zIndex(10f)
                ) {
                    //month calendar
                    HorizontalCalendar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        state = state,
                        calendarScrollPaged = true,
                        monthHeader = {month ->
                            MonthHeader(month)
                        },
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
                .size(if (isSelected) 35.dp else 30.dp)
                .testTag("MonthDay")
                .padding(if (isSelected) 1.dp else 4.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    color = when (day.position) {
                        DayPosition.MonthDate -> if (isSelected) Red80 else Orange80
                        DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
                    }
                )
                .border(2.dp, if( MyDate.fromLocalDate(day.date).dateString == MyDate.now().dateString) Color.White else Color.Transparent, RoundedCornerShape(5.dp))
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
                tint = if(todayHighlight) com.example.todoapp.screen.missions.Red80 else  Orange40,
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
                tint = if (todayHighlight) com.example.todoapp.screen.missions.Red80 else Color.White,
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
