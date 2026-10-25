package com.example.todoapp.screen.missions.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.EditorStep
import com.example.todoapp.domain.MissionDraft
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.SentencePart
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.CardBorder
import com.example.todoapp.screen.basicutils.ChipBorder
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Orange180
import com.example.todoapp.screen.basicutils.Orange80
import com.example.todoapp.screen.basicutils.PeachInk
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.TrackBeige
import com.example.todoapp.screen.basicutils.Yellow80
import com.example.todoapp.screen.basicutils.pillarLight
import com.example.todoapp.screen.diary.plan.PlanButton
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs

private const val STEP_TAG = "step"
private const val BLANK_TAG = "blank"
private const val SENTENCE_FONT = 22
private const val SENTENCE_LINE = 44

/** Tilts for stacked cards, as on the mockups. */
internal val cardTilts = listOf(-0.7f, 0.6f, -0.4f, 0.7f, -0.5f)

/** The pillar's light color once a pillar is chosen; a neutral marker before. */
fun sentenceHighlight(draft: MissionDraft): Color =
    if (draft.kind == MissionKind.RECHARGE || draft.pillarChoice != null) pillarLight(draft.pillar) else Yellow80

/**
 * The mission sentence on ruled diary lines. Answers get a marker highlight; the blank being asked
 * gets a dashed line. With [onTap], tapping an answer reports its step. Lines are 44 tall, so each
 * highlighted answer is a 44dp target.
 */
@Composable
fun SentenceText(
    parts: List<SentencePart>,
    highlight: Color,
    modifier: Modifier = Modifier,
    onTap: ((EditorStep) -> Unit)? = null,
) {
    val text = remember(parts) {
        buildAnnotatedString {
            parts.forEach { part ->
                val step = part.step
                when {
                    part.placeholder -> {
                        pushStringAnnotation(BLANK_TAG, step?.name.orEmpty())
                        append(" ".repeat(12))
                        pop()
                    }
                    step != null -> {
                        pushStringAnnotation(STEP_TAG, step.name)
                        append(part.text)
                        pop()
                    }
                    else -> append(part.text)
                }
            }
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentOnTap by rememberUpdatedState(onTap)
    val tapModifier = if (onTap == null) Modifier else Modifier.pointerInput(text) {
        detectTapGestures { position ->
            val l = layout ?: return@detectTapGestures
            val offset = l.getOffsetForPosition(position)
            val name = text.getStringAnnotations(STEP_TAG, offset, offset).firstOrNull()?.item
            EditorStep.entries.firstOrNull { it.name == name }?.let { currentOnTap?.invoke(it) }
        }
    }
    Text(
        text,
        fontSize = SENTENCE_FONT.sp,
        lineHeight = SENTENCE_LINE.sp,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Light,
        letterSpacing = 0.4.sp,
        color = InkBlack,
        onTextLayout = { layout = it },
        modifier = modifier.fillMaxWidth()
            .heightIn(min = (SENTENCE_LINE * 3).dp)
            .drawBehind {
                val l = layout ?: return@drawBehind
                val stroke = 1.dp.toPx()
                var y = 0f
                for (line in 0 until l.lineCount) {
                    y = l.getLineBottom(line)
                    drawLine(Blue80, Offset(0f, y), Offset(size.width, y), stroke)
                }
                val step = SENTENCE_LINE.sp.toPx()
                y += step
                while (y <= size.height) {
                    drawLine(Blue80, Offset(0f, y), Offset(size.width, y), stroke)
                    y += step
                }
                val fontPx = SENTENCE_FONT.sp.toPx()
                text.getStringAnnotations(STEP_TAG, 0, text.length).forEach { marker(l, it.start, it.end, highlight, fontPx) }
                text.getStringAnnotations(BLANK_TAG, 0, text.length).forEach { blank(l, it.start, it.end) }
            }
            .then(tapModifier)
    )
}

/** A marker swipe behind [start, end), line by line, around the letters rather than the whole line box. */
private fun DrawScope.marker(l: TextLayoutResult, start: Int, end: Int, color: Color, fontPx: Float) {
    if (end <= start) return
    val pad = 3.dp.toPx()
    for (line in l.getLineForOffset(start)..l.getLineForOffset(end - 1)) {
        val from = maxOf(start, l.getLineStart(line))
        val to = minOf(end, l.getLineEnd(line, visibleEnd = true))
        if (to <= from) continue
        val left = l.getBoundingBox(from).left
        val right = l.getBoundingBox(to - 1).right
        val baseline = l.getLineBaseline(line)
        drawRoundRect(
            color,
            topLeft = Offset(minOf(left, right) - pad, baseline - fontPx * 0.8f),
            size = Size(abs(right - left) + 2 * pad, fontPx * 1.05f),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
    }
}

/** The dashed blank for the question being asked. It never wraps: it's made of no-break spaces. */
private fun DrawScope.blank(l: TextLayoutResult, start: Int, end: Int) {
    if (end <= start) return
    val line = l.getLineForOffset(start)
    val left = l.getBoundingBox(start).left
    val right = l.getBoundingBox(end - 1).right
    val y = l.getLineBaseline(line) + 4.dp.toPx()
    drawLine(
        Orange180, Offset(minOf(left, right), y), Offset(maxOf(left, right), y),
        strokeWidth = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))
    )
}

/** Close, "New mission", "3 of 9". */
@Composable
fun EditorHeader(title: String, progress: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clickable(role = Role.Button) { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = InkGray)
        }
        Text(
            title,
            fontSize = 20.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(progress, fontSize = 13.sp, color = InkGray, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 44.dp))
    }
}

