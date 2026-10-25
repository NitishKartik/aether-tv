package com.gathin.tvhome;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Dialog host for the Orbital Defender offline TV arcade game.
 */
public final class OrbitalDefenderDialog extends Dialog {

    private OrbitalDefenderView gameView;
    private Button btnPause;
    private Button btnClose;

    public OrbitalDefenderDialog(Context context) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_orbital_game);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        gameView = findViewById(R.id.orbitalGameView);
        btnPause = findViewById(R.id.btnPauseGame);
        btnClose = findViewById(R.id.btnCloseGame);

        btnPause.setOnClickListener(v -> {
            gameView.togglePause();
            btnPause.setText(gameView.getGameState() == OrbitalDefenderView.GameState.PAUSED ? "Resume" : "Pause");
        });

        btnClose.setOnClickListener(v -> dismiss());

        gameView.setFocusable(true);
        gameView.setFocusableInTouchMode(true);
        gameView.requestFocus();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (gameView.getGameState() == OrbitalDefenderView.GameState.PLAYING) {
                gameView.togglePause();
                btnPause.setText("Resume");
                return true;
            }
            dismiss();
            return true;
        }
        if (gameView != null && gameView.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
