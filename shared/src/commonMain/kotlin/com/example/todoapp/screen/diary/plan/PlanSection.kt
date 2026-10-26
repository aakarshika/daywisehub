package com.example.todoapp.screen.diary.plan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.TaskProgress
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.minutesLabel
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.ChipBorder
import com.example.todoapp.screen.basicutils.CrownGold
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.LineBlueBorder
import com.example.todoapp.screen.basicutils.Orange180
import com.example.todoapp.screen.basicutils.Orange80
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarLight
import com.example.todoapp.screen.basicutils.pillarStrong
import com.example.todoapp.screen.diary.DiaryUiState
import com.example.todoapp.screen.diary.PlanRow
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.check_square
import todoapp.shared.generated.resources.crown_a
import todoapp.shared.generated.resources.square_b

class PlanRowActions(
    val onToggle: (Long) -> Unit,
    val onCrown: (Long) -> Unit,
    val onSwap: (Long) -> Unit,
    val onRemove: (Long) -> Unit,
    val onDidSome: (Long) -> Unit,
    val onMove: (Long) -> Unit,
    val onSkip: (Long) -> Unit,
    /** The swipe (spec 6.5): an item and how much of it got done, in 5% steps. */
    val onProgress: (Long, Int) -> Unit,
)

/** The tick box and the row's side padding. The swipe's track starts after both, where the title does. */
private val BoxSize = 44.dp
private val RowPadding = 4.dp
/** How long a let-go percent is held while its save comes back. */
private const val LET_GO_HOLD_MILLIS = 3_000L

