package com.gathin.tvhome;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Dialog interface for configuring AdGuard Private DNS on Xiaomi Mi Box 4.
 */
public final class AdGuardDnsDialog extends Dialog {

    private TextView txtDnsStatus;
    private Button btnCopyDns;
    private Button btnOpenNetwork;
    private Button btnTestDns;
    private Button btnClose;

    public AdGuardDnsDialog(Context context) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_adguard_dns);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        txtDnsStatus = findViewById(R.id.txtDnsStatus);
        btnCopyDns = findViewById(R.id.btnCopyDns);
        btnOpenNetwork = findViewById(R.id.btnOpenNetworkSettings);
        btnTestDns = findViewById(R.id.btnTestDns);
        btnClose = findViewById(R.id.btnCloseDns);

        btnCopyDns.setOnClickListener(v -> {
            AdGuardDnsHelper.copyHostnameToClipboard(getContext());
            Toast.makeText(getContext(), "Copied 'dns.adguard.com' to clipboard", Toast.LENGTH_SHORT).show();
            txtDnsStatus.setText("✓ 'dns.adguard.com' copied to clipboard!");
            txtDnsStatus.setTextColor(Color.parseColor("#FF00E676"));
        });

        btnOpenNetwork.setOnClickListener(v -> {
            boolean opened = AdGuardDnsHelper.openPrivateDnsSettings(getContext());
            if (!opened) {
                Toast.makeText(getContext(), "Unable to launch network settings activity", Toast.LENGTH_SHORT).show();
            }
        });

        btnTestDns.setOnClickListener(v -> {
            btnTestDns.setEnabled(false);
            btnTestDns.setText("Testing DNS...");
            txtDnsStatus.setText("Checking DNS resolution...");
            txtDnsStatus.setTextColor(Color.parseColor("#FF00E5FF"));

            AdGuardDnsHelper.verifyDnsFiltering((isResolving, details) -> {
                btnTestDns.setEnabled(true);
                btnTestDns.setText("🔍 Run DNS Verification Test");
                txtDnsStatus.setText(details);
                txtDnsStatus.setTextColor(isResolving ? Color.parseColor("#FF00E676") : Color.parseColor("#FFFF5252"));
            });
        });

        btnClose.setOnClickListener(v -> dismiss());
    }
}
