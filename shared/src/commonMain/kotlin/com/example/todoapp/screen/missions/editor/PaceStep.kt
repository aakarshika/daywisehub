package com.example.todoapp.screen.missions.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.EditorStep
import com.example.todoapp.domain.MissionDraft
import com.example.todoapp.domain.MissionDrafts
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.Period
import com.example.todoapp.domain.shortLabel
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.diary.plan.SmallChip
import kotlinx.datetime.daysUntil

/** Step 4. What it asks depends on the kind (spec section 3). */
@Composable
fun PaceStep(state: EditorUiState, viewModel: MissionEditorViewModel) {
    val draft = state.draft
    when (draft.kind) {
        MissionKind.BLOCK -> BlockPace(draft, viewModel)
        MissionKind.RHYTHM -> RhythmPace(draft, viewModel)
        MissionKind.RECHARGE -> RechargePace(draft, EditorStep.PACE in state.answered, viewModel)
        MissionKind.FINISH_LINE -> {
            CountPace(draft, viewModel, finishLine = true)
            FinishPace(draft, viewModel)
        }
        else -> CountPace(draft, viewModel, finishLine = false)
    }
}

/**
 * How often, for habits and finish lines, with optional preferred days on a weekly count.
 * A finish line is counted per week only (spec 3), up to 7 times.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountPace(draft: MissionDraft, viewModel: MissionEditorViewModel, finishLine: Boolean) {
    FieldLabel("How often")
    FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        if (!finishLine) {
            SmallChip("Every day", selected = draft.period == Period.DAY) { viewModel.setCount(1, Period.DAY) }
        }
        val mostPerWeek = if (finishLine) 7 else 6
        (1..mostPerWeek).forEach { n ->
            SmallChip(if (n == 1) "Once a week" else "$n× a week", selected = draft.period == Period.WEEK && draft.count == n) {
                viewModel.setCount(n, Period.WEEK)
            }
        }
        if (!finishLine) {
            (1..3).forEach { n ->
                SmallChip(if (n == 1) "Once a month" else "$n× a month", selected = draft.period == Period.MONTH && draft.count == n) {
                    viewModel.setCount(n, Period.MONTH)
                }
            }
        }
    }
    if (draft.usesDays) {
        FieldLabel("Any days that suit it best? (optional)")
        DayToggles(draft.weekDays, viewModel::toggleDay)
    }
}

/** By when, and the milestones that show you're done. */
@Composable
private fun FinishPace(draft: MissionDraft, viewModel: MissionEditorViewModel) {
    val end = draft.endDate
    FieldLabel("By when")
    if (end != null) {
        val weeks = (viewModel.today.daysUntil(end) + 3) / 7
        val away = when {
            weeks <= 0 -> "already passed"
            weeks == 1 -> "in 1 week"
            else -> "in $weeks weeks"
        }
        val limits = viewModel.endDateLimits(draft)
        Stepper(
            "Ends", "${end.shortLabel()} · $away",
            onMinus = { viewModel.shiftEndDate(-1) }, onPlus = { viewModel.shiftEndDate(1) },
            minusEnabled = end > limits.start, plusEnabled = end < limits.endInclusive
        )
    }
    FieldLabel("How will you know you're done? (optional)")
    val dates = MissionDrafts.milestoneDates(draft.startDate, draft.endDate, draft.milestones.size)
    draft.milestones.forEachIndexed { i, milestone ->
        MilestoneRow(
            text = milestone.text,
            placeholder = "Milestone ${i + 1}",
            due = dates.getOrNull(i)?.shortLabel().orEmpty(),
            onChange = { viewModel.setMilestone(i, it) },
            onRemove = { viewModel.removeMilestone(i) }
        )
    }
    if (draft.milestones.size < MissionDrafts.MAX_MILESTONES) {
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            SmallChip("+ Add a milestone") { viewModel.addMilestone() }
        }
    }
}

@Composable
private fun MilestoneRow(text: String, placeholder: String, due: String, onChange: (String) -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = text,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 17.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light, color = InkBlack),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            cursorBrush = SolidColor(PeachText),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).drawBehind {
                        drawLine(Blue80, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                    },
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) Text(placeholder, fontSize = 17.sp, fontStyle = FontStyle.Italic, color = InkGray.copy(alpha = 0.6f))
                    inner()
                }
            }
        )
        Text(due, fontSize = 12.sp, color = InkGray, modifier = Modifier.padding(horizontal = 8.dp))
        Box(
            modifier = Modifier.size(44.dp).clickable(role = Role.Button, onClickLabel = "Remove milestone") { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove milestone", tint = InkGray, modifier = Modifier.size(18.dp))
        }
    }
}

/** Which days and from when to when, in half hours. */
@Composable
private fun BlockPace(draft: MissionDraft, viewModel: MissionEditorViewModel) {
    val step = MissionDrafts.CLOCK_STEP
    FieldLabel("Which days")
    DayToggles(draft.weekDays, viewModel::toggleDay)
    FieldLabel("When")
    Stepper(
        "From", MissionWords.clockLabel(draft.startMinute),
        onMinus = { viewModel.shiftStart(-step) }, onPlus = { viewModel.shiftStart(step) },
        minusEnabled = draft.startMinute > 0, plusEnabled = draft.startMinute < MissionDrafts.DAY_MINUTES - step
    )
    Stepper(
        "To", MissionWords.clockLabel(draft.endMinute),
        onMinus = { viewModel.shiftEnd(-step) }, onPlus = { viewModel.shiftEnd(step) },
        minusEnabled = draft.endMinute > draft.startMinute + step, plusEnabled = draft.endMinute < MissionDrafts.DAY_MINUTES
    )
    Text(
        draft.blocker
            ?: "${MissionWords.hoursPhrase(draft.endMinute - draft.startMinute)} each time. Fixed blocks don't use your free hours.",
        fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)
    )
}

/** Once every N days, weeks or months, up to 3 months. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RhythmPace(draft: MissionDraft, viewModel: MissionEditorViewModel) {
    val range = MissionDrafts.intervalRange(draft.intervalUnit)
    val unitName = draft.intervalUnit.name.lowercase()
    Stepper(
        "Every", if (draft.intervalCount == 1) "1 $unitName" else "${draft.intervalCount} ${unitName}s",
        onMinus = { viewModel.setInterval(draft.intervalCount - 1, draft.intervalUnit) },
        onPlus = { viewModel.setInterval(draft.intervalCount + 1, draft.intervalUnit) },
        minusEnabled = draft.intervalCount > range.first, plusEnabled = draft.intervalCount < range.last
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        listOf(Period.DAY to "Days", Period.WEEK to "Weeks", Period.MONTH to "Months").forEach { (unit, label) ->
            SmallChip(label, selected = draft.intervalUnit == unit) { viewModel.setInterval(draft.intervalCount, unit) }
        }
    }
    Text(
        "Up to every 3 months for now.",
        fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)
    )
}

/** About N times a week; one tap answers. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RechargePace(draft: MissionDraft, answered: Boolean, viewModel: MissionEditorViewModel) {
    FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        (1..7).forEach { n ->
            SmallChip(if (n == 1) "About once a week" else "About $n× a week", selected = answered && draft.count == n) {
                viewModel.pickRechargeCount(n)
            }
        }
    }
}
