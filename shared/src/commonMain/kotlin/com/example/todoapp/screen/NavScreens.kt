package com.example.todoapp.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import com.example.todoapp.app.MyScreen
import com.example.todoapp.reminders.ReminderLinks
import com.example.todoapp.screen.basicutils.DiaryBackground
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.di.KoinF
import com.example.todoapp.screen.diary.DiaryScreen
import com.example.todoapp.screen.diary.DiaryViewModel
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeViewModel
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.metrics.MetricsScreen
import com.example.todoapp.screen.metrics.MetricsViewModel
import com.example.todoapp.screen.missions.MissionsScreen
import com.example.todoapp.screen.missions.MissionsViewModel
import com.example.todoapp.screen.missions.editor.MissionEditorScreen
import com.example.todoapp.screen.missions.editor.MissionEditorViewModel
import com.example.todoapp.screen.missions.ideas.IdeasScreen
import kotlinx.coroutines.cancel
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.acos
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.NavOptions
import moe.tlaster.precompose.navigation.PopUpTo
import moe.tlaster.precompose.navigation.rememberNavigator
import org.koin.core.parameter.parametersOf
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.diary_heart_closed
import todoapp.shared.generated.resources.metrics_calendar
import todoapp.shared.generated.resources.target

/** [startOnMissions]: no missions yet, so the app opens once on Missions with the ideas library up. */
@Composable
fun NavScreens(calDateViewModel: CalDiaryViewModel, startOnMissions: Boolean = false) {
    val navigator = rememberNavigator()
    val backStackEntry by navigator.currentEntry.collectAsState(null)
    val currentDate by calDateViewModel.currentDate.collectAsState()
    val today by calDateViewModel.today.collectAsState()

    val currentScreen = MyScreen.valueOf(
        backStackEntry?.route?.route ?: MyScreen.Diary.name
    )

    // Timers pause in the background, so the day is looked at again as soon as the app is back in front.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { calDateViewModel.checkDate() }

    fun navigateTab(screen: MyScreen) {
        // A tab opens on the current day, without waiting for the next timed check.
        calDateViewModel.checkDate()
        navigator.navigate(
            screen.name,
            NavOptions(
                launchSingleTop = true,
                popUpTo = PopUpTo(MyScreen.Diary.name, inclusive = false)
            )
        )
    }

    var openLibraryOnStart by remember { mutableStateOf(startOnMissions) }
    // On top of the diary, so back from Missions lands on today.
    LaunchedEffect(Unit) { if (startOnMissions) navigateTab(MyScreen.Missions) }
    // A tapped notification opens the diary on its day.
    val reminderDate by ReminderLinks.date.collectAsState()
    LaunchedEffect(reminderDate) {
        val date = reminderDate ?: return@LaunchedEffect
        calDateViewModel.checkDate()
        calDateViewModel.selectDate(date)
        navigateTab(MyScreen.Diary)
        ReminderLinks.consumed()
    }
    // The month widget opens the Metrics tab.
    val openMetrics by ReminderLinks.metrics.collectAsState()
    LaunchedEffect(openMetrics) {
        if (!openMetrics) return@LaunchedEffect
        navigateTab(MyScreen.Calendar)
        ReminderLinks.metricsConsumed()
    }

    Scaffold(
        // What shows behind the status bar: the page's own colour.
        containerColor = if (currentScreen == MyScreen.Diary) DiaryBackground else Light_Yellowww,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == MyScreen.Diary,
                    onClick = { if (currentScreen != MyScreen.Diary) navigateTab(MyScreen.Diary) },
                    label = { Text("Diary") },
                    icon = { Icon(DiaryTabIcon, tint = Color.Gray,
                        contentDescription = "Diary", modifier = Modifier.size(30.dp)) }
                )
                NavigationBarItem(
                    selected = currentScreen == MyScreen.Missions,
                    onClick = { if (currentScreen != MyScreen.Missions) navigateTab(MyScreen.Missions) },
                    label = { Text("Missions") },
                    icon = { Icon(MissionsTabIcon, tint = Color.Gray,
                        contentDescription = "Missions", modifier = Modifier.size(30.dp)) }
                )
                NavigationBarItem(
                    selected = currentScreen == MyScreen.Calendar,
                    onClick = { if (currentScreen != MyScreen.Calendar) navigateTab(MyScreen.Calendar) },
                    label = { Text("Metrics") },
                    icon = { Icon(MetricsTabIcon, tint = Color.Gray,
                        contentDescription = "Metrics", modifier = Modifier.size(30.dp)) }
                )
            }
        }
    ) { innerPadding ->
        // The tab bar's room counts as taken, so a screen's imePadding only adds what the keyboard covers above it.
        Box(modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)) {
            NavHost(
                navigator = navigator,
                initialRoute = MyScreen.Diary.name
            ) {
                scene(route = MyScreen.Missions.name) {
                    // A new one after midnight, so its "today" follows the date, as the Metrics tab's does.
                    var missionsDay by remember { mutableStateOf(today) }
                    val missionsViewModel = remember(missionsDay) { KoinF.di!!.get<MissionsViewModel>() }
                    DisposableEffect(missionsViewModel) {
                        onDispose { missionsViewModel.viewModelScope.cancel() }
                    }
                    LaunchedEffect(missionsViewModel) {
                        if (openLibraryOnStart) {
                            openLibraryOnStart = false
                            missionsViewModel.openNew()
                        }
                    }
                    // Held here, so the list keeps its place while the editor is open.
                    val missionsList = rememberLazyListState()
                    val editor by missionsViewModel.editor.collectAsState()
                    val libraryOpen by missionsViewModel.libraryOpen.collectAsState()
                    val target = editor
                    // The open editor lives in the view model, so a new day waits for it to close rather than
                    // dropping a draft. Each opening makes a fresh editor, which reads the date then.
                    LaunchedEffect(today, target) { if (target == null) missionsDay = today }
                    if (target == null) {
                        if (libraryOpen) IdeasScreen(missionsViewModel) else MissionsScreen(missionsViewModel, missionsList)
                    } else {
                        // A fresh editor for each opening; its saves can't be cancelled, so closing mid-save is safe.
                        val editorViewModel = remember(target) {
                            KoinF.di!!.get<MissionEditorViewModel> { parametersOf(target) }
                        }
                        DisposableEffect(editorViewModel) {
                            onDispose { editorViewModel.viewModelScope.cancel() }
                        }
                        MissionEditorScreen(editorViewModel, onClose = { missionsViewModel.closeEditor(saved = editorViewModel.finished.value) })
                    }
                }
                scene(route = MyScreen.Diary.name) {
                    // Each day is a page, one per epoch day. A sideways drag anywhere a row doesn't use it turns the page.
                    // Not saved: coming back to the tab restored the old page, and the settled page below then wrote it
                    // over the date Metrics had just picked. The view model's value is read straight, as the collected
                    // state may not have caught up with that pick yet.
                    val pagerState = remember { PagerState(currentPage = calDateViewModel.currentDate.value.toEpochDays()) { DIARY_PAGES } }
                    // A date picked elsewhere (the week strip, Metrics, midnight) turns to it.
                    LaunchedEffect(currentDate) {
                        val page = currentDate.toEpochDays()
                        if (pagerState.currentPage != page) pagerState.animateScrollToPage(page)
                    }
                    // The page the diary settles on is the date every tab shares.
                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.settledPage }.collect { calDateViewModel.selectDate(LocalDate.fromEpochDays(it)) }
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        // The days either side are ready, so the page underneath is already filled in when one turns.
                        beyondViewportPageCount = 1,
                        key = { it },
                        flingBehavior = PagerDefaults.flingBehavior(pagerState, snapPositionalThreshold = 0.25f),
                    ) { page ->
                        val date = LocalDate.fromEpochDays(page)
                        Box(Modifier.fillMaxSize().pageTurn(pagerState, page)) {
                            val diaryViewModel = remember(date) {
                                KoinF.di!!.get<DiaryViewModel> { parametersOf(date) }
                            }
                            val moodViewModel = remember(date) {
                                KoinF.di!!.get<MoodViewModel> { parametersOf(date) }
                            }
                            val waterIntakeViewModel = remember(date) {
                                KoinF.di!!.get<WaterIntakeViewModel> { parametersOf(date) }
                            }
                            // Nothing owns these view models, so stop their work when the page leaves or the diary leaves the screen.
                            DisposableEffect(diaryViewModel, moodViewModel, waterIntakeViewModel) {
                                onDispose {
                                    diaryViewModel.viewModelScope.cancel()
                                    moodViewModel.viewModelScope.cancel()
                                    waterIntakeViewModel.viewModelScope.cancel()
                                }
                            }
                            DiaryScreen(
                                diaryViewModel,
                                moodViewModel,
                                waterIntakeViewModel,
                                date,
                                changeDate = { calDateViewModel.selectDate(it) },
                                openMissions = { navigateTab(MyScreen.Missions) }
                            )
                        }
                    }
                }
                scene(route = MyScreen.Calendar.name) {
                    // A new one after midnight, so its "today" and "this week" follow the date.
                    val metricsViewModel = remember(today) { KoinF.di!!.get<MetricsViewModel>() }
                    // Nothing owns this view model either, so stop its work when the date changes or the tab leaves the screen.
                    DisposableEffect(metricsViewModel) {
                        onDispose { metricsViewModel.viewModelScope.cancel() }
                    }
                    MetricsScreen(metricsViewModel, openDay = { date ->
                        // The diary shows the date the shared CalDiaryViewModel holds.
                        calDateViewModel.selectDate(date)
                        navigateTab(MyScreen.Diary)
                    })
                }
            }
        }
    }
}

