package org.mozilla.focus.persistence;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {BookmarkModel.class}, version = 2)
public abstract class BookmarksDatabase extends RoomDatabase {

    private static volatile BookmarksDatabase instance;

    public abstract BookmarkDao bookmarkDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    };

    public static BookmarksDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (BookmarksDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            BookmarksDatabase.class, "bookmarks.db")
                            .build();
                }
            }
        }
        return instance;
    }
}
