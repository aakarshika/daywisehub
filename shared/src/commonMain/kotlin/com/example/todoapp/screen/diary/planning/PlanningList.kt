package com.example.todoapp.screen.diary.planning

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mission.MissionWithFewDetails
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.db.models.TaskType
import com.kizitonwose.calendar.core.minusDays
import com.kizitonwose.calendar.core.now
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

// Data class to hold a mission and its categorization reasons
enum class MissionCategory {
    RED, YELLOW, GREEN
}
// Define witty comments for each condition type
val wittyComments = mapOf(
    "DAILY_MISSED" to listOf(
        "It's been more than 2 days since you've done this daily habit!",
    ),
    "BIWEEKLY_QUOTA_MISSED" to listOf(
        "You haven't met your weekly goals in the past two weeks!",
    ),
    "WEEKLY_QUOTA_MISSED" to listOf(
        "This week's goal pending.",
    ),
    "BIMONTHLY_QUOTA_MISSED" to listOf(
        "Your monthly quota hasn't been met in two months!!",
    ),
    "MONTHLY_QUOTA_MISSED" to listOf(
        "Your monthly quota hasn't been met  this month",
    )
)

data class CategorizedMission(
    val mission: MissionWithFewDetails,
    val category: MissionCategory,
    val reasons: List<String>,
    val missionTaskToday : TodayTask?
)
@Composable
fun PlanningList(todaysDate: LocalDate, viewModel: PlanningViewModel) {

    val tasksMonth: List<TodayTask> by viewModel.tasksPastMonth.collectAsState(emptyList())
    val allMissions by viewModel.allMissions.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMissions()
        viewModel.loadTasksPastMonth()
    }

    val date = todaysDate
    val dateWiseTasks = tasksMonth.groupBy { it.taskDate.dateString }
    val twodays = (1..2).map { MyDate.fromLocalDate(date.minusDays(it)).dateString }.flatMap { dateWiseTasks[it].orEmpty() }
    val weektasks = (1..7).map { MyDate.fromLocalDate(date.minusDays(it)).dateString }.flatMap { dateWiseTasks[it].orEmpty() }
    val biweektasks = (1..15).map { MyDate.fromLocalDate(date.minusDays(it)).dateString }.flatMap { dateWiseTasks[it].orEmpty() }
    val monthtasks = (1..30).map { MyDate.fromLocalDate(date.minusDays(it)).dateString }.flatMap { dateWiseTasks[it].orEmpty() }
    val twomonth = (1..60).map { MyDate.fromLocalDate(date.minusDays(it)).dateString }.flatMap { dateWiseTasks[it].orEmpty() }

    val redMissions = remember(allMissions, tasksMonth) {
        allMissions.mapNotNull { mission ->
            val reasons = mutableListOf<String>()

            // Check if the mission is DAILY, WEEKLY, or MONTHLY and if it's missed
            when {
                mission.missionFrequency?.isDailyHabit == true || mission.missionFrequency?.frequencyPeriod == "DAILY" ->
                    if (twodays.count { it.missionId == mission.mission?.missionId && it.taskStatus == TaskStatus.COMPLETED.value } < (mission.missionFrequency?.frequency ?: 1)) {
                        reasons.add(wittyComments["DAILY_MISSED"]?.random() ?: "You've missed your daily habit!")
                    }
                mission.missionFrequency?.frequencyPeriod == "WEEKLY" ->
                    if (biweektasks.count { it.missionId == mission.mission?.missionId && it.taskStatus == TaskStatus.COMPLETED.value } < (mission.missionFrequency?.frequency ?: 1)) {
                        reasons.add(wittyComments["BIWEEKLY_QUOTA_MISSED"]?.random() ?: "You've missed your weekly habit!")
                    }
                mission.missionFrequency?.frequencyPeriod == "MONTHLY" ->
                    if (twomonth.count { it.missionId == mission.mission?.missionId && it.taskStatus == TaskStatus.COMPLETED.value } < (mission.missionFrequency?.frequency ?: 1)) {
                        reasons.add(wittyComments["BIMONTHLY_QUOTA_MISSED"]?.random() ?: "You've missed your monthly habit!")
                    }
            }

            val missionTaskToday = tasksMonth.filter {
                it.missionId == mission.mission!!.missionId && it.taskDate.dateString == MyDate.fromLocalDate(todaysDate).dateString }
            if (reasons.isNotEmpty()) {
                CategorizedMission(mission, MissionCategory.RED, reasons, missionTaskToday.firstOrNull())
            } else null
        }
    }

    val yellowMissions = remember(allMissions, tasksMonth) {
        allMissions.filterNot { redMissions.any { red -> red.mission.mission!!.missionId == it.mission!!.missionId } }
            .mapNotNull { mission ->
                val reasons = mutableListOf<String>()

                // Check if the mission is DAILY, WEEKLY, or MONTHLY and if it's partially completed
                when {
                    mission.missionFrequency?.isDailyHabit == true || mission.missionFrequency?.frequencyPeriod == "DAILY" ->
                        if (twodays.count { it.missionId == mission.mission?.missionId && it.taskStatus == TaskStatus.COMPLETED.value } < (mission.missionFrequency?.frequency ?: 1)) {
                            reasons.add(wittyComments["DAILY_MISSED"]?.random() ?: "You've missed your daily habit!")
                        }
                    mission.missionFrequency?.frequencyPeriod == "WEEKLY" ->
                        if (weektasks.count { it.missionId == mission.mission?.missionId && it.taskStatus == TaskStatus.COMPLETED.value } < (mission.missionFrequency?.frequency ?: 1)) {
                            reasons.add(wittyComments["WEEKLY_QUOTA_MISSED"]?.random() ?: "You've missed your weekly habit!")
                        }
                    mission.missionFrequency?.frequencyPeriod == "MONTHLY" ->
                        if (monthtasks.count { it.missionId == mission.mission?.missionId && it.taskStatus == TaskStatus.COMPLETED.value } < (mission.missionFrequency?.frequency ?: 1)) {
                            reasons.add(wittyComments["MONTHLY_QUOTA_MISSED"]?.random() ?: "You've missed your monthly habit!")
                        }
                }

                if (reasons.isNotEmpty()) {
                    val missionTaskToday = tasksMonth.filter {
                        it.missionId == mission.mission!!.missionId && it.taskDate.dateString == MyDate.fromLocalDate(todaysDate).dateString }
                    CategorizedMission(mission, MissionCategory.YELLOW, reasons, missionTaskToday.firstOrNull())
                } else null
            }
    }

    val greenMissions = remember(allMissions,tasksMonth) {
        allMissions.filterNot { mission ->
            redMissions.any { red -> red.mission.mission!!.missionId == mission.mission!!.missionId } ||
                    yellowMissions.any { yellow -> yellow.mission.mission!!.missionId == mission.mission!!.missionId }
        }.mapNotNull { mission ->
            val missionTaskToday = tasksMonth.filter {
                it.missionId == mission.mission!!.missionId && it.taskDate.dateString == MyDate.fromLocalDate(todaysDate).dateString }
            CategorizedMission(mission, MissionCategory.GREEN, listOf("You're on top of your missions"), missionTaskToday.firstOrNull())
        }
    }

    Box{
        Row{
            LazyColumn {
                MissionItems(
                    title = "Critically lagging",
                    missionList = redMissions,
                    missionAdded = {mission: CategorizedMission ->
                        addRemoveTask(tasksMonth, todaysDate, mission, viewModel)
                    },
                    missionHighlighted = {mission: CategorizedMission ->
                        toggleHighlightTask( mission, viewModel)
                    }
                )
                MissionItems(
                    title = "Remaining this week",
                    missionList = yellowMissions,
                    missionAdded = {mission: CategorizedMission ->
                        addRemoveTask(tasksMonth, todaysDate, mission, viewModel)
                    },
                    missionHighlighted = {mission: CategorizedMission ->
                        toggleHighlightTask( mission, viewModel)
                    }
                )
                MissionItems(
                    title = "Doing well",
                    missionList = greenMissions,
                    missionAdded = {mission: CategorizedMission ->
                        addRemoveTask(tasksMonth, todaysDate, mission, viewModel)
                    },
                    missionHighlighted = {mission: CategorizedMission ->
                        toggleHighlightTask( mission, viewModel)
                    }
                )
            }
        }
    }
}

