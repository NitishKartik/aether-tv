package com.gathin.tvhome;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Aether TV Privacy & Security Configuration Dialog.
 * Manage lockscreen state, 4-digit PIN, inactivity relock timeout, and media guard.
 */
public final class PrivacySecurityDialog extends Dialog {

    public interface PrivacySecurityListener {
        void onLockNowRequested();
        void onSecuritySettingsChanged();
    }

    private static final int[] TIMEOUT_MINUTES = {0, 1, 2, 5, 10, 15, 30, 60};
    private static final String[] TIMEOUT_LABELS = {
            "Never", "1 Minute", "2 Minutes", "5 Minutes",
            "10 Minutes", "15 Minutes (Default)", "30 Minutes", "1 Hour"
    };

    private final LockManager lockManager;
    private final PrivacySecurityListener listener;

    private TextView txtLockState;
    private TextView txtAutoLockTimeout;
    private TextView txtMediaGuardState;

    public PrivacySecurityDialog(Context context, LockManager lockManager, PrivacySecurityListener listener) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.lockManager = lockManager;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setContentView(R.layout.dialog_privacy_security);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        txtLockState = findViewById(R.id.txtLockState);
        txtAutoLockTimeout = findViewById(R.id.txtAutoLockTimeout);
        txtMediaGuardState = findViewById(R.id.txtMediaGuardState);

        View btnBack = findViewById(R.id.btnPrivacyBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> dismiss());
        }

        View rowLockToggle = findViewById(R.id.rowLockToggle);
        if (rowLockToggle != null) {
            rowLockToggle.setOnClickListener(v -> toggleLockEnabled());
            attachFocusSound(rowLockToggle);
        }

        View rowChangePin = findViewById(R.id.rowChangePin);
        if (rowChangePin != null) {
            rowChangePin.setOnClickListener(v -> showPinSetupDialog());
            attachFocusSound(rowChangePin);
        }

        View rowAutoLockTimeout = findViewById(R.id.rowAutoLockTimeout);
        if (rowAutoLockTimeout != null) {
            rowAutoLockTimeout.setOnClickListener(v -> showTimeoutPicker());
            attachFocusSound(rowAutoLockTimeout);
        }

        View rowMediaGuard = findViewById(R.id.rowMediaGuard);
        if (rowMediaGuard != null) {
            rowMediaGuard.setOnClickListener(v -> toggleMediaGuard());
            attachFocusSound(rowMediaGuard);
        }

        View rowLockNow = findViewById(R.id.rowLockNow);
        if (rowLockNow != null) {
            rowLockNow.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onLockNowRequested();
            });
            attachFocusSound(rowLockNow);
        }

        refreshUI();
        if (rowLockToggle != null) rowLockToggle.requestFocus();
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

    private void refreshUI() {
        boolean enabled = lockManager.isLockEnabled();
        if (txtLockState != null) {
            txtLockState.setText(enabled ? "Enabled" : "Disabled");
            txtLockState.setTextColor(enabled ? Color.parseColor("#FF00E676") : Color.parseColor("#FFFF5252"));
        }

        int currentMins = lockManager.getAutoRelockMinutes();
        if (txtAutoLockTimeout != null) {
            String label = "15 Minutes";
            for (int i = 0; i < TIMEOUT_MINUTES.length; i++) {
                if (TIMEOUT_MINUTES[i] == currentMins) {
                    label = TIMEOUT_LABELS[i];
                    break;
                }
            }
            txtAutoLockTimeout.setText(label);
        }

        boolean guard = lockManager.isMediaGuardEnabled();
        if (txtMediaGuardState != null) {
            txtMediaGuardState.setText(guard ? "Active" : "Off");
            txtMediaGuardState.setTextColor(guard ? Color.parseColor("#FF00E676") : Color.parseColor("#B3FFFFFF"));
        }
    }

    private void toggleLockEnabled() {
        boolean now = !lockManager.isLockEnabled();
        lockManager.setLockEnabled(now);
        SoundManager.getInstance(getContext()).playSelect();
        Toast.makeText(getContext(), now ? "Lock Screen Protected (PIN: 1234 default)" : "Lock Screen Disabled", Toast.LENGTH_SHORT).show();
        refreshUI();
        if (listener != null) listener.onSecuritySettingsChanged();
    }

    private void showPinSetupDialog() {
        SoundManager.getInstance(getContext()).playDialogOpen();
        PinSetupDialog dialog = new PinSetupDialog(getContext(), lockManager, pin -> {
            refreshUI();
            if (listener != null) listener.onSecuritySettingsChanged();
        });
        dialog.show();
    }

    private void toggleMediaGuard() {
        boolean now = !lockManager.isMediaGuardEnabled();
        lockManager.setMediaGuardEnabled(now);
        SoundManager.getInstance(getContext()).playSelect();
        Toast.makeText(getContext(), now ? "Media Guard Activated" : "Media Guard Deactivated", Toast.LENGTH_SHORT).show();
        refreshUI();
        if (listener != null) listener.onSecuritySettingsChanged();
    }

    private void showTimeoutPicker() {
        int current = lockManager.getAutoRelockMinutes();
        int checked = 5; // default 15 min
        for (int i = 0; i < TIMEOUT_MINUTES.length; i++) {
            if (TIMEOUT_MINUTES[i] == current) {
                checked = i;
                break;
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Auto-Lock Inactivity Timeout")
                .setSingleChoiceItems(TIMEOUT_LABELS, checked, (dialog, which) -> {
                    dialog.dismiss();
                    int chosen = TIMEOUT_MINUTES[which];
                    lockManager.setAutoRelockMinutes(chosen);
                    SoundManager.getInstance(getContext()).playSelect();
                    refreshUI();
                    if (listener != null) listener.onSecuritySettingsChanged();
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
