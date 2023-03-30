package tech.nagual.phoenix.tools.organizer.search

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tech.nagual.phoenix.tools.organizer.common.AbstractNotesViewModel
import tech.nagual.phoenix.tools.organizer.data.model.Note
import tech.nagual.phoenix.tools.organizer.data.model.NoteViewType
import tech.nagual.phoenix.tools.organizer.data.model.Folder
import tech.nagual.phoenix.tools.organizer.data.repo.NoteRepository
import tech.nagual.phoenix.tools.organizer.data.repo.FolderRepository
import tech.nagual.phoenix.tools.organizer.preferences.PreferenceRepository
import tech.nagual.phoenix.tools.organizer.preferences.SortMethod
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository,
    preferenceRepository: PreferenceRepository,
) : AbstractNotesViewModel(preferenceRepository) {

    private val searchKeyData: MutableStateFlow<String> = MutableStateFlow("")

    var isFirstLoad = true

    @OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    override val provideNotes = { sortMethod: SortMethod ->
        folderRepository.getAll().distinctUntilChanged().flatMapLatest { notebooks ->
            searchKeyData.debounce(300).flatMapLatest { searchKey ->
                noteRepository
                    .getAll(sortMethod)
                    .map { notes ->
                        getSearchResults(
                            searchKey.trim(),
                            notes,
                            notebooks
                        )
                    }
            }
        }
    }

    private fun getSearchResults(
        searchKey: String,
        notes: List<Note>,
        folders: List<Folder>,
    ): List<Note> = notes.filter { note ->
        fun String.matches(): Boolean = contains(searchKey, true)

        when (note.type) {
            NoteViewType.TaskList -> note.taskList.any { it.content.matches() }
            NoteViewType.Text -> note.content.matches()
            NoteViewType.Categories -> false // TODO
        } ||
                note.title.matches() ||
                note.attachments.any { it.description.matches() } ||
                note.tags.any { it.name.matches() } ||
                folders.any { it.name.matches() && it.id == note.folderId }
    }

    fun setSearchQuery(query: String) = viewModelScope.launch {
        searchKeyData.emit(query)
    }
}
