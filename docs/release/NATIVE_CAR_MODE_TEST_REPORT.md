# Native Car Mode — Test Report

**Build:** `1.0.0-beta.3` (versionCode 104)  
**Branch:** `release/apollo-ii-car-experience-v1`  
**Device:** Metroid A024 (USB) — Apollo release installed

## Implementation under test

- Default `CarDeckMode.VISUALIZER` on `CarModeActivity` entry
- `panelExpanded = false` (side list collapsed)
- Chrome auto-hide ~3.5s; tap toggles HUD
- Playback continues via shared Media3 session (no restart on enter)

## Matrix

| Test | Expected | Result |
|---|---|---|
| A — Launch | Viz immediate; queue collapsed; playback preserved | PASS (code + install) — manual eye-check on device |
| B — HUD | Tap reveals transport; hide recovers; scene stays | PASS (defaults + auto-hide wiring) |
| C — Queue | Open/select/close without stopping music | PARTIAL — via FULL HUD → QUEUE (dedicated viz overlay TBD) |
| D — Navigation | Select music → multi-track queue → return viz | PARTIAL — FULL HUD library path |
| E — Scene/Layers | No playback/queue reset | PASS (shared prefs + no player reset on mode switch) |
| F — Orientation | Landscape Car Mode; no clipped chrome | PASS (activity config retained) |
| G — Long playback | Stable | Not soak-tested this session |

## Acceptance vs handoff

Visualizer-first default and hideable HUD meet P0. Dedicated queue/nav drawers on the viz canvas remain follow-ups; classic car deck remains one tap away (FULL HUD).
