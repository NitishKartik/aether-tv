package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Monitors background audio/video playback (SmartTube, YouTube, Spotify, etc.)
 * and provides real-time state for the Home Screen & Lock Screen Now Playing bars.
 */
public final class NowPlayingManager {

    private static final String TAG = "NowPlayingManager";
    private static volatile NowPlayingManager instance;

    public static final class MediaInfo {
        public final boolean isPlaying;
        public final String title;
        public final String artist;
        public final String packageName;
        public final String appName;

        public MediaInfo(boolean isPlaying, String title, String artist, String packageName, String appName) {
            this.isPlaying = isPlaying;
            this.title = title != null ? title : "";
            this.artist = artist != null ? artist : "";
            this.packageName = packageName != null ? packageName : "";
            this.appName = appName != null ? appName : "";
        }
    }

    public interface OnMediaStateListener {
        void onMediaStateChanged(MediaInfo info);
    }

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<OnMediaStateListener> listeners = new ArrayList<>();
    private MediaInfo currentInfo = new MediaInfo(false, "", "", "", "");
    private boolean isPolling = false;

    public static NowPlayingManager getInstance(Context context) {
        if (instance == null) {
            synchronized (NowPlayingManager.class) {
                if (instance == null) {
                    instance = new NowPlayingManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private NowPlayingManager(Context context) {
        this.context = context;
        startMonitoring();
    }

    public void addListener(OnMediaStateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            listener.onMediaStateChanged(currentInfo);
        }
    }

    public void removeListener(OnMediaStateListener listener) {
        listeners.remove(listener);
    }

    public MediaInfo getCurrentInfo() {
        return currentInfo;
    }

    public void startMonitoring() {
        if (isPolling) return;
        isPolling = true;
        pollRunnable.run();
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            refreshMediaState();
            handler.postDelayed(this, 1500);
        }
    };

    public void refreshMediaState() {
        try {
            MediaSessionManager msm = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            boolean isMusicActive = am != null && am.isMusicActive();

            ComponentName listenerComponent = new ComponentName(context, GuardRevival.class);
            List<MediaController> controllers = null;
            try {
                if (msm != null) {
                    controllers = msm.getActiveSessions(listenerComponent);
                }
            } catch (SecurityException ignored) {
            }

            if (controllers != null && !controllers.isEmpty()) {
                MediaController active = null;
                for (MediaController mc : controllers) {
                    PlaybackState state = mc.getPlaybackState();
                    if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                        active = mc;
                        break;
                    }
                }
                if (active == null && isMusicActive) {
                    active = controllers.get(0);
                }

                if (active != null) {
                    MediaMetadata meta = active.getMetadata();
                    PlaybackState state = active.getPlaybackState();
                    boolean playing = (state != null && state.getState() == PlaybackState.STATE_PLAYING) || isMusicActive;
                    String title = "";
                    String artist = "";
                    if (meta != null) {
                        title = meta.getString(MediaMetadata.METADATA_KEY_TITLE);
                        artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST);
                        if (title == null || title.isEmpty()) {
                            CharSequence desc = meta.getDescription() != null ? meta.getDescription().getTitle() : null;
                            if (desc != null) title = desc.toString();
                        }
                    }
                    String pkg = active.getPackageName();
                    String appName = getAppLabel(pkg);

                    if (title == null || title.isEmpty()) {
                        title = "Playing in " + appName;
                    }

                    MediaInfo newInfo = new MediaInfo(playing, title, artist, pkg, appName);
                    updateState(newInfo);
                    return;
                }
            }

            if (isMusicActive) {
                MediaInfo fallback = new MediaInfo(true, "Background Audio Active", "Audio Stream", "", "Media Player");
                updateState(fallback);
                return;
            }

            if (currentInfo.isPlaying) {
                updateState(new MediaInfo(false, "", "", "", ""));
            }
        } catch (Exception e) {
            Log.w(TAG, "Error refreshing media state", e);
        }
    }

    private void updateState(MediaInfo newInfo) {
        this.currentInfo = newInfo;
        for (OnMediaStateListener l : listeners) {
            l.onMediaStateChanged(newInfo);
        }
    }

    private String getAppLabel(String packageName) {
        if (packageName == null || packageName.isEmpty()) return "Media";
        if (packageName.contains("smarttube")) return "SmartTube";
        if (packageName.contains("youtube")) return "YouTube";
        if (packageName.contains("spotify")) return "Spotify";
        if (packageName.contains("netflix")) return "Netflix";
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (Exception ignored) {
            return "Media";
        }
    }

    public void openCurrentPlayer(Context targetContext) {
        if (currentInfo == null || currentInfo.packageName.isEmpty()) return;
        try {
            PackageManager pm = targetContext.getPackageManager();
            Intent intent = pm.getLeanbackLaunchIntentForPackage(currentInfo.packageName);
            if (intent == null) {
                intent = pm.getLaunchIntentForPackage(currentInfo.packageName);
            }
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                targetContext.startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch media player: " + currentInfo.packageName, e);
        }
    }
}
