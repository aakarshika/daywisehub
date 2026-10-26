package com.example.todoapp.screen.metrics

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.diary.plan.SmallChip

/** Spec 9: the Metrics tab, one week at a time. */
@Composable
fun MetricsScreen(viewModel: MetricsViewModel) {
    val state by viewModel.state.collectAsState()
    val s = state

    Box(modifier = Modifier.fillMaxSize().background(Light_Yellowww)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (s != null) {
                item(key = "header") {
                    WeekHeader(s, onPrevious = viewModel::previousWeek, onNext = viewModel::nextWeek, onThisWeek = viewModel::backToThisWeek)
                }
                item(key = "rings") { PillarRings(s.score) }
            }
            item(key = "bottom-space") { Spacer(Modifier.height(120.dp)) }
        }
    }
}

/** "This week" and its dates, arrows a week back and forward, and "Back to this week" when away from it. */
@Composable
private fun WeekHeader(state: MetricsUiState, onPrevious: () -> Unit, onNext: () -> Unit, onThisWeek: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 30.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.title, fontSize = 30.sp, fontStyle = FontStyle.Italic, letterSpacing = 0.5.sp, color = InkGray,
                    modifier = Modifier.semantics { heading() }
                )
                Text(state.range, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, modifier = Modifier.padding(top = 4.dp))
            }
            WeekArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous week", enabled = state.canGoBack, onClick = onPrevious)
            WeekArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next week", enabled = state.canGoForward, onClick = onNext)
        }
        if (!state.isThisWeek) SmallChip("Back to this week", onClick = onThisWeek)
    }
}

/** A 44dp arrow button; faded and inactive at either end. */
@Composable
private fun WeekArrow(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) InkGray else InkGray.copy(alpha = 0.3f))
    }
}
