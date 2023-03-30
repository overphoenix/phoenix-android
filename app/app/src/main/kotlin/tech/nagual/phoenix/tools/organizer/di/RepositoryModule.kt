package tech.nagual.phoenix.tools.organizer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.nagual.phoenix.tools.organizer.data.OrganizersDatabase
//import tech.nagual.phoenix.tools.gps.data.repo.GpsRepository
import tech.nagual.phoenix.tools.organizer.data.repo.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideNotebookRepository(
        appDatabase: OrganizersDatabase
    ) = FolderRepository(appDatabase.folderDao)

    @Provides
    @Singleton
    fun provideNoteRepository(
        appDatabase: OrganizersDatabase,
    ) = NoteRepository(appDatabase.noteDao, appDatabase.reminderDao)

    @Provides
    @Singleton
    fun provideReminderRepository(appDatabase: OrganizersDatabase) =
        ReminderRepository(appDatabase.reminderDao)

    @Provides
    @Singleton
    fun provideTagRepository(
        appDatabase: OrganizersDatabase
    ) = TagRepository(appDatabase.tagDao, appDatabase.noteTagDao)

    @Provides
    @Singleton
    fun provideCategoryRepository(
        appDatabase: OrganizersDatabase
    ) = CategoriesRepository(appDatabase.categoryDao, appDatabase.variantDao, appDatabase.categoryVariableDao)

    @Provides
    @Singleton
    fun provideOrganizerRepository(
        appDatabase: OrganizersDatabase,
    ) = OrganizerRepository(appDatabase.organizerDao)

    @Provides
    @Singleton
    fun provideWorkflowRepository(
        appDatabase: OrganizersDatabase,
    ) = WorkflowRepository(appDatabase.workflowDao)
}
