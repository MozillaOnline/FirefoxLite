package org.mozilla.focus.persistence;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

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
                            BookmarksDatabase.class, "bookmarks.db").addMigrations(BookmarksDatabase.MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}
