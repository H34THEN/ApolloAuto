# Apollo Auto

Cyberpunk USB music player for Android phones and Android Auto car decks.

Tuned for landscape car radios around **7.4″ × 3.81″** (~1.94:1). Use **Car Mode** for the neon deck UI; Android Auto discovers the same library via Media3.

## Features

- **Cyberpunk phone + Car Mode UI** — neon grid, spectrum orb, large transport controls
- **USB-C SSD library** — pick the drive root once (SAF); scans MP3/FLAC/M4A/AAC/WAV/OGG/OPUS…
- **Android Auto** — `MediaLibraryService` browse tree (`All Tracks` + folders)

## Build

```bash
export JAVA_HOME="$HOME/toolchains/jdk-17/Contents/Home"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk` (also copied to `ApolloAuto-cyber-debug.apk`).

## Install (USB debugging)

1. Unlock the phone and accept **Allow USB debugging** if prompted.
2. `adb install -r ApolloAuto-cyber-debug.apk`

## First-run on phone

1. Attach the SSD with a USB-C OTG / hub (phone must host the drive).
2. Open **Apollo Auto** → **LINK USB SSD** → grant access to the drive root.
3. Tap **CAR MODE** for the landscape cyber deck, or connect the phone to the car and open **Apollo Auto** in Android Auto.

## Note on Android Auto visuals

Android Auto draws its own media templates on the head unit. Apollo supplies the browse tree, metadata, and playback. The full cyberpunk chrome lives in Apollo’s **Car Mode** on the phone (and any phone-mirrored display).
