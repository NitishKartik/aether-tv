package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small, cached current-weather client. It uses a manually chosen city and never requests GPS. */
final class WeatherManager {
    interface Callback { void onWeather(Weather weather); }

    static final class Weather {
        final String city;
        final String temperature;
        final int code;
        Weather(String city, String temperature, int code) {
            this.city = city;
            this.temperature = temperature;
            this.code = code;
        }
    }

    static final String CITY_KEY = "weather_city";
    static final String FAHRENHEIT_KEY = "weather_fahrenheit";
    private static final String CACHE_KEY = "weather_cache";
    private static final String CACHE_CITY_KEY = "weather_cache_city";
    private static final String CACHE_UNIT_KEY = "weather_cache_unit";
    private static final String CACHE_TIME_KEY = "weather_cache_time";
    private static final long CACHE_MS = 30L * 60L * 1000L;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final SharedPreferences prefs;

    WeatherManager(Context context) {
        prefs = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
    }

    void load(Callback callback) {
        String city = prefs.getString(CITY_KEY, "").trim();
        boolean fahrenheit = prefs.getBoolean(FAHRENHEIT_KEY, false);
        if (city.isEmpty()) { callback.onWeather(null); return; }

        long cachedAt = prefs.getLong(CACHE_TIME_KEY, 0L);
        String cacheCity = prefs.getString(CACHE_CITY_KEY, "");
        boolean cacheUnit = prefs.getBoolean(CACHE_UNIT_KEY, false);
        String rawCache = prefs.getString(CACHE_KEY, null);
        Weather cachedValue = null;
        if (rawCache != null && city.equalsIgnoreCase(cacheCity) && fahrenheit == cacheUnit) {
            try {
                JSONObject cached = new JSONObject(rawCache);
                cachedValue = fromJson(cached);
                callback.onWeather(cachedValue);
                if (System.currentTimeMillis() - cachedAt < CACHE_MS) return;
            } catch (Exception ignored) { }
        }
        final Weather staleValue = cachedValue;
        EXECUTOR.execute(() -> {
            Weather result = fetch(city, fahrenheit);
            if (result != null) {
                try {
                    JSONObject value = new JSONObject();
                    value.put("city", result.city);
                    value.put("temperature", result.temperature);
                    value.put("code", result.code);
                    prefs.edit().putString(CACHE_KEY, value.toString())
                            .putString(CACHE_CITY_KEY, city).putBoolean(CACHE_UNIT_KEY, fahrenheit)
                            .putLong(CACHE_TIME_KEY, System.currentTimeMillis()).apply();
                } catch (Exception ignored) { }
            }
            android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
            if (result != null || staleValue == null) main.post(() -> callback.onWeather(result));
        });
    }

    private Weather fetch(String city, boolean fahrenheit) {
        try {
            JSONObject geocode = geocode(city);
            JSONArray results = geocode.optJSONArray("results");
            if (results == null || results.length() == 0) return null;
            JSONObject place = results.getJSONObject(0);
            double lat = place.getDouble("latitude"), lon = place.getDouble("longitude");
            String displayCity = place.optString("name", city);
            String region = place.optString("admin1", "");
            if (!region.isEmpty() && !region.equalsIgnoreCase(displayCity)) displayCity += ", " + region;
            String unit = fahrenheit ? "fahrenheit" : "celsius";
            JSONObject forecast = request(String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f&current=temperature_2m,weather_code&temperature_unit=%s&timezone=auto",
                    lat, lon, unit));
            JSONObject current = forecast.optJSONObject("current");
            if (current == null || !current.has("temperature_2m")) return null;
            String suffix = fahrenheit ? "°F" : "°C";
            String temp = Math.round(current.getDouble("temperature_2m")) + suffix;
            return new Weather(displayCity, temp, current.optInt("weather_code", -1));
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Accept a city alone or a fuller "city, region, country" entry from TV Settings. */
    private static JSONObject geocode(String location) throws Exception {
        List<String> candidates = new ArrayList<>();
        String trimmed = location.trim();
        if (trimmed.contains(",") || trimmed.contains(";")) {
            String city = trimmed.split("[,;]", 2)[0].trim();
            if (!city.isEmpty()) candidates.add(city);
        } else {
            candidates.add(trimmed);
            String[] words = trimmed.split("\\s+");
            for (int count = words.length - 1; count > 0; count--) {
                StringBuilder prefix = new StringBuilder();
                for (int i = 0; i < count; i++) {
                    if (prefix.length() > 0) prefix.append(' ');
                    prefix.append(words[i]);
                }
                candidates.add(prefix.toString());
            }
        }

        String countryCode = countryCodeFor(trimmed);
        for (String candidate : candidates) {
            String query = URLEncoder.encode(candidate, StandardCharsets.UTF_8.name());
            String suffix = countryCode.isEmpty() ? "" : "&countryCode=" + countryCode;
            JSONObject response = request("https://geocoding-api.open-meteo.com/v1/search?name="
                    + query + "&count=1&language=en&format=json" + suffix);
            JSONArray results = response.optJSONArray("results");
            if (results != null && results.length() > 0) return response;
        }
        return new JSONObject();
    }

    private static String countryCodeFor(String location) {
        String[] parts = location.trim().split("[,;\\s]+");
        if (parts.length == 0) return "";
        String suffix = parts[parts.length - 1].trim();
        if ("USA".equalsIgnoreCase(suffix)) return "US";
        if ("UK".equalsIgnoreCase(suffix)) return "GB";
        if ("UAE".equalsIgnoreCase(suffix)) return "AE";
        if (suffix.length() == 2) {
            String code = suffix.toUpperCase(Locale.ROOT);
            for (String known : Locale.getISOCountries()) if (known.equals(code)) return code;
        }
        for (String code : Locale.getISOCountries()) {
            String name = new Locale("", code).getDisplayCountry(Locale.ENGLISH);
            if (name.equalsIgnoreCase(suffix)) return code;
        }
        return "";
    }

    private static JSONObject request(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "CleanYourTV/1.0 AndroidTV");
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300)
                throw new java.io.IOException("Weather service unavailable");
            try (InputStream input = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
                return new JSONObject(body.toString());
            }
        } finally { connection.disconnect(); }
    }

    private static Weather fromJson(JSONObject value) {
        return new Weather(value.optString("city"), value.optString("temperature"), value.optInt("code", -1));
    }

    static String iconForCode(int code) {
        if (code == 0) return "☀";
        if (code == 1 || code == 2) return "⛅";
        if (code == 3) return "☁";
        if (code == 45 || code == 48) return "〰";
        if (code >= 71 && code <= 77) return "❄";
        if (code >= 95) return "⚡";
        if (code >= 51) return "☂";
        return "☁";
    }
}
