# Apollo II V2 — Rollback Guide

**Feature branch:** `feature/apollo-v2-visualizers-library`  
**Checkpoint before V2 work:** `cf50bb1` — *checkpoint: metadata parse, reactive viz modes, title-row skips, app-wide font*  
**Prior D.3 tip:** `15d5648` — *fix(d3): dedupe metadata, car heart favorite, stable shuffle queue*

## Restore source (Git)

Do **not** force-reset shared remotes. Prefer switching branches or creating a recovery branch.

```bash
# Leave V2 work on its branch; return to pre-V2 checkpoint tip
git checkout fix/apollo-metadata-car-controls-shuffle
# or pin exactly:
git checkout -b recovery/pre-v2 cf50bb1
```

If you need the older D.3 tip without the viz/font checkpoint:

```bash
git checkout -b recovery/d3-tip 15d5648
```

Git restore alone does **not** change the APK already installed on a device.

## Restore APK on device

Rebuild and reinstall from the recovery branch:

```bash
export JAVA_HOME="$HOME/toolchains/jdk-17/Contents/Home"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
./gradlew :app:assembleDebug
"$ANDROID_HOME/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
```

Or uninstall then install if signatures/package state conflict:

```bash
adb uninstall com.apolloauto.player.debug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Data safety

- Uninstalling **clears** SharedPreferences (themes, playlists JSON, library URIs, font choice).
- Prefer reinstalling over uninstall when keeping Car Favorites / playlists.
- Removing a music source inside Apollo must never delete audio files on disk (V2 policy).

## Do not

- `git push --force` to `master`
- Hard-reset shared branches without an explicit request
- Merge V2 automatically into other feature branches
