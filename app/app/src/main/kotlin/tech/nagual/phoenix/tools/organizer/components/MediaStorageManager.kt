package tech.nagual.phoenix.tools.organizer.components

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import tech.nagual.phoenix.BuildConfig
import tech.nagual.phoenix.tools.organizer.attachments.getAttachmentUri
import tech.nagual.phoenix.tools.organizer.data.repo.NoteRepository
import java.io.File

class MediaStorageManager(
    private val context: Context,
    private val noteRepository: NoteRepository,
    private val mediaFolder: String,
) {
    val directory
        get() = File(context.filesDir, mediaFolder)
            .also { it.mkdir() }

    fun listMediaFiles(): List<String> {
        return directory
            .list()
            .orEmpty()
            .toList()
    }

    fun deleteAllMedia() {
        directory.deleteRecursively()
    }

    suspend fun cleanUpStorage() = runCatching {
        val filesUsed = noteRepository
            .getAll()
            .first()
            .flatMap { it.attachments }
            .map { it.path }

        val files = directory
            .list()
            .orEmpty()

        for (file in files) {
            if (getAttachmentUri(context, file, mediaFolder).toString() !in filesUsed) {
                File(directory, file).delete()
            }
        }
    }

    /***
     * Creates a media file in local storage.
     *
     * @return The file's [Uri] and [File] objects.
     */
    suspend fun createMediaFile(
        type: MediaType,
        extension: String = type.defaultExtension
    ): Pair<Uri, File>? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val prefix = when (type) {
                    MediaType.IMAGE -> "IMG_"
                    MediaType.VIDEO -> "VID_"
                    MediaType.AUDIO -> "AUD_"
                }

                val file = File.createTempFile(prefix, extension, directory)
                FileProvider.getUriForFile(
                    context,
                    "${BuildConfig.APPLICATION_ID}.provider",
                    file
                ) to file
            }.getOrNull()
        }
    }

    enum class MediaType(val defaultExtension: String) {
        IMAGE(".jpg"), AUDIO(".mp3"), VIDEO(".mp4");
    }
}
