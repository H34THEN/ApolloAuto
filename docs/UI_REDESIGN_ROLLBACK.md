# Apollo II — UI Redesign Rollback (Phase D.2)

## Baseline recorded at branch creation

| Item | Value |
|---|---|
| Parent Kenney branch | `feature/kenney-cyberpunk-hud` |
| Parent HEAD (pre-D2) | `57875824a3a5b7709fbe21018941905fc06ce8d4` |
| UX feature branch | `feature/apollo-hud-ux-refinement` |
| Pre-Kenney tag | `apollo-pre-kenney-hud` → `7632dc81a4b437b9c47ab92a13539c7f15455e9f` |
| Master (theme/EQ/sleep) | `7632dc8` |
| Package (debug) | `com.apolloauto.player.debug` |
| Workspace | `/Users/Gengar/Desktop/local-github/ApolloAuto` |

Uncommitted Kenney/font work present at branch cut was carried onto `feature/apollo-hud-ux-refinement` (working tree), then checkpointed in subsequent commits on this branch.

## Recovery procedures

### A) Return to Kenney HUD experimental branch (source)

```bash
cd /Users/Gengar/Desktop/local-github/ApolloAuto
git switch feature/kenney-cyberpunk-hud
export JAVA_HOME="${JAVA_HOME:-$HOME/toolchains/jdk-17/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
./gradlew :app:assembleDebug
"$ANDROID_HOME/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
"$ANDROID_HOME/platform-tools/adb" shell am start -n com.apolloauto.player.debug/com.apolloauto.player.ui.MainActivity
```

### B) Return to pre-Kenney baseline (theme engine / EQ / sleep)

```bash
cd /Users/Gengar/Desktop/local-github/ApolloAuto
git switch --detach apollo-pre-kenney-hud
# or: git switch master
export JAVA_HOME="${JAVA_HOME:-$HOME/toolchains/jdk-17/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
./gradlew :app:assembleDebug
"$ANDROID_HOME/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
```

### C) Stay on UX branch but use Classic chrome

Settings → Display → Interface Style → **Classic Apollo**

Does not alter playlists, Car Favorites, EQ, themes, or application ID.

## Build / reinstall (any branch)

```bash
export JAVA_HOME="${JAVA_HOME:-$HOME/toolchains/jdk-17/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
./gradlew :app:assembleDebug
"$ANDROID_HOME/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
```

## Data safety

- Application ID unchanged (`com.apolloauto.player` / `.debug`)
- SharedPreferences / playlist store / theme prefs preserved across UI branches when package ID is unchanged
- Prefer no Room migrations for this phase

## Do not use

- `git reset --hard` (unless explicitly authorized)
- `git clean -fd` without reviewing untracked assets
- Force-push to remote
- Merging into `master` without approval
