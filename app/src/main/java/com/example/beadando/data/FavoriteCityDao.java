package com.example.beadando.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * A kedvenc városok tábla lekérdezései. A Room ebből generálja az SQL-t futtató kódot.
 * Minden metódust háttérszálon kell hívni.
 */
@Dao
public interface FavoriteCityDao {

    @Query("SELECT * FROM favorite_cities ORDER BY name COLLATE NOCASE")
    List<FavoriteCity> getAll();

    /** Van-e már elmentett város kb. 5 km-en belül (így a GPS-hely és a keresett város egyezik). */
    @Query("SELECT * FROM favorite_cities "
            + "WHERE ABS(latitude - :lat) < 0.05 AND ABS(longitude - :lon) < 0.05 LIMIT 1")
    FavoriteCity findNear(double lat, double lon);

    @Insert
    long insert(FavoriteCity city);

    @Delete
    void delete(FavoriteCity city);
}
