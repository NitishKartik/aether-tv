package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Aether TV Remote Key & D-Pad Customization Engine.
 * Supports configurable single, double, triple clicks, and long-press actions
 * with customizable hold thresholds (0.5s to 3.0s) for remote buttons.
 */
public final class KeyRemapManager {

    public static final String PREFS_NAME = "aether_remote_key_customizer";

    // Action Constants
    public static final String ACTION_NONE = "NONE";
    public static final String ACTION_LOCK_SCREEN = "LOCK_SCREEN";
    public static final String ACTION_SCREENSAVER = "SCREENSAVER";
    public static final String ACTION_APP_LIBRARY = "APP_LIBRARY";
    public static final String ACTION_GAME_ARENA = "GAME_ARENA";
    public static final String ACTION_LAUNCHER_SETTINGS = "LAUNCHER_SETTINGS";
    public static final String ACTION_TV_SETTINGS = "TV_SETTINGS";
    public static final String ACTION_MUTE_TOGGLE = "MUTE_TOGGLE";
    public static final String ACTION_OPEN_APP = "OPEN_APP";

    public interface ActionCallback {
        void onExecute(String action, String appPackage);
    }

    public static final class KeyDef {
        public final int keyCode;
        public final String id;
        public final String name;
        public final String icon;

        public KeyDef(int keyCode, String id, String name, String icon) {
            this.keyCode = keyCode;
            this.id = id;
            this.name = name;
            this.icon = icon;
        }
    }

    private static volatile KeyRemapManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<KeyDef> supportedKeys = new ArrayList<>();

    // Multi-click & hold state
    private int activeKeyCode = -1;
    private int clickCount = 0;
    private boolean longPressTriggered = false;
    private Runnable longPressRunnable;
    private Runnable multiClickRunnable;

