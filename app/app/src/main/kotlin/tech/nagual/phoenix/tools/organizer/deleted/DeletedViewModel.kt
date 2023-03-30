package tech.nagual.phoenix.tools.organizer.deleted

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tech.nagual.phoenix.tools.organizer.common.AbstractNotesViewModel
import tech.nagual.phoenix.tools.organizer.components.MediaStorageManager
import tech.nagual.phoenix.tools.organizer.data.repo.NoteRepository
import tech.nagual.phoenix.tools.organizer.preferences.PreferenceRepository
import javax.inject.Inject

@HiltViewModel
class DeletedViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val mediaStorageManager: MediaStorageManager,
    preferenceRepository: PreferenceRepository,
) : AbstractNotesViewModel(preferenceRepository) {

    override val provideNotes = noteRepository::getDeleted

    fun permanentlyDeleteNotesInBin() {
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.permanentlyDeleteNotesInBin()
            mediaStorageManager.cleanUpStorage()
        }
    }
}
