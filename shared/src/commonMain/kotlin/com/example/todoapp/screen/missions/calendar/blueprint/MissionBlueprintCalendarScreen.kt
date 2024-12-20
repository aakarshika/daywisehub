package com.example.todoapp.screen.missions.calendar.blueprint

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.screen.missions.darken
import com.example.todoapp.screen.missions.getPillarColor
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.YearMonth
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.minusYears
import com.kizitonwose.calendar.core.plusYears
import kotlinx.datetime.number
import kotlin.random.Random


@Composable
fun MissionBlueprintCalendarScreen(selectedMission: MutableState<Mission?>, pillar: MutableState<Pillar?>) {

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {

        Column {
            CalendarArea( selectedMission, pillar)
        }
    }
}

@Composable
private fun CalendarArea(
    selectedMission: MutableState<Mission?>,
    pillar: MutableState<Pillar?>
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
            DayMission(day,
                selectedMission,
                pillar
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
        }
    )
}

@Composable
private fun DayMission(
    day: CalendarDay,
    selectedMission: MutableState<Mission?>,
    pillar: MutableState<Pillar?>
) {
    Box(modifier = Modifier.wrapContentSize()) {

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .size(20.dp)
                .testTag("MonthDay")
                .padding(2.dp)
                .clip(CircleShape)
                .background(
                    color = if(selectedMission.value?.frequencySetValue?:0f > Random.nextFloat()) when (day.position) {
                        DayPosition.MonthDate -> getPillarColor(pillar.value?.pillarName).darken()
                        DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
                    } else Color.Transparent
                )
                // Disable clicks on inDates/outDates
                .clickable(
                    enabled = day.position == DayPosition.MonthDate,
                    onClick = {  },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.Center)
            ) {
            }
            val textColor = when (day.position) {
                DayPosition.MonthDate ->  Color.Black
                DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
            }
            Text(
                text = day.date.dayOfMonth.toString(),
                color = textColor,
                fontSize = 11.sp,
            )
        }

    }
}
