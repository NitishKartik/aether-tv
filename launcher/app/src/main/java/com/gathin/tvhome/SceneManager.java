package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import java.util.Calendar;

/**
 * Aether TV Dynamic Day/Night Scene Manager.
 * Orchestrates cinematic visual profiles matching ambient time of day:
 * - MORNING (06:00 - 11:59): Radiant celestial blue and golden dawn glass
 * - DAY (12:00 - 17:59): Crisp arctic teal and high-contrast glass
 * - EVENING (18:00 - 21:59): Warm sunset amber and twilight violet glow
 * - NIGHT (22:00 - 05:59): Deep OLED midnight with subtle neon cyan accents
 */
public final class SceneManager {

    public enum Scene {
        MORNING("Morning Glow", "Celestial dawn with radiant glass accents"),
        DAY("Midday Clarity", "High-contrast clean glass for bright viewing"),
        EVENING("Sunset Cyber", "Warm amber and violet twilight ambiance"),
        NIGHT("Midnight OLED", "Deep black surfaces with soft neon edge lighting");

        private final String displayName;
        private final String description;

        Scene(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum Mode {
        AUTO,
        MANUAL_MORNING,
        MANUAL_DAY,
        MANUAL_EVENING,
        MANUAL_NIGHT
    }

    private static final String PREFS_NAME = "aether_scene_settings";
    private static final String KEY_MODE = "scene_mode";
    private static final String KEY_USER_NAME = "profile_user_name";

    private final SharedPreferences prefs;

    public SceneManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public Mode getMode() {
        String modeName = prefs.getString(KEY_MODE, Mode.AUTO.name());
        try {
            return Mode.valueOf(modeName);
        } catch (IllegalArgumentException e) {
            return Mode.AUTO;
        }
    }

    public void setMode(Mode mode) {
        prefs.edit().putString(KEY_MODE, mode.name()).apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "NITISH");
    }

    public void setUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    /**
     * Resolves the current active scene according to schedule or manual override.
     */
    public Scene getCurrentScene() {
        Mode mode = getMode();
        switch (mode) {
            case MANUAL_MORNING:
                return Scene.MORNING;
            case MANUAL_DAY:
                return Scene.DAY;
            case MANUAL_EVENING:
                return Scene.EVENING;
            case MANUAL_NIGHT:
                return Scene.NIGHT;
            case AUTO:
            default:
                return getAutoScene(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        }
    }

    public static Scene getAutoScene(int hourOfDay) {
        if (hourOfDay >= 6 && hourOfDay < 12) {
            return Scene.MORNING;
        } else if (hourOfDay >= 12 && hourOfDay < 18) {
            return Scene.DAY;
        } else if (hourOfDay >= 18 && hourOfDay < 22) {
            return Scene.EVENING;
        } else {
            return Scene.NIGHT;
        }
    }

    public int getPrimaryAccentColor(Scene scene) {
        switch (scene) {
            case MORNING:
                return Color.parseColor("#00E5FF"); // Cyan Dawn
            case DAY:
                return Color.parseColor("#00B0FF"); // Arctic Sky
            case EVENING:
                return Color.parseColor("#FF9100"); // Twilight Amber
            case NIGHT:
            default:
                return Color.parseColor("#7C4DFF"); // Neon Purple / Midnight
        }
    }

    public int getSecondaryAccentColor(Scene scene) {
        switch (scene) {
            case MORNING:
                return Color.parseColor("#FFD700"); // Warm Sun Gold
            case DAY:
                return Color.parseColor("#00E676"); // Vibrant Mint
            case EVENING:
                return Color.parseColor("#E040FB"); // Magenta / Violet
            case NIGHT:
            default:
                return Color.parseColor("#00E5FF"); // Electric Cyan
        }
    }

    public int getAmbientGlowColor(Scene scene) {
        switch (scene) {
            case MORNING:
                return Color.argb(40, 0, 229, 255);
            case DAY:
                return Color.argb(30, 0, 176, 255);
            case EVENING:
                return Color.argb(45, 255, 145, 0);
            case NIGHT:
            default:
                return Color.argb(35, 124, 77, 255);
        }
    }

    public String getGreetingText() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String name = getUserName();
        if (hour >= 5 && hour < 12) {
            return "GOOD MORNING // " + name;
        } else if (hour >= 12 && hour < 17) {
            return "GOOD AFTERNOON // " + name;
        } else if (hour >= 17 && hour < 22) {
            return "GOOD EVENING // " + name;
        } else {
            return "GOOD NIGHT // " + name;
        }
    }
}
