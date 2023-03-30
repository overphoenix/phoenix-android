/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

/*
 * Modified by Metamorph
 */

package me.zhanghai.android.files.navigation

import androidx.lifecycle.MediatorLiveData
import java8.nio.file.Path
import tech.nagual.app.navigationItemListLiveData
import me.zhanghai.android.files.util.valueCompat

object NavigationRootMapLiveData : MediatorLiveData<Map<Path, NavigationRoot>>() {
    init {
        // Initialize value before we have any active observer.
        loadValue()
        addSource(navigationItemListLiveData) { loadValue() }
    }

    private fun loadValue() {
        value = navigationItemListLiveData.valueCompat
            .mapNotNull { it as? NavigationRoot }
            .associateBy { it.path }
    }
}
