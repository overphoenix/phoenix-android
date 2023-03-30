package tech.nagual.phoenix.tools.organizer.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tech.nagual.phoenix.tools.organizer.data.model.Folder

@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: Folder): Long

    @Delete
    suspend fun delete(vararg folders: Folder)

    @Update
    suspend fun update(vararg folders: Folder)

    @Query("SELECT * FROM folders WHERE organizerId = :organizerId")
    fun getAll(organizerId: Long): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :folderId AND organizerId = :organizerId")
    fun getById(folderId: Long, organizerId: Long): Flow<Folder?>

    @Query("SELECT * FROM folders WHERE folderName = :name AND organizerId = :organizerId LIMIT 1")
    fun getByName(name: String, organizerId: Long): Flow<Folder?>
}
