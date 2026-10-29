package com.example.todoapp.screen.missions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.di.KoinF
import com.example.todoapp.domain.MissionWords
import com.example.todoapp.reminders.ReminderSync
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.Orange80
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.components.MarkerHeading
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarLight
import com.example.todoapp.screen.diary.plan.StickyNote
import com.example.todoapp.screen.missions.blueprint.BlueprintScreen
import com.example.todoapp.screen.missions.blueprint.BlueprintStrip
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.flag_open
import todoapp.shared.generated.resources.plus_sign

/**
 * Spec 9: missions per pillar under marker headings, as tilted sticky notes. + opens the sentence flow.
 * [listState] comes from the caller, so the scroll position survives the editor.
 */
@Composable
fun MissionsScreen(viewModel: MissionsViewModel, listState: LazyListState) {
    val state by viewModel.state.collectAsState()
    val notice by viewModel.notice.collectAsState()
    var showFreeTime by remember { mutableStateOf(false) }
    var showReminders by remember { mutableStateOf(false) }
    var showBackup by remember { mutableStateOf(false) }
    val backupNote by viewModel.backupNote.collectAsState()
    val pendingRestore by viewModel.pendingRestore.collectAsState()
    val backupFiles = rememberBackupFiles(onSaved = viewModel::backupSaved, onOpened = viewModel::readBackup)
    val reminders by viewModel.reminders.collectAsState()
    val blocks by viewModel.blocks.collectAsState()
    val blueprint by viewModel.blueprint.collectAsState()
    var showBlueprint by remember { mutableStateOf(false) }
    val s = state

    Box(modifier = Modifier.fillMaxSize().background(Light_Yellowww)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "title") {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 30.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Missions", fontSize = 30.sp, fontStyle = FontStyle.Italic, letterSpacing = 0.5.sp, color = InkGray,
                            modifier = Modifier.weight(1f)
                        )
                        // Up here rather than floating, so it never covers a card.
                        AddButton { viewModel.openNew() }
                        if (s != null) {
                            SettingsMenu(
                                freeTimeLabel = "Free time · ${MissionWords.hoursNumber(s.weeklyFreeMinutes)} h a week",
                                onFreeTime = { showFreeTime = true },
                                onReminders = {
                                    // Asked here or after the first mission, not when the app first opens.
                                    KoinF.di?.get<ReminderSync>()?.askPermission()
                                    showReminders = true
                                },
                                onBackup = {
                                    viewModel.clearBackupNote()
                                    showBackup = true
                                },
                            )
                        }
                    }
                    if (s != null) {
                        Text(s.summary, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, modifier = Modifier.padding(top = 4.dp, end = 8.dp))
                        notice?.let { text ->
                            Text(
                                text, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
                                modifier = Modifier.padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                    }
                }
            }
            if (s != null && s.isEmpty) {
                item(key = "empty") {
                    StickyNote(rotation = -1.2f) {
                        Text(
                            "Nothing here yet. Tap + and write down one thing you want to make room for. " +
                                "Each morning the diary picks from your missions.",
                            fontSize = 16.sp, fontStyle = FontStyle.Italic, lineHeight = 23.sp, color = InkBlack
                        )
                    }
                }
            }
            val plan = blueprint
            if (s != null && !s.isEmpty && plan != null) {
                item(key = "blueprint") { BlueprintStrip(plan) { showBlueprint = true } }
            }
            if (s != null && !s.isEmpty) {
                s.groups.forEach { group ->
                    item(key = "heading-${group.pillar.name}") {
                        MarkerHeading(group.heading, pillarLight(group.pillar), Modifier.padding(top = 24.dp, bottom = 8.dp))
                    }
                    if (group.cards.isEmpty()) {
                        item(key = "none-${group.pillar.name}") {
                            Text(
                                "Nothing here yet.",
                                fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                        }
                    }
                    group.cards.chunked(2).forEach { row ->
                        item(key = "cards-${row.first().missionId}") {
                            // Both cards in a row are as tall as the taller one.
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)
                                    .padding(horizontal = 18.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                row.forEach { card ->
                                    MissionCard(card, Modifier.weight(1f).fillMaxHeight()) { viewModel.openEdit(card.missionId) }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item(key = "about") { AboutLinks() }
            item(key = "bottom-space") { Spacer(Modifier.height(24.dp)) }
        }
        val plan = blueprint
        if (showBlueprint && plan != null) BlueprintScreen(plan, onClose = { showBlueprint = false })
    }

    val r = reminders
    if (showReminders && r != null) {
        RemindersDialog(
            settings = r,
            blocks = blocks,
            onSave = {
                showReminders = false
                viewModel.saveReminders(it)
            },
            onDismiss = { showReminders = false }
        )
    }
    if (showBackup) {
        BackupDialog(
            note = backupNote,
            onSave = { viewModel.exportBackup(backupFiles::save) },
            onRestore = backupFiles::open,
            onDismiss = { showBackup = false }
        )
    }
    pendingRestore?.let { backup ->
        RestoreConfirmDialog(backup, onConfirm = viewModel::confirmRestore, onDismiss = viewModel::cancelRestore)
    }
    if (showFreeTime && s != null) {
        FreeTimeDialog(
            minutesByWeekday = s.freeMinutes,
            onSave = {
                showFreeTime = false
                viewModel.saveFreeTime(it)
            },
            onDismiss = { showFreeTime = false }
        )
    }
}

/** A tilted sticky note: pillar band, title, pace and weight, and a status line. */
@Composable
private fun MissionCard(card: MissionCardRow, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier
            .rotate(card.tilt)
            .shadow(6.dp, shape)
            .clip(shape)
            .background(Color.White)
            .clickable(role = Role.Button, onClickLabel = "Edit ${card.title}") { onClick() }
            .heightIn(min = 92.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(pillarLight(card.pillar)))
        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(card.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = InkBlack, modifier = Modifier.weight(1f))
                if (card.finishLine) {
                    Image(
                        painterResource(Res.drawable.flag_open), contentDescription = "Finish line",
                        colorFilter = ColorFilter.tint(pillarInk(card.pillar)), modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(card.cadence, fontSize = 12.sp, lineHeight = 16.sp, color = InkGray, modifier = Modifier.padding(top = 4.dp))
            Text(
                card.status, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = pillarInk(card.pillar),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(40.dp).shadow(3.dp, shape).clip(shape).background(Orange80)
                .clickable(role = Role.Button, onClickLabel = "Add a mission") { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painterResource(Res.drawable.plus_sign), contentDescription = "Add a mission",
                colorFilter = ColorFilter.tint(PeachText), modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** The gear: free time, reminders and backup, which are settings rather than missions. */
@Composable
private fun SettingsMenu(freeTimeLabel: String, onFreeTime: () -> Unit, onReminders: () -> Unit, onBackup: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = InkGray)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Color.White) {
            listOf(freeTimeLabel to onFreeTime, "Reminders" to onReminders, "Backup" to onBackup).forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(label, fontSize = 15.sp, color = InkBlack) },
                    onClick = {
                        open = false
                        action()
                    }
                )
            }
        }
    }
}

/** The stores want the privacy policy reachable from inside the app. */
@Composable
private fun AboutLinks() {
    val uri = LocalUriHandler.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        listOf("Privacy policy" to "https://ideeya.com/inbalance/privacy/", "Help" to "https://ideeya.com/inbalance/")
            .forEach { (label, url) ->
                Text(
                    label, fontSize = 13.sp, color = InkGray, textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(role = Role.Button) { uri.openUri(url) }.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
    }
}
