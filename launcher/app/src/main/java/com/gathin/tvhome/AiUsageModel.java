package com.gathin.tvhome;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Data model representing AI Provider quota and usage statistics.
 */
public final class AiUsageModel {

    public final String providerId;
    public final String name;
    public final String primaryModel;
    public final String usagePeriod;
    public final long tokensConsumed;
    public final long tokenLimit;
    public final int requestsMade;
    public final double spentUsd;
    public final double billingLimitUsd;
    public final String rateLimitInfo;
    public final long lastSyncTimestamp;
    public final String status; // ONLINE, CACHED, DEMO

    public AiUsageModel(String providerId, String name, String primaryModel, String usagePeriod,
                        long tokensConsumed, long tokenLimit, int requestsMade,
                        double spentUsd, double billingLimitUsd, String rateLimitInfo,
                        long lastSyncTimestamp, String status) {
        this.providerId = providerId;
        this.name = name;
        this.primaryModel = primaryModel;
        this.usagePeriod = usagePeriod;
        this.tokensConsumed = tokensConsumed;
        this.tokenLimit = tokenLimit;
        this.requestsMade = requestsMade;
        this.spentUsd = spentUsd;
        this.billingLimitUsd = billingLimitUsd;
        this.rateLimitInfo = rateLimitInfo;
        this.lastSyncTimestamp = lastSyncTimestamp;
        this.status = status;
    }

    public int getUsagePercentage() {
        if (tokenLimit <= 0) {
            return 0;
        }
        return (int) Math.min(100, Math.max(0, (tokensConsumed * 100) / tokenLimit));
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("providerId", providerId);
        obj.put("name", name);
        obj.put("primaryModel", primaryModel);
        obj.put("usagePeriod", usagePeriod);
        obj.put("tokensConsumed", tokensConsumed);
        obj.put("tokenLimit", tokenLimit);
        obj.put("requestsMade", requestsMade);
        obj.put("spentUsd", spentUsd);
        obj.put("billingLimitUsd", billingLimitUsd);
        obj.put("rateLimitInfo", rateLimitInfo);
        obj.put("lastSyncTimestamp", lastSyncTimestamp);
        obj.put("status", status);
        return obj;
    }

    public static AiUsageModel fromJson(JSONObject obj) {
        return new AiUsageModel(
            obj.optString("providerId", ""),
            obj.optString("name", "Unknown"),
            obj.optString("primaryModel", "N/A"),
            obj.optString("usagePeriod", "Current Month"),
            obj.optLong("tokensConsumed", 0),
            obj.optLong("tokenLimit", 1000000),
            obj.optInt("requestsMade", 0),
            obj.optDouble("spentUsd", 0.0),
            obj.optDouble("billingLimitUsd", 0.0),
            obj.optString("rateLimitInfo", "N/A"),
            obj.optLong("lastSyncTimestamp", System.currentTimeMillis()),
            obj.optString("status", "CACHED")
        );
    }
}
