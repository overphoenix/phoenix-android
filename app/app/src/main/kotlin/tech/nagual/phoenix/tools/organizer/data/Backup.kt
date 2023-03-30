package tech.nagual.phoenix.tools.organizer.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.nagual.phoenix.tools.organizer.data.model.*

@Serializable
data class Backup(
    val version: Int,
    val notes: Set<Note>? = null,
    val folders: Set<Folder>? = null,
    val reminders: Set<Reminder>? = null,
    val tags: Set<Tag>? = null,
    val joins: Set<NoteTagJoin>? = null,
    val categories: Set<Category>? = null,
    val categoryVariables: Set<CategoryVariable>? = null,
    val variants: Set<Variant>? = null
) {
    fun serialize() = Json.encodeToString(this)

    companion object {
        fun fromString(serialized: String): Backup = Json.decodeFromString(serialized)
    }
}
