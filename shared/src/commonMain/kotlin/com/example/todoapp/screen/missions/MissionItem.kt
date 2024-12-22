package com.example.todoapp.screen.missions

import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.MissionWithDetails
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.MyDate.Companion.daysBetween
import com.example.todoapp.db.models.MyDate.Companion.now
import com.example.todoapp.screen.basicblocks.WriteText
import com.example.todoapp.screen.missions.calendar.progress.MissionProgressCalendar


val taskWidth = 320.dp
val taskMinHeight = 88.dp
@Composable
fun MissionItem(missionId: Long,
                    missionItemViewModel: MissionItemViewModel,
                    isSelected:Boolean,
                    isTrigger:Boolean,
                    modifier: Modifier = Modifier,
                    onEditClick: (MissionWithDetails) -> Unit,
                    onViewClick: (Long) -> Unit,
                    onCloseClick: () -> Unit
) {
    val height by animateDpAsState(targetValue = if (isSelected) 200.dp else 10.dp)
    val width by animateDpAsState(targetValue = if (isSelected) 100.dp else 1.dp)
    val elevation by animateDpAsState(targetValue = if (isSelected) 10.dp else 6.dp)

    val mission by missionItemViewModel.mission.collectAsState(null)
    val pillar by missionItemViewModel.pillar.collectAsState(null)
    val missionFrequency by missionItemViewModel.missionFrequency.collectAsState(null)
    val milestones by missionItemViewModel.milestones.collectAsState(null)

    val missionWithDetails = mission?.let {
        MissionWithDetails(
            mission = it,
            pillar = pillar,
            missionFrequency = missionFrequency,
            milestones= milestones
        )
    }

    LaunchedEffect(isTrigger) {
        missionItemViewModel.loadMissionDetails()
    }


        Box(
            modifier = modifier
                .width(taskWidth + width)
                .height(taskMinHeight + height)
        ) {

            Column(
                modifier = Modifier
                    .width(taskWidth + width)
                    .height(taskMinHeight + height)
                    .shadow(10.dp, spotColor = Color.Black)
                    .background(color = Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = getPillarColor(pillar?.pillarName).copy(alpha = 0.5f))
                ) {
                    if (isSelected && missionWithDetails != null) {
                        missionWithDetails?.let { ExpandedListItem(it, onEditClick, onCloseClick ) }
                    } else {
                        missionWithDetails?.let { SmallListItem(it, onEditClick, onViewClick) }
                    }
                }

        }
    }
}
fun getPillarColor(pillarName: String?): Color {
    return if (pillarName == "HEALTH") Color(0xFFF6D3FF)
    else if (pillarName == "WEALTH") Color(0xFFFFD4B8)
    else if (pillarName == "LOVE") Color(0xFFFFCBD2)
    else if (pillarName == "LIFE") Color(0xFFD4EBFF)
    else  Color(0xFFCCC2DC)
}


fun getThemeColor(theme: String?): Color {
    return if (theme == "HABIT") Color(0xFFF6D3FF)
    else if (theme == "TOP3") Color(0xFFFFD4B8)
    else  Color(0xFFCCC2DC)
}

@Composable
private fun SmallListItem(
    mission: MissionWithDetails,
    onEditClick: (MissionWithDetails) -> Unit,
    onViewClick: (Long) -> Unit
) {
    Box(modifier = Modifier
        .clickable {
            mission?.let { onViewClick(it.mission?.missionId?:0L) }
        }
        .padding(10.dp)
    ){
        Box(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 25.dp, start = 1.dp, end = 1.dp)
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
            ) {
                Row(
                    modifier = Modifier
                ) {
                    WriteText(
                        mission?.mission?.missionTitle?:"MISSION",
                        fontStyle = FontStyle.Italic
                    )
                }
                Row(
                    modifier = Modifier
                ) {
                    MilestoneList(mission)
                }
                Row(
                    modifier = Modifier
                ) {
                }
            }
        }
        Box(modifier = Modifier
            .size(30.dp)
            .align(Alignment.TopEnd)
            .clickable {
                mission?.let { onEditClick(it) }
            }) {
            Icon(
                Icons.Default.Create,
                "dxfgh",
                modifier = Modifier.fillMaxSize(),
                tint = Color.Black
            )
        }
    }
}

@Composable
private fun ExpandedListItem(
    mission: MissionWithDetails,
    onEditClick: (MissionWithDetails) -> Unit,
    onCloseClick: () -> Unit
) {
    Box(modifier = Modifier
        .fillMaxSize()
        .clickable {
            onCloseClick()
        }
        .padding(10.dp)
    ){
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, bottom = 25.dp, start = 1.dp, end = 1.dp)
        ) {
            Column(modifier = Modifier
                .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentSize()
                ) {
                    WriteText(
                        mission.mission?.missionTitle?:"MISSION",
                        fontStyle = FontStyle.Italic
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .width((7 * 22).dp)
                            .height(150.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.End)
                        ) {
                            MissionProgressCalendar(mission)
                        }
                    }
                }

            }
        }
        Box(modifier = Modifier
            .size(30.dp)
            .align(Alignment.TopEnd)
            .clickable {
                mission.let { onEditClick(it) }
            })
        {
            Icon(
                Icons.Default.Create,
                "dxfgh",
                modifier = Modifier.fillMaxSize(),
                tint = Color.Black
            )
        }
    }
}

@Composable
fun MilestoneList(mission: MissionWithDetails) {

    val nextMilestones = mission.milestones?.filter {milestone->
        val daysToMilestone = daysBetween(now(), milestone.milestone.expectedCompletionDate)

        milestone.milestone.status != "COMPLETED"
                && daysToMilestone>=0
                && daysToMilestone<=45
    }

    val completedMilestones = mission.milestones?.filter {milestone->
        val daysToMilestone = daysBetween(now(), milestone.milestone.expectedCompletionDate)

        milestone.milestone.status == "COMPLETED"
                || daysToMilestone<=0

    }
    Column(
        modifier = Modifier
            .width(taskWidth)) {

        // Display milestone information
        nextMilestones?.take(1)?.forEach { milestone ->
            // Calculate the due date
            val daysToMilestone = daysBetween(now(), milestone.milestone.expectedCompletionDate)

            Row{
                WriteText(
                    text = "${milestone.milestone.text}",
                    fontSize = 12f,
                    color = getPillarColor(mission.pillar?.pillarName).darken(0.5f)
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.End)
            ){
                WriteText(
                    text = "in ",
                    fontSize = 10f,
                    color = Color.Gray
                )
                WriteText(
                    text = "${daysToMilestone} day${if (daysToMilestone > 1) "s" else ""} ",
                    fontSize = 11f,
                    color = Color.Black
                )
            }
        }

        Row{
            completedMilestones?.forEach { milestone ->
                Icon(listOf(
                    Icons.Filled.CheckCircle,
                    Icons.Filled.Face,
                    Icons.Filled.Star,
                    Icons.Filled.Favorite,
                    Icons.Filled.ThumbUp,
                ).random(),
                    modifier = Modifier
                        .size(10.dp),
                    contentDescription = "Completed",
                    tint = getPillarColor(mission.pillar?.pillarName).darken())
            }
        }
    }



}
