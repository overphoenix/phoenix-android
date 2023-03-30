package tech.nagual.phoenix.navigation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.storage.StorageVolume
import androidx.annotation.DrawableRes
import androidx.annotation.Size
import androidx.annotation.StringRes
import java8.nio.file.Path
import java8.nio.file.Paths
import me.zhanghai.android.files.bookmarks.BookmarkDirectory
import me.zhanghai.android.files.compat.getDescriptionCompat
import me.zhanghai.android.files.compat.isPrimaryCompat
import me.zhanghai.android.files.compat.pathCompat
import me.zhanghai.android.files.file.JavaFile
import me.zhanghai.android.files.file.asFileSize
import me.zhanghai.android.files.standarddirectories.StandardDirectoriesLiveData
import me.zhanghai.android.files.standarddirectories.StandardDirectory
import me.zhanghai.android.files.standarddirectories.getExternalStorageDirectory
import me.zhanghai.android.files.storage.FileSystemRoot
import me.zhanghai.android.files.storage.Storage
import me.zhanghai.android.files.storage.StorageVolumeListLiveData
import me.zhanghai.android.files.util.createIntent
import me.zhanghai.android.files.util.isMounted
import tech.nagual.common.R
import tech.nagual.app.application
import me.zhanghai.android.files.navigation.NavigationItem
import me.zhanghai.android.files.navigation.NavigationRoot
import tech.nagual.settings.Settings
import tech.nagual.phoenix.settings.SettingsActivity
import me.zhanghai.android.files.tools.Tool
import me.zhanghai.android.files.tools.Tools
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.model.Organizer
import tech.nagual.phoenix.organizers.EditOrganizerDialogActivity
import kotlin.random.Random

val navigationItems: List<NavigationItem?>
    get() =
        mutableListOf<NavigationItem?>().apply {
            var addSep = false
            if (storageItems.isNotEmpty()) {
                addAll(storageItems)
                addSep = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Starting with R, we can get read/write access to non-primary storage volumes with
                // MANAGE_EXTERNAL_STORAGE. However before R, we only have read-only access to them
                // and need to use the Storage Access Framework instead, so hide them in this case
                // to avoid confusion.
                addAll(storageVolumeItems)
            }
            if (Settings.ADDING_STORAGES_FROM_NAVIGATION.valueCompat) {
                add(AddStorageItem())
                addSep = true
            }
            if (addSep) {
                add(null)
                addSep = false
            }
            val standardDirectoryItems = standardDirectoryItems
            if (standardDirectoryItems.isNotEmpty()) {
                addAll(standardDirectoryItems)
                add(null)
            }
            val bookmarkDirectoryItems = bookmarkDirectoryItems
            if (bookmarkDirectoryItems.isNotEmpty()) {
                addAll(bookmarkDirectoryItems)
                add(null)
            }
            val organizerItems = organizerItems
            if (Settings.OPENING_ORGANIZERS_FROM_MENU.valueCompat && organizerItems.isNotEmpty()) {
                addAll(organizerItems)
                addSep = true
            }
            if (Settings.ADDING_ORGANIZERS_FROM_MENU.valueCompat) {
                add(CreateOrganizerItem())
                addSep = true

            }
            if (addSep) {
                add(null)
            }
            if (toolItems.isNotEmpty()) {
                addAll(toolItems)
                add(null)
            }
            add(
                ActivityMenuItem(
                    R.drawable.settings_icon_white_24dp, R.string.navigation_settings,
                    SettingsActivity::class.createIntent()
                )
            )
        }

private val storageItems: List<NavigationItem>
    @Size(min = 0)
    get() = Settings.STORAGES.valueCompat.filter { it.isVisible }.map { StorageItem(it) }

private val organizerItems: List<NavigationItem>
    @Size(min = 0)
    get() = OrganizersManager.organizers.map { OrganizerItem(it) }


