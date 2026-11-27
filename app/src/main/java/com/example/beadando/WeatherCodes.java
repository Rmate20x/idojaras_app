package com.example.beadando;

import androidx.annotation.StringRes;

/**
 * Az Open-Meteo időjáráskódjainak (WMO) fordítása szövegre, emojira és háttérszínre.
 */
public final class WeatherCodes {

    private WeatherCodes() {
    }

    @StringRes
    public static int description(int code) {
        if (code == 0) return R.string.weather_clear;
        if (code == 1 || code == 2) return R.string.weather_partly_cloudy;
        if (code == 3) return R.string.weather_overcast;
        if (code == 45 || code == 48) return R.string.weather_fog;
        if (code >= 51 && code <= 55) return R.string.weather_drizzle;
        if (code == 56 || code == 57) return R.string.weather_freezing_drizzle;
        if (code >= 61 && code <= 65) return R.string.weather_rain;
        if (code == 66 || code == 67) return R.string.weather_freezing_rain;
        if (code >= 71 && code <= 77) return R.string.weather_snow;
        if (code >= 80 && code <= 82) return R.string.weather_showers;
        if (code == 85 || code == 86) return R.string.weather_snow_showers;
        if (code >= 95) return R.string.weather_thunderstorm;
        return R.string.weather_unknown;
    }

    public static String emoji(int code, boolean isDay) {
        if (code == 0) return isDay ? "☀️" : "🌙";
        if (code == 1 || code == 2) return isDay ? "⛅" : "☁️";
        if (code == 3) return "☁️";
        if (code == 45 || code == 48) return "🌫️";
        if (code >= 51 && code <= 67) return "🌧️";
        if (code >= 71 && code <= 77) return "❄️";
        if (code >= 80 && code <= 82) return "🌦️";
        if (code == 85 || code == 86) return "🌨️";
        if (code >= 95) return "⛈️";
        return "❓";
    }

    /** A háttér színátmenete (felső és alsó szín) az időjárás és a napszak szerint. */
    public static int[] backgroundColors(int code, boolean isDay) {
        if (!isDay) return new int[]{0xFF1A237E, 0xFF0D1030};                   // éjszaka
        if (code == 0 || code == 1) return new int[]{0xFF29B6F6, 0xFF1565C0};   // napos
        if (code == 2 || code == 3) return new int[]{0xFF78909C, 0xFF37474F};   // felhős
        if (code == 45 || code == 48) return new int[]{0xFF90A4AE, 0xFF546E7A}; // köd
        if (code >= 71 && code <= 77 || code == 85 || code == 86) {
            return new int[]{0xFF7BA4CF, 0xFF3F5F8A};                           // hó
        }
        if (code >= 95) return new int[]{0xFF455A64, 0xFF1C2529};               // zivatar
        if (code >= 51) return new int[]{0xFF5C7A99, 0xFF263B52};               // eső
        return new int[]{0xFF42A5F5, 0xFF1976D2};
    }
}
