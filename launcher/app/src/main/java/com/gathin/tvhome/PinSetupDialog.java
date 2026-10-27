package com.gathin.tvhome;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Aether TV Frosted Glass PIN Setup Dialog.
 * Remote-friendly 4-digit PIN configuration with audio feedback and Outfit styling.
 */
public final class PinSetupDialog extends Dialog {

    public interface OnPinSetListener {
        void onPinSet(String newPin);
    }

    private final LockManager lockManager;
    private final OnPinSetListener listener;
    private final StringBuilder pinBuilder = new StringBuilder();

    private TextView pinSetupTitle;
    private TextView pinSetupStatus;
    private TextView[] pinDots = new TextView[4];

    public PinSetupDialog(Context context, LockManager lockManager, OnPinSetListener listener) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.lockManager = lockManager;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setContentView(R.layout.dialog_pin_setup);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        pinSetupTitle = findViewById(R.id.pinSetupTitle);
        pinSetupStatus = findViewById(R.id.pinSetupStatus);
        pinDots[0] = findViewById(R.id.pinDot0);
        pinDots[1] = findViewById(R.id.pinDot1);
        pinDots[2] = findViewById(R.id.pinDot2);
        pinDots[3] = findViewById(R.id.pinDot3);

        setupKeypad();
        updateDots();

        View cancel = findViewById(R.id.btnPinCancel);
        if (cancel != null) cancel.setOnClickListener(v -> dismiss());

        Button btn1 = findViewById(R.id.btnPin1);
        if (btn1 != null) btn1.requestFocus();
    }

    private void setupKeypad() {
        int[] numIds = {
                R.id.btnPin0, R.id.btnPin1, R.id.btnPin2, R.id.btnPin3, R.id.btnPin4,
                R.id.btnPin5, R.id.btnPin6, R.id.btnPin7, R.id.btnPin8, R.id.btnPin9
        };

        for (int i = 0; i < numIds.length; i++) {
            final String digit = String.valueOf(i);
            Button btn = findViewById(numIds[i]);
            if (btn != null) {
                btn.setOnClickListener(v -> appendDigit(digit));
            }
        }

        Button btnClear = findViewById(R.id.btnPinClear);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                if (pinBuilder.length() > 0) {
                    pinBuilder.deleteCharAt(pinBuilder.length() - 1);
                    SoundManager.getInstance(getContext()).playKeyTap();
                    updateDots();
                }
            });
        }

        Button btnSave = findViewById(R.id.btnPinSave);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> submitPin());
        }
    }

    private void appendDigit(String digit) {
        if (pinBuilder.length() < 4) {
            pinBuilder.append(digit);
            SoundManager.getInstance(getContext()).playKeyTap();
            updateDots();
            if (pinBuilder.length() == 4) {
                Button btnSave = findViewById(R.id.btnPinSave);
                if (btnSave != null) btnSave.requestFocus();
            }
        }
    }

    private void updateDots() {
        int len = pinBuilder.length();
        for (int i = 0; i < 4; i++) {
            if (pinDots[i] != null) {
                if (i < len) {
                    pinDots[i].setText("●");
                    pinDots[i].setTextColor(Color.parseColor("#FF00E5FF"));
                } else {
                    pinDots[i].setText("○");
                    pinDots[i].setTextColor(Color.parseColor("#80FFFFFF"));
                }
            }
        }
    }

    private void submitPin() {
        if (pinBuilder.length() != 4) {
            SoundManager.getInstance(getContext()).playError();
            pinSetupStatus.setText("PIN must be exactly 4 digits");
            pinSetupStatus.setTextColor(Color.parseColor("#FFFF5252"));
            return;
        }

        String pin = pinBuilder.toString();
        if (lockManager.setPassword(pin)) {
            SoundManager.getInstance(getContext()).playUnlock();
            Toast.makeText(getContext(), "PIN updated successfully: " + pin, Toast.LENGTH_LONG).show();
            if (listener != null) listener.onPinSet(pin);
            dismiss();
        } else {
            SoundManager.getInstance(getContext()).playError();
            pinSetupStatus.setText("Failed to save PIN");
            pinSetupStatus.setTextColor(Color.parseColor("#FFFF5252"));
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int code = event.getKeyCode();
            if (code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9) {
                appendDigit(String.valueOf(code - KeyEvent.KEYCODE_0));
                return true;
            } else if (code == KeyEvent.KEYCODE_DEL) {
                if (pinBuilder.length() > 0) {
                    pinBuilder.deleteCharAt(pinBuilder.length() - 1);
                    SoundManager.getInstance(getContext()).playKeyTap();
                    updateDots();
                }
                return true;
            } else if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (pinBuilder.length() == 4) {
                    submitPin();
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
