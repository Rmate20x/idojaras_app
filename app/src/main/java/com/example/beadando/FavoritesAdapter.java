package com.example.beadando;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beadando.data.FavoriteCity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A kedvenc városok listájának elemei (RecyclerView).
 */
public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    public interface CityListener {
        void onCity(FavoriteCity city);
    }

    private final List<FavoriteCity> items = new ArrayList<>();
    private final CityListener onSelect;
    private final CityListener onDelete;

    public FavoritesAdapter(CityListener onSelect, CityListener onDelete) {
        this.onSelect = onSelect;
        this.onDelete = onDelete;
    }

    public void setItems(List<FavoriteCity> cities) {
        items.clear();
        items.addAll(cities);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FavoriteCity city = items.get(position);
        holder.tvName.setText(city.name);

        String coords = String.format(Locale.US, "%.2f, %.2f", city.latitude, city.longitude);
        holder.tvSubtitle.setText(city.country != null && !city.country.isEmpty()
                ? city.country + " · " + coords
                : coords);

        holder.itemView.setOnClickListener(v -> onSelect.onCity(city));
        holder.btnDelete.setOnClickListener(v -> onDelete.onCity(city));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvSubtitle;
        final View btnDelete;

        ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tvCityName);
            tvSubtitle = view.findViewById(R.id.tvCitySubtitle);
            btnDelete = view.findViewById(R.id.btnDelete);
        }
    }
}
