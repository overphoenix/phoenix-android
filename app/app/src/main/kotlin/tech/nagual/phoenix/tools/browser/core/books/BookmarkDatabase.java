package tech.nagual.phoenix.tools.browser.core.books;

import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {Bookmark.class}, version = 75, exportSchema = false)
public abstract class BookmarkDatabase extends RoomDatabase {


    public abstract BookmarkDao bookmarkDao();

}
