package me.zhanghai.android.files.provider.common

import android.os.Parcel
import java8.nio.file.attribute.FileTime
import kotlinx.parcelize.Parceler
import org.threeten.bp.Instant
import me.zhanghai.android.files.compat.readSerializableCompat

object FileTimeParceler : Parceler<FileTime?> {
    override fun create(parcel: Parcel): FileTime? =
        parcel.readSerializableCompat<Instant>()?.let { FileTime.from(it) }

    override fun FileTime?.write(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(this?.toInstant())
    }
}
