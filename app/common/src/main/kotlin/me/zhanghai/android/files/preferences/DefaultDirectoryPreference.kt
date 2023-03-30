package me.zhanghai.android.files.preferences

import androidx.fragment.app.Fragment
import java8.nio.file.Path
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.settings.Settings

class DefaultDirectoryPreference(key: String, fragment: Fragment) : PathPreference(key, fragment) {
    override var persistedPath: Path
        get() = Settings.FILE_LIST_DEFAULT_DIRECTORY.valueCompat
        set(value) {
            Settings.FILE_LIST_DEFAULT_DIRECTORY.putValue(value)
        }
}