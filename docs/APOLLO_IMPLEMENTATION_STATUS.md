# Apollo II — Implementation Status

**Codename:** APOLLO // AUDIO COMMAND SYSTEM  
**Workspace:** `/Users/Gengar/Desktop/local-github/ApolloAuto`  
**Package:** `com.apolloauto.player` (debug: `com.apolloauto.player.debug`)  
**Last updated:** 2026-10-09 (Kenney HUD branch)

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
| Android Auto browse | Implemented / Untested on DHU | Car Favorites first |
| Theme presets (6) | Implemented | Apollo Prime → Monochrome |
| Custom theme editor | Implemented | Hue + hex + glow; readability guard |
| Motion profiles | Implemented | Full / Reduced / Static / System |
| Equalizer (real) | Implemented | Session-linked + presets UI |
| Sleep timer | Implemented | Service-scoped + fade-out |
| Visualizers (real audio) | Implemented / Device pending | Android Visualizer + RECORD_AUDIO |
| Now Playing layout modes | Implemented | Tactical / Immersive / Terminal |
| Kenney Sci-Fi HUD assets | Partial | Now Playing + HUD components; K5 app-wide pending |
| Classic interface fallback | Implemented | Settings → Interface Style |
| Smart playlists / M3U | Missing | Phase E/G |
| Offline pin cache | Missing | Phase G |
| Library maintenance | Missing | Phase E/G |
| Automated tests | Missing | Phase H |

### Checkpoint
- Git baseline: `b29787a` — phase-2 baseline before HUD/EQ/theme expansion
- Pre-Kenney tag: `apollo-pre-kenney-hud` → `7632dc8`
- Feature branch: `feature/kenney-cyberpunk-hud`
- Device: `00021157P002695` (authorized)
- Rollback: `docs/HUD_ROLLBACK.md`

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
