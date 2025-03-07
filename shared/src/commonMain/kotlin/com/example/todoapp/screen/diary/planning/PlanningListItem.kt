package com.example.todoapp.screen.diary.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoapp.screen.diary.diaryitem.components.GeneralItem

@Composable
fun PlanningListItem(
    mission : CategorizedMission,
    extraInfo : String,
    onMissionClick : (CategorizedMission) -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 10.dp)) {
        ItemComponents(
            mission, onMissionClick
        )
    }
}
