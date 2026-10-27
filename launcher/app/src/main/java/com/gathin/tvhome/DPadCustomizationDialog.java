package com.gathin.tvhome;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aether TV D-Pad & Remote Customization Dialog.
 * Configure double-click, triple-click, and long-press actions (with configurable hold duration)
 * for all TV remote buttons.
 */
public final class DPadCustomizationDialog extends Dialog {

    private static final String[] ACTION_IDS = {
            KeyRemapManager.ACTION_NONE,
            KeyRemapManager.ACTION_LOCK_SCREEN,
            KeyRemapManager.ACTION_SCREENSAVER,
            KeyRemapManager.ACTION_APP_LIBRARY,
            KeyRemapManager.ACTION_GAME_ARENA,
            KeyRemapManager.ACTION_LAUNCHER_SETTINGS,
            KeyRemapManager.ACTION_TV_SETTINGS,
            KeyRemapManager.ACTION_MUTE_TOGGLE,
            KeyRemapManager.ACTION_OPEN_APP
    };

    private static final String[] ACTION_TITLES = {
            "None (Default)",
            "🔒 Lock Screen Now",
            "🌙 Start Screensaver",
            "📚 Open App Library",
            "🏎️ Open Game Arena",
            "⚙️ Launcher Settings",
            "📺 Android TV Settings",
            "🔇 Toggle Audio Mute",
            "🚀 Launch Specific App..."
    };

    private static final int[] DURATIONS_MS = {500, 800, 1000, 1500, 2000, 3000};
    private static final String[] DURATION_LABELS = {
            "0.5 Seconds", "0.8 Seconds", "1.0 Second (Recommended)", "1.5 Seconds", "2.0 Seconds", "3.0 Seconds"
    };

    private final KeyRemapManager remapManager;
    private final List<KeyRemapManager.KeyDef> keys;
    private KeyRemapManager.KeyDef selectedKey;

    private LinearLayout keyListContainer;
    private TextView selectedKeyIcon;
    private TextView selectedKeyTitle;
    private TextView txtDoubleClickAction;
    private TextView txtTripleClickAction;
    private TextView txtLongPressAction;
    private TextView txtHoldDuration;

