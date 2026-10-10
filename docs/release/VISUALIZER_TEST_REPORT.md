# Visualizer Test Report — Car Experience Cut

**Version:** `1.0.0-beta.3`  
**Scope:** Stable gallery readiness for native Car Mode / Immersive

## Core scenes (preserve)

| Scene | Status | Notes |
|---|---|---|
| LIQUID | PASS (prior soak) | Strong audio response |
| SPIRAL | PASS (prior soak) | Strong audio response |
| POLY | PASS (code) | Landscape proportional scale retained; no distortion stretch |
| Trance Reactor | PASS (prior) | In gallery |

## Procedural / weak-audit set

| Scene | Status | Notes |
|---|---|---|
| SMOKE | PASS (code review) | Procedural fBm vapor; beat pressure / treble turbulence preserved |
| STAR / STAR2 | AUDIT | Functional; intensity varies by track — keep in gallery |
| SIER | AUDIT | Keep; not flagged as blank |
| RAIN | AUDIT | Keep |

## Shadertoy-inspired (Compose Canvas)

| ID | Status | Report |
|---|---|---|
| ST01 | REPAIRED (beta.1) | See `ST01_ST03_REPAIR_REPORT.md` — owner visual QA still recommended |
| ST02 | OK | Not reported broken |
| ST03 | REPAIRED (beta.1) | Landscape height-normalized Fuji layout |

## Background layers

AUTO / BLACK / ALBUM / SCENE — unchanged architecture; Car Mode Layers sheet continues to work without restarting playback.

## Device soak

Extended thermal/GPU soak on Sobalai projection is **N/A** (no projected shaders). Phone Immersive soak: not re-run this cut; no intentional renderer recreation loops introduced by AA/Car Mode changes.
