package tech.nagual.phoenix.tools.organizer.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tech.nagual.phoenix.tools.organizer.data.model.CategoryVariable

@Dao
interface CategoryVariableDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(variable: CategoryVariable): Long

    @Delete
    suspend fun delete(vararg variable: CategoryVariable)

//    @Update
//    suspend fun update(vararg variable: CategoryVariable)

    @Query("DELETE FROM category_variables WHERE categoryId = :categoryId AND name = :name")
    suspend fun deleteByName(categoryId: Long, name: String)

    @Query("SELECT * FROM category_variables WHERE categoryId = :categoryId AND name = :name LIMIT 1")
    fun getByName(categoryId: Long, name: String): Flow<CategoryVariable?>

    @Query("SELECT * FROM category_variables")
    fun getAll(): Flow<List<CategoryVariable>>

    @Query("SELECT * FROM category_variables WHERE categoryId = :categoryId")
    fun getAllForCategory(categoryId: Long): Flow<List<CategoryVariable>>
}
