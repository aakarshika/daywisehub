package com.example.todoapp.reminders

import co.touchlab.kermit.Logger
import com.example.todoapp.domain.Reminders
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.ReminderRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Keeps the scheduled notifications in step with what's stored: any change to the missions, the days ahead,
 * their plans or the reminder settings schedules them all again, a moment later. Started once the app is ready.
 * The notification prompt waits for the first mission (or the Reminders dialog), so a new user meets the app first.
 */
class ReminderSync(
    private val reminderRepository: ReminderRepository,
    private val missionRepository: MissionRepository,
    private val todayTaskRepository: TodayTaskRepository,
    private val planRepository: PlanRepository,
    private val scheduler: ReminderScheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun start() {
        if (job != null) return
        scope.launch {
            missionRepository.observeActive().first { it.isNotEmpty() }
            askPermission()
        }
        job = scope.launch {
            // The date, checked every minute, so a new day reads its own days ahead.
            flow {
                while (true) {
                    emit(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
                    delay(60_000)
                }
            }.distinctUntilChanged().flatMapLatest { today ->
                val last = today.plus(Reminders.BLOCK_DAYS + 1, DateTimeUnit.DAY)
                combine(
                    reminderRepository.observe(),
                    missionRepository.observeActive(),
                    todayTaskRepository.observeBetween(today, last),
                    planRepository.observePlans(today, last),
                ) { settings, missions, tasks, plans ->
                    Reminders.build(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()), settings, missions, tasks, plans)
                }
            }.debounce(1_500).collectLatest { reminders ->
                runCatching { scheduler.replaceAll(reminders) }
                    .onFailure { Logger.w(it) { "Reminders couldn't be scheduled; the next change tries again." } }
            }
        }
    }

    /** The system's notification prompt; it shows only while the user hasn't answered it. */
    fun askPermission() {
        scope.launch(Dispatchers.Main) { scheduler.askPermission() }
    }
}
