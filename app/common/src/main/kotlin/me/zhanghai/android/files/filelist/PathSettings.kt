package me.zhanghai.android.files.filelist

import java8.nio.file.Path
import tech.nagual.settings.ParcelValueSettingLiveData
import tech.nagual.settings.SettingLiveData
import tech.nagual.common.R

object PathSettings {
    private const val NAME_SUFFIX = "path"

    @Suppress("UNCHECKED_CAST")
    fun getFileListSortOptions(path: Path): SettingLiveData<FileSortOptions?> =
        ParcelValueSettingLiveData(
            NAME_SUFFIX, R.string.pref_key_file_list_sort_options, path.toString(), null
        )
}
