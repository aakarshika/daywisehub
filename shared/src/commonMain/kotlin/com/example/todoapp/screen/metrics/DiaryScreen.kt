package com.example.todoapp.screen.metrics


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.room.Embedded
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskReminder
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.metrics.components.Blue80
import com.example.todoapp.screen.metrics.components.GeneralItem
import com.example.todoapp.screen.metrics.components.GeneralItemCheckbox
import com.example.todoapp.screen.metrics.components.GeneralItemHabit
import com.example.todoapp.screen.metrics.components.GeneralItemTop3Star
import com.example.todoapp.screen.metrics.components.GeneralItemTopButton
import com.example.todoapp.screen.metrics.components.ListHeader
import com.example.todoapp.screen.metrics.components.MoodHeader
import com.example.todoapp.screen.metrics.components.WeatherHeader
import com.example.todoapp.screen.metrics.components.HabitItemViewModel
import com.example.todoapp.screen.metrics.components.MenuItem
import com.example.todoapp.screen.metrics.components.MoodViewModel
import com.example.todoapp.screen.metrics.components.NotebookLine
import com.example.todoapp.screen.missions.Orange80
import com.kizitonwose.calendar.core.minusDays
import kotlinx.datetime.LocalDate
import org.koin.core.parameter.parametersOf

data class ComboTask(
    @Embedded val todayTask: TodayTask?,
    @Embedded val todayTaskReminder: TodayTaskReminder?,
    @Embedded val mission: Mission?,
    @Embedded val pillar: Pillar?,
    @Embedded val missionFrequency: MissionFrequency?
) {
    override fun toString(): String {
        return if(missionFrequency?.isDailyHabit == true)
            "habit(" +
                    "'m${mission?.missionId?:0L},t${todayTask?.todayTaskId?:0L}'${mission?.missionTitle}-${pillar?.pillarName}." +
                    (todayTask?.taskDate?.dateString?:"")+
                    (if (todayTask?.taskStatus=="COMPLETED") "/CMPLD" else "/${todayTask?.taskProgressVal}")+
                    (if (todayTask?.taskPageTag=="TOP3") "/T3" else "")+
                    ")"
        else "todo(" +
                "'m${mission?.missionId?:0L},t${todayTask?.todayTaskId?:0L}'${mission?.missionTitle}-${pillar?.pillarName}." +
                (todayTask?.taskDate?.dateString?:"")+
                (if (todayTask?.taskStatus=="COMPLETED") "/CMPLD" else "/${todayTask?.taskProgressVal}")+
                (if (todayTask?.taskPageTag=="TOP3") "/T3" else "")+
                ")"
    }
}



@Composable
fun DiaryScreen(
    selection: LocalDate,
    calMode: String,
    diaryViewModel: DiaryViewModel,
    diaryLoaded: () -> Unit

    ) {
    val selectedTaskList: List<ComboTask>? by diaryViewModel.dayTasks.collectAsState(emptyList())
    val habitTasks: List<ComboTask>? by diaryViewModel.habitList.collectAsState(emptyList())

    LaunchedEffect(selection) {
//        if(selection!=LocalDate.fromEpochDays(0)) {
            diaryViewModel.loadTaskDetails()
//        }
    }
    LaunchedEffect(Unit) {
//        if(selection!=LocalDate.fromEpochDays(0)) {
//            diaryViewModel.loadTaskDetails()
//        }
    }
//    LaunchedEffect(showDialog.value){
//        if(showDialog.value){
//            diaryViewModel.loadTaskDetails()
//            showDialog.value = false
//        }
//    }
    val all : List<ComboTask> = (selectedTaskList?.toList() ?: listOf()) + (habitTasks?.toList() ?: listOf())
    val allTasks = all
                        .sortedBy { it.missionFrequency?.isDailyHabit == true }
                        .sortedBy {  it.todayTask?.taskPageTag!="TOP3" }

//    val tasks= allTasks?.filter {  it.todayTask?.taskPageTag=="TODO" }
//    val top3Tasks= allTasks?.filter {  it.todayTask?.taskPageTag=="TOP3" }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Light_Yellowww)
    ) {
        val editingTaskMode = remember { mutableStateOf( "ViewItems") }
        val magicMode = remember { mutableStateOf("CHECK") }
        Column {
                LazyColumn(modifier = Modifier) {
                    MoodHeaderItem(
                        selection
                    )
                    WeatherHeaderItem()
                    ExtraLines(1)
                    TodoListItems(selection, allTasks,
                        editingTaskMode, magicMode, diaryViewModel)
                    TodoMenuItems(selection, diaryViewModel, editingTaskMode)
                    ExtraLines(7)
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    item {
                        Spacer(modifier = Modifier.height(500.dp))
                    }
                }
        }
    }

}

private fun LazyListScope.ExtraLines(n: Int ) {
    item {
        Column {
            (1..n).forEach {
                NotebookLine()
            }
        }
    }
}

private fun LazyListScope.TodoMenuItems(
    selection: LocalDate,
    diaryViewModel: DiaryViewModel,
    editingTaskMode: MutableState<String>
) {
    item{
        MenuItem(
            selection,
            52,
            editingTaskMode.value,
            prioritizeClicked={
                editingTaskMode.value = it
            }, addRandomTask = {
                diaryViewModel.addRandomTask(selection)
            }
        )
    }
}

private fun LazyListScope.MoodHeaderItem(
    selection: LocalDate
) {
    item {
        MoodHeader(
            selection,
            KoinF.di?.get<MoodViewModel> { parametersOf(selection) }!!
        )
    }
}

private fun LazyListScope.WeatherHeaderItem() {
    item {
        WeatherHeader()
    }
}




private fun LazyListScope.TodoListItems(
    selection: LocalDate,
    allTasks: List<ComboTask>?,
    editingTaskMode: MutableState<String>,
    magicMode: MutableState<String>,
    taskViewModel: DiaryViewModel,
) {
    item {
        if(!allTasks.isNullOrEmpty()) {
//            ListHeader("TO DO", modifier = Modifier.animateItem())
        }
    }
    itemsIndexed(allTasks?: listOf(), key = { _, task ->
        ((task.mission?.missionId?:0L)*1000)+(task.todayTask?.todayTaskId?:0L)
    }) { i, task ->
        Box(modifier = Modifier.animateItem()) {
            DiaryItem(selection, task, editingTaskMode, taskViewModel, magicMode,
                KoinF.di?.get<HabitItemViewModel> { parametersOf(selection,task.mission?.missionId) }!!
            )
        }
    }
}
