package tech.nagual.phoenix.tools.organizer.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tech.nagual.phoenix.tools.organizer.data.model.Tag

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag): Long

    @Update
    suspend fun update(vararg tags: Tag)

    @Delete
    suspend fun delete(vararg tags: Tag)

    @Query("SELECT * FROM tags WHERE organizerId = :organizerId")
    fun getAll(organizerId: Long): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE id = :tagId AND organizerId = :organizerId")
    fun getById(tagId: Long, organizerId: Long): Flow<Tag?>

    @Query("SELECT * FROM tags WHERE name = :name AND organizerId = :organizerId LIMIT 1")
    fun getByName(name: String, organizerId: Long): Flow<Tag?>
}
