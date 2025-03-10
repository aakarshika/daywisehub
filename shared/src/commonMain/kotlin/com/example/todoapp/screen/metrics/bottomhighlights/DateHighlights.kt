package com.example.todoapp.screen.metrics.bottomhighlights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.todotask.TodayTaskWithFewDetails
import com.example.todoapp.screen.missions.Light_Yellowww
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.crown_b
import todoapp.shared.generated.resources.diary_edit
import todoapp.shared.generated.resources.diary_editing_hand
import todoapp.shared.generated.resources.heart_filled
import todoapp.shared.generated.resources.star_border
import todoapp.shared.generated.resources.star_filled


@Composable
 fun DateHighlights(
    bottomHighlightsViewModel: BottomHighlightsViewModel,
    selection: LocalDate,
    goToDiary: () -> Unit
) {
    val selectedTaskList: List<TodayTaskWithFewDetails>? by bottomHighlightsViewModel.dayTasks.collectAsState(emptyList())

    LaunchedEffect(selection, Unit) {
        bottomHighlightsViewModel.loadTaskDetails()
    }
    val allTasks = (selectedTaskList?: listOf() )
        .sortedBy { t-> t.pillar?.pillarName }
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
    if (top3Tasks.size > 0 && top3Tasks.size == top3TasksCompleted.size) {

        highlights.add(mapOf(Pair(
            "Top3_Tasks_Completed",listOf(
                "Your top ${top3Tasks.size} tasks were done!",
                "You're on top of your priorities.",
                "Priority tasks completed.",
            ))))
    }

    DateHighlightsScreen(selection, goToDiary, highlights, allTasks, allTasksCompleted)
}

@Composable
private fun DateHighlightsScreen(
    selection: LocalDate,
    goToDiary: () -> Unit,
    highlights: MutableList<Map<String, List<String>>>,
    allTasks: List<TodayTaskWithFewDetails>,
    allTasksCompleted: List<TodayTaskWithFewDetails>
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Light_Yellowww)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selection.dayOfMonth} ${selection.month}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        painterResource(Res.drawable.diary_editing_hand),
                        contentDescription = "Go to diary",
                        tint = Color.Gray,
                        modifier = Modifier
                            .clickable { goToDiary() }
                            .size(36.dp)
                            .padding(2.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth().background(Color.Gray.copy(alpha = 0.3f)).height(1.dp)
            )

            // Highlights section
            if (highlights.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    items(highlights) { highlight ->
                        HighlightItem(
                            highlight = highlight,
                            allTasksSize = allTasks.size,
                            completedTasksSize = allTasksCompleted.size
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No highlights for this day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            // Add bottom padding to avoid navigation bar overlay
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

@Composable
private fun HighlightItem(
    highlight: Map<String, List<String>>,
    allTasksSize: Int,
    completedTasksSize: Int
) {
    highlight["All_Tasks_Completed"]?.let { allTasksTexts ->
        if (allTasksTexts.isNotEmpty()) {
            HighlightRow(
                iconRes = Res.drawable.heart_filled,
                text = allTasksTexts.random(),
                contentDescription = "All tasks completed"
            )
        }
    }

    highlight["Top3_Tasks_Completed"]?.let { top3TasksTexts ->
        if (top3TasksTexts.isNotEmpty()) {
            HighlightRow(
                iconRes = Res.drawable.crown_b,
                text = top3TasksTexts.random(),
                contentDescription = "Top tasks completed"
            )
        }
    }

    highlight["Total_Tasks_Completed"]?.let { totalTasksTexts ->
        if (totalTasksTexts.isNotEmpty()) {
            HighlightRow(
                iconRes = if (allTasksSize == completedTasksSize && allTasksSize > 0)
                    Res.drawable.star_filled
                else
                    Res.drawable.star_border,
                text = totalTasksTexts.random(),
                contentDescription = "Tasks completed"
            )
        }
    }
}

@Composable
private fun HighlightRow(
    iconRes: DrawableResource,
    text: String,
    contentDescription: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.DarkGray,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(24.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 16.sp,
                lineHeight = 20.sp
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 2
        )
    }
}