package tech.nagual.phoenix.tools.organizer.data.dao

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.flow.Flow
import tech.nagual.phoenix.tools.organizer.data.model.*
import tech.nagual.phoenix.tools.organizer.preferences.SortMethod
import tech.nagual.phoenix.tools.organizer.preferences.SortMethod.*

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(vararg notes: NoteEntity)

    @Delete
    suspend fun delete(vararg notes: NoteEntity)

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND organizerId = :organizerId")
    suspend fun permanentlyDeleteNotesInBin(organizerId: Long)

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId AND organizerId = :organizerId")
    fun getById(noteId: Long, organizerId: Long): Flow<Note?>

    @Transaction
    @RawQuery(
        observedEntities = [
            NoteEntity::class,
            Tag::class,
            Reminder::class,
            NoteTagJoin::class,
        ]
    )
    fun rawGetQuery(query: SimpleSQLiteQuery): Flow<List<Note>>

    fun getDeleted(sortMethod: SortMethod, organizerId: Long): Flow<List<Note>> {
        val (column, order) = getOrderByMethod(sortMethod)
        return rawGetQuery(
            SimpleSQLiteQuery(
                """
                SELECT * FROM notes WHERE isDeleted = 1 AND organizerId = $organizerId
                ORDER BY isPinned DESC, $column $order
            """
            )
        )
    }

    fun getArchived(sortMethod: SortMethod, organizerId: Long): Flow<List<Note>> {
        val (column, order) = getOrderByMethod(sortMethod)
        return rawGetQuery(
            SimpleSQLiteQuery(
                """
                SELECT * FROM notes WHERE isArchived = 1 AND isDeleted = 0 AND organizerId = $organizerId 
                ORDER BY isPinned DESC, $column $order
            """
            )
        )
    }

    fun getNonDeleted(sortMethod: SortMethod, organizerId: Long): Flow<List<Note>> {
        val (column, order) = getOrderByMethod(sortMethod)
        return rawGetQuery(
            SimpleSQLiteQuery(
                """
                SELECT * FROM notes WHERE isDeleted = 0 AND organizerId = $organizerId
                ORDER BY isPinned DESC, $column $order
            """
            )
        )
    }

    fun getNonDeletedOrArchived(sortMethod: SortMethod, organizerId: Long): Flow<List<Note>> {
        val (column, order) = getOrderByMethod(sortMethod)
        return rawGetQuery(
            SimpleSQLiteQuery(
                """
                SELECT * FROM notes WHERE isArchived = 0 AND isDeleted = 0 AND organizerId = $organizerId 
                ORDER BY isPinned DESC, $column $order
            """
            )
        )
    }

    fun getAll(sortMethod: SortMethod, organizerId: Long): Flow<List<Note>> {
        val (column, order) = getOrderByMethod(sortMethod)
        return rawGetQuery(
            SimpleSQLiteQuery(
                """
                SELECT * FROM notes WHERE organizerId = $organizerId
                ORDER BY isPinned DESC, $column $order
            """
            )
        )
    }

    fun getByFolder(
        folderId: Long,
        sortMethod: SortMethod,
        organizerId: Long
    ): Flow<List<Note>> {
        val (column, order) = getOrderByMethod(sortMethod)
        return rawGetQuery(
            SimpleSQLiteQuery(
                """
                SELECT * FROM notes WHERE isArchived = 0 AND isDeleted = 0 AND folderId = $folderId AND organizerId = $organizerId 
                ORDER BY isPinned DESC, $column $order
            """
            )
        )
    }

    private fun getOrderByMethod(sortMethod: SortMethod): Pair<String, String> {
        val column = when (sortMethod) {
            TITLE_ASC, TITLE_DESC -> "title"
            CREATION_ASC, CREATION_DESC -> "creationDate"
            MODIFIED_ASC, MODIFIED_DESC -> "modifiedDate"
        }
        val order = when (sortMethod) {
            TITLE_ASC, CREATION_ASC, MODIFIED_ASC -> "ASC"
            TITLE_DESC, CREATION_DESC, MODIFIED_DESC -> "DESC"
        }
        return Pair(column, order)
    }

    fun getNotesWithoutFolder(sortMethod: SortMethod, organizerId: Long): Flow<List<Note>> {
        val (column, order) = getOrderByMethod(sortMethod)
        return rawGetQuery(
            SimpleSQLiteQuery(
                """
                SELECT * FROM notes WHERE isArchived = 0 AND isDeleted = 0 AND folderId IS NULL AND organizerId = $organizerId  
                ORDER BY isPinned DESC, $column $order
            """
            )
        )
    }
}
