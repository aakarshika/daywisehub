package com.example.todoapp.screen.missions.editpopup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.screen.missions.getDarkPillarColor
import com.example.todoapp.screen.missions.getPillarColor

/**
 * Frequency slider for selecting mission frequency
 */
@Composable
fun FrequencySlider(
    mission: MutableState<Mission?>,
    pillar: Pillar?,
    onValueChange: (Float) -> Unit
) {
    val intermediateLabels = listOf("OCCASIONAL", "COMFORTABLE", "BALANCED", "RIGOROUS")

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Frequency labels
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                intermediateLabels.forEachIndexed { index, label ->
                    val sliderPosition = (index + 1) * 0.25f
                    val isCurrentValue = (mission.value?.frequencySetValue ?: 0f) in
                            (sliderPosition - 0.125f)..(sliderPosition + 0.125f)

                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isCurrentValue)
                                getDarkPillarColor(pillar?.pillarName)
                            else
                                Color.Transparent
                            )
                            .padding(4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCurrentValue) Color.White else Color.Black
                        )
                    }
                }
            }

            // Slider
            Slider(
                value = mission.value?.frequencySetValue ?: 0f,
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = getPillarColor(pillar?.pillarName),
                    activeTrackColor = getPillarColor(pillar?.pillarName),
                    inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                )
            )
        }
    }
}