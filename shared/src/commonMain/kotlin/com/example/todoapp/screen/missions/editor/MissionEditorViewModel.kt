package com.example.todoapp.screen.missions.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Effort
import com.example.todoapp.domain.EditorStep
import com.example.todoapp.domain.History
import com.example.todoapp.domain.IfMissed
import com.example.todoapp.domain.MilestoneDraft
import com.example.todoapp.domain.MissionDraft
import com.example.todoapp.domain.MissionDrafts
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.Period
import com.example.todoapp.domain.PillarType
import com.example.todoapp.domain.WeekCost
import com.example.todoapp.domain.WeekCosts
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class EditorUiState(
    val isNew: Boolean,
    val draft: MissionDraft,
    val answered: Set<EditorStep>,
    /** The question on screen; null on the before-saving screen. */
    val step: EditorStep?,
    /** The question was opened from the before-saving screen, so Back returns there. */
    val fromSummary: Boolean = false,
    /** A save or an archive is under way; the draft can't change. */
    val saving: Boolean = false,
    /** The write under way is an archive, so Save doesn't read "Saving…". */
    val archiving: Boolean = false,
    /** The last save or archive didn't go through. */
    val saveFailed: Boolean = false,
) {
    val steps: List<EditorStep> get() = draft.steps

    /** 1-based place of [step] for "3 of 9"; the step count on the before-saving screen. */
    val position: Int get() = step?.let { steps.indexOf(it) + 1 } ?: steps.size

    /** Every question that applies has an answer, and the draft can be saved. */
    val complete: Boolean get() = draft.blocker == null && steps.all { it in answered }
}

/**
 * The nine-question sentence for a new mission ([missionId] 0) or an existing one.
 * A new mission walks the steps in order; an existing one opens on the before-saving screen,
 * and tapping an answer there asks that one question again.
 */
