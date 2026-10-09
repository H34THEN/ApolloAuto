# Apollo II — HUD Rollback Guide

## Baseline (pre-Kenney HUD)

| Item | Value |
|---|---|
| Baseline commit | `7632dc81a4b437b9c47ab92a13539c7f15455e9f` |
| Baseline tag | `apollo-pre-kenney-hud` |
| Feature branch | `feature/kenney-cyberpunk-hud` |
| Package (debug) | `com.apolloauto.player.debug` |
| Workspace | `/Users/Gengar/Desktop/local-github/ApolloAuto` |

Prior checkpoints also exist:
- `b29787a` — phase-2 baseline before HUD/EQ/theme expansion
- `7632dc8` — theme engine + EQ + sleep timer (same as tag)

## Return to original UI (source)

```bash
cd /Users/Gengar/Desktop/local-github/ApolloAuto
git switch master
# or explicitly:
git switch --detach apollo-pre-kenney-hud
export JAVA_HOME="$HOME/toolchains/jdk-17/Contents/Home"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
./gradlew :app:assembleDebug
"$ANDROID_HOME/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
"$ANDROID_HOME/platform-tools/adb" shell am start -n com.apolloauto.player.debug/com.apolloauto.player.ui.MainActivity
```

## Return to Kenney HUD redesign (source)

```bash
cd /Users/Gengar/Desktop/local-github/ApolloAuto
git switch feature/kenney-cyberpunk-hud
export JAVA_HOME="$HOME/toolchains/jdk-17/Contents/Home"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
./gradlew :app:assembleDebug
"$ANDROID_HOME/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
```

## In-app fallback

Settings → Display → **Interface Style**
- `Apollo Cyberpunk HUD` (Kenney-skinned)
- `Classic Apollo` (pre-Kenney Compose chrome)

This flag does not change database schemas. Switching Git branches does **not** change an already-installed APK until you reinstall.

## Data / schema

This redesign branch targets presentation only. Prefer no Room/SQLite migrations.
App data (playlists, Car Favorites, EQ, themes) lives in SharedPreferences and remains compatible across UI styles when package ID is unchanged.

## Do not use

- `git reset --hard` (unless you explicitly authorize data loss)
- `git clean -fd` without reviewing untracked assets
- Force-push to remote

## Recover uncommitted work

If a WIP commit or stash was created later:

```bash
git stash list
git stash show -p stash@{0}
# only apply when intended:
git stash apply stash@{0}
```
