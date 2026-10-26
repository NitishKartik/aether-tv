package com.gathin.tvhome;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Frosted-glass Game Arena launcher dialog matching Aether TV aesthetic.
 */
public final class GameArenaDialog extends Dialog {

    public interface OnGameSelectedListener {
        void onGameSelected(int which);
    }

    private final OnGameSelectedListener listener;

    public GameArenaDialog(Context context, OnGameSelectedListener listener) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_game_arena);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        View cardAetherDrift = findViewById(R.id.cardAetherDrift);
        View cardOrbitalDefender = findViewById(R.id.cardOrbitalDefender);
        View btnBack = findViewById(R.id.btnBackArena);

        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start();
                SoundManager.getInstance(getContext()).playSelect();
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
            }
        };

        if (cardAetherDrift != null) {
            cardAetherDrift.setOnFocusChangeListener(focusListener);
            cardAetherDrift.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onGameSelected(0);
            });
        }

        if (cardOrbitalDefender != null) {
            cardOrbitalDefender.setOnFocusChangeListener(focusListener);
            cardOrbitalDefender.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onGameSelected(1);
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> dismiss());
            btnBack.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) SoundManager.getInstance(getContext()).playSelect();
            });
        }

        if (cardAetherDrift != null) {
            cardAetherDrift.post(cardAetherDrift::requestFocus);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            dismiss();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
