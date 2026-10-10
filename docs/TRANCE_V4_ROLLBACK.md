# Trance Reactor V4 — Rollback Guide

**Created:** 2026-10-09  
**Purpose:** Recoverable baseline before Trance Reactor V4 work.

## Pre-V4 baseline

| Item | Value |
|---|---|
| Pre-WIP commit | `f632822` — `feat(v2): music sources, M3U engine, cosmic visualizers, immersive player` |
| Branch before V4 tip | `feature/apollo-v2-visualizers-library` |
| Tag | `apollo-pre-trance-v4` (points at tip commit that includes all WIP up to V4 start) |
| Feature branch | `feature/apollo-trance-reactor-v4` |

## What the tip commit preserves

Uncommitted work that was checkpointed into the tip includes (non-exhaustive):

- Library catalog disk cache + album-art resolver
- XR3 / DNA / DROP / MV2 / STARS2 visualizer scenes
- Immersive lyrics, companion voice, play history, session store
- Visualizer settings customizer

## Rollback options (non-destructive)

### A — Leave V4 branch, return to tip tag

```bash
git checkout apollo-pre-trance-v4
# or
git checkout feature/apollo-v2-visualizers-library
```

### B — Discard only V4 commits (keep tip)

```bash
git checkout feature/apollo-trance-reactor-v4
git log --oneline apollo-pre-trance-v4..HEAD
# Then reset soft/hard ONLY if you explicitly intend to drop V4 commits:
# git reset --hard apollo-pre-trance-v4
```

### C — Return to V2 commit before any WIP tip

```bash
git checkout f632822
```

### D — Reinstall prior debug APK

If you kept a previous `app-debug.apk`:

```bash
adb install -r /path/to/previous-app-debug.apk
```

Package id (debug): `com.apolloauto.player.debug`

## Do not

- Force-push to `master`
- Run destructive resets unless you intend to drop local commits
- Merge V4 to `master` automatically

## User data

Rollback does **not** clear SharedPreferences, library cache files, or playlists.
To clear app data: Android Settings → Apps → Apollo II → Clear storage (manual).
