package com.example.todoapp.screen.diary.plan

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import com.example.todoapp.screen.metrics.badges.DayFactorSticker
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.TaskProgress
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.minutesLabel
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.diary.diaryitem.components.MilestoneLine
import kotlinx.datetime.LocalDate
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
import kotlin.math.abs
import kotlin.math.roundToInt

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

private val BoxSize = 44.dp
private val RowPadding = 4.dp
/** Where the swipe's track starts: after the row's side padding and the tick box, where the title does. */
private val TrackStart = BoxSize + RowPadding
/** How long a let-go percent is held while its save comes back. */
private const val LET_GO_HOLD_MILLIS = 3_000L
/** How far the finger must end up sideways from where it went down for letting go to save. */
private val MinSwipe = 24.dp
/** The width a swipe left uncovers per chip: Move and Skip are words, Reset and Remove narrower icons. */
private val ChipWidth = 74.dp
private val IconChipWidth = 52.dp
private fun chipWidth(chip: String): Dp = if (chip == "reset" || chip == "remove") IconChipWidth else ChipWidth

/** "Picked for you · 2 h 15 min of your 2½ h free" and a link to change the day. */
@Composable
fun PlanSubline(state: DiaryUiState, onChange: () -> Unit) {
//    Column(
//        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        val energy = if (state.energy == Energy.LOW) " · low energy" else ""
//        Text(
//            "Picked for you · ${minutesLabel(state.plannedMinutes)} of your ${minutesLabel(state.freeMinutes)} free$energy",
//            fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.Center
//        )
//        Box(
//            modifier = Modifier.heightIn(min = 44.dp).widthIn(min = 44.dp).clickable(role = Role.Button) { onChange() },
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                "change",
//                fontSize = 13.sp, fontStyle = FontStyle.Italic, color = PeachText,
//                textDecoration = TextDecoration.Underline,
//                modifier = Modifier.padding(horizontal = 12.dp)
//            )
//        }
//    }
}

