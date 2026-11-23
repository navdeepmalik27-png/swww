package com.overcast.mini;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/**
 * Minimal, gzip-enabled HTTP client for the free Open-Meteo Forecast & Geocoding APIs.
 * No API key required. Payload is ~600-800 bytes compressed to minimize cellular radio wake time.
 */
public final class OpenMeteoClient {
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;

    private OpenMeteoClient() {}

    public static final class GeoResult {
        public final String label;
        public final double lat;
        public final double lon;

        public GeoResult(String label, double lat, double lon) {
            this.label = label;
            this.lat = lat;
            this.lon = lon;
        }
    }

    public static String fetchForecastJson(double lat, double lon, boolean fahrenheit) {
        String unit = fahrenheit ? "&temperature_unit=fahrenheit" : "";
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast"
                        + "?latitude=%.4f&longitude=%.4f"
                        + "&current=temperature_2m,apparent_temperature,weather_code,is_day,precipitation_probability"
                        + "&hourly=temperature_2m,weather_code,precipitation_probability"
                        + "&daily=temperature_2m_max,temperature_2m_min"
                        + "&forecast_days=1&forecast_hours=5&timezone=auto%s",
                lat, lon, unit);
        return httpGet(url);
    }

    public static GeoResult searchCity(String query) {
        if (query == null || query.trim().isEmpty()) return null;
        String url = "https://geocoding-api.open-meteo.com/v1/search?name="
                + Uri.encode(query.trim()) + "&count=1&language=en&format=json";
        String body = httpGet(url);
        if (body == null) return null;
        try {
            JSONObject root = new JSONObject(body);
            JSONArray res = root.optJSONArray("results");
            if (res == null || res.length() == 0) return null;
            JSONObject first = res.getJSONObject(0);
            String name = first.getString("name");
            String cc = first.optString("country_code", "");
            String label = cc.isEmpty() ? name : (name + ", " + cc);
            return new GeoResult(label, first.getDouble("latitude"), first.getDouble("longitude"));
        } catch (Exception e) {
            return null;
        }
    }

    private static String httpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setUseCaches(false);
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setRequestProperty("User-Agent", "OvercastMini/1.0");
            if (conn.getResponseCode() != 200) return null;

            InputStream in = conn.getInputStream();
            if ("gzip".equalsIgnoreCase(conn.getContentEncoding())) {
                in = new GZIPInputStream(in);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            byte[] buf = new byte[1024];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            in.close();
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
