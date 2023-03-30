package tech.nagual.phoenix.tools.organizer.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tech.nagual.phoenix.tools.organizer.data.model.Workflow

@Dao
interface WorkflowDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workflow: Workflow): Long

    @Delete
    suspend fun delete(vararg workflows: Workflow)

    @Update
    suspend fun update(vararg workflows: Workflow)

    @Query("SELECT * FROM workflows WHERE organizerId = :organizerId")
    fun getAll(organizerId: Long): Flow<List<Workflow>>

    @Query("SELECT * FROM workflows WHERE id = :workflowId AND organizerId = :organizerId")
    fun getById(workflowId: Long, organizerId: Long): Flow<Workflow?>

    @Query("SELECT * FROM workflows WHERE name = :name AND organizerId = :organizerId LIMIT 1")
    fun getByName(name: String, organizerId: Long): Flow<Workflow?>
}
