package com.example.todoapp.screen.missions.calendar.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
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
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.missions.DARKGREEN180
import com.example.todoapp.screen.missions.MissionItemViewModel
import com.example.todoapp.screen.missions.Yellow180
import com.example.todoapp.screen.missions.Yellow80
import com.example.todoapp.screen.missions.darken
import com.example.todoapp.screen.missions.getDarkPillarColor
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
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.number
import org.jetbrains.compose.resources.painterResource
import org.koin.core.parameter.parametersOf
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.circle_badge
import todoapp.shared.generated.resources.circle_filled_a
import todoapp.shared.generated.resources.square_b
import todoapp.shared.generated.resources.square_check_b


@Composable
fun MissionProgressCalendar(
    viewModel: DayMissionProgressViewModel,
    missionn: MissionWithDetails) {
    val miles: List<MilestoneWithDetails>? by viewModel.milestones.collectAsState(emptyList())
    val tasks: List<TodayTaskWithFewDetails>? by viewModel.tasks.collectAsState(emptyList())

    var dateWiseMiles by remember { mutableStateOf<Map<String, List<MilestoneWithDetails>?>?>(mapOf()) }
    dateWiseMiles=
        miles?.sortedBy { it.pillar?.pillarId }
            ?.groupBy { mile-> mile.milestone.expectedCompletionDate.dateString }
            ?: mapOf()

    //{date: [t1], date: [t2], date: [t3],..}
    val dateWiseTasks by remember(tasks) {
        mutableStateOf(
            tasks
                ?.sortedWith(
                    compareBy<TodayTaskWithFewDetails> { it.todayTask.taskStatus != "COMPLETED" }
                        .thenBy { it.pillar?.pillarId }
                )
                ?.groupBy { it.todayTask.taskDate.dateString }
        )
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
            CalendarArea(
                    miles=dateWiseMiles,
                    tasks=dateWiseTasks,
                    selectedMission = missionn)
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
        calendarScrollPaged = false,dayContent = { day ->
            val date = MyDate.fromLocalDate(day.date)

            // Weekly Task List (Last 7 Days from Saturday)
            val weeklyTaskList = if (day.date.dayOfWeek == DayOfWeek.SATURDAY) {
                (1..7).flatMap { n ->
                    tasks?.get(date.subtractDays(n).dateString).orEmpty()
                }
            } else {
                emptyList()
            }

            Box(modifier = Modifier.width(if(day.date.dayOfWeek == DayOfWeek.SATURDAY) 40.dp else 20.dp)) {
                key(day.date, miles, tasks) {
                    DayMission(
                        day,
                        miles = miles?.get(date.dateString) ?: listOf(),
                        tasks = tasks?.get(date.dateString) ?: listOf(),
                        weeklyTaskList,
                        selectedMission
                    )
                }
            }
        },
        monthHeader = { month ->
        },
        monthBody = { month, content ->

                Box(
                    modifier = Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = if (month.yearMonth.month.number % 2 == 0) listOf(
                                Color.White.copy(alpha = 0.70f),
                                Color.White.copy(alpha = 0.65f)
                            ) else listOf(
                                Color.White.copy(alpha = 0.65f),
                                Color.White.copy(alpha = 0.70f)
                            )
                        )
                    )
                ) {
                    content() // Render the provided content!
                }
        },
        monthContainer = { month, container ->

            // Monthly Task List (Tasks from the same month)
            val monthlyTaskList = (tasks?.filterKeys { dateString ->
                MyDate(dateString).toLocalDate().month == month.yearMonth.month
            }?.values?.flatMap { it?: listOf() })?: listOf()
            Column(modifier = Modifier.fillMaxWidth()) { // Wrap month in a Column
                val weeklycompleted  = monthlyTaskList!!.filter { t-> t.todayTask.taskStatus == "COMPLETED" }
                val freq = (selectedMission?.missionFrequency?.frequency?:1)

                MonthHeader(month) // Display month header
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
                                else if(monthlyTaskList.size > 0 ) Yellow80
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
@Composable
private fun DayMission(
    day: CalendarDay,
    miles: List<MilestoneWithDetails>?,
    tasks: List<TodayTaskWithFewDetails>?,
    weeklytasklist: List<TodayTaskWithFewDetails>,
    selectedMission: MissionWithDetails?,
) {
    val isSaturday = if(day.date.dayOfWeek == DayOfWeek.SATURDAY) true else false

    Box(modifier = Modifier
        .width(if(isSaturday) 80.dp else 20.dp)
        .height(20.dp)
    ) {
        if(isSaturday ){
            val weeklycompleted  = weeklytasklist.filter { t-> t.todayTask.taskStatus == "COMPLETED" }
            val freq = (selectedMission?.missionFrequency?.frequency?:1)
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .width(10.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        color =
                        if(selectedMission?.missionFrequency?.frequencyPeriod == "WEEKLY")
                            if(weeklycompleted.size >= freq) DARKGREEN180
                            else if(weeklycompleted.size > 0 ) Yellow180
                            else if(weeklytasklist.size > 0 ) Yellow80
                            else Color.Transparent
                        else Color.Transparent
                    )
            ) {
//                Icon(
//                    painterResource(Res.drawable.square_check_b),
//                    "dfg",
//                    tint = when (day.position) {
//                        DayPosition.MonthDate ->
//                            getPillarColor(pillarName = selectedMission?.pillar?.pillarName)
//
//                        DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
//                    }
//                )
            }
        }
        Box(
            modifier = Modifier.wrapContentSize()
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(2.dp, if( MyDate.fromLocalDate(day.date).dateString == MyDate.now().dateString) Yellow180 else Color.Transparent, RoundedCornerShape(4.dp))
                    .align(Alignment.TopStart)
            ) {

                    if(tasks != null && tasks!!.size>0 &&
                        tasks.get(0).todayTask.taskStatus  == "COMPLETED"){
                        Icon(
                            painterResource(Res.drawable.circle_badge),
                            "dfg",
                            tint = when (day.position) {
                                DayPosition.MonthDate ->
                                    getPillarColor(pillarName = selectedMission?.pillar?.pillarName)
                                DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
                            }
                        )
                    }
                    if(miles != null && miles!!.size>0 &&
                        miles.get(0).milestone.expectedCompletionDate.dateString  == MyDate.fromLocalDate(day.date).dateString){
                        Icon(painterResource(Res.drawable.square_b),
                            "sfgh",
                            tint = when (day.position) {
                                    DayPosition.MonthDate ->
                                        getDarkPillarColor(pillarName = selectedMission?.pillar?.pillarName)
                                    DayPosition.InDate, DayPosition.OutDate -> Color.Transparent
                                }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.Center)
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
