package tech.nagual.phoenix.tools.organizer.data.model

import android.os.Parcelable
import androidx.room.*
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.Instant

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            onDelete = ForeignKey.SET_NULL,
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"]
        ),
        ForeignKey(
            onDelete = ForeignKey.CASCADE,
            entity = Organizer::class,
            parentColumns = ["id"],
            childColumns = ["organizerId"]
        )
    ]
)
@Serializable
data class NoteEntity(
    val title: String,
    val content: String,
    val type: NoteViewType,
    val taskList: List<NoteTask>,
    val variants: List<RawVariant>,
    val isArchived: Boolean,
    val isDeleted: Boolean,
    val isPinned: Boolean,
    val isHidden: Boolean,
    val isMarkdownEnabled: Boolean,
    val creationDate: Long,
    val modifiedDate: Long,
    val deletionDate: Long?,
    val attachments: List<Attachment>,
    val color: NoteColor,
    @ColumnInfo(index = true) val folderId: Long?,
    @ColumnInfo(index = true) val organizerId: Long,
    @PrimaryKey(autoGenerate = true) val id: Long,
)

@Serializable
@Parcelize
data class Note(
    val title: String = "",
    val content: String = "",
    val type: NoteViewType = NoteViewType.Text,
    val taskList: List<NoteTask> = listOf(),
    val variants: List<RawVariant> = listOf(),
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val isMarkdownEnabled: Boolean = true,
    val creationDate: Long = Instant.now().epochSecond,
    val modifiedDate: Long = Instant.now().epochSecond,
    val deletionDate: Long? = null,
    val attachments: List<Attachment> = listOf(),
    val color: NoteColor = NoteColor.Default,
    val folderId: Long? = null,
    val organizerId: Long,
    val id: Long = 0L,
    @Relation(
        entity = Tag::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagJoin::class,
            parentColumn = "noteId",
            entityColumn = "tagId",
        )
    )
    val tags: List<Tag> = listOf(),
    @Relation(
        parentColumn = "id",
        entityColumn = "noteId",
    )
    val reminders: List<Reminder> = listOf(),
) : Parcelable {

    fun isEmpty(): Boolean {
        val baseCondition =
            title.isBlank() && attachments.isEmpty() && reminders.isEmpty() && variants.isEmpty() && tags.isEmpty()
        return when (type) {
            NoteViewType.Text -> baseCondition && content.isBlank()
            NoteViewType.TaskList -> baseCondition && taskList.isEmpty()
            NoteViewType.Categories -> baseCondition
        }
    }

    fun taskListToString(withCheckmarks: Boolean = false): String {
        return taskList.joinToString("\n") {
            val prefix = when {
                withCheckmarks -> if (it.isDone) "☑ " else "☐ "
                else -> ""
            }

            "$prefix${it.content.trim()}"
        }
    }

    fun categoriesToString(): String {
        var content = ""
        for (rawVariant in variants) {
            if (content.isNotEmpty())
                content += "\n"
            content += "${rawVariant.categoryName}: ${rawVariant.getCompleteValue("::")}"
        }
        return content
    }

    fun toEntity(): NoteEntity = NoteEntity(
        title = title,
        content = content,
        type = type,
        taskList = taskList,
        variants = variants,
        isArchived = isArchived,
        isDeleted = isDeleted,
        isPinned = isPinned,
        isHidden = isHidden,
        isMarkdownEnabled = isMarkdownEnabled,
        creationDate = creationDate,
        modifiedDate = modifiedDate,
        deletionDate = deletionDate,
        attachments = attachments,
        color = color,
        folderId = folderId,
        organizerId = organizerId,
        id = id
    )
}
