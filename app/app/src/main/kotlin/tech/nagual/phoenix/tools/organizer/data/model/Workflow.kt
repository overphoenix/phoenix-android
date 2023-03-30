package tech.nagual.phoenix.tools.organizer.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(
    tableName = "workflows",
    foreignKeys = [
        ForeignKey(
            onDelete = ForeignKey.CASCADE,
            entity = Organizer::class,
            parentColumns = ["id"],
            childColumns = ["organizerId"]
        ),
        ForeignKey(
            onDelete = ForeignKey.SET_NULL,
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"]
        )
    ]
)
@Serializable
@Parcelize
data class Workflow(
    val name: String,
    val creationDate: Long,
    val active: Boolean = false,
    val noteViewType: NoteViewType = NoteViewType.Text,
    val folderId: Long? = null,
    val attachmentType: Attachment.Type? = null,
    val isHiddenNote: Boolean = false,
    override val categories: List<RawCategory> = listOf(),
    @ColumnInfo(index = true) val organizerId: Long,
    @PrimaryKey(autoGenerate = true) val id: Long = 0L
) : RawCategoriesHolder, Parcelable
