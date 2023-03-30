package tech.nagual.phoenix.tools.organizer.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import tech.nagual.phoenix.tools.organizer.data.model.Organizer

@Dao
interface OrganizerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(organizer: Organizer): Long

    @Query("SELECT MAX(ordinal) FROM organizers")
    fun getMaxOrdinal(): Flow<Long?>

    @Transaction
    suspend fun insertWithOrdinal(organizer: Organizer): Long {
        val maxOrdinal = getMaxOrdinal().first()
        val nextOrdinal = if (maxOrdinal == null) 0 else maxOrdinal + 1L
        return insert(
            organizer.copy(
                ordinal = nextOrdinal.toInt()
            )
        )
    }

    @Delete
    suspend fun delete(organizer: Organizer)

    @Transaction
    suspend fun deleteAndFixOrdinals(organizer: Organizer) {
        delete(organizer)
        val organizers = getAllFromOrdinal(organizer.ordinal).first().toMutableList()
        for (i in 0 until organizers.size) {
            val organizer = organizers[i]
            organizers[i] = organizer.copy(ordinal = organizer.ordinal - 1)
        }
        update(*organizers.toTypedArray())
    }

    @Update
    suspend fun update(vararg organizers: Organizer)

    @Query("SELECT * FROM organizers WHERE id = :organizerId")
    fun getById(organizerId: Long): Flow<Organizer?>

    @Query("SELECT * FROM organizers WHERE ordinal = :ordinal")
    fun getByOrdinal(ordinal: Int): Flow<Organizer?>

    @Query("SELECT * FROM organizers WHERE ordinal > :ordinal")
    fun getAllFromOrdinal(ordinal: Int): Flow<List<Organizer>>

    @Query("SELECT * FROM organizers ORDER BY ordinal ASC")
    fun getAll(): Flow<List<Organizer>>

    @Query("SELECT * FROM organizers WHERE name = :organizerName LIMIT 1")
    fun getByName(organizerName: String): Flow<Organizer?>
}
