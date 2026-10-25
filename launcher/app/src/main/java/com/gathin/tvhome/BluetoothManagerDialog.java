package com.gathin.tvhome;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.Set;

/**
 * Dedicated In-Launcher Bluetooth Manager.
 */
public class BluetoothManagerDialog extends Dialog {

    private LinearLayout pairedContainer;
    private View noDevices;
    private final float density;

    public BluetoothManagerDialog(@NonNull Context context) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        density = context.getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        setContentView(R.layout.dialog_bluetooth_manager);

        pairedContainer = findViewById(R.id.pairedDevicesContainer);
        noDevices = findViewById(R.id.noBtDevices);

        View dialogBackdrop = findViewById(R.id.dialogBackdrop);
        if (dialogBackdrop != null) {
            dialogBackdrop.setOnClickListener(v -> dismiss());
        }

        View btnPairNew = findViewById(R.id.btnPairNew);
        if (btnPairNew != null) {
            btnPairNew.setOnClickListener(v -> {
                try {
                    Intent pairIntent = new Intent();
                    pairIntent.setComponent(new ComponentName("com.android.tv.settings", "com.android.tv.settings.accessories.AddAccessoryActivity"));
                    getContext().startActivity(pairIntent);
                    dismiss();
                } catch (Exception e) {
                    try {
                        getContext().startActivity(new Intent("android.settings.BLUETOOTH_SETTINGS"));
                        dismiss();
                    } catch (Exception ignored) {}
                }
            });
            btnPairNew.setOnFocusChangeListener((v, f) -> {
                v.animate().scaleX(f ? 1.08f : 1f).scaleY(f ? 1.08f : 1f).setDuration(160).start();
            });
        }

        loadDevices();
    }

    private void loadDevices() {
        pairedContainer.removeAllViews();
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null || !btAdapter.isEnabled()) {
            noDevices.setVisibility(View.VISIBLE);
            ((TextView) noDevices).setText("Bluetooth is turned off");
            return;
        }

        Set<BluetoothDevice> bonded = btAdapter.getBondedDevices();
        if (bonded == null || bonded.isEmpty()) {
            noDevices.setVisibility(View.VISIBLE);
            return;
        }

        noDevices.setVisibility(View.GONE);
        for (BluetoothDevice device : bonded) {
            View row = LayoutInflater.from(getContext()).inflate(R.layout.item_bluetooth_device, pairedContainer, false);
            TextView title = row.findViewById(R.id.title);
            TextView badge = row.findViewById(R.id.status);
            View dot = row.findViewById(R.id.dot);

            String devName = device.getName() != null ? device.getName() : device.getAddress();
            title.setText(devName);
            badge.setText(device.getType() == BluetoothDevice.DEVICE_TYPE_LE ? "BLE" : "Paired");
            badge.setVisibility(View.VISIBLE);
            if (dot != null) dot.setVisibility(View.VISIBLE);

            row.setOnClickListener(v -> {
                Toast.makeText(getContext(), devName + " selected", Toast.LENGTH_SHORT).show();
            });

            row.setOnFocusChangeListener((v, focused) -> {
                v.animate().scaleX(focused ? 1.04f : 1f).scaleY(focused ? 1.04f : 1f).setDuration(140).start();
            });

            pairedContainer.addView(row);
        }

        if (pairedContainer.getChildCount() > 0) {
            pairedContainer.post(() -> pairedContainer.getChildAt(0).requestFocus());
        }
    }
}
