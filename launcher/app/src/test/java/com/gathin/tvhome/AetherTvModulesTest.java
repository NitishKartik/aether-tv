package com.gathin.tvhome;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test suite validating Aether TV core modules:
 * - Dynamic Day/Night Scene auto-scheduling
 * - AI Usage model calculation and JSON serialization
 * - Entertainment model and IMDb rating attributes
 * - AdGuard DNS hostname integrity
 */
public class AetherTvModulesTest {

    @Test
    public void autoSceneMatchesTimeOfDay() {
        assertEquals(SceneManager.Scene.NIGHT, SceneManager.getAutoScene(0));
        assertEquals(SceneManager.Scene.NIGHT, SceneManager.getAutoScene(3));
        assertEquals(SceneManager.Scene.NIGHT, SceneManager.getAutoScene(5));

        assertEquals(SceneManager.Scene.MORNING, SceneManager.getAutoScene(6));
        assertEquals(SceneManager.Scene.MORNING, SceneManager.getAutoScene(9));
        assertEquals(SceneManager.Scene.MORNING, SceneManager.getAutoScene(11));

        assertEquals(SceneManager.Scene.DAY, SceneManager.getAutoScene(12));
        assertEquals(SceneManager.Scene.DAY, SceneManager.getAutoScene(15));
        assertEquals(SceneManager.Scene.DAY, SceneManager.getAutoScene(17));

        assertEquals(SceneManager.Scene.EVENING, SceneManager.getAutoScene(18));
        assertEquals(SceneManager.Scene.EVENING, SceneManager.getAutoScene(20));
        assertEquals(SceneManager.Scene.EVENING, SceneManager.getAutoScene(21));

        assertEquals(SceneManager.Scene.NIGHT, SceneManager.getAutoScene(22));
        assertEquals(SceneManager.Scene.NIGHT, SceneManager.getAutoScene(23));
    }

    @Test
    public void aiUsagePercentageCalculation() {
        AiUsageModel normal = new AiUsageModel(
            "openai", "OpenAI", "GPT-4o", "October 2026",
            340000, 1000000, 500, 6.80, 50.0, "10k TPM", 123456L, "ONLINE"
        );
        assertEquals(34, normal.getUsagePercentage());

        AiUsageModel zeroLimit = new AiUsageModel(
            "test", "Test", "M1", "Oct", 100, 0, 1, 0, 0, "", 0L, "ONLINE"
        );
        assertEquals(0, zeroLimit.getUsagePercentage());

        AiUsageModel overLimit = new AiUsageModel(
            "test", "Test", "M1", "Oct", 200, 100, 1, 0, 0, "", 0L, "ONLINE"
        );
        assertEquals(100, overLimit.getUsagePercentage());
    }

    @Test
    public void aiUsageJsonRoundTrip() throws Exception {
        AiUsageModel original = new AiUsageModel(
            "anthropic", "Anthropic", "Claude 3.5 Sonnet", "October 2026",
            150000, 500000, 250, 3.0, 25.0, "4000 RPM", 999999L, "CACHED"
        );

        JSONObject json = original.toJson();
        assertNotNull(json);
        assertEquals("anthropic", json.getString("providerId"));
        assertEquals(150000L, json.getLong("tokensConsumed"));

        AiUsageModel restored = AiUsageModel.fromJson(json);
        assertEquals(original.providerId, restored.providerId);
        assertEquals(original.name, restored.name);
        assertEquals(original.tokensConsumed, restored.tokensConsumed);
        assertEquals(original.tokenLimit, restored.tokenLimit);
        assertEquals(original.status, restored.status);
    }

    @Test
    public void entertainmentModelSerialization() throws Exception {
        EntertainmentModel movie = new EntertainmentModel(
            "tt0816692", "Interstellar", 2014, 8.7, "Sci-Fi", "2h 49m",
            "Wormhole space travel to save humanity.", "Matthew McConaughey", false, false, true
        );

        JSONObject json = movie.toJson();
        assertNotNull(json);
        assertEquals("Interstellar", json.getString("title"));
        assertEquals(8.7, json.getDouble("imdbRating"), 0.01);
        assertTrue(json.getBoolean("isWatchlist"));

        EntertainmentModel restored = EntertainmentModel.fromJson(json);
        assertEquals(movie.id, restored.id);
        assertEquals(movie.title, restored.title);
        assertEquals(movie.imdbRating, restored.imdbRating, 0.01);
        assertTrue(restored.isWatchlist);
        assertFalse(restored.isHindi);
        assertFalse(restored.isTvShow);
    }

    @Test
    public void adGuardDnsHostnameIntegrity() {
        assertEquals("dns.adguard.com", AdGuardDnsHelper.ADGUARD_HOSTNAME);
    }

    @Test
    public void lockManagerCryptoVerification() {
        byte[] salt1 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        byte[] salt2 = new byte[]{16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1};

        byte[] hash1 = LockManager.computeHash("1234", salt1);
        byte[] hash1Repeat = LockManager.computeHash("1234", salt1);
        byte[] hash2 = LockManager.computeHash("1234", salt2);
        byte[] hashOtherPin = LockManager.computeHash("9999", salt1);

        assertNotNull(hash1);
        assertNotNull(hash1Repeat);
        assertNotNull(hash2);
        assertNotNull(hashOtherPin);

        // Deterministic on same salt & PIN
        assertTrue(LockManager.constantTimeEquals(hash1, hash1Repeat));

        // Different salt yields completely distinct hash (mitigates rainbow tables)
        assertFalse(LockManager.constantTimeEquals(hash1, hash2));

        // Different PIN yields distinct hash
        assertFalse(LockManager.constantTimeEquals(hash1, hashOtherPin));

        // Constant time equals rejects mismatched lengths safely
        assertFalse(LockManager.constantTimeEquals(hash1, new byte[]{1, 2, 3}));
        assertFalse(LockManager.constantTimeEquals(null, hash1));

        // Hex serialization roundtrip
        String hex = LockManager.bytesToHex(salt1);
        assertEquals(32, hex.length());
        byte[] restoredBytes = LockManager.hexToBytes(hex);
        assertTrue(LockManager.constantTimeEquals(salt1, restoredBytes));
    }

    @Test
    public void aiUsageHonestSampleDataStatus() {
        AiUsageModel sample = new AiUsageModel(
            "openai", "OpenAI", "GPT-4o", "October 2026",
            100, 1000, 5, 0.5, 10.0, "10k TPM", 123456L, "SAMPLE DATA"
        );
        assertEquals("SAMPLE DATA", sample.status);
        assertFalse(sample.status.contains("ONLINE"));
    }
}