class MissionEditorViewModel(
    private val missionRepository: MissionRepository,
    todayTaskRepository: TodayTaskRepository,
    private val planRepository: PlanRepository,
    private val milestoneRepository: MilestoneRepository,
    private val missionId: Long,
) : ViewModel() {

    val today: LocalDate = MyDate.now().toLocalDate()
    private var initial: MissionDraft? = null

    private val _state = MutableStateFlow<EditorUiState?>(null)
    val state: StateFlow<EditorUiState?> = _state

    private val _finished = MutableStateFlow(false)
    /** True once saved or archived, or when the mission to edit is gone. The screen then closes. */
    val finished: StateFlow<Boolean> = _finished

    /**
     * What the mission costs this week; worked out only on the before-saving screen.
     * While saving it keeps its last value, so the summary doesn't jump as the saved row arrives.
     */
    val cost: StateFlow<WeekCost?> = run {
        var lastCost: WeekCost? = null
        combine(
            _state,
            missionRepository.observeActive(),
            todayTaskRepository.observeBetween(
                today.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY),
                today.plus(7, DateTimeUnit.DAY),
            ),
            planRepository.observeSettings(),
        ) { s, missions, tasks, settings ->
            when {
                s == null || s.step != null -> null
                s.saving -> lastCost
                else -> WeekCosts.compute(s.draft, missions, History(tasks), settings, today)
            }.also { lastCost = it }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }

    init {
        viewModelScope.launch {
            val draft = if (missionId == 0L) {
                MissionDrafts.new(today)
            } else {
                val mission = missionRepository.get(missionId)
                if (mission == null || !mission.active) {
                    _finished.value = true
                    return@launch
                }
                MissionDrafts.from(mission, milestoneRepository.getForMission(missionId))
            }
            initial = draft
            _state.value = EditorUiState(
                isNew = missionId == 0L,
                draft = draft,
                answered = if (missionId == 0L) emptySet() else EditorStep.entries.toSet(),
                step = if (missionId == 0L) EditorStep.TITLE else null,
            )
        }
    }

    /** Worth asking before leaving: anything written for a new mission, any change to an existing one. */
    fun hasChanges(): Boolean {
        val s = _state.value ?: return false
        return if (s.isNew) s.draft.title.isNotBlank() || s.answered.isNotEmpty() else s.draft != initial
    }

    // Step 1. A blank title isn't an answer, so Back can't return to a sentence without one.
    fun setTitle(text: String) = change(unanswer = { if (it.title.isBlank()) setOf(EditorStep.TITLE) else emptySet() }) {
        it.copy(title = text.take(MissionDrafts.MAX_TITLE_LENGTH))
    }

    fun confirmTitle() {
        if (_state.value?.draft?.title?.isNotBlank() == true) answer(EditorStep.TITLE)
    }

    // Step 2
    fun pickPillar(pillar: PillarType) = answer(EditorStep.PILLAR) { it.copy(pillarChoice = pillar) }

    // Step 3: a different kind starts its pace over.
    fun pickKind(kind: MissionKind) {
        val s = _state.value ?: return
        if (s.step != EditorStep.KIND || s.saving) return
        if (s.draft.kind != kind) _state.value = s.copy(answered = s.answered - EditorStep.PACE - EditorStep.GENTLE)
        answer(EditorStep.KIND) { it.withKind(kind, today) }
    }

    // Step 4. Changes show at once; "Next" confirms them. Tapping the pace already picked keeps a gentle start's pace.
    fun setCount(count: Int, period: Period) {
        val d = _state.value?.draft ?: return
        if (d.count == count && d.period == period) return
        change {
            it.copy(
                count = count,
                period = period,
                gentleCount = null,
                weekDays = if (period == Period.WEEK) it.weekDays else emptySet(),
            )
        }
    }

    /** A block left with no days loses its pace answer, so Back can't return to the before-saving screen. */
    fun toggleDay(day: Int) = change(
        unanswer = { if (it.kind == MissionKind.BLOCK && it.weekDays.isEmpty()) setOf(EditorStep.PACE) else emptySet() }
    ) { d ->
        d.copy(weekDays = if (day in d.weekDays) d.weekDays - day else d.weekDays + day)
    }

    fun shiftStart(minutes: Int) = change { d ->
        val start = (d.startMinute + minutes).coerceIn(0, MissionDrafts.DAY_MINUTES - MissionDrafts.CLOCK_STEP)
        d.copy(startMinute = start, endMinute = d.endMinute.coerceAtLeast(start + MissionDrafts.CLOCK_STEP))
    }

    fun shiftEnd(minutes: Int) = change { d ->
        d.copy(endMinute = (d.endMinute + minutes).coerceIn(d.startMinute + MissionDrafts.CLOCK_STEP, MissionDrafts.DAY_MINUTES))
    }

    /** The earliest and latest end a finish line can move to. */
    fun endDateLimits(d: MissionDraft): ClosedRange<LocalDate> =
        maxOf(today, d.startDate).plus(7, DateTimeUnit.DAY)..today.plus(MissionDrafts.MAX_FINISH_WEEKS * 7, DateTimeUnit.DAY)

    /** A finish line ends at least a week out and at most a year out; one already past can only move later. */
    fun shiftEndDate(weeks: Int) = change { d ->
        val limits = endDateLimits(d)
        val earliest = limits.start
        val latest = limits.endInclusive
        val current = d.endDate ?: earliest
        val moved = current.plus(weeks * 7, DateTimeUnit.DAY)
        d.copy(endDate = moved.coerceIn(minOf(earliest, current), maxOf(latest, current)))
    }

    fun addMilestone() = change { d ->
        if (d.milestones.size >= MissionDrafts.MAX_MILESTONES) d else d.copy(milestones = d.milestones + MilestoneDraft())
    }

    fun setMilestone(index: Int, text: String) = change { d ->
        d.copy(milestones = d.milestones.mapIndexed { i, m ->
            if (i == index) m.copy(text = text.take(MissionDrafts.MAX_MILESTONE_LENGTH)) else m
        })
    }

    fun removeMilestone(index: Int) = change { d -> d.copy(milestones = d.milestones.filterIndexed { i, _ -> i != index }) }

    fun setInterval(count: Int, unit: Period) = change {
        it.copy(intervalCount = count.coerceIn(MissionDrafts.intervalRange(unit)), intervalUnit = unit)
    }

    /** Recharge's pace is a single tap. */
    fun pickRechargeCount(count: Int) = answer(EditorStep.PACE) { it.copy(count = count, period = Period.WEEK) }

    /** "Next" on the pace step. A block needs at least one day. */
    fun confirmPace() {
        val d = _state.value?.draft ?: return
        if (d.kind == MissionKind.BLOCK && d.weekDays.isEmpty()) return
        answer(EditorStep.PACE)
    }

    // Steps 5 to 9
    fun pickDuration(minutes: Int) = answer(EditorStep.DURATION) { it.copy(durationMinutes = minutes) }
    fun pickEffort(effort: Effort) = answer(EditorStep.EFFORT) { it.copy(effort = effort) }
    fun pickWeight(weight: Int) = answer(EditorStep.WEIGHT) { it.copy(weight = weight) }
    fun pickIfMissed(rule: IfMissed) = answer(EditorStep.IF_MISSED) { it.copy(ifMissed = rule) }
    fun pickGentle(on: Boolean) = answer(EditorStep.GENTLE) { it.copy(gentle = on) }

    /** The gentle-start sticky on the before-saving screen. */
    fun setGentle(on: Boolean) {
        change { it.copy(gentle = on) }
        _state.value?.let { _state.value = it.copy(answered = it.answered + EditorStep.GENTLE) }
    }

    /** Tapping a highlighted answer asks that question again. */
    fun goTo(step: EditorStep) {
        val s = _state.value ?: return
        if (!s.saving && s.draft.applies(step)) _state.value = s.copy(step = step, fromSummary = true)
    }

    /** "Make it lighter" returns to the pace question. */
    fun makeLighter() = goTo(EditorStep.PACE)

    /**
     * One step back. A question opened from the before-saving screen returns there; otherwise Back walks
     * back through the questions. From the before-saving screen a new mission goes back to its last
     * question. False when there's nowhere left to go, so the screen should close.
     */
    fun back(): Boolean {
        val s = _state.value ?: return false
        if (s.saving) return true
        val steps = s.steps
        val step = s.step
        if (step == null) {
            if (!s.isNew) return false
            _state.value = s.copy(step = steps.last(), fromSummary = false)
            return true
        }
        if (s.fromSummary && steps.all { it in s.answered }) {
            _state.value = s.copy(step = null, fromSummary = false)
            return true
        }
        val index = steps.indexOf(step)
        if (index <= 0) return false
        _state.value = s.copy(step = steps[index - 1])
        return true
    }

    /**
     * Saves once, then re-picks today if it isn't confirmed yet. Both finish even if the screen goes away.
     * A failed save stays on screen to try again, keeping any id already saved so a retry updates, not copies.
     */
    fun save() {
        val s = _state.value ?: return
        if (s.saving || s.step != null || !s.complete) return
        _state.value = s.copy(saving = true, saveFailed = false)
        viewModelScope.launch {
            var savedId = s.draft.missionId
            try {
                withContext(NonCancellable) {
                    val mission = s.draft.toMission()
                    val milestones = if (s.draft.kind == MissionKind.FINISH_LINE) s.draft.milestones else emptyList()
                    savedId = missionRepository.saveWithMilestones(mission, milestones)
                    // The mission is saved by now; a failed re-pick of today mustn't make it look unsaved.
                    runCatching { planRepository.refreshToday() }
                }
                _state.value = _state.value?.let { it.copy(draft = it.draft.copy(missionId = savedId)) }
                _finished.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val id = savedId
                _state.value = _state.value?.let {
                    it.copy(saving = false, saveFailed = true, draft = it.draft.copy(missionId = id))
                }
            }
        }
    }

    fun archive() {
        val s = _state.value ?: return
        if (s.isNew || s.saving) return
        _state.value = s.copy(saving = true, archiving = true, saveFailed = false)
        viewModelScope.launch {
            try {
                planRepository.archiveMission(s.draft.missionId)
                _finished.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value?.copy(saving = false, archiving = false, saveFailed = true)
            }
        }
    }

    /**
     * Applies a change to the draft. A question that only now applies is asked again, and so is any
     * question [unanswer] names for the changed draft, all in one update.
     */
    private fun change(
        unanswer: (MissionDraft) -> Set<EditorStep> = { emptySet() },
        transform: (MissionDraft) -> MissionDraft,
    ) {
        val s = _state.value ?: return
        if (s.saving) return
        val draft = transform(s.draft)
        val newlyAsked = EditorStep.entries.filter { draft.applies(it) && !s.draft.applies(it) }.toSet()
        _state.value = s.copy(draft = draft, answered = s.answered - newlyAsked - unanswer(draft))
    }

    /** Records the answer to [step] (ignored if [step] isn't on screen) and moves to the first open question, or the before-saving screen. */
    private fun answer(step: EditorStep, transform: (MissionDraft) -> MissionDraft = { it }) {
        if (_state.value?.step != step) return
        change(transform = transform)
        val s = _state.value ?: return
        if (s.saving) return
        val answered = s.answered + step
        val next = s.draft.steps.firstOrNull { it !in answered }
        _state.value = s.copy(answered = answered, step = next, fromSummary = s.fromSummary && next != null)
    }
}
