package me.zhanghai.android.files.features.ftpserver

import androidx.fragment.app.Fragment
import java8.nio.file.Path
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.common.R
import me.zhanghai.android.files.preferences.PathPreference
import tech.nagual.settings.Settings

class FtpServerHomeDirectoryPreference(fragment: Fragment) :
    PathPreference(fragment.getString(R.string.pref_key_ftp_server_home_directory), fragment) {
    override var persistedPath: Path
        get() = Settings.FTP_SERVER_HOME_DIRECTORY.valueCompat
        set(value) {
            Settings.FTP_SERVER_HOME_DIRECTORY.putValue(value)
        }
}