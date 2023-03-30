package tech.nagual.phoenix.tools.organizer.data.repo

import kotlinx.coroutines.flow.Flow
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.dao.NoteTagDao
import tech.nagual.phoenix.tools.organizer.data.dao.TagDao
import tech.nagual.phoenix.tools.organizer.data.model.NoteTagJoin
import tech.nagual.phoenix.tools.organizer.data.model.Tag

class TagRepository(
    private val tagDao: TagDao,
    private val noteTagDao: NoteTagDao
) {

    fun getAll(): Flow<List<Tag>> {
        return tagDao.getAll(OrganizersManager.activeOrganizer.id)
    }

    fun getById(tagId: Long): Flow<Tag?> {
        return tagDao.getById(tagId, OrganizersManager.activeOrganizer.id)
    }

    fun getByNoteId(noteId: Long): Flow<List<Tag>> {
        return noteTagDao.getByNoteId(noteId)
    }

    fun getByName(name: String): Flow<Tag?> {
        return tagDao.getByName(name, OrganizersManager.activeOrganizer.id)
    }

    suspend fun insert(tag: Tag): Long {
        return tagDao.insert(tag)
    }

    suspend fun delete(vararg tags: Tag) {
        tagDao.delete(*tags)
    }

    suspend fun update(vararg tags: Tag) {
        tagDao.update(*tags)
    }

    suspend fun addTagToNote(tagId: Long, noteId: Long) {
        noteTagDao.insert(NoteTagJoin(tagId, noteId))
    }

    suspend fun deleteTagFromNote(tagId: Long, noteId: Long) {
        noteTagDao.delete(NoteTagJoin(tagId, noteId))
    }

    suspend fun copyTags(fromNoteId: Long, toNoteId: Long) {
        noteTagDao.copyTags(fromNoteId, toNoteId)
    }
}
