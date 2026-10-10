# Rollback — `v1.0.0-beta.3`

See also `V1_ROLLBACK.md` for earlier beta procedure.

## Quick rollback

1. Install previous release APK from GitHub Releases (`v1.0.0-beta.2` or prior).
2. Or checkout prior tag and rebuild:

```bash
git checkout v1.0.0-beta.2
./gradlew :app:assembleRelease
```

3. Force-stop Android Auto + Apollo after downgrade:

```bash
adb shell am force-stop com.google.android.projection.gearhead
adb shell am force-stop com.apolloauto.player
```

## Feature flags / data

Browse-context prefs are soft state; clearing Apollo app data restores default library scan without affecting media files.
