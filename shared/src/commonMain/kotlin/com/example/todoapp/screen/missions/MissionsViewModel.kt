package com.example.todoapp.screen.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.todoapp.db.backup.Backup
import com.example.todoapp.db.backup.BackupException
import com.example.todoapp.db.backup.BackupRepository
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.Blueprint
import com.example.todoapp.domain.Blueprints
import com.example.todoapp.domain.History
import com.example.todoapp.domain.Idea
import com.example.todoapp.domain.IdeaLibrary
import com.example.todoapp.domain.MissionDraft
import com.example.todoapp.domain.Points
import com.example.todoapp.repo.IdeaBank
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.reminder.ReminderSettings
import com.example.todoapp.domain.MissionKind
import com.example.todoapp.domain.missionKind
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.ReminderRepository
import com.example.todoapp.repo.TodayTaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Which mission the editor is open on; 0 for a new one. A new one may start from [seed], a draft
 * borrowed from the idea titled [ideaTitle], and then opens on the before-saving screen.
 */
data class EditorTarget(val missionId: Long, val seed: MissionDraft? = null, val ideaTitle: String? = null)

/** "Added. +7 Health points a week." with Undo, after a quick add. [explains] is the first one ever, which also says what points and areas are. */
data class AddedNotice(val text: String, val missionId: Long, val explains: Boolean = false)

