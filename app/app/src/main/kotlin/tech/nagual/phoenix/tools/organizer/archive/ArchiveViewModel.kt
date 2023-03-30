package tech.nagual.phoenix.tools.organizer.archive

import dagger.hilt.android.lifecycle.HiltViewModel
import tech.nagual.phoenix.tools.organizer.data.repo.NoteRepository
import tech.nagual.phoenix.tools.organizer.preferences.PreferenceRepository
import tech.nagual.phoenix.tools.organizer.common.AbstractNotesViewModel
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    preferenceRepository: PreferenceRepository,
) : AbstractNotesViewModel(preferenceRepository) {

    override val provideNotes = noteRepository::getArchived
}
