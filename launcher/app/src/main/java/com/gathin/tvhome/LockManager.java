package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Aether TV macOS-Style Lock Screen Security Manager.
 * Provides cryptographically secure PIN/passphrase authentication with:
 * - PBKDF2WithHmacSHA256 (10,000 rounds) or salted SHA-256 fallback
 * - Cryptographically random 16-byte salt per credential
 * - Exponential lockout after 5 consecutive failed attempts
 * - Inactivity relock timeouts
 * - Constant-time hash verification to mitigate timing attacks
 * - Zero plaintext password persistence
 */
public final class LockManager {

    private static final String PREFS_NAME = "aether_lock_security";
    private static final String KEY_ENABLED = "lock_enabled";
    private static final String KEY_SALT = "lock_salt";
    private static final String KEY_HASH = "lock_hash";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKOUT_UNTIL = "lockout_until_ms";
    private static final String KEY_AUTO_RELOCK_MINUTES = "auto_relock_minutes";
    private static final String KEY_LAST_UNLOCKED_TIME = "last_unlocked_time";
    private static final String KEY_IS_LOCKED = "is_currently_locked";

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final int KEY_LENGTH_BITS = 256;

    private final Context context;
    private final SharedPreferences prefs;
    private final SecureRandom secureRandom = new SecureRandom();

    public LockManager(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.prefs = context != null
                ? context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                : null;
        if (prefs != null && !prefs.contains(KEY_HASH)) {
            setPassword("1234");
        }
    }

    public boolean isLockEnabled() {
        if (prefs == null) return false;
        return prefs.getBoolean(KEY_ENABLED, false) && prefs.contains(KEY_HASH);
    }

    public boolean isCurrentlyLocked() {
        if (!isLockEnabled()) {
            return false;
        }
        if (prefs.getBoolean(KEY_IS_LOCKED, true)) {
            return true;
        }
        // Check auto-relock timeout
        int timeoutMinutes = getAutoRelockMinutes();
        if (timeoutMinutes > 0) {
            long lastUnlocked = prefs.getLong(KEY_LAST_UNLOCKED_TIME, 0);
            long now = System.currentTimeMillis();
            if (now - lastUnlocked > timeoutMinutes * 60L * 1000L) {
                // If media (music/video in SmartTube, YouTube, etc.) is active, defer auto-lock
                if (isMediaPlaying()) {
                    keepAlive();
                    return false;
                }
                lock();
                return true;
            }
        }
        return false;
    }

    public boolean isMediaPlaying() {
        if (context == null) return false;
        try {
            android.media.AudioManager am = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            return am != null && am.isMusicActive();
        } catch (Exception ignored) {
            return false;
        }
    }

    public void keepAlive() {
        if (prefs != null) {
            prefs.edit().putLong(KEY_LAST_UNLOCKED_TIME, System.currentTimeMillis()).apply();
        }
    }

    public void lock() {
        if (isLockEnabled()) {
            prefs.edit().putBoolean(KEY_IS_LOCKED, true).apply();
        }
    }

    public void unlock() {
        prefs.edit()
            .putBoolean(KEY_IS_LOCKED, false)
            .putLong(KEY_LAST_UNLOCKED_TIME, System.currentTimeMillis())
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0)
            .apply();
    }

    public int getAutoRelockMinutes() {
        return prefs.getInt(KEY_AUTO_RELOCK_MINUTES, 15); // default 15 min
    }

    public void setAutoRelockMinutes(int minutes) {
        prefs.edit().putInt(KEY_AUTO_RELOCK_MINUTES, minutes).apply();
    }

    public boolean isLockedOut() {
        long lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0);
        return System.currentTimeMillis() < lockoutUntil;
    }

    public long getRemainingLockoutSeconds() {
        long lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0);
        long diff = lockoutUntil - System.currentTimeMillis();
        return diff > 0 ? (diff / 1000) : 0;
    }

    public int getRemainingAttempts() {
        int failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0);
        return Math.max(0, MAX_FAILED_ATTEMPTS - failed);
    }

    /**
     * Sets a new PIN or passphrase, generating a fresh random salt and secure hash.
     */
    public boolean setPassword(String password) {
        if (password == null || password.trim().length() < 4) {
            return false;
        }
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        byte[] hash = computeHash(password.trim(), salt);
        if (hash == null) {
            return false;
        }

        prefs.edit()
            .putBoolean(KEY_ENABLED, true)
            .putString(KEY_SALT, bytesToHex(salt))
            .putString(KEY_HASH, bytesToHex(hash))
            .putBoolean(KEY_IS_LOCKED, false)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0)
            .apply();
        return true;
    }

    /**
     * Verifies the provided password against the stored salt and hash in constant time.
     */
    public boolean verifyPassword(String password) {
        if (!isLockEnabled()) {
            return true;
        }
        if (isLockedOut()) {
            return false;
        }
        if (password == null) {
            recordFailedAttempt();
            return false;
        }

        String storedSaltHex = prefs.getString(KEY_SALT, "");
        String storedHashHex = prefs.getString(KEY_HASH, "");
        if (storedSaltHex.isEmpty() || storedHashHex.isEmpty()) {
            return false;
        }

        byte[] salt = hexToBytes(storedSaltHex);
        byte[] expectedHash = hexToBytes(storedHashHex);
        byte[] computedHash = computeHash(password.trim(), salt);

        if (computedHash == null) {
            recordFailedAttempt();
            return false;
        }

        if (constantTimeEquals(computedHash, expectedHash)) {
            unlock();
            return true;
        } else {
            recordFailedAttempt();
            return false;
        }
    }

    public void clearPassword() {
        prefs.edit().clear().apply();
    }

    private void recordFailedAttempt() {
        int failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_FAILED_ATTEMPTS, failed);

        if (failed >= MAX_FAILED_ATTEMPTS) {
            // Exponential backoff: 30s, 60s, 120s...
            int overAttempts = failed - MAX_FAILED_ATTEMPTS;
            long lockoutMs = 30000L * (1L << Math.min(overAttempts, 4));
            editor.putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + lockoutMs);
        }
        editor.apply();
    }

    static byte[] computeHash(String password, byte[] salt) {
        try {
            // Prefer PBKDF2WithHmacSHA256
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            // Fallback to SHA-256 with salt
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(salt);
                return md.digest(password.getBytes("UTF-8"));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}
