package com.gathin.tvhome;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks and manages running and recently opened applications on Android TV.
 */
public class RecentAppsManager {
    private static final String PREFS_NAME = "recents_prefs";
    private static final String KEY_LAUNCH_HISTORY = "launch_history";
    private static final String KEY_LAST_CLEARED = "last_cleared_timestamp";
    private static final String KEY_DISMISSED = "dismissed_apps";
    private static final int MAX_RECENTS = 20;

    public static class RecentApp {
        public final String packageName;
        public final String label;
        public final Drawable icon;
        public final Intent launchIntent;
        public boolean isRunning;

        public RecentApp(String packageName, String label, Drawable icon, Intent launchIntent, boolean isRunning) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.launchIntent = launchIntent;
            this.isRunning = isRunning;
        }
    }

    /**
     * Records an app launch in MRU order.
     */
    public static void recordLaunch(Context ctx, String packageName) {
        if (packageName == null || packageName.equals(ctx.getPackageName())) return;
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Set<String> dismissed = new HashSet<>(prefs.getStringSet(KEY_DISMISSED, new HashSet<>()));
            dismissed.remove(packageName);

            String raw = prefs.getString(KEY_LAUNCH_HISTORY, "");
            String[] existing = raw.split(",");
            LinkedHashSet<String> set = new LinkedHashSet<>();
            set.add(packageName);
            for (String s : existing) {
                s = s.trim();
                if (!s.isEmpty() && !s.equals(packageName)) {
                    set.add(s);
                }
                if (set.size() >= MAX_RECENTS) break;
            }
            StringBuilder sb = new StringBuilder();
            for (String pkg : set) {
                if (sb.length() > 0) sb.append(",");
                sb.append(pkg);
            }
            prefs.edit()
                    .putString(KEY_LAUNCH_HISTORY, sb.toString())
                    .putStringSet(KEY_DISMISSED, dismissed)
                    .apply();
        } catch (Exception ignored) {}
    }

    /**
     * Retrieves recent and active background apps.
     */
    public static List<RecentApp> getRecentApps(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        LinkedHashSet<String> candidatePackages = new LinkedHashSet<>();
        Set<String> runningPackages = new HashSet<>();

        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastCleared = prefs.getLong(KEY_LAST_CLEARED, 0L);
        Set<String> dismissed = prefs.getStringSet(KEY_DISMISSED, Collections.emptySet());

        // 1. Check running app processes
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
                if (procs != null) {
                    for (ActivityManager.RunningAppProcessInfo p : procs) {
                        if (p.pkgList != null) {
                            for (String pkg : p.pkgList) {
                                if (!pkg.equals(ctx.getPackageName()) && !pkg.equals("android") && !pkg.contains("systemui") && !dismissed.contains(pkg)) {
                                    runningPackages.add(pkg);
                                    candidatePackages.add(pkg);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // 2. Query UsageStatsManager if permitted
        try {
            UsageStatsManager usm = (UsageStatsManager) ctx.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm != null) {
                long now = System.currentTimeMillis();
                List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, Math.max(lastCleared, now - 86400000L * 3), now);
                if (stats != null && !stats.isEmpty()) {
                    List<UsageStats> sorted = new ArrayList<>(stats);
                    Collections.sort(sorted, (a, b) -> Long.compare(b.getLastTimeUsed(), a.getLastTimeUsed()));
                    for (UsageStats stat : sorted) {
                        String pkg = stat.getPackageName();
                        if (stat.getLastTimeUsed() > lastCleared && !pkg.equals(ctx.getPackageName()) && !pkg.equals("android") && !pkg.contains("systemui") && !dismissed.contains(pkg)) {
                            candidatePackages.add(pkg);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // 3. Fallback / supplement with saved MRU launch history
        try {
            String raw = prefs.getString(KEY_LAUNCH_HISTORY, "");
            for (String s : raw.split(",")) {
                s = s.trim();
                if (!s.isEmpty() && !s.equals(ctx.getPackageName()) && !dismissed.contains(s)) {
                    candidatePackages.add(s);
                }
            }
        } catch (Exception ignored) {}

        // 4. Resolve candidate packages to displayable apps
        List<RecentApp> result = new ArrayList<>();
        for (String pkg : candidatePackages) {
            try {
                Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
                if (intent == null) {
                    intent = pm.getLaunchIntentForPackage(pkg);
                }
                if (intent == null) continue;

                ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                CharSequence label = pm.getApplicationLabel(appInfo);
                Drawable icon = pm.getApplicationIcon(appInfo);
                boolean isRunning = runningPackages.contains(pkg);

                result.add(new RecentApp(pkg, label != null ? label.toString() : pkg, icon, intent, isRunning));
                if (result.size() >= MAX_RECENTS) break;
            } catch (Exception ignored) {}
        }

        return result;
    }

    /**
     * Force-cleans / terminates a background app process and dismisses it from recents.
     */
    public static void killApp(Context ctx, String packageName) {
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(packageName);
            }
        } catch (Exception ignored) {}

        try {
            Runtime.getRuntime().exec(new String[]{"am", "force-stop", packageName});
        } catch (Exception ignored) {}

        // Add to dismissed set
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Set<String> dismissed = new HashSet<>(prefs.getStringSet(KEY_DISMISSED, new HashSet<>()));
            dismissed.add(packageName);

            String raw = prefs.getString(KEY_LAUNCH_HISTORY, "");
            StringBuilder sb = new StringBuilder();
            for (String s : raw.split(",")) {
                s = s.trim();
                if (!s.isEmpty() && !s.equals(packageName)) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(s);
                }
            }
            prefs.edit()
                    .putString(KEY_LAUNCH_HISTORY, sb.toString())
                    .putStringSet(KEY_DISMISSED, dismissed)
                    .apply();
        } catch (Exception ignored) {}
    }

    /**
     * Cleans all active background apps and clears recents completely.
     */
    public static int killAll(Context ctx, List<RecentApp> list) {
        int count = 0;
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (RecentApp app : list) {
            if (app.packageName.equals(ctx.getPackageName())) continue;
            try {
                if (am != null) am.killBackgroundProcesses(app.packageName);
                Runtime.getRuntime().exec(new String[]{"am", "force-stop", app.packageName});
                count++;
            } catch (Exception ignored) {}
        }
        try {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_LAST_CLEARED, System.currentTimeMillis())
                    .putString(KEY_LAUNCH_HISTORY, "")
                    .putStringSet(KEY_DISMISSED, Collections.emptySet())
                    .apply();
        } catch (Exception ignored) {}
        return count;
    }
}
