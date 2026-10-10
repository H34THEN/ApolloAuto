# Apollo V5.1 — Visual QA Checklist

**Date:** 2026-10-09  
**APK:** `app/build/outputs/apk/debug/app-debug.apk`  
**Installed:** `com.apolloauto.player.debug` on `00021157P002695`

## Screenshot matrix

| Shot | Target | Status |
|---|---|---|
| Smoke idle (Car HUD) | SYNTH_SMOKE prefs forced | Captured `docs/qa/v5_1/car_synth_smoke_v2.png` — mottled magenta/cyan vapor, **not** sine strokes |
| Landscape transport | Car Mode | Captured `car_mode.png` / `car_synth_smoke_v2.png` |
| Main launch | Home | Captured `main_launch.png` |
| Smoke on ALBUM / SCENE | Immersive | Pending (ImmersiveActivity not exported for adb) |
| Viz-Only BG sheet open | Car VIZ ONLY | Pending manual tap |
| Immersive BG sheet open | Immersive Layers | Pending manual tap |
| Chromatic Phi / Circ Infinity matrix | Immersive | Pending |

## Acceptance (code-complete vs verified)

| Criterion | Code | Device verified |
|---|---|---|
| Smoke is density vapor, not wavy lines | Yes | **Yes** — idle Car screenshot shows soft cloud field |
| Band-specific smoke response | Yes | Pending (0 tracks / no playback this session) |
| No 3.2s clock reset | Preserved mono clock | Pending soak |
| Immersive BG selector | Yes | Pending manual |
| Viz-Only BG selector | Yes | Pending manual |
| AUTO/BLACK/ALBUM/SCENE | Yes | Unit tests passed |
| Album blur/dark/sat | Yes (existing + sheet) | Pending |
| BG switch no scene restart | Prefs-only | Pending |
| Settings persist | ThemeStore | SYNTH_SMOKE prefs survived force-stop |
