# Trance Reactor V4 — Scene Matrix

**Branch:** `feature/apollo-trance-reactor-v4`  
**Updated:** 2026-10-09 (V4.2 hardening on `feature/apollo-trance-v4-2-finalization`)  
**Renderer:** Jetpack Compose Canvas (shared `AudioVisualizerController`)  
**Statuses:** Planned · Implementing · Rendering · Audio reactive · Tested · Verified · Blocked

Legend for device columns: **Y** = yes, **N** = no / not run this pass, **—** = n/a.

| ID | Name | Mode enum | Audio inputs | Status | Emulator | Device | Limitations |
|---|---|---|---|---|---|---|---|
| S01 | FFT | SPECTRUM | fft, energy | Audio reactive | N | N | Preserved |
| S02 | BARS | BARS | fft | Audio reactive | N | N | Preserved |
| S03 | WAVE | WAVEFORM | wave | Audio reactive | N | N | Preserved |
| S04 | ORB | CIRCULAR | fft, bass | Audio reactive | N | N | Preserved |
| S05 | RINGS | RINGS | bass, level | Audio reactive | N | N | Preserved |
| S06 | VU | LEVEL | level, bass | Audio reactive | N | N | Preserved |
| C01 | HYPERDRIVE | HYPERDRIVE | features, fft | Audio reactive | N | N | Preserved cosmic |
| C02 | NEBULA | NEBULA | features | Audio reactive | N | N | Preserved |
| C03 | RAVE//CORE | RAVE_CORE | features, fft | Audio reactive | N | N | Preserved |
| C04 | SIGNAL OCEAN | SIGNAL_OCEAN | features, wave | Audio reactive | N | N | Preserved |
| C05 | PLASMA HEART | PLASMA_HEART | features | Audio reactive | N | N | Preserved |
| C06 | FRACTURE | FRACTURE | features, fft | Audio reactive | N | N | Preserved |
| TR01 | CHROMATIC PHI | CHROMATIC_PHI | fft, bass, treble, onset | Rendering / Audio reactive | N | N | Unit compile only |
| TR02 | ECTOPLASM | ECTOPLASM | subBass, lowMid, rms | Rendering / Audio reactive | N | N | Metaball approx |
| TR03 | CIRC∞ | CIRC_INFINITY | fft, band splits | Rendering / Audio reactive | N | N | |
| TR04 | PRISM RAIN | PRISM_RAIN | fft, onset, treble | Rendering / Audio reactive | N | N | |
| TR05 | SYNTH SMOKE | SYNTH_SMOKE | sub/bass/mids/treble/onset via fBm plumes (V5.1) | Rendering / Audio reactive | N | N | Was sine Path strokes; rewritten V5.1 |
| TR06 | NOTE SWARM | NOTE_SWARM | per-region bands | Rendering / Audio reactive | N | N | |
| TR07 | HARMONIC MANDALA | HARMONIC_MANDALA | fft, folds from lowMid | Rendering / Audio reactive | N | N | |
| TR08 | LIQUID SPECTRUM | LIQUID_SPECTRUM | fft + band regions | Rendering / Audio reactive | N | N | |
| TR09 | AUDIBLE SPIRALS | AUDIBLE_SPIRALS | sub/bass/low/upper/treble/onset | Rendering / Audio reactive | N | N | Variants via ThemeStore |
| TR10 | FLUCTUS FLOW | FLUCTUS_FLOW | subBass→bend, bands | Rendering / Audio reactive | N | N | Compose projection not WebGL |
| TR11 | WAVEFORM VOID | WAVEFORM_VOID | fft+wave layered | Rendering / Audio reactive | N | N | Widescreen extend |
| TR12 | OBSIDIAN REACTOR | OBSIDIAN_REACTOR | bass/onset/facets | Rendering / Audio reactive | N | N | Low detail via quality tier |
| TR13 | VISSONANCE MATRIX | VISSONANCE_MATRIX | fft + submodes A–D | Rendering / Audio reactive | N | N | Submode cycle in settings |

## Extra modes (not in 25-entry V4 matrix; still shipped)

SPECTR (WAVEBARS/MIRROR/PULSE/PARTICLES), MV2/STARS2, DNA, DROP, XR3, MINIMAL, DECORATIVE — **preserved**.

## Shared analysis

| Feature | Status |
|---|---|
| subBass / bass / lowMid / upperMid / presence / treble | Implemented |
| rms / peak / spectralFlux / onset / silence | Implemented |
| CaptureSource PLAYBACK_SESSION vs DECORATIVE | Implemented |
| Chromagram / tempo / beat confidence / centroid / rolloff | **Planned** |
| Microphone capture | **Planned** (not equivalent to session FFT) |
| OpenGL ES / AGSL engine | **Planned** |

## Automated tests this pass

- `AudioFeaturesBandsTest` — PASS (band separation + flux + psytrance profile)
- Prior `LrcParserTest` — not re-run in this matrix update unless suite invoked

## Device / performance

Physical landscape screenshot capture and FPS telemetry: **not completed this pass**. Do not treat as Verified.
APK assemble + adb install: recorded in implementation status when run.


## V4.2 hardening notes

- Animation clock is monotonic (no Restart warp snap).
- Background AUTO defaults rainbow scenes to BLACK.
- Device Verified column still N until soak QA.
