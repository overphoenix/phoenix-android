package tech.nagual.phoenix.tools.organizer.backup

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import tech.nagual.common.R
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.model.Note
import tech.nagual.phoenix.tools.organizer.preferences.PreferenceRepository
import javax.inject.Inject

@AndroidEntryPoint
class BackupService : LifecycleService() {
    private var nextId = 0
        get() {
            field += 1
            return "backup_$field".hashCode()
        }

    private val jobs = mutableListOf<Job>()

    @Inject
    lateinit var preferenceRepository: PreferenceRepository

    @Inject
    lateinit var backupManager: BackupManager

    private var notificationManager: NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.let { intent ->
            val action = intent.extras?.getSerializable(ACTION) as? Action ?: return@let
            val uri = intent.extras?.getParcelable<Uri>(URI_EXTRA) ?: return@let

            when (action) {
                Action.RESTORE -> {
                    import(uri)
                }
                Action.BACKUP -> {
                    val notes = intent.extras?.getParcelableArrayList<Note>(NOTES)?.toSet()
                    val exportType = ExportType.values()[intent.extras?.getInt("exportType")!!]
                    val withoutAttachments: Boolean =
                        intent.extras?.getBoolean("withoutAttachments")!!
                    export(notes, exportType, withoutAttachments, uri)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.cancel()
    }

    private fun startJob(block: suspend CoroutineScope.() -> Unit) {
        val job = lifecycleScope.launch(Dispatchers.IO, block = block)
        job.invokeOnCompletion {
            if (jobs.all { it.isCompleted }) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                stopSelf()
            }
            jobs.remove(job)
        }
        jobs.add(job)
    }

    /**
     * Backs up the specific [notes] to a file with URI [outputUri] or all notes if [notes] is null.
     */
    private fun export(
        notes: Set<Note>? = null,
        exportType: ExportType,
        withoutAttachments: Boolean,
        outputUri: Uri
    ) = startJob {
        val handler = when (withoutAttachments) {
            true -> AttachmentHandler.KeepNothing
            false -> AttachmentHandler.IncludeFiles(applicationContext)
        }

        val backup = backupManager.createBackup(
            notes = notes,
            attachmentHandler = handler,
            exportType
        )

        val notificationId = nextId
        val notificationBuilder =
            NotificationCompat.Builder(applicationContext, OrganizersManager.BACKUPS_CHANNEL_ID)
                .setOnlyAlertOnce(true)
                .setContentTitle(getString(R.string.organizer_notification_exporting))
                .setSmallIcon(R.drawable.organizer_notification_backup_icon)
                .setOngoing(true)

        val progressHandler = object : ProgressHandler {
            override fun onProgressChanged(current: Int, max: Int) {
                notificationManager?.notify(
                    notificationId,
                    notificationBuilder.setProgress(max, current, false).build()
                )
            }

            override fun onCompletion() {
                notificationManager?.notify(
                    notificationId,
                    notificationBuilder
                        .setContentTitle(getString(R.string.organizer_notification_export_complete))
                        .setProgress(0, 0, false)
                        .setAutoCancel(true)
                        .setOngoing(false)
                        .build()
                )
            }

            override fun onFailure(e: Throwable) {
                notificationManager?.notify(
                    notificationId,
                    notificationBuilder
                        .setContentTitle(getString(R.string.organizer_notification_export_failed))
                        .setProgress(0, 0, false)
                        .setAutoCancel(true)
                        .setOngoing(false)
                        .build()
                )
            }
        }

        startForeground(notificationId, notificationBuilder.build())

        backupManager.createBackupZipFile(
            backup.serialize(),
            handler,
            outputUri,
            progressHandler
        )
    }

    private fun import(backupUri: Uri) = startJob {
        val notificationId = nextId
        val notificationBuilder =
            NotificationCompat.Builder(applicationContext, OrganizersManager.BACKUPS_CHANNEL_ID)
                .setOnlyAlertOnce(true)
                .setContentTitle(getString(R.string.organizer_notification_importing_notes))
                .setSmallIcon(R.drawable.organizer_notification_backup_icon)
                .setOngoing(true)
        val notification = notificationBuilder.build()

        startForeground(notificationId, notification)

        val backup = backupManager.backupFromZipFile(backupUri, DefaultMigrationHandler()).fold(
            onSuccess = { it },
            onFailure = {
                val notification = notificationBuilder
                    .setContentTitle(getString(R.string.organizer_notification_import_failed))
                    .setContentText(it.message)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .build()

                notificationManager?.notify(notificationId, notification)
                return@startJob
            },
        )

        backupManager.restoreNotesFromBackup(backup)

        notificationManager?.notify(
            notificationId,
            notificationBuilder
                .setContentTitle(getString(R.string.organizer_notification_import_complete))
                .setOngoing(false)
                .setAutoCancel(true)
                .build()
        )
    }

    enum class Action {
        RESTORE,
        BACKUP,
    }

    enum class ExportType {
        EXPORT_ALL,
        EXPORT_ONLY_NOTES,
        EXPORT_ONLY_CATEGORIES
    }

    companion object {
        private const val URI_EXTRA = "URI_EXTRA"
        private const val NOTES = "NOTES"
        private const val ACTION = "ACTION"

        fun import(context: Context, backupUri: Uri) {
            Intent(context, BackupService::class.java).also { intent ->
                intent.putExtra(ACTION, Action.RESTORE)
                intent.putExtra(URI_EXTRA, backupUri)
                ContextCompat.startForegroundService(context, intent)
            }
        }

        fun export(
            context: Context,
            notes: Set<Note>? = null,
            exportType: ExportType,
            withoutAttachments: Boolean,
            outputUri: Uri
        ) {
            Intent(context, BackupService::class.java).also { intent ->
                intent.putExtra(ACTION, Action.BACKUP)
                if (notes != null) intent.putParcelableArrayListExtra(NOTES, ArrayList(notes))
                intent.putExtra(URI_EXTRA, outputUri)
                intent.putExtra("exportType", exportType.ordinal)
                intent.putExtra("withoutAttachments", withoutAttachments)
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }
}
