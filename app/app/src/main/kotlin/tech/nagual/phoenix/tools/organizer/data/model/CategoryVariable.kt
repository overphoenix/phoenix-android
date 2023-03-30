package tech.nagual.phoenix.tools.organizer.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(
    tableName = "category_variables",
    primaryKeys = ["categoryId", "name"],
    foreignKeys = [
        ForeignKey(
            onDelete = ForeignKey.CASCADE,
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"]
        ),
    ]
)
@Serializable
@Parcelize
data class CategoryVariable(
    val name: String,
    val value: String,
    val categoryId: Long
) : Parcelable
