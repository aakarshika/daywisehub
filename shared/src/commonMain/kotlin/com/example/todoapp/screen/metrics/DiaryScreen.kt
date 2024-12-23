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
import com.example.todoapp.screen.metrics.components.GeneralItemTopButton
import com.example.todoapp.screen.metrics.components.ListHeader
import com.example.todoapp.screen.metrics.components.MoodHeader
import com.example.todoapp.screen.metrics.components.WeatherHeader
import com.example.todoapp.screen.metrics.components.HabitItemViewModel
import com.example.todoapp.screen.metrics.components.MoodViewModel
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
                    (if (todayTask?.taskStatus=="COMPLETED") "/CMPLD" else "")+
                    (if (todayTask?.taskPageTag=="TOP3") "/T3" else "")+
                    ")"
        else "todo(" +
                "'m${mission?.missionId?:0L},t${todayTask?.todayTaskId?:0L}'${mission?.missionTitle}-${pillar?.pillarName}." +
                (todayTask?.taskDate?.dateString?:"")+
                (if (todayTask?.taskStatus=="COMPLETED") "/CMPLD" else "")+
                (if (todayTask?.taskPageTag=="TOP3") "/T3" else "")+
                ")"
    }
}



@Composable
fun DiaryScreen(
    selection: LocalDate,
    diaryViewModel: DiaryViewModel
    ) {
    val selectedTaskList: List<ComboTask>? by diaryViewModel.dayTasks.collectAsState(emptyList())
    val habitTasks: List<ComboTask>? by diaryViewModel.habitList.collectAsState(emptyList())

    LaunchedEffect(selection) {
        diaryViewModel.loadTaskDetails()
    }
//    LaunchedEffect(showDialog.value){
//        if(showDialog.value){
//            diaryViewModel.loadTaskDetails()
//            showDialog.value = false
//        }
//    }
    val all : List<ComboTask> = (selectedTaskList?.toList() ?: listOf()) + (habitTasks?.toList() ?: listOf())
    val allTasks = all
                        .sortedBy {  it.todayTask?.taskPageTag!="TOP3" }
                        .sortedBy { it.missionFrequency?.isDailyHabit == true }

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
                    TodoListItems(
                        selection,
                        allTasks,
                        editingTaskMode,
                        magicMode,
                        diaryViewModel
                    )
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    TodoMenuItems(selection, allTasks, diaryViewModel, magicMode, editingTaskMode)
                    item {
                        Spacer(modifier = Modifier.height(500.dp))
                    }
                }
        }
    }

}

private fun LazyListScope.TodoMenuItems(
    selection: LocalDate,
    allTasks: List<ComboTask>?,
    diaryViewModel: DiaryViewModel,
    magicMode: MutableState<String>,
    editingTaskMode: MutableState<String>
) {
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
        Row {
            Box(modifier = Modifier.clickable {
                Logger.e("addtask $selection")
                diaryViewModel.addRandomTask(selection)
            }) { Text("               +    ") }

            Box(modifier = Modifier.clickable {
                Logger.e("check/draw")
                if (magicMode.value == "CHECK")
                    magicMode.value = "DRAW" else
                    magicMode.value = "CHECK"
            }) { Text(if (magicMode.value == "CHECK") " CHECK " else " DRAW ") }

            Box(modifier = Modifier.clickable {
                Logger.e("Is Priority?")
                if (editingTaskMode.value == "ViewItems")
                    editingTaskMode.value = "prioritize"
                else
                    editingTaskMode.value = "ViewItems"
            }) { Text(if (editingTaskMode.value == "prioritize") "                  Done " else "    Prioritize ") }
        }
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
            ListHeader("TO DO", modifier = Modifier.animateItem())
        }
    }
    item{
        Box(modifier = Modifier.padding(top = 10.dp))
    }
    itemsIndexed(allTasks?: listOf(), key = { _, task ->
        ((task.mission?.missionId?:0L)*1000)+(task.todayTask?.todayTaskId?:0L)
    }) { i, task ->
        if(task.missionFrequency?.isDailyHabit == true){
            Box(modifier = Modifier.animateItem()) {
                HabitItem(selection, task, editingTaskMode, taskViewModel, magicMode,
                    KoinF.di?.get<HabitItemViewModel> { parametersOf(selection,task.mission?.missionId) }!!
                )
            }
        } else {
            Box(modifier = Modifier.animateItem()) {
                TodoItem(selection, task, editingTaskMode, taskViewModel, magicMode)
            }
        }
    }
    item{
        Column {
            Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Blue80))
            Box(modifier = Modifier.height(25.dp).fillMaxWidth())
            Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Blue80))
        }
    }
