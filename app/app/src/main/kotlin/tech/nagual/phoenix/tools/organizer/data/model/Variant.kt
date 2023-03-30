package tech.nagual.phoenix.tools.organizer.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(
    tableName = "variants",
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
data class Variant(
    val value: String,
    @ColumnInfo(index = true) val categoryId: Long,
    @ColumnInfo(index = true) val parentId: Long = 0,
    @PrimaryKey(autoGenerate = true) val id: Long = 0
) : Parcelable