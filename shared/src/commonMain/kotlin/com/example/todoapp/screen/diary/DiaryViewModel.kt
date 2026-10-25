package com.example.todoapp.screen.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.OfferResponse
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayTaskRepository
import com.example.todoapp.repo.WeekRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class DiaryViewModel(
    missionRepository: MissionRepository,
    todayTaskRepository: TodayTaskRepository,
    private val planRepository: PlanRepository,
    private val milestoneRepository: MilestoneRepository,
    weekRepository: WeekRepository,
    private val currentDate: LocalDate,
) : ViewModel() {

    private val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    private val today: LocalDate = now.date
    val isToday: Boolean = currentDate == today
    /** Past days, and today from 18:00, when this view model was made. */
    private val startedInEvening: Boolean = currentDate < now.date || (currentDate == now.date && now.hour >= EVENING_HOUR)

    private val _eveningOpen = MutableStateFlow(startedInEvening)
    /** Today's list switches to the evening check at 18:00, or anytime by hand; past days always show it. */
    val eveningOpen: StateFlow<Boolean> = _eveningOpen

    private val _notice = MutableStateFlow<String?>(null)
    /** A short one-off message under the list, cleared by the next action. */
    val notice: StateFlow<String?> = _notice

    private val _addOptions = MutableStateFlow<List<AddOption>?>(null)
    /** The pencil's list while its dialog is open; null when it's closed. */
    val addOptions: StateFlow<List<AddOption>?> = _addOptions
    private var addLoading: Job? = null

    /** False until today's plan is built, so the list doesn't flicker through half-built states. */
    private val planReady = MutableStateFlow(!isToday)

    /**
     * The last day the diary reads: a week past this day or today, whichever is later, for room to move
     * into. A page older than yesterday names no move (DiaryStateBuilder.namesMoves), so it reads only a
     * week past itself.
     */
    private val lookAhead =
        if (DiaryStateBuilder.namesMoves(currentDate, today)) maxOf(currentDate, today).plus(7, DateTimeUnit.DAY)
        else currentDate.plus(7, DateTimeUnit.DAY)

    private val diaryExtras = combine(
        // The week's earlier plans decide whether this day opens the week; later ones give the room ahead.
        planRepository.observePlans(WeekMath.weekStart(currentDate), lookAhead),
        planRepository.observeSettings(),
        weekRepository.observeSummary(WeekMath.weekStart(currentDate).minus(7, DateTimeUnit.DAY)),
        // Offers show only on today's page.
        if (isToday) weekRepository.observeOffers(WeekMath.weekStart(today)) else flowOf(emptyList()),
    ) { plans, settings, summary, offers -> DiaryExtras(plans, settings, summary, offers) }

    /** Archived missions come in too, only so their points earned this week still count in the bars. */
    val state: StateFlow<DiaryUiState?> = combine(
        missionRepository.observeActive(),
        missionRepository.observeAll(),
        todayTaskRepository.observeBetween(currentDate.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY), lookAhead),
        diaryExtras,
        planReady,
    ) { missions, allMissions, tasks, extras, ready ->
        if (ready) {
            DiaryStateBuilder.build(
                date = currentDate, today = today,
                missions = missions, allMissions = allMissions,
                tasks = tasks, extras = extras,
            )
        } else null
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val milestones: StateFlow<List<MilestoneWithDetails>> = milestoneRepository
        .observeInRange(MyDate.fromLocalDate(currentDate).subtractDays(7), MyDate.fromLocalDate(currentDate).addDays(7))
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            try {
                if (isToday) planRepository.ensurePlanned(currentDate)
                if (startedInEvening) planRepository.tickFixedBlocks(currentDate)   // past days too; it does nothing without a plan
            } finally {
                planReady.value = true
            }
        }
        if (isToday && !startedInEvening) viewModelScope.launch { openEveningAtSix() }
    }

    /**
     * Opens the evening check at 18:00 while today's diary stays on screen. It looks at the clock at
     * least every half minute rather than sleeping until 18:00, because a sleeping phone pauses timers.
     */
    private suspend fun openEveningAtSix() {
        while (true) {
            val clock = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            // After midnight CalDiaryViewModel moves the diary to the new day, with a new view model.
            if (clock.date != currentDate) return
            if (clock.hour >= EVENING_HOUR) {
                if (!_eveningOpen.value) openEvening()
                return
            }
            val untilSix = ((EVENING_HOUR - clock.hour) * 3600L - clock.minute * 60L - clock.second) * 1000
            delay(minOf(EVENING_CHECK_MILLIS, untilSix))
        }
    }

    /** Shows the evening check and ticks the day's fixed blocks (once per day, so an untick sticks). */
    private fun openEvening() {
        _eveningOpen.value = true
        act { planRepository.tickFixedBlocks(currentDate) }
    }

    fun toggleDone(taskId: Long) = act { planRepository.toggleDone(taskId) }
    fun didSome(taskId: Long) = act { planRepository.setState(taskId, TaskState.PARTIAL) }
    fun moveTask(taskId: Long) = act { planRepository.moveToNextRoom(taskId) }
    fun skipTask(taskId: Long) = act { planRepository.setState(taskId, TaskState.SKIPPED) }
    fun toggleCrown(taskId: Long) = act { planRepository.toggleCrown(taskId) }
    fun swap(taskId: Long) = act {
        _notice.value = if (planRepository.swap(taskId)) null else "Nothing else fits today, so this one stays."
    }
    fun removeTask(taskId: Long) = act { planRepository.removeTask(taskId) }
    fun addMission(missionId: Long) = act { planRepository.addMission(currentDate, missionId) }
    fun lighterDay() = act { planRepository.lighterDay(currentDate) }
    fun changeDay(freeMinutes: Int, energy: Energy) = act { planRepository.changeDay(currentDate, freeMinutes, energy) }
    fun confirm() = act { planRepository.confirm(currentDate) }
    /** The write can't be cancelled (PlanRepository.write), and a second answer to the same offer does nothing. */
    fun answerOffer(offerId: Long, response: OfferResponse) = act { planRepository.answerOffer(offerId, response) }

    /** Opens the pencil's list. A tap before the diary is built, or a second tap while it loads or shows, does nothing. */
    fun openAdd() {
        if (state.value == null || _addOptions.value != null || addLoading?.isActive == true) return
        _notice.value = null
        addLoading = viewModelScope.launch {
            try {
                _addOptions.value = planRepository.addOptions(currentDate).map { AddOption.from(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _notice.value = "The list didn't load. Tap the pencil to try again."
            }
        }
    }

    fun closeAdd() {
        addLoading?.cancel()
        _addOptions.value = null
    }

    fun toggleEveningCheck() {
        if (!isToday) return
        if (_eveningOpen.value) {
            _notice.value = null
            _eveningOpen.value = false
        } else {
            openEvening()
        }
    }

    fun updateMilestoneStatus(milestone: Milestone, achieved: Boolean, completionDate: LocalDate) = act {
        val updated = if (achieved) {
            milestone.copy(status = TaskStatus.COMPLETED.value, actualCompletionDate = MyDate.fromLocalDate(completionDate))
        } else {
            milestone.copy(status = TaskStatus.IN_PROGRESS.value, actualCompletionDate = null)
        }
        milestoneRepository.updateMilestone(updated)
    }

    /** Every action clears the last notice first; swap may set a new one. */
    private fun act(block: suspend () -> Unit) {
        _notice.value = null
        viewModelScope.launch { block() }
    }

    companion object {
        const val EVENING_HOUR = 18
        private const val EVENING_CHECK_MILLIS = 30_000L
    }
}
