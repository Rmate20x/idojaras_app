package com.example.beadando;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.beadando.data.AppDatabase;
import com.example.beadando.data.FavoriteCity;
import com.example.beadando.data.FavoriteCityDao;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kedvenc városok: keresés (Open-Meteo geokódoló API), mentés, törlés és kiválasztás.
 * A városok a helyi Room adatbázisban vannak.
 */
public class FavoritesActivity extends AppCompatActivity {

    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LON = "lon";

    private EditText etSearch;
    private TextView tvEmpty;
    private FavoritesAdapter adapter;

    private FavoriteCityDao favoriteDao;
    private RequestQueue requestQueue;
    // Az adatbázis-műveletek nem futhatnak a fő (UI) szálon
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_favorites);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.favoritesRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            int pad = Math.round(24 * getResources().getDisplayMetrics().density);
            v.setPadding(bars.left + pad, bars.top + pad, bars.right + pad, bars.bottom + pad);
            return insets;
        });

        favoriteDao = AppDatabase.getInstance(this).favoriteCityDao();
        requestQueue = Volley.newRequestQueue(this);

        etSearch = findViewById(R.id.etSearch);
        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new FavoritesAdapter(this::selectCity, this::confirmDelete);
        RecyclerView rv = findViewById(R.id.rvFavorites);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btnSearch).setOnClickListener(v -> searchCity());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchCity();
                return true;
            }
            return false;
        });

        loadFavorites();
    }

    // --- Adatbázis ---

    private void loadFavorites() {
        dbExecutor.execute(() -> {
            List<FavoriteCity> cities = favoriteDao.getAll();
            runOnUiThread(() -> {
                adapter.setItems(cities);
                tvEmpty.setVisibility(cities.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void addFavorite(FavoriteCity city) {
        dbExecutor.execute(() -> {
            boolean exists = favoriteDao.findNear(city.latitude, city.longitude) != null;
            if (!exists) {
                favoriteDao.insert(city);
            }
            runOnUiThread(() -> Toast.makeText(this,
                    getString(exists ? R.string.toast_favorite_exists : R.string.toast_favorite_added, city.name),
                    Toast.LENGTH_SHORT).show());
            loadFavorites();
        });
    }

    private void confirmDelete(FavoriteCity city) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(getString(R.string.delete_confirm_message, city.name))
                .setPositiveButton(R.string.delete, (d, w) -> deleteFavorite(city))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteFavorite(FavoriteCity city) {
        dbExecutor.execute(() -> {
            favoriteDao.delete(city);
            runOnUiThread(() -> Toast.makeText(this,
                    getString(R.string.toast_favorite_removed, city.name), Toast.LENGTH_SHORT).show());
            loadFavorites();
        });
    }

    // A kiválasztott várost visszaadjuk a főképernyőnek
    private void selectCity(FavoriteCity city) {
        Intent data = new Intent();
        data.putExtra(EXTRA_NAME, city.name);
        data.putExtra(EXTRA_LAT, city.latitude);
        data.putExtra(EXTRA_LON, city.longitude);
        setResult(RESULT_OK, data);
        finish();
    }

    // --- Városkeresés ---

    private void searchCity() {
        Editable text = etSearch.getText();
        String query = text == null ? "" : text.toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, R.string.search_empty_query, Toast.LENGTH_SHORT).show();
            return;
        }
        hideKeyboard();

        String encoded;
        try {
            encoded = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return; // UTF-8 mindig elérhető
        }
        String url = "https://geocoding-api.open-meteo.com/v1/search?count=5&language=hu&format=json&name=" + encoded;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> showSearchResults(response.optJSONArray("results")),
                error -> Toast.makeText(this, R.string.error_api, Toast.LENGTH_SHORT).show());
        requestQueue.add(request);
    }

    private void showSearchResults(JSONArray results) {
        if (results == null || results.length() == 0) {
            Toast.makeText(this, R.string.search_no_results, Toast.LENGTH_SHORT).show();
            return;
        }

        List<FavoriteCity> candidates = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < results.length(); i++) {
            JSONObject r = results.optJSONObject(i);
            if (r == null) continue;
            String name = r.optString("name");
            String country = r.optString("country", "");
            String region = r.optString("admin1", "");

            candidates.add(new FavoriteCity(name, country, r.optDouble("latitude"), r.optDouble("longitude")));
            StringBuilder label = new StringBuilder(name);
            if (!region.isEmpty() && !region.equals(name)) label.append(", ").append(region);
            if (!country.isEmpty()) label.append(", ").append(country);
            labels.add(label.toString());
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.search_results)
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    addFavorite(candidates.get(which));
                    etSearch.setText("");
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
