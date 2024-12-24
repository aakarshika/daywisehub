package com.example.todoapp.screen.diary


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.room.Embedded
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskReminder
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.diary.diarymood.MoodHeader
import com.example.todoapp.screen.diary.diaryitem.components.WeatherHeader
import com.example.todoapp.screen.diary.diaryitem.HabitItemViewModel
import com.example.todoapp.screen.diary.diaryitem.components.MenuItem
import com.example.todoapp.screen.diary.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.NotebookLine
import com.example.todoapp.screen.diary.diaryitem.DiaryItem
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


fun LazyListScope.ExtraLines(n: Int ) {
    item {
        Column {
            (1..n).forEach {
                NotebookLine()
            }
        }
    }
}

fun LazyListScope.TodoMenuItems(
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

fun LazyListScope.MoodHeaderItem(
    selection: LocalDate
) {
    item {
        MoodHeader(
            selection,
            KoinF.di?.get<MoodViewModel> { parametersOf(selection) }!!
        )
    }
}

fun LazyListScope.WeatherHeaderItem() {
    item {
        WeatherHeader()
    }
}




fun LazyListScope.TodoListItems(
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