private abstract class PathItem(val path: Path) : NavigationItem() {
    override fun isChecked(listener: Listener): Boolean = listener.currentPath == path

    override fun onClick(listener: Listener) {
        if (this is NavigationRoot) {
            listener.navigateToRoot(path)
        } else {
            listener.navigateTo(path)
        }
        listener.closeNavigationDrawer()
    }
}

private class StorageItem(
    private val storage: Storage
) : PathItem(storage.path), NavigationRoot {
    init {
        require(storage.isVisible)
    }

    override val id: Long
        get() = storage.id

    override val iconRes: Int
        @DrawableRes
        get() = storage.iconRes

    override fun getTitle(context: Context): String = storage.getName(context)

    override fun getSubtitle(context: Context): String? =
        storage.linuxPath?.let { getStorageSubtitle(it, context) }

    override fun onLongClick(listener: Listener): Boolean {
        listener.onEditStorage(storage)
        return true
    }

    override fun getName(context: Context): String = getTitle(context)
}

private val storageVolumeItems: List<NavigationItem>
    @Size(min = 0)
    get() =
        StorageVolumeListLiveData.valueCompat.filter { !it.isPrimaryCompat && it.isMounted }
            .map { StorageVolumeItem(it) }

private class StorageVolumeItem(
    private val storageVolume: StorageVolume
) : PathItem(Paths.get(storageVolume.pathCompat)), NavigationRoot {
    override val id: Long
        get() = storageVolume.hashCode().toLong()

    override val iconRes: Int
        @DrawableRes
        get() = R.drawable.sd_card_icon_white_24dp

    override fun getTitle(context: Context): String = storageVolume.getDescriptionCompat(context)

    override fun getSubtitle(context: Context): String? =
        getStorageSubtitle(storageVolume.pathCompat, context)

    override fun getName(context: Context): String = getTitle(context)
}

private fun getStorageSubtitle(linuxPath: String, context: Context): String? {
    var totalSpace = JavaFile.getTotalSpace(linuxPath)
    val freeSpace: Long
    when {
        totalSpace != 0L -> freeSpace = JavaFile.getFreeSpace(linuxPath)
        linuxPath == FileSystemRoot.LINUX_PATH -> {
            // Root directory may not be an actual partition on legacy Android versions (can be
            // a ramdisk instead). On modern Android the system partition will be mounted as
            // root instead so let's try with the system partition again.
            // @see https://source.android.com/devices/bootloader/system-as-root
            val systemPath = Environment.getRootDirectory().path
            totalSpace = JavaFile.getTotalSpace(systemPath)
            freeSpace = JavaFile.getFreeSpace(systemPath)
        }
        else -> freeSpace = 0
    }
    if (totalSpace == 0L) {
        return null
    }
    val freeSpaceString = freeSpace.asFileSize().formatHumanReadable(context)
    val totalSpaceString = totalSpace.asFileSize().formatHumanReadable(context)
    return context.getString(
        R.string.navigation_storage_subtitle_format, freeSpaceString, totalSpaceString
    )
}

private class AddStorageItem : NavigationItem() {
    override val id: Long = R.string.navigation_add_storage.toLong()

    @DrawableRes
    override val iconRes: Int = R.drawable.add_icon_white_24dp

    override fun getTitle(context: Context): String =
        context.getString(R.string.navigation_add_storage)

    override fun onClick(listener: Listener) {
        listener.onAddStorage()
    }
}


private class CreateOrganizerItem : NavigationItem() {
    override val id: Long = R.string.navigation_create_organizer.toLong()

    private var mContent: Context = application.applicationContext

    @DrawableRes
    override val iconRes: Int = R.drawable.organizer_new_icon_white_24dp

    override fun getTitle(context: Context): String {
        mContent = context
        return context.getString(R.string.navigation_create_organizer)
    }

    override fun onClick(listener: Listener) {
        EditOrganizerDialogActivity.showCreateOrganizerDialog(mContent)
    }
}

