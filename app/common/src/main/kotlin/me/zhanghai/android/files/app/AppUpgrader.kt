package me.zhanghai.android.files.app

import android.net.Uri
import android.os.Parcel
import androidx.annotation.StringRes
import androidx.core.content.edit
import tech.nagual.app.appClassLoader
import tech.nagual.app.application
import tech.nagual.app.defaultSharedPreferences
import me.zhanghai.android.files.compat.readBooleanCompat
import me.zhanghai.android.files.compat.writeBooleanCompat
import me.zhanghai.android.files.compat.writeParcelableListCompat
import me.zhanghai.android.files.util.*
import tech.nagual.common.R
import me.zhanghai.android.files.provider.common.ByteString
import me.zhanghai.android.files.provider.content.ContentFileSystem
import me.zhanghai.android.files.provider.document.DocumentFileSystem
import me.zhanghai.android.files.provider.linux.LinuxFileSystem
import me.zhanghai.android.files.provider.root.RootStrategy
import me.zhanghai.android.files.provider.sftp.SftpFileSystem
import me.zhanghai.android.files.provider.smb.SmbFileSystem
import tech.nagual.app.appVersionCode

private const val KEY_VERSION_CODE = "key_version_code"

private const val VERSION_CODE_0_5_3 = 53
private const val VERSION_CODE_0_6_0 = 60

private var lastVersionCode: Int
    get() {
        if (defaultSharedPreferences.all.isEmpty()) {
            // This is a new install.
            lastVersionCode = appVersionCode
            return appVersionCode
        }
        return defaultSharedPreferences.getInt(KEY_VERSION_CODE, appVersionCode)
    }
    set(value) {
        defaultSharedPreferences.edit { putInt(KEY_VERSION_CODE, value) }
    }

fun upgradeApp() {
    upgradeAppFrom(lastVersionCode)
    lastVersionCode = appVersionCode
}

private fun upgradeAppFrom(lastVersionCode: Int) {
    if (lastVersionCode < VERSION_CODE_0_5_3) {
        upgradeAppTo0_5_3()
    }
    if (lastVersionCode < VERSION_CODE_0_6_0) {
        upgradeAppTo0_6_0()
    }
}


// Upgraders
private const val PARCEL_VAL_PARCELABLE = 4
private const val PARCEL_VAL_LIST = 11

internal fun upgradeAppTo0_5_3() {
    migratePathSetting0_5_3(R.string.pref_key_file_list_default_directory)
    migrateSftpServersSetting0_5_3()
    migrateBookmarkDirectoriesSetting0_5_3()
    migrateRootStrategySetting0_5_3()
    migratePathSetting0_5_3(R.string.pref_key_ftp_server_home_directory)
}

