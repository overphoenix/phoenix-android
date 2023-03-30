package me.zhanghai.android.files.standarddirectories

import android.os.Environment
import me.zhanghai.android.files.compat.getDrawableCompat
import me.zhanghai.android.files.compat.setTintCompat
import tech.nagual.common.preferences.PreferenceFragment
import tech.nagual.common.preferences.helpers.onCheckedChange
import tech.nagual.common.preferences.helpers.screen
import tech.nagual.common.preferences.helpers.switch
import me.zhanghai.android.files.util.getColorByAttr
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.settings.Settings

fun getExternalStorageDirectory(relativePath: String): String =
    @Suppress("DEPRECATION")
    Environment.getExternalStoragePublicDirectory(relativePath).path

class StandardDirectoryListFragment : PreferenceFragment() {
    override fun createRootScreen() = screen(context) {
        val secondaryTextColor = requireContext().getColorByAttr(android.R.attr.textColorSecondary)
        for (standardDirectory in StandardDirectories.list) {
            switch(standardDirectory.key) {
                icon = requireContext().getDrawableCompat(standardDirectory.iconRes).apply {
                    mutate()
                    setTintCompat(secondaryTextColor)
                }
                persistent = false
                title = standardDirectory.getTitle(requireContext())
                summary = getExternalStorageDirectory(standardDirectory.relativePath)
                defaultValue = standardDirectory.isEnabled
                onCheckedChange { checked ->
                    val id = key
                    val settingsList =
                        Settings.STANDARD_DIRECTORY_SETTINGS.valueCompat.toMutableList()
                    val index = settingsList.indexOfFirst { it.id == id }
                    if (index != -1) {
                        settingsList[index] = settingsList[index].copy(isEnabled = checked)
                    } else {
                        val standardDirectory =
                            StandardDirectoriesLiveData.valueCompat.find { it.key == id }!!
                        settingsList += standardDirectory.toSettings().copy(isEnabled = checked)
                    }
                    Settings.STANDARD_DIRECTORY_SETTINGS.putValue(settingsList)
                    true
                }
            }
        }
    }
}
