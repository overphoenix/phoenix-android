package me.zhanghai.android.files.storage

import android.content.Context
import android.content.Intent
import android.os.storage.StorageVolume
import androidx.annotation.DrawableRes
import java8.nio.file.Path
import java8.nio.file.Paths
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import tech.nagual.app.application
import me.zhanghai.android.files.compat.getDescriptionCompat
import me.zhanghai.android.files.compat.isPrimaryCompat
import me.zhanghai.android.files.compat.pathCompat
import me.zhanghai.android.files.util.putArgs
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.common.R
import me.zhanghai.android.files.util.createIntent
import tech.nagual.app.externalDataPath

sealed class DeviceStorage : Storage() {
    override val description: String
        get() = linuxPath

    override val path: Path
        get() = Paths.get(linuxPath)

    abstract override val linuxPath: String

    override fun createEditIntent(): Intent =
        EditDeviceStorageDialogActivity::class.createIntent()
            .putArgs(EditDeviceStorageDialogFragment.Args(this))

    fun copy_(
        customName: String? = this.customName,
        isVisible: Boolean = this.isVisible
    ): DeviceStorage =
        when (this) {
            is FileSystemRoot -> copy(customName, isVisible)
            is PrimaryStorageVolume -> copy(customName, isVisible)
            is InternalAppData -> copy(customName, isVisible)
            is ExternalAppData -> copy(customName, isVisible)
        }
}

@Parcelize
data class FileSystemRoot(
    override val customName: String?,
    override val isVisible: Boolean
) : DeviceStorage() {
    override val id: Long
        get() = "FileSystemRoot".hashCode().toLong()

    override val iconRes: Int
        @DrawableRes
        get() = R.drawable.device_icon_white_24dp

    override fun getDefaultName(context: Context): String =
        context.getString(R.string.storage_file_system_root_title)

    override val linuxPath: String
        get() = LINUX_PATH

    companion object {
        const val LINUX_PATH = "/"
    }
}

@Parcelize
data class PrimaryStorageVolume(
    override val customName: String?,
    override val isVisible: Boolean
) : DeviceStorage() {
    override val id: Long
        get() = "PrimaryStorageVolume".hashCode().toLong()

    override val iconRes: Int
        @DrawableRes
        get() = R.drawable.sd_card_icon_white_24dp

    override fun getDefaultName(context: Context): String =
        storageVolume.getDescriptionCompat(context)

    override val linuxPath: String
        get() = storageVolume.pathCompat

    private val storageVolume: StorageVolume
        get() = StorageVolumeListLiveData.valueCompat.find { it.isPrimaryCompat }!!
}

@Parcelize
data class InternalAppData(
    override val customName: String?,
    override val isVisible: Boolean
) : DeviceStorage() {
    @IgnoredOnParcel
    override val id: Long = "InternalAppData".hashCode().toLong()

    @DrawableRes
    @IgnoredOnParcel
    override val iconRes: Int = R.drawable.phoenix_appdata_icon_white_24dp

    override fun getDefaultName(context: Context): String =
        context.getString(R.string.navigation_phoenix_internal_appdata)

    @IgnoredOnParcel
    override val linuxPath: String = application.dataDir.absolutePath
}

@Parcelize
data class ExternalAppData(
    override val customName: String?,
    override val isVisible: Boolean
) : DeviceStorage() {
    @IgnoredOnParcel
    override val id: Long = "ExternalAppData".hashCode().toLong()

    @DrawableRes
    @IgnoredOnParcel
    override val iconRes: Int = R.drawable.phoenix_appdata_icon_white_24dp

    override fun getDefaultName(context: Context): String =
        context.getString(R.string.navigation_phoenix_external_appdata)

    @IgnoredOnParcel
    override val linuxPath: String = externalDataPath
}
