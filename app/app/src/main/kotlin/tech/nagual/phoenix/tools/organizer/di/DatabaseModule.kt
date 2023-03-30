package tech.nagual.phoenix.tools.organizer.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import tech.nagual.phoenix.tools.organizer.data.OrganizersDatabase
import javax.inject.Singleton


var rdc: RoomDatabase.Callback = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        // do something after database has been created
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        // do something every time database is open
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRoomDatabase(
        @ApplicationContext context: Context,
    ): OrganizersDatabase {
        return Room.databaseBuilder(context, OrganizersDatabase::class.java, OrganizersDatabase.DB_NAME)
            .fallbackToDestructiveMigration()
            .addCallback(rdc)
            .build()
    }
}
