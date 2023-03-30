package tech.nagual.phoenix.tools.organizer.data.repo

import kotlinx.coroutines.flow.Flow
import tech.nagual.phoenix.tools.organizer.data.dao.ReminderDao
import tech.nagual.phoenix.tools.organizer.data.model.Reminder

class ReminderRepository(private val reminderDao: ReminderDao) {

    fun getAll(): Flow<List<Reminder>> {
        return reminderDao.getAll()
    }

    fun getByNoteId(noteId: Long): Flow<List<Reminder>> {
        return reminderDao.getByNoteId(noteId)
    }

    fun getById(reminderId: Long): Flow<Reminder?> {
        return reminderDao.getById(reminderId)
    }

    suspend fun insert(reminder: Reminder): Long {
        return reminderDao.insert(reminder)
    }

    suspend fun update(vararg reminders: Reminder) {
        reminderDao.update(*reminders)
    }

    suspend fun deleteById(id: Long) {
        reminderDao.deleteById(id)
    }

    suspend fun deleteByNoteId(noteId: Long) {
        reminderDao.deleteByNoteId(noteId)
    }

    suspend fun copyReminders(fromNoteId: Long, toNoteId: Long) {
        reminderDao.copyReminders(fromNoteId, toNoteId)
    }
}
