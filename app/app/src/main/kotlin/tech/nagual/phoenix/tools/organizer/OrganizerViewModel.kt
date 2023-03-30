package tech.nagual.phoenix.tools.organizer

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import tech.nagual.common.R
import tech.nagual.phoenix.tools.organizer.common.AbstractNotesViewModel
import tech.nagual.phoenix.tools.organizer.data.repo.NoteRepository
import tech.nagual.phoenix.tools.organizer.data.repo.FolderRepository
import tech.nagual.phoenix.tools.organizer.preferences.PreferenceRepository
import tech.nagual.phoenix.tools.organizer.preferences.SortMethod
import javax.inject.Inject

@HiltViewModel
class OrganizerViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository,
    preferenceRepository: PreferenceRepository,
) : AbstractNotesViewModel(preferenceRepository) {

    private val folderIdFlow: MutableStateFlow<Long?> = MutableStateFlow(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val provideNotes = { sortMethod: SortMethod ->
        folderIdFlow.flatMapLatest { id ->
            when (id) {
                null -> noteRepository.getNonDeletedOrArchived(sortMethod)
                R.id.default_notebook.toLong() -> noteRepository.getNotesWithoutFolder(sortMethod)
                else -> noteRepository.getByFolder(id, sortMethod)
            }
        }
    }

    suspend fun notebookExists(folderId: Long) =
        folderRepository.getById(folderId).firstOrNull() != null

    fun initialize(folderId: Long?) {
        viewModelScope.launch { folderIdFlow.emit(folderId) }
    }
}
