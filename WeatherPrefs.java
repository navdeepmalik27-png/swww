package com.overcast.mini;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lightweight SharedPreferences cache.
 * Keeps widget rendering instant (0 network calls on redraw/resize/screen-on).
 */
public final class WeatherPrefs {
    private static final String PREF = "oc_mini";
    private static final String K_CITY = "city";
    private static final String K_LAT = "lat";
    private static final String K_LON = "lon";
    private static final String K_FAHRENHEIT = "fahr";
    private static final String K_INTERVAL_MIN = "iv_min";
    private static final String K_JSON = "json";
    private static final String K_UPDATED_MS = "ts";

    private WeatherPrefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void saveLocation(Context c, String city, double lat, double lon) {
        sp(c).edit()
            .putString(K_CITY, city)
            .putString(K_LAT, Double.toString(lat))
            .putString(K_LON, Double.toString(lon))
            .apply();
    }

    public static void saveSettings(Context c, boolean fahrenheit, int intervalMinutes) {
        sp(c).edit()
            .putBoolean(K_FAHRENHEIT, fahrenheit)
            .putInt(K_INTERVAL_MIN, intervalMinutes)
            .apply();
    }

    public static void saveCache(Context c, String rawJson, long nowMs) {
        sp(c).edit()
            .putString(K_JSON, rawJson)
            .putLong(K_UPDATED_MS, nowMs)
            .apply();
    }

    public static String getCity(Context c) {
        return sp(c).getString(K_CITY, "");
    }

    public static boolean hasLocation(Context c) {
        return !sp(c).getString(K_LAT, "").isEmpty();
    }

    public static double getLat(Context c) {
        try { return Double.parseDouble(sp(c).getString(K_LAT, "0")); }
        catch (NumberFormatException e) { return 0; }
    }

    public static double getLon(Context c) {
        try { return Double.parseDouble(sp(c).getString(K_LON, "0")); }
        catch (NumberFormatException e) { return 0; }
    }

    public static boolean isFahrenheit(Context c) {
        return sp(c).getBoolean(K_FAHRENHEIT, false);
    }

    /** Default 120 min (2 hours) for minimal radio/battery usage. */
    public static int getIntervalMinutes(Context c) {
        return sp(c).getInt(K_INTERVAL_MIN, 120);
    }

    public static String getCachedJson(Context c) {
        return sp(c).getString(K_JSON, "");
    }

    public static long getLastUpdatedMs(Context c) {
        return sp(c).getLong(K_UPDATED_MS, 0L);
    }
}
