package com.gathin.tvhome;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Robust Wi-Fi and network state helper for Android TV (API 26+ / Android 9 Pie).
 * Handles safe SSID normalization, location permission checks, and graceful fallbacks.
 */
public final class WifiHelper {

    public static final String UNKNOWN_SSID = "<unknown ssid>";
    public static final String DEFAULT_FALLBACK_SSID = "Wi-Fi Connected";

    private WifiHelper() {}

    /**
     * Cleans and normalizes raw SSID strings returned by Android WifiInfo.
     * Rejects placeholder, null, empty, hex, and '<unknown ssid>' values.
     *
     * @param rawSsid Raw SSID string from WifiInfo.getSSID()
     * @return Clean normalized SSID, or null if invalid/masked.
     */
    public static String normalizeSsid(String rawSsid) {
        if (rawSsid == null) return null;
        String cleaned = rawSsid.trim();
        // Remove surrounding quotes if present
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        if (cleaned.isEmpty()) return null;
        if (cleaned.equalsIgnoreCase(UNKNOWN_SSID)) return null;
        if (cleaned.equalsIgnoreCase("unknown ssid")) return null;
        if (cleaned.equalsIgnoreCase("0x")) return null;
        if (cleaned.equalsIgnoreCase("<unknown>")) return null;
        return cleaned;
    }

    /**
     * Truncates long SSIDs for display in top-bar pills.
     */
    public static String truncateSsid(String ssid, int maxChars) {
        if (ssid == null) return "";
        if (ssid.length() <= maxChars) return ssid;
        return ssid.substring(0, Math.max(1, maxChars - 1)) + "…";
    }

    /**
     * Resolves the active Wi-Fi SSID with permission awareness and safe fallbacks.
     * Never returns literal '<unknown ssid>'.
     */
    public static String resolveSsid(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null || !wm.isWifiEnabled()) return null;

            WifiInfo info = wm.getConnectionInfo();
            if (info != null && info.getNetworkId() != -1) {
                String normalized = normalizeSsid(info.getSSID());
                if (normalized != null) return normalized;
                // If connected but SSID is hidden due to missing permission, return fallback
                return DEFAULT_FALLBACK_SSID;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Checks if fine location permission is currently granted.
     */
    public static boolean hasLocationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Converts integer IP address to human-readable IPv4 string.
     */
    public static String formatIpAddress(int ip) {
        if (ip == 0) return "Auto (DHCP)";
        try {
            byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ip).array();
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (Exception e) {
            return "Connected";
        }
    }

    /**
     * Data holder representing network state for UI display.
     */
    public static final class State {
        public final boolean isWifiEnabled;
        public final boolean isWifiConnected;
        public final boolean isEthernet;
        public final boolean isOnline;
        public final String ssid;
        public final int signalLevel; // 0 to 4
        public final int rssi;
        public final int linkSpeed;
        public final String ipAddress;

        public State(boolean isWifiEnabled, boolean isWifiConnected, boolean isEthernet,
                     boolean isOnline, String ssid, int signalLevel, int rssi, int linkSpeed, String ipAddress) {
            this.isWifiEnabled = isWifiEnabled;
            this.isWifiConnected = isWifiConnected;
            this.isEthernet = isEthernet;
            this.isOnline = isOnline;
            this.ssid = ssid;
            this.signalLevel = signalLevel;
            this.rssi = rssi;
            this.linkSpeed = linkSpeed;
            this.ipAddress = ipAddress;
        }
    }

    /**
     * Inspects full network status using ConnectivityManager and WifiManager.
     */
    public static State queryNetworkState(Context context) {
        boolean wifiEnabled = false;
        boolean wifiConnected = false;
        boolean ethernetConnected = false;
        boolean online = false;
        String ssid = null;
        int signalLevel = 0;
        int rssi = -100;
        int linkSpeed = 0;
        String ipAddress = "Disconnected";

        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                for (Network net : cm.getAllNetworks()) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(net);
                    if (caps == null) continue;
                    if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        online = true;
                    }
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        ethernetConnected = true;
                    }
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        wifiConnected = true;
                    }
                }
            }

            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                wifiEnabled = wm.isWifiEnabled();
                if (wifiEnabled) {
                    WifiInfo info = wm.getConnectionInfo();
                    if (info != null && info.getNetworkId() != -1) {
                        wifiConnected = true;
                        String clean = normalizeSsid(info.getSSID());
                        ssid = clean != null ? clean : DEFAULT_FALLBACK_SSID;
                        rssi = info.getRssi();
                        signalLevel = Math.max(0, Math.min(4, WifiManager.calculateSignalLevel(rssi, 5)));
                        linkSpeed = info.getLinkSpeed();
                        ipAddress = formatIpAddress(info.getIpAddress());
                    }
                }
            }
            if (wifiConnected && ssid == null) ssid = DEFAULT_FALLBACK_SSID;
        } catch (Exception ignored) {}

        return new State(wifiEnabled, wifiConnected, ethernetConnected, online,
                ssid, signalLevel, rssi, linkSpeed, ipAddress);
    }
}
