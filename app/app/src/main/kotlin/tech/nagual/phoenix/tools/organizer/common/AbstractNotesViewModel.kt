package tech.nagual.phoenix.tools.organizer.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import tech.nagual.common.preferences.datastore.defaultOf
import tech.nagual.phoenix.tools.organizer.data.model.Note
import tech.nagual.phoenix.tools.organizer.preferences.LayoutMode
import tech.nagual.phoenix.tools.organizer.preferences.NoteDeletionTime
import tech.nagual.phoenix.tools.organizer.preferences.SortMethod
import tech.nagual.phoenix.tools.organizer.preferences.getAllForOrganizer
import tech.nagual.phoenix.tools.organizer.preferences.PreferenceRepository

abstract class AbstractNotesViewModel(
    val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    protected abstract val provideNotes: (SortMethod) -> Flow<List<Note>>

    @OptIn(ExperimentalCoroutinesApi::class)
    val data = preferenceRepository.getAllForOrganizer()
        .flatMapLatest { prefs ->
            provideNotes(prefs.sortMethod).map { notes ->
                Data(notes, prefs.sortMethod, prefs.layoutMode, prefs.noteDeletionTime.toDays())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Data())

    data class Data(
        val notes: List<Note> = emptyList(),
        val sortMethod: SortMethod = defaultOf(),
        val layoutMode: LayoutMode = defaultOf(),
        val noteDeletionTimeInDays: Long = defaultOf<NoteDeletionTime>().toDays(),
    )
}
