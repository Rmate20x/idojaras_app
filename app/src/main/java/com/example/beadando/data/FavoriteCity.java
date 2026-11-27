package com.example.beadando.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Egy kedvenc város: ez az adatbázis "favorite_cities" táblájának egy sora.
 */
@Entity(tableName = "favorite_cities")
public class FavoriteCity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name;

    /** Ország vagy megye, a listában alcímként jelenik meg (lehet üres). */
    @Nullable
    public String country;

    public double latitude;
    public double longitude;

    /** Mikor mentettük el (ezredmásodperc). */
    public long createdAt;

    public FavoriteCity(@NonNull String name, @Nullable String country, double latitude, double longitude) {
        this.name = name;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = System.currentTimeMillis();
    }
}