    public static KeyRemapManager getInstance(Context context) {
        if (instance == null) {
            synchronized (KeyRemapManager.class) {
                if (instance == null) {
                    instance = new KeyRemapManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private KeyRemapManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        registerSupportedKeys();
    }

    private void registerSupportedKeys() {
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_DPAD_CENTER, "dpad_center", "D-Pad Center / OK", "🔘"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_BACK, "back", "Back Button", "🔙"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_MENU, "menu", "Menu Button", "📋"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_ALL_APPS, "all_apps", "App / 9-Square Button", "⊞"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_VOICE_ASSIST, "voice_assist", "Voice / Mic Button", "🎤"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_DPAD_UP, "dpad_up", "D-Pad Up", "⬆️"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_DPAD_DOWN, "dpad_down", "D-Pad Down", "⬇️"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_DPAD_LEFT, "dpad_left", "D-Pad Left", "⬅️"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_DPAD_RIGHT, "dpad_right", "D-Pad Right", "➡️"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_VOLUME_MUTE, "volume_mute", "Mute Button", "🔇"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_VOLUME_UP, "volume_up", "Volume Up", "🔊"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_VOLUME_DOWN, "volume_down", "Volume Down", "🔉"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_PROG_RED, "color_red", "Red Key", "🔴"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_PROG_GREEN, "color_green", "Green Key", "🟢"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_PROG_YELLOW, "color_yellow", "Yellow Key", "🟡"));
        supportedKeys.add(new KeyDef(KeyEvent.KEYCODE_PROG_BLUE, "color_blue", "Blue Key", "🔵"));
    }

    public List<KeyDef> getSupportedKeys() {
        return supportedKeys;
    }

    public KeyDef findKeyDef(int keyCode) {
        for (KeyDef k : supportedKeys) {
            if (k.keyCode == keyCode) return k;
            // Also alias KEYCODE_ENTER and KEYCODE_NUMPAD_ENTER to DPAD_CENTER
            if (k.keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                    && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                return k;
            }
            // Alias KEYCODE_ASSIST and KEYCODE_SEARCH to VOICE_ASSIST
            if (k.keyCode == KeyEvent.KEYCODE_VOICE_ASSIST
                    && (keyCode == KeyEvent.KEYCODE_ASSIST || keyCode == KeyEvent.KEYCODE_SEARCH)) {
                return k;
            }
            // Alias Xiaomi 9-Square 284
            if (k.keyCode == KeyEvent.KEYCODE_ALL_APPS && keyCode == 284) {
                return k;
            }
        }
        return null;
    }

    public String getActionLabel(String action) {
        if (action == null || action.equals(ACTION_NONE)) return "None (Default)";
        if (action.equals(ACTION_LOCK_SCREEN)) return "🔒 Lock Screen";
        if (action.equals(ACTION_SCREENSAVER)) return "🌙 Start Screensaver";
        if (action.equals(ACTION_APP_LIBRARY)) return "📚 Open App Library";
        if (action.equals(ACTION_GAME_ARENA)) return "🏎️ Open Game Arena";
        if (action.equals(ACTION_LAUNCHER_SETTINGS)) return "⚙️ Launcher Settings";
        if (action.equals(ACTION_TV_SETTINGS)) return "📺 Android TV Settings";
        if (action.equals(ACTION_MUTE_TOGGLE)) return "🔇 Toggle Mute";
        if (action.startsWith(ACTION_OPEN_APP)) {
            String pkg = getAppPackage(action);
            return "🚀 Open: " + getAppName(pkg);
        }
        return action;
    }

    public String getAppName(String pkg) {
        if (pkg == null || pkg.isEmpty()) return "Unknown App";
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    public static String buildAppAction(String pkg) {
        return ACTION_OPEN_APP + ":" + (pkg != null ? pkg : "");
    }

    public static String getAppPackage(String action) {
        if (action != null && action.startsWith(ACTION_OPEN_APP + ":")) {
            return action.substring((ACTION_OPEN_APP + ":").length()).trim();
        }
        return "";
    }

    // Getters and setters for button configuration
    public String getDoubleAction(String keyId) {
        return prefs.getString("key_" + keyId + "_double", ACTION_NONE);
    }

    public void setDoubleAction(String keyId, String action) {
        prefs.edit().putString("key_" + keyId + "_double", action).apply();
    }

    public String getTripleAction(String keyId) {
        return prefs.getString("key_" + keyId + "_triple", ACTION_NONE);
    }

    public void setTripleAction(String keyId, String action) {
        prefs.edit().putString("key_" + keyId + "_triple", action).apply();
    }

    public String getLongAction(String keyId) {
        return prefs.getString("key_" + keyId + "_long", ACTION_NONE);
    }

    public void setLongAction(String keyId, String action) {
        prefs.edit().putString("key_" + keyId + "_long", action).apply();
    }

    public int getLongDurationMs(String keyId) {
        return prefs.getInt("key_" + keyId + "_long_ms", 1000); // 1.0 second default
    }

    public void setLongDurationMs(String keyId, int durationMs) {
        prefs.edit().putInt("key_" + keyId + "_long_ms", durationMs).apply();
    }

    public boolean hasCustomMappings(String keyId) {
        return !getDoubleAction(keyId).equals(ACTION_NONE)
                || !getTripleAction(keyId).equals(ACTION_NONE)
                || !getLongAction(keyId).equals(ACTION_NONE);
    }

    public boolean hasAnyCustomMappings() {
        for (KeyDef k : supportedKeys) {
            if (hasCustomMappings(k.id)) return true;
        }
        return false;
    }

    /**
     * Intercepts key events for configured custom actions.
     * Returns true if event is consumed, false to proceed with normal processing.
     */
    public boolean handleKeyEvent(KeyEvent event, ActionCallback callback) {
        int code = event.getKeyCode();
        if (code == 0 && event.getScanCode() == 580) {
            code = KeyEvent.KEYCODE_ALL_APPS;
        }
        KeyDef keyDef = findKeyDef(code);
        if (keyDef == null) return false;

        String keyId = keyDef.id;
        String doubleAction = getDoubleAction(keyId);
        String tripleAction = getTripleAction(keyId);
        String longAction = getLongAction(keyId);
        int longDurationMs = getLongDurationMs(keyId);

        boolean hasLong = !longAction.equals(ACTION_NONE);
        boolean hasDouble = !doubleAction.equals(ACTION_NONE);
        boolean hasTriple = !tripleAction.equals(ACTION_NONE);

        if (!hasLong && !hasDouble && !hasTriple) {
            return false;
        }

        int action = event.getAction();

        // 1. Long Press Detection
        if (action == KeyEvent.ACTION_DOWN) {
            if (event.getRepeatCount() == 0) {
                activeKeyCode = code;
                longPressTriggered = false;
                if (hasLong) {
                    if (longPressRunnable != null) handler.removeCallbacks(longPressRunnable);
                    longPressRunnable = () -> {
                        longPressTriggered = true;
                        executeAction(longAction, callback);
                    };
                    handler.postDelayed(longPressRunnable, longDurationMs);
                }
            }
            // For long press or multi-click on designated keys, we still allow repeat or down
            // But if long press is triggered, consume repeat down events
            if (longPressTriggered) return true;
            return false;
        }

        // 2. Key Release & Multi-Click Handling
        if (action == KeyEvent.ACTION_UP) {
            if (longPressRunnable != null) {
                handler.removeCallbacks(longPressRunnable);
                longPressRunnable = null;
            }
            if (longPressTriggered) {
                longPressTriggered = false;
                return true; // Consumed long press up
            }

            if (!hasDouble && !hasTriple) {
                // Key only has long press configured, so normal click passes through
                return false;
            }

            // Track multi-click
            clickCount++;
            if (multiClickRunnable != null) {
                handler.removeCallbacks(multiClickRunnable);
            }

            if (clickCount >= 3 && hasTriple) {
                clickCount = 0;
                executeAction(tripleAction, callback);
                return true;
            } else if (clickCount == 2) {
                if (hasTriple) {
                    // Wait brief window to see if a 3rd click arrives
                    multiClickRunnable = () -> {
                        clickCount = 0;
                        if (hasDouble) executeAction(doubleAction, callback);
                    };
                    handler.postDelayed(multiClickRunnable, 260);
                    return true;
                } else if (hasDouble) {
                    clickCount = 0;
                    executeAction(doubleAction, callback);
                    return true;
                }
            } else if (clickCount == 1) {
                // First click: wait for potential double click
                multiClickRunnable = () -> {
                    clickCount = 0;
                    // Timeout passed: if user intended normal single click, we can fire standard behavior or let it pass
                };
                handler.postDelayed(multiClickRunnable, 320);
                // For DPAD directions or select, return false to avoid blocking navigation if only double/triple configured
                if (code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                        || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                        || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_BACK) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    private void executeAction(String actionStr, ActionCallback callback) {
        if (actionStr == null || actionStr.equals(ACTION_NONE)) return;
        SoundManager.getInstance(context).playJarvis();
        String pkg = "";
        String cleanAction = actionStr;
        if (actionStr.startsWith(ACTION_OPEN_APP)) {
            cleanAction = ACTION_OPEN_APP;
            pkg = getAppPackage(actionStr);
        }
        if (callback != null) {
            callback.onExecute(cleanAction, pkg);
        }
    }
}
