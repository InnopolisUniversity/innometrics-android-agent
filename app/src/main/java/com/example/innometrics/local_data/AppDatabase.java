package com.example.innometrics.local_data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;


/**
 * For local SQL data Room library is used
 */
@Database(entities = {ForegroundApp.class, LocationItem.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract ForegroundAppDao foregroundAppDao();
    public abstract LocationItemDao locationDao();



    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "activity-database")
                            .build();
        }
        return instance;
    }
}