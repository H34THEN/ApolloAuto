# Visualizer Architecture (V2.4–V2.8)

## Layers
1. **Capture** — `AudioVisualizerController` (session Visualizer → FFT/wave/features)
2. **Modes** — `VisualizerMode` enum (SIGNAL + SPECTR + COSMIC/RAVE + MV2)
3. **Render** — `ApolloVisualizerSurface` + scene modules (Compose Canvas)
4. **Selection** — `VisualizerModeSelector` categories SIGNAL / SPECTR / COSMIC / RAVE / MV2
5. **Customizer** — Settings → **VISUALIZER** tab (`ThemeStore.enabledVisualizerModes`)
6. **Immersive** — `ImmersiveVisualizerActivity` (fullscreen overlay transport)
7. **Persistence** — `ThemeStore.visualizerMode` + enabled set + randomizer

## SIGNAL instruments (preserved)
FFT · BARS · WAVE · ORB · RINGS · VU

## COSMIC / RAVE worlds
WIRE (HYPERDRIVE id; nixonyh-inspired wireframe sphere) · NEBULA · SIGNAL OCEAN · PLASMA HEART · RAVE//CORE · FRACTURE

**SPECTR (audiospectr-inspired, MIT):** WBAR · MIRR · PULSE · DOTS — plus immersive blurred cover backdrop. Not a video exporter.

**MV2 (MusicVisualizerV2-inspired, MIT):** NATION · BUBBLE · CPULSE · CSIG · DUAL · MIXED · SHAPE · STARS — Compose ports of CalebABG/MusicVisualizerV2 ideas (not Processing/Minim). See `docs/licenses/CALEBABG_MUSICVISUALIZERV2_MIT.txt`.

**STARS2:** streaking FFT-driven stars inspired by mostafa7904/Stars-Audio-Visualizer (Compose reimplementation). See `docs/licenses/MOSTAFA7904_STARS_AUDIO_VISUALIZER_NOTE.txt`.

**DNA (MusicDNA-inspired aesthetic):** HELIX · STRAND — original Apollo Compose scenes (MusicDNA is CC BY-NC-SA; we do not vendor its source). See `docs/licenses/MUSICDNA_CC_BY_NC_SA_NOTE.txt`.

**DROP (BeatDrop/MilkDrop-inspired):** TUNNEL · DROP — original Compose beat-warp scenes (not MilkDrop/ns-eel). See `docs/licenses/BEATDROP_BSD_NOTE.txt`.

**XR3 (XR3Player-inspired, LGPL note):** OSC · CIRC · POLY · XBARS · XVU · SIER · JULIA · SPHERE — original Compose scenes inspired by goxr3plus/XR3Player display modes (not a JavaFX port). See `docs/licenses/GOXR3PLUS_XR3PLAYER_LGPL_NOTE.txt`.

## Customizer
- Enable/disable individual modes; quick filters ALL / SIGNAL / SPECTR / COSMIC / MV2
- Selectors, immersive next, and randomizer only use enabled modes
- At least one mode must remain enabled; newly added modes auto-enable on upgrade

## Car Mode
- Compact mode chips on viz surface (filtered by customizer)
- **IMMERSIVE** launches fullscreen activity (phone-side; not projected AA video)

## Performance notes
Canvas scenes; pause capture when UI disables; avoid game-engine dependency. Quality presets / thermal adaptive scaling remain future polish.
