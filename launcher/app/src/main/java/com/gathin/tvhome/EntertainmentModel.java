package com.gathin.tvhome;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Entertainment metadata item representing a movie or TV show.
 */
public final class EntertainmentModel {

    public final String id;
    public final String title;
    public final int year;
    public final double imdbRating;
    public final String genre;
    public final String duration;
    public final String plot;
    public final String cast;
    public final boolean isHindi;
    public final boolean isTvShow;
    public boolean isWatchlist;
    public final String posterUrl;

    public EntertainmentModel(String id, String title, int year, double imdbRating,
                              String genre, String duration, String plot, String cast,
                              boolean isHindi, boolean isTvShow, boolean isWatchlist) {
        this(id, title, year, imdbRating, genre, duration, plot, cast, isHindi, isTvShow, isWatchlist, "");
    }

    public EntertainmentModel(String id, String title, int year, double imdbRating,
                              String genre, String duration, String plot, String cast,
                              boolean isHindi, boolean isTvShow, boolean isWatchlist,
                              String posterUrl) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.imdbRating = imdbRating;
        this.genre = genre;
        this.duration = duration;
        this.plot = plot;
        this.cast = cast;
        this.isHindi = isHindi;
        this.isTvShow = isTvShow;
        this.isWatchlist = isWatchlist;
        this.posterUrl = posterUrl != null ? posterUrl : "";
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("title", title);
        obj.put("year", year);
        obj.put("imdbRating", imdbRating);
        obj.put("genre", genre);
        obj.put("duration", duration);
        obj.put("plot", plot);
        obj.put("cast", cast);
        obj.put("isHindi", isHindi);
        obj.put("isTvShow", isTvShow);
        obj.put("isWatchlist", isWatchlist);
        obj.put("posterUrl", posterUrl);
        return obj;
    }

    public static EntertainmentModel fromJson(JSONObject obj) {
        return new EntertainmentModel(
            obj.optString("id", ""),
            obj.optString("title", "Unknown"),
            obj.optInt("year", 2024),
            obj.optDouble("imdbRating", 8.0),
            obj.optString("genre", "Drama"),
            obj.optString("duration", "2h"),
            obj.optString("plot", ""),
            obj.optString("cast", ""),
            obj.optBoolean("isHindi", false),
            obj.optBoolean("isTvShow", false),
            obj.optBoolean("isWatchlist", false),
            obj.optString("posterUrl", "")
        );
    }
}
