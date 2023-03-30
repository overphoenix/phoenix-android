package tech.nagual.phoenix.tools.organizer.data

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.nagual.phoenix.tools.organizer.data.model.*
import java.util.*

object DatabaseConverters {
    @TypeConverter
    fun jsonFromAttachments(attachments: List<Attachment>): String =
        Json.encodeToString(attachments)

    @TypeConverter
    fun attachmentsFromJson(json: String): List<Attachment> = Json.decodeFromString(json)

    @TypeConverter
    fun jsonFromTasks(tasks: List<NoteTask>): String = Json.encodeToString(tasks)

    @TypeConverter
    fun tasksFromJson(json: String): List<NoteTask> = Json.decodeFromString(json)

    @TypeConverter
    fun jsonFromRawVariant(variants: List<RawVariant>): String = Json.encodeToString(variants)

    @TypeConverter
    fun rawVariantsFromJson(json: String): List<RawVariant> = Json.decodeFromString(json)

    @TypeConverter
    fun jsonFromRawCategories(rawCategories: List<RawCategory>): String = Json.encodeToString(rawCategories)

    @TypeConverter
    fun rawCategoriesFromJson(json: String): List<RawCategory> = Json.decodeFromString(json)

    @TypeConverter
    fun stringFromTypeEnum(color: NoteViewType): String = color.name

    @TypeConverter
    fun typeEnumFromString(name: String): NoteViewType = NoteViewType.valueOf(name)

    @TypeConverter
    fun stringFromColorEnum(color: NoteColor): String = color.name

    @TypeConverter
    fun colorEnumFromString(name: String): NoteColor = NoteColor.valueOf(name)

    @TypeConverter
    fun ordinalFromCategoryTypeEnum(categoryType: CategoryType): Int = categoryType.ordinal

    @TypeConverter
    fun categoryTypeEnumFromOrdinal(ordinal: Int): CategoryType = CategoryType.values()[ordinal]

    @TypeConverter
    fun dateFromLong(dateLong: Long): Date = Date(dateLong)

    @TypeConverter
    fun longFromDate(date: Date): Long = date.time
}
