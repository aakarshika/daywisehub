package com.example.todoapp.widget

import com.example.todoapp.app.AppClock
import com.example.todoapp.repo.DayLogRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * While the app runs, reloads the widgets a moment after the app changes what they show: today's and
 * the week's items, plans, missions, water and moods.
 */
class WidgetSync(
    private val missionRepository: MissionRepository,
    private val todayTaskRepository: TodayTaskRepository,
    private val planRepository: PlanRepository,
    private val dayLogRepository: DayLogRepository,
) {
    private var job: Job? = null

    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        val today = AppClock.today()
        val from = today.minus(31, DateTimeUnit.DAY)
        val to = today.plus(7, DateTimeUnit.DAY)
        job = scope.launch {
            combine(
                missionRepository.observeAll(),
                todayTaskRepository.observeBetween(from, to),
                planRepository.observePlans(from, to),
                dayLogRepository.observe(today, today),
            ) { _, _, _, _ -> }
                .drop(1)
                .debounce(800)
                .collect { WidgetHooks.changed() }
        }
    }
}
