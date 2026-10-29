package com.example.todoapp.screen.missions.ideas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.Idea
import com.example.todoapp.domain.Ideas
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.Orange80
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.components.MarkerHeading
import com.example.todoapp.screen.basicutils.components.TypingPlaceholder
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarLight
import com.example.todoapp.screen.diary.plan.PlanButton
import com.example.todoapp.screen.diary.plan.SmallChip
import com.example.todoapp.screen.diary.plan.StickyNote
import com.example.todoapp.screen.missions.MissionsViewModel
import com.example.todoapp.screen.missions.editor.EditorHeader
import kotlinx.coroutines.delay
import moe.tlaster.precompose.navigation.BackHandler

/**
 * Spec 8.2: the ideas library + opens. Search, "Write your own", pillar filters and themed sections.
 * + on a row adds it as is; on an idea with a blank it opens the row to fill the blank first.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdeasScreen(viewModel: MissionsViewModel) {
    val library by viewModel.library.collectAsState()
    val addedTitles by viewModel.addedTitles.collectAsState()
    val added by viewModel.added.collectAsState()
    var query by remember { mutableStateOf("") }
    var pillar by remember { mutableStateOf<PillarType?>(null) }
    var openBlank by remember { mutableStateOf<String?>(null) }

    BackHandler { viewModel.closeLibrary() }

    // The notice goes away by itself after a few seconds.
    LaunchedEffect(added) {
        val notice = added ?: return@LaunchedEffect
        delay(if (notice.explains) 9000 else 4000)
        viewModel.clearAdded(notice)
    }

    Box(modifier = Modifier.fillMaxSize().background(Light_Yellowww)) {
        Column(modifier = Modifier.fillMaxSize()) {
            EditorHeader(title = "Add a mission", progress = "", onClose = { viewModel.closeLibrary() })
            SearchField(query) { query = it }
            val lib = library
            LazyColumn(modifier = Modifier.weight(1f)) {
                item(key = "own") {
                    StickyNote(rotation = -1.2f, modifier = Modifier.clickable(role = Role.Button) { viewModel.writeOwn() }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = InkBlack, modifier = Modifier.size(48.dp))
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text("Write your own", fontSize = 17.sp, fontStyle = FontStyle.Italic, color = InkBlack)
                                Text(
                                    "Type what you want to make room for. We'll suggest a match or walk you through it.",
                                    fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
                item(key = "filters") {
                    FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                        SmallChip("All", selected = pillar == null) { pillar = null }
                        PillarType.entries.forEach { p ->
                            SmallChip(p.title, selected = pillar == p) { pillar = if (pillar == p) null else p }
                        }
                    }
                }
                if (lib == null) {
                    item(key = "loading") {
                        Text("Loading ideas…", fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
                            modifier = Modifier.padding(24.dp))
                    }
                } else {
                    val pool = lib.ideas.filter { pillar == null || it.pillar == pillar }
                    val row: @Composable (Idea) -> Unit = { idea ->
                        IdeaRow(
                            idea = idea,
                            isAdded = idea.blank == null && idea.displayTitle.lowercase() in addedTitles,
                            open = openBlank == idea.id,
                            onAdd = { if (idea.blank != null) openBlank = if (openBlank == idea.id) null else idea.id else viewModel.quickAdd(idea) },
                            onEdit = { viewModel.editIdea(idea) },
                            onAddFilled = { text -> openBlank = null; viewModel.quickAdd(idea, idea.filled(text)) },
                            onMore = { text -> viewModel.editIdea(idea, if (text.isBlank()) idea.displayTitle else idea.filled(text)) },
                        )
                    }
                    if (query.isNotBlank()) {
                        val ids = pool.map { it.id }.toSet()
                        val found = Ideas.match(query, lib, limit = Int.MAX_VALUE).map { it.idea }.filter { it.id in ids }
                        item(key = "found") {
                            MarkerHeading(if (found.isEmpty()) "NOTHING MATCHES" else "MATCHES", Blue80, Modifier.padding(top = 18.dp, bottom = 6.dp))
                        }
                        items(found, key = { "found-${it.id}" }) { row(it) }
                    } else {
                        lib.themes.forEach { theme ->
                            val inTheme = pool.filter { it.theme == theme.id }
                            if (inTheme.isEmpty()) return@forEach
                            // The heading takes the color of the pillar most of its ideas feed.
                            val main = inTheme.groupingBy { it.pillar }.eachCount().maxBy { it.value }.key
                            item(key = "theme-${theme.id}") {
                                MarkerHeading(theme.name.uppercase(), pillarLight(main), Modifier.padding(top = 18.dp, bottom = 6.dp))
                            }
                            items(inTheme, key = { it.id }) { row(it) }
                        }
                    }
                }
                item(key = "bottom-space") { Spacer(Modifier.height(90.dp)) }
            }
        }
        added?.let { notice ->
            AddedBar(notice.text, canUndo = notice.missionId != 0L, onUndo = viewModel::undoAdd,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 17.sp, fontStyle = FontStyle.Italic, color = InkBlack),
        cursorBrush = SolidColor(PeachText),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
        decorationBox = { inner ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).drawBehind {
                    drawLine(Blue80, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = InkGray, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search ideas", fontSize = 17.sp, fontStyle = FontStyle.Italic, color = InkGray.copy(alpha = 0.6f))
                    inner()
                }
            }
        }
    )
}

/** Title, pillar chip and preset line, with Edit and +. An open blank row asks for the blank first. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdeaRow(
    idea: Idea,
    isAdded: Boolean,
    open: Boolean,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onAddFilled: (String) -> Unit,
    onMore: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(idea.displayTitle, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = InkBlack)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                    Text(
                        idea.pillar.title, fontSize = 11.sp, color = pillarInk(idea.pillar),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(pillarLight(idea.pillar))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                    Text(Ideas.preset(idea), fontSize = 12.sp, color = InkGray, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text(
                "Edit", fontSize = 13.sp, color = PeachText,
                modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Edit ${idea.displayTitle}") { onEdit() }
                    .padding(horizontal = 10.dp, vertical = 12.dp)
            )
            if (isAdded) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 40.dp)) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = PeachText, modifier = Modifier.size(18.dp))
                    Text("Added", fontSize = 12.sp, fontStyle = FontStyle.Italic, color = PeachText)
                }
            } else {
                Box(
                    modifier = Modifier.size(36.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(Orange80)
                        .clickable(role = Role.Button, onClickLabel = "Add ${idea.displayTitle}") { onAdd() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = PeachText, modifier = Modifier.size(20.dp))
                }
            }
        }
        val blank = idea.blank
        if (open && blank != null) {
            var text by remember { mutableStateOf("") }
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(30) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 17.sp, fontStyle = FontStyle.Italic, color = InkBlack),
                cursorBrush = SolidColor(PeachText),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onAddFilled(text) }),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).drawBehind {
                            drawLine(Blue80, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                        },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.isEmpty()) {
                            TypingPlaceholder(
                                listOf(blank.label) + blank.examples,
                                TextStyle(fontSize = 17.sp, fontStyle = FontStyle.Italic, color = InkGray.copy(alpha = 0.6f))
                            )
                        }
                        inner()
                    }
                }
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                PlanButton("More options", filled = false) { onMore(text) }
                PlanButton("Add", filled = true) { if (text.isNotBlank()) onAddFilled(text) }
            }
        }
    }
}

@Composable
private fun AddedBar(text: String, canUndo: Boolean, onUndo: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier.fillMaxWidth().shadow(6.dp, shape).clip(shape).background(Color(0xFF3A3A3A))
            .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 14.sp, color = Color.White, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
        if (canUndo) {
            Text(
                "Undo", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Orange80,
                modifier = Modifier.clickable(role = Role.Button) { onUndo() }.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}
