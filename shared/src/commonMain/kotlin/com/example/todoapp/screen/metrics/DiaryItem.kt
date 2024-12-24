package com.example.todoapp.screen.metrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskReminder
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.metrics.components.GeneralItem
import com.example.todoapp.screen.metrics.components.GeneralItemHabit
import com.example.todoapp.screen.metrics.components.GeneralItemTop3Star
import com.example.todoapp.screen.metrics.components.GeneralItemTopButton
import com.example.todoapp.screen.metrics.components.HabitItemViewModel
import com.example.todoapp.screen.metrics.components.ItemProgressBar
import com.kizitonwose.calendar.core.now
import kotlinx.datetime.LocalDate

@Composable
fun DiaryItem(
    selection: LocalDate,
    hTask: ComboTask,
    editingTaskMode: MutableState<String>,
    taskViewModel: DiaryViewModel,
    magicMode: MutableState<String>,
    habitViewModel: HabitItemViewModel
) {
    val rowHeight = remember { mutableStateOf(52) }
    val taskProgressWeekList:List<TodayTask>? by habitViewModel.taskProgressForPastAround.collectAsState(emptyList())
    LaunchedEffect(selection){
        habitViewModel.loadTaskProgressForPastAround()
    }
    GeneralItem(rowHeight.value,hTask.mission!!, textArranged = { lineCount ->
        rowHeight.value = 26*(lineCount+1)
    })
    GeneralItemTop3Star(rowHeight.value,hTask, editingTaskMode.value, taskIsTop = { tt, check ->})
    if(hTask.missionFrequency?.frequencyPeriod == "DAILY") {
        GeneralItemHabit(rowHeight.value,selection, hTask, taskProgressWeekList, editingTaskMode.value) { tt, check -> }
        if (hTask.missionFrequency.frequency > 10){
            //colorable area segments
        }else{
            //stampable area segments
        }
    }
    ItemProgressBar(rowHeight.value,selection,hTask.todayTask ,hTask.pillar!!, hTask.mission!!, hTask.missionFrequency!!,
        editingTaskMode.value){  progress->
        Logger.e("progress: ${progress} for $hTask")
        updateTaskProgress(selection, hTask, progress, taskViewModel)
//        if(progress==1f){
//            animateCompletedTask.value = hTask
//        } else if (progress==0f){
//            animateRefreshedTask.value = hTask
//        }
    }
    if(editingTaskMode.value == "prioritize") {
        GeneralItemTopButton(rowHeight.value,hTask, editingTaskMode.value){ tt, check ->
            if (tt != null) {
                updateTaskPriority(selection, hTask, if (check) "TOP3" else "TODO", taskViewModel)
            }
        }
    }
    if (editingTaskMode.value == "ViewItems" && magicMode.value == "CHECK") {
//        GeneralItemCheckbox(rowHeight.value,hTask.todayTask ,hTask.pillar, editingTaskMode.value,){ tt, check ->
//            if((tt?.todayTaskId ?: 0L) > 0L) {
//                Logger.w("updating status: ${hTask.todayTask}")
//                updateTaskStatus(hTask.todayTask!!,
//                    if (check) "COMPLETED" else "REFRESHED", taskViewModel)
//            } else {
//                Logger.w("try insert status: ${hTask.todayTask}")
//                updateInsertTaskStatus(hTask, MyDate.fromLocalDate(selection),
//                    if (check) "COMPLETED" else "REFRESHED", taskViewModel)
//            }
//        }
    }
}

fun updateTaskStatus(
    selection: LocalDate,
    t: ComboTask,
    status: String,
    taskViewModel: DiaryViewModel) {
    val updatedTask = (t.todayTask?:getNewTask(t.mission!!.missionId, selection)).copy(
        taskStatus = status
    )
    upsertTask(updatedTask, taskViewModel)
}

fun updateTaskPriority(
    selection: LocalDate,
    t: ComboTask,
    tag: String,
    taskViewModel: DiaryViewModel) {
    val updatedTask = (t.todayTask?:getNewTask(t.mission!!.missionId, selection)).copy(
        taskPageTag = tag
    )
    upsertTask(updatedTask, taskViewModel)
}
fun updateTaskProgress(
    selection: LocalDate,
    t: ComboTask,
    progress: Float,
    taskViewModel: DiaryViewModel) {
    var status = t.todayTask?.taskStatus?:"ADDED"
    if(progress==1f){
        status = "COMPLETED"
    } else if (progress==0f){
        status = "REFRESHED"
    }
    val updatedTask = (t.todayTask?:getNewTask(t.mission!!.missionId, selection)).copy(
        taskProgressVal = progress,
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
//    val newTask = ComboTask(
//        todayTask = TodayTask(
//            todayTaskId = 0L, // Example task ID
//            userId = 1L, // Example userId
//            missionId = missionId,
//            taskStatus = "ADDED",
//            taskDate = MyDate.fromLocalDate(taskDate) ,
//            taskType = "USER_HABIT", // Example type
//            taskPageTag = "TODO",
//            taskProgressVal = 0f, // Example progress
//            taskText = "", // Example text
//            taskPictureUrl = null, // Example URL (can be null)
//            taskLink = null // Example link (can be null)
//        ),
//        todayTaskReminder = TodayTaskReminder(
//            todayTaskId = 0L, // Example task ID
//            timeOfDay = "08:00", // Example time
//            alarmTone = "Default Tone", // Example alarm tone
//            timeBefore = 10, // Example time before
//            timeBeforeUnit = 1 // Example time before unit (e.g., minutes)
//        ),
//        mission = null,
//        pillar = null,
//        missionFrequency = null
//    )
//    Logger.w("inserting status: ${newTask.todayTask}")
//    return newTask
}
