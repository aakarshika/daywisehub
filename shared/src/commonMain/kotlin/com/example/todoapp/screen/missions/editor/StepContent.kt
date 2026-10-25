package com.example.todoapp.screen.missions.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.EditorStep
import com.example.todoapp.domain.Effort
import com.example.todoapp.domain.IfMissed
import com.example.todoapp.domain.MissionDrafts
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.pillarStrong
import com.example.todoapp.screen.diary.plan.SmallChip
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.check_square
import todoapp.shared.generated.resources.flag_open
import todoapp.shared.generated.resources.heart_filled
import todoapp.shared.generated.resources.metrics_calendar
import todoapp.shared.generated.resources.star_border

/** The question written on the marker heading for [step]. */
fun questionFor(step: EditorStep, state: EditorUiState): String = when (step) {
    EditorStep.TITLE -> "WHAT DO YOU WANT TO MAKE ROOM FOR?"
    EditorStep.PILLAR -> "WHICH PART OF YOUR LIFE DOES IT FEED?"
    EditorStep.KIND -> "WHAT KIND OF THING IS IT?"
    EditorStep.PACE -> when (state.draft.kind) {
        MissionKind.FINISH_LINE -> "BY WHEN? HOW WILL YOU KNOW YOU'RE DONE?"
        MissionKind.BLOCK -> "WHICH DAYS, AND WHEN?"
        MissionKind.RHYTHM -> "EVERY HOW LONG?"
        MissionKind.RECHARGE -> "HOW OFTEN DO YOU NEED IT?"
        else -> "ABOUT HOW OFTEN?"
    }
    EditorStep.DURATION -> "EACH TIME TAKES ABOUT…"
    EditorStep.EFFORT -> "HOW DOES IT FEEL TO DO?"
    EditorStep.WEIGHT -> "HOW BIG A PART OF YOUR ${state.draft.pillar.title.uppercase()} WEEK IS IT?"
    EditorStep.IF_MISSED -> "IF A DAY GOES SIDEWAYS…"
    EditorStep.GENTLE -> "IT'S NEW. START GENTLY?"
}

