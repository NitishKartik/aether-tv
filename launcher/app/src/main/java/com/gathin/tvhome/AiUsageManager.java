package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Manager for AI Usage, Limits, and Provider Credential Tracking.
 * Stores cached usage snapshots locally for offline access, supports user-entered
 * API credentials with masking and safe encrypted/private persistence.
 */
public final class AiUsageManager {

    private static final String PREFS_NAME = "aether_ai_usage_prefs";
    private static final String KEY_SNAPSHOT = "cached_usage_snapshot";
    private static final String KEY_API_KEY_PREFIX = "api_key_";

    public interface SyncCallback {
        void onSyncComplete(List<AiUsageModel> updatedModels);
    }

    private final SharedPreferences prefs;

    public AiUsageManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<AiUsageModel> getUsageModels() {
        String cachedJson = prefs.getString(KEY_SNAPSHOT, null);
        if (cachedJson != null && !cachedJson.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(cachedJson);
                List<AiUsageModel> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    list.add(AiUsageModel.fromJson(arr.getJSONObject(i)));
                }
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (Exception ignored) {
            }
        }
        return getDefaultProviders();
    }

    public void saveUsageModels(List<AiUsageModel> models) {
        try {
            JSONArray arr = new JSONArray();
            for (AiUsageModel m : models) {
                arr.put(m.toJson());
            }
            prefs.edit().putString(KEY_SNAPSHOT, arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public void setApiKey(String providerId, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            prefs.edit().remove(KEY_API_KEY_PREFIX + providerId).apply();
        } else {
            prefs.edit().putString(KEY_API_KEY_PREFIX + providerId, apiKey.trim()).apply();
        }
    }

    public String getMaskedApiKey(String providerId) {
        String key = prefs.getString(KEY_API_KEY_PREFIX + providerId, null);
        if (key == null || key.isEmpty()) {
            return "Not Set (Using Sample Limits)";
        }
        if (key.length() <= 8) {
            return "••••••••";
        }
        return key.substring(0, 4) + "••••••••" + key.substring(key.length() - 4);
    }

    public boolean hasCustomApiKey(String providerId) {
        return prefs.contains(KEY_API_KEY_PREFIX + providerId);
    }

    public List<AiUsageModel> getDefaultProviders() {
        List<AiUsageModel> list = new ArrayList<>();
        long now = System.currentTimeMillis();

        list.add(new AiUsageModel(
            "openai", "OpenAI", "GPT-4o / o1", "October 2026",
            342150, 1000000, 1284, 6.84, 50.00, "10,000 TPM · Tier 3", now, "SAMPLE DATA"
        ));

        list.add(new AiUsageModel(
            "anthropic", "Anthropic", "Claude 3.5 Sonnet", "October 2026",
            184200, 500000, 642, 3.71, 25.00, "4,000 RPM · Tier 2", now, "SAMPLE DATA"
        ));

        list.add(new AiUsageModel(
            "gemini", "Google Gemini", "Gemini 1.5 Pro", "October 2026",
            512000, 2000000, 2150, 0.00, 100.00, "360 RPM · PayG", now, "SAMPLE DATA"
        ));

        list.add(new AiUsageModel(
            "copilot", "GitHub Copilot", "Copilot Models", "Active Plan",
            8920, 50000, 1420, 10.00, 10.00, "Unlimited Chat · Individual", now, "SAMPLE DATA"
        ));

        list.add(new AiUsageModel(
            "openrouter", "OpenRouter", "Unified Multi-Model", "October 2026",
            94500, 250000, 312, 1.89, 15.00, "200 Req/min · Balance $13.11", now, "SAMPLE DATA"
        ));

        list.add(new AiUsageModel(
            "huggingface", "Hugging Face", "Inference Endpoints", "October 2026",
            42000, 100000, 185, 0.00, 9.00, "PRO Tier · Warm Endpoints", now, "SAMPLE DATA"
        ));

        return list;
    }

    public void syncAllProviders(SyncCallback callback) {
        new Thread(() -> {
            List<AiUsageModel> current = getUsageModels();
            List<AiUsageModel> refreshed = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (AiUsageModel m : current) {
                String key = prefs.getString(KEY_API_KEY_PREFIX + m.providerId, null);
                if (key == null || key.trim().isEmpty()) {
                    refreshed.add(new AiUsageModel(
                        m.providerId, m.name, m.primaryModel, m.usagePeriod,
                        m.tokensConsumed, m.tokenLimit, m.requestsMade,
                        m.spentUsd, m.billingLimitUsd, m.rateLimitInfo,
                        now, "SAMPLE DATA"
                    ));
                    continue;
                }

                // If OpenRouter key is provided, query official credits endpoint
                if ("openrouter".equalsIgnoreCase(m.providerId)) {
                    double[] credits = fetchOpenRouterCredits(key.trim());
                    if (credits != null) {
                        double totalUsage = credits[0];
                        double totalCredits = credits[1];
                        refreshed.add(new AiUsageModel(
                            m.providerId, m.name, m.primaryModel, m.usagePeriod,
                            m.tokensConsumed, m.tokenLimit, m.requestsMade,
                            totalUsage, totalCredits, "Live Balance: $" + String.format(java.util.Locale.US, "%.2f", totalCredits - totalUsage),
                            now, "ONLINE (SYNCED)"
                        ));
                        continue;
                    }
                }

                // For other providers with configured keys or when offline
                refreshed.add(new AiUsageModel(
                    m.providerId, m.name, m.primaryModel, m.usagePeriod,
                    m.tokensConsumed, m.tokenLimit, m.requestsMade,
                    m.spentUsd, m.billingLimitUsd, m.rateLimitInfo,
                    now, "CONFIGURED (SAVED KEY)"
                ));
            }

            saveUsageModels(refreshed);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (callback != null) {
                    callback.onSyncComplete(refreshed);
                }
            });
        }).start();
    }

    private double[] fetchOpenRouterCredits(String apiKey) {
        try {
            java.net.URL url = new java.net.URL("https://openrouter.ai/api/v1/credits");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("User-Agent", "AetherTV/1.0");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);

            if (conn.getResponseCode() == 200) {
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) sb.append(line);
                in.close();

                JSONObject root = new JSONObject(sb.toString());
                JSONObject data = root.optJSONObject("data");
                if (data != null) {
                    double usage = data.optDouble("total_usage", 0.0);
                    double credits = data.optDouble("total_credits", 0.0);
                    return new double[]{usage, credits};
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
