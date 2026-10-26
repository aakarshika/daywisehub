package com.example.todoapp.screen.diary.diaryitem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.components.CustomCheckbox
import com.example.todoapp.screen.basicutils.components.DiaryLineText
import com.example.todoapp.domain.pillarType
import com.example.todoapp.screen.basicutils.pillarStrong

private const val MilestoneRowHeight = 36

fun LazyListScope.DiaryMilestoneSection(
    milestones: List<MilestoneWithDetails>,
    onMilestoneChecked: (MilestoneWithDetails, Boolean) -> Unit
) {
    if (milestones.isEmpty()) return
    item(key = "milestones") {
        DiaryMilestoneSectionContent(
            milestones = milestones,
            onMilestoneChecked = onMilestoneChecked
        )
    }
}

@Composable
fun DiaryMilestoneSectionContent(
    milestones: List<MilestoneWithDetails>,
    onMilestoneChecked: (MilestoneWithDetails, Boolean) -> Unit
) {
    if (milestones.isEmpty()) return

    val upcoming = milestones.filter { it.milestone.status != TaskStatus.COMPLETED.value }
    val completed = milestones.filter { it.milestone.status == TaskStatus.COMPLETED.value }

    Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
        if (upcoming.isNotEmpty()) {
            SubheadingLabel("Upcoming")
            upcoming.forEach { mwd ->
                MilestoneRow(
                    mwd = mwd,
                    achieved = false,
                    onAchievedChange = { onMilestoneChecked(mwd, it) }
                )
            }
        }
        if (completed.isNotEmpty()) {
            SubheadingLabel("Completed")
            completed.forEach { mwd ->
                MilestoneRow(
                    mwd = mwd,
                    achieved = true,
                    onAchievedChange = { onMilestoneChecked(mwd, it) }
                )
            }
        }
    }
}

@Composable
private fun SubheadingLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 8.dp, start = 15.dp, bottom = 4.dp),
        fontSize = 14.sp,
        color = Color.Gray
    )
}

@Composable
private fun MilestoneRow(
    mwd: MilestoneWithDetails,
    achieved: Boolean,
    onAchievedChange: (Boolean) -> Unit
) {
    val rowHeight = MilestoneRowHeight
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight.dp)
            .drawBehind {
                val lineSpacing = 26.dp.toPx()
                val horizontalPadding = 15.dp.toPx()
                var y = 0f
                while (y <= size.height) {
                    drawLine(
                        color = Blue80,
                        start = Offset(horizontalPadding, y),
                        end = Offset(size.width - horizontalPadding, y),
                        strokeWidth = 1f
                    )
                    y += lineSpacing
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight.dp)
                .padding(top = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 15.dp, top = 2.dp)
            ) {
                CustomCheckbox(
                    checked = achieved,
                    onCheckedChange = onAchievedChange,
                    modifier = Modifier.size(20.dp),
                    checkedColor = pillarStrong(mwd.mission.pillarType),
                    uncheckedColor = Color.Gray,
                    disabledColor = Color.Gray,
                    iconTag = "Milestone"
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 3.dp, start = 8.dp, end = 8.dp)
            ) {
                DiaryLineText(
                    text = mwd.milestone.text,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(top = 3.dp, end = 15.dp)
            ) {
                val daysTo = MyDate.daysBetween(MyDate.now(), mwd.milestone.expectedCompletionDate)
                val dayLabel = when {
                    daysTo < 0 -> "${-daysTo}d ago"
                    daysTo == 0 -> "Today"
                    else -> "in ${daysTo}d"
                }
                DiaryLineText(
                    text = dayLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
