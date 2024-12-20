package com.example.todoapp.screen.metrics


import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.screen.metrics.components.GeneralItem
import com.example.todoapp.screen.metrics.components.GeneralItemCheckbox
import com.example.todoapp.screen.metrics.components.ListHeader
import com.example.todoapp.screen.metrics.components.MoodHeader
import com.example.todoapp.screen.metrics.components.TodoItemCustom
import com.example.todoapp.screen.metrics.components.WeatherHeader


@Composable
fun DiaryScreen(
    selectedTaskList: List<TodayTaskWithFewDetails>?,
    taskViewModel: CalDiaryViewModel
    ) {

    val allTasks = selectedTaskList

    val tasks= selectedTaskList?.filter {  it.todayTask.taskPageTag=="TODO" }
    val top3Tasks= selectedTaskList?.filter {  it.todayTask.taskPageTag=="TOP3" }
    val habitTasks= selectedTaskList?.filter {  it.todayTask.taskPageTag=="HABIT" }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Light_Yellowww)
    ) {
        val draggedTask = remember { mutableStateOf<TodayTaskWithFewDetails?>(null) }
        val dragOffset = remember { mutableStateOf(Offset.Zero) }

        val top3HeaderPosition = remember { mutableStateOf(0f) }
        val todoListHeaderPosition = remember { mutableStateOf(0f) }

        val editingTaskMode = remember {
            mutableStateOf( "ViewItems")
        }

        val magicMode = remember {
            mutableStateOf("CHECK")
        }
        Column {
//                    HeadingDiaryPage(taskViewModel, onDateClicked = { dateClicked ->
//                        taskViewModel.selectDate(dateClicked)
//                        editingTaskMode.value = if(tasks.isNotEmpty() || top3Tasks.isNotEmpty())
//                            "ViewItems"
//                        else "PrepareButton"
//                    })

                LazyColumn(modifier = Modifier) {
                    MoodHeaderItem()
                    WeatherHeaderItem()


                    TodoListItems(
                        tasks,
                        top3Tasks,
                        habitTasks,
                        dragOffset,
                        editingTaskMode,
                        magicMode,
                        taskViewModel,
                        draggedTask,
                        todoListHeaderPosition,
                        top3HeaderPosition
                    )
//
//                    PrepareListItems(
//                        allTasks,
//                        dragOffset,
//                        editingTaskMode,
//                        draggedTask,
//                        todoListHeaderPosition,
//                        top3HeaderPosition
//                    )

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
                        TodoItemCustom(editingMode = editingTaskMode.value, magicMode.value) { tag->
                            if(tag=="addRandomTask") {
                                val success = taskViewModel.addRandomTask()
                                if (!success) {
                                }
                            } else if(tag=="rearrangeTaskList") {
                                editingTaskMode.value = "Rearrange-ViewItems"
                            } else if(tag=="doneTaskList") {
                                editingTaskMode.value = "ViewItems"
                            } else if(tag=="prepareDoneTaskList") {
                                if (tasks != null) {
                                    if (top3Tasks != null) {
                                        editingTaskMode.value =
                                            if(tasks.isNotEmpty() || top3Tasks.isNotEmpty())
                                                "ViewItems"
                                            else "PrepareButton"
                                    }
                                }
                            } else if(tag=="autoPrepareButtonClick") {
                                if (allTasks != null && allTasks.isEmpty()){
                                    isLoading.value = true
//                                    taskViewModel.prepareTodaysTasks()

                                }
                                editingTaskMode.value = "PrepareItems"
                            } else if(tag=="magicModeCHECK") {
                                magicMode.value = "CHECK"
                            } else if(tag=="magicModeDRAW") {
                                magicMode.value = "DRAW"
                            } else if(tag=="plannerMode") {
                                editingTaskMode.value = "PrepareItems"
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(500.dp))
                    }
                }
        }

        draggedTask.value?.let {
            val animatedOffset = animateOffsetAsState(targetValue =
            Offset(dragOffset.value.x + 50, dragOffset.value.y - 100))

            it.mission?.let { it1 ->
                Text(
                    text = it1.missionTitle,
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = animatedOffset.value.x
                            translationY = animatedOffset.value.y
                        }
                        .background(Color.Gray)
                )
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

//private fun LazyListScope.PrepareListItems(
//    allTasks: List<TodayTaskWithFewDetails>?,
//    dragOffset: MutableState<Offset>,
//    editingTaskMode: MutableState<String>,
//    draggedTask: MutableState<TodayTaskWithFewDetails?>,
//    todoListHeaderPosition: MutableState<Float>,
//    top3HeaderPosition: MutableState<Float>
//) {
//    // Todo List Header
//
//    item {
//        if(editingTaskMode.value == "PrepareItems") {
//            ListHeader(
//                "DAY PLANNER",
//                modifier = Modifier
//                    .padding(top = 8.dp)
//                    .background(if (draggedTask.value != null) Light_Yellowww else Color.Transparent)
//
//            )
//            PlannerHeadingItems{tag ->}
//        }
//    }
//
//    itemsIndexed(allTasks.itemList) { i, task ->
//        if (editingTaskMode.value  == "PrepareItems") {
//            PrepareTaskItemBox(
//                task,
//                editingTaskMode,
//                taskViewModel,
//                dragOffset,
//                draggedTask,
//                todoListHeaderPosition,
//                top3HeaderPosition
//            )
//        }
//    }
//}


private fun LazyListScope.TodoListItems(
    tasks: List<TodayTaskWithFewDetails>?,
    top3tasks: List<TodayTaskWithFewDetails>?,
    habitTasks: List<TodayTaskWithFewDetails>?,
    dragOffset: MutableState<Offset>,
    editingTaskMode: MutableState<String>,
    magicMode: MutableState<String>,
    taskViewModel: CalDiaryViewModel,
    draggedTask: MutableState<TodayTaskWithFewDetails?>,
    todoListHeaderPosition: MutableState<Float>,
    top3HeaderPosition: MutableState<Float>
) {
    item {
        if(editingTaskMode.value == "ViewItems"|| editingTaskMode.value == "Rearrange-ViewItems") {
            ListHeader(
                "PRIORITIES",
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(if (draggedTask.value != null) Light_Yellowww else Color.Transparent)
                    .onGloballyPositioned { coordinates ->
                        top3HeaderPosition.value = coordinates.positionInWindow().y
                    }
            )
        }
    }
    itemsIndexed(top3tasks?: listOf()) { i, task ->
        if (editingTaskMode.value  == "ViewItems"|| editingTaskMode.value == "Rearrange-ViewItems") {
            TaskItemBox(
                task,
                editingTaskMode,
                magicMode,
                taskViewModel,
                dragOffset,
                draggedTask,
                todoListHeaderPosition,
                top3HeaderPosition
            )
        }
    }
    // Todo List Header
    item {
        if(editingTaskMode.value == "ViewItems"|| editingTaskMode.value == "Rearrange-ViewItems") {
            ListHeader(
                "TO DO",
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(if (draggedTask.value != null) Light_Yellowww else Color.Transparent)
                    .onGloballyPositioned { coordinates ->
                        todoListHeaderPosition.value = coordinates.positionInWindow().y
                    }
            )
        }
    }
    itemsIndexed(tasks?: listOf()) { i, task ->
        if (editingTaskMode.value == "ViewItems" || editingTaskMode.value == "Rearrange-ViewItems") {
            TaskItemBox(
                task,
                editingTaskMode,
                magicMode,
                taskViewModel,
                dragOffset,
                draggedTask,
                todoListHeaderPosition,
                top3HeaderPosition
            )
        }
    }
    item {
        if(editingTaskMode.value == "ViewItems"|| editingTaskMode.value == "Rearrange-ViewItems") {
            ListHeader(
                "HABITS",
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(if (draggedTask.value != null) Light_Yellowww else Color.Transparent)
                    .onGloballyPositioned { coordinates ->
                        todoListHeaderPosition.value = coordinates.positionInWindow().y
                    }
            )
        }
    }
    itemsIndexed(habitTasks?: listOf()) { i, task ->
        if (editingTaskMode.value  == "ViewItems"|| editingTaskMode.value == "Rearrange-ViewItems") {
            TaskItemBox(
                task,
                editingTaskMode,
                magicMode,
                taskViewModel,
                dragOffset,
                draggedTask,
                todoListHeaderPosition,
                top3HeaderPosition
            )
        }
    }
}

@Composable
private fun TaskItemBox(
    task: TodayTaskWithFewDetails,
    editingTaskMode: MutableState<String>,
    magicMode: MutableState<String>,
    taskViewModel: CalDiaryViewModel,
    dragOffset: MutableState<Offset>,
    draggedTask: MutableState<TodayTaskWithFewDetails?>,
    todoListHeaderPosition: MutableState<Float>,
    top3HeaderPosition: MutableState<Float>
) {
    Box {
        GeneralItem(task)
//        if (editingTaskMode.value == "ViewItems" && magicMode.value == "DRAW") {
//            GeneralItemCanvasHighlight(task, editingTaskMode.value,
//                taskErased = { tt, eraseLines, check ->
//                    if (check) {
//                        updateTaskStatus(task, TaskStatus.COMPLETED, taskViewModel)
//                    } else {
//                        updateTaskStatus(task, TaskStatus.REFRESHED, taskViewModel)
//                    }
//                }
//            )
//        } else
        if (editingTaskMode.value == "ViewItems" && magicMode.value == "CHECK") {
            GeneralItemCheckbox(task, editingTaskMode.value,
                taskErased = { tt,  check ->
                    if (check) {
                        updateTaskStatus(task, "COMPLETED", taskViewModel)
                    } else {
                        updateTaskStatus(task, "REFRESHED", taskViewModel)
                    }
                }
            )
//        } else if (editingTaskMode.value == "Rearrange-ViewItems") {
//            GeneralItemRearrangeView(task, dragOffset, editingTaskMode.value,
//                onDragStarted = { tt -> draggedTask.value = tt },
//                onDragCancel = { draggedTask.value = null },
//                onDragEnd = {
//                    val dropY = dragOffset.value.y
//                    if (dropY > todoListHeaderPosition.value) {
//                        if (draggedTask.value != null) {
//                            updateMoveTask(draggedTask.value!!, "TODO", taskViewModel)
//                        }
//                    } else if (dropY > top3HeaderPosition.value) {
//                        if (draggedTask.value != null) {
//                            updateMoveTask(draggedTask.value!!, "TOP3", taskViewModel)
//                        }
//                    }
//                    draggedTask.value = null
//                }
//            )
        }
    }
}



//@Composable
//private fun PrepareTaskItemBox(
//    task: TodayTaskWithDetails,
//    editingTaskMode: MutableState<String>,
//    taskViewModel: TaskViewModel,
//    dragOffset: MutableState<Offset>,
//    draggedTask: MutableState<TodayTaskWithDetails?>,
//    todoListHeaderPosition: MutableState<Float>,
//    top3HeaderPosition: MutableState<Float>
//) {
//    Box {
//        PrepareItem(task){tt, tag->
//            Log.e("prepare item", "$tt, $tag")
//            Log.e("prepare item", " $tag")
//            updateMoveTask(tt, tag, taskViewModel)
//        }
//    }
//}


fun updateTaskStatus(taskWithDetails: TodayTaskWithFewDetails, taskStatus: String, taskViewModel: CalDiaryViewModel){
    taskViewModel.updateTaskStatus(taskWithDetails.todayTask.todayTaskId, taskStatus)
}


fun updateMoveTask(taskWithDetails: TodayTaskWithFewDetails, moveTo:String, taskViewModel: CalDiaryViewModel){
    taskViewModel.updateTaskTag(taskWithDetails.todayTask.todayTaskId,moveTo)
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


