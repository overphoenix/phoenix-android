package tech.nagual.phoenix.tools.organizer.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class RawChildVariant(
    val id: Long,
    val value: String,
    var child: RawChildVariant? = null
) : Parcelable

@Serializable
@Parcelize
data class RawVariant(
    val id: Long,
    val value: String,
    val child: RawChildVariant? = null,
    val categoryId: Long,
    val categoryName: String,
    val categoryType: CategoryType,
    val flags: Long // Reserved for future use
) : Parcelable {
    fun getCompleteValue(separator: String = "\n"): String {
        return if (categoryType == CategoryType.ExVariants) {
            var value = value
            var rawChildVariant = child
            while (rawChildVariant != null) {
                value += "${separator}${rawChildVariant.value}"
                rawChildVariant = rawChildVariant.child
            }
            value
        } else value
    }
}