class MissionsViewModel(
    private val missionRepository: MissionRepository,
    todayTaskRepository: TodayTaskRepository,
    private val planRepository: PlanRepository,
    milestoneRepository: MilestoneRepository,
    private val reminderRepository: ReminderRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    /** Fixed for this view model's life; the tab makes a new one each time it opens. */
    private val today: LocalDate = MyDate.now().toLocalDate()

    val state: StateFlow<MissionsUiState?> = combine(
        missionRepository.observeActive(),
        todayTaskRepository.observeBetween(
            today.minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY),
            today.plus(7, DateTimeUnit.DAY),
        ),
        milestoneRepository.observeAll(),
        planRepository.observeSettings(),
    ) { missions, tasks, milestones, settings ->
        MissionsStateBuilder.build(today, missions, tasks, milestones, settings)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The year at a glance: the strip on the tab and the full Blueprint screen. */
    val blueprint: StateFlow<Blueprint?> = combine(
        missionRepository.observeAll(),
        todayTaskRepository.observeBetween(
            // Older than the first week shown, so rhythms know when they were last done.
            Blueprints.firstWeek(today).minus(PlanRepository.HISTORY_DAYS, DateTimeUnit.DAY),
            Blueprints.lastDay(today),
        ),
        milestoneRepository.observeAll(),
        planRepository.observeSettings(),
    ) { missions, tasks, milestones, settings ->
        Blueprints.build(today, missions, tasks, milestones, settings)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _editor = MutableStateFlow<EditorTarget?>(null)
    /** Non-null while the sentence flow is open. */
    val editor: StateFlow<EditorTarget?> = _editor

    private val _libraryOpen = MutableStateFlow(false)
    /** True while the ideas library is open (spec 8); + opens it. */
    val libraryOpen: StateFlow<Boolean> = _libraryOpen

    private val _library = MutableStateFlow<IdeaLibrary?>(null)
    val library: StateFlow<IdeaLibrary?> = _library

    /** Lowercased titles of active missions, so ideas already added show "Added". */
    val addedTitles: StateFlow<Set<String>> = missionRepository.observeActive()
        .map { list -> list.map { it.title.trim().lowercase() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _added = MutableStateFlow<AddedNotice?>(null)
    val added: StateFlow<AddedNotice?> = _added

    init {
        viewModelScope.launch {
            runCatching { IdeaBank.get() }
                .onSuccess { _library.value = it }
                .onFailure { Logger.w(it) { "The ideas library didn't load." } }
        }
    }

    /** + opens the ideas library. A second tap while something is opening does nothing. */
    fun openNew() {
        if (_editor.value == null) _libraryOpen.value = true
    }

    fun closeLibrary() {
        _libraryOpen.value = false
        _added.value = null
    }

    /** "Write your own": the nine questions from the start. */
    fun writeOwn() {
        if (_editor.value == null) _editor.value = EditorTarget(0L)
    }

    /** Edit on an idea: the before-saving screen, pre-filled. */
    fun editIdea(idea: Idea, title: String = idea.displayTitle) {
        if (_editor.value == null) _editor.value = EditorTarget(0L, idea.toDraft(today, title), idea.displayTitle)
    }

    fun openEdit(missionId: Long) {
        if (_editor.value == null) _editor.value = EditorTarget(missionId)
    }

    /** A saved mission closes the library too; leaving the editor otherwise goes back to it. */
    fun closeEditor(saved: Boolean = false) {
        _editor.value = null
        if (saved) _libraryOpen.value = false
    }

    /** + on an idea saves it as is and says what it adds to the week. */
    fun quickAdd(idea: Idea, title: String = idea.displayTitle) {
        viewModelScope.launch {
            try {
                val draft = idea.toDraft(today, title)
                val first = missionRepository.count() == 0
                val mission = draft.toMission()
                val id = missionRepository.saveWithMilestones(mission, draft.milestones)
                runCatching { planRepository.missionSaved(id, false) }
                    .onFailure { Logger.w(it) { "Today's re-pick after a quick add failed." } }
                val points = if (mission.weight > 0 && draft.pillar.scored) {
                    mission.weight * Points.occurrencesInWeek(mission.copy(missionId = id), today, History(emptyList()))
                } else 0
                val added = if (points > 0) "Added. +$points ${draft.pillar.title} ${if (points == 1) "point" else "points"} a week." else "Added."
                val text = if (first) "$added\nPoints show how much of your week a mission fills. Health, Wealth, Love, Life and Recharge are the areas you balance." else added
                _added.value = AddedNotice(text, id, explains = first)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Quick add didn't save." }
                _added.value = AddedNotice("That didn't save. Try again.", 0L)
            }
        }
    }

    /** Undo on the added notice archives what was just added. */
    fun undoAdd() {
        val notice = _added.value ?: return
        _added.value = null
        if (notice.missionId == 0L) return
        viewModelScope.launch {
            runCatching { planRepository.archiveMission(notice.missionId) }
                .onFailure { Logger.w(it) { "Undo of a quick add failed." } }
        }
    }

    fun clearAdded(notice: AddedNotice) {
        if (_added.value == notice) _added.value = null
    }

    private val _notice = MutableStateFlow<String?>(null)
    /** A short message under the free-time chip when a save didn't go through; cleared by the next save. */
    val notice: StateFlow<String?> = _notice

    val reminders: StateFlow<ReminderSettings?> = reminderRepository.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Fixed blocks, for the reminders' "starts now" switches. */
    val blocks: StateFlow<List<Mission>> = missionRepository.observeActive()
        .map { all -> all.filter { it.missionKind == MissionKind.BLOCK } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun saveReminders(settings: ReminderSettings) {
        _notice.value = null
        viewModelScope.launch {
            try {
                reminderRepository.save(settings)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Reminders didn't save." }
                _notice.value = "That didn't save. Try again."
            }
        }
    }

    /** The write itself can't be cancelled (PlanRepository.write), so leaving the tab doesn't lose it. */
    fun saveFreeTime(minutesByWeekday: Map<Int, Int>) {
        _notice.value = null
        viewModelScope.launch {
            try {
                planRepository.saveSettings(minutesByWeekday)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Free time didn't save." }
                _notice.value = "That didn't save. Try again."
            }
        }
    }

    private val _backupNote = MutableStateFlow<String?>(null)
    /** A line in the backup dialog: what the last save or restore did. */
    val backupNote: StateFlow<String?> = _backupNote

    private val _pendingRestore = MutableStateFlow<Backup?>(null)
    /** A backup that was read and checked, waiting for "Replace everything?". */
    val pendingRestore: StateFlow<Backup?> = _pendingRestore

    fun clearBackupNote() {
        _backupNote.value = null
    }

    /** Builds the file and hands it to [save], the platform's save dialog. */
    fun exportBackup(save: (fileName: String, text: String) -> Unit) {
        _backupNote.value = null
        viewModelScope.launch {
            try {
                save("inbalance-backup-${MyDate.now().dateString}.json", backupRepository.export())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Backup didn't build." }
                _backupNote.value = "The backup didn't save. Try again."
            }
        }
    }

    fun backupSaved(ok: Boolean) {
        _backupNote.value = if (ok) "Backup saved." else "The backup didn't save. Try again."
    }

    /** [text] is the picked file, or null when it couldn't be read. A good one waits in [pendingRestore]. */
    fun readBackup(text: String?) {
        _backupNote.value = null
        if (text == null) {
            _backupNote.value = "That file couldn't be opened."
            return
        }
        viewModelScope.launch {
            try {
                _pendingRestore.value = backupRepository.read(text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupException) {
                _backupNote.value = e.message
            } catch (e: Exception) {
                Logger.w(e) { "Backup didn't read." }
                _backupNote.value = "That file couldn't be opened."
            }
        }
    }

    fun cancelRestore() {
        _pendingRestore.value = null
    }

    fun confirmRestore() {
        val backup = _pendingRestore.value ?: return
        _pendingRestore.value = null
        viewModelScope.launch {
            try {
                backupRepository.restore(backup)
                _backupNote.value = "Restored. Your data is back as it was on ${backup.exportedOn}."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Restore failed." }
                _backupNote.value = "The restore didn't work, so nothing changed."
            }
        }
    }
}