/** "Picked for you · 2 h 15 min of your 2½ h free" and a link to change the day. */
@Composable
fun PlanSubline(state: DiaryUiState, onChange: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val energy = if (state.energy == Energy.LOW) " · low energy" else ""
        Text(
            "Picked for you · ${minutesLabel(state.plannedMinutes)} of your ${minutesLabel(state.freeMinutes)} free$energy",
            fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier.heightIn(min = 44.dp).widthIn(min = 44.dp).clickable(role = Role.Button) { onChange() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "change",
                fontSize = 13.sp, fontStyle = FontStyle.Italic, color = PeachText,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
fun PlanSection(
    state: DiaryUiState,
    isToday: Boolean,
    evening: Boolean,
    editMode: Boolean,
    actions: PlanRowActions,
    onToggleEvening: () -> Unit,
    onOpenMissions: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(1.dp, LineBlueBorder, RoundedCornerShape(8.dp))
            .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 10.dp)
    ) {
        if (state.fixed.isNotEmpty()) {
            GroupLabel("ALREADY IN YOUR DAY")
            state.fixed.forEach { key(it.taskId) { PlanRowItem(it, evening, editMode, actions) } }
        }
        GroupLabel(if (!isToday) "THIS DAY" else if (evening) "TODAY" else "PICKED FOR TODAY")
        if (state.picks.isEmpty()) {
            Text(
                when {
                    !state.hasMissions -> "No missions yet. Add a few on the Missions tab, and each morning the diary picks from them."
                    isToday -> "Nothing picked yet. Tap the pencil to see what can be added."
                    else -> "Nothing was planned for this day."
                },
                fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            if (!state.hasMissions) SmallChip("Go to Missions") { onOpenMissions() }
        }
        state.picks.forEach { key(it.taskId) { PlanRowItem(it, evening, editMode, actions) } }
        if (isToday && !evening && state.picks.isNotEmpty()) {
            Text(
                "Hold a title to crown it.",
                fontSize = 11.sp, color = InkGray, modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (isToday) {
            SmallChip(if (evening) "Back to the plan" else "Evening check") { onToggleEvening() }
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(text, fontSize = 11.sp, letterSpacing = 1.2.sp, color = InkGray, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
}

/**
 * One item. Dragging sideways across it records how much got done (spec 6.5): the line along the ruled
 * line and the points pill follow the finger in 5% steps, and letting go saves. A did-some keeps its line.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanRowItem(row: PlanRow, evening: Boolean, editMode: Boolean, actions: PlanRowActions) {
    val done = row.state.countsAsDone
    val closed = !row.state.isOpen
    val strong = pillarStrong(row.pillar)
    val moved = row.state == TaskState.MOVED
    val draggable = !moved && !editMode
    // The percent under the finger, while dragging; null otherwise.
    var dragPercent by remember { mutableStateOf<Int?>(null) }
    // The percent let go at, held until the saved row comes back so the line doesn't blink back first.
    // A save that fails never brings it, so it lets go after a moment.
    var letGoPercent by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(row) { letGoPercent = null }
    LaunchedEffect(letGoPercent) {
        if (letGoPercent != null) {
            delay(LET_GO_HOLD_MILLIS)
            letGoPercent = null
        }
    }
    val held = dragPercent ?: letGoPercent
    val percent = held ?: row.progress
    val showLine = held != null || row.state == TaskState.PARTIAL
    // The gesture outlives recompositions (it's keyed on the item), so it reads the latest row and callback.
    val latestRow by rememberUpdatedState(row)
    val onProgress by rememberUpdatedState(actions.onProgress)
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(
                if (draggable) {
                    Modifier
                        .pointerInput(row.taskId) {
                            var raw = 0f
                            try {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        val from = letGoPercent ?: latestRow.progress
                                        raw = from.toFloat()
                                        dragPercent = from
                                    },
                                    onDragEnd = {
                                        val end = dragPercent
                                        dragPercent = null
                                        if (end != null && end != (letGoPercent ?: latestRow.progress)) {
                                            letGoPercent = end
                                            onProgress(latestRow.taskId, end)
                                        }
                                    },
                                    onDragCancel = { dragPercent = null },
                                ) { _, dx ->
                                    val track = (size.width - (BoxSize + RowPadding).toPx()).coerceAtLeast(1f)
                                    raw = (raw + dx / track * 100).coerceIn(0f, 100f)
                                    dragPercent = TaskProgress.rounded(raw)
                                }
                            } finally {
                                // The row stopped being draggable mid-drag (it moved, or swap mode came on): nothing is saved.
                                dragPercent = null
                            }
                        }
                        // A screen reader sets the same value without dragging. The box keeps its own checkbox node.
                        .semantics(mergeDescendants = true) {
                            progressBarRangeInfo = ProgressBarRangeInfo(percent / 100f, 0f..1f, steps = 19)
                            setProgress(label = "Set how much got done") { value ->
                                val target = TaskProgress.rounded(value * 100)
                                if (target != row.progress) actions.onProgress(row.taskId, target)
                                true
                            }
                        }
                } else Modifier
            )
            .drawBehind {
                drawLine(Blue80, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                if (showLine && percent > 0) {
                    val start = (BoxSize + RowPadding).toPx()
                    val end = start + (size.width - start) * percent / 100f
                    drawLine(strong, Offset(start, size.height), Offset(end, size.height), strokeWidth = 3.dp.toPx())
                }
            }
            .padding(vertical = 9.dp, horizontal = RowPadding),
        verticalAlignment = Alignment.Top
    ) {
        // A moved item can't be ticked, unless its copy is still planned: then ticking takes the move back,
        // and the box is less faded, so it doesn't look off.
        val boxAlpha = if (moved && row.canTakeBack) 0.7f else if (closed) 0.4f else 1f
        Box(
            modifier = Modifier.size(BoxSize)
                .clickable(enabled = !moved || row.canTakeBack, role = Role.Checkbox) { actions.onToggle(row.taskId) },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(if (done) Res.drawable.check_square else Res.drawable.square_b),
                contentDescription = when {
                    moved && row.canTakeBack -> "Take back the move of ${row.title}"
                    moved -> "${row.title}, moved"
                    done -> "Untick ${row.title}"
                    else -> "Tick ${row.title}"
                },
                colorFilter = ColorFilter.tint(strong),
                modifier = Modifier.size(24.dp).alpha(boxAlpha)
            )
            if (row.crowned) {
                Image(
                    painter = painterResource(Res.drawable.crown_a),
                    contentDescription = "Crowned",
                    colorFilter = ColorFilter.tint(CrownGold),
                    modifier = Modifier.align(Alignment.TopEnd).size(width = 18.dp, height = 14.dp)
                )
            }
        }
        val crownable = !row.isFixedBlock && row.state.isOpen
        Column(
            modifier = Modifier.weight(1f).then(
                if (crownable) {
                    Modifier.pointerInput(row.taskId) { detectTapGestures(onLongPress = { actions.onCrown(row.taskId) }) }
                } else Modifier
            )
        ) {
            var titleLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
            Text(
                row.title,
                fontSize = 20.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light, letterSpacing = 0.6.sp,
                color = if (closed) InkGray else InkBlack,
                onTextLayout = { titleLayout = it },
                modifier = Modifier.drawWithContent {
                    drawContent()
                    val layout = titleLayout
                    if (done && layout != null) {
                        for (line in 0 until layout.lineCount) {
                            val y = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2
                            drawLine(
                                strong, Offset(layout.getLineLeft(line), y), Offset(layout.getLineRight(line), y),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }
                }
            )
            Text(subline(row), fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, lineHeight = 17.sp)
            if (evening && row.state == TaskState.PLANNED && !row.isFixedBlock) {
                FlowRow(modifier = Modifier.padding(top = 4.dp)) {
                    val half = (row.points + 1) / 2
                    SmallChip(if (row.pillar.scored) "Did some · +$half" else "Did some") { actions.onDidSome(row.taskId) }
                    // No day to name means the mission can't go anywhere this week: leave Move out rather than skip it quietly.
                    row.moveTo?.let { day -> SmallChip("Move to $day") { actions.onMove(row.taskId) } }
                    SmallChip("Skip today") { actions.onSkip(row.taskId) }
                }
            }
            if (editMode && row.state == TaskState.PLANNED && !row.isFixedBlock) {
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    if (!row.movedIn && !row.crowned) SmallChip("Swap") { actions.onSwap(row.taskId) }
                    SmallChip("Remove") { actions.onRemove(row.taskId) }
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp, top = 3.dp)) {
            Text(minutesLabel(row.minutes), fontSize = 11.sp, color = InkGray)
            Spacer(Modifier.height(5.dp))
            Text(
                when {
                    held != null -> "$held%"
                    !row.pillar.scored -> "recharge"
                    row.state == TaskState.PARTIAL -> "+${row.pointsEarned} of ${row.points}"
                    else -> "+${row.points}"
                },
                fontSize = 12.sp, fontWeight = FontWeight.Medium, color = pillarInk(row.pillar),
                // Faded once closed, but not while it shows the dragged percent.
                modifier = Modifier.alpha(if (closed && held == null) 0.4f else 1f)
                    .clip(RoundedCornerShape(10.dp)).background(pillarLight(row.pillar))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

private fun subline(row: PlanRow): String = when (row.state) {
    TaskState.MOVED -> "Moved to ${row.movedTo ?: "another day"}" + if (row.canTakeBack) " · tick to take it back" else ""
    TaskState.SKIPPED -> "Skipped today"
    TaskState.PARTIAL -> "Did some · ${row.progress}%"
    else -> {
        val ticked = if (!row.tickedForYou) null else if (row.reason.isEmpty()) "Ticked for you" else "ticked for you"
        listOfNotNull(row.reason.takeIf { it.isNotEmpty() }, ticked).joinToString(" · ")
    }
}

/** Small rounded choice used for evening options, swap mode and the energy picker. */
@Composable
fun SmallChip(text: String, selected: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier.heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }.padding(end = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 13.sp, color = Color(0xFF4A4A4A),
            modifier = Modifier.clip(shape)
                .border(1.dp, if (selected) Orange180 else ChipBorder, shape)
                .background(if (selected) Orange80 else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}
