package tech.nagual.phoenix.tools.gps.helpers

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.google.gson.Gson
import tech.nagual.app.application
import tech.nagual.phoenix.tools.gps.Keys
import tech.nagual.phoenix.tools.gps.data.GsonUtil
import tech.nagual.phoenix.tools.gps.data.Track
import tech.nagual.phoenix.tools.gps.data.TrackList
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.text.NumberFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.ln
import kotlin.math.pow

object FileHelper {
    fun getTextFileStream(context: Context, uri: Uri): InputStream? {
        var stream: InputStream? = null
        try {
            stream = context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return stream
    }


    /* Get file size for given Uri */
    fun getFileSize(context: Context, uri: Uri): Long {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            val sizeIndex: Int = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            val size: Long = cursor.getLong(sizeIndex)
            cursor.close()
            return size
        } else {
            return 0L
        }
    }


    /* Get file name for given Uri */
    fun getFileName(context: Context, uri: Uri): String {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            val nameIndex: Int = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            val name: String = cursor.getString(nameIndex)
            cursor.close()
            return name
        } else {
            return String()
        }
    }


    /* Clears given folder - keeps given number of files */
    fun clearFolder(folder: File?, keep: Int, deleteFolder: Boolean = false) {
        if (folder != null && folder.exists()) {
            val files = folder.listFiles()
            val fileCount: Int = files.size
            files.sortBy { it.lastModified() }
            for (fileNumber in files.indices) {
                if (fileNumber < fileCount - keep) {
                    files[fileNumber].delete()
                }
            }
            if (deleteFolder && keep == 0) {
                folder.delete()
            }
        }
    }

    fun readTrackList(): TrackList {
        // get JSON from text file
        val json: String = readTextFile(getTracklistFileUri())
        var tracklist: TrackList = TrackList()
        when (json.isNotBlank()) {
            // convert JSON and return as tracklist
            true -> try {
                tracklist = GsonUtil.getCustomGson().fromJson(json, TrackList::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return tracklist
    }

    fun deleteTempFile(context: Context) {
        getTempTrackFileUri(context).toFile().delete()
    }

    /* Creates Uri for Gpx file of a track */
    fun getGpxFileUri(track: Track): Uri =
        File(application.getExternalFilesDir(Keys.FOLDER_GPX), getGpxFileName(track)).toUri()

    /* Creates file name for Gpx file of a track */
    fun getGpxFileName(track: Track): String =
        DateTimeHelper.convertToSortableDateString(track.recordingStart) + Keys.GPX_FILE_EXTENSION

    fun getTrackFileUri(track: Track): Uri {
        val fileName: String =
            DateTimeHelper.convertToSortableDateString(track.recordingStart) + Keys.TRACKBOOK_FILE_EXTENSION
        return File(application.getExternalFilesDir(Keys.FOLDER_TRACKS), fileName).toUri()
    }

    fun getTempTrackFileUri(context: Context): Uri =
        File(context.getExternalFilesDir(Keys.FOLDER_TEMP), Keys.TEMP_FILE).toUri()

    suspend fun addTrackAndSaveTracklistSuspended(
        track: Track,
        modificationDate: Date = track.recordingStop
    ) {
        return suspendCoroutine { cont ->
            val tracklist: TrackList = readTrackList()
            tracklist.tracks.add(track)
            tracklist.totalDistance += track.length
//            tracklist.totalDuration += track.duration // note: TracklistElement does not contain duration
//            tracklist.totalRecordingPaused += track.recordingPaused // note: TracklistElement does not contain recordingPaused
//            tracklist.totalStepCount += track.stepCount // note: TracklistElement does not contain stepCount
            cont.resume(saveTracklist(tracklist, modificationDate))
        }
    }


    /* Suspend function: Wrapper for saveTracklist */
    suspend fun saveTracklistSuspended(
        tracklist: TrackList,
        modificationDate: Date
    ) {
        return suspendCoroutine { cont ->
            cont.resume(saveTracklist(tracklist, modificationDate))
        }
    }

    /* Suspend function: Deletes tracks that are not starred using deleteTracks */
    suspend fun deleteNonStarredSuspended(tracklist: TrackList): TrackList {
        return suspendCoroutine { cont ->
            val trackListElements = mutableListOf<Track>()
            tracklist.tracks.forEach { track ->
                if (!track.starred) {
                    trackListElements.add(track)
                }
            }
            cont.resume(deleteTracks(trackListElements, tracklist))
        }
    }


    /* Suspend function: Wrapper for readTracklist */
    suspend fun readTracklistSuspended(): TrackList {
        return suspendCoroutine { cont ->
            cont.resume(readTrackList())
        }
    }

    /* Suspend function: Wrapper for copyFile */
    suspend fun saveCopyOfFileSuspended(
        context: Context,
        originalFileUri: Uri,
        targetFileUri: Uri,
        deleteOriginal: Boolean = false
    ) {
        return suspendCoroutine { cont ->
            cont.resume(copyFile(context, originalFileUri, targetFileUri, deleteOriginal))
        }
    }


    /* Saves track tracklist as JSON text file */
    fun saveTracklist(tracklist: TrackList, modificationDate: Date) {
        tracklist.modificationDate = modificationDate
        // convert to JSON
        val gson: Gson = GsonUtil.getCustomGson()
        var json: String = String()
        try {
            json = gson.toJson(tracklist)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (json.isNotBlank()) {
            // write text file
            writeTextFile(json, getTracklistFileUri())
        }
    }


    /* Creates Uri for tracklist file */
    private fun getTracklistFileUri(): Uri {
        return File(application.getExternalFilesDir(""), Keys.TRACKLIST_FILE).toUri()
    }


    /* Deletes multiple tracks */
    private fun deleteTracks(
        trackListElements: MutableList<Track>,
        tracklist: TrackList
    ): TrackList {
        trackListElements.forEach { tracklistElement ->
            // delete track files
            tracklistElement.trackUriString.toUri().toFile().delete()
            tracklistElement.gpxUriString.toUri().toFile().delete()
            // subtract track length from total distance
            tracklist.totalDistance -= tracklistElement.length
        }
        tracklist.tracks.removeAll { trackListElements.contains(it) }
        saveTracklist(tracklist, GregorianCalendar.getInstance().time)
        return tracklist
    }


    /* Copies file to specified target */
    private fun copyFile(
        context: Context,
        originalFileUri: Uri,
        targetFileUri: Uri,
        deleteOriginal: Boolean = false
    ) {
        val inputStream = context.contentResolver.openInputStream(originalFileUri)
        val outputStream = context.contentResolver.openOutputStream(targetFileUri)
        if (outputStream != null) {
            inputStream?.copyTo(outputStream)
        }
        if (deleteOriginal) {
            context.contentResolver.delete(originalFileUri, null, null)
        }
    }

    /* Converts byte value into a human readable format */
    // Source: https://programming.guide/java/formatting-byte-size-to-human-readable-format.html
    fun getReadableByteCount(bytes: Long, si: Boolean = true): String {

        // check if Decimal prefix symbol (SI) or Binary prefix symbol (IEC) requested
        val unit: Long = if (si) 1000L else 1024L

        // just return bytes if file size is smaller than requested unit
        if (bytes < unit) return "$bytes B"

        // calculate exp
        val exp: Int = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()

        // determine prefix symbol
        val prefix: String = ((if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i")

        // calculate result and set number format
        val result: Double = bytes / unit.toDouble().pow(exp.toDouble())
        val numberFormat = NumberFormat.getNumberInstance()
        numberFormat.maximumFractionDigits = 1

        return numberFormat.format(result) + " " + prefix + "B"
    }

    fun readTextFile(fileUri: Uri): String {
        // todo read https://commonsware.com/blog/2016/03/15/how-consume-content-uri.html
        // https://developer.android.com/training/secure-file-sharing/retrieve-info
        val file: File = fileUri.toFile()
        // check if file exists
        if (!file.exists()) {
            return String()
        }
        // read until last line reached
        val stream: InputStream = file.inputStream()
        val reader: BufferedReader = BufferedReader(InputStreamReader(stream))
        val builder: StringBuilder = StringBuilder()
        reader.forEachLine {
            builder.append(it)
            builder.append("\n")
        }
        stream.close()
        return builder.toString()
    }

    /* Writes given text to file on storage */
    fun writeTextFile(text: String, fileUri: Uri) {
        if (text.isNotEmpty()) {
            val file: File = fileUri.toFile()
            file.writeText(text)
        }
    }
}