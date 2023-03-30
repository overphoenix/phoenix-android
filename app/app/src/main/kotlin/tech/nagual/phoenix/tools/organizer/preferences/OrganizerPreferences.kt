package tech.nagual.phoenix.tools.organizer.preferences

import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import tech.nagual.common.preferences.datastore.defaultOf
import tech.nagual.common.preferences.datastore.getEnum
import java.io.IOException

data class OrganizerPreferences(
    val defaultNoteType: DefaultNoteType = defaultOf(),
    val layoutMode: LayoutMode = defaultOf(),
    val sortMethod: SortMethod = defaultOf(),
    val dateFormat: DateFormat = defaultOf(),
    val timeFormat: TimeFormat = defaultOf(),
    val showDate: ShowDate = defaultOf(),
    val groupUnassignedNotes: GroupNotesWithoutNotebook = defaultOf(),
    val noteDeletionTime: NoteDeletionTime = defaultOf(),
)

fun PreferenceRepository.getAllForOrganizer(): Flow<OrganizerPreferences> = dataStore.data
    .catch {
        if (it is IOException) {
            emit(emptyPreferences())
        } else {
            throw it
        }
    }
    .map { prefs ->
        OrganizerPreferences(
            defaultNoteType = prefs.getEnum(),
            layoutMode = prefs.getEnum(),
            sortMethod = prefs.getEnum(),
            noteDeletionTime = prefs.getEnum(),
            dateFormat = prefs.getEnum(),
            timeFormat = prefs.getEnum(),
            showDate = prefs.getEnum(),
            groupUnassignedNotes = prefs.getEnum(),
        )
    }