private fun migratePathSetting0_5_3(@StringRes keyRes: Int) {
    val key = application.getString(keyRes)
    val oldBytes = defaultSharedPreferences.getString(key, null)?.asBase64()?.toByteArray()
        ?: return
    val newBytes = try {
        Parcel.obtain().use { newParcel ->
            Parcel.obtain().use { oldParcel ->
                oldParcel.unmarshall(oldBytes, 0, oldBytes.size)
                oldParcel.setDataPosition(0)
                newParcel.writeInt(oldParcel.readInt())
                migratePath0_5_3(oldParcel, newParcel)
            }
            newParcel.marshall()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
    defaultSharedPreferences.edit { putString(key, newBytes?.toBase64()?.value) }
}

private fun migratePath0_5_3(oldParcel: Parcel, newParcel: Parcel) {
    val className = oldParcel.readString()
    newParcel.writeString(className)
    newParcel.writeByte(oldParcel.readByte())
    newParcel.writeBooleanCompat(oldParcel.readBooleanCompat())
    newParcel.writeParcelableListCompat(oldParcel.readParcelableListCompat<ByteString>(), 0)
    when (className) {
        "me.zhanghai.android.files.provider.archive.ArchivePath" -> {
            newParcel.writeString(oldParcel.readString())
            migratePath0_5_3(oldParcel, newParcel)
        }
        "me.zhanghai.android.files.provider.content.ContentPath" -> {
            newParcel.writeParcelable(oldParcel.readParcelable<ContentFileSystem>(), 0)
            newParcel.writeParcelable(oldParcel.readParcelable<Uri>(), 0)
        }
        "me.zhanghai.android.files.provider.document.DocumentPath" ->
            newParcel.writeParcelable(oldParcel.readParcelable<DocumentFileSystem>(), 0)
        "me.zhanghai.android.files.provider.linux.LinuxPath" -> {
            newParcel.writeParcelable(oldParcel.readParcelable<LinuxFileSystem>(), 0)
            oldParcel.readBooleanCompat()
        }
        "me.zhanghai.android.files.provider.sftp.SftpPath" ->
            newParcel.writeParcelable(oldParcel.readParcelable<SftpFileSystem>(), 0)
        "me.zhanghai.android.files.provider.smb.SmbPath" ->
            newParcel.writeParcelable(oldParcel.readParcelable<SmbFileSystem>(), 0)
        else -> throw IllegalStateException(className)
    }
}

private fun migrateSftpServersSetting0_5_3() {
    val key = application.getString(R.string.pref_key_storages)
    val oldBytes = defaultSharedPreferences.getString(key, null)?.asBase64()?.toByteArray()
        ?: return
    val newBytes = try {
        Parcel.obtain().use { newParcel ->
            Parcel.obtain().use { oldParcel ->
                oldParcel.unmarshall(oldBytes, 0, oldBytes.size)
                oldParcel.setDataPosition(0)
                newParcel.writeInt(oldParcel.readInt())
                val size = oldParcel.readInt()
                newParcel.writeInt(size)
                repeat(size) {
                    val oldPosition = oldParcel.dataPosition()
                    oldParcel.readInt()
                    when (oldParcel.readString()) {
                        "me.zhanghai.android.files.storage.SftpServer" -> {
                            newParcel.writeInt(PARCEL_VAL_PARCELABLE)
                            newParcel.writeString("me.zhanghai.android.files.storage.SftpServer")
                            val id = oldParcel.readLong()
                            newParcel.writeLong(id)
                            val customName = oldParcel.readString()
                            newParcel.writeString(customName)
                            val authorityHost = oldParcel.readString()
                            newParcel.writeString(authorityHost)
                            val authorityPort = oldParcel.readInt()
                            newParcel.writeInt(authorityPort)
                            val authenticationClassName = oldParcel.readString()
                            val authorityUsername = oldParcel.readString()
                            newParcel.writeString(authorityUsername)
                            newParcel.writeString(authenticationClassName)
                            val authenticationPasswordOrPrivateKey = oldParcel.readString()
                            newParcel.writeString(authenticationPasswordOrPrivateKey)
                            val relativePath = oldParcel.readString()
                            newParcel.writeString(relativePath)
                        }
                        else -> {
                            oldParcel.setDataPosition(oldPosition)
                            val storage = oldParcel.readValue(appClassLoader)
                            newParcel.writeValue(storage)
                        }
                    }
                }
            }
            newParcel.marshall()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
    defaultSharedPreferences.edit { putString(key, newBytes?.toBase64()?.value) }
}

private fun migrateBookmarkDirectoriesSetting0_5_3() {
    val key = application.getString(R.string.pref_key_bookmark_directories)
    val oldBytes = defaultSharedPreferences.getString(key, null)?.asBase64()?.toByteArray()
        ?: return
    val newBytes = try {
        Parcel.obtain().use { newParcel ->
            Parcel.obtain().use { oldParcel ->
                oldParcel.unmarshall(oldBytes, 0, oldBytes.size)
                oldParcel.setDataPosition(0)
                newParcel.writeInt(oldParcel.readInt())
                val size = oldParcel.readInt()
                newParcel.writeInt(size)
                repeat(size) {
                    newParcel.writeInt(oldParcel.readInt())
                    newParcel.writeString(oldParcel.readString())
                    newParcel.writeLong(oldParcel.readLong())
                    newParcel.writeString(oldParcel.readString())
                    migratePath0_5_3(oldParcel, newParcel)
                }
            }
            newParcel.marshall()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
    defaultSharedPreferences.edit { putString(key, newBytes?.toBase64()?.value) }
}

private fun migrateRootStrategySetting0_5_3() {
    val key = application.getString(R.string.pref_key_root_strategy)
    val oldValue = defaultSharedPreferences.getString(key, null)?.toInt() ?: return
    val newValue = when (oldValue) {
        0 -> RootStrategy.NEVER
        3 -> RootStrategy.ALWAYS
        else -> RootStrategy.AUTOMATIC
    }.ordinal.toString()
    defaultSharedPreferences.edit { putString(key, newValue) }
}



internal fun upgradeAppTo0_6_0() {
    migrateSftpServersSetting0_6_0()
}

private fun migrateSftpServersSetting0_6_0() {
    val key = application.getString(R.string.pref_key_storages)
    val oldBytes = defaultSharedPreferences.getString(key, null)?.asBase64()?.toByteArray()
        ?: return
    val newBytes = try {
        Parcel.obtain().use { newParcel ->
            Parcel.obtain().use { oldParcel ->
                oldParcel.unmarshall(oldBytes, 0, oldBytes.size)
                oldParcel.setDataPosition(0)
                newParcel.writeInt(oldParcel.readInt())
                val size = oldParcel.readInt()
                newParcel.writeInt(size)
                repeat(size) {
                    val oldPosition = oldParcel.dataPosition()
                    oldParcel.readInt()
                    when (oldParcel.readString()) {
                        "me.zhanghai.android.files.storage.SftpServer" -> {
                            newParcel.writeInt(PARCEL_VAL_PARCELABLE)
                            newParcel.writeString("me.zhanghai.android.files.storage.SftpServer")
                            val id = oldParcel.readLong()
                            newParcel.writeLong(id)
                            val customName = oldParcel.readString()
                            newParcel.writeString(customName)
                            val authorityHost = oldParcel.readString()
                            newParcel.writeString(authorityHost)
                            val authorityPort = oldParcel.readInt()
                            newParcel.writeInt(authorityPort)
                            val authorityUsername = oldParcel.readString()
                            newParcel.writeString(authorityUsername)
                            val authenticationClassName = oldParcel.readString()
                            newParcel.writeString(authenticationClassName)
                            val authenticationPasswordOrPrivateKey = oldParcel.readString()
                            newParcel.writeString(authenticationPasswordOrPrivateKey)
                            if (authenticationClassName == "me.zhanghai.android.files.provider.sftp"
                                + ".client.PublicKeyAuthentication") {
                                newParcel.writeString(null)
                            }
                            val relativePath = oldParcel.readString()
                            newParcel.writeString(relativePath)
                        }
                        else -> {
                            oldParcel.setDataPosition(oldPosition)
                            val storage = oldParcel.readValue(appClassLoader)
                            newParcel.writeValue(storage)
                        }
                    }
                }
            }
            newParcel.marshall()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
    defaultSharedPreferences.edit { putString(key, newBytes?.toBase64()?.value) }
}
