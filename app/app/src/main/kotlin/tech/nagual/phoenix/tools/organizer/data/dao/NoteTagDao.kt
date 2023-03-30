package tech.nagual.phoenix.tools.organizer.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tech.nagual.phoenix.tools.organizer.data.model.Note
import tech.nagual.phoenix.tools.organizer.data.model.NoteTagJoin
import tech.nagual.phoenix.tools.organizer.data.model.Tag

@Dao
interface NoteTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg joins: NoteTagJoin)

    @Delete
    suspend fun delete(vararg joins: NoteTagJoin)

    @Query(
        """
        SELECT tags.* FROM tags 
        INNER JOIN note_tags ON tags.id = note_tags.tagId 
        WHERE note_tags.noteId = :noteId
        """
    )
    fun getByNoteId(noteId: Long): Flow<List<Tag>>

    @Query(
        """
        INSERT INTO note_tags (tagId, noteId)
        SELECT tagId, :toNoteId FROM note_tags WHERE noteId = :fromNoteId
        """
    )
    suspend fun copyTags(fromNoteId: Long, toNoteId: Long)

    @Query("SELECT * FROM note_tags")
    fun getAllNoteTagRelations(): Flow<List<NoteTagJoin>>

    @RewriteQueriesToDropUnusedColumns
    @Transaction
    @Query(
        """
        SELECT * FROM notes 
        INNER JOIN note_tags ON notes.id = note_tags.noteId 
        WHERE note_tags.tagId = :tagId
        """
    )
    fun getNotesByTagId(tagId: Long): Flow<List<Note>>
}
