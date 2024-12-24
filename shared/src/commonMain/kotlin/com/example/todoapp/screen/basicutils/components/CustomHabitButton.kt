package com.example.todoapp.screen.basicutils.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun CustomHabitButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedColor: Color = Color.Red,//Orange80, // Default checked color (Orange)
    uncheckedColor: Color = Color.Gray, // Default unchecked color (Gray)
    disabledColor: Color = Color.Gray // Color when disabled
) {
    // Animation for smooth scaling
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.2f else 1f,
        animationSpec = tween(durationMillis = 300)
    )

    // Choose the icon based on the checked state
    val iconRes = Icons.Default.DateRange

    // Determine the tint color based on the state
    val tintColor = if (!enabled) disabledColor else if (checked) checkedColor else uncheckedColor

    Image(
        iconRes,
        contentDescription = if (checked) "Checked" else "Unchecked",
        colorFilter = ColorFilter.tint(tintColor),
        modifier = modifier
            .size(if (checked) 30.dp else 20.dp)
            .clickable(enabled = enabled) {
                onCheckedChange(!checked)
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
    )
}