/** One page per epoch day, up to the year 2243. */
private const val DIARY_PAGES = 100_000

/** How far [page] is from rest: above 0 it is turned over toward the left, below 0 it lies under the page on top. */
private fun PagerState.turnOf(page: Int): Float = currentPage - page + currentPageOffsetFraction

/**
 * A book's page turn in place of the pager's slide. Pages stay put; the earlier of the two lifts about its left
 * edge over the later one, its right edge following the finger, and the page underneath lightens as it shows.
 */
private fun Modifier.pageTurn(state: PagerState, page: Int): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            // The page being turned lies on top of the one it uncovers.
            placeable.place(0, 0, zIndex = state.turnOf(page))
        }
    }
    .graphicsLayer {
        val turn = state.turnOf(page)
        // A page fully turned, either way, keeps the pager's place off screen, so it never takes a touch.
        if (abs(turn) >= 1f) return@graphicsLayer
        translationX = turn * size.width
        if (turn > 0f) {
            transformOrigin = TransformOrigin(0f, 0.5f)
            cameraDistance = 24f * density
            rotationY = -(acos(1f - turn) * 180f / kotlin.math.PI).toFloat()
        }
    }
    .drawWithContent {
        drawContent()
        val turn = state.turnOf(page)
        val shade = when {
            abs(turn) >= 1f -> 0f
            turn > 0f -> 0.12f * turn
            else -> 0.25f * -turn
        }
        if (shade > 0f) drawRect(Color.Black.copy(alpha = shade))
    }
