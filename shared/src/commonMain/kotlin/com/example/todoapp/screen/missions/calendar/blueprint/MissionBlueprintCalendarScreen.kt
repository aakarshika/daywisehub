package com.example.todoapp.screen.missions.calendar.blueprint

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.missions.DARKGREEN180
import com.example.todoapp.screen.missions.GREEN80
import com.example.todoapp.screen.missions.Gray40
import com.example.todoapp.screen.missions.Gray60
import com.example.todoapp.screen.missions.Yellow180
import com.example.todoapp.screen.missions.Yellow80
import com.example.todoapp.screen.missions.calendar.progress.DayMissionProgressViewModel
import com.kizitonwose.calendar.compose.HeatMapCalendar
import com.kizitonwose.calendar.compose.heatmapcalendar.rememberHeatMapCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.YearMonth
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.minusYears
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusYears
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.circle_badge
import todoapp.shared.generated.resources.crown_b
import todoapp.shared.generated.resources.crown_filled_b
import todoapp.shared.generated.resources.square_b
import todoapp.shared.generated.resources.square_filled_a
import todoapp.shared.generated.resources.star_border
import todoapp.shared.generated.resources.star_filled


@Composable
fun MissionBlueprintCalendarScreen(
    selectedMission: MutableState<Mission?>,
    selectedMissionFrequency: MutableState<MissionFrequency?>,
    pillar: MutableState<Pillar?>,
    viewModel: DayMissionProgressViewModel
) {
    val miles: List<MilestoneWithDetails>? by viewModel.milestones.collectAsState(emptyList())
    val tasks: List<TodayTaskWithFewDetails>? by viewModel.tasks.collectAsState(emptyList())

    val dateWiseMiles by remember(miles) {
        mutableStateOf(
        miles
            ?.sortedBy { it.pillar.pillarId }
            ?.groupBy { mile-> mile.milestone.expectedCompletionDate.dateString }
            ?: mapOf()
    ) }

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
            CalendarArea( selectedMission,selectedMissionFrequency, pillar, dateWiseMiles,dateWiseTasks)
        }
    }
}
@Composable
private fun CalendarArea(
    selectedMission: MutableState<Mission?>,
    selectedMissionFrequency: MutableState<MissionFrequency?>,
    pillar: MutableState<Pillar?>,
    miles: Map<String,List<MilestoneWithDetails>?>?,
    tasks: Map<String,List<TodayTaskWithFewDetails>?>?,
) {


    val currentMonth = remember { YearMonth.now() }
    val currentYear = remember { currentMonth.year }

    val startMonth = remember { currentMonth.minusYears(3) } // Adjust as needed
    val endMonth = remember { currentMonth.plusYears(3) } // Adjust as needed
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() } // Available from the library

    val state = rememberHeatMapCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = DayOfWeek.MONDAY,
    )
    HeatMapCalendar(
        modifier = Modifier.fillMaxWidth(),
        state = state,
        dayContent = { day, week ->
            val date = MyDate.fromLocalDate(day.date)

            DayMission(day,
                selectedMission,
                selectedMissionFrequency,
                miles = miles?.get(date.dateString) ?: listOf(),
                tasks = tasks?.get(date.dateString) ?: listOf(),
                pillar
            )
        },
        monthHeader = {
            Text("${it.yearMonth.month.name}")
        }
    )
}
@Composable
private fun DayMission(
    day: CalendarDay,
    selectedMission: MutableState<Mission?>,
    selectedMissionFrequency: MutableState<MissionFrequency?>,
    miles: List<MilestoneWithDetails>?,
    tasks: List<TodayTaskWithFewDetails>?,
    pillar: MutableState<Pillar?>
) {
    // Get current date for comparison
    val currentDate = remember { LocalDate.now() }
    val biasedFrequency = selectedMissionFrequency.value!!.frequency + (listOf(1,0,-1).random())

    // Determine the background color based on date
    val dateColor = when {
        day.date == currentDate -> Yellow180
        day.date.compareTo(currentDate) < 0-> Gray60
        else -> if((selectedMissionFrequency.value!!.frequencyPeriod =="WEEKLY" &&
                    biasedFrequency>=day.date.dayOfWeek.isoDayNumber)
            || (selectedMissionFrequency.value!!.frequencyPeriod =="MONTHLY" &&
                    biasedFrequency>=day.date.dayOfMonth)
            || (selectedMissionFrequency.value!!.frequencyPeriod =="DAILY"))
            GREEN80 else Gray40
    }

    Box(modifier = Modifier.wrapContentSize()) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = Color.White
                )
                .clip(RoundedCornerShape(3.dp))
                // Disable clicks on inDates/outDates
                .clickable(
                    enabled = day.position == DayPosition.MonthDate,
                    onClick = { },
                ),
            contentAlignment = Alignment.Center,
        ) {

            Box(
                modifier = Modifier
                    .size(15.dp)
                    .align(Alignment.Center)
                    .background(dateColor)
            ){
                if((tasks?.filter { it.todayTask.taskStatus=="COMPLETED" }?.size?:0)>0) {
                    Icon(
                        painterResource(
                            Res.drawable.square_filled_a
                    ),
                    contentDescription = "Task status",
                    tint = DARKGREEN180
                    )
                } else if((tasks?.size?:0)>0) {
                    Icon(
                        painterResource(
                            Res.drawable.square_b
                        ),
                        contentDescription = "Task status",
                        tint = DARKGREEN180
                    )
                }
                if ((miles?.size ?: 0) > 0){
                    Icon(
                        painterResource(

                            if(day.date.compareTo(currentDate) < 0)
                                Res.drawable.star_filled
                            else Res.drawable.star_border

                        ),
                        contentDescription = "miles status",
                        tint = Color.DarkGray
                    )
                }
            }

            if(tasks?.any { it.todayTask.taskPageTag=="TOP3" } == true) {
                Box(modifier = Modifier.padding(end = 5.dp, bottom = 5.dp)) {
                    Icon(
                        painterResource(
                            Res.drawable.crown_filled_b
                        ),
                        contentDescription = "Task status",
                        tint = Yellow80
                    )
                    Icon(
                        painterResource(
                            Res.drawable.crown_b
                        ),
                        contentDescription = "Task status",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}