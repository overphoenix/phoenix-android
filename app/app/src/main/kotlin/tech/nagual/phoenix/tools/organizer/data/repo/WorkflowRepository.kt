package tech.nagual.phoenix.tools.organizer.data.repo

import kotlinx.coroutines.flow.Flow
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.dao.WorkflowDao
import tech.nagual.phoenix.tools.organizer.data.model.Workflow

class WorkflowRepository(
    private val workflowDao: WorkflowDao
) {
    suspend fun insert(workflow: Workflow): Long {
        return workflowDao.insert(workflow)
    }

    suspend fun delete(vararg workflows: Workflow) {
        workflowDao.delete(*workflows)
    }

    suspend fun update(vararg workflows: Workflow) {
        workflowDao.update(*workflows)
    }

    fun getAll(): Flow<List<Workflow>> {
        return workflowDao.getAll(OrganizersManager.activeOrganizer.id)
    }

    fun getById(workflowId: Long): Flow<Workflow?> {
        return workflowDao.getById(workflowId, OrganizersManager.activeOrganizer.id)
    }

    fun getByName(name: String): Flow<Workflow?> {
        return workflowDao.getByName(name, OrganizersManager.activeOrganizer.id)
    }
}