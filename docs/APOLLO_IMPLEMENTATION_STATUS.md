# Apollo II — Implementation Status

**Codename:** APOLLO // AUDIO COMMAND SYSTEM  
**Workspace:** `/Users/Gengar/Desktop/local-github/ApolloAuto`  
**Package:** `com.apolloauto.player` (debug: `com.apolloauto.player.debug`)  
**Last updated:** 2026-10-10 (AA UX hotfix beta.2 — queue expand, shallow browse; see `docs/release/ANDROID_AUTO_UX_AUDIT.md`)

## Phase A — Existing Application Audit

### Architecture
- Single-module Android app (Kotlin + Jetpack Compose + Media3 ExoPlayer)
- `ApolloPlaybackService` (`MediaLibraryService`) owns playback + Android Auto browse
- `LibraryStore` / `DeviceMusicScanner` / `UsbMusicScanner` for sources
- `PlaylistStore` with locked `Car Favorites` system playlist
- Theme tokens via `HudPalette` + `ThemeStore`
- `EqualizerController` + `SleepTimerController` (service-scoped)

### Feature matrix

| Feature | Status | Notes |
|---|---|---|
| App install / launch | Implemented / Tested (device) | Metroid A024 via ADB |
| Phone MediaStore library | Implemented | Permission gated |
| USB SSD SAF scan | Implemented / Untested w/ SSD | Persistable URI |
| Playback (Media3) | Implemented | Shared session for Auto |
| Playlists CRUD | Implemented | Custom create/delete |
| Car Favorites (protected) | Implemented | Cannot rename/delete |
| Playlist membership sheet | Implemented | |
| ALL / CAR ★ / LISTS | Implemented | |
| Car Mode | Implemented | Landscape HUD |
| Android Auto browse | Implemented / Owner HU confirmed | Shallow root + More; see AA UX docs |
| Android Auto Next/Queue | Fixed in beta.2 | Single-track expand via `MediaQueueExpand` |
| Theme presets (6) | Implemented | Apollo Prime → Monochrome |
| Custom theme editor | Implemented | Hue + hex + glow; readability guard |
| Motion profiles | Implemented | Full / Reduced / Static / System |
| Equalizer (real) | Implemented | Session-linked + presets UI |
| Sleep timer | Implemented | Service-scoped + fade-out |
| Visualizers (real audio) | Implemented / Device pending | Android Visualizer + RECORD_AUDIO |
| Now Playing layout modes | Implemented | Tactical / Immersive / Terminal |
| Kenney Sci-Fi HUD assets | Partial | Now Playing + HUD components; K5 app-wide pending |
| Classic interface fallback | Implemented | Settings → Interface Style |
| Smart playlists / M3U | Partial (V2) | Import/discover/resolve; export API; AA browse unverified |
| Music Source Manager | Implemented (V2) | Multi SAF + MediaStore; Settings → Library |
| Cosmic visualizers (6) | Implemented (V2) | Compose Canvas scenes; device reactivity pending |
| Immersive viz player | Implemented (V2) | Phone + Car IMMERSIVE; not AA projection |
| Offline pin cache | Missing | Phase G |
| Library maintenance | Partial | Source disable/remove; deeper tools TBD |
| Automated tests | Partial | Filename + M3U unit tests |

### Checkpoint
- Git baseline: `b29787a` — phase-2 baseline before HUD/EQ/theme expansion
- Pre-Kenney tag: `apollo-pre-kenney-hud` → `7632dc8`
- Kenney branch: `feature/kenney-cyberpunk-hud` @ `5787582`
- UX branch: `feature/apollo-hud-ux-refinement`
- Active fix branch: `fix/apollo-metadata-car-controls-shuffle` @ `cf50bb1` checkpoint
- V2 branch: `feature/apollo-v2-visualizers-library`
- V4 tip tag: `apollo-pre-trance-v4` @ `0be111d`
- V4 branch: `feature/apollo-trance-reactor-v4`
- Device: `00021157P002695` (authorized)
- Rollback: `docs/TRANCE_V4_ROLLBACK.md` (+ `docs/V2_ROLLBACK.md`)

---

## Milestone log

### Phase A complete
**Files changed:** git init, `docs/APOLLO_IMPLEMENTATION_STATUS.md`  
**Features completed:** Audit + recoverable checkpoint  
**Tests executed:** `adb devices`, source inventory  
**Tests passed:** Device authorized  
**Tests failed:** None  
**Regressions discovered:** None  
**Regressions fixed:** N/A  
**Remaining risks:** No automated tests; Android Auto untested on DHU/car  
**Next implementation phase:** B + C

### Phase B + C complete (Theme engine, EQ, Sleep timer)
**Files changed:**
- `theme/HudPalette.kt`, `ThemeStore.kt`, `CyberTheme.kt`
- `media/EqualizerController.kt`, `SleepTimerController.kt`, `ApolloPlaybackService.kt`
- `ApolloApp.kt`, `ui/SettingsActivity.kt`, `ui/hud/ApolloHudComponents.kt`
- `ui/MainActivity.kt`, `ui/CyberBackground.kt`, `AndroidManifest.xml`, `strings.xml`
- `docs/APOLLO_IMPLEMENTATION_STATUS.md`

**Features completed:**
- Six named themes (Apollo Prime, Hades Watch, Eidolon, Neon Revenant, Ghost Protocol, Monochrome Terminal)
- Custom theme editor (hex, hue sliders, glow, apply/restore, contrast guard)
- Motion profile + glow intensity
- Real Android `Equalizer` + `BassBoost` on ExoPlayer audio session
- Built-in EQ presets (Flat, Bass Boost, Vocal, Rock, Electronic, Classical, Acoustic, Car Audio) + save/delete custom
- Sleep timer in app/service scope: presets, custom minutes, end of track/queue, extend, cancel, optional fade-out
- Settings Command Center (Display / Audio / Playback)
- Status bar + settings entry on main HUD
- WindowInsets padding on main/settings

**Tests executed:**
- `./gradlew :app:assembleDebug` → SUCCESS
- `adb install -r` → Success
- `adb shell am start …MainActivity` → launched
- Device: Metroid A024 connected

**Tests passed:** Compile, install, launch  
**Tests failed:** None in this milestone  
**Regressions discovered:** None observed at install/launch  
**Regressions fixed:** N/A  

**Remaining risks / unverified:**
- EQ audible effect: not ear-verified in this session (requires playback + listening)
- Sleep timer expire/fade: not wait-tested for full duration
- Android Auto: not exercised on DHU/real head unit this milestone
- Custom theme contrast guard is heuristic, not WCAG formal

**Next implementation phase:** D — Now Playing HUD layouts + real audio visualizers

### Kenney HUD K0–K4 / K6 (partial K5)
**Branch:** `feature/kenney-cyberpunk-hud` (not merged to master)  
**Files changed:** Kenney inventory tool, selected drawables/fonts, `KenneyHudComponents`, `NowPlayingActivity`, `AudioVisualizerController`, interface style flag, docs (`HUD_ROLLBACK`, `HUD_ASSET_MANIFEST`, `HUD_VISUAL_QA`, inventory JSON)

**Features completed:**
- Tag `apollo-pre-kenney-hud` + feature branch
- Located `~/Downloads/kenney_ui-pack-space-expansion.zip` (Kenney UI Pack Sci-fi 2.0, CC0)
- Asset inventory (1114 files) + recommendations
- Curated runtime assets only (11 PNGs + 2 fonts)
- Theme-tintable Kenney HUD components
- Now Playing: Tactical / Immersive / Terminal (persisted)
- Real Visualizer FFT/waveform/circular/level when RECORD_AUDIO granted
- Classic Apollo interface style fallback (persisted)
- Build + install + launch on Metroid A024

**Tests executed:** `assembleDebug`, `adb install -r`, launch MainActivity  
**Tests passed:** compile, install, launch  
**Tests failed:** none this milestone  
**Regressions discovered:** none at launch  
**Remaining risks:** Visualizer/EQ listening unverified; full-app Kenney skin (K5) incomplete; Android Auto DHU untested; Classic mode only swaps Settings panels + chrome selectively  

**Next:** K5 full-app Kenney migration (library rows, Car Mode, EQ chrome) + visual QA screenshots

### Phase D.2 — HUD UX reconstruction (in progress / installable)
**Branch:** `feature/apollo-hud-ux-refinement` (not merged)  
**Report:** `docs/APOLLO_UI_REDESIGN_REPORT.md`  
**Rollback:** `docs/UI_REDESIGN_ROLLBACK.md`

**Features completed:**
- Spacing / type tokens (`ApolloSpace`, `ApolloTypeScale`)
- Decorative font restricted; readable body face on controls/nav/rows
- Compact header + bottom navigation (Home / Library / Lists / Car / Settings)
- Redesigned Now Playing card (art fallback, primary vs secondary transport)
- Library rows with hierarchy + overflow actions + playing accent
- Responsive Car Mode landscape (art / playback / CAR·ALL·QUEUE)
- Compact empty Car Favorites state
- Device screenshots under `docs/screenshots/d2/`
- Debug APK installed on Metroid A024

**Acceptance progress:** Header density, nav readability, NP spacing, art fallback, library hierarchy, Car Mode clipping/empty state — addressed. Full §15 screenshot matrix and deep EQ/sleep/AA regression still open.

**Next:** Extended visual QA (themes + Settings/EQ/sleep), optional Albums/Artists categories, merge approval gate

### Phase D.3 — Metadata, Car heart, stable shuffle
**Branch:** `fix/apollo-metadata-car-controls-shuffle` (not merged)  
**Reports:** `docs/APOLLO_D3_FIX_REPORT.md`, `docs/APOLLO_QUEUE_BEHAVIOR.md`

