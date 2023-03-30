package tech.nagual.phoenix.tools.organizer.data.repo

import kotlinx.coroutines.flow.Flow
import tech.nagual.phoenix.tools.organizer.data.dao.OrganizerDao
import tech.nagual.phoenix.tools.organizer.data.model.Organizer

class OrganizerRepository(
    private val organizerDao: OrganizerDao
) {

    suspend fun insert(organizer: Organizer): Long {
        return organizerDao.insertWithOrdinal(organizer)
    }

    suspend fun delete(organizer: Organizer) {
        organizerDao.delete(organizer)
    }

    suspend fun deleteAndFixOrdinals(organizer: Organizer) {
        organizerDao.deleteAndFixOrdinals(organizer)
    }

    suspend fun update(vararg organizer: Organizer) {
        organizerDao.update(*organizer)
    }

//    suspend fun swapOrganizers(fromPosition: Int, toPosition: Int) {
//        organizerDao.swapOrganizers(fromPosition, toPosition)
//    }

    fun getById(organizerId: Long): Flow<Organizer?> = organizerDao.getById(organizerId)

    fun getAll(): Flow<List<Organizer>> = organizerDao.getAll()

    fun getByName(name: String): Flow<Organizer?> = organizerDao.getByName(name)
}