@Composable
fun PlanSection(
    state: DiaryUiState,
    isToday: Boolean,
    isTomorrow: Boolean = false,
    evening: Boolean,
    /** The plan buttons are open: no boxes, drags or swipes until Looks good. */
    editing: Boolean = false,
    editMode: Boolean,
    actions: PlanRowActions,
    onOpenMissions: () -> Unit,
    /** The page's milestones: each under its mission's item, the rest after the items. */
    milestones: List<MilestoneWithDetails> = emptyList(),
    page: LocalDate,
    onMilestone: (MilestoneWithDetails, Boolean) -> Unit = { _, _ -> },
) {
    val byMission = milestones.groupBy { it.mission.missionId }
    val onTheList = (state.fixed + state.picks).map { it.missionId }.toSet()
    @Composable
    fun under(row: PlanRow) = byMission[row.missionId].orEmpty().forEach { m ->
        key("m-${m.milestone.milestoneId}") { MilestoneLine(m, page, indent = true) { onMilestone(m, it) } }
    }
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        if (state.installDay) {
            Text(
                "Tick what got done. Swipe right for some of it, left to move, skip or remove.",
                fontSize = 12.sp, fontStyle = FontStyle.Italic, color = InkGray, lineHeight = 16.sp
            )
        }
        if (state.fixed.isNotEmpty()) {
            GroupLabel("ALREADY IN YOUR DAY")
            state.fixed.forEach { key(it.taskId) { PlanRowItem(it, evening, editing, editMode, actions, answers = isToday) }; under(it) }
        }
        GroupLabel(
            when {
                isTomorrow -> "PICKED FOR TOMORROW"
                !isToday -> "THIS DAY"
                evening -> "TODAY"
                else -> "PICKED FOR TODAY"
            }
        )
        if (state.picks.isEmpty()) {
            if (state.hasMissions && !isToday && !isTomorrow) {
                // A day gone by with nothing on it just sleeps.
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    DayFactorSticker(
                        "sleeping", tilt = -4f, size = 72.dp,
                        modifier = Modifier.semantics { contentDescription = "Nothing was planned for this day" }
                    )
                }
            } else {
                Text(
                    if (!state.hasMissions) "No missions yet. Add a few on the Missions tab, and each morning the diary picks from them."
                    else "Nothing picked yet. Tap the pencil to see what can be added.",
                    fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
            if (!state.hasMissions) SmallChip("Go to Missions") { onOpenMissions() }
        }
        state.picks.forEach { key(it.taskId) { PlanRowItem(it, evening, editing, editMode, actions, answers = isToday) }; under(it) }
        // Milestones whose mission has no item on this day.
        val alone = milestones.filter { it.mission.missionId !in onTheList }
        if (alone.isNotEmpty()) {
            GroupLabel("MILESTONES")
            alone.forEach { m -> key("m-${m.milestone.milestoneId}") { MilestoneLine(m, page, indent = false) { onMilestone(m, it) } } }
        }
        if (state.installDay && (isToday || isTomorrow) && !evening && state.picks.isNotEmpty()) {
            Text(
                "Hold a title to crown it.",
                fontSize = 11.sp, color = InkGray, modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(text, fontSize = 11.sp, letterSpacing = 1.2.sp, color = InkGray, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
}

/**
 * One item. Dragging sideways across it records how much got done (spec 6.5): past the touch slop, the
 * line along the ruled line ends under the finger, in 5% steps, the points pill shows the percent, and
 * letting go saves, once the finger is at least [MinSwipe] sideways from where it went down. A shorter
 * drag, such as a scroll that slips sideways, puts the line back as a cancel does and saves nothing. Only
 * a done item looks ticked; a did-some keeps its open box, its line and its percent.
 *
 * A drag that starts leftward on an open, did-some or done item slides it left instead, to show Remove, and on
 * today's page also Reset (once some is done), Move (while nothing is) and Skip; a fixed block gets Reset only. A slide of
 * [MinSwipe] or more opens it; one as far right, or an answer, closes it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanRowItem(row: PlanRow, evening: Boolean, editing: Boolean, editMode: Boolean, actions: PlanRowActions, answers: Boolean) {
    // "If filled completely, it gets ticked off": a did-some isn't ticked.
    val ticked = row.state == TaskState.DONE
    val closed = !row.state.isOpen
    val strong = pillarStrong(row.pillar)
    val moved = row.state == TaskState.MOVED
    val draggable = !moved && !editing && !editMode
    val crownable = !row.isFixedBlock && row.state.isOpen
    // A fixed block can only be reset: it isn't moved, skipped or removed. Reset, Move and Skip answer today's
    // list; Remove works on any page, so an item added by mistake can go.
    val chips = listOfNotNull(
        "reset".takeIf { answers && row.progress > 0 },
        "move".takeIf { answers && row.state == TaskState.PLANNED && !row.isFixedBlock && row.moveTo != null },
        "skip".takeIf { answers && !row.isFixedBlock },
        "remove".takeIf { !row.isFixedBlock },
    )
    val answerable = !editing && row.state.let { it == TaskState.PLANNED || it == TaskState.PARTIAL || it == TaskState.DONE } && chips.isNotEmpty()
    val actionsWidth = chips.fold(0.dp) { sum, chip -> sum + chipWidth(chip) }
    val latestActionsWidth by rememberUpdatedState(actionsWidth)
    // How far the item is slid left to show Move and Skip: 0, or down to minus the actions' width.
    var slideX by remember { mutableFloatStateOf(0f) }
    var sliding by remember { mutableStateOf(false) }
    val settledX by animateFloatAsState(slideX, label = "slide")
    val shownX = if (sliding) slideX else settledX
    LaunchedEffect(answerable) { if (!answerable) slideX = 0f }
    val latestAnswerable by rememberUpdatedState(answerable)
    // The percent under the finger, while dragging; null otherwise.
    var dragPercent by remember { mutableStateOf<Int?>(null) }
    // Where the finger went down, for how far a drag went: the drag only reports where it passed the slop.
    var downX by remember { mutableFloatStateOf(0f) }
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

    // A screen reader's change. It starts from the held percent, so quick repeats each take their own step.
    fun adjust(to: (from: Int) -> Int): Boolean {
        val from = letGoPercent ?: latestRow.progress
        val target = to(from).coerceIn(0, TaskProgress.FULL)
        if (target == from) return false
        letGoPercent = target
        onProgress(latestRow.taskId, target)
        return true
    }

    Column(
        // The evening buttons fold away once one is used, instead of the rows below jumping up.
        modifier = Modifier.fillMaxWidth().animateContentSize()
            .then(
                if (draggable) {
                    Modifier.pointerInput(row.taskId) {
                        // Only watches: every touch's first point, consumed or not.
                        awaitEachGesture { downX = awaitFirstDown(requireUnconsumed = false).position.x }
                    }.pointerInput(row.taskId) {
                        // The percent at [x] along the track, from the title's left edge to the row's right edge.
                        fun percentAt(x: Float): Int {
                            val start = TrackStart.toPx()
                            return TaskProgress.rounded((x - start) / (size.width - start).coerceAtLeast(1f) * 100)
                        }
                        var lastX = 0f
                        var slide = false
                        // Where the slide started, so a short flick either way opens or closes it.
                        var slideFrom = 0f
                        fun settle() {
                            val full = latestActionsWidth.toPx()
                            val min = MinSwipe.toPx()
                            sliding = false
                            slideX = when {
                                slideX < slideFrom - min -> -full
                                slideX > slideFrom + min -> 0f
                                slideFrom < -full / 2 -> -full
                                else -> 0f
                            }
                        }
                        try {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    slide = latestAnswerable && (slideX < 0f || it.x < downX)
                                    if (slide) {
                                        slideFrom = slideX
                                        sliding = true
                                    } else {
                                        lastX = it.x
                                        dragPercent = percentAt(it.x)
                                    }
                                },
                                onDragEnd = {
                                    if (slide) return@detectHorizontalDragGestures settle()
                                    val end = dragPercent
                                    dragPercent = null
                                    // A slip isn't a swipe: too short, and it ends as a cancel does.
                                    val far = abs(lastX - downX) >= MinSwipe.toPx()
                                    // Against what's stored, not what's held, so a retry after a failed save goes through.
                                    if (far && end != null && end != latestRow.progress) {
                                        letGoPercent = end
                                        onProgress(latestRow.taskId, end)
                                    }
                                },
                                onDragCancel = { if (slide) settle() else dragPercent = null },
                            ) { change, amount ->
                                if (slide) {
                                    slideX = (slideX + amount).coerceIn(-latestActionsWidth.toPx(), 0f)
                                } else {
                                    lastX = change.position.x
                                    dragPercent = percentAt(lastX)
                                }
                            }
                        } finally {
                            // The row stopped being draggable mid-drag (it moved, or swap mode came on): nothing is saved.
                            dragPercent = null
                            if (sliding) settle()
                        }
                    }
                } else Modifier
            )
            // One node for a screen reader in every mode. The box keeps its own checkbox node.
            .semantics(mergeDescendants = true) {
                val readerActions = mutableListOf<CustomAccessibilityAction>()
                if (draggable) {
                    progressBarRangeInfo = ProgressBarRangeInfo(percent / 100f, 0f..1f, steps = 19)
                    stateDescription = "$percent%"
                    setProgress(label = "Set how much got done") { value -> adjust { TaskProgress.rounded(value * 100) } }
                    // iOS reads neither the range nor setProgress, so the steps are actions too.
                    readerActions += CustomAccessibilityAction("${TaskProgress.STEP}% more") { adjust { it + TaskProgress.STEP } }
                    readerActions += CustomAccessibilityAction("${TaskProgress.STEP}% less") { adjust { it - TaskProgress.STEP } }
                }
                if (crownable) {
                    // The title's long-press, for a screen reader: Android's hold, and an action iOS lists.
                    val crown = if (row.crowned) "Remove crown" else "Crown"
                    onLongClick(label = crown) {
                        actions.onCrown(row.taskId)
                        true
                    }
                    readerActions += CustomAccessibilityAction(crown) {
                        actions.onCrown(row.taskId)
                        true
                    }
                }
                if (readerActions.isNotEmpty()) customActions = readerActions
            }
            .drawBehind {
                drawLine(Blue80, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                if (showLine && percent > 0) {
                    val start = TrackStart.toPx()
                    val end = start + (size.width - start) * percent / 100f
                    drawLine(strong, Offset(start, size.height), Offset(end, size.height), strokeWidth = 3.dp.toPx())
                }
            }
            .padding(vertical = 9.dp, horizontal = RowPadding),
    ) {
      Box(modifier = Modifier.fillMaxWidth().clipToBounds()) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.offset { IntOffset(shownX.roundToInt(), 0) }) {
            // A moved item can't be ticked, unless its copy is still planned: then ticking takes the move back,
            // and the box is less faded, so it doesn't look off.
            val boxAlpha = if (moved && row.canTakeBack) 0.7f else if (closed) 0.4f else 1f
            // While the plan buttons are open the box hides; its space stays, so the titles don't jump.
            Box(
                modifier = Modifier.size(BoxSize)
                    .clickable(enabled = !editing && (!moved || row.canTakeBack), role = Role.Checkbox) { actions.onToggle(row.taskId) },
                contentAlignment = Alignment.Center
            ) {
                if (!editing) {
                    Image(
                        painter = painterResource(if (ticked) Res.drawable.check_square else Res.drawable.square_b),
                        contentDescription = when {
                            moved && row.canTakeBack -> "Take back the move of ${row.title}"
                            moved -> "${row.title}, moved"
                            ticked -> "Untick ${row.title}"
                            else -> "Tick ${row.title}"
                        },
                        colorFilter = ColorFilter.tint(strong),
                        modifier = Modifier.size(24.dp).alpha(boxAlpha)
                    )
                }
                if (row.crowned) {
                    Image(
                        painter = painterResource(Res.drawable.crown_a),
                        contentDescription = "Crowned",
                        colorFilter = ColorFilter.tint(CrownGold),
                        modifier = Modifier.align(Alignment.TopEnd).size(width = 18.dp, height = 14.dp)
                    )
                }
            }
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
                    // A done title fades under a thin strike, so a mostly done list still reads calm.
                    modifier = Modifier.alpha(if (ticked) 0.55f else 1f).drawWithContent {
                        drawContent()
                        val layout = titleLayout
                        if (ticked && layout != null) {
                            for (line in 0 until layout.lineCount) {
                                val y = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2
                                drawLine(
                                    strong.copy(alpha = 0.6f), Offset(layout.getLineLeft(line), y), Offset(layout.getLineRight(line), y),
                                    strokeWidth = 1.2.dp.toPx()
                                )
                            }
                        }
                    }
                )
                Text(
                    subline(row), fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, lineHeight = 17.sp,
                    modifier = Modifier.alpha(if (ticked) 0.55f else 1f)
                )
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
        if (shownX < 0f) {
            val width = with(LocalDensity.current) { (-shownX).toDp() }
            SlideActions(
                row, chips, actionsWidth, actions, onDone = { slideX = 0f },
                modifier = Modifier.align(Alignment.CenterEnd).width(width).clipToBounds()
            )
        }
      }
    }
}

/**
 * Reset, Move and Skip, uncovered from the right as an item slides left. They keep their full width
 * while only part shows, so the labels don't squash. No day to name means the mission can't go anywhere
 * this week, so Move is left out rather than skipping it quietly.
 */
@Composable
private fun SlideActions(
    row: PlanRow, chips: List<String>, width: Dp,
    actions: PlanRowActions, onDone: () -> Unit, modifier: Modifier,
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true).width(width).padding(start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            chips.forEach { chip ->
                val weight = chipWidth(chip).value
                when (chip) {
                    "reset" -> EveningButton("Reset", weight, Icons.Default.Refresh) { onDone(); actions.onProgress(row.taskId, 0) }
                    "move" -> EveningButton("→ ${shortDay(row.moveTo.orEmpty())}", weight) { onDone(); actions.onMove(row.taskId) }
                    "remove" -> EveningButton("Remove", weight, Icons.Default.Delete) { onDone(); actions.onRemove(row.taskId) }
                    else -> EveningButton("Skip", weight) { onDone(); actions.onSkip(row.taskId) }
                }
            }
        }
    }
}

/** A slide chip: [text] as its label, or, with an [icon], as what a screen reader says. */
@Composable
private fun RowScope.EveningButton(text: String, weight: Float, icon: ImageVector? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier.weight(weight).heightIn(min = 40.dp).clip(shape)
            .border(1.dp, ChipBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = text, tint = Color(0xFF4A4A4A), modifier = Modifier.size(20.dp))
        } else {
            Text(text, fontSize = 13.sp, color = Color(0xFF4A4A4A), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** "next Thursday" → "Next Thu", "today" → "Today". */
private fun shortDay(day: String): String =
    day.split(" ").joinToString(" ") { if (it.first().isUpperCase()) it.take(3) else it }
        .replaceFirstChar { it.uppercase() }

private fun subline(row: PlanRow): String = when (row.state) {
    TaskState.MOVED -> "Moved to ${row.movedTo ?: "another day"}" + if (row.canTakeBack) " · tick to take it back" else ""
    TaskState.SKIPPED -> "Skipped today"
    TaskState.PARTIAL -> "Did some · ${row.progress}%"
    else -> {
        val ticked = if (!row.tickedForYou) null else if (row.reason.isEmpty()) "Ticked for you" else "ticked for you"
        listOfNotNull(row.reason.takeIf { it.isNotEmpty() }, ticked).joinToString(" · ")
    }
}

/**
 * The app's small rounded chip, at least 44dp tall: an action ("Skip today", "All months", "Done") or one
 * choice among a few (the energy picker, a mission's pace, the All months filters). A choice passes
 * [selected], filled while it's on, and a screen reader hears whether it is; an action leaves it null, so
 * it isn't presented as something that can be selected. [modifier] adds to the chip's own node, such as
 * a state description.
 */
@Composable
fun SmallChip(text: String, selected: Boolean? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val on = selected == true
    Box(
        modifier = modifier.heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onClick)
            .then(if (selected != null) Modifier.semantics { this.selected = selected } else Modifier)
            .padding(end = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 13.sp, color = Color(0xFF4A4A4A),
            modifier = Modifier.clip(shape)
                .border(1.dp, if (on) Orange180 else ChipBorder, shape)
                .background(if (on) Orange80 else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}
