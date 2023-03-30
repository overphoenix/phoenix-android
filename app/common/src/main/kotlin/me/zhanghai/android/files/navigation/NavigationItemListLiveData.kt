package me.zhanghai.android.files.navigation

import androidx.lifecycle.MediatorLiveData
import me.zhanghai.android.files.standarddirectories.StandardDirectoriesLiveData
import me.zhanghai.android.files.storage.StorageVolumeListLiveData
import tech.nagual.settings.Settings

abstract class NavigationItemListLiveData : MediatorLiveData<List<NavigationItem?>>() {
    init {
        // Initialize value before we have any active observer.
        loadValue()
        addSource(Settings.ADDING_STORAGES_FROM_NAVIGATION) { loadValue() }
        addSource(Settings.ADDING_ORGANIZERS_FROM_MENU) { loadValue() }
        addSource(Settings.OPENING_ORGANIZERS_FROM_MENU) { loadValue() }
        addSource(Settings.STORAGES) { loadValue() }
        addSource(Settings.TOOLS) { loadValue() }
        addSource(StorageVolumeListLiveData) { loadValue() }
        addSource(StandardDirectoriesLiveData) { loadValue() }
        addSource(Settings.BOOKMARK_DIRECTORIES) { loadValue() }
    }

    abstract fun loadValue()
}