//    item {
//        if(!tasks.isNullOrEmpty()) {
//            ListHeader("TO DO", modifier = Modifier.animateItem())
//        }
//    }
//    itemsIndexed(tasks?: listOf(), key = { _, task ->
//        task.mission!!.missionId*1000+(task.todayTask?.todayTaskId?:0L)
//    }) { i, task ->
//        Box(modifier = Modifier.animateItem()) {
//            TodoItem(selection, task, editingTaskMode, taskViewModel, magicMode)
//        }
//    }
}

@Composable
private fun TodoItem(
    selection: LocalDate,
    task: ComboTask,
    editingTaskMode: MutableState<String>,
    taskViewModel: DiaryViewModel,
    magicMode: MutableState<String>
) {
    GeneralItem(task.mission!!)
    GeneralItemTopButton(task, editingTaskMode.value,
        taskIsTop = { tt, check ->
            if (check) {
                updateMoveTask(task.todayTask!!, "TOP3", taskViewModel)
            } else {
                updateMoveTask(task.todayTask!!, "TODO", taskViewModel)
            }
        }
    )
        if (editingTaskMode.value == "ViewItems" && magicMode.value == "CHECK") {
            GeneralItemCheckbox(task.todayTask,task.pillar, editingTaskMode.value,
                taskErased = { tt, check ->
                    if (check) {
                        updateTaskStatus(task.todayTask!!, "COMPLETED", taskViewModel)
                    } else {
                        updateTaskStatus(task.todayTask!!, "REFRESHED", taskViewModel)
                    }
                }
            )
        }
}
@Composable
private fun HabitItem(
    selection: LocalDate,
    hTask: ComboTask,
    editingTaskMode: MutableState<String>,
    taskViewModel: DiaryViewModel,
    magicMode: MutableState<String>,
    habitViewModel: HabitItemViewModel
) {
    GeneralItem(hTask.mission!!)
    GeneralItemTopButton(hTask, editingTaskMode.value,
        taskIsTop = { tt, check ->
            if (check) {
                updateMoveTask(hTask.todayTask!!, "TOP3", taskViewModel)
            } else {
                updateMoveTask(hTask.todayTask!!, "TODO", taskViewModel)
            }
        }
    )
    val taskProgressWeekList:List<TodayTask>? by habitViewModel.taskProgressForPastAround.collectAsState(emptyList())
    LaunchedEffect(selection){
        habitViewModel.loadTaskProgressForPastAround()
    }
    GeneralItemHabit(
        selection,
        hTask,
        taskProgressWeekList,
        editingTaskMode.value,
        taskIsHabit = { tt, check ->}
    )
    if (editingTaskMode.value == "ViewItems" && magicMode.value == "CHECK") {
        GeneralItemCheckbox(hTask.todayTask ,hTask.pillar, editingTaskMode.value,
            taskErased = { tt, check ->
                if((tt?.todayTaskId ?: 0L) > 0L) {
                    Logger.w("updating status: ${hTask.todayTask}")
                    updateTaskStatus(
                        hTask.todayTask!!,
                        if (check) "COMPLETED" else "REFRESHED",
                        taskViewModel)
                } else {
                    Logger.w("try insert status: ${hTask.todayTask}")
                    updateInsertTaskStatus(
                        hTask,
                        MyDate.fromLocalDate(selection),
                        if (check) "COMPLETED" else "REFRESHED",
                        taskViewModel)
                }
            }
        )
    }
}

fun updateInsertTaskStatus(task: ComboTask,taskDate: MyDate, taskStatus: String, taskViewModel: DiaryViewModel){
    val newTask = ComboTask(
        todayTask = TodayTask(
            todayTaskId = 0L, // Example task ID
            userId = 1L, // Example userId
            missionId = task.mission!!.missionId,
            taskStatus = taskStatus,
            taskDate = taskDate ,
            taskType = "USER_HABIT", // Example type
            taskPageTag = "TODO",
            taskProgressVal = 0f, // Example progress
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
        mission = null,
        pillar = null,
        missionFrequency = null
    )
    Logger.w("inserting status: ${newTask.todayTask}")
    taskViewModel.insertTask(newTask)
}



fun updateTaskStatus(task: TodayTask, taskStatus: String, taskViewModel: DiaryViewModel){
    taskViewModel.updateTaskStatus(task.todayTaskId, taskStatus)
}


fun updateMoveTask(task: TodayTask, moveTo:String, taskViewModel: DiaryViewModel){
    taskViewModel.updateTaskTag(task.todayTaskId,moveTo)
}
//
//
//fun updateTaskHighlightLines(taskWithDetails: ComboTask, eraseLines:List<HighlightTaskLine>, taskViewModel: TaskViewModel){
//    taskViewModel.updateFullTaskUi(
//        taskWithDetails.copy(
//            taskUiData = taskWithDetails.taskUiData.copy(
//                highlightLines = eraseLines
//            )
//        )
//    )
//}


