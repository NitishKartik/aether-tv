package com.gathin.tvhome;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.Locale;

/**
 * Aether TV 2-Pane System Control Center.
 * Unifies Display, Audio, Network, Application management, Device Diagnostics,
 * and Day/Night Scene profiles into a remote-friendly glassmorphism dashboard.
 */
public final class ControlCenterDialog extends Dialog {

    private final SceneManager sceneManager;
    private final LockManager lockManager;

    private TextView panelHeaderTitle;
    private TextView panelDescription;
    private LinearLayout panelActionsContainer;
    private Button[] navButtons;
    private int selectedNavIndex = 0;

    public ControlCenterDialog(Context context, SceneManager sceneManager, LockManager lockManager) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.sceneManager = sceneManager;
        this.lockManager = lockManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_control_center);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        panelHeaderTitle = findViewById(R.id.panelHeaderTitle);
        panelDescription = findViewById(R.id.panelDescription);
        panelActionsContainer = findViewById(R.id.panelActionsContainer);
        Button btnClose = findViewById(R.id.btnCloseCC);

        setupNavigation();
        selectCategory(0);

        btnClose.setOnClickListener(v -> dismiss());
    }

    private void setupNavigation() {
        Button catDisplay = findViewById(R.id.catDisplay);
        Button catSound = findViewById(R.id.catSound);
        Button catNetwork = findViewById(R.id.catNetwork);
        Button catApps = findViewById(R.id.catApps);
        Button catDevice = findViewById(R.id.catDevice);
        Button catScenes = findViewById(R.id.catScenes);

        navButtons = new Button[]{catDisplay, catSound, catNetwork, catApps, catDevice, catScenes};

        for (int i = 0; i < navButtons.length; i++) {
            final int index = i;
            navButtons[i].setOnClickListener(v -> selectCategory(index));
        }
    }

    private void selectCategory(int index) {
        selectedNavIndex = index;
        for (int i = 0; i < navButtons.length; i++) {
            navButtons[i].setSelected(i == index);
        }

        panelActionsContainer.removeAllViews();

        switch (index) {
            case 0:
                renderDisplayPanel();
                break;
            case 1:
                renderSoundPanel();
                break;
            case 2:
                renderNetworkPanel();
                break;
            case 3:
                renderAppsPanel();
                break;
            case 4:
                renderDevicePanel();
                break;
            case 5:
                renderScenesPanel();
                break;
        }
    }

    private void renderDisplayPanel() {
        panelHeaderTitle.setText("Display & Visuals");
        panelDescription.setText("Configure TV resolution, screensaver timeouts, and ambient themes.");

        addActionCard("⚙️ Launcher Settings", "Configure macOS lockscreen, D-Pad buttons, screensaver, and dock layout", v -> {
            dismiss();
            try {
                getContext().startActivity(new Intent(getContext(), LauncherSettingsActivity.class));
            } catch (Exception ignored) {}
        });

        addActionCard("📺 Android Display Settings", "Adjust resolution, aspect ratio, and HDR settings", v -> {
            launchSystemIntent(Settings.ACTION_DISPLAY_SETTINGS);
        });

        addActionCard("⏳ System Screensaver", "Select screensaver clock and idle timeout", v -> {
            launchSystemIntent("android.settings.DREAM_SETTINGS");
        });
    }

    private void renderSoundPanel() {
        panelHeaderTitle.setText("Sound & Audio Output");
        panelDescription.setText("Manage TV audio output formats, surround sound, and sound effects.");

        addActionCard("🔊 System Sound Settings", "Configure HDMI audio pass-through, volume, and Dolby audio", v -> {
            launchSystemIntent(Settings.ACTION_SOUND_SETTINGS);
        });
    }

    private void renderNetworkPanel() {
        panelHeaderTitle.setText("Network & Connectivity");
        panelDescription.setText("Manage Wi-Fi networks and Ethernet connection.");

        addActionCard("📶 Wi-Fi Settings", "Scan and connect to 2.4 GHz and 5 GHz wireless networks", v -> {
            launchSystemIntent(Settings.ACTION_WIFI_SETTINGS);
        });
    }

    private void renderAppsPanel() {
        panelHeaderTitle.setText("Applications & Storage");
        panelDescription.setText("Manage installed TV apps, clear cache, and review system permissions.");

        addActionCard("📱 Manage Installed Applications", "Force stop, clear app cache, or uninstall streaming apps", v -> {
            launchSystemIntent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
        });

        addActionCard("💾 Storage Diagnostics", "Inspect internal flash storage and USB drive allocation", v -> {
            launchSystemIntent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        });
    }

    private void renderDevicePanel() {
        panelHeaderTitle.setText("Device Diagnostics // Mi Box 4");
        panelDescription.setText("Live hardware diagnostics, Android 9 Pie build specs, and resource utilization.");

        // Storage metrics
        long totalBytes = 0;
        long freeBytes = 0;
        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            totalBytes = stat.getTotalBytes();
            freeBytes = stat.getAvailableBytes();
        } catch (Exception ignored) {}

        double totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0);
        double freeGb = freeBytes / (1024.0 * 1024.0 * 1024.0);

        // Memory metrics
        Runtime rt = Runtime.getRuntime();
        long maxMemMb = rt.maxMemory() / (1024 * 1024);
        long freeMemMb = rt.freeMemory() / (1024 * 1024);

        addInfoCard("Model & Hardware", Build.MODEL + " (" + Build.MANUFACTURER + ") · " + Build.CPU_ABI);
        addInfoCard("Android OS Version", "Android " + Build.VERSION.RELEASE + " (API Level " + Build.VERSION.SDK_INT + ")");
        addInfoCard("Build Fingerprint", Build.DISPLAY);
        addInfoCard("Internal Flash Storage", String.format(Locale.US, "%.1f GB Free of %.1f GB Total", freeGb, totalGb));
        addInfoCard("JVM Heap / RAM Usage", String.format(Locale.US, "%d MB Free / %d MB Max Heap", freeMemMb, maxMemMb));

        addActionCard("⚙️ System About Settings", "Open Android system About screen", v -> {
            launchSystemIntent(Settings.ACTION_DEVICE_INFO_SETTINGS);
        });
    }

    private void renderScenesPanel() {
        panelHeaderTitle.setText("Dynamic Day/Night Scene Profile");
        panelDescription.setText("Switch ambient scene themes or lock into your preferred visual style.");

        SceneManager.Scene current = sceneManager.getCurrentScene();
        addInfoCard("Active Ambient Scene", current.getDisplayName() + " — " + current.getDescription());

        addActionCard("⏱️ Auto Dynamic Schedule (Current: " + current.getDisplayName() + ")", "Automatically transitions between Morning, Day, Evening, and Night based on clock", v -> {
            sceneManager.setMode(SceneManager.Mode.AUTO);
            notifyMainActivityScene();
            Toast.makeText(getContext(), "Scene mode set to AUTO", Toast.LENGTH_SHORT).show();
            renderScenesPanel();
        });

        addActionCard("🌅 Morning Glow (Manual Override)", "Celestial blue and golden dawn accents", v -> {
            sceneManager.setMode(SceneManager.Mode.MANUAL_MORNING);
            notifyMainActivityScene();
            Toast.makeText(getContext(), "Scene set to Morning Glow", Toast.LENGTH_SHORT).show();
            renderScenesPanel();
        });

        addActionCard("☀️ Midday Clarity (Manual Override)", "High-contrast arctic teal for bright rooms", v -> {
            sceneManager.setMode(SceneManager.Mode.MANUAL_DAY);
            notifyMainActivityScene();
            Toast.makeText(getContext(), "Scene set to Midday Clarity", Toast.LENGTH_SHORT).show();
            renderScenesPanel();
        });

        addActionCard("🌇 Sunset Cyber (Manual Override)", "Warm amber and twilight violet glow", v -> {
            sceneManager.setMode(SceneManager.Mode.MANUAL_EVENING);
            notifyMainActivityScene();
            Toast.makeText(getContext(), "Scene set to Sunset Cyber", Toast.LENGTH_SHORT).show();
            renderScenesPanel();
        });

        addActionCard("🌌 Midnight OLED (Manual Override)", "Deep black surfaces with neon purple & cyan edges", v -> {
            sceneManager.setMode(SceneManager.Mode.MANUAL_NIGHT);
            notifyMainActivityScene();
            Toast.makeText(getContext(), "Scene set to Midnight OLED", Toast.LENGTH_SHORT).show();
            renderScenesPanel();
        });
    }

    private void notifyMainActivityScene() {
        if (getContext() instanceof MainActivity) {
            ((MainActivity) getContext()).applyAetherScene(sceneManager.getCurrentScene());
        }
    }

    private void addActionCard(String title, String subtitle, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.glass_card_bg);
        card.setPadding(20, 16, 20, 16);
        card.setFocusable(true);
        card.setClickable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, 12);
        card.setLayoutParams(lp);

        TextView titleView = new TextView(getContext());
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(16f);

        TextView subView = new TextView(getContext());
        subView.setText(subtitle);
        subView.setTextColor(Color.parseColor("#99FFFFFF"));
        subView.setTextSize(12f);
        subView.setPadding(0, 4, 0, 0);

        card.addView(titleView);
        card.addView(subView);
        card.setOnClickListener(listener);

        card.setOnFocusChangeListener((v, hasFocus) -> {
            titleView.setTextColor(hasFocus ? Color.WHITE : Color.parseColor("#E6FFFFFF"));
            subView.setTextColor(hasFocus ? Color.WHITE : Color.parseColor("#99FFFFFF"));
        });

        panelActionsContainer.addView(card);
    }

    private void addInfoCard(String label, String value) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.glass_card_bg);
        card.setPadding(20, 14, 20, 14);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, 10);
        card.setLayoutParams(lp);

        TextView labelView = new TextView(getContext());
        labelView.setText(label.toUpperCase(Locale.getDefault()));
        labelView.setTextColor(Color.parseColor("#FF00E5FF"));
        labelView.setTextSize(11f);
        labelView.setLetterSpacing(0.1f);

        TextView valueView = new TextView(getContext());
        valueView.setText(value);
        valueView.setTextColor(Color.WHITE);
        valueView.setTextSize(15f);
        valueView.setPadding(0, 4, 0, 0);

        card.addView(labelView);
        card.addView(valueView);

        panelActionsContainer.addView(card);
    }

    private void launchSystemIntent(String action) {
        try {
            Intent intent = new Intent(action);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                getContext().startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Setting activity not supported on this TV", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open system screen", Toast.LENGTH_SHORT).show();
        }
    }
}
