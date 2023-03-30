package tech.nagual.phoenix.tools.organizer.data.repo

import kotlinx.coroutines.flow.Flow
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.dao.FolderDao
import tech.nagual.phoenix.tools.organizer.data.model.Folder

class FolderRepository(
    private val folderDao: FolderDao
) {

    suspend fun insert(folder: Folder): Long {
        return folderDao.insert(folder)
    }

    suspend fun delete(vararg folders: Folder) {
        folderDao.delete(*folders)
    }

    suspend fun update(vararg folders: Folder) {
        folderDao.update(*folders)
    }

    fun getAll(): Flow<List<Folder>> {
        return folderDao.getAll(OrganizersManager.activeOrganizer.id)
    }

    fun getById(folderId: Long): Flow<Folder?> {
        return folderDao.getById(folderId, OrganizersManager.activeOrganizer.id)
    }

    fun getByName(name: String): Flow<Folder?> {
        return folderDao.getByName(name, OrganizersManager.activeOrganizer.id)
    }
}