/** One segment per question that applies; filled up to [done]. */
@Composable
fun StepProgress(total: Int, done: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(total) { i ->
            Box(
                modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (i < done) Orange180 else TrackBeige)
            )
        }
    }
}

/** A tilted white answer card, as on the kind step of the mockups. */
@Composable
fun ChoiceCard(
    title: String,
    example: String?,
    selected: Boolean,
    tilt: Float,
    onClick: () -> Unit,
    icon: DrawableResource? = null,
    accent: Color? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .rotate(tilt)
            .shadow(3.dp, shape)
            .clip(shape)
            .background(Color.White)
            .border(if (selected) 2.dp else 1.dp, if (selected) Orange180 else CardBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (accent != null) {
            Box(modifier = Modifier.size(width = 6.dp, height = 34.dp).clip(RoundedCornerShape(3.dp)).background(accent))
            Spacer(Modifier.width(14.dp))
        }
        if (icon != null) {
            Image(
                painterResource(icon), contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFF6B6B6B)), modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, color = InkBlack)
            if (example != null) Text(example, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray)
        }
    }
}

/** Small grey label above a group of controls. */
@Composable
fun FieldLabel(text: String) {
    Text(
        text, fontSize = 13.sp, color = InkGray,
        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 4.dp)
    )
}

/** Monday-to-Sunday toggles, 44dp each. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayToggles(selected: Set<Int>, onToggle: (Int) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (1..7).forEach { day ->
            val on = day in selected
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .border(1.dp, if (on) Orange180 else ChipBorder, CircleShape)
                    .background(if (on) Orange80 else Color.Transparent)
                    .clickable(role = Role.Checkbox) { onToggle(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(MissionWords.shortDay(day), fontSize = 12.sp, color = if (on) PeachInk else Color(0xFF4A4A4A))
            }
        }
    }
}

/** "From  (−)  9:00  (+)". */
@Composable
fun Stepper(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = InkGray, modifier = Modifier.width(64.dp))
        StepButton("−", "$label earlier or less", minusEnabled, onMinus)
        Text(
            value, fontSize = 18.sp, fontStyle = FontStyle.Italic, color = InkBlack, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        StepButton("+", "$label later or more", plusEnabled, onPlus)
    }
}

@Composable
private fun StepButton(symbol: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape)
            .border(1.5.dp, if (enabled) Orange180 else ChipBorder, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 20.sp, color = if (enabled) PeachText else ChipBorder)
    }
}

/** Back, the reassurance line, and Next when the step needs one and it can go on. */
@Composable
fun StepFooter(onBack: () -> Unit, onNext: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.heightIn(min = 44.dp).clickable(role = Role.Button) { onBack() }.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Back", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PeachText)
        }
        Text(
            "You can change any of this later.",
            fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        if (onNext != null) PlanButton("Next", filled = true, onClick = onNext)
    }
}
