package com.example.beadando;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    
    private TextView tvLocation;
    private TextView tvTemperature;
    private TextView tvWeatherCode;
    private TextView tvWeatherEmoji;
    private TextView tvWindSpeed;
    private TextView tvHumidity;
    private TextView tvPressure;

    private FusedLocationProviderClient fusedLocationClient;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvLocation = findViewById(R.id.tvLocation);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvWeatherCode = findViewById(R.id.tvWeatherCode);
        tvWeatherEmoji = findViewById(R.id.tvWeatherEmoji);
        tvWindSpeed = findViewById(R.id.tvWindSpeed);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvPressure = findViewById(R.id.tvPressure);
        Button btnRefresh = findViewById(R.id.btnRefresh);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestQueue = Volley.newRequestQueue(this);

        btnRefresh.setOnClickListener(v -> checkLocationPermissionAndFetch());

        checkLocationPermissionAndFetch();
    }

    private void checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 
                LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        tvLocation.setText(getString(R.string.location_searching));
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        tvLocation.setText(getString(R.string.location_format, String.format(Locale.US, "%.4f", lat), String.format(Locale.US, "%.4f", lon)));
                        fetchWeatherData(lat, lon);
                    } else {
                        tvLocation.setText(getString(R.string.location_not_found));
                        Toast.makeText(MainActivity.this, "Kérlek kapcsold be a GPS-t, majd próbáld újra!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchWeatherData(double lat, double lon) {
        // Hozzáadtuk a szélsebesség, relatív páratartalom és nyomás lekérését
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + 
                     "&current=temperature_2m,relative_humidity_2m,surface_pressure,wind_speed_10m,weather_code";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject current = response.getJSONObject("current");
                        
                        double temp = current.getDouble("temperature_2m");
                        int weatherCode = current.getInt("weather_code");
                        double windSpeed = current.getDouble("wind_speed_10m");
                        int humidity = current.getInt("relative_humidity_2m");
                        double pressure = current.getDouble("surface_pressure");

                        tvTemperature.setText(getString(R.string.temperature_format, temp));
                        tvWeatherCode.setText(getWeatherDescription(weatherCode));
                        tvWeatherEmoji.setText(getWeatherEmoji(weatherCode));
                        
                        tvWindSpeed.setText(getString(R.string.wind_speed_format, windSpeed));
                        tvHumidity.setText(getString(R.string.humidity_format, humidity));
                        tvPressure.setText(getString(R.string.pressure_format, Math.round(pressure)));

                    } catch (JSONException e) {
                        Toast.makeText(MainActivity.this, "Hiba az adatok feldolgozásakor", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(MainActivity.this, "Hiba az API hívásakor", Toast.LENGTH_SHORT).show());

        requestQueue.add(jsonObjectRequest);
    }

    private String getWeatherDescription(int code) {
        if (code == 0) return "Tiszta égbolt";
        if (code == 1 || code == 2 || code == 3) return "Részben felhős";
        if (code == 45 || code == 48) return "Ködös";
        if (code >= 51 && code <= 55) return "Szitáló eső";
        if (code >= 61 && code <= 65) return "Eső";
        if (code >= 71 && code <= 77) return "Hóesés";
        if (code >= 80 && code <= 82) return "Zápor";
        if (code >= 95) return "Zivatar";
        return "Ismeretlen";
    }

    private String getWeatherEmoji(int code) {
        if (code == 0) return "☀️";
        if (code == 1 || code == 2 || code == 3) return "⛅";
        if (code == 45 || code == 48) return "🌫️";
        if (code >= 51 && code <= 55) return "🌧️";
        if (code >= 61 && code <= 65) return "🌧️";
        if (code >= 71 && code <= 77) return "❄️";
        if (code >= 80 && code <= 82) return "🌦️";
        if (code >= 95) return "⛈️";
        return "❓";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermissionAndFetch();
            } else {
                Toast.makeText(this, "Helymeghatározás engedély megtagadva", Toast.LENGTH_SHORT).show();
                tvLocation.setText("Helyzet: Engedély megtagadva");
            }
        }
    }
}