package com.example.todoapp.screen.diary.planning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.screen.basicutils.components.DiaryLineText
import com.example.todoapp.screen.basicutils.components.PlanningLineText
import com.example.todoapp.screen.basicutils.components.WriteText
import com.example.todoapp.screen.diary.ComboTask
import com.example.todoapp.screen.diary.diaryitem.components.NotebookLine
import com.example.todoapp.screen.diary.diaryitem.components.columnWeights
import com.example.todoapp.screen.missions.Blue80
import com.example.todoapp.screen.missions.DARKGREEN180
import com.example.todoapp.screen.missions.Red80
import com.example.todoapp.screen.missions.Yellow180
import com.example.todoapp.screen.missions.Yellow80
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.happy
import todoapp.shared.generated.resources.happyemoji
import todoapp.shared.generated.resources.sad
@Composable
fun ItemComponents(
    mission: CategorizedMission,
    onMissionClick: (CategorizedMission) -> Unit
) {
    var showReason = remember { mutableStateOf(false) }
    val missionColor = when (mission.category) {
        MissionCategory.RED -> Color.Red.copy(alpha = 0.15f)
        MissionCategory.YELLOW -> Yellow180.copy(alpha = 0.15f)
        MissionCategory.GREEN -> DARKGREEN180.copy(alpha = 0.15f)
    }

    val iconTint = when (mission.category) {
        MissionCategory.RED -> Color.Red
        MissionCategory.YELLOW -> Yellow180
        MissionCategory.GREEN -> DARKGREEN180
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp)
            .clickable { onMissionClick(mission) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status icon with category color
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(missionColor, CircleShape)
                        .clickable { showReason.value = !(showReason.value) }
                        .padding(8.dp)
                ) {
                    Icon(
                        painterResource(
                            when (mission.category) {
                                MissionCategory.RED -> Res.drawable.sad
                                MissionCategory.YELLOW -> Res.drawable.happy
                                MissionCategory.GREEN -> Res.drawable.happyemoji
                            }
                        ),
                        contentDescription = "Mission Status",
                        modifier = Modifier.size(24.dp),
                        tint = iconTint
                    )
                }

                // Mission title
                Text(
                    text = mission.mission.mission!!.missionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )

                // Add button
                IconButton(
                    onClick = { onMissionClick(mission) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if(mission.missionTaskToday?.active=="Y") Red80 else Blue80,
                            shape = if(mission.missionTaskToday?.active=="Y") RoundedCornerShape(3.dp) else CircleShape
                        )
                ) {
                    Icon(
                        if(mission.missionTaskToday?.active=="Y") Icons.Default.Delete else Icons.Default.AddCircle,
                        contentDescription = "Add to mission",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }

            // Reason text (animated visibility)
            AnimatedVisibility(
                visible = showReason.value,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            color = missionColor.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = mission.reasons.random(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}