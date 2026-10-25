# Aether TV — Rollback & Recovery Guide

This document provides fail-safe, step-by-step procedures to revert your Xiaomi Mi Box 4 (or any Android TV device) from **Aether TV** back to the factory stock launcher and settings.

---

## 1. Safety Backups Reference

During the initial deployment of Aether TV, a full snapshot of device configurations was preserved in:
`launcher/backups/device_state/`

This includes:
- `active_home_launcher.txt` — The exact package and activity that served as the stock TV Home launcher.
- `all_packages.txt` / `enabled_packages.txt` / `disabled_packages.txt` — Complete package state prior to debloating or customizing.
- `private_dns_mode.txt` & `private_dns_specifier.txt` — Stock DNS settings before configuring AdGuard Private DNS (`dns.adguard.com`).
- `enabled_accessibility_services.txt` — Any pre-existing accessibility hooks.
- `getprop_dump.txt` — Full hardware and OS build fingerprint.
- `stock_tvlauncher_base.apk` (7.4 MB) — Complete APK extracted directly from device.

---

## 2. Automated One-Command Rollback (Recommended)

From your computer terminal connected to the TV over Wi-Fi (`10.199.103.54:5555`):

```powershell
# In PowerShell:
adb connect 10.199.103.54:5555

# Restore disabled stock packages from backup
powershell -ExecutionPolicy Bypass -File .\tools\restore.ps1 -TvHost 10.199.103.54:5555 examples\xiaomi-mi-tv-4a\debloat.txt -Stock com.google.android.tvlauncher
```

---

## 3. Manual Step-by-Step Rollback via ADB

If you prefer to perform each step individually:

### Step 1: Re-enable the Stock TV Launcher
```bash
# Enable Google Android TV Launcher
adb shell pm enable com.google.android.tvlauncher

# Or enable Xiaomi PatchWall / MiTV Home (if present)
adb shell pm enable com.mitv.tvhome
```

### Step 2: Set the Default HOME Activity
```bash
# Clear Aether TV default preference
adb shell pm clear-package-preferred-activities com.gathin.tvhome

# Launch the stock launcher
adb shell am start -c android.intent.category.HOME -a android.intent.action.MAIN
```

### Step 3: Revert Private DNS (AdGuard) to Default
```bash
# Set Private DNS back to automatic ("opportunistic") or off:
adb shell settings put global private_dns_mode opportunistic
adb shell settings put global private_dns_specifier ""
```

### Step 4: Uninstall Aether TV (Optional)
```bash
# Safely remove Aether TV (keeps your user data intact until uninstalled)
adb shell pm uninstall com.gathin.tvhome
```

---

## 4. Emergency Recovery Without PC (TV Remote Only)

If your PC is unavailable or ADB is disconnected:

1. Press the **Settings** or **Input** button on your Xiaomi Mi Box remote.
2. If only the Home button is available, navigate to **Aether TV Control Center** (top bar gear icon).
3. Select **Display & System Settings** -> **Android System Settings**.
4. In Android TV Settings:
   - Go to **Apps** -> **See all apps** -> **Aether TV**.
   - Select **Clear defaults**, or select **Uninstall**.
5. Press the **Home** button on the remote: Android TV will prompt you to select the default launcher. Choose **Android TV Home** and select **Always**.