/** The answers for [step]. A choice shows as picked only once its question has an answer. */
@Composable
fun StepQuestion(step: EditorStep, state: EditorUiState, viewModel: MissionEditorViewModel) {
    val draft = state.draft
    val answered = step in state.answered
    when (step) {
        EditorStep.TITLE -> TitleField(draft.title, viewModel::setTitle, viewModel::confirmTitle)
        EditorStep.PILLAR -> PillarType.scoredPillars.forEachIndexed { i, pillar ->
            ChoiceCard(
                pillar.title, pillarExample(pillar), answered && draft.pillarChoice == pillar, cardTilts[i % cardTilts.size],
                onClick = { viewModel.pickPillar(pillar) }, accent = pillarStrong(pillar)
            )
        }
        EditorStep.KIND -> MissionKind.entries.forEachIndexed { i, kind ->
            val (title, example, icon) = when (kind) {
                MissionKind.HABIT -> Triple("Something I keep doing", "like the gym, or guitar", Res.drawable.check_square)
                MissionKind.FINISH_LINE -> Triple("Something with a finish line", "like a dance routine by spring", Res.drawable.flag_open)
                MissionKind.BLOCK -> Triple("A fixed block in my day", "like your job, on weekdays", Res.drawable.metrics_calendar)
                MissionKind.RHYTHM -> Triple("Something I do every so often", "like calling Grandma, or a trip every 3 months", Res.drawable.star_border)
                MissionKind.RECHARGE -> Triple("Something that recharges me", "like a new episode. No points, never missed.", Res.drawable.heart_filled)
            }
            ChoiceCard(
                title, example, answered && draft.kind == kind, cardTilts[i % cardTilts.size],
                onClick = { viewModel.pickKind(kind) }, icon = icon
            )
        }
        EditorStep.PACE -> PaceStep(state, viewModel)
        EditorStep.DURATION -> DurationChoices(draft.durationMinutes, answered, viewModel::pickDuration)
        EditorStep.EFFORT -> Effort.entries.forEachIndexed { i, effort ->
            val example = when (effort) {
                Effort.LIGHT -> "easy to fit in, even on a tired day"
                Effort.STEADY -> "takes some focus"
                Effort.DRAINING -> "needs a good day. Left out on low-energy days."
            }
            ChoiceCard(
                MissionWords.effortWord(effort).replaceFirstChar { it.uppercase() }, example,
                answered && draft.effort == effort, cardTilts[i % cardTilts.size], onClick = { viewModel.pickEffort(effort) }
            )
        }
        EditorStep.WEIGHT -> (1..3).forEach { weight ->
            ChoiceCard(
                MissionWords.weightWord(weight).replaceFirstChar { it.uppercase() },
                "${MissionWords.pointsPhrase(weight)} each time",
                answered && draft.weight == weight, cardTilts[(weight - 1) % cardTilts.size],
                onClick = { viewModel.pickWeight(weight) }, accent = pillarStrong(draft.pillar)
            )
        }
        EditorStep.IF_MISSED -> IfMissed.entries.forEachIndexed { i, rule ->
            val (title, example) = when (rule) {
                IfMissed.MOVE -> "Move it to another day" to "it goes to the next day with room"
                IfMissed.SAME_DAY -> "It has to be that day" to "if it doesn't happen, it simply drops"
                IfMissed.SKIP -> "Skip it, no catch-up" to "nothing to catch up on"
            }
            ChoiceCard(title, example, answered && draft.ifMissed == rule, cardTilts[i % cardTilts.size], onClick = { viewModel.pickIfMissed(rule) })
        }
        EditorStep.GENTLE -> {
            val start = MissionDrafts.gentleStart(draft.count)
            ChoiceCard(
                "Start at $start× a week", "and step up to ${draft.count}× after 3 good weeks",
                answered && draft.gentle, cardTilts[0], onClick = { viewModel.pickGentle(true) }
            )
            ChoiceCard(
                "Go straight to ${draft.count}× a week", "the full pace from the start",
                answered && !draft.gentle, cardTilts[1], onClick = { viewModel.pickGentle(false) }
            )
        }
    }
}

private fun pillarExample(pillar: PillarType): String = when (pillar) {
    PillarType.HEALTH -> "like the gym, sleep or a walk"
    PillarType.WEALTH -> "like your job, a side project or money"
    PillarType.LOVE -> "like family, friends or a partner"
    PillarType.LIFE -> "like learning, making things or travel"
    PillarType.RECHARGE -> "rest that keeps the rest going"
}

/** Step 1, written on a ruled line. */
@Composable
private fun TitleField(title: String, onChange: (String) -> Unit, onNext: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        value = title,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(
            fontSize = 22.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light,
            letterSpacing = 0.4.sp, color = InkBlack
        ),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        cursorBrush = SolidColor(PeachText),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp).focusRequester(focus),
        decorationBox = { inner ->
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).drawBehind {
                    drawLine(Blue80, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                },
                contentAlignment = Alignment.CenterStart
            ) {
                if (title.isEmpty()) {
                    Text(
                        "Learn guitar", fontSize = 22.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light,
                        color = InkGray.copy(alpha = 0.6f)
                    )
                }
                inner()
            }
        }
    )
}

/** Step 5: the spec's choices, plus a saved length that isn't one of them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationChoices(current: Int, answered: Boolean, onPick: (Int) -> Unit) {
    val standard = MissionDrafts.durationChoices
    val choices = if (current in standard || current > MissionWords.FULL_DAY_MINUTES) standard else (standard + current).sorted()
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        choices.forEach { minutes ->
            SmallChip(
                MissionWords.durationWords(minutes).replaceFirstChar { it.uppercase() },
                selected = answered && minutes == current
            ) { onPick(minutes) }
        }
    }
}
