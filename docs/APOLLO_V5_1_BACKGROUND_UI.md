# Apollo V5.1 — Immersive / Viz-Only Background UI

**Date:** 2026-10-09  
**Branch:** `fix/apollo-smoke-immersive-controls-v5`

## Existing engine (reused)

V4.2 already provided `VisualizerBackgroundMode`, `VisualizerBackgroundPolicy`, and `VisualizerBackgroundLayer` (blur / darkness / saturation). V5.1 exposes that engine inside fullscreen HUDs and adds user-facing **SCENE**.

## Modes

| Chip | Behavior |
|---|---|
| AUTO | Scene preference: rainbow → BLACK; atmospheric (incl. Smoke) → SCENE_DEFINED |
| BLACK | Pure `#000000`; suppress opaque scene fills |
| ALBUM | Blurred/dimmed cover under geometry |
| SCENE | Native atmosphere (`clearSceneBackdrop = false`) |

Precedence: per-scene override → global mode → AUTO recommendation.

## UI entry points

1. **Immersive** — top-right Layers icon → `VisualBackgroundSheet`
2. **Viz-Only (Car Mode)** — tap reveals chrome; Layers icon → same sheet; auto-hides after ~4.5s
3. **Gallery sheet** — AUTO/BLACK/ALBUM/SCENE chips
4. **Visualizer Settings** — BACKGROUND panel + SCENE note

Background changes update ThemeStore prefs only — no renderer recreation, no mono-clock reset.

## Files

- `ui/VisualBackgroundSheet.kt` (new)
- `ui/ImmersiveVisualizerActivity.kt`
- `ui/CarModeActivity.kt` (`VisualizerOnlyDeck`)
- `media/VisualizerBackgroundMode.kt` (+ SCENE)
- `theme/ThemeStore.kt` (scene overrides + smoke prefs)
