package com.example.todoapp.screen.diary

import KottieAnimation
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.screen.basicutils.Blue80
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.Orange80
import com.example.todoapp.screen.basicutils.Red80
import com.example.todoapp.screen.basicutils.Yellow180
import com.example.todoapp.screen.diary.diaryitem.components.DiaryMilestoneSection
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeViewModel
import com.example.todoapp.screen.diary.plan.AddMissionDialog
import com.example.todoapp.screen.diary.plan.DayCapacityDialog
import com.example.todoapp.screen.diary.plan.DayHighlightsCard
import com.example.todoapp.screen.diary.plan.DidntFitNote
import com.example.todoapp.screen.diary.plan.PillarWeekStrip
import com.example.todoapp.screen.diary.plan.PlanActions
import com.example.todoapp.screen.diary.plan.PlanRowActions
import com.example.todoapp.screen.diary.plan.PlanSection
import com.example.todoapp.screen.diary.plan.PlanSubline
import kotlinx.datetime.LocalDate
import kottieAnimationState.KottieAnimationState
import kottieComposition.KottieCompositionSpec
import kottieComposition.animateKottieCompositionAsState
import kottieComposition.rememberKottieComposition
import org.jetbrains.compose.resources.ExperimentalResourceApi
import todoapp.shared.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DiaryScreen(
    diaryViewModel: DiaryViewModel,
    moodViewModel: MoodViewModel,
    waterIntakeViewModel: WaterIntakeViewModel,
    selection: LocalDate,
    changeDate: (LocalDate) -> Unit
) {
    val state by diaryViewModel.state.collectAsState()
    val missions by diaryViewModel.missions.collectAsState()
    val milestones by diaryViewModel.milestones.collectAsState()
    val todayMoodList: List<TodayMoodWithDetails>? by moodViewModel.todayMoodStatuses.collectAsState(emptyList())
    val waterIntake: WaterIntake? by waterIntakeViewModel.waterIntake.collectAsState()

    val evening by diaryViewModel.eveningOpen.collectAsState()
    val notice by diaryViewModel.notice.collectAsState()
    var editMode by remember(selection) { mutableStateOf(false) }
    var showCapacity by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }

    var confettiJson by remember { mutableStateOf("") }
    var waterJson by remember { mutableStateOf("") }
    var confettiPlaying by remember { mutableStateOf(false) }
    var waterPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(selection) {
        moodViewModel.loadDailyMoodStatus(selection)
        waterIntakeViewModel.loadWaterIntake(selection)
    }
    LaunchedEffect(Unit) {
        confettiJson = Res.readBytes("files/confetti-big-animation.json").decodeToString()
        waterJson = Res.readBytes("files/water-bubbles-animation.json").decodeToString()
    }
    // Confetti when the last open pick gets done in this session, not when opening a finished day.
    var lastDone by remember(selection) { mutableStateOf<Int?>(null) }
    var wasAllDone by remember(selection) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(state?.allPicksDone, state?.donePicks) {
        val s = state ?: return@LaunchedEffect
        if (wasAllDone == false && s.allPicksDone && (lastDone ?: 0) < s.donePicks) confettiPlaying = true
        wasAllDone = s.allPicksDone
        lastDone = s.donePicks
    }

    val confetti = rememberKottieComposition(spec = KottieCompositionSpec.File(confettiJson))
    val confettiState by animateKottieCompositionAsState(composition = confetti, speed = 2.5f, isPlaying = confettiPlaying)
    val water = rememberKottieComposition(spec = KottieCompositionSpec.File(waterJson))
    val waterState by animateKottieCompositionAsState(composition = water, speed = 2.5f, isPlaying = waterPlaying)
    LaunchedEffect(confettiState.isCompleted) { if (confettiState.isCompleted) confettiPlaying = false }
    LaunchedEffect(waterState.isCompleted) { if (waterState.isCompleted) waterPlaying = false }

    val actions = remember(diaryViewModel) {
        PlanRowActions(
            onToggle = diaryViewModel::toggleDone,
            onCrown = diaryViewModel::toggleCrown,
            onSwap = diaryViewModel::swap,
            onRemove = diaryViewModel::removeTask,
            onDidSome = diaryViewModel::didSome,
            onMove = diaryViewModel::moveTask,
            onSkip = diaryViewModel::skipTask,
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Light_Yellowww)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            DateHeaderItem(selection, changeDate = changeDate)
            state?.let { s -> item(key = "week-strip") { PillarWeekStrip(s.pillars) } }
            HeadingText("MOOD", Red80)
            MoodHeaderItem(todayMoodList, selection, upsertMood = { moodViewModel.upsertTodayMood(it) })
            ToDoListHeader("TO DO LIST", Orange80, editClicked = { showAdd = true })
            state?.let { s ->
                if (s.hasPlan && diaryViewModel.isToday && !evening) {
                    item(key = "plan-subline") { PlanSubline(s, onChange = { showCapacity = true }) }
                }
                item(key = "plan-section") {
                    PlanSection(
                        s, isToday = diaryViewModel.isToday, evening = evening, editMode = editMode, actions = actions,
                        onToggleEvening = {
                            editMode = false
                            diaryViewModel.toggleEveningCheck()
                        }
                    )
                }
                notice?.let { text ->
                    item(key = "notice") {
                        Text(
                            text,
                            fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                }
                s.didntFit?.let { row -> item(key = "didnt-fit") { DidntFitNote(row) } }
                if (diaryViewModel.isToday && !evening) {
                    item(key = "plan-actions") {
                        PlanActions(
                            editMode = editMode,
                            showLooksGood = !s.confirmed,
                            onEdit = { editMode = !editMode },
                            onLighter = { diaryViewModel.lighterDay() },
                            onLooksGood = {
                                editMode = false
                                diaryViewModel.confirm()
                            }
                        )
                    }
                }
                if (evening && (s.hasPlan || s.picks.isNotEmpty() || s.fixed.isNotEmpty())) {
                    item(key = "highlights") { DayHighlightsCard(s, selection) }
                }
            }
            HeadingText("WATER INTAKE", Blue80)
            WaterIntakeHeaderItem(
                waterIntake, selection,
                upsertWaterIntake = { waterIntakeViewModel.upsertWaterIntake(it) },
                waterIntakeCompleted = { waterPlaying = it }
            )
            if (milestones.isNotEmpty()) {
                HeadingText("MILESTONE", Orange80)
                DiaryMilestoneSection(milestones = milestones, onMilestoneChecked = { mwd, achieved ->
                    diaryViewModel.updateMilestoneStatus(mwd.milestone, achieved, selection)
                })
            }
            HeadingText("NOTES", Yellow180)
            Notes(waterIntake, selection, upsertWaterIntake = { waterIntakeViewModel.upsertWaterIntake(it) })
            item { Spacer(Modifier.height(120.dp)) }
        }
        if (confettiPlaying) AnimationConfetti(confetti, confettiState)
        if (waterPlaying) AnimationWater(water, waterState)
    }

    state?.let { s ->
        if (showCapacity) {
            DayCapacityDialog(
                freeMinutes = s.freeMinutes,
                energy = s.energy,
                onSave = { minutes, energy ->
                    showCapacity = false
                    diaryViewModel.changeDay(minutes, energy)
                },
                onDismiss = { showCapacity = false }
            )
        }
        if (showAdd) {
            AddMissionDialog(
                missions = missions,
                todayMissionIds = s.todayMissionIds,
                onAdd = { diaryViewModel.addMission(it) },
                onDismiss = { showAdd = false }
            )
        }
    }
}

@Composable
private fun AnimationWater(composition: Any?, animationState: KottieAnimationState) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent), contentAlignment = Alignment.Center) {
        KottieAnimation(
            composition = composition,
            progress = { animationState.progress },
            modifier = Modifier.fillMaxSize().background(Color.Transparent)
        )
    }
}

@Composable
private fun AnimationConfetti(composition: Any?, animationState: KottieAnimationState) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent), contentAlignment = Alignment.Center) {
        KottieAnimation(
            composition = composition,
            progress = { animationState.progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}
