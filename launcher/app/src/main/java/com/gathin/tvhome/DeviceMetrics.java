package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

/** Reads only system-provided values; unavailable sensor data is reported as unavailable. */
final class DeviceMetrics {
    final String storage;
    final String memory;
    final String cpu;
    final String temperature;
    DeviceMetrics(String storage, String memory, String cpu, String temperature) {
        this.storage = storage; this.memory = memory; this.cpu = cpu; this.temperature = temperature;
    }

    static DeviceMetrics read(Context context) {
        return new DeviceMetrics(readStorage(), readMemory(context), readCpu(), readTemperature());
    }

    private static String readStorage() {
        try {
            StatFs fs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            long total = fs.getTotalBytes(), free = fs.getAvailableBytes();
            if (total <= 0) return "Unavailable";
            return String.format(Locale.US, "%.1f GB free / %.1f GB", free / 1e9, total / 1e9);
        } catch (RuntimeException e) { return "Unavailable"; }
    }

    private static String readMemory(Context context) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            if (manager == null) return "Unavailable";
            manager.getMemoryInfo(info);
            return String.format(Locale.US, "%.0f MB free / %.1f GB", info.availMem / 1048576.0,
                    info.totalMem / 1e9);
        } catch (RuntimeException e) { return "Unavailable"; }
    }

    private static String readCpu() {
        try {
            long[] before = readCpuCounters();
            if (before == null) return "Unavailable";
            try { Thread.sleep(350L); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return "Unavailable"; }
            long[] after = readCpuCounters();
            if (after == null) return "Unavailable";
            long total = after[0] - before[0], idle = after[1] - before[1];
            if (total <= 0) return "Unavailable";
            int percent = Math.max(0, Math.min(100, Math.round(100f * (total - idle) / total)));
            return percent + "%";
        } catch (Exception e) { return "Unavailable"; }
    }

    private static long[] readCpuCounters() throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
            String line = reader.readLine();
            if (line == null || !line.startsWith("cpu ")) return null;
            String[] values = line.trim().split("\\s+");
            long total = 0, idle = 0;
            for (int i = 1; i < values.length; i++) {
                long value = Long.parseLong(values[i]);
                total += value;
                if (i == 4 || i == 5) idle += value;
            }
            return new long[]{total, idle};
        }
    }

    private static String readTemperature() {
        try {
            File thermal = new File("/sys/class/thermal");
            File[] zones = thermal.listFiles((dir, name) -> name.startsWith("thermal_zone"));
            if (zones == null) return "Unavailable";
            for (File zone : zones) {
                try {
                    String type;
                    try (BufferedReader typeReader = new BufferedReader(new FileReader(new File(zone, "type")))) {
                        type = typeReader.readLine();
                    }
                    if (type == null) continue;
                    String normalizedType = type.toLowerCase(Locale.US);
                    if (!normalizedType.contains("cpu") && !normalizedType.contains("soc")
                            && !normalizedType.contains("gpu") && !normalizedType.contains("cluster")) continue;
                    File input = new File(zone, "temp");
                    if (!input.canRead()) continue;
                    try (BufferedReader reader = new BufferedReader(new FileReader(input))) {
                        String raw = reader.readLine();
                        if (raw == null) continue;
                        double value = Double.parseDouble(raw.trim());
                        if (value > 1000) value /= 1000.0;
                        if (value > 0 && value < 150) return String.format(Locale.US, "%.1f °C", value);
                    }
                } catch (Exception ignored) { }
                }
        } catch (RuntimeException ignored) { }
        return "Unavailable";
    }
}
