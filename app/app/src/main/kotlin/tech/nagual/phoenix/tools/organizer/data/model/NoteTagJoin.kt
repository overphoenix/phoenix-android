package tech.nagual.phoenix.tools.organizer.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import kotlinx.serialization.Serializable

@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(
            onDelete = CASCADE,
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"]
        ),
        ForeignKey(
            onDelete = CASCADE,
            onUpdate = CASCADE,
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"]
        )
    ],
)
@Serializable
data class NoteTagJoin(
    @ColumnInfo(index = true) val tagId: Long = 0L,
    @ColumnInfo(index = true) val noteId: Long = 0L
)
