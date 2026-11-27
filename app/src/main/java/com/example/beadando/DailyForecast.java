package com.example.beadando;

/**
 * Egy nap előrejelzése (az előrejelzés lista egy eleme).
 */
public class DailyForecast {
    public final String dayLabel;
    public final int weatherCode;
    public final double maxTemp;
    public final double minTemp;

    public DailyForecast(String dayLabel, int weatherCode, double maxTemp, double minTemp) {
        this.dayLabel = dayLabel;
        this.weatherCode = weatherCode;
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
    }
}
