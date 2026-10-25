package com.gathin.tvhome;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Aether TV macOS-Style Frosted Glass Lock Screen Dialog.
 * Displays macOS-inspired clock, profile avatar, PIN masking, on-screen TV remote keypad,
 * smooth error shake animations, and exponential lockout countdown.
 */
public final class LockScreenDialog extends Dialog {

    public interface OnUnlockListener {
        void onUnlocked();
    }

    private final LockManager lockManager;
    private final SceneManager sceneManager;
    private final OnUnlockListener listener;
    private final StringBuilder pinBuilder = new StringBuilder();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView lockUserName;
    private TextView userInitial;
    private TextView lockPinDisplay;
    private TextView lockStatus;
    private View lockCard;
    private Runnable countdownRunnable;

    public LockScreenDialog(Context context, LockManager lockManager, SceneManager sceneManager, OnUnlockListener listener) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.lockManager = lockManager;
        this.sceneManager = sceneManager;
        this.listener = listener;
        setCancelable(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_macos_lockscreen);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        lockUserName = findViewById(R.id.lockUserName);
        userInitial = findViewById(R.id.userInitial);
        lockPinDisplay = findViewById(R.id.lockPinDisplay);
        lockStatus = findViewById(R.id.lockStatus);
        lockCard = findViewById(R.id.lockCard);

        String userName = sceneManager.getUserName();
        lockUserName.setText(userName);
        if (!userName.isEmpty()) {
            userInitial.setText(String.valueOf(userName.charAt(0)).toUpperCase());
        }

        if (!lockManager.isLockEnabled()) {
            lockStatus.setText("Set 4-digit security PIN using remote keypad, then tap ➔");
            lockStatus.setTextColor(Color.parseColor("#FF00E5FF"));
            lockPinDisplay.setHint("Set PIN");
        }

        setupKeypad();
        checkLockoutState();
    }

    private void setupKeypad() {
        int[] numIds = {
            R.id.btnKey0, R.id.btnKey1, R.id.btnKey2, R.id.btnKey3, R.id.btnKey4,
            R.id.btnKey5, R.id.btnKey6, R.id.btnKey7, R.id.btnKey8, R.id.btnKey9
        };

        for (int i = 0; i < numIds.length; i++) {
            final String digit = String.valueOf(i);
            Button btn = findViewById(numIds[i]);
            if (btn != null) {
                btn.setOnClickListener(v -> appendDigit(digit));
            }
        }

        Button btnClear = findViewById(R.id.btnKeyClear);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> deleteDigit());
        }

        Button btnUnlock = findViewById(R.id.btnKeyUnlock);
        if (btnUnlock != null) {
            btnUnlock.setOnClickListener(v -> attemptUnlock());
        }
    }

    private void appendDigit(String digit) {
        if (lockManager.isLockedOut()) {
            checkLockoutState();
            return;
        }
        if (pinBuilder.length() < 12) {
            pinBuilder.append(digit);
            updatePinDisplay();
        }
    }

    private void deleteDigit() {
        if (pinBuilder.length() > 0) {
            pinBuilder.deleteCharAt(pinBuilder.length() - 1);
            updatePinDisplay();
        }
    }

    private void updatePinDisplay() {
        if (pinBuilder.length() == 0) {
            lockPinDisplay.setText("");
            lockPinDisplay.setHint("Enter PIN");
        } else {
            StringBuilder masked = new StringBuilder();
            for (int i = 0; i < pinBuilder.length(); i++) {
                masked.append("● ");
            }
            lockPinDisplay.setText(masked.toString().trim());
        }
    }

    private void attemptUnlock() {
        if (lockManager.isLockedOut()) {
            checkLockoutState();
            return;
        }

        String pin = pinBuilder.toString();
        if (pin.isEmpty()) {
            shakeCard();
            lockStatus.setText("Please enter your PIN");
            lockStatus.setTextColor(Color.parseColor("#FFFF5252"));
            return;
        }

        if (!lockManager.isLockEnabled()) {
            if (pin.length() < 4) {
                shakeCard();
                lockStatus.setText("PIN must be at least 4 digits");
                lockStatus.setTextColor(Color.parseColor("#FFFF5252"));
                return;
            }
            lockManager.setPassword(pin);
            lockManager.lock();
            lockStatus.setText("PIN saved! Aether TV is locked.");
            lockStatus.setTextColor(Color.parseColor("#FF00E676"));
            pinBuilder.setLength(0);
            updatePinDisplay();
            return;
        }

        boolean success = lockManager.verifyPassword(pin);
        if (success) {
            lockStatus.setText("Unlocked!");
            lockStatus.setTextColor(Color.parseColor("#FF00E676"));
            handler.postDelayed(() -> {
                dismiss();
                if (listener != null) {
                    listener.onUnlocked();
                }
            }, 300);
        } else {
            shakeCard();
            pinBuilder.setLength(0);
            updatePinDisplay();
            if (lockManager.isLockedOut()) {
                checkLockoutState();
            } else {
                int remaining = lockManager.getRemainingAttempts();
                lockStatus.setText("Incorrect PIN (" + remaining + " attempts remaining)");
                lockStatus.setTextColor(Color.parseColor("#FFFF5252"));
            }
        }
    }

    private void checkLockoutState() {
        if (lockManager.isLockedOut()) {
            long remainingSec = lockManager.getRemainingLockoutSeconds();
            lockStatus.setText("Too many attempts. Locked out for " + remainingSec + "s");
            lockStatus.setTextColor(Color.parseColor("#FFFF5252"));
            lockPinDisplay.setEnabled(false);

            if (countdownRunnable != null) {
                handler.removeCallbacks(countdownRunnable);
            }
            countdownRunnable = new Runnable() {
                @Override
                public void run() {
                    if (lockManager.isLockedOut()) {
                        long sec = lockManager.getRemainingLockoutSeconds();
                        lockStatus.setText("Too many attempts. Locked out for " + sec + "s");
                        handler.postDelayed(this, 1000);
                    } else {
                        lockStatus.setText("Ready to unlock. Enter PIN");
                        lockStatus.setTextColor(Color.parseColor("#99FFFFFF"));
                        lockPinDisplay.setEnabled(true);
                    }
                }
            };
            handler.postDelayed(countdownRunnable, 1000);
        }
    }

    private void shakeCard() {
        if (lockCard != null) {
            ObjectAnimator shake = ObjectAnimator.ofFloat(
                lockCard, "translationX", 0f, 25f, -25f, 20f, -20f, 10f, -10f, 0f
            );
            shake.setDuration(400);
            shake.start();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            appendDigit(String.valueOf(keyCode - KeyEvent.KEYCODE_0));
            return true;
        } else if (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
            appendDigit(String.valueOf(keyCode - KeyEvent.KEYCODE_NUMPAD_0));
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            deleteDigit();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            attemptUnlock();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            // macOS lock screen prevents exiting to launcher without PIN
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
    }
}