    public DPadCustomizationDialog(Context context) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.remapManager = KeyRemapManager.getInstance(context);
        this.keys = remapManager.getSupportedKeys();
        if (!keys.isEmpty()) {
            this.selectedKey = keys.get(0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setContentView(R.layout.dialog_dpad_customization);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        keyListContainer = findViewById(R.id.keyListContainer);
        selectedKeyIcon = findViewById(R.id.selectedKeyIcon);
        selectedKeyTitle = findViewById(R.id.selectedKeyTitle);
        txtDoubleClickAction = findViewById(R.id.txtDoubleClickAction);
        txtTripleClickAction = findViewById(R.id.txtTripleClickAction);
        txtLongPressAction = findViewById(R.id.txtLongPressAction);
        txtHoldDuration = findViewById(R.id.txtHoldDuration);

        View btnBack = findViewById(R.id.btnDpadBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> dismiss());

        View rowDouble = findViewById(R.id.rowDoubleClick);
        if (rowDouble != null) {
            rowDouble.setOnClickListener(v -> showActionPicker("double"));
            attachFocusSound(rowDouble);
        }

        View rowTriple = findViewById(R.id.rowTripleClick);
        if (rowTriple != null) {
            rowTriple.setOnClickListener(v -> showActionPicker("triple"));
            attachFocusSound(rowTriple);
        }

        View rowLong = findViewById(R.id.rowLongPress);
        if (rowLong != null) {
            rowLong.setOnClickListener(v -> showActionPicker("long"));
            attachFocusSound(rowLong);
        }

        View rowHold = findViewById(R.id.rowHoldDuration);
        if (rowHold != null) {
            rowHold.setOnClickListener(v -> showDurationPicker());
            attachFocusSound(rowHold);
        }

        buildKeyList();
        updateSelectedKeyUI();
    }

    private void attachFocusSound(View v) {
        v.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                view.setScaleX(1.02f);
                view.setScaleY(1.02f);
                SoundManager.getInstance(getContext()).playKeyTap();
            } else {
                view.setScaleX(1.0f);
                view.setScaleY(1.0f);
            }
        });
    }

    private void buildKeyList() {
        if (keyListContainer == null) return;
        keyListContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < keys.size(); i++) {
            final KeyRemapManager.KeyDef k = keys.get(i);
            View row = inflater.inflate(R.layout.item_setting_key_row, keyListContainer, false);
            if (row == null) {
                // Fallback creation
                row = createDefaultKeyRow(k);
            } else {
                bindKeyRow(row, k);
            }
            final View finalRow = row;
            row.setOnClickListener(v -> {
                selectedKey = k;
                SoundManager.getInstance(getContext()).playSelect();
                updateSelectedKeyUI();
            });
            row.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    selectedKey = k;
                    updateSelectedKeyUI();
                    v.setScaleX(1.03f);
                    v.setScaleY(1.03f);
                    SoundManager.getInstance(getContext()).playKeyTap();
                } else {
                    v.setScaleX(1.0f);
                    v.setScaleY(1.0f);
                }
            });
            keyListContainer.addView(row);
            if (i == 0) row.requestFocus();
        }
    }

    private View createDefaultKeyRow(KeyRemapManager.KeyDef k) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.glass_pill_bg);
        row.setFocusable(true);
        row.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * getContext().getResources().getDisplayMetrics().density));
        lp.bottomMargin = (int) (8 * getContext().getResources().getDisplayMetrics().density);
        row.setLayoutParams(lp);
        row.setPadding(24, 0, 24, 0);

        TextView icon = new TextView(getContext());
        icon.setText(k.icon);
        icon.setTextSize(18);
        row.addView(icon);

        TextView title = new TextView(getContext());
        title.setText(k.name);
        title.setTextColor(Color.WHITE);
        title.setTextSize(14);
        title.setPadding(16, 0, 0, 0);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        title.setLayoutParams(tlp);
        row.addView(title);

        if (remapManager.hasCustomMappings(k.id)) {
            TextView dot = new TextView(getContext());
            dot.setText("●");
            dot.setTextColor(Color.parseColor("#FF00E5FF"));
            dot.setTextSize(14);
            row.addView(dot);
        }

        return row;
    }

    private void bindKeyRow(View row, KeyRemapManager.KeyDef k) {
        TextView icon = row.findViewById(R.id.keyRowIcon);
        TextView title = row.findViewById(R.id.keyRowTitle);
        TextView badge = row.findViewById(R.id.keyRowBadge);

        if (icon != null) icon.setText(k.icon);
        if (title != null) title.setText(k.name);
        if (badge != null) {
            badge.setVisibility(remapManager.hasCustomMappings(k.id) ? View.VISIBLE : View.GONE);
        }
    }

    private void updateSelectedKeyUI() {
        if (selectedKey == null) return;
        if (selectedKeyIcon != null) selectedKeyIcon.setText(selectedKey.icon);
        if (selectedKeyTitle != null) selectedKeyTitle.setText(selectedKey.name);

        String doubleAction = remapManager.getDoubleAction(selectedKey.id);
        String tripleAction = remapManager.getTripleAction(selectedKey.id);
        String longAction = remapManager.getLongAction(selectedKey.id);
        int durationMs = remapManager.getLongDurationMs(selectedKey.id);

        if (txtDoubleClickAction != null) {
            txtDoubleClickAction.setText(remapManager.getActionLabel(doubleAction));
            txtDoubleClickAction.setTextColor(doubleAction.equals(KeyRemapManager.ACTION_NONE) ? Color.parseColor("#80FFFFFF") : Color.parseColor("#FF00E5FF"));
        }
        if (txtTripleClickAction != null) {
            txtTripleClickAction.setText(remapManager.getActionLabel(tripleAction));
            txtTripleClickAction.setTextColor(tripleAction.equals(KeyRemapManager.ACTION_NONE) ? Color.parseColor("#80FFFFFF") : Color.parseColor("#FF00E5FF"));
        }
        if (txtLongPressAction != null) {
            txtLongPressAction.setText(remapManager.getActionLabel(longAction));
            txtLongPressAction.setTextColor(longAction.equals(KeyRemapManager.ACTION_NONE) ? Color.parseColor("#80FFFFFF") : Color.parseColor("#FF00E5FF"));
        }
        if (txtHoldDuration != null) {
            txtHoldDuration.setText(String.format("%.1f SECONDS", durationMs / 1000.0f));
        }
    }

    private void showActionPicker(String triggerType) {
        if (selectedKey == null) return;
        SoundManager.getInstance(getContext()).playDialogOpen();

        new AlertDialog.Builder(getContext())
                .setTitle("Select Action for " + selectedKey.name)
                .setItems(ACTION_TITLES, (dialog, which) -> {
                    dialog.dismiss();
                    String action = ACTION_IDS[which];
                    if (action.equals(KeyRemapManager.ACTION_OPEN_APP)) {
                        showAppPicker(triggerType);
                    } else {
                        saveAction(triggerType, action);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAppPicker(String triggerType) {
        PackageManager pm = getContext().getPackageManager();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(tv, 0);
        if (list.isEmpty()) {
            Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            list = pm.queryIntentActivities(main, 0);
        }

        final List<String[]> appEntries = new ArrayList<>();
        for (ResolveInfo r : list) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getContext().getPackageName())) continue;
            String label = r.loadLabel(pm).toString();
            boolean found = false;
            for (String[] a : appEntries) {
                if (a[1].equals(pkg)) {
                    found = true;
                    break;
                }
            }
            if (!found) appEntries.add(new String[]{label, pkg});
        }
        Collections.sort(appEntries, (a, b) -> a[0].compareToIgnoreCase(b[0]));

        String[] labels = new String[appEntries.size()];
        for (int i = 0; i < appEntries.size(); i++) {
            labels[i] = appEntries.get(i)[0];
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Select App to Launch")
                .setItems(labels, (dialog, which) -> {
                    dialog.dismiss();
                    String pkg = appEntries.get(which)[1];
                    String fullAction = KeyRemapManager.buildAppAction(pkg);
                    saveAction(triggerType, fullAction);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveAction(String triggerType, String action) {
        if (selectedKey == null) return;
        if (triggerType.equals("double")) {
            remapManager.setDoubleAction(selectedKey.id, action);
        } else if (triggerType.equals("triple")) {
            remapManager.setTripleAction(selectedKey.id, action);
        } else if (triggerType.equals("long")) {
            remapManager.setLongAction(selectedKey.id, action);
        }
        SoundManager.getInstance(getContext()).playSelect();
        Toast.makeText(getContext(), "Action saved for " + selectedKey.name, Toast.LENGTH_SHORT).show();
        updateSelectedKeyUI();
        buildKeyList();
    }

    private void showDurationPicker() {
        if (selectedKey == null) return;
        int currentMs = remapManager.getLongDurationMs(selectedKey.id);
        int checked = 2; // default 1.0s
        for (int i = 0; i < DURATIONS_MS.length; i++) {
            if (DURATIONS_MS[i] == currentMs) {
                checked = i;
                break;
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Hold Threshold for " + selectedKey.name)
                .setSingleChoiceItems(DURATION_LABELS, checked, (dialog, which) -> {
                    dialog.dismiss();
                    remapManager.setLongDurationMs(selectedKey.id, DURATIONS_MS[which]);
                    SoundManager.getInstance(getContext()).playSelect();
                    updateSelectedKeyUI();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            dismiss();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
