package tech.nagual.common.views

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.exifinterface.media.ExifInterface
import tech.nagual.common.R
import tech.nagual.common.databinding.DialogRenameItemsPatternBinding
import tech.nagual.common.extensions.*
import tech.nagual.common.activities.BaseSimpleActivity
import tech.nagual.common.extensions.*
import tech.nagual.common.interfaces.RenameTab
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RenamePatternTab(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs),
    RenameTab {
    var ignoreClicks = false
    var stopLooping =
        false     // we should request the permission on Android 30+ for all uris at once, not one by one
    var currentIncrementalNumber = 1
    var numbersCnt = 0
    var activity: tech.nagual.common.activities.BaseSimpleActivity? = null
    var paths = ArrayList<String>()

    private val binding = DialogRenameItemsPatternBinding.inflate(LayoutInflater.from(context))

    override fun initTab(activity: tech.nagual.common.activities.BaseSimpleActivity, paths: ArrayList<String>) {
        this.activity = activity
        this.paths = paths
        binding.renameItemsValue.setText(activity.baseConfig.lastRenamePatternUsed)
    }

    override fun dialogConfirmed(
        useMediaFileExtension: Boolean,
        callback: (success: Boolean) -> Unit
    ) {
        stopLooping = false
        if (ignoreClicks) {
            return
        }

        val newNameRaw = binding.renameItemsValue.value
        if (newNameRaw.isEmpty()) {
            callback(false)
            return
        }

        val validPaths = paths.filter { activity?.getDoesFilePathExist(it) == true }
        val sdFilePath =
            validPaths.firstOrNull { activity?.isPathOnSD(it) == true } ?: validPaths.firstOrNull()
        if (sdFilePath == null) {
            activity?.toast(R.string.unknown_error_occurred)
            return
        }

        activity?.baseConfig?.lastRenamePatternUsed = binding.renameItemsValue.value
        activity?.handleSAFDialog(sdFilePath) {
            if (!it) {
                return@handleSAFDialog
            }

            ignoreClicks = true
            var pathsCnt = validPaths.size
            numbersCnt = pathsCnt.toString().length
            for (path in validPaths) {
                if (stopLooping) {
                    return@handleSAFDialog
                }

                try {
                    val newPath = getNewPath(path, useMediaFileExtension) ?: continue
                    activity?.renameFile(path, newPath, true) { success, useAndroid30Way ->
                        if (success) {
                            pathsCnt--
                            if (pathsCnt == 0) {
                                callback(true)
                            }
                        } else {
                            ignoreClicks = false
                            if (useAndroid30Way) {
                                currentIncrementalNumber = 1
                                stopLooping = true
                                renameAllFiles(validPaths, useMediaFileExtension, callback)
                            } else {
                                activity?.toast(R.string.unknown_error_occurred)
                            }
                        }
                    }
                } catch (e: Exception) {
                    activity?.showErrorToast(e)
                }
            }
            stopLooping = false
        }
    }

    private fun getNewPath(path: String, useMediaFileExtension: Boolean): String? {
        try {
            val exif = ExifInterface(path)
            var dateTime =
                exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: exif.getAttribute(
                    ExifInterface.TAG_DATETIME
                )

            if (dateTime == null) {
                val calendar = Calendar.getInstance(Locale.ENGLISH)
                calendar.timeInMillis = File(path).lastModified()
                dateTime = DateFormat.format("yyyy:MM:dd kk:mm:ss", calendar).toString()
            }

            val pattern = if (dateTime.substring(
                    4,
                    5
                ) == "-"
            ) "yyyy-MM-dd kk:mm:ss" else "yyyy:MM:dd kk:mm:ss"
            val simpleDateFormat = SimpleDateFormat(pattern, Locale.ENGLISH)

            val dt = simpleDateFormat.parse(dateTime.replace("T", " "))
            val cal = Calendar.getInstance()
            cal.time = dt
            val year = cal.get(Calendar.YEAR).toString()
            val month = (cal.get(Calendar.MONTH) + 1).ensureTwoDigits()
            val day = (cal.get(Calendar.DAY_OF_MONTH)).ensureTwoDigits()
            val hours = (cal.get(Calendar.HOUR_OF_DAY)).ensureTwoDigits()
            val minutes = (cal.get(Calendar.MINUTE)).ensureTwoDigits()
            val seconds = (cal.get(Calendar.SECOND)).ensureTwoDigits()

            var newName = binding.renameItemsValue.value
                .replace("%Y", year, false)
                .replace("%M", month, false)
                .replace("%D", day, false)
                .replace("%h", hours, false)
                .replace("%m", minutes, false)
                .replace("%s", seconds, false)
                .replace("%i", String.format("%0${numbersCnt}d", currentIncrementalNumber))

            if (newName.isEmpty()) {
                return null
            }

            currentIncrementalNumber++
            if ((!newName.contains(".") && path.contains(".")) || (useMediaFileExtension && !".${
                    newName.substringAfterLast(
                        "."
                    )
                }".isMediaFile())
            ) {
                val extension = path.substringAfterLast(".")
                newName += ".$extension"
            }

            var newPath = "${path.getParentPath()}/$newName"

            var currentIndex = 0
            while (activity?.getDoesFilePathExist(newPath) == true) {
                currentIndex++
                var extension = ""
                val name = if (newName.contains(".")) {
                    extension = ".${newName.substringAfterLast(".")}"
                    newName.substringBeforeLast(".")
                } else {
                    newName
                }

                newPath = "${path.getParentPath()}/$name~$currentIndex$extension"
            }

            return newPath
        } catch (e: Exception) {
            return null
        }
    }

    private fun renameAllFiles(
        paths: List<String>,
        useMediaFileExtension: Boolean,
        callback: (success: Boolean) -> Unit
    ) {
        val fileDirItems = paths.map { File(it).toFileDirItem(context) }
        val uriPairs = context.getFileUrisFromFileDirItems(fileDirItems)
        val validPaths = uriPairs.first
        val uris = uriPairs.second
        activity?.updateSDK30Uris(uris) { success ->
            if (success) {
                try {
                    uris.forEachIndexed { index, uri ->
                        val path = validPaths[index]
                        val newFileName =
                            getNewPath(path, useMediaFileExtension)?.getFilenameFromPath()
                                ?: return@forEachIndexed
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, newFileName)
                        }

                        context.contentResolver.update(uri, values, null, null)
                    }
                    callback(true)
                } catch (e: Exception) {
                    callback(false)
                }
            }
        }
    }
}
