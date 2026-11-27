package com.example.beadando.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Az app helyi SQLite adatbázisa (Room). Egyetlen példány létezik belőle (singleton).
 */
@Database(entities = {FavoriteCity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract FavoriteCityDao favoriteCityDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "idojaras.db").build();
                }
            }
        }
        return instance;
    }
}
