package com.example.todoapp.screen.missions.editpopup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.screen.basicutils.components.WritingTextField
import com.example.todoapp.screen.basicutils.getPillarColor
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MissionHeaderSection(
    mission: MutableState<Mission?>,
    pillarSelected: MutableState<Pillar?>,
    pillarOptions: List<Pillar>,
    fieldBackground: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Task title section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = fieldBackground),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "What would you like to achieve?",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                WritingTextField(
                    taskName = mission.value?.missionTitle ?: "",
                    modifier = Modifier.fillMaxWidth(),
                    placeHolder = "Enter your mission title...",
                    valueChanged = { newTitle ->
                        mission.value = mission.value?.copy(missionTitle = newTitle)
                    }
                )
            }
        }

        // Pillar selection section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = fieldBackground),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "To help build your",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // Pillar selector with dropdown
                PillarSelector(
                    pillarSelected = pillarSelected,
                    pillarOptions = pillarOptions
                )

//                Spacer(modifier = Modifier.height(16.dp))
//                SwipeablePillarDescription(
//                    pillarSelected = pillarSelected,
//                    pillarOptions = pillarOptions
//                )
            }
        }
    }
}


@Composable
private fun SwipeablePillarDescription(
    pillarSelected: MutableState<Pillar?>,
    pillarOptions: List<Pillar>
) {
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = 80f
    val animationSpec =
        tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
    val animatableOffset = remember { Animatable(0f) }

    val currentPillar = pillarSelected.value ?: return
    val pillarColor = getPillarColor(currentPillar.pillarName)

    LaunchedEffect(offsetX) {
        if (abs(offsetX) > swipeThreshold) {
            // Determine direction and get next/previous pillar
            val currentIndex = pillarOptions.indexOf(currentPillar)
            val newIndex = if (offsetX > 0) {
                // Swipe right - previous pillar
                if (currentIndex <= 0) pillarOptions.size - 1 else currentIndex - 1
            } else {
                // Swipe left - next pillar
                (currentIndex + 1) % pillarOptions.size
            }

            // Update selected pillar
            pillarSelected.value = pillarOptions[newIndex]

            // Reset offset
            offsetX = 0f
            animatableOffset.snapTo(0f)
        } else if (offsetX != 0f) {
            // Animate back to center if not past threshold
            animatableOffset.animateTo(
                targetValue = 0f,
                animationSpec = animationSpec
            )
            offsetX = 0f
        }
    }

    Surface(
        color = pillarColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {

                        // Let LaunchedEffect handle the animation back
                    },
                    onDragCancel = {
                        // Reset on cancel
                        offsetX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        // Limit the visual drag
                        offsetX = offsetX.coerceIn(-150f, 150f)
//                        animatableOffset.snapTo(offsetX)
                    }
                )
            }
            .offset { IntOffset(animatableOffset.value.roundToInt(), 0) }
    ) {
    }
}

@Composable
private fun PillarDescription(pillar: Pillar) {
    val pillarColor = getPillarColor(pillar.pillarName)

    Surface(
        color = pillarColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual indicators for the pillar type
            Column(
                modifier = Modifier.padding(end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(pillarColor.copy(alpha = 0.2f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Face,
                        contentDescription = null,
                        tint = pillarColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Pillar rating indicators
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index < 2) pillarColor
                                    else pillarColor.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }

            // Pillar description
            Column {
                Text(
                    text = pillar.pillarName ?: "Pillar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = pillarColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = pillar.pillarDescription ?: "No description available",
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}


@Composable
private fun PillarSelector(
    pillarSelected: MutableState<Pillar?>,
    pillarOptions: List<Pillar>
) {
    var expanded by remember { mutableStateOf(false) }
    val currentPillar = pillarSelected.value
    val pillarColor = getPillarColor(currentPillar?.pillarName)

    Box(modifier = Modifier.fillMaxWidth()) {
        // Selected pillar display
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            color = pillarColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(10.dp))

                // Visual indicators for the pillar type
                // Pillar color indicator
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(pillarColor)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Face,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Pillar name
                    Text(
                        text = currentPillar?.pillarName ?: "Select a pillar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = currentPillar?.pillarDescription
                            ?: "No description available",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))

                // Dropdown indicator - now placed outside any weight containers
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select pillar",
                    tint = Color.DarkGray,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp)
                )

                Spacer(modifier = Modifier.width(5.dp))
            }
        }

        // Dropdown menu for pillar selection
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color.White)
        ) {
            pillarOptions.forEach { pillar ->
                val isPillarSelected = pillar.pillarId == currentPillar?.pillarId
                val itemColor = getPillarColor(pillar.pillarName)

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Color indicator
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(itemColor)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Pillar name with selected indicator
                            Text(
                                text = pillar.pillarName ?: "Pillar",
                                fontWeight = if (isPillarSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isPillarSelected) itemColor else Color.Black
                            )
                        }
                    },
                    onClick = {
                        pillarSelected.value = pillar
                        expanded = false
                    },
                    modifier = Modifier.background(
                        if (isPillarSelected) itemColor.copy(alpha = 0.1f) else Color.Transparent
                    )
                )

                if (pillar != pillarOptions.last()) {
                    Divider(modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        }
    }
}