package me.zhanghai.android.files.standarddirectories

import androidx.lifecycle.MediatorLiveData
import tech.nagual.settings.Settings

object StandardDirectoriesLiveData : MediatorLiveData<List<StandardDirectory>>() {
    init {
        // Initialize value before we have any active observer.
        loadValue()
        addSource(Settings.STANDARD_DIRECTORY_SETTINGS) { loadValue() }
    }

    private fun loadValue() {
        value = StandardDirectories.list
    }
}
