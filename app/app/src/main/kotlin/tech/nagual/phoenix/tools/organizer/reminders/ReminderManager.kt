package tech.nagual.phoenix.tools.organizer.reminders

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import kotlinx.coroutines.flow.first
import tech.nagual.phoenix.R
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.repo.ReminderRepository

class ReminderManager(
    private val context: Context,
    private val reminderRepository: ReminderRepository,
) {
    private fun requestBroadcast(
        reminderId: Long,
        noteId: Long,
        flag: Int = PendingIntent.FLAG_UPDATE_CURRENT
    ): PendingIntent? {
        val notificationIntent = Intent(context, ReminderReceiver::class.java).apply {
            putExtras(
                bundleOf(
                    "noteId" to noteId,
                    "reminderId" to reminderId,
                )
            )
            action = ReminderReceiver.REMINDER_HAS_FIRED
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            notificationIntent,
            flag or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun isReminderSet(reminderId: Long, noteId: Long): Boolean {
        return requestBroadcast(reminderId, noteId, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) != null
    }

    fun schedule(reminderId: Long, dateTime: Long, noteId: Long) {
        val alarmManager =
            ContextCompat.getSystemService(context, AlarmManager::class.java) ?: return
        val broadcast = requestBroadcast(reminderId, noteId) ?: return

        cancel(reminderId, noteId, keepIntent = true)
        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            dateTime * 1000, // convert seconds to millis
            broadcast
        )
    }

    fun cancel(reminderId: Long, noteId: Long, keepIntent: Boolean = false) {
        val alarmManager =
            ContextCompat.getSystemService(context, AlarmManager::class.java) ?: return
        val broadcast = requestBroadcast(reminderId, noteId, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        alarmManager.cancel(broadcast)
        if (!keepIntent) broadcast.cancel()
    }

    suspend fun cancelAllRemindersForNote(noteId: Long) {
        val reminders = reminderRepository.getByNoteId(noteId).first()
        reminders.forEach { cancel(it.id, noteId) }
    }

    suspend fun rescheduleAll() {
        reminderRepository
            .getAll()
            .first()
            .forEach { reminder ->
                if (reminder.hasExpired()) {
                    reminderRepository.deleteById(reminder.id)
                    return@forEach
                }
                schedule(reminder.id, reminder.date, reminder.noteId)
            }
    }

    suspend fun sendNotification(reminderId: Long, noteId: Long) {
        val notificationManager =
            ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return

        var notificationTitle = ""

        reminderRepository.getById(reminderId).first()?.let { notificationTitle = it.name }
        reminderRepository.deleteById(reminderId)

        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.organizer_nav_graph)
            .setDestination(R.id.organizer_editor_fragment)
            .setArguments(
                bundleOf(
                    "noteId" to noteId,
                    "transitionName" to ""
                )
            )
            .createPendingIntent()

        val notification =
            NotificationCompat.Builder(context, OrganizersManager.REMINDERS_CHANNEL_ID)
                .setContentText(notificationTitle)
                .setContentTitle(context.getString(R.string.notification_reminder_fired))
                .setSmallIcon(R.drawable.organizer_icon)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

        notificationManager.notify(reminderId.toInt(), notification)
    }
}
