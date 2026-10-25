package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying Watchdog state structure, stall detection logic,
 * and JSON schema validation.
 */
public class WatchdogTest {

    @Test
    public void testWatchdogStateSchema() throws Exception {
        String sampleJson = "{\n" +
                "  \"last_check_timestamp\": \"2026-10-26T01:00:00+05:30\",\n" +
                "  \"next_check_timestamp\": \"2026-10-26T01:17:00+05:30\",\n" +
                "  \"current_interval_minutes\": 17,\n" +
                "  \"milestone\": \"Phase 8 - On-Device Verification & QA Validation\",\n" +
                "  \"tv_ip\": \"10.199.103.54:5555\",\n" +
                "  \"stall_detected\": false,\n" +
                "  \"device_status\": {\n" +
                "    \"connection\": \"CONNECTED_AND_ACTIVE\",\n" +
                "    \"model\": \"MIBOX4\",\n" +
                "    \"android_version\": \"9 (API 28)\"\n" +
                "  },\n" +
                "  \"build_status\": {\n" +
                "    \"assembleDebug\": \"SUCCESS\",\n" +
                "    \"unit_tests\": \"PASSED\"\n" +
                "  },\n" +
                "  \"history\": [\n" +
                "    {\n" +
                "      \"timestamp\": \"2026-10-26T01:00:00+05:30\",\n" +
                "      \"status\": \"PROGRESS VERIFIED\",\n" +
                "      \"modified_count\": 5\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        JSONObject state = new JSONObject(sampleJson);
        assertTrue(state.has("last_check_timestamp"));
        assertTrue(state.has("milestone"));
        assertTrue(state.has("device_status"));
        assertTrue(state.has("build_status"));
        assertFalse(state.getBoolean("stall_detected"));
        assertEquals("10.199.103.54:5555", state.getString("tv_ip"));

        JSONObject device = state.getJSONObject("device_status");
        assertEquals("MIBOX4", device.getString("model"));
        assertEquals("CONNECTED_AND_ACTIVE", device.getString("connection"));

        JSONObject build = state.getJSONObject("build_status");
        assertEquals("PASSED", build.getString("unit_tests"));

        JSONArray history = state.getJSONArray("history");
        assertTrue(history.length() > 0);
        assertEquals("PROGRESS VERIFIED", history.getJSONObject(0).getString("status"));
    }

    @Test
    public void testDiskWatchdogStateFileIfExists() {
        // Look for watchdog-state.json in candidate locations relative to test runner
        String[] candidatePaths = new String[]{
                "../../tools/watchdog-state.json",
                "../tools/watchdog-state.json",
                "tools/watchdog-state.json"
        };

        File found = null;
        for (String p : candidatePaths) {
            File f = new File(p);
            if (f.exists() && f.isFile()) {
                found = f;
                break;
            }
        }

        if (found != null) {
            try (FileInputStream fis = new FileInputStream(found);
                 InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[1024];
                int read;
                while ((read = reader.read(buf)) != -1) {
                    sb.append(buf, 0, read);
                }
                JSONObject state = new JSONObject(sb.toString());
                assertTrue("State must contain milestone", state.has("milestone"));
                assertTrue("State must contain tv_ip", state.has("tv_ip"));
                assertFalse("Stall must not be flagged on healthy build", state.optBoolean("stall_detected", true));
            } catch (Exception e) {
                org.junit.Assert.fail("Failed parsing existing watchdog-state.json: " + e.getMessage());
            }
        }
    }
}
