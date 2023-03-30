package tech.nagual.phoenix.tools.organizer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import tech.nagual.common.preferences.datastore.defaultOf
import tech.nagual.phoenix.tools.organizer.components.MediaStorageManager
import tech.nagual.phoenix.tools.organizer.data.model.Folder
import tech.nagual.phoenix.tools.organizer.data.model.Note
import tech.nagual.phoenix.tools.organizer.data.model.Workflow
import tech.nagual.phoenix.tools.organizer.data.repo.*
import tech.nagual.phoenix.tools.organizer.preferences.GroupNotesWithoutNotebook
import tech.nagual.phoenix.tools.organizer.preferences.LayoutMode
import tech.nagual.phoenix.tools.organizer.preferences.NoteDeletionTime
import tech.nagual.phoenix.tools.organizer.preferences.SortMethod
import tech.nagual.phoenix.tools.organizer.reminders.ReminderManager
import tech.nagual.phoenix.tools.organizer.preferences.PreferenceRepository
import java.io.File
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    val folderRepository: FolderRepository,
    private val preferenceRepository: PreferenceRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderManager: ReminderManager,
    private val tagRepository: TagRepository,
    private val workflowRepository: WorkflowRepository,
    private val mediaStorageManager: MediaStorageManager,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val workflows: StateFlow<List<Workflow>> =
        workflowRepository.getAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = listOf(),
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val folders: StateFlow<Pair<Boolean, List<Folder>>> =
        preferenceRepository.get<GroupNotesWithoutNotebook>()
            .flatMapLatest { groupNotesWithoutNotebook ->
                folderRepository.getAll()
                    .map { notebooks ->
                        (groupNotesWithoutNotebook == GroupNotesWithoutNotebook.YES) to notebooks
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = (defaultOf<GroupNotesWithoutNotebook>() == GroupNotesWithoutNotebook.YES) to listOf(),
            )

    var showHiddenNotes: Boolean = false
    var notesToProcess: Set<Note>? = null
    var tempMediaUri: Uri? = null
    var tempMediaFile: File? = null

    fun discardEmptyNotesAsync() = viewModelScope.async(Dispatchers.IO) {
        noteRepository.discardEmptyNotes()
    }

    fun deleteNotesPermanently(vararg notes: Note) = viewModelScope.launch(Dispatchers.IO) {
        notes.forEach { reminderManager.cancelAllRemindersForNote(it.id) }
        noteRepository.deleteNotes(*notes)
    }

    fun deleteNotes(vararg notes: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            notes.forEach { reminderManager.cancelAllRemindersForNote(it.id) }

            when (preferenceRepository.get<NoteDeletionTime>().first()) {
                NoteDeletionTime.INSTANTLY -> {
                    noteRepository.deleteNotes(*notes)
                    mediaStorageManager.cleanUpStorage()
                }
                else -> {
                    noteRepository.moveNotesToBin(*notes)
                }
            }
        }
    }

    fun restoreNotes(vararg notes: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.restoreNotes(*notes)
        }
    }

    fun archiveNotes(vararg notes: Note) = update(*notes) { note ->
        note.copy(
            isArchived = true,
        )
    }

    fun unarchiveNotes(vararg notes: Note) = update(*notes) { note ->
        note.copy(
            isArchived = false,
        )
    }

    fun showNotes(vararg notes: Note) = update(*notes) { note ->
        note.copy(
            isHidden = false,
        )
    }

    fun hideNotes(vararg notes: Note) = update(*notes) { note ->
        note.copy(
            isHidden = true,
        )
    }

    fun pinNotes(vararg notes: Note) = update(*notes) { note ->
        note.copy(
            isPinned = !note.isPinned,
        )
    }

    fun moveNotes(folderId: Long?, vararg notes: Note) = update(*notes) { note ->
        note.copy(
            folderId = folderId,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun disableMarkdown(vararg notes: Note) = update(*notes) { note ->
        note.copy(
            isMarkdownEnabled = false,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun enableMarkdown(vararg notes: Note) = update(*notes) { note ->
        note.copy(
            isMarkdownEnabled = true,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun duplicateNotes(vararg notes: Note) = notes.forEachAsync { note ->
        val oldId = note.id
        val cloned = note.copy(
            id = 0L,
            creationDate = Instant.now().epochSecond,
            modifiedDate = Instant.now().epochSecond,
            deletionDate = if (note.isDeleted) Instant.now().epochSecond else null
        )

        val newId = noteRepository.insertNote(cloned)
        tagRepository.copyTags(oldId, newId)
        reminderRepository.copyReminders(oldId, newId)

        reminderRepository
            .getByNoteId(newId)
            .first()
            .forEach {
                reminderManager.schedule(it.id, it.date, it.noteId)
            }
    }

    fun setLayoutMode(layoutMode: LayoutMode) {
        viewModelScope.launch(Dispatchers.IO) {
            preferenceRepository.set(layoutMode)
        }
    }

    fun setSortMethod(method: SortMethod) {
        viewModelScope.launch(Dispatchers.IO) {
            preferenceRepository.set(method)
        }
    }

    suspend fun createMediaFile(type: MediaStorageManager.MediaType): Uri? {
        val (uri, file) = mediaStorageManager.createMediaFile(type = type)
            ?: return null
        tempMediaUri = uri
        tempMediaFile = file
        return uri
    }

    fun getMediaPath(): File = mediaStorageManager.directory

    private inline fun update(
        vararg notes: Note,
        crossinline transform: suspend (Note) -> Note,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val notes = notes
            .map { transform(it) }
            .toTypedArray()

        noteRepository.updateNotes(*notes)
    }

    private inline fun Array<out Note>.forEachAsync(crossinline block: suspend CoroutineScope.(Note) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            forEach { block(it) }
        }
    }
}
