package com.gathin.tvhome;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import java.net.InetAddress;

/**
 * Helper for AdGuard Private DNS workflow on Xiaomi Mi Box 4 (Android 9+).
 * Handles clipboard copying of dns.adguard.com, opens system Private DNS settings,
 * and performs asynchronous DNS filtering verification tests.
 */
public final class AdGuardDnsHelper {

    public static final String ADGUARD_HOSTNAME = "dns.adguard.com";

    public interface DnsTestCallback {
        void onTestResult(boolean isResolving, String details);
    }

    public static void copyHostnameToClipboard(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("AdGuard DNS Hostname", ADGUARD_HOSTNAME);
            clipboard.setPrimaryClip(clip);
        }
    }

    public static boolean openPrivateDnsSettings(Context context) {
        // 1. Try Network Provider / Wireless Settings (Android 9/10+)
        Intent[] candidateIntents = new Intent[]{
            new Intent("android.settings.NETWORK_PROVIDER_SETTINGS"),
            new Intent(Settings.ACTION_WIRELESS_SETTINGS),
            new Intent(Settings.ACTION_WIFI_SETTINGS),
            new Intent(Settings.ACTION_SETTINGS)
        };

        for (Intent intent : candidateIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static void verifyDnsFiltering(DnsTestCallback callback) {
        new Thread(() -> {
            boolean generalInternetReachable = false;
            boolean adguardReachable = false;
            boolean adDomainBlocked = false;
            String details;

            // 1. Verify general internet/DNS connectivity first
            try {
                InetAddress generalDns = InetAddress.getByName("cloudflare.com");
                generalInternetReachable = (generalDns != null);
            } catch (Exception ignored) {
            }

            // 2. Verify dns.adguard.com resolution
            try {
                InetAddress adguardAddr = InetAddress.getByName(ADGUARD_HOSTNAME);
                adguardReachable = (adguardAddr != null);
            } catch (Exception ignored) {
            }

            // 3. Test ad tracking domain
            if (generalInternetReachable || adguardReachable) {
                try {
                    InetAddress testAdAddr = InetAddress.getByName("adservice.google.com");
                    if (testAdAddr != null && "0.0.0.0".equals(testAdAddr.getHostAddress())) {
                        adDomainBlocked = true; // Sinkholed to 0.0.0.0
                    }
                } catch (Exception ignored) {
                    // When internet is reachable but ad domain resolution fails, DNS filtering is active!
                    adDomainBlocked = true;
                }
            }

            if (!generalInternetReachable && !adguardReachable) {
                details = "OFFLINE // No internet connectivity. Reconnect TV Wi-Fi or Ethernet to enable DNS shield.";
            } else if (adDomainBlocked) {
                details = "ACTIVE // AdGuard DNS is actively filtering ad networks (adservice.google.com sinkholed).";
            } else if (adguardReachable) {
                details = "STANDBY // dns.adguard.com is reachable, but system DNS is not filtering ads yet. Tap 'Open Private DNS Settings' to configure.";
            } else {
                details = "OFFLINE // Unable to resolve dns.adguard.com. Verify TV network settings.";
            }

            final String finalDetails = details;
            final boolean finalResult = adDomainBlocked;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (callback != null) {
                    callback.onTestResult(finalResult, finalDetails);
                }
            });
        }).start();
    }
}
