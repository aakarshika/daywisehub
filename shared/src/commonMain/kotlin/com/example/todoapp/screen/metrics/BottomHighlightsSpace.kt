package com.example.todoapp.screen.metrics

import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.todotask.TodayTaskWithDetails
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.db.models.MyDate
import kotlinx.datetime.LocalDate

@Composable
 fun BottomHighlightsSpace(
    selection: LocalDate,
    calMode: String,
    bottomHighlightsViewModel: BottomHighlightsViewModel,
    onExpandClicked: () -> Unit,
    diaryLoaded: () -> Unit
) {
    val selectedTaskList: List<TodayTaskWithFewDetails>? by bottomHighlightsViewModel.dayTasks.collectAsState(emptyList())

    LaunchedEffect(selection) {
        bottomHighlightsViewModel.loadTaskDetails()
    }
    LaunchedEffect(Unit){
        diaryLoaded()

    }
    val allTasks = (selectedTaskList?: listOf() )
//        .sortedBy { t-> t.missionWithDetails.pillar?.pillarName }
        .sortedBy { t-> t.todayTask.taskStatus }
    val allTasksCompleted = allTasks.filter { t->  t.todayTask.taskStatus== "COMPLETED" }
    val top3Tasks = allTasks.filter { t-> t.todayTask.taskPageTag == "TOP3" }
    val top3TasksCompleted = allTasks.filter { t-> t.todayTask.taskPageTag == "TOP3" && t.todayTask.taskStatus== "COMPLETED" }


    val highlights:MutableList<Map<String,List<String>>> = mutableListOf()

    highlights.add(mapOf(Pair(
        "Total_Tasks_Completed",listOf(
            "${allTasksCompleted.size} out of ${allTasks.size} tasks done.",
            "${allTasksCompleted.size} / ${allTasks.size} tasks done.",
            "${allTasksCompleted.size} / ${allTasks.size} for the day!",
        ))))

    if (allTasks.size > 0 && allTasks.size == allTasksCompleted.size) {
        highlights.add(mapOf(Pair(
            "All_Tasks_Completed",listOf(
                "Completed all tasks!",
                "Completed everything for the day.",
                "Wiped out the bucket.",
            ))))
    }
    if (top3Tasks.size == 3 && top3Tasks.size == top3TasksCompleted.size) {

        highlights.add(mapOf(Pair(
            "Top3_Tasks_Completed",listOf(
                "Top 3 tasks done!",
                "On top of priorities.",
                "Priority tasks completed.",
            ))))
    }

    Box(modifier = Modifier
        .fillMaxSize()) {
        Box(
            modifier = Modifier
        ) {
            Column(
                modifier = Modifier
                    .background(Light_Yellowww)
                    .fillMaxSize().padding(16.dp)
            ) {
                // Heading
                BasicText(
                    text = "${selection.dayOfMonth} ${selection.month}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bulleted List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(highlights) { highlight ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            val allTasksText: String =
                                (highlight["All_Tasks_Completed"] ?: listOf("")).random()
                            val top3TasksText: String =
                                (highlight["Top3_Tasks_Completed"] ?: listOf("")).random()
                            val totalTasksText: String =
                                (highlight["Total_Tasks_Completed"] ?: listOf("")).random()
                            if (highlight["All_Tasks_Completed"] != null) {
                                Icon(
                                    Icons.Default.ThumbUp,
                                    "alldone",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.padding(4.dp)
                                )
                                BasicText(
                                    text = allTasksText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                )
                            }
                            if (highlight["Top3_Tasks_Completed"] != null) {
                                Icon(
                                    Icons.Default.Star,
                                    "topdone",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.padding(4.dp)
                                )
                                BasicText(
                                    text = top3TasksText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                )
                            }
                            if (highlight["Total_Tasks_Completed"] != null) {
                                Icon(
                                    Icons.Default.Face,
                                    "totaldone",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.padding(4.dp)
                                )
                                BasicText(
                                    text = totalTasksText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                )
                            }
                            Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                                Icon(Icons.Default.MoreVert, "dsfdgsd",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.padding(4.dp).clickable {
                                        onExpandClicked()
                                    }
                                )

                            }

                        }
                    }
                }
                Spacer(modifier = Modifier.height(70.dp))

            }
        }
    }
}
//
//@RequiresApi(Build.VERSION_CODES.O)
//@Preview
//@Composable
//fun BottomHighlightsSpacePreview() {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//    ) {
//        BottomHighlightsSpace(
//            selection = MyDate.now(),
//            modifier = Modifier.fillMaxSize(),
//            {}
//        )
//    }
//}

val Light_Yellowww =  Color(0xFFFCF9ED)
