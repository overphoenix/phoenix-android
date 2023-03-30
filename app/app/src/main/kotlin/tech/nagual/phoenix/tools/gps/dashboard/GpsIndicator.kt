package tech.nagual.phoenix.tools.gps.dashboard

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class GpsIndicator(
    val name: String,
    @IgnoredOnParcel
    var value: String = "",
    @DrawableRes
    @IgnoredOnParcel
    val iconRes: Int,
    val isVisible: Boolean
) : Parcelable {
    @IgnoredOnParcel
    val id: Long = name.hashCode().toLong()
}
