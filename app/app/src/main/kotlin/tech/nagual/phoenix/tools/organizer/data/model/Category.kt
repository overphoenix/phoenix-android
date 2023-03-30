package tech.nagual.phoenix.tools.organizer.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import tech.nagual.phoenix.tools.organizer.OrganizersManager

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            onDelete = ForeignKey.CASCADE,
            entity = Organizer::class,
            parentColumns = ["id"],
            childColumns = ["organizerId"]
        )
    ]
)
@Serializable
@Parcelize
data class Category(
    val name: String,
    val type: CategoryType,
    @ColumnInfo(index = true) val organizerId: Long = OrganizersManager.activeOrganizer.id,
    @PrimaryKey(autoGenerate = true) val id: Long = 0L
) : Parcelable

