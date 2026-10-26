package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Aether TV Audio Engine.
 * Manages low-latency audio effects for the macOS-style lock screen,
 * system transitions, Jarvis AI, and arcade entertainment.
 */
public final class SoundManager {

    private static final String TAG = "AetherSoundManager";
    private static final String PREFS_NAME = "aether_sound_prefs";
    private static final String KEY_SOUNDS_ENABLED = "ui_sounds_enabled";

    private static volatile SoundManager instance;

    private final Context context;
    private final SharedPreferences prefs;
    private SoundPool soundPool;

    private int soundLock = -1;
    private int soundUnlock = -1;
    private int soundKeyTap = -1;
    private int soundError = -1;
    private int soundJarvis = -1;
    private int soundDialogOpen = -1;
    private int soundSelect = -1;
    private int soundGameDrift = -1;
    private int soundGameCrash = -1;
    private int soundGameCoin = -1;

    private final Map<Integer, Boolean> loadedSounds = new HashMap<>();

    public static SoundManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SoundManager.class) {
                if (instance == null) {
                    instance = new SoundManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private SoundManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        initSoundPool();
    }

    private void initSoundPool() {
        try {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(8)
                    .setAudioAttributes(audioAttributes)
                    .build();

            soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
                if (status == 0) {
                    loadedSounds.put(sampleId, true);
                }
            });

            soundLock = soundPool.load(context, R.raw.snd_lock, 1);
            soundUnlock = soundPool.load(context, R.raw.snd_unlock, 1);
            soundKeyTap = soundPool.load(context, R.raw.snd_key_tap, 1);
            soundError = soundPool.load(context, R.raw.snd_error, 1);
            soundJarvis = soundPool.load(context, R.raw.snd_jarvis, 1);
            soundDialogOpen = soundPool.load(context, R.raw.snd_dialog_open, 1);
            soundSelect = soundPool.load(context, R.raw.snd_select, 1);
            soundGameDrift = soundPool.load(context, R.raw.snd_game_drift, 1);
            soundGameCrash = soundPool.load(context, R.raw.snd_game_crash, 1);
            soundGameCoin = soundPool.load(context, R.raw.snd_game_coin, 1);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SoundPool", e);
        }
    }

    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUNDS_ENABLED, true);
    }

    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUNDS_ENABLED, enabled).apply();
    }

    private void play(int soundId, float volume, float rate) {
        if (!isSoundEnabled() || soundPool == null || soundId == -1) return;
        try {
            soundPool.play(soundId, volume, volume, 1, 0, rate);
        } catch (Exception e) {
            Log.w(TAG, "Failed to play sound: " + soundId, e);
        }
    }

    public void playLock() {
        play(soundLock, 0.95f, 1.0f);
    }

    public void playUnlock() {
        play(soundUnlock, 0.95f, 1.0f);
    }

    public void playKeyTap() {
        play(soundKeyTap, 0.55f, 1.0f);
    }

    public void playError() {
        play(soundError, 0.85f, 1.0f);
    }

    public void playJarvis() {
        play(soundJarvis, 0.90f, 1.0f);
    }

    public void playDialogOpen() {
        play(soundDialogOpen, 0.60f, 1.0f);
    }

    public void playSelect() {
        play(soundSelect, 0.70f, 1.0f);
    }

    public void playGameDrift() {
        play(soundGameDrift, 0.65f, 1.0f);
    }

    public void playGameCrash() {
        play(soundGameCrash, 0.95f, 1.0f);
    }

    public void playGameCoin() {
        play(soundGameCoin, 0.85f, 1.0f);
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
