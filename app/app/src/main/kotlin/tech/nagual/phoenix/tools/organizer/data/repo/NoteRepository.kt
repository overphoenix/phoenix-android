package tech.nagual.phoenix.tools.organizer.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import tech.nagual.common.preferences.datastore.defaultOf
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.dao.NoteDao
import tech.nagual.phoenix.tools.organizer.data.dao.ReminderDao
import tech.nagual.phoenix.tools.organizer.data.model.Note
import tech.nagual.phoenix.tools.organizer.preferences.SortMethod
import java.time.Instant

class NoteRepository(
    private val noteDao: NoteDao,
    private val reminderDao: ReminderDao,
) {

    suspend fun insertNote(note: Note): Long {
        return noteDao.insert(note.toEntity())
    }

    suspend fun updateNotes(vararg notes: Note) {
        val array = notes
            .map { it.toEntity() }
            .toTypedArray()
        noteDao.update(*array)
    }

    suspend fun moveNotesToBin(vararg notes: Note) {
        val array = notes
            .map { it.toEntity().copy(isDeleted = true, deletionDate = Instant.now().epochSecond) }
            .toTypedArray()
        noteDao.update(*array)

        reminderDao.deleteIfNoteIdIn(notes.map { it.id })
    }

    suspend fun restoreNotes(vararg notes: Note) {
        val array = notes
            .map { it.toEntity().copy(isDeleted = false, deletionDate = null) }
            .toTypedArray()
        noteDao.update(*array)
    }

    suspend fun deleteNotes(vararg notes: Note) {
        val array = notes
            .map { it.toEntity() }
            .toTypedArray()
        noteDao.delete(*array)
    }

    suspend fun discardEmptyNotes(): Boolean {
        val notes = noteDao.getAll(defaultOf(), OrganizersManager.activeOrganizer.id)
            .first()
            .filter { it.isEmpty() }
            .toTypedArray()

        deleteNotes(*notes)

        return notes.isNotEmpty()
    }

    suspend fun permanentlyDeleteNotesInBin() {
        val organizerId = OrganizersManager.activeOrganizer.id
//        val noteIds = noteDao.getDeleted(defaultOf(), organizerId)
//            .first()
//            .map { it.id }
//            .toLongArray()

        noteDao.permanentlyDeleteNotesInBin(organizerId)
    }

    fun getById(noteId: Long): Flow<Note?> {
        return noteDao.getById(noteId, OrganizersManager.activeOrganizer.id)
    }

    fun getDeleted(sortMethod: SortMethod = defaultOf()): Flow<List<Note>> {
        return noteDao.getDeleted(sortMethod, OrganizersManager.activeOrganizer.id)
    }

    fun getArchived(sortMethod: SortMethod = defaultOf()): Flow<List<Note>> {
        return noteDao.getArchived(sortMethod, OrganizersManager.activeOrganizer.id)
    }

    fun getNonDeleted(sortMethod: SortMethod = defaultOf()): Flow<List<Note>> {
        return noteDao.getNonDeleted(sortMethod, OrganizersManager.activeOrganizer.id)
    }

    fun getNonDeletedOrArchived(sortMethod: SortMethod = defaultOf()): Flow<List<Note>> {
        return noteDao.getNonDeletedOrArchived(sortMethod, OrganizersManager.activeOrganizer.id)
    }

    fun getAll(sortMethod: SortMethod = defaultOf()): Flow<List<Note>> {
        return noteDao.getAll(sortMethod, OrganizersManager.activeOrganizer.id)
    }

    fun getByFolder(folderId: Long, sortMethod: SortMethod = defaultOf()): Flow<List<Note>> {
        return noteDao.getByFolder(folderId, sortMethod, OrganizersManager.activeOrganizer.id)
    }

    fun getNotesWithoutFolder(sortMethod: SortMethod = defaultOf()): Flow<List<Note>> {
        return noteDao.getNotesWithoutFolder(sortMethod, OrganizersManager.activeOrganizer.id)
    }
}
