package com.gathin.tvhome;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Dedicated In-Launcher Wi-Fi Manager.
 */
public class WifiManagerDialog extends Dialog {

    private TextView currentSsid;
    private TextView ipAddressText;
    private TextView linkSpeedText;
    private TextView signalStrengthText;
    private TextView connectionBadge;
    private View activeNetworkCard;

    public WifiManagerDialog(@NonNull Context context) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        setContentView(R.layout.dialog_wifi_manager);

        currentSsid = findViewById(R.id.currentSsid);
        ipAddressText = findViewById(R.id.ipAddressText);
        linkSpeedText = findViewById(R.id.linkSpeedText);
        signalStrengthText = findViewById(R.id.signalStrengthText);
        connectionBadge = findViewById(R.id.connectionBadge);
        activeNetworkCard = findViewById(R.id.activeNetworkCard);

        View dialogBackdrop = findViewById(R.id.dialogBackdrop);
        if (dialogBackdrop != null) {
            dialogBackdrop.setOnClickListener(v -> dismiss());
        }

        View btnWifiSettings = findViewById(R.id.btnWifiSettings);
        if (btnWifiSettings != null) {
            btnWifiSettings.setOnClickListener(v -> {
                getContext().startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                dismiss();
            });
            btnWifiSettings.setOnFocusChangeListener((v, f) -> {
                v.animate().scaleX(f ? 1.08f : 1f).scaleY(f ? 1.08f : 1f).setDuration(160).start();
            });
        }

        if (activeNetworkCard != null) {
            activeNetworkCard.setOnClickListener(v -> {
                getContext().startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                dismiss();
            });
            activeNetworkCard.setOnFocusChangeListener((v, f) -> {
                v.animate().scaleX(f ? 1.04f : 1f).scaleY(f ? 1.04f : 1f).setDuration(160).start();
            });
        }

        loadWifiDetails();
    }

    private void loadWifiDetails() {
        try {
            WifiHelper.State state = WifiHelper.queryNetworkState(getContext());
            if (state.isWifiEnabled) {
                if (state.isWifiConnected && state.ssid != null) {
                    currentSsid.setText(state.ssid);
                    ipAddressText.setText("IP: " + state.ipAddress);
                    linkSpeedText.setText("Speed: " + (state.linkSpeed > 0 ? state.linkSpeed + " Mbps" : "N/A"));

                    String sig = state.signalLevel >= 4 ? "Excellent (5/5)"
                            : state.signalLevel >= 3 ? "Good (4/5)"
                            : state.signalLevel >= 2 ? "Fair (3/5)" : "Weak";
                    signalStrengthText.setText("Signal: " + sig + " (" + state.rssi + " dBm)");

                    connectionBadge.setText("Connected");
                    connectionBadge.setTextColor(0xFF00FF66);
                } else {
                    currentSsid.setText("Not Connected");
                    ipAddressText.setText("IP: Disconnected");
                    connectionBadge.setText("Disconnected");
                    connectionBadge.setTextColor(0xFFFF6666);
                }
            } else {
                currentSsid.setText("Wi-Fi Disabled");
                ipAddressText.setText("Wi-Fi is turned off");
                connectionBadge.setText("Off");
                connectionBadge.setTextColor(0xFFFF6666);
            }
        } catch (Exception e) {
            currentSsid.setText("Wi-Fi Connected");
        }

        if (activeNetworkCard != null) {
            activeNetworkCard.post(() -> activeNetworkCard.requestFocus());
        }
    }
}
