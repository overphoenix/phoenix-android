package tech.nagual.phoenix.tools.organizer.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class RawCategory(
    val id: Long,
    val name: String,
    val type: CategoryType,
    val flags: Long
) : Parcelable {

    fun isActive() = flags and FLAG_ACTIVE == FLAG_ACTIVE

    companion object {
        const val FLAG_ACTIVE = 0x1L
    }
}
