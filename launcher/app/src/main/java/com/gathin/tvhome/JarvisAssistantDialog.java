package com.gathin.tvhome;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Aether TV Jarvis Voice Assistant HUD Dialog.
 * Controls JarvisView states (IDLE, LISTENING, PROCESSING, SPEAKING, ERROR),
 * executes voice commands or TV remote quick actions with audio privacy compliance.
 */
public final class JarvisAssistantDialog extends Dialog {

    public interface AssistantActionListener {
        void onOpenGame();
        void onOpenAiDashboard();
        void onOpenAdGuardDns();
        void onLockTv();
        void onOpenSettings();
        void onOpenYouTube();
    }

    private final AssistantActionListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private JarvisView jarvisView;
    private TextView jarvisStateText;
    private TextView jarvisResponseText;
    private Button btnMicTrigger;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;

    public JarvisAssistantDialog(Context context, AssistantActionListener listener) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_jarvis_assistant);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        jarvisView = findViewById(R.id.jarvisOrbitalView);
        jarvisStateText = findViewById(R.id.jarvisStateText);
        jarvisResponseText = findViewById(R.id.jarvisResponseText);
        btnMicTrigger = findViewById(R.id.btnMicTrigger);

        btnMicTrigger.setOnClickListener(v -> toggleListening());

        Button btnCmdYoutube = findViewById(R.id.btnCmdYoutube);
        Button btnCmdGame = findViewById(R.id.btnCmdGame);
        Button btnCmdAi = findViewById(R.id.btnCmdAi);
        Button btnCmdDns = findViewById(R.id.btnCmdDns);
        Button btnCmdLock = findViewById(R.id.btnCmdLock);
        Button btnClose = findViewById(R.id.btnCloseJarvis);

        if (btnCmdYoutube != null) {
            btnCmdYoutube.setOnClickListener(v -> {
                speakResponse("Launching YouTube application.", () -> {
                    dismiss();
                    if (listener != null) listener.onOpenYouTube();
                });
            });
        }

        btnCmdGame.setOnClickListener(v -> {
            speakResponse("Launching Orbital Defender arcade defense system.", () -> {
                dismiss();
                if (listener != null) listener.onOpenGame();
            });
        });

        btnCmdAi.setOnClickListener(v -> {
            speakResponse("Opening AI Usage and Provider Quotas Dashboard.", () -> {
                dismiss();
                if (listener != null) listener.onOpenAiDashboard();
            });
        });

        btnCmdDns.setOnClickListener(v -> {
            speakResponse("Accessing AdGuard Private DNS Configuration.", () -> {
                dismiss();
                if (listener != null) listener.onOpenAdGuardDns();
            });
        });

        btnCmdLock.setOnClickListener(v -> {
            speakResponse("Locking Aether TV workstation.", () -> {
                dismiss();
                if (listener != null) listener.onLockTv();
            });
        });

        btnClose.setOnClickListener(v -> dismiss());
    }

    private void toggleListening() {
        if (isListening) {
            stopListening();
        } else {
            startListening();
        }
    }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(getContext())) {
            jarvisStateText.setText("MIC UNAVAILABLE // TV REMOTE READY");
            jarvisResponseText.setText("Speech recognition service not present on this firmware. Use TV remote buttons or quick commands below.");
            jarvisView.setState(JarvisView.State.IDLE);
            return;
        }

        try {
            isListening = true;
            btnMicTrigger.setText("■ Stop");
            btnMicTrigger.setTextColor(Color.parseColor("#FFFF5252"));
            jarvisView.setState(JarvisView.State.LISTENING);
            jarvisStateText.setText("LISTENING // AUDIO RECEPTOR ACTIVE");
            jarvisResponseText.setText("Listening... Speak your command now.");

            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
            }

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {
                    float normalized = Math.max(0f, Math.min(1f, (rmsdB + 2f) / 12f));
                    jarvisView.setAudioLevel(normalized);
                }
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {
                    jarvisView.setState(JarvisView.State.PROCESSING);
                    jarvisStateText.setText("PROCESSING NEURAL INTENT...");
                }
                @Override public void onError(int error) {
                    stopListening();
                    jarvisView.setState(JarvisView.State.ERROR);
                    jarvisStateText.setText("RECOGNITION TIMEOUT // STANDBY");
                    jarvisResponseText.setText("No voice input detected. Select a quick action or press Speak Now again.");
                }
                @Override public void onResults(Bundle results) {
                    stopListening();
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        handleCommand(matches.get(0));
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            stopListening();
            jarvisView.setState(JarvisView.State.IDLE);
            jarvisResponseText.setText("Audio recognition failed. You can use the quick commands below.");
        }
    }

    private void stopListening() {
        isListening = false;
        btnMicTrigger.setText("● Speak Now");
        btnMicTrigger.setTextColor(Color.parseColor("#FF00E676"));
        jarvisView.setAudioLevel(0f);
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }
    }

    private void handleCommand(String rawCommand) {
        String cmd = rawCommand.toLowerCase(Locale.getDefault());
        jarvisView.setState(JarvisView.State.PROCESSING);
        jarvisStateText.setText("INTENT: \"" + rawCommand + "\"");

        if (cmd.contains("youtube") || cmd.contains("you tube") || cmd.contains("video") || cmd.contains("play video")) {
            speakResponse("Launching YouTube application.", () -> {
                dismiss();
                if (listener != null) listener.onOpenYouTube();
            });
        } else if (cmd.contains("what is ai") || cmd.contains("what's ai") || cmd.contains("what is artificial")
                || cmd.contains("ai") || cmd.contains("usage") || cmd.contains("token") || cmd.contains("limit")) {
            speakResponse("Accessing AI intelligence telemetry and usage analytics.", () -> {
                dismiss();
                if (listener != null) listener.onOpenAiDashboard();
            });
        } else if (cmd.contains("game") || cmd.contains("play") || cmd.contains("orbital")) {
            speakResponse("Executing protocol: Orbital Defender arcade launch.", () -> {
                dismiss();
                if (listener != null) listener.onOpenGame();
            });
        } else if (cmd.contains("dns") || cmd.contains("adguard") || cmd.contains("adblock") || cmd.contains("ad")) {
            speakResponse("Accessing DNS shield settings and Private DNS configuration.", () -> {
                dismiss();
                if (listener != null) listener.onOpenAdGuardDns();
            });
        } else if (cmd.contains("lock") || cmd.contains("protect") || cmd.contains("secure")) {
            speakResponse("Locking workstation. Terminal PIN required to resume.", () -> {
                dismiss();
                if (listener != null) listener.onLockTv();
            });
        } else if (cmd.contains("setting") || cmd.contains("control") || cmd.contains("device")) {
            speakResponse("Opening Aether TV Control Center.", () -> {
                dismiss();
                if (listener != null) listener.onOpenSettings();
            });
        } else {
            speakResponse("Recognized command: \"" + rawCommand + "\". Command mapped to system standby.", null);
        }
    }

    private void speakResponse(String text, Runnable afterSpeech) {
        jarvisView.setState(JarvisView.State.SPEAKING);
        jarvisStateText.setText("SPEAKING // SYNTHESIZING RESPONSE");
        jarvisResponseText.setText(text);

        handler.postDelayed(() -> {
            jarvisView.setState(JarvisView.State.IDLE);
            jarvisStateText.setText("READY // STANDBY");
            if (afterSpeech != null) {
                afterSpeech.run();
            }
        }, 1200);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopListening();
    }
}
