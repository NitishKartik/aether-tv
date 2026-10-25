package com.gathin.tvhome;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Aether TV Glass AI Usage and Quota Dashboard.
 * Displays live and cached usage metrics across OpenAI, Anthropic, Gemini, Copilot,
 * OpenRouter, and Hugging Face.
 */
public final class AiDashboardDialog extends Dialog {

    private final AiUsageManager aiUsageManager;
    private RecyclerView providersRecycler;
    private TextView lastSyncText;
    private Button btnSync;
    private Button btnClose;
    private ProviderAdapter adapter;
    private List<AiUsageModel> models;

    public AiDashboardDialog(Context context, AiUsageManager aiUsageManager) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.aiUsageManager = aiUsageManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_ai_dashboard);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        providersRecycler = findViewById(R.id.providersRecycler);
        lastSyncText = findViewById(R.id.lastSyncText);
        btnSync = findViewById(R.id.btnSync);
        btnClose = findViewById(R.id.btnCloseDashboard);

        providersRecycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        models = aiUsageManager.getUsageModels();
        adapter = new ProviderAdapter();
        providersRecycler.setAdapter(adapter);

        updateLastSyncLabel();

        btnSync.setOnClickListener(v -> {
            btnSync.setEnabled(false);
            btnSync.setText("Syncing...");
            aiUsageManager.syncAllProviders(updated -> {
                models = updated;
                adapter.notifyDataSetChanged();
                btnSync.setEnabled(true);
                btnSync.setText("Sync Metrics");
                updateLastSyncLabel();
            });
        });

        btnClose.setOnClickListener(v -> dismiss());
    }

    private void updateLastSyncLabel() {
        if (!models.isEmpty()) {
            long ts = models.get(0).lastSyncTimestamp;
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault());
            lastSyncText.setText("Last Synced: " + sdf.format(new Date(ts)) + " · Offline Cache Ready");
        }
    }

    private class ProviderAdapter extends RecyclerView.Adapter<ProviderViewHolder> {

        @NonNull
        @Override
        public ProviderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_provider, parent, false);
            return new ProviderViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ProviderViewHolder holder, int position) {
            AiUsageModel model = models.get(position);
            holder.bind(model);
        }

        @Override
        public int getItemCount() {
            return models.size();
        }
    }

    private class ProviderViewHolder extends RecyclerView.ViewHolder {
        TextView providerName, providerStatus, providerModel, tokensText, spentText, rateLimitText, requestsText;
        ProgressBar tokenProgress;

        ProviderViewHolder(View itemView) {
            super(itemView);
            providerName = itemView.findViewById(R.id.providerName);
            providerStatus = itemView.findViewById(R.id.providerStatus);
            providerModel = itemView.findViewById(R.id.providerModel);
            tokensText = itemView.findViewById(R.id.tokensText);
            spentText = itemView.findViewById(R.id.spentText);
            rateLimitText = itemView.findViewById(R.id.rateLimitText);
            requestsText = itemView.findViewById(R.id.requestsText);
            tokenProgress = itemView.findViewById(R.id.tokenProgress);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    showKeyConfigDialog(models.get(pos));
                }
            });
        }

        void bind(AiUsageModel m) {
            providerName.setText(m.name);
            providerStatus.setText(m.status);
            if (m.status != null && m.status.contains("ONLINE")) {
                providerStatus.setTextColor(android.graphics.Color.parseColor("#FF00E676")); // Green
            } else if (m.status != null && m.status.contains("SAMPLE")) {
                providerStatus.setTextColor(android.graphics.Color.parseColor("#FFFFB300")); // Amber
            } else {
                providerStatus.setTextColor(android.graphics.Color.parseColor("#FF00E5FF")); // Cyan
            }
            providerModel.setText(m.primaryModel + " · " + m.usagePeriod);

            int pct = m.getUsagePercentage();
            tokenProgress.setProgress(pct);

            tokensText.setText(formatTokens(m.tokensConsumed) + " / " + formatTokens(m.tokenLimit) + " (" + pct + "%)");
            spentText.setText(String.format(Locale.US, "$%.2f / $%.2f", m.spentUsd, m.billingLimitUsd));
            rateLimitText.setText(m.rateLimitInfo);
            requestsText.setText(String.format(Locale.US, "%,d requests", m.requestsMade));
        }

        private String formatTokens(long tokens) {
            if (tokens >= 1_000_000) {
                return String.format(Locale.US, "%.1fM", tokens / 1_000_000.0);
            } else if (tokens >= 1_000) {
                return (tokens / 1_000) + "K";
            }
            return String.valueOf(tokens);
        }
    }

    private void showKeyConfigDialog(AiUsageModel model) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle(model.name + " Configuration");

        String currentKey = aiUsageManager.getMaskedApiKey(model.providerId);
        final EditText input = new EditText(getContext());
        input.setHint("Enter " + model.name + " API Key");
        input.setText("");

        builder.setMessage("Active Key: " + currentKey + "\n\nEnter your official API key to fetch private account limits, or leave empty to use sample quotas.");
        builder.setView(input);

        builder.setPositiveButton("Save Key", (dialog, which) -> {
            String newKey = input.getText().toString().trim();
            aiUsageManager.setApiKey(model.providerId, newKey);
            aiUsageManager.syncAllProviders(updated -> {
                models = updated;
                adapter.notifyDataSetChanged();
                updateLastSyncLabel();
            });
        });

        builder.setNegativeButton("Remove Key", (dialog, which) -> {
            aiUsageManager.setApiKey(model.providerId, null);
            aiUsageManager.syncAllProviders(updated -> {
                models = updated;
                adapter.notifyDataSetChanged();
                updateLastSyncLabel();
            });
        });

        builder.setNeutralButton("Cancel", null);
        builder.show();
    }
}
