# ANS Monitor Pro — Setup Instructions

## Step 1: Add your Firebase google-services.json

The Chat tab requires Firebase Realtime Database.

1. Go to https://console.firebase.google.com
2. Open your existing BioSpace Monitor project (or create new)
3. Add Android app with package name: `com.biospace.ansmonitorpro`
4. Download `google-services.json`
5. Replace `app/google-services.json` with your downloaded file

## Step 2: Push to GitHub

```bash
cd ANSMonitorPro
git init
git add .
git commit -m "ANS Monitor Pro v1.0"
git remote add origin https://github.com/PotsPrisoner/ANS-MONITOR-PRO.git
git branch -M main
git push -u origin main
```

## Step 3: Get your APK

1. Go to github.com/PotsPrisoner/ANS-MONITOR-PRO → Actions tab
2. Wait for the Build APK workflow to complete (~3–5 min)
3. Click the completed run → Artifacts → Download `ANSMonitorPro-debug-apk`

## Tabs

| Tab | Data Source | Notes |
|-----|-------------|-------|
| SPACE | NOAA SWPC | Kp, storm scales, solar wind |
| IMF | NOAA SWPC DSCOVR | Bz 7-day, BT, components |
| SR | Derived model | No public real-time SR API exists |
| ANS | Computed | Derived from space + env data |
| ENV | Open-Meteo | Your GPS location |
| CME | NOAA indicator | Kp-based |
| ASSESS | Computed | Integrated body burden |
| ALERTS | NOAA SWPC | Live space weather alerts |
| CHAT | Firebase | Global channel — requires google-services.json |

## If Chat tab crashes without Firebase

If you don't have a Firebase project, remove the Chat tab temporarily:
In `MainActivity.kt`, delete the `AppTab.CHAT` entry from the `AppTab` enum
and the `AppTab.CHAT -> ChatScreen(...)` line in the `when` block.
Also remove `id 'com.google.gms.google-services'` from `app/build.gradle`
and the Firebase dependencies + classpath entries.
