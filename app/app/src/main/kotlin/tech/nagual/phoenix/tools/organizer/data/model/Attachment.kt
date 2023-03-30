package tech.nagual.phoenix.tools.organizer.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Attachment(
    val type: Type = Type.PHOTO,
    val path: String = "",
    val description: String = "",
    val fileName: String = "",
) : Parcelable {
    enum class Type {
        GENERIC,
        PHOTO,
        VIDEO,
        AUDIO,
    }

    fun isEmpty() = path.isEmpty() && description.isEmpty() && fileName.isEmpty()
}