private val standardDirectoryItems: List<NavigationItem>
    @Size(min = 0)
    get() =
        StandardDirectoriesLiveData.valueCompat
            .filter { it.isEnabled }
            .map { StandardDirectoryItem(it) }

private class StandardDirectoryItem(
    private val standardDirectory: StandardDirectory
) : PathItem(
    Paths.get(getExternalStorageDirectory(standardDirectory.relativePath))
) {
    init {
        require(standardDirectory.isEnabled)
    }

    override val id: Long
        get() = standardDirectory.id

    override val iconRes: Int
        @DrawableRes
        get() = standardDirectory.iconRes

    override fun getTitle(context: Context): String = standardDirectory.getTitle(context)

    override fun onLongClick(listener: Listener): Boolean {
        listener.onEditStandardDirectory(standardDirectory)
        return true
    }
}

private val bookmarkDirectoryItems: List<NavigationItem>
    @Size(min = 0)
    get() = Settings.BOOKMARK_DIRECTORIES.valueCompat.map { BookmarkDirectoryItem(it) }

private class BookmarkDirectoryItem(
    private val bookmarkDirectory: BookmarkDirectory
) : PathItem(bookmarkDirectory.path) {
    // We cannot simply use super.getId() because different bookmark directories may have
    // the same path.
    override val id: Long
        get() = bookmarkDirectory.id

    @DrawableRes
    override val iconRes: Int = R.drawable.directory_icon_white_24dp

    override fun getTitle(context: Context): String = bookmarkDirectory.name

    override fun onLongClick(listener: Listener): Boolean {
        Settings
        listener.onEditBookmarkDirectory(bookmarkDirectory)
        return true
    }
}

private class ToolItem(
    private val tool: Tool
) : NavigationItem() {
    override val id: Long
        get() = tool.id

    override val iconRes: Int
        @DrawableRes
        get() = Tools.infos[tool.origName]!!.iconRes

    override fun getTitle(context: Context): String = tool.getName()

    override fun onClick(listener: Listener) {
        listener.startActivity(
            Intent(
                application,
                Class.forName(Tools.infos[tool.origName]!!.packageName)
            )
        )
        listener.closeNavigationDrawer()
    }

    override fun onLongClick(listener: Listener): Boolean {
        listener.onEditTool(tool)
        return true
    }

    fun getName(context: Context): String = getTitle(context)
}

private val toolItems: List<ToolItem>
    @Size(min = 0)
    get() = Settings.TOOLS.valueCompat.filter { it.isVisible }
        .map { ToolItem(it) }

private abstract class MenuItem(
    @DrawableRes override val iconRes: Int,
    @StringRes val titleRes: Int
) : NavigationItem() {
    override fun getTitle(context: Context): String = context.getString(titleRes)
}

private class ActivityMenuItem(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    private val intent: Intent
) : MenuItem(iconRes, titleRes) {
    override val id: Long
        get() = intent.component.hashCode().toLong()

    override fun onClick(listener: Listener) {
        // TODO: startActivitySafe()?
        listener.startActivity(intent)
        listener.closeNavigationDrawer()
    }
}

private class OrganizerItem(
    private val organizer: Organizer
) : NavigationItem() {

    override val id: Long
        get() = Random.nextLong()

    override val iconRes: Int
        @DrawableRes
        get() = R.drawable.organizer_icon

    override fun getTitle(context: Context): String = organizer.name

    override fun getSubtitle(context: Context): String? {
        return organizer.description
    }

    override fun onClick(listener: Listener) {
        OrganizersManager.getInstance().open(organizer, listener.currentContext)
        listener.closeNavigationDrawer()
    }

    override fun onLongClick(listener: Listener): Boolean {
        OrganizersManager.getInstance().edit(organizer, listener.currentContext)
        return true
    }

    fun getName(context: Context): String = getTitle(context)
}
