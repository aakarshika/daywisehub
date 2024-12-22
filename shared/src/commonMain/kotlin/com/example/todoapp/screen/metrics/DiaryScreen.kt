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
import androidx.compose.foundation.layout.size
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
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.todotask.HabitTaskWithFewDetails
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskReminder
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.metrics.components.GeneralItem
import com.example.todoapp.screen.metrics.components.GeneralItemCheckbox
import com.example.todoapp.screen.metrics.components.GeneralItemHabit
import com.example.todoapp.screen.metrics.components.GeneralItemTopButton
import com.example.todoapp.screen.metrics.components.ListHeader
import com.example.todoapp.screen.metrics.components.MoodHeader
import com.example.todoapp.screen.metrics.components.WeatherHeader
import com.example.todoapp.screen.metrics.components.HabitItemViewModel
import com.example.todoapp.screen.missions.Orange80
import com.kizitonwose.calendar.core.minusDays
import kotlinx.datetime.LocalDate
import org.koin.core.parameter.parametersOf


@Composable
fun DiaryScreen(
    selection: LocalDate,
    diaryViewModel: DiaryViewModel
    ) {
    val selectedTaskList: List<TodayTaskWithFewDetails>? by diaryViewModel.dayTasks.collectAsState(emptyList())
    val habitTasks: List<HabitTaskWithFewDetails>? by diaryViewModel.habitList.collectAsState(emptyList())

    LaunchedEffect(selection) {
        diaryViewModel.loadTaskDetails()
    }
//    LaunchedEffect(showDialog.value){
//        if(showDialog.value){
//            diaryViewModel.loadTaskDetails()
//            showDialog.value = false
//        }
//    }
    val allTasks = selectedTaskList?.filter { it.missionFrequency?.isDailyHabit != true }

    val tasks= allTasks?.filter {  it.todayTask.taskPageTag=="TODO" }
    val top3Tasks= allTasks?.filter {  it.todayTask.taskPageTag=="TOP3" }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Light_Yellowww)
    ) {
        val editingTaskMode = remember { mutableStateOf( "ViewItems") }
        val magicMode = remember { mutableStateOf("CHECK") }
        Column {
                LazyColumn(modifier = Modifier) {
                    MoodHeaderItem()
                    WeatherHeaderItem()
                    TodoListItems(
                        selection,
                        tasks,
                        top3Tasks,
                        editingTaskMode,
                        magicMode,
                        diaryViewModel
                    )
                    HabitListItems(
                        selection,
                        habitTasks,
                        editingTaskMode,
                        magicMode,
                        diaryViewModel
                    )
                    item {
                        val isLoading = remember { mutableStateOf(false) }
                        if (allTasks != null) {
                            if (isLoading.value && allTasks.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        Row{
                            Box(modifier = Modifier.clickable{
                                Logger.e("addtask")
                                diaryViewModel.addRandomTask()
                            }){ Text("               +    ") }

                            Box(modifier = Modifier.clickable{
                                Logger.e("check/draw")
                                if(magicMode.value=="CHECK")
                                    magicMode.value = "DRAW" else
                                        magicMode.value = "CHECK"
                            }){ Text(if(magicMode.value=="CHECK") " CHECK " else " DRAW ") }

                            Box(modifier = Modifier.clickable{
                                Logger.e("Is Priority?")
                                if (editingTaskMode.value == "ViewItems")
                                    editingTaskMode.value = "prioritize"
                                else
                                    editingTaskMode.value = "ViewItems"
                            }){ Text(if(editingTaskMode.value=="prioritize") "                  Done " else "    Prioritize ") }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(500.dp))
                    }
                }
        }
    }

}
private fun LazyListScope.MoodHeaderItem() {
    item {
        MoodHeader()
    }
}

private fun LazyListScope.WeatherHeaderItem() {
    item {
        WeatherHeader()
    }
}


private fun LazyListScope.HabitListItems(
    selection: LocalDate,
    habitTasks: List<HabitTaskWithFewDetails>?,
    editingTaskMode: MutableState<String>,
    magicMode: MutableState<String>,
    taskViewModel: DiaryViewModel,
) {
    item {
        if(!habitTasks.isNullOrEmpty()) {
            ListHeader("HABITS", modifier = Modifier.animateItem())
        }
    }
    itemsIndexed(habitTasks?: listOf(), key = { _, task ->
        task.mission.missionId*1000+(task.todayTask?.todayTaskId?:0L)
    }) { i, task ->
        if (true) {
            Box(modifier = Modifier.animateItem()) {
                HabitItem(selection, task, editingTaskMode, taskViewModel, magicMode, KoinF.di?.get<HabitItemViewModel> { parametersOf(selection,task.mission?.missionId) }!!)
            }
        }
    }
}

private fun LazyListScope.TodoListItems(
    selection: LocalDate,
    tasks: List<TodayTaskWithFewDetails>?,
    top3tasks: List<TodayTaskWithFewDetails>?,
    editingTaskMode: MutableState<String>,
    magicMode: MutableState<String>,
    taskViewModel: DiaryViewModel,
) {
    item {
        if(!top3tasks.isNullOrEmpty()) {
            ListHeader("PRIORITIES", modifier = Modifier.animateItem())
        }
    }
    itemsIndexed(top3tasks?: listOf(), key = { _, task ->
        task.mission!!.missionId*1000+(task.todayTask?.todayTaskId?:0L)
    }) { i, task ->
        if (true) {
            Box(modifier = Modifier.animateItem()) {
                TodoItem(selection, task, editingTaskMode, taskViewModel, magicMode)
            }
        }
    }
    item {
        if(true) {
            ListHeader("TO DO", modifier = Modifier.animateItem())
        }
    }
    itemsIndexed(tasks?: listOf(), key = { _, task ->
        task.mission!!.missionId*1000+(task.todayTask?.todayTaskId?:0L)
    }) { i, task ->
        if (true) {
            Box(modifier = Modifier.animateItem()) {
                TodoItem(selection, task, editingTaskMode, taskViewModel, magicMode)
            }
        }
    }
}

@Composable
private fun TodoItem(
    selection: LocalDate,
    task: TodayTaskWithFewDetails,
    editingTaskMode: MutableState<String>,
    taskViewModel: DiaryViewModel,
    magicMode: MutableState<String>
) {
    GeneralItem(task.mission!!)
    if (editingTaskMode.value == "prioritize") {
        GeneralItemTopButton(task, editingTaskMode.value,
            taskIsTop = { tt, check ->
                if (check) {
                    updateMoveTask(task.todayTask, "TOP3", taskViewModel)
                } else {
                    updateMoveTask(task.todayTask, "TODO", taskViewModel)
                }
            }
        )
    } else
        if (editingTaskMode.value == "ViewItems" && magicMode.value == "CHECK") {
            GeneralItemCheckbox(task.todayTask,task.pillar, editingTaskMode.value,
                taskErased = { tt, check ->
                    if (check) {
                        updateTaskStatus(task.todayTask, "COMPLETED", taskViewModel)
                    } else {
                        updateTaskStatus(task.todayTask, "REFRESHED", taskViewModel)
                    }
                }
            )
        }
}
@Composable
private fun HabitItem(
    selection: LocalDate,
    hTask: HabitTaskWithFewDetails,
    editingTaskMode: MutableState<String>,
    taskViewModel: DiaryViewModel,
    magicMode: MutableState<String>,
    habitViewModel: HabitItemViewModel
) {
    GeneralItem(hTask.mission)
        val taskProgressWeekList:List<TodayTask>? by habitViewModel.taskProgressForPastAround.collectAsState(emptyList())
        LaunchedEffect(selection){
            habitViewModel.loadTaskProgressForPastAround()
        }
        Box(modifier = Modifier.height(40.dp).fillMaxWidth()){
            Row(modifier = Modifier.fillMaxWidth().padding(end = 5.dp), horizontalArrangement = Arrangement.End) {
                (1..6).forEach {
                    val boxDate = MyDate.fromLocalDate(selection.minusDays(5 - (it)))
                    val t = taskProgressWeekList?.find { it.taskDate.dateString == boxDate.dateString }
                    if(it==5){
                        Box(
                            modifier = Modifier
                                .padding(start=4.dp, top = 5.dp)
                                .size(15.dp).clip(RoundedCornerShape(4.dp))
                                .align(Alignment.CenterVertically)
                                .background(Color.LightGray)
                        ){Box(
                            modifier = Modifier
                                .size(13.dp).clip(RoundedCornerShape(4.dp))
                                .align(Alignment.Center)
                                .background(
                                    if (hTask.todayTask?.taskStatus == "COMPLETED") Orange80
                                    else Color.White
                                )
                        )}
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp, top = 5.dp)
                                .size(13.dp).clip(RoundedCornerShape(4.dp))
                                .align(Alignment.CenterVertically)
                                .background(
                                    if (t?.taskStatus == "COMPLETED") Orange80
                                    else Color.White
                                )
                        )
                    }
                }
            }
        }
        if (editingTaskMode.value == "ViewItems" && magicMode.value == "CHECK") {
            GeneralItemCheckbox(hTask.todayTask ,hTask.pillar, editingTaskMode.value,
                taskErased = { tt, check ->
                    if((hTask.todayTask?.todayTaskId ?: 0L) > 0L) {
                        updateTaskStatus(hTask.todayTask!!, if (check) "COMPLETED" else "REFRESHED", taskViewModel)
                    } else {
                        val newTask = TodayTaskWithFewDetails(
                            todayTask = TodayTask(
                                todayTaskId = 0L, // Example task ID
                                userId = 1L, // Example userId
                                missionId = hTask.mission.missionId,
                                taskStatus = if (check) "COMPLETED" else "REFRESHED",
                                taskDate = MyDate.fromLocalDate(selection) ,
                                taskType = "USER_HABIT", // Example type
                                taskPageTag = "TODO",
                                taskProgressVal = if (check) 1f else 0f, // Example progress
                                taskText = "", // Example text
                                taskPictureUrl = null, // Example URL (can be null)
                                taskLink = null // Example link (can be null)
                            ),
                            todayTaskReminder = TodayTaskReminder(
                                todayTaskId = 0L, // Example task ID
                                timeOfDay = "08:00", // Example time
                                alarmTone = "Default Tone", // Example alarm tone
                                timeBefore = 10, // Example time before
                                timeBeforeUnit = 1 // Example time before unit (e.g., minutes)
                            ),
                            mission = hTask.mission,
                            pillar = hTask.pillar,
                            missionFrequency = hTask.missionFrequency
                        )
                        taskViewModel.insertTask(newTask)
                    }
//                    habitViewModel.loadTaskProgressForPastAround()
                }
            )
        }
}


fun updateTaskStatus(task: TodayTask, taskStatus: String, taskViewModel: DiaryViewModel){
    taskViewModel.updateTaskStatus(task.todayTaskId, taskStatus)
}


fun updateMoveTask(task: TodayTask, moveTo:String, taskViewModel: DiaryViewModel){
    taskViewModel.updateTaskTag(task.todayTaskId,moveTo)
}
//
//
//fun updateTaskHighlightLines(taskWithDetails: TodayTaskWithFewDetails, eraseLines:List<HighlightTaskLine>, taskViewModel: TaskViewModel){
//    taskViewModel.updateFullTaskUi(
//        taskWithDetails.copy(
//            taskUiData = taskWithDetails.taskUiData.copy(
//                highlightLines = eraseLines
//            )
//        )
//    )
//}