private fun addRemoveTask(
    tasksMonth: List<TodayTask>,
    todaysDate: LocalDate,
    mission: CategorizedMission,
    viewModel: PlanningViewModel
) {
    val taskId = (tasksMonth.find { t ->
        t.taskDate.dateString == MyDate.fromLocalDate(todaysDate).dateString && t.missionId == mission.mission.mission!!.missionId
    }?.todayTaskId)?:0

    if (mission.missionTaskToday?.active == "Y") {
        viewModel.deleteTaskForToday(taskId)
    } else {
        if ((mission.missionTaskToday?.todayTaskId?:0) > 0) {
            viewModel.activateTaskForToday(taskId)
        } else {
            viewModel.addTaskForToday(todaysDate, mission.mission.mission!!)
        }
    }
}
private fun toggleHighlightTask(
    mission: CategorizedMission,
    viewModel: PlanningViewModel
) {
    if (mission.missionTaskToday?.taskPageTag == TaskType.TOP3.value) {
        viewModel.updateTag(mission.missionTaskToday!!.todayTaskId, TaskType.TODO.value)
    } else {
        viewModel.updateTag(mission.missionTaskToday!!.todayTaskId, TaskType.TOP3.value)
    }
}

fun LazyListScope.MissionItems(
    title: String,
    missionList: List<CategorizedMission>,
    missionAdded: (CategorizedMission) -> Unit,
    missionHighlighted: (CategorizedMission) -> Unit) {
    item {
        if(!missionList.isNullOrEmpty()) {
            Text("$title", modifier = Modifier.padding(start = 15.dp))
        }
    }
    itemsIndexed(missionList) { i, mission ->
        Box(modifier = Modifier) {
            PlanningListItem(
                mission = mission,
                extraInfo = "",
                onMissionClick = {m-> missionAdded(m) },
                onHighlightClick = {m-> missionHighlighted(m) }
            )
        }
    }
}
