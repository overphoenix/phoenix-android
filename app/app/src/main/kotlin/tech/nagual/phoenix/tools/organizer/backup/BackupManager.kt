package tech.nagual.phoenix.tools.organizer.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.first
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.Backup
import tech.nagual.phoenix.tools.organizer.data.model.*
import tech.nagual.phoenix.tools.organizer.data.repo.*
import tech.nagual.phoenix.tools.organizer.reminders.ReminderManager
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val currentVersion: Int,
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository,
    private val tagRepository: TagRepository,
    private val categoriesRepository: CategoriesRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderManager: ReminderManager,
    private val context: Context,
) {

    private val BUFFER = 4096

    /**
     * Creates a backup which contains [notes] or the whole database if [notes] is null.
     */
    suspend fun createBackup(
        notes: Set<Note>?,
        attachmentHandler: AttachmentHandler,
        exportType: BackupService.ExportType
    ): Backup {
        val withNotes = exportType == BackupService.ExportType.EXPORT_ALL ||
                exportType == BackupService.ExportType.EXPORT_ONLY_NOTES
        val withCategories =
            exportType == BackupService.ExportType.EXPORT_ALL || exportType == BackupService.ExportType.EXPORT_ONLY_CATEGORIES
        val notes: Set<Note>? = when (withNotes) {
            true -> notes ?: noteRepository.getAll().first().toSet()
            false -> null
        }

        var folders: Set<Folder>? = null
        var reminders: Set<Reminder>? = null
        var tags: Set<Tag>? = null
        var joins: Set<NoteTagJoin>? = null

        val categories: Set<Category>? =
            if (withCategories) categoriesRepository.getAll().first().toSet() else null
        val categoryVars: Set<CategoryVariable>? =
            if (withCategories) categoriesRepository.getAllVariables().first().toSet() else null
        val variants: Set<Variant>? =
            if (withCategories) categoriesRepository.getAllVariants().first().toSet() else null

        val newNotes = if (notes != null) {
            folders = mutableSetOf()
            reminders = mutableSetOf()
            tags = mutableSetOf()
            joins = mutableSetOf()

            notes.map { note ->
                note.folderId?.let { folderId ->
                    val folder = folderRepository.getById(folderId).first() ?: return@let
                    folders.add(folder)
                }

                val noteReminders = reminderRepository.getByNoteId(note.id).first()
                reminders.addAll(noteReminders)

                val noteTags = tagRepository.getByNoteId(note.id).first()
                tags.addAll(noteTags)

                val noteTagJoins = noteTags.map { tag -> NoteTagJoin(tag.id, note.id) }
                joins.addAll(noteTagJoins)

                val newAttachments = mutableListOf<Attachment>()
                note.attachments.forEach { old ->
                    attachmentHandler
                        .handle(old)
                        ?.let { newAttachments.add(it) }
                }
                note.copy(attachments = newAttachments)
            }.toSet()
        } else null

        return Backup(
            currentVersion,
            newNotes,
            if (folders.isNullOrEmpty()) null else folders,
            if (reminders.isNullOrEmpty()) null else reminders,
            if (tags.isNullOrEmpty()) null else tags,
            if (joins.isNullOrEmpty()) null else joins,
            if (categories.isNullOrEmpty()) null else categories,
            if (categoryVars.isNullOrEmpty()) null else categoryVars,
            if (variants.isNullOrEmpty()) null else variants
        )
    }

    suspend fun restoreNotesFromBackup(backup: Backup) {
//        val foldersMap = mutableMapOf<Long, Long>()
//        val tagsMap = mutableMapOf<Long, Long>()
//        val notesMap = mutableMapOf<Long, Long>()
//
//        backup.folders.forEach { folder ->
//            val existingFolder = folderRepository.getByName(folder.name).firstOrNull()
//            if (existingFolder != null) {
//                foldersMap[folder.id] = existingFolder.id
//            } else {
//                foldersMap[folder.id] = folderRepository.insert(folder.copy(id = 0L))
//            }
//        }
//
//        backup.tags.forEach { tag ->
//            val existingTag = tagRepository.getByName(tag.name).firstOrNull()
//            if (existingTag != null) {
//                tagsMap[tag.id] = existingTag.id
//                return@forEach
//            }
//            tagsMap[tag.id] = tagRepository.insert(tag.copy(id = 0L))
//        }
//
//        backup.notes.forEach { note ->
//            val newNote = note.copy(
//                id = 0L,
//                folderId = foldersMap[note.folderId],
//                attachments = note.attachments.map { attachment ->
//                    if (attachment.fileName.isNotEmpty()) {
//                        attachment.copy(
//                            path = getAttachmentUri(context, attachment.fileName).toString(),
//                            fileName = ""
//                        )
//                    } else {
//                        attachment
//                    }
//                }
//            )
//            notesMap[note.id] = noteRepository.insertNote(newNote)
//        }
//
//        backup.joins.forEach { join ->
//            val tagId = tagsMap[join.tagId] ?: return@forEach
//            val noteId = notesMap[join.noteId] ?: return@forEach
//            tagRepository.addTagToNote(tagId, noteId)
//        }
//
//        backup.reminders.forEach { reminder ->
//            if (reminder.hasExpired()) return@forEach
//
//            val noteId = notesMap[reminder.noteId] ?: return@forEach
//            val reminderId = reminderRepository.insert(reminder.copy(id = 0L, noteId = noteId))
//            reminderManager.schedule(
//                reminderId = reminderId,
//                noteId = noteId,
//                dateTime = reminder.date
//            )
//        }
    }

    fun backupFromZipFile(
        uri: Uri,
        migrationHandler: MigrationHandler,
    ): Result<Backup> = runCatching {
        var backup: Backup? = null
        val nameMap = mutableMapOf<String, String>()

        ZipInputStream(BufferedInputStream(context.contentResolver.openInputStream(uri))).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                when (entry.name) {
                    "backup.json" -> {
                        // Create backup class
                        val builder = StringBuilder()
                        val buffer = ByteArray(BUFFER)
                        var length = 0
                        while (input.read(buffer).also { length = it } > 0) {
                            builder.append(String(buffer, 0, length))
                        }
                        val deserialized = migrationHandler.migrate(builder.toString())

                        backup = Backup.fromString(deserialized)
                    }

                    "${OrganizersManager.MEDIA_FOLDER}/" -> {
                        // Ignore directory
                        continue
                    }

                    // Copy media files to local storage
                    else -> {
                        val dir = File(
                            context.filesDir,
                            OrganizersManager.MEDIA_FOLDER
                        ).also { it.mkdirs() }

                        var fileId = 1
                        val originalName = entry.name.split("/").last()
                        val generateFilename = { "${fileId}_$originalName".also { fileId += 1 } }
                        var fileName = originalName

                        // If a file with the same name already exists in app storage
                        // give the new file a new name and loop until it is unique
                        do {
                            val exists = dir.listFiles()?.any { it.name == fileName } == true
                            if (exists) {
                                fileName = generateFilename()
                                // Map the old name to the new name so we can change the notes later to use the new name
                                nameMap[originalName] = fileName
                            }
                        } while (exists)

                        FileOutputStream(File(dir, fileName)).use { out ->
                            val buffer = ByteArray(BUFFER)
                            var length = 0
                            while (input.read(buffer).also { length = it } > 0) {
                                out.write(buffer, 0, length)
                            }
                        }
                        input.closeEntry()
                    }
                }
            }
        }

        backup.run {
            if (this == null || nameMap.isEmpty()) return@run this
            val newNotes = notes
                ?.map { note ->
                    val newAttachments = note.attachments.map { attachment ->
                        attachment.copy(
                            fileName = nameMap[attachment.fileName] ?: attachment.fileName
                        )
                    }
                    note.copy(attachments = newAttachments)
                }
                ?.toSet()

            copy(notes = newNotes)
        } ?: throw IOException()
    }

    fun createBackupZipFile(
        noteJson: String,
        handler: AttachmentHandler,
        uri: Uri,
        progressHandler: ProgressHandler,
    ) {
        runCatching {
            ZipOutputStream(BufferedOutputStream(context.contentResolver.openOutputStream(uri))).use { out ->
                var current = 0
                var max = 1

                if (handler is AttachmentHandler.IncludeFiles) {
                    val attachments = handler.attachmentsMap
                    max = attachments.size + 1

                    if (attachments.isNotEmpty()) out.putNextEntry(ZipEntry("${OrganizersManager.MEDIA_FOLDER}/"))
                    for ((fileName, inputUri) in attachments) {
                        progressHandler.onProgressChanged(++current, max)

                        out.putNextEntry(ZipEntry("${OrganizersManager.MEDIA_FOLDER}/$fileName"))
                        context.contentResolver.openInputStream(inputUri)?.use { input ->
                            input.copyTo(out, BUFFER)
                        }
                    }
                }

                progressHandler.onProgressChanged(++current, max)
                out.putNextEntry(ZipEntry("backup.json"))
                out.write(noteJson.toByteArray())
                out.finish()
            }
        }.fold(
            onSuccess = { progressHandler.onCompletion() },
            onFailure = { progressHandler.onFailure(it) }
        )
    }
}
