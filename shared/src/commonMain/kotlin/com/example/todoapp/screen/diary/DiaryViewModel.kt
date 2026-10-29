package com.example.todoapp.screen.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.app.AppClock
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.data.mission.milestone.MilestoneWithDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.db.models.TaskStatus
import com.example.todoapp.domain.Energy
import com.example.todoapp.domain.OfferResponse
import com.example.todoapp.domain.TaskState
import com.example.todoapp.domain.WeekMath
import com.example.todoapp.domain.taskState
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.DayLogRepository
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class DiaryViewModel(
    private val missionRepository: MissionRepository,
    private val todayTaskRepository: TodayTaskRepository,
    private val planRepository: PlanRepository,
    private val milestoneRepository: MilestoneRepository,
    weekRepository: WeekRepository,
    dayLogRepository: DayLogRepository,
    private val currentDate: LocalDate,
) : ViewModel() {

    private val now = AppClock.now()
    private val today: LocalDate = now.date
    val isToday: Boolean = currentDate == today
    val isTomorrow: Boolean = currentDate == today.plus(1, DateTimeUnit.DAY)
    /** Today, and tomorrow the night before: the pages with free time, swap, lighter day and "Looks good". */
    val canPlan: Boolean = isToday || isTomorrow
    /** Past days, and today from 18:00, when this view model was made. */
    private val startedInEvening: Boolean = currentDate < now.date || (currentDate == now.date && now.hour >= EVENING_HOUR)

    private val _eveningOpen = MutableStateFlow(startedInEvening)
    /** Today's list switches to the evening check at 18:00; past days always show it. */
    val eveningOpen: StateFlow<Boolean> = _eveningOpen

    private val _notice = MutableStateFlow<String?>(null)
    /** A short one-off message under the list, cleared by the next action. */
    val notice: StateFlow<String?> = _notice

    /** True once tomorrow got its "Looks good"; the evening check offers to plan it until then. */
    val tomorrowPlanned: StateFlow<Boolean> = planRepository.observePlan(today.plus(1, DateTimeUnit.DAY))
        .map { it?.confirmed == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    private val pageWeek = WeekMath.weekStart(currentDate)

    private val diaryExtras = combine(
        // The week's earlier plans decide whether this day opens the week; later ones give the room ahead.
        planRepository.observePlans(pageWeek, lookAhead),
        planRepository.observeSettings(),
        weekRepository.observeSummary(pageWeek.minus(7, DateTimeUnit.DAY)),
        // Only a finished week has a summary; the strip reads its stored targets.
        if (pageWeek < WeekMath.weekStart(today)) weekRepository.observeSummary(pageWeek) else flowOf(null),
        // Offers show only on today's page.
        if (isToday) weekRepository.observeOffers(WeekMath.weekStart(today)) else flowOf(emptyList()),
    ) { plans, settings, lastWeek, thisPageWeek, offers -> DiaryExtras(plans, settings, lastWeek, thisPageWeek, offers) }

    /**
     * The page's badges also read when milestones were done, archived missions' too, the first stored day,
     * and the day's mood and water.
     */
    private val badgeExtras = combine(
        milestoneRepository.observeIncludingArchived(),
        todayTaskRepository.observeFirstDate(),
        dayLogRepository.observe(currentDate, currentDate),
    ) { milestones, first, logs -> BadgeExtras(milestones, first, logs) }

    /**
     * Archived missions come in too, only so their points earned this week still count in the bars.
     * A builder failure keeps what's on screen instead of closing the app, as on the Metrics tab.
     */
    private var lastState: DiaryUiState? = null

    val state: StateFlow<DiaryUiState?> = combine(
        missionRepository.observeActive(),
        missionRepository.observeAll(),
        todayTaskRepository.observeBetween(currentDate.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY), lookAhead),
        diaryExtras.combine(badgeExtras) { extras, badges -> extras to badges },
        planReady,
    ) { missions, allMissions, tasks, (extras, badgeExtras), ready ->
        if (!ready) return@combine null
        // One bad build keeps the last screen and the next change of data builds again.
        try {
            DiaryStateBuilder.build(
                date = currentDate, today = today,
                missions = missions, allMissions = allMissions,
                tasks = tasks, extras = extras, badgeExtras = badgeExtras,
            ).also { lastState = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "The diary couldn't be built; it keeps what it showed." }
            lastState
        }
    }
        .flowOn(Dispatchers.Default)
        .catch { e ->
            if (e is CancellationException) throw e
            Logger.w(e) { "The diary's data stopped loading; it keeps what it showed." }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The page's milestones, for the to-do list (see [pageMilestones]). */
    val milestones: StateFlow<List<MilestoneWithDetails>> = milestoneRepository
        .observeActiveWithDetails()
        .map { all -> pageMilestones(all, currentDate) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _milestoneOffer = MutableStateFlow<MilestoneOffer?>(null)
    /** A finish line's item just ticked, with the milestone it might have reached; null when there's none to ask about. */
    val milestoneOffer: StateFlow<MilestoneOffer?> = _milestoneOffer

    init {
        // A failed build still shows the diary, with what's stored, so the app always opens.
        viewModelScope.launch {
            try {
                if (isToday) planRepository.ensurePlanned(currentDate)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "The day couldn't be planned or closed; the next open tries again." }
                if (isToday) _notice.value = "Today's list couldn't be built. Reopen the app to try again."
            } finally {
                planReady.value = true
            }
            // Past days too; it does nothing without a plan. A failure here leaves the list as built.
            if (startedInEvening) {
                try {
                    planRepository.tickFixedBlocks(currentDate)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.w(e) { "Fixed blocks couldn't be ticked for the evening check." }
                }
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
            val clock = AppClock.now()
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

    fun toggleDone(taskId: Long) = act {
        planRepository.toggleDone(taskId)
        offerMilestone(taskId)
    }
    fun didSome(taskId: Long) = act { planRepository.setState(taskId, TaskState.PARTIAL) }
    /** The swipe (spec 6.5): [percent] of the item got done. */
    fun setProgress(taskId: Long, percent: Int) = act {
        planRepository.setProgress(taskId, percent)
        offerMilestone(taskId)
    }

    /**
     * When [taskId] is now done and its mission has an open milestone, asks whether that's reached too: only
     * from a week before the milestone's date, and not while a "Not yet" for it is still quiet.
     */
    private suspend fun offerMilestone(taskId: Long) {
        val task = todayTaskRepository.get(taskId)?.takeIf { it.taskState == TaskState.DONE } ?: return
        val next = milestoneRepository.getForMission(task.missionId)
            .sortedBy { it.milestoneOrder }
            .firstOrNull { it.status != TaskStatus.COMPLETED.value } ?: return
        if (currentDate < next.expectedCompletionDate.toLocalDate().minus(MILESTONE_ASK_DAYS, DateTimeUnit.DAY)) return
        if (milestoneQuietUntil[next.milestoneId]?.let { currentDate < it } == true) return
        val mission = missionRepository.get(task.missionId) ?: return
        _milestoneOffer.value = MilestoneOffer(next, mission.title, taskId)
    }

    /** Yes on the offer: the milestone is done on this page's day, by that item. */
    fun acceptMilestoneOffer() {
        val offer = _milestoneOffer.value ?: return
        _milestoneOffer.value = null
        act {
            milestoneRepository.updateMilestone(
                offer.milestone.copy(
                    status = TaskStatus.COMPLETED.value,
                    actualCompletionDate = MyDate.fromLocalDate(currentDate),
                    completionTaskId = offer.taskId,
                )
            )
        }
    }

    /** "Not yet": this milestone isn't asked about again for [MILESTONE_QUIET_DAYS]. */
    fun dismissMilestoneOffer() {
        _milestoneOffer.value?.let { milestoneQuietUntil[it.milestone.milestoneId] = currentDate.plus(MILESTONE_QUIET_DAYS, DateTimeUnit.DAY) }
        _milestoneOffer.value = null
    }
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
    fun confirm() = act {
        planRepository.confirm(currentDate)
        if (isTomorrow) _notice.value = "Tomorrow is set. Sleep well."
    }
    /** The write can't be cancelled (PlanRepository.write), and a second answer to the same offer does nothing. */
    fun answerOffer(offerId: Long, response: OfferResponse) = act { planRepository.answerOffer(offerId, response) }

    /** Opens the pencil's list. A tap before the diary is built, or a second tap while it loads or shows, does nothing. */
    fun openAdd() {
        if (state.value == null || _addOptions.value != null || addLoading?.isActive == true) return
        _notice.value = null
        addLoading = viewModelScope.launch {
            try {
                val choices = planRepository.addOptions(currentDate)
                _addOptions.value = choices.recommended.map { AddOption.from(it, recommended = true) } +
                    choices.rest.map { AddOption.from(it, recommended = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "The pencil's list didn't load." }
                _notice.value = "The list didn't load. Tap the pencil to try again."
            }
        }
    }

    fun closeAdd() {
        addLoading?.cancel()
        _addOptions.value = null
    }

    fun updateMilestoneStatus(milestone: Milestone, achieved: Boolean, completionDate: LocalDate) = act {
        val updated = if (achieved) {
            milestone.copy(status = TaskStatus.COMPLETED.value, actualCompletionDate = MyDate.fromLocalDate(completionDate))
        } else {
            milestone.copy(status = TaskStatus.IN_PROGRESS.value, actualCompletionDate = null, completionTaskId = null)
        }
        milestoneRepository.updateMilestone(updated)
    }

    /** Every action clears the last notice first; swap may set a new one, and so does a write that fails. */
    private fun act(block: suspend () -> Unit) {
        _notice.value = null
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "A diary write failed." }
                _notice.value = "That didn't save. Try again."
            }
        }
    }

    companion object {
        const val EVENING_HOUR = 18
        private const val EVENING_CHECK_MILLIS = 30_000L
    }
}

/** A milestone that ticking [taskId] may have reached, with its mission's title for the question. */
data class MilestoneOffer(val milestone: Milestone, val missionTitle: String, val taskId: Long)

/**
 * A page's milestones: open ones due within [UPCOMING_DAYS] after it, and late ones, so they can still be
 * ticked; done ones on the day they were done and the next, then they're gone. Sorted by due date.
 */
internal fun pageMilestones(all: List<MilestoneWithDetails>, page: LocalDate): List<MilestoneWithDetails> =
    all.filter {
        val doneOn = it.milestone.actualCompletionDate?.toLocalDate()
        if (it.milestone.status == TaskStatus.COMPLETED.value) doneOn != null && doneOn.daysUntil(page) in 0 until DONE_DAYS
        else page.daysUntil(it.milestone.expectedCompletionDate.toLocalDate()) <= UPCOMING_DAYS
    }.sortedBy { it.milestone.expectedCompletionDate.dateString }

/** How far ahead an open milestone shows in the diary. */
private const val UPCOMING_DAYS = 15
/** For how many days, the done day first, a done milestone stays in the diary. */
private const val DONE_DAYS = 2
/** How many days before a milestone's date ticking its mission starts asking whether it's reached. */
private const val MILESTONE_ASK_DAYS = 7
/** How long a "Not yet" keeps a milestone from being asked about again. */
private const val MILESTONE_QUIET_DAYS = 3
/**
 * Milestone id to the day it may be asked about again. Kept for the app's run, across diary pages;
 * an app restart forgets it.
 */
private val milestoneQuietUntil = mutableMapOf<Long, LocalDate>()
