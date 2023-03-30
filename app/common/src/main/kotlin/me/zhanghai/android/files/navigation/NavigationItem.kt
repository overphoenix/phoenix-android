/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.navigation

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import java8.nio.file.Path
import me.zhanghai.android.files.compat.getDrawableCompat
import me.zhanghai.android.files.bookmarks.BookmarkDirectory
import me.zhanghai.android.files.standarddirectories.StandardDirectory
import me.zhanghai.android.files.storage.Storage
import me.zhanghai.android.files.tools.Tool

abstract class NavigationItem {
    abstract val id: Long

    fun getIcon(context: Context): Drawable = context.getDrawableCompat(iconRes!!)

    @get:DrawableRes
    protected abstract val iconRes: Int?

    abstract fun getTitle(context: Context): String

    open fun getSubtitle(context: Context): String? = null

    open fun isChecked(listener: Listener): Boolean = false

    abstract fun onClick(listener: Listener)

    open fun onLongClick(listener: Listener): Boolean = false

    interface Listener {
        val currentPath: Path
        val currentContext: Context
        fun navigateTo(path: Path)
        fun navigateToRoot(path: Path)
        fun onAddStorage()
        fun onEditStorage(storage: Storage)
        fun onEditStandardDirectory(standardDirectory: StandardDirectory)
        fun onEditTool(tool: Tool)
        fun onEditBookmarkDirectory(bookmarkDirectory: BookmarkDirectory)
        fun closeNavigationDrawer()
        fun startActivity(intent: Intent)
    }
}
