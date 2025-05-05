package com.example.todoapp.screen.diary.diaryitem

import KottieAnimation
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskReminder
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.diary.ComboTask
import com.example.todoapp.screen.diary.DiaryViewModel
import com.example.todoapp.screen.diary.diaryitem.components.GeneralItem
import com.example.todoapp.screen.diary.diaryitem.components.GeneralItemCheckbox
import com.example.todoapp.screen.diary.diaryitem.components.GeneralItemHabit
import com.example.todoapp.screen.diary.diaryitem.components.GeneralItemTop3Star
import com.example.todoapp.screen.diary.diaryitem.components.GeneralItemTopButton
import com.example.todoapp.screen.diary.diaryitem.components.ItemProgressBar
import com.example.todoapp.screen.missions.Blue80
import kotlinx.datetime.LocalDate
import kottieComposition.KottieCompositionSpec
import kottieComposition.animateKottieCompositionAsState
import kottieComposition.rememberKottieComposition
import org.jetbrains.compose.resources.ExperimentalResourceApi
import todoapp.shared.generated.resources.Res

@Composable
fun DiaryItem(
    selection: LocalDate,
    hTask: ComboTask,
    editingTaskMode: MutableState<String>,
    taskViewModel: DiaryViewModel,
    habitViewModel: HabitItemViewModel,
    taskProgress: (Float) -> Unit
) {
    val rowHeight = remember { mutableStateOf(52) }
    val taskProgressWeekList:List<TodayTask>? by habitViewModel.taskProgressForPastAround.collectAsState(emptyList())
    LaunchedEffect(selection){
        habitViewModel.loadTaskProgressForPastAround()
    }
    Row{
        Box(modifier = Modifier.padding(start = 8.dp).width(1.dp).height(rowHeight.value.dp).background(
            Blue80))
        Box(modifier = Modifier.weight(1f)){
            Item(
                rowHeight,
                hTask,
                editingTaskMode,
                selection,
                taskProgressWeekList,
                taskViewModel,
                taskProgress
            )
        }
        Box(modifier = Modifier.padding(end = 8.dp).width(1.dp).height(rowHeight.value.dp).background(
            Blue80))

    }
}

@Composable
private fun Item(
    rowHeight: MutableState<Int>,
    hTask: ComboTask,
    editingTaskMode: MutableState<String>,
    selection: LocalDate,
    taskProgressWeekList: List<TodayTask>?,
    taskViewModel: DiaryViewModel,
    taskProgress: (Float) -> Unit
) {
    GeneralItem(rowHeight.value, hTask.mission!!, textArranged = { lineCount ->
        rowHeight.value = 26 * (lineCount + 1)
    })
    GeneralItemTop3Star(
        rowHeight.value,
        hTask,
        editingTaskMode.value,
        taskIsTop = { tt, check -> })
    if (hTask.missionFrequency?.frequencyPeriod == "DAILY") {
        GeneralItemHabit(
            rowHeight.value,
            selection,
            hTask,
            taskProgressWeekList,
            editingTaskMode.value
        ) { tt, check -> }
    }
    if (editingTaskMode.value == "ViewItems" && hTask.todayTask?.taskStatus != "COMPLETED") {
        ItemProgressBar(rowHeight.value, selection, hTask, editingTaskMode.value) { progress ->
            updateTaskProgress(selection, hTask, progress, taskViewModel)
            Logger.e("task Progress $progress")
            taskProgress(progress)
        }
    }
    if (editingTaskMode.value == "prioritize") {
        GeneralItemTopButton(rowHeight.value, hTask, editingTaskMode.value) { tt, check ->
            if (tt != null) {
                updateTaskPriority(
                    selection,
                    hTask,
                    if (check) "TOP3" else "TODO",
                    taskViewModel
                )
            }
        }
    }
    if (editingTaskMode.value == "ViewItems" && hTask.todayTask?.taskStatus == "COMPLETED") {
        GeneralItemCheckbox(rowHeight.value, hTask, editingTaskMode.value) { tt, check ->
            updateTaskStatus(
                selection, hTask,
                if (check) "COMPLETED" else "REFRESHED", taskViewModel
            )
        }
    }
}



fun updateTaskStatus(
    selection: LocalDate,
    t: ComboTask,
    status: String,
    taskViewModel: DiaryViewModel
) {
    val updatedTask = (t.todayTask?: getNewTask(t.mission!!.missionId, selection)).copy(
        taskStatus = status
    )
    upsertTask(updatedTask, taskViewModel)
}

fun updateTaskPriority(
    selection: LocalDate,
    t: ComboTask,
    tag: String,
    taskViewModel: DiaryViewModel
) {
    val updatedTask = (t.todayTask?: getNewTask(t.mission!!.missionId, selection)).copy(
        taskPageTag = tag
    )
    upsertTask(updatedTask, taskViewModel)
}
fun updateTaskProgress(
    selection: LocalDate,
    t: ComboTask,
    progress: Float,
    taskViewModel: DiaryViewModel
) {
    var pp = progress
    var status = t.todayTask?.taskStatus?:"ADDED"
    if(progress==1f){
        status = "COMPLETED"
    } else {
        status = "REFRESHED"
    }
    val updatedTask = (t.todayTask?: getNewTask(t.mission!!.missionId, selection)).copy(
        taskProgressVal = pp,
        taskStatus = status
    )
    Logger.w("updating/inserting task : ${updatedTask}")
    upsertTask(updatedTask, taskViewModel)
}
fun upsertTask(
    updatedTask: TodayTask?,
    taskViewModel: DiaryViewModel
){
    if(updatedTask != null && updatedTask.todayTaskId > 0L){
        taskViewModel.updateTodayTask(updatedTask)
    } else {
        taskViewModel.insertTask(
            ComboTask(updatedTask,
            todayTaskReminder = getNewReminder(0L),
            mission = null,
            pillar = null,
            missionFrequency = null
            )
        )
    }
}

fun getNewTask(missionId: Long, taskDate: LocalDate): TodayTask{
    return TodayTask(
        todayTaskId = 0L, // Example task ID
        userId = 1L, // Example userId
        missionId = missionId,
        taskStatus = "ADDED",
        taskDate = MyDate.fromLocalDate(taskDate) ,
        taskType = "USER_HABIT", // Example type
        taskPageTag = "TODO",
        taskProgressVal = 0f, // Example progress
        taskText = "", // Example text
        taskPictureUrl = null, // Example URL (can be null)
        taskLink = null // Example link (can be null)
    )
}
//getnew reminder
fun getNewReminder(todayTaskId: Long): TodayTaskReminder{
    return TodayTaskReminder(
        todayTaskId = todayTaskId, // Example task ID
        timeOfDay = "08:00", // Example time
        alarmTone = "Default Tone", // Example alarm tone
        timeBefore = 10, // Example time before
        timeBeforeUnit = 1 // Example time before unit (e.g., minutes)
    )
}