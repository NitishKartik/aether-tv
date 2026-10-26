# 🌌 Aether TV — Master User & Configuration Guide

**Device:** Xiaomi Mi Box 4 (Android 9 Pie, API 28)  
**Launcher Version:** Aether TV 2.0 (Custom Build)  
**Host IP:** `10.199.103.54:5555`  
**Default Security PIN:** `1234`

---

## 📑 Quick Navigation & Capabilities Summary

| Feature | Remote Shortcut | Voice Command (Jarvis) | Description |
| :--- | :--- | :--- | :--- |
| **Jarvis Voice Assistant** | Press **Google Assistant** key | *"Hey Jarvis"* | Floating HUD assistant with voice command recognition & quick chips |
| **3-Second Screen Lock** | Hold **BACK** button for 3s | *"Lock TV"* | Locks workstation; triggers macOS-style frosted PIN lock screen |
| **All Apps / App Library** | Press **9-Square / 9-Dot** key | — | Instantly brings up full installed application catalog |
| **Open YouTube** | Top Bar / Remote D-pad | *"Open YouTube"* | Direct intent launch to YouTube TV Leanback app |
| **AI Intelligence Report** | Top Bar 📊 | *"What is AI?"* | Telemetry report across OpenAI, Anthropic, Gemini, Groq, Ollama |
| **Aether Drift (Car Game)** | Top Bar 🎮 / Arcade | *"Play Drift"* / *"Car game"* | 60 FPS Cyberpunk arcade highway racer with D-pad remote controls |
| **IMDb Cinema Posters** | Top Bar 🎬 | *"Show movies"* | Curated Hindi & Hollywood catalog with real cached posters & ratings |
| **AdGuard Private DNS** | Top Bar 🛡️ | *"AdGuard DNS"* | Native encrypted DNS-over-TLS blocking ad networks & telemetry |
| **Dynamic Brand Glow** | Grid Navigation | — | Focused app tile border glows in the app's genuine brand color |

---

## 🛠️ What You Need to Configure Manually (Step-by-Step)

Most features have already been pre-configured, tested, and automated over ADB. Here is the exact checklist of what you can customize or configure manually:

### 1. Private DNS (AdGuard Ad-Blocking)
AdGuard Private DNS is already active on your TV via ADB global settings (`dns.adguard.com`). If you ever factory-reset your TV or change Wi-Fi networks and wish to verify or re-enter it manually in the stock Android TV settings:

1. Open **Aether TV Control Center** (slider icon on top right) or open TV **Stock Settings**.
2. Select **Network & Internet**.
3. Scroll down to the **Private DNS** section.
4. Select **Private DNS provider hostname**.
5. Enter:
   ```
   dns.adguard.com
   ```
6. Click **Save**.
7. *Verification:* Open Aether TV's **AdGuard DNS** dialog (shield icon in top bar) and click **Run DNS Verification Test** — you will see a green checkmark confirming that ad domains are sinkholed to `0.0.0.0`.

> [!NOTE]
> AdGuard Private DNS blocks banner ads, tracking scripts, and pop-up servers system-wide across all apps. Note that video ads delivered directly through the same stream content servers (e.g. YouTube in-stream video ads) cannot be blocked by DNS alone without a modified client.

---

### 2. Lock Screen PIN & Security Settings
Your Aether TV is protected with a macOS-style frosted glass PIN screen.
- **Default PIN:** `1234`
- **How to Lock the Screen at Any Time:**
  - **Press and hold the TV Remote `BACK` button for 3 seconds** anywhere on the home screen.
  - Or click the **🔒 Lock icon** in the top bar.
- **How to Unlock:**
  - Use the TV Remote D-pad (navigation arrows) to highlight digits **1 → 2 → 3 → 4** on the on-screen keypad and press **➔** (or OK on the remote).
  - The TV will immediately unlock into your home screen without looping.
- **How to Change the Security PIN:**
  - Open **Aether TV Control Center** (top bar).
  - Navigate to **Privacy & Security**.
  - Choose **Change Security PIN**, enter your current PIN (`1234`), followed by your new 4-digit PIN.

---

### 3. Remote Control Button Mappings

Your Xiaomi Bluetooth TV Remote is fully integrated with custom low-level key listeners:

1. **Google Voice Assistant Button (`KEYCODE_VOICE_ASSIST` / `KEYCODE_SEARCH`):**
   - Pressing this button immediately opens **Jarvis Assistant**.
   - If a voice input service is active, speak your command into the remote microphone.
   - Available voice commands:
     - *"Open YouTube"* → launches YouTube TV leanback app.
     - *"What is AI?"* / *"AI Usage"* → opens the AI usage telemetry dashboard.
     - *"Car game"* / *"Aether Drift"* → starts the retro cyberpunk driving arcade.
     - *"Lock TV"* → locks the screen immediately.
2. **The 9-Square / 9-Dot Button (`KEYCODE_ALL_APPS`):**
   - Pressing the 3x3 square button on your remote immediately opens the **App Library** showing all sideloaded and installed Leanback/Android applications.
3. **The Back Button (Hold for 3s):**
   - Holding down the Back button for 3 seconds automatically triggers the security lock.

---

### 4. Aether Drift — TV Remote Car Racing Arcade

We built a custom 60 FPS retro-futuristic arcade racer that runs natively without any external dependencies:
- **How to Launch:** Click the 🎮 Game icon in the top bar (choose **Aether Drift**), or ask Jarvis (*"Car game"*).
- **Remote Controls:**
  - **D-pad Left (←):** Steer car left into the adjacent highway lane.
  - **D-pad Right (→):** Steer car right into the adjacent highway lane.
  - **D-pad Up (↑):** **Turbo Nitro Boost!** Accelerates your vehicle up to 180 km/h with cyan/orange exhaust flames and doubles score gain.
  - **D-pad Down (↓):** **Brakes!** Decelerates to weave through tight highway traffic.
  - **D-pad Center / OK:** Start game, or restart after a crash.
  - **Remote Back:** Pause game or return to the TV launcher.
- **High Scores:** Best distances and scores are saved in local flash storage.

---

### 5. IMDb Movie Posters & Cinematic Catalog

The Cinema discovery hub (🎬 icon) features curated Hindi & English movies with verified IMDb scores:
- Real movie posters are downloaded asynchronously from TMDB/IMDb CDN with local disk caching in `/data/data/com.gathin.tvhome/cache/posters/`.
- If offline, the launcher automatically renders custom high-res graphic canvas posters with movie initials and rating badges.
- Click any movie card with the remote to read the full plot summary, duration, cast list, and save it to your **Personal Watchlist**.

---

### 6. Dynamic App-Based Glow

When you browse your installed apps on the home grid:
- The frosted glass selection border automatically extracts the vibrant dominant color of the focused app's icon or banner.
- YouTube glows ruby red (`#FF0000`).
- Netflix glows cinema crimson (`#E50914`).
- Prime Video glows electric cyan (`#00A8E1`).
- Spotify glows neon emerald (`#1ED760`).
- Hotstar glows sapphire blue (`#0C2045`).

---

### 7. How to Rollback to Stock TV Launcher (If Ever Desired)

Your stock launcher configuration and APKs were safely backed up prior to any customizations. If you ever want to revert back to the factory Google Android TV launcher:

Run from your computer terminal connected to the TV:
```powershell
adb connect 10.199.103.54:5555
adb shell pm enable com.google.android.tvlauncher
adb shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity
```
To re-enable Aether TV as default:
```powershell
adb shell pm disable-user --user 0 com.google.android.tvlauncher
adb shell cmd package set-home-activity com.gathin.tvhome/.MainActivity
```

---
*Created with ❤️ for your Xiaomi Mi Box 4 workstation.*
