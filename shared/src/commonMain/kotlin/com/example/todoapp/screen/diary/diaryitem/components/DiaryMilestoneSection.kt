package com.example.todoapp.screen.diary.diaryitem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.domain.Badge
import com.example.todoapp.domain.pillarType
import com.example.todoapp.domain.shortLabel
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarLight
import com.example.todoapp.screen.metrics.badges.BadgeSticker
import com.example.todoapp.screen.metrics.badges.badgeLines
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * A milestone in the to-do list, under its mission's item or on its own: set apart from the items by a
 * tinted strip in its pillar's light color and a flag, a line drawing until it's done, then the Milestone
 * sticker. Tapping it ticks it off on [page], or unticks it. The date counts from [page].
 */
@Composable
fun MilestoneLine(mwd: MilestoneWithDetails, page: LocalDate, indent: Boolean, onChecked: (Boolean) -> Unit) {
    val done = mwd.milestone.status == TaskStatus.COMPLETED.value
    val pillar = mwd.mission.pillarType
    val daysTo = page.daysUntil(mwd.milestone.expectedCompletionDate.toLocalDate())
    val label = when {
        done -> mwd.milestone.actualCompletionDate?.let { "done ${it.toLocalDate().shortLabel()}" } ?: "done"
        daysTo < 0 -> "${-daysTo}d late"
        daysTo == 0 -> "due this day"
        else -> "due in ${daysTo}d"
    }
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = if (indent) 26.dp else 0.dp, top = 2.dp, bottom = 4.dp)
            .background(pillarLight(pillar).copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .heightIn(min = 44.dp)
            .clickable(role = Role.Checkbox, onClickLabel = if (done) "Mark not done" else "Mark done") { onChecked(!done) }
            .semantics { stateDescription = if (done) "Done" else "Not done" }
            .padding(start = 6.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            if (done) BadgeSticker(Badge.MILESTONE, page, 32.dp, byRarity = false)
            else Icon(badgeLines(Badge.MILESTONE), contentDescription = null, tint = pillarInk(pillar), modifier = Modifier.size(28.dp))
        }
        Box(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (indent) "MILESTONE" else "MILESTONE · ${mwd.mission.title.uppercase()}",
                fontSize = 9.sp, letterSpacing = 1.1.sp, color = pillarInk(pillar)
            )
            Text(
                mwd.milestone.text, fontSize = 15.sp, fontStyle = FontStyle.Italic, color = InkBlack,
                textDecoration = if (done) TextDecoration.LineThrough else null
            )
        }
        Text(label, fontSize = 11.sp, color = InkGray, modifier = Modifier.padding(start = 6.dp))
    }
}
