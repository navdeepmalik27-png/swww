package com.overcast.mini;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Zero-reflection parser for Open-Meteo Forecast API JSON.
 */
public final class WeatherData {
    public int temp;
    public int feelsLike;
    public int high;
    public int low;
    public int wmoCode;
    public boolean isDay;
    public int precipProb;

    public final String[] hourLabels = new String[4];
    public final int[] hourTemps = new int[4];
    public final int[] hourCodes = new int[4];
    public final int[] hourRain = new int[4];
    public int hourCount;

    public static WeatherData parse(String rawJson) {
        if (rawJson == null || rawJson.isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(rawJson);
            JSONObject cur = root.getJSONObject("current");
            WeatherData d = new WeatherData();
            d.temp = (int) Math.round(cur.getDouble("temperature_2m"));
            d.feelsLike = (int) Math.round(cur.optDouble("apparent_temperature", d.temp));
            d.wmoCode = cur.optInt("weather_code", 0);
            d.isDay = cur.optInt("is_day", 1) == 1;
            d.precipProb = cur.optInt("precipitation_probability", 0);

            JSONObject daily = root.optJSONObject("daily");
            if (daily != null) {
                JSONArray maxA = daily.optJSONArray("temperature_2m_max");
                JSONArray minA = daily.optJSONArray("temperature_2m_min");
                if (maxA != null && maxA.length() > 0) d.high = (int) Math.round(maxA.getDouble(0));
                if (minA != null && minA.length() > 0) d.low = (int) Math.round(minA.getDouble(0));
            } else {
                d.high = d.temp;
                d.low = d.temp;
            }

            JSONObject hourly = root.optJSONObject("hourly");
            if (hourly != null) {
                JSONArray times = hourly.optJSONArray("time");
                JSONArray temps = hourly.optJSONArray("temperature_2m");
                JSONArray codes = hourly.optJSONArray("weather_code");
                JSONArray rains = hourly.optJSONArray("precipitation_probability");
                if (times != null && temps != null && codes != null) {
                    int start = times.length() > 1 ? 1 : 0; // skip current hour, show next 4
                    int n = Math.min(4, times.length() - start);
                    d.hourCount = Math.max(0, n);
                    for (int i = 0; i < d.hourCount; i++) {
                        int idx = start + i;
                        String iso = times.getString(idx); // e.g. 2026-11-23T19:00
                        int tPos = iso.indexOf('T');
                        d.hourLabels[i] = (tPos >= 0 && iso.length() >= tPos + 6)
                                ? iso.substring(tPos + 1, tPos + 6)
                                : iso;
                        d.hourTemps[i] = (int) Math.round(temps.getDouble(idx));
                        d.hourCodes[i] = codes.getInt(idx);
                        d.hourRain[i] = (rains != null && idx < rains.length()) ? rains.optInt(idx, 0) : 0;
                    }
                }
            }
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    public static String conditionLabel(int wmo) {
        switch (wmo) {
            case 0: return "Clear";
            case 1: return "Mainly clear";
            case 2: return "Partly cloudy";
            case 3: return "Overcast";
            case 45: case 48: return "Foggy";
            case 51: case 53: case 55: return "Drizzle";
            case 56: case 57: return "Freezing drizzle";
            case 61: case 63: return "Rain";
            case 65: return "Heavy rain";
            case 66: case 67: return "Freezing rain";
            case 71: case 73: case 75: case 77: return "Snow";
            case 80: case 81: case 82: return "Showers";
            case 85: case 86: return "Snow showers";
            case 95: return "Thunderstorm";
            case 96: case 99: return "Storm & hail";
            default: return "Overcast";
        }
    }

    public static int iconRes(int wmo, boolean isDay) {
        if (wmo == 0) return isDay ? R.drawable.ic_w_clear_day : R.drawable.ic_w_clear_night;
        if (wmo == 1 || wmo == 2) return isDay ? R.drawable.ic_w_partly_day : R.drawable.ic_w_partly_night;
        if (wmo == 3) return R.drawable.ic_w_cloudy;
        if (wmo == 45 || wmo == 48) return R.drawable.ic_w_fog;
        if ((wmo >= 51 && wmo <= 67) || (wmo >= 80 && wmo <= 82)) return R.drawable.ic_w_rain;
        if ((wmo >= 71 && wmo <= 77) || wmo == 85 || wmo == 86) return R.drawable.ic_w_snow;
        if (wmo >= 95) return R.drawable.ic_w_storm;
        return R.drawable.ic_w_cloudy;
    }
}
