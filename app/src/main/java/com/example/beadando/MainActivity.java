package com.example.beadando;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.beadando.data.AppDatabase;
import com.example.beadando.data.FavoriteCity;
import com.example.beadando.data.FavoriteCityDao;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String KEY_USE_GPS = "useGps";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_NAME = "name";

    private View rootView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvLocation;
    private TextView tvTemperature;
    private TextView tvWeatherCode;
    private TextView tvWeatherEmoji;
    private TextView tvWindSpeed;
    private TextView tvHumidity;
    private TextView tvPressure;
    private MaterialButton btnFavoriteToggle;
    private final ForecastAdapter forecastAdapter = new ForecastAdapter();

    private FusedLocationProviderClient fusedLocationClient;
    private RequestQueue requestQueue;
    private FavoriteCityDao favoriteDao;
    // Az adatbázis-műveletek nem futhatnak a fő (UI) szálon
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    // Az éppen megjelenített hely: saját GPS-hely, vagy egy kedvencek közül választott város
    private boolean useGps = true;
    private boolean hasLocation = false;
    private double currentLat;
    private double currentLon;
    private String currentName;
    private String currentCountry;
    private FavoriteCity currentFavorite; // null, ha a mostani hely nincs a kedvencek között

    private final ActivityResultLauncher<Intent> favoritesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (result.getResultCode() == RESULT_OK && data != null) {
                    showCity(data.getStringExtra(FavoritesActivity.EXTRA_NAME),
                            data.getDoubleExtra(FavoritesActivity.EXTRA_LAT, 0),
                            data.getDoubleExtra(FavoritesActivity.EXTRA_LON, 0));
                } else {
                    // Lehet, hogy közben töröltek egy kedvencet: frissítjük a csillagot
                    updateFavoriteButton();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Sötét háttér van, ezért világos ikonok kellenek az állapotsorban
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);

        rootView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvLocation = findViewById(R.id.tvLocation);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvWeatherCode = findViewById(R.id.tvWeatherCode);
        tvWeatherEmoji = findViewById(R.id.tvWeatherEmoji);
        tvWindSpeed = findViewById(R.id.tvWindSpeed);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvPressure = findViewById(R.id.tvPressure);
        btnFavoriteToggle = findViewById(R.id.btnFavoriteToggle);

        RecyclerView rvForecast = findViewById(R.id.rvForecast);
        rvForecast.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvForecast.setAdapter(forecastAdapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestQueue = Volley.newRequestQueue(this);
        favoriteDao = AppDatabase.getInstance(this).favoriteCityDao();

        swipeRefresh.setColorSchemeResources(R.color.primaryDarkColor);
        swipeRefresh.setOnRefreshListener(this::refresh);
        btnFavoriteToggle.setOnClickListener(v -> toggleFavorite());
        findViewById(R.id.btnFavorites).setOnClickListener(v ->
                favoritesLauncher.launch(new Intent(this, FavoritesActivity.class)));
        findViewById(R.id.btnMyLocation).setOnClickListener(v -> {
            useGps = true;
            refresh();
        });

        // Elforgatás után ugyanazt a várost mutatjuk, amit előtte
        if (savedInstanceState != null && !savedInstanceState.getBoolean(KEY_USE_GPS, true)) {
            showCity(savedInstanceState.getString(KEY_NAME),
                    savedInstanceState.getDouble(KEY_LAT),
                    savedInstanceState.getDouble(KEY_LON));
        } else {
            refresh();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_USE_GPS, useGps);
        outState.putDouble(KEY_LAT, currentLat);
        outState.putDouble(KEY_LON, currentLon);
        outState.putString(KEY_NAME, currentName);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }

    /** Frissítés: saját hely esetén újra lekéri a GPS-t, választott városnál csak az időjárást. */
    private void refresh() {
        if (useGps) {
            checkLocationPermissionAndFetch();
        } else {
            fetchWeatherData(currentLat, currentLon);
        }
    }

    /** Egy kedvenc város időjárásának megjelenítése. */
    private void showCity(String name, double lat, double lon) {
        useGps = false;
        hasLocation = true;
        currentName = name;
        currentCountry = null;
        currentLat = lat;
        currentLon = lon;
        tvLocation.setText(getString(R.string.location_city_format, name));
        updateFavoriteButton();
        fetchWeatherData(lat, lon);
    }

    // --- Helymeghatározás ---

    private void checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
            swipeRefresh.setRefreshing(false);
            return;
        }

        swipeRefresh.setRefreshing(true);
        tvLocation.setText(getString(R.string.location_searching));
        // Friss helyet kérünk GPS-szel (emulátoron csak GPS van, wifi/cella alapú hely nincs).
        // Ha ez nem ad eredményt, a telefon utoljára ismert helyét használjuk.
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        onLocationFound(location);
                    } else {
                        useLastKnownLocation();
                    }
                })
                .addOnFailureListener(this, e -> useLastKnownLocation());
    }

    @SuppressLint("MissingPermission") // az engedélyt a checkLocationPermissionAndFetch már ellenőrizte
    private void useLastKnownLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        onLocationFound(location);
                    } else {
                        showLocationNotFound();
                    }
                })
                .addOnFailureListener(this, e -> showLocationNotFound());
    }

    private void onLocationFound(Location location) {
        if (!useGps) return; // közben a felhasználó egy kedvenc várost választott
        currentLat = location.getLatitude();
        currentLon = location.getLongitude();
        currentName = String.format(Locale.US, "%.4f, %.4f", currentLat, currentLon);
        currentCountry = null;
        hasLocation = true;
        tvLocation.setText(getString(R.string.location_format,
                String.format(Locale.US, "%.4f", currentLat), String.format(Locale.US, "%.4f", currentLon)));
        showCityName(currentLat, currentLon);
        updateFavoriteButton();
        fetchWeatherData(currentLat, currentLon);
    }

    private void showLocationNotFound() {
        swipeRefresh.setRefreshing(false);
        tvLocation.setText(getString(R.string.location_not_found));
        Toast.makeText(this, R.string.error_gps_off, Toast.LENGTH_SHORT).show();
    }

    // A koordinátákból városnevet keres (Geocoder), és ha talál, azt írja ki
    private void showCityName(double lat, double lon) {
        if (!Geocoder.isPresent()) return;
        Geocoder geocoder = new Geocoder(this, Locale.forLanguageTag("hu-HU"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lon, 1, addresses -> runOnUiThread(() -> setCityName(addresses)));
        } else {
            // A régi API blokkol, ezért háttérszálon hívjuk
            new Thread(() -> {
                try {
                    @SuppressWarnings("deprecation")
                    List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                    runOnUiThread(() -> setCityName(addresses));
                } catch (IOException ignored) {
                    // Nincs hálózat vagy szolgáltatás: maradnak a koordináták
                }
            }).start();
        }
    }

    private void setCityName(List<Address> addresses) {
        if (!useGps || addresses == null || addresses.isEmpty()) return;
        String city = addresses.get(0).getLocality();
        if (city != null) {
            currentName = city;
            currentCountry = addresses.get(0).getCountryName();
            tvLocation.setText(getString(R.string.location_city_format, city));
        }
    }

    // --- Kedvencek (adatbázis) ---

    /** Megnézi az adatbázisban, hogy a mostani hely kedvenc-e, és ennek megfelelően állítja a gombot. */
    private void updateFavoriteButton() {
        if (!hasLocation) {
            btnFavoriteToggle.setEnabled(false);
            return;
        }
        final double lat = currentLat;
        final double lon = currentLon;
        dbExecutor.execute(() -> {
            FavoriteCity found = favoriteDao.findNear(lat, lon);
            runOnUiThread(() -> {
                currentFavorite = found;
                btnFavoriteToggle.setText(found != null ? R.string.favorite_saved : R.string.favorite_add);
                btnFavoriteToggle.setEnabled(true);
            });
        });
    }

    /** Csillag gomb: ha még nem kedvenc, elmenti; ha már az, törli. */
    private void toggleFavorite() {
        if (!hasLocation) return;
        btnFavoriteToggle.setEnabled(false);
        final FavoriteCity existing = currentFavorite;
        final FavoriteCity newCity = new FavoriteCity(currentName, currentCountry, currentLat, currentLon);
        dbExecutor.execute(() -> {
            if (existing != null) {
                favoriteDao.delete(existing);
            } else {
                favoriteDao.insert(newCity);
            }
            runOnUiThread(() -> Toast.makeText(this, existing != null
                    ? getString(R.string.toast_favorite_removed, existing.name)
                    : getString(R.string.toast_favorite_added, newCity.name), Toast.LENGTH_SHORT).show());
            runOnUiThread(this::updateFavoriteButton);
        });
    }

    // --- Időjárás API ---

    private void fetchWeatherData(double lat, double lon) {
        swipeRefresh.setRefreshing(true);
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + String.format(Locale.US, "%.4f", lat)
                + "&longitude=" + String.format(Locale.US, "%.4f", lon)
                + "&current=temperature_2m,relative_humidity_2m,surface_pressure,wind_speed_10m,weather_code,is_day"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                + "&timezone=auto";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    swipeRefresh.setRefreshing(false);
                    try {
                        showCurrentWeather(response.getJSONObject("current"));
                        showForecast(response.getJSONObject("daily"));
                    } catch (JSONException e) {
                        Toast.makeText(MainActivity.this, R.string.error_parse, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(MainActivity.this, R.string.error_api, Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(jsonObjectRequest);
    }

    private void showCurrentWeather(JSONObject current) throws JSONException {
        double temp = current.getDouble("temperature_2m");
        int weatherCode = current.getInt("weather_code");
        boolean isDay = current.optInt("is_day", 1) == 1;

        tvTemperature.setText(getString(R.string.temperature_format, temp));
        tvWeatherCode.setText(WeatherCodes.description(weatherCode));
        tvWeatherEmoji.setText(WeatherCodes.emoji(weatherCode, isDay));
        tvWindSpeed.setText(getString(R.string.wind_speed_format, current.getDouble("wind_speed_10m")));
        tvHumidity.setText(getString(R.string.humidity_format, current.getInt("relative_humidity_2m")));
        tvPressure.setText(getString(R.string.pressure_format, Math.round(current.getDouble("surface_pressure"))));

        applyBackground(weatherCode, isDay);
    }

    private void showForecast(JSONObject daily) throws JSONException {
        JSONArray dates = daily.getJSONArray("time");
        JSONArray codes = daily.getJSONArray("weather_code");
        JSONArray maxTemps = daily.getJSONArray("temperature_2m_max");
        JSONArray minTemps = daily.getJSONArray("temperature_2m_min");

        List<DailyForecast> days = new ArrayList<>();
        for (int i = 0; i < dates.length(); i++) {
            String label = i == 0 ? getString(R.string.forecast_today) : dayName(dates.getString(i));
            days.add(new DailyForecast(label, codes.getInt(i), maxTemps.getDouble(i), minTemps.getDouble(i)));
        }
        forecastAdapter.setItems(days);
    }

    /** "2026-11-28" → "Szo" */
    private String dayName(String isoDate) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate);
            if (date == null) return isoDate;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return getResources().getStringArray(R.array.day_short)[cal.get(Calendar.DAY_OF_WEEK) - 1];
        } catch (ParseException e) {
            return isoDate;
        }
    }

    /** Színátmenetes háttér az időjárás és a napszak szerint. */
    private void applyBackground(int weatherCode, boolean isDay) {
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, WeatherCodes.backgroundColors(weatherCode, isDay));
        rootView.setBackground(gradient);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Elég bármelyik engedély: Android 12+ alatt a felhasználó választhat
            // "hozzávetőleges" helyet is, ilyenkor csak a COARSE engedélyt kapjuk meg
            boolean anyGranted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    anyGranted = true;
                    break;
                }
            }
            if (anyGranted) {
                checkLocationPermissionAndFetch();
            } else {
                Toast.makeText(this, R.string.error_permission_denied, Toast.LENGTH_SHORT).show();
                tvLocation.setText(getString(R.string.location_permission_denied));
            }
        }
    }
}
