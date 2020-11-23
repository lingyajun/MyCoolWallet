package com.bethel.mycoolwallet.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {AddressBook.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AddressBookDao addressBookDao();

    private static  AppDatabase instance;
    public static AppDatabase getInstance(Context context) {
        if (null == instance) {
            synchronized (AppDatabase.class) {
                if (null == instance) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class,
                            "db_address_book").allowMainThreadQueries().build();
                }
            }
        }
        return instance;
    }
}