**Features completed:**
- `TrackMetadataFormatter` suppresses redundant artist/album lines (storage unchanged)
- USB scan no longer duplicates folder into artist+album
- Car Mode SAVE TO CAR → heart icon (Car Favorites toggle)
- `StablePlaybackQueue` one-shot shuffle reorder; NEXT does not reshuffle
- Car Mode QUEUE shows player timeline order
- Unit tests for formatter + shuffle order helper

**Verified:** unit tests + debug install (device).  
**Unverified:** Android Auto NEXT parity, process-death shuffle restore.


### Phase Trance Reactor V4
**Branch:** `feature/apollo-trance-reactor-v4` (not merged)  
**Docs:** `docs/TRANCE_V4_ROLLBACK.md`, `docs/TRANCE_V4_REFERENCE_AUDIT.md`, `docs/TRANCE_V4_SCENE_MATRIX.md`

**Features completed (code):**
- Expanded `AudioFeatures` bands + flux/peak; `TranceResponseProfile` multipliers
- TR01–TR08 Trance Reactor Compose scenes
- TR09–TR13 Experimental Compose scenes (spirals / fluctus / void / obsidian / vissonance)
- Gallery sheet, favorites, palette/quality/glow, Auto DJ interval + track boundary
- Photosensitive default caps beat flash
- Band unit tests (`AudioFeaturesBandsTest`)

**Honest gaps:**
- No OpenGL ES / AGSL engine this pass
- No chromagram / tempo / microphone path
- Physical device scene FPS + landscape screenshot matrix not verified
- Do not label TR scenes "Verified" until device QA

**Next:** Device landscape QA, optional GPU path, merge approval gate


### Phase Trance Reactor V4.2
**Branch:** `feature/apollo-trance-v4-2-finalization` (not merged)  
**Docs:** `docs/TRANCE_V4_2_*.md`

**Critical fixes:**
- Monotonic viz clock (removed 3.2s Restart snap)
- Capture soft-pause + stale recovery + diagnostics overlay
- Background engine AUTO/BLACK/ALBUM_ART

**Honest gaps:** device soak FPS/screenshots not yet Verified.

### Phase V5.1 — Smoke + Immersive BG controls (intermediate)
**Branch:** `fix/apollo-smoke-immersive-controls-v5` (not merged)  
**Baseline tag:** `apollo-pre-v5-1` → `3003d61`  
**Rollback:** `docs/APOLLO_V5_1_ROLLBACK.md`  
**Docs:** `docs/APOLLO_V5_1_*.md`

**Completed in code:**
- SYNTH SMOKE rewritten as fBm density vapor (`SynthSmokeScene.kt`) — not sine strokes
- User SCENE background mode + Immersive/Viz-Only Layers sheet
- Smoke color presets + controls in ThemeStore / Settings
- Per-scene background overrides
- Debug APK assembled and installed on Metroid A024

**Not claimed finished:**
- Screenshot matrix visual inspection
- 10 / 30 minute soak metrics
- Full V5 music-source / M3U / ECHOHUNT roadmap
- AcoustID credentials (never commit passwords; use Settings API key later)

**Next:** Device visual QA → V5 feature audit → ECHOHUNT scaffold

### Phase V5.2 — Visualizer reactivity stabilization
**Branch:** `fix/apollo-v5-2-visualizer-reactivity` (not merged)  
**Baseline:** `apollo-v5-1-checkpoint` / `bb5a667`  
**Rollback:** `docs/APOLLO_V5_2_ROLLBACK.md`

**Completed in code:**
- Shared `VisualResponseEngine` (band expand, kick/sparkle, envelopes, POLY viewport)
- STAR / STAR2 / SIER / RAIN rebuilt with geometry-first mappings
- SMOKE contrast + beat pressure + chromatic vibration (still fBm vapor)
- POLY FILL + spiral scale (default 1.35× max-side) — audio mapping preserved
- LIQUID / SPIRAL left untouched
- Unit tests for mapper + smoke defaults + BG policy

**Not claimed finished:**
- Music-on-device band A/B (library may still be empty)
- 30-minute soak / FPS profiling
- Full synthetic Audio Test Lab PCM fixtures
- Broader V5 / ECHOHUNT product work (paused per gate)

### Phase AA UX hotfix — `1.0.0-beta.2`
**Branch:** `hotfix/android-auto-ux-p0`  
**Docs:** `docs/release/ANDROID_AUTO_*.md`, `RELEASE_CHECKLIST.md`

**Completed in code:**
- `MediaQueueExpand` restores AA Next/Queue for single-track browse taps
- Shallow root browse + `More` secondary categories; short titles
- Automotive controllers get fewer custom session commands
- OEM ringtone MediaStore filter + SAF album-art enrich (checkpoint on same branch)

**Not claimed finished:**
- Physical Sobalai retest of Next/Queue after install
- Car App Library MediaPlaybackTemplate migration (deferred; see UX audit)
- GitHub Release publish for beta.2 (APK built locally)
