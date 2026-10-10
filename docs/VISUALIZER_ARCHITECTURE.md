# Visualizer Architecture (V2.4–V2.8)

## Layers
1. **Capture** — `AudioVisualizerController` (session Visualizer → FFT/wave/features)
2. **Modes** — `VisualizerMode` enum (6 SIGNAL + 6 COSMIC/RAVE)
3. **Render** — `ApolloVisualizerSurface` + `CosmicVisualizerScenes` (Compose Canvas)
4. **Selection** — `VisualizerModeSelector` categories SIGNAL / COSMIC / RAVE
5. **Immersive** — `ImmersiveVisualizerActivity` (fullscreen overlay transport)
6. **Persistence** — `ThemeStore.visualizerMode`

## SIGNAL instruments (preserved)
FFT · BARS · WAVE · ORB · RINGS · VU

## COSMIC / RAVE worlds
HYPERDRIVE · NEBULA · SIGNAL OCEAN · PLASMA HEART · RAVE//CORE · FRACTURE

## Car Mode
- Compact mode chips on viz surface
- **IMMERSIVE** launches fullscreen activity (phone-side; not projected AA video)

## Performance notes
Canvas scenes; pause capture when UI disables; avoid game-engine dependency. Quality presets / thermal adaptive scaling remain future polish.
