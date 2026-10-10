# Apollo V5.1 — Rollback Guide

**Created:** 2026-10-09  
**Purpose:** Recoverable baseline before smoke renderer + immersive background UI work.

## Baseline

| Item | Value |
|---|---|
| Pre-V5.1 commit | `3003d61` — `fix(trance-v4.2): stop 3.2s viz snap, harden capture, add AUTO/BLACK/ART backgrounds` |
| Source branch | `feature/apollo-trance-v4-2-finalization` |
| Tag | `apollo-pre-v5-1` → `3003d61` |
| Feature branch | `fix/apollo-smoke-immersive-controls-v5` |
| Earlier V4.2 tag | `apollo-pre-trance-v4-2` → `689a0c6` |

## Restore commands (non-destructive)

```bash
# Return to V4.2 tip (pre V5.1)
git checkout apollo-pre-v5-1
# or
git checkout feature/apollo-trance-v4-2-finalization

# Return to pre-V4.2 tip
git checkout apollo-pre-trance-v4-2
```

## Reinstall prior APK

```bash
adb install -r /path/to/previous-app-debug.apk
```

Debug package: `com.apolloauto.player.debug`

## Do not

- Force-push to `master`
- Merge without approval
- Clear user SharedPreferences / library cache unless requested

## Credentials note

AcoustID / MusicBrainz login passwords must never be committed. Use Settings-held API keys only.
