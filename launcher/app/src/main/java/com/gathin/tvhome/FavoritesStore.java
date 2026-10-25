package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** User-curated Favorites tab, independent of the apps pinned to the Home shelf. */
final class FavoritesStore {
    private static final String KEY = "favorite_apps";

    private FavoritesStore() { }

    static boolean contains(Context context, String packageName) {
        return preferences(context).getStringSet(KEY, java.util.Collections.emptySet()).contains(packageName);
    }

    static boolean set(Context context, String packageName, boolean favorite) {
        if (!LibraryApps.validPackage(packageName)) return false;
        SharedPreferences preferences = preferences(context);
        Set<String> current = new HashSet<>(preferences.getStringSet(KEY, java.util.Collections.emptySet()));
        if (favorite) current.add(packageName); else current.remove(packageName);
        return preferences.edit().putStringSet(KEY, current).commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
    }
}
