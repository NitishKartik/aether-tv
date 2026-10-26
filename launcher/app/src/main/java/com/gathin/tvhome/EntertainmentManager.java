package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Entertainment Discovery Manager providing IMDb ratings, metadata caching,
 * Hindi and English curation, search, and local watchlist persistence.
 */
public final class EntertainmentManager {

    private static final String PREFS_NAME = "aether_entertainment_prefs";
    private static final String KEY_WATCHLIST = "user_watchlist_ids";

    private final SharedPreferences prefs;
    private final List<EntertainmentModel> catalog = new ArrayList<>();

    public EntertainmentManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadDefaultCatalog();
    }

    private void loadDefaultCatalog() {
        Set<String> watchlist = getWatchlistIds();

        catalog.add(new EntertainmentModel(
            "tt0816692", "Interstellar", 2014, 8.7, "Sci-Fi / Adventure", "2h 49m",
            "When Earth becomes uninhabitable in the future, a farmer and ex-NASA pilot is tasked to pilot a spacecraft to find a new planet for humanity.",
            "Matthew McConaughey, Anne Hathaway, Jessica Chastain", false, false, watchlist.contains("tt0816692"),
            "https://image.tmdb.org/t/p/w342/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg"
        ));

        catalog.add(new EntertainmentModel(
            "tt1375666", "Inception", 2010, 8.8, "Sci-Fi / Action", "2h 28m",
            "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.",
            "Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page", false, false, watchlist.contains("tt1375666"),
            "https://image.tmdb.org/t/p/w342/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg"
        ));

        catalog.add(new EntertainmentModel(
            "tt15398776", "Oppenheimer", 2023, 8.9, "Biography / Drama", "3h 00m",
            "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb during World War II.",
            "Cillian Murphy, Emily Blunt, Matt Damon, Robert Downey Jr.", false, false, watchlist.contains("tt15398776"),
            "https://image.tmdb.org/t/p/w342/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg"
        ));

        catalog.add(new EntertainmentModel(
            "tt0903747", "Breaking Bad", 2008, 9.5, "Crime / Drama / Thriller", "5 Seasons",
            "A chemistry teacher diagnosed with inoperable lung cancer turns to manufacturing methamphetamine with a former student to secure his family's future.",
            "Bryan Cranston, Aaron Paul, Anna Gunn", false, true, watchlist.contains("tt0903747"),
            "https://image.tmdb.org/t/p/w342/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg"
        ));

        catalog.add(new EntertainmentModel(
            "tt8758854", "Mirzapur", 2018, 8.5, "Action / Crime / Thriller", "3 Seasons",
            "A shocking incident at a wedding procession ignites a series of events entangling two families in the lawless city of Mirzapur.",
            "Pankaj Tripathi, Ali Fazal, Divyenndu, Shweta Tripathi", true, true, watchlist.contains("tt8758854"),
            "https://image.tmdb.org/t/p/w342/o87Yw678F1vK7qN0aB9p9c9x1cQ.jpg"
        ));

        catalog.add(new EntertainmentModel(
            "tt8178634", "RRR", 2022, 7.8, "Action / Drama", "3h 07m",
            "A fearless revolutionary and an officer in the British force decide to join forces in 1920s India against the tyrannical British rulers.",
            "N.T. Rama Rao Jr., Ram Charan, Ajay Devgn, Alia Bhatt", true, false, watchlist.contains("tt8178634"),
            "https://image.tmdb.org/t/p/w342/wE0noFU9ETqBM8vlge17Z9o5iS7.jpg"
        ));

        catalog.add(new EntertainmentModel(
            "tt5074352", "Dangal", 2016, 8.3, "Action / Biography / Drama", "2h 41m",
            "Former wrestler Mahavir Singh Phogat trains his daughters Geeta and Babita to become world-class wrestlers against all social stigmas.",
            "Aamir Khan, Sakshi Tanwar, Fatima Sana Shaikh", true, false, watchlist.contains("tt5074352"),
            "https://image.tmdb.org/t/p/w342/l1z4PZ1f57H2wWb3d8Qk3J1F0v2.jpg"
        ));

        catalog.add(new EntertainmentModel(
            "tt11280740", "Severance", 2022, 8.7, "Sci-Fi / Thriller", "2 Seasons",
            "Mark leads a team of office workers whose memories have been surgically divided between their work and personal lives.",
            "Adam Scott, Zach Cherry, Britt Lower, Patricia Arquette", false, true, watchlist.contains("tt11280740"),
            "https://image.tmdb.org/t/p/w342/1zhd7v16tW4H5a2n3w1p4x5o2yA.jpg"
        ));

        catalog.add(new EntertainmentModel(
            "tt5753856", "Dark", 2017, 8.7, "Crime / Drama / Mystery", "3 Seasons",
            "A family saga with a supernatural twist, set in a German town where the disappearance of two young children exposes the relationships among four families.",
            "Louis Hofmann, Karoline Eichhorn, Lisa Vicari", false, true, watchlist.contains("tt5753856"),
            "https://image.tmdb.org/t/p/w342/apbrVkgrrL4DlhW3t9bJv16oD2F.jpg"
        ));

        catalog.add(new EntertainmentModel(
            "tt0468569", "The Dark Knight", 2008, 9.0, "Action / Crime / Drama", "2h 32m",
            "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological tests of his ability to fight injustice.",
            "Christian Bale, Heath Ledger, Aaron Eckhart, Michael Caine", false, false, watchlist.contains("tt0468569"),
            "https://image.tmdb.org/t/p/w342/qJ2tW6WMUDux911r6m7haRef0WH.jpg"
        ));

        catalog.add(new EntertainmentModel(
            "tt15327088", "Kantara", 2022, 8.2, "Action / Adventure / Drama", "2h 28m",
            "When greed paves the way for betrayal and murder, a young tribal champion invokes his ancestors' spirit to protect his community's sacred land.",
            "Rishab Shetty, Kishore Kumar G., Achyuth Kumar", true, false, watchlist.contains("tt15327088"),
            "https://image.tmdb.org/t/p/w342/1z7a3s3B329qF2245e317c88081.jpg"
        ));

        catalog.add(new EntertainmentModel(
            "tt4574334", "Stranger Things", 2016, 8.7, "Drama / Fantasy / Horror", "4 Seasons",
            "When a young boy vanishes, a small town uncovers a mystery involving secret experiments, terrifying supernatural forces and one strange little girl.",
            "Millie Bobby Brown, Finn Wolfhard, Winona Ryder, David Harbour", false, true, watchlist.contains("tt4574334"),
            "https://image.tmdb.org/t/p/w342/49WJfeN0moxb9IPfGn8AIqMGskD.jpg"
        ));
    }

    public Set<String> getWatchlistIds() {
        return new HashSet<>(prefs.getStringSet(KEY_WATCHLIST, new HashSet<>()));
    }

    public void toggleWatchlist(String id) {
        Set<String> current = getWatchlistIds();
        boolean nowInWatchlist = !current.contains(id);
        if (nowInWatchlist) {
            current.add(id);
        } else {
            current.remove(id);
        }
        prefs.edit().putStringSet(KEY_WATCHLIST, current).apply();

        for (EntertainmentModel item : catalog) {
            if (item.id.equals(id)) {
                item.isWatchlist = nowInWatchlist;
                break;
            }
        }
    }

    public List<EntertainmentModel> getByCategory(String category) {
        if ("ALL".equalsIgnoreCase(category)) {
            return new ArrayList<>(catalog);
        }
        List<EntertainmentModel> filtered = new ArrayList<>();
        for (EntertainmentModel item : catalog) {
            if ("MOVIES".equalsIgnoreCase(category) && !item.isTvShow) {
                filtered.add(item);
            } else if ("TV_SHOWS".equalsIgnoreCase(category) && item.isTvShow) {
                filtered.add(item);
            } else if ("HINDI".equalsIgnoreCase(category) && item.isHindi) {
                filtered.add(item);
            } else if ("TOP_IMDB".equalsIgnoreCase(category) && item.imdbRating >= 8.5) {
                filtered.add(item);
            } else if ("WATCHLIST".equalsIgnoreCase(category) && item.isWatchlist) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public List<EntertainmentModel> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getByCategory("ALL");
        }
        String q = query.toLowerCase(Locale.getDefault()).trim();
        List<EntertainmentModel> results = new ArrayList<>();
        for (EntertainmentModel item : catalog) {
            if (item.title.toLowerCase(Locale.getDefault()).contains(q)
                || item.genre.toLowerCase(Locale.getDefault()).contains(q)
                || item.cast.toLowerCase(Locale.getDefault()).contains(q)) {
                results.add(item);
            }
        }
        return results;
    }
}
