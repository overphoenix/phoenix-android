package me.zhanghai.android.files.standarddirectories

import android.os.Environment
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.common.R
import tech.nagual.settings.Settings

object StandardDirectories {
    val list: List<StandardDirectory>
        get() {
            val settingsMap = Settings.STANDARD_DIRECTORY_SETTINGS.valueCompat.associateBy { it.id }
            return defaultStandardDirectories.map {
                val settings = settingsMap[it.key]
                if (settings != null) it.withSettings(settings) else it
            }
        }

    private val defaultStandardDirectories: List<StandardDirectory>
        get() = DEFAULT_STANDARD_DIRECTORIES.mapNotNull { it }

    // @see android.os.Environment#STANDARD_DIRECTORIES
    private val DEFAULT_STANDARD_DIRECTORIES = listOf(
        StandardDirectory(
            R.drawable.alarm_icon_white_24dp, R.string.navigation_standard_directory_alarms,
            Environment.DIRECTORY_ALARMS, false
        ),
        StandardDirectory(
            R.drawable.camera_icon_white_24dp, R.string.navigation_standard_directory_dcim,
            Environment.DIRECTORY_DCIM, false
        ),
        StandardDirectory(
            R.drawable.document_icon_white_24dp, R.string.navigation_standard_directory_documents,
            Environment.DIRECTORY_DOCUMENTS, false
        ),
        StandardDirectory(
            R.drawable.download_icon_white_24dp, R.string.navigation_standard_directory_downloads,
            Environment.DIRECTORY_DOWNLOADS, false
        ),
        StandardDirectory(
            R.drawable.video_icon_white_24dp, R.string.navigation_standard_directory_movies,
            Environment.DIRECTORY_MOVIES, false
        ),
        StandardDirectory(
            R.drawable.audio_icon_white_24dp, R.string.navigation_standard_directory_music,
            Environment.DIRECTORY_MUSIC, false
        ),
        StandardDirectory(
            R.drawable.notification_icon_white_24dp,
            R.string.navigation_standard_directory_notifications, Environment.DIRECTORY_NOTIFICATIONS,
            false
        ),
        StandardDirectory(
            R.drawable.image_icon_white_24dp, R.string.navigation_standard_directory_pictures,
            Environment.DIRECTORY_PICTURES, false
        ),
        StandardDirectory(
            R.drawable.podcast_icon_white_24dp, R.string.navigation_standard_directory_podcasts,
            Environment.DIRECTORY_PODCASTS, false
        ),
        StandardDirectory(
            R.drawable.ringtone_icon_white_24dp, R.string.navigation_standard_directory_ringtones,
            Environment.DIRECTORY_RINGTONES, false
        )
    )
}