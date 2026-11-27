package com.example.beadando;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * A 7 napos előrejelzés vízszintes listája.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {

    private final List<DailyForecast> items = new ArrayList<>();

    public void setItems(List<DailyForecast> days) {
        items.clear();
        items.addAll(days);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forecast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyForecast day = items.get(position);
        holder.tvDay.setText(day.dayLabel);
        holder.tvEmoji.setText(WeatherCodes.emoji(day.weatherCode, true));
        holder.tvMax.setText(holder.itemView.getContext().getString(R.string.temp_short_format, Math.round(day.maxTemp)));
        holder.tvMin.setText(holder.itemView.getContext().getString(R.string.temp_short_format, Math.round(day.minTemp)));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDay;
        final TextView tvEmoji;
        final TextView tvMax;
        final TextView tvMin;

        ViewHolder(View view) {
            super(view);
            tvDay = view.findViewById(R.id.tvDay);
            tvEmoji = view.findViewById(R.id.tvDayEmoji);
            tvMax = view.findViewById(R.id.tvMax);
            tvMin = view.findViewById(R.id.tvMin);
        }
    }
}
