package me.zhanghai.android.files.preferences

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import java8.nio.file.Path
import tech.nagual.common.preferences.Preference
import tech.nagual.common.preferences.PreferenceFragment
import tech.nagual.common.preferences.PreferencesAdapter
import me.zhanghai.android.files.util.startActivityForResultSafe
import me.zhanghai.android.files.filelist.FileListActivity
import me.zhanghai.android.files.filelist.toUserFriendlyString

abstract class PathPreference(key: String, private val fragment: Fragment) : Preference(key),
    PreferenceFragment.ActivityResultListener {

    private val pickDirectoryContract = FileListActivity.PickDirectoryContract()

    protected abstract var persistedPath: Path

    var path: Path = persistedPath
        set(value) {
            if (field == value) {
                return
            }
            field = value
            persistedPath = value
            requestRebind()
        }

    val requestCode: Int
        get() = key.hashCode() and 0x0000FFFF

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == this.requestCode) {
            val result = pickDirectoryContract.parseResult(resultCode, data)
            if (result != null) {
                path = result
            }
        }
    }

    override fun resolveSummary(context: Context): CharSequence? {
        return path.toUserFriendlyString()
    }

    override fun onClick(holder: PreferencesAdapter.ViewHolder) {
        fragment.startActivityForResultSafe(
            pickDirectoryContract.createIntent(fragment.requireContext(), path), requestCode
        )
    }
}
