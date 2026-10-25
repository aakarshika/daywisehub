package com.example.todoapp.screen.missions.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.domain.EditorStep
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.MissionSentence
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.Orange80
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.components.MarkerHeading
import moe.tlaster.precompose.navigation.BackHandler

/** The nine-question sentence and the before-saving screen, for a new or an existing mission. */
@Composable
fun MissionEditorScreen(viewModel: MissionEditorViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val cost by viewModel.cost.collectAsState()
    val finished by viewModel.finished.collectAsState()
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }

    LaunchedEffect(finished) { if (finished) onClose() }

    // Nothing leaves while a save or an archive is under way.
    fun leave() {
        if (viewModel.state.value?.saving == true) return
        if (viewModel.hasChanges()) confirmLeave = true else onClose()
    }
    BackHandler { if (!viewModel.back()) leave() }

    val s = state
    Column(modifier = Modifier.fillMaxSize().background(Light_Yellowww)) {
        if (s != null) {
            val step = s.step
            EditorHeader(
                title = if (s.isNew) "New mission" else "Edit mission",
                progress = if (step == null) "Ready" else "${s.position} of ${s.steps.size}",
                onClose = { leave() },
                // A new mission's before-saving screen goes back to its last question.
                onBack = if (step == null && s.isNew) ({ viewModel.back() }) else null
            )
            StepProgress(total = s.steps.size, done = s.position)
            // Each question starts at the top.
            val scroll = key(step) { rememberScrollState() }
            Column(modifier = Modifier.weight(1f).verticalScroll(scroll)) {
                if (step == null) {
                    SummaryStep(
                        state = s,
                        cost = cost,
                        onTap = viewModel::goTo,
                        onGentle = viewModel::setGentle,
                        onLighter = viewModel::makeLighter,
                        onSave = viewModel::save,
                        onArchive = { confirmArchive = true },
                    )
                } else {
                    SentenceText(
                        MissionSentence.parts(s.draft, s.answered, step),
                        highlight = sentenceHighlight(s.draft),
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp)
                    )
                    MarkerHeading(questionFor(step, s), Orange80, Modifier.padding(top = 18.dp, bottom = 8.dp))
                    StepQuestion(step, s, viewModel)
                    StepFooter(onBack = { if (!viewModel.back()) leave() }, onNext = nextAction(step, s, viewModel))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    val buttonColors = ButtonDefaults.textButtonColors(contentColor = PeachText)
    val isNew = s?.isNew ?: true
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; onClose() }, colors = buttonColors) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }, colors = buttonColors) {
                    Text(if (isNew) "Keep writing" else "Keep editing")
                }
            },
            title = { Text(if (isNew) "Leave this mission?" else "Leave without saving?") },
            text = { Text(if (isNew) "What you've written so far won't be kept." else "Your changes won't be kept.") },
            containerColor = Light_Yellowww
        )
    }
    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            confirmButton = {
                TextButton(onClick = { confirmArchive = false; viewModel.archive() }, colors = buttonColors) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = { confirmArchive = false }, colors = buttonColors) { Text("Keep it") }
            },
            title = { Text("Archive this mission?") },
            text = {
                Text(
                    "It leaves your missions and the diary, and stops counting toward your week. " +
                        "Anything still planned for it from today on comes off the list. There's no way to bring it back yet."
                )
            },
            containerColor = Light_Yellowww
        )
    }
}

/** Steps with more than one control confirm with Next; the rest answer with a single tap. */
private fun nextAction(step: EditorStep, state: EditorUiState, viewModel: MissionEditorViewModel): (() -> Unit)? = when (step) {
    EditorStep.TITLE -> if (state.draft.title.isNotBlank()) viewModel::confirmTitle else null
    EditorStep.PACE -> when (state.draft.kind) {
        MissionKind.RECHARGE -> null
        MissionKind.BLOCK -> if (state.draft.weekDays.isNotEmpty()) viewModel::confirmPace else null
        else -> viewModel::confirmPace
    }
    else -> null
}
