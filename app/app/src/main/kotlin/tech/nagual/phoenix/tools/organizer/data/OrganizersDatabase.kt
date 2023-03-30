package tech.nagual.phoenix.tools.organizer.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import tech.nagual.phoenix.tools.organizer.data.dao.*
import tech.nagual.phoenix.tools.organizer.data.model.*

@Database(
    entities = [
        Organizer::class,
        NoteEntity::class,
        NoteTagJoin::class,
        Folder::class,
        Tag::class,
        Reminder::class,
        Category::class,
        CategoryVariable::class,
        Variant::class,
        Workflow::class
    ],
    version = 1,
)
@TypeConverters(DatabaseConverters::class)
abstract class OrganizersDatabase : RoomDatabase() {
    abstract val organizerDao: OrganizerDao
    abstract val noteDao: NoteDao
    abstract val folderDao: FolderDao
    abstract val noteTagDao: NoteTagDao
    abstract val tagDao: TagDao
    abstract val reminderDao: ReminderDao
    abstract val categoryDao: CategoryDao
    abstract val categoryVariableDao: CategoryVariableDao
    abstract val variantDao: VariantDao
    abstract val workflowDao: WorkflowDao

    companion object {
        const val DB_NAME = "app_database"
    }
}
