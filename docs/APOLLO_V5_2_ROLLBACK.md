# Apollo V5.2 — Rollback Guide

**Created:** 2026-10-09  
**Purpose:** Recoverable baseline before visualizer reactivity rehabilitation.

## Checkpoints

| Item | Value |
|---|---|
| Pre-V5.1 | `apollo-pre-v5-1` → `3003d61` |
| V5.1 committed | `apollo-v5-1-checkpoint` / `bb5a667` — smoke + BG sheet |
| V5.1 branch | `fix/apollo-smoke-immersive-controls-v5` |
| V5.2 branch | `fix/apollo-v5-2-visualizer-reactivity` |

## Restore commands (non-destructive)

```bash
# Return to V5.1 tip (smoke + immersive BG)
git checkout apollo-v5-1-checkpoint
# or
git checkout fix/apollo-smoke-immersive-controls-v5

# Return to pre-V5.1 / V4.2 tip
git checkout apollo-pre-v5-1
```

## APK

Previous debug APK path (rebuild if missing):

`app/build/outputs/apk/debug/app-debug.apk`

Package: `com.apolloauto.player.debug`

## Do not

- Force-push to `master`
- Merge without approval
- Discard ThemeStore / library prefs without request
