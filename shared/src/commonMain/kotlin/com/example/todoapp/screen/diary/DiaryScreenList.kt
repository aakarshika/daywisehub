package com.example.todoapp.screen.diary


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.room.Embedded
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskReminder
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.db.models.TaskType
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodHeader
import com.example.todoapp.screen.diary.diaryitem.HabitItemViewModel
import com.example.todoapp.screen.diary.diaryitem.DiaryItem
import com.example.todoapp.screen.diary.diaryitem.components.NotesItem
import com.example.todoapp.screen.diary.diaryitem.components.dateheader.DateHeader
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeHeader
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.water_daily_limit
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.Light_Yellowww
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.koin.core.parameter.parametersOf
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.block_b
import todoapp.shared.generated.resources.glass_a
import todoapp.shared.generated.resources.glass_b
import todoapp.shared.generated.resources.glass_c
import todoapp.shared.generated.resources.pencil

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
                    (if (todayTask?.taskStatus==TaskStatus.COMPLETED.value) "/CMPLD" else "/${todayTask?.taskProgressVal}")+
                    (if (todayTask?.taskPageTag==TaskType.TOP3.value) "/T3" else "")+
                    ")"
        else "todo(" +
                "'m${mission?.missionId?:0L},t${todayTask?.todayTaskId?:0L}'${mission?.missionTitle}-${pillar?.pillarName}." +
                (todayTask?.taskDate?.dateString?:"")+
                (if (todayTask?.taskStatus==TaskStatus.COMPLETED.value) "/CMPLD" else "/${todayTask?.taskProgressVal}")+
                (if (todayTask?.taskPageTag==TaskType.TOP3.value) "/T3" else "")+
                ")"
    }
}


fun LazyListScope.MoodHeaderItem(todayMoodList: List<TodayMoodWithDetails>?,
                                 selection: LocalDate,
                                 upsertMood : (TodayMood) -> Unit) {
    item {
        MoodHeader(
            todayMoodList,
            selection,
            upsertMood
        )
    }
}

fun LazyListScope.DateHeaderItem(cDate: LocalDate,
                                 changeDate: (LocalDate) -> Unit) {
    item {
        DateHeader(cDate, changeDate = { changeDate ( it ) })
    }
}
fun LazyListScope.WaterIntakeHeaderItem(waterIntake: WaterIntake?,
                                        selection: LocalDate,
                                        upsertWaterIntake : (WaterIntake) -> Unit,
                                        waterIntakeCompleted : (Boolean) -> Unit) {
    item {
        WaterIntakeHeader(waterIntake?.waterIntakeProgress?:0.0, selection, upsertWaterIntake = {progress->
            upsertWaterIntake((waterIntake ?: WaterIntake(
                waterIntakeId = 0,
                waterIntakeProgress = progress,
                waterDate = MyDate.fromLocalDate(selection)
            )).copy(waterIntakeProgress = progress))
            waterIntakeCompleted(progress.toInt() == (water_daily_limit-1))
        })
    }
}
fun LazyListScope.Notes(waterIntake: WaterIntake?,
                                        selection: LocalDate,
                                        upsertWaterIntake : (WaterIntake) -> Unit) {
    item {
        val rowHeight = remember { mutableStateOf(52) }

        NotesItem(rowHeight.value,waterIntake, selection,textArranged = { lineCount ->
            rowHeight.value = 26*(lineCount+1)
        }, upsertWaterIntake = {n:String->
            upsertWaterIntake((waterIntake ?: WaterIntake(
                waterIntakeId = 0,
                notes = n,
                waterDate = MyDate.fromLocalDate(selection)
            )).copy(notes = n))
        })
    }
}

fun LazyListScope.ToDoListHeader(headingtext: String, color: Color, editClicked: () -> Unit) {
    item {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            HeadingBanner(color, headingtext)
            Box(modifier = Modifier.fillMaxWidth().padding(end = 5.dp).wrapContentHeight()) {

                Box(
                    modifier = Modifier.clickable { editClicked() }
                        .wrapContentSize()
                        .shadow(5.dp, CircleShape)
                        .clip(CircleShape)
                        .background(color)
                        .padding( 8.dp)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(Res.drawable.pencil),
                        contentDescription = "edit_button",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp).padding(start = 2.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}
fun LazyListScope.HeadingText(headingtext: String, color: Color) {
    item {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            HeadingBanner(color, headingtext)
        }
    }
}
@Composable
private fun HeadingBanner(color: Color, headingtext: String) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.height(30.dp).fillMaxWidth()
                .padding(horizontal = 60.dp)
        ) {
            Image(
                painterResource(Res.drawable.block_b),
                contentDescription = "Section heading background",
                colorFilter = ColorFilter.tint(color),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
            )
            Text(
                text = headingtext,
                modifier = Modifier
                    .align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

fun LazyListScope.TodoListItems(
    selection: LocalDate,
    allTasks: List<ComboTask>?,
    editingTaskMode: MutableState<String>,
    taskViewModel: DiaryViewModel,
    allTasksCompleted: (Boolean) -> Unit
) {
    item {
    }
    item {
        Box(modifier = Modifier.fillMaxWidth().height(20.dp).padding(horizontal = 8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Blue80, RoundedCornerShape(topEnd = 5.dp, topStart = 5.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 5.dp, start = 1.dp, end = 1.dp)
                    .background(Light_Yellowww)
            )
        }
    }
    itemsIndexed(allTasks?: listOf(), key = { _, task ->
        ((task.mission?.missionId?:0L)*1000)+(task.todayTask?.todayTaskId?:0L)
    }) { i, task ->
        Box(modifier = Modifier
            .animateItem()) {
            DiaryItem(selection, task, editingTaskMode, taskViewModel,
                KoinF.di?.get<HabitItemViewModel> {
                    parametersOf(
                        selection,
                        task.mission?.missionId
                    )
                }!!,
                taskProgress = { p ->
                    allTasksCompleted(
                        if (p == 1f &&
                            allTasks?.filter { it.todayTask!!.todayTaskId != task.todayTask!!.todayTaskId }
                                ?.all { it.todayTask!!.taskStatus == TaskStatus.COMPLETED.value } == true
                        )
                            true
                        else
                            false)

                }
            )
        }
    }
    item {
        Box(modifier = Modifier.fillMaxWidth().height(10.dp).padding(horizontal = 8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Blue80, RoundedCornerShape(bottomEnd = 5.dp, bottomStart = 5.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 5.dp, start = 1.dp, end = 1.dp)
                    .background(Light_Yellowww)
            )
        }
    }
}
