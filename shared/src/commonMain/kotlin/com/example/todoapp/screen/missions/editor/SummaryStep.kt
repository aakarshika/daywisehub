package com.example.todoapp.screen.missions.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.EditorStep
import com.example.todoapp.domain.MissionDraft
import com.example.todoapp.domain.MissionSentence
import com.example.todoapp.domain.WeekCost
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.components.MarkerHeading
import com.example.todoapp.screen.diary.plan.PlanButton
import com.example.todoapp.screen.diary.plan.StickyNote
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.wand

/** Spec 4.2, before saving: the whole sentence, gentle start, the week's cost, then Make it lighter and Save. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryStep(
    state: EditorUiState,
    cost: WeekCost?,
    onTap: (EditorStep) -> Unit,
    onGentle: (Boolean) -> Unit,
    onLighter: () -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit,
) {
    val draft = state.draft
    SentenceText(
        MissionSentence.parts(draft, state.answered, current = null),
        highlight = sentenceHighlight(draft),
        onTap = onTap,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp)
    )
    Text(
        "Tap any highlighted words to change them.",
        fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp)
    )
    if (draft.gentleEligible) GentleNote(draft, state.isNew, onGentle)
    MarkerHeading("YOUR WEEK", Blue80, Modifier.padding(top = 22.dp))
    if (cost != null) WeekCostView(cost, draft.pillar)
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlanButton("Make it lighter", filled = false, onClick = onLighter)
        PlanButton(if (state.saving && !state.archiving) "Saving…" else "Save mission", filled = true, onClick = onSave)
    }
    // Save does nothing while the draft can't be saved; say why.
    val blocker = draft.blocker
    if (!state.complete && blocker != null) {
        Text(
            blocker,
            fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 16.dp)
        )
    }
    if (state.failure != null) {
        Text(
            state.failure,
            fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 16.dp)
        )
    }
    if (!state.isNew) {
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.heightIn(min = 44.dp).clickable(role = Role.Button) { onArchive() }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Archive this mission", fontSize = 14.sp, color = PeachText, textDecoration = TextDecoration.Underline)
            }
        }
    }
}

/** The gentle-start sticky: shows the choice and lets it flip without leaving the screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GentleNote(draft: MissionDraft, isNew: Boolean, onGentle: (Boolean) -> Unit) {
    StickyNote(rotation = -1f) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(Res.drawable.wand), contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(0xFF6A5A2A)), modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (isNew) "IT'S NEW. START GENTLY?" else "START GENTLY?",
                    fontSize = 11.sp, letterSpacing = 1.2.sp, color = Color(0xFF6A5A2A)
                )
            }
            Text(
                if (draft.gentleOn) {
                    "${draft.currentCount}× a week to begin, then ${draft.count}× after 3 good weeks."
                } else {
                    "Straight to ${draft.count}× a week from the start."
                },
                fontSize = 16.sp, fontStyle = FontStyle.Italic, lineHeight = 23.sp, color = InkBlack,
                modifier = Modifier.padding(top = 6.dp)
            )
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlanButton("Start gently", filled = draft.gentleOn, onClick = { onGentle(true) })
                PlanButton("Go straight to ${draft.count}×", filled = !draft.gentleOn, onClick = { onGentle(false) })
            }
        }
    }
}
