package tech.nagual.phoenix.tools.organizer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import tech.nagual.phoenix.BuildConfig
import tech.nagual.phoenix.tools.organizer.components.MediaStorageManager
import tech.nagual.phoenix.tools.organizer.backup.BackupManager
import tech.nagual.phoenix.tools.organizer.reminders.ReminderManager
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.repo.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilModule {

    @Provides
    @Singleton
    fun provideMediaStorageManager(
        @ApplicationContext context: Context,
        noteRepository: NoteRepository,
    ) = MediaStorageManager(context, noteRepository, OrganizersManager.MEDIA_FOLDER)

    @Provides
    @Singleton
    fun provideReminderManager(
        @ApplicationContext context: Context,
        reminderRepository: ReminderRepository,
    ) = ReminderManager(context, reminderRepository)

    @Provides
    @Singleton
    fun provideBackupManager(
        noteRepository: NoteRepository,
        folderRepository: FolderRepository,
        tagRepository: TagRepository,
        categoriesRepository: CategoriesRepository,
        reminderRepository: ReminderRepository,
        reminderManager: ReminderManager,
        @ApplicationContext context: Context,
    ) = BackupManager(
        BuildConfig.VERSION_CODE,
        noteRepository,
        folderRepository,
        tagRepository,
        categoriesRepository,
        reminderRepository,
        reminderManager,
        context
    )
}
