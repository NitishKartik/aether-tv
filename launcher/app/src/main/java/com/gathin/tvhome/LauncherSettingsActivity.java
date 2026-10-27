package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aether TV Launcher Settings Activity.
 * Supports Privacy & Security, D-Pad Remote Customization, Screensaver, and Grid Layout.
 */
public final class LauncherSettingsActivity extends Activity {

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));
    }

    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    static final String EXTRA_LOCK_NOW = "com.gathin.tvhome.extra.LOCK_NOW";

    private static final int[] SCREENSAVER_MINUTES = {0, 1, 2, 5, 10, 15, 30, 60};
    private static final String[] SCREENSAVER_LABELS = {
            "Never / Off", "1 Minute", "2 Minutes", "5 Minutes (Default)",
            "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour"
    };

    private SharedPreferences choices;
    private LockManager lockManager;
    private KeyRemapManager remapManager;

    private final TextView[] columnChoices = new TextView[4];
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];

    private TextView txtPrivacyLockBadge;
    private TextView txtPrivacyTimeoutBadge;
    private TextView txtDpadStatusBadge;
    private TextView txtScreensaverTimeout;

    private TextView wallpaperChoice;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private TextView openOnStartChoice;

    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private boolean animations;
    private boolean advancedEffects;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        lockManager = new LockManager(this);
        remapManager = KeyRemapManager.getInstance(this);

        Config config = Config.load(this);

        View btnBack = findViewById(R.id.btnSettingsBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
            attachFocusAnimation(btnBack);
        }

        // Section 1: Privacy & Security
        View privacyChoice = findViewById(R.id.privacySecurityChoice);
        txtPrivacyLockBadge = findViewById(R.id.txtPrivacyLockBadge);
        txtPrivacyTimeoutBadge = findViewById(R.id.txtPrivacyTimeoutBadge);
        if (privacyChoice != null) {
            privacyChoice.setOnClickListener(v -> showPrivacySecurityDialog());
            attachFocusAnimation(privacyChoice);
        }

        // Section 2: D-Pad & Remote Customization
        View dpadChoice = findViewById(R.id.dpadCustomizationChoice);
        txtDpadStatusBadge = findViewById(R.id.txtDpadStatusBadge);
        if (dpadChoice != null) {
            dpadChoice.setOnClickListener(v -> showDpadCustomizationDialog());
            attachFocusAnimation(dpadChoice);
        }

        // Section 3: Screensaver Timeout
        View screensaverChoice = findViewById(R.id.screensaverChoice);
        txtScreensaverTimeout = findViewById(R.id.txtScreensaverTimeout);
        if (screensaverChoice != null) {
            screensaverChoice.setOnClickListener(v -> showScreensaverPicker(config));
            attachFocusAnimation(screensaverChoice);
        }

        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        attachFocusAnimation(reorderChoice);

        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
            attachFocusAnimation(columnChoices[i]);
        }
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
            attachFocusAnimation(rowChoices[i]);
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
            attachFocusAnimation(dockChoices[i]);
        }

        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
        }

        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());
        attachFocusAnimation(wallpaperChoice);

        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        attachFocusAnimation(animationsChoice);

        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        attachFocusAnimation(advancedChoice);

        openOnStartChoice = findViewById(R.id.openOnStartChoice);
        if (config.openOnStartChoice) {
            openOnStartChoice.setVisibility(View.VISIBLE);
            openOnStartChoice.setOnClickListener(v -> showOpenOnStartPicker());
            attachFocusAnimation(openOnStartChoice);
        }

        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                    .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY)
                    .remove(Config.DOCK_OPACITY_KEY).remove(OpenOnStart.KEY)
                    .remove("screensaver_idle_minutes").commit();
            Config current = Config.load(this);
            columns = current.columns;
            rowsBelowDock = current.rowsBelowDock;
            dockOpacity = current.dockOpacity;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            changed();
            refresh();
        });
        attachFocusAnimation(followThemeChoice);

        View defaultHome = findViewById(R.id.defaultHomeChoice);
        defaultHome.setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        attachFocusAnimation(defaultHome);

        View tvSettings = findViewById(R.id.tvSettingsChoice);
        tvSettings.setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        attachFocusAnimation(tvSettings);

        refresh();

        if (privacyChoice != null) privacyChoice.requestFocus();
        else if (config.launcherSettingsButton) reorderChoice.requestFocus();
        else columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    private void attachFocusAnimation(View v) {
        v.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                view.setScaleX(1.03f);
                view.setScaleY(1.03f);
                SoundManager.getInstance(this).playKeyTap();
            } else {
                view.setScaleX(1.0f);
                view.setScaleY(1.0f);
            }
        });
    }

    private void showPrivacySecurityDialog() {
        SoundManager.getInstance(this).playDialogOpen();
        PrivacySecurityDialog dialog = new PrivacySecurityDialog(this, lockManager, new PrivacySecurityDialog.PrivacySecurityListener() {
            @Override
            public void onLockNowRequested() {
                setResult(RESULT_OK, new Intent().putExtra(EXTRA_LOCK_NOW, true));
                finish();
            }

            @Override
            public void onSecuritySettingsChanged() {
                refresh();
                changed();
            }
        });
        dialog.show();
    }

    private void showDpadCustomizationDialog() {
        SoundManager.getInstance(this).playDialogOpen();
        DPadCustomizationDialog dialog = new DPadCustomizationDialog(this);
        dialog.setOnDismissListener(d -> refresh());
        dialog.show();
    }

    private void showScreensaverPicker(Config config) {
        int currentMinutes = choices.getInt("screensaver_idle_minutes", config.homeIdleClockMinutes);
        int checked = 3; // 5 min default
        for (int i = 0; i < SCREENSAVER_MINUTES.length; i++) {
            if (SCREENSAVER_MINUTES[i] == currentMinutes) {
                checked = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Screensaver Idle Timeout")
                .setSingleChoiceItems(SCREENSAVER_LABELS, checked, (dialog, which) -> {
                    dialog.dismiss();
                    int chosen = SCREENSAVER_MINUTES[which];
                    choices.edit().putInt("screensaver_idle_minutes", chosen).apply();
                    changed();
                    SoundManager.getInstance(this).playSelect();
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new java.util.ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 1];
        for (int i = 0; i < images.size(); i++) labels[i] = images.get(i).getName();
        labels[images.size()] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = "none".equals(current) ? images.size()
                : indexOf(images, current);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == images.size()) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                    } else {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,
                                images.get(which).getName()).apply();
                    }
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (dialog, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void refresh() {
        // Privacy & Security badges
        boolean lockEnabled = lockManager.isLockEnabled();
        if (txtPrivacyLockBadge != null) {
            txtPrivacyLockBadge.setText(lockEnabled ? "PROTECTED" : "DISABLED");
            txtPrivacyLockBadge.setTextColor(lockEnabled ? Color.parseColor("#FF00E676") : Color.parseColor("#FFFF5252"));
        }
        int autoLockMinutes = lockManager.getAutoRelockMinutes();
        if (txtPrivacyTimeoutBadge != null) {
            txtPrivacyTimeoutBadge.setText(autoLockMinutes > 0 ? (autoLockMinutes + " MIN") : "OFF");
        }

        // D-Pad Customization badge
        if (txtDpadStatusBadge != null) {
            boolean hasCustom = remapManager.hasAnyCustomMappings();
            txtDpadStatusBadge.setText(hasCustom ? "CUSTOMIZED" : "CUSTOMIZE ➔");
            txtDpadStatusBadge.setTextColor(hasCustom ? Color.parseColor("#FF00E676") : Color.parseColor("#FF00E5FF"));
        }

        // Screensaver timeout
        int screensaverMinutes = choices.getInt("screensaver_idle_minutes", 5);
        if (txtScreensaverTimeout != null) {
            txtScreensaverTimeout.setText(screensaverMinutes > 0 ? (screensaverMinutes + " MINUTES") : "OFF");
        }

        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        wallpaperChoice.setText(getString(R.string.wallpaper_state,
                "none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                        : wallpaperCurrent != null ? wallpaperCurrent
                        : getString(R.string.wallpaper_theme_default)));
        openOnStartChoice.setText(getString(R.string.open_on_start_state, openOnStartLabel()));
        followThemeChoice.setSelected(!hasOnTvChoices());
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY)
                || choices.contains("animations") || choices.contains("advanced_effects")
                || choices.contains(Config.WALLPAPER_CHOICE_KEY) || choices.contains(Config.DOCK_OPACITY_KEY)
                || choices.contains(OpenOnStart.KEY) || choices.contains("screensaver_idle_minutes");
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
