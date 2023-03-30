package tech.nagual.phoenix.tools.organizer.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(
    tableName = "folders",
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
data class Folder(
    @ColumnInfo(name = "folderName") val name: String,
    @ColumnInfo(index = true) val organizerId: Long,
    @PrimaryKey(autoGenerate = true) val id: Long = 0L
) : Parcelable
