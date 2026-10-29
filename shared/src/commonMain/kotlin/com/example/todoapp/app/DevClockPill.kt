package com.example.todoapp.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Dev builds only: a small pill at the top that moves [AppClock] ahead, to try tomorrow, the evening check
 * or a new week without waiting. Moving past midnight works like the real midnight: the diary moves on.
 */
@Composable
fun DevClockPill(modifier: Modifier = Modifier) {
    val ahead by AppClock.ahead.collectAsState()
    var open by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }
    val now = remember(ahead, tick) { AppClock.now() }
    val shifted = ahead != Duration.ZERO
    val shape = RoundedCornerShape(50)

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (shifted) "Dev clock · ${format(now)} (+${aheadLabel(ahead)})" else "Dev clock · real time",
            fontSize = 11.sp,
            color = if (shifted) Color.White else Color(0xFF6B6B6B),
            modifier = Modifier
                .background(if (shifted) Color(0xFFB4532A) else Color(0x1A000000), shape)
                .clickable { open = !open }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        if (open) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                DevButton("+3 h") { AppClock.moveAhead(3.hours) }
                DevButton("+1 day") { AppClock.moveAhead(1.days) }
                DevButton("+1 week") { AppClock.moveAhead(7.days) }
                DevButton("Now") { AppClock.reset() }
            }
        }
    }
}

@Composable
private fun DevButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 13.sp,
        color = Color.White,
        modifier = Modifier
            .background(Color(0xE6333333), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

private fun format(time: LocalDateTime): String {
    val day = time.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val month = time.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val minute = time.minute.toString().padStart(2, '0')
    return "$day ${time.dayOfMonth} $month, ${time.hour}:$minute"
}

private fun aheadLabel(ahead: Duration): String {
    val days = ahead.inWholeDays
    val hours = ahead.inWholeHours - days * 24
    return listOfNotNull(days.takeIf { it > 0 }?.let { "$it d" }, hours.takeIf { it > 0 }?.let { "$it h" }).joinToString(" ")
}
