package com.gathin.tvhome;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Dialog container for Aether Drift TV arcade racing game.
 */
public final class AetherDriftDialog extends Dialog {

    private AetherDriftView gameView;
    private Button btnPause;
    private Button btnClose;

    public AetherDriftDialog(Context context) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_drift_game);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        gameView = findViewById(R.id.driftGameView);
        btnPause = findViewById(R.id.btnPauseDrift);
        btnClose = findViewById(R.id.btnCloseDrift);

        btnPause.setOnClickListener(v -> {
            gameView.togglePause();
            btnPause.setText(gameView.getGameState() == AetherDriftView.GameState.PAUSED ? "Resume" : "Pause");
        });

        btnClose.setOnClickListener(v -> dismiss());

        gameView.setFocusable(true);
        gameView.setFocusableInTouchMode(true);
        gameView.requestFocus();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (gameView.getGameState() == AetherDriftView.GameState.PLAYING) {
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

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (gameView != null && gameView.onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
