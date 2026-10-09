# Apollo II — Implementation Status

**Codename:** APOLLO // AUDIO COMMAND SYSTEM  
**Workspace:** `/Users/Gengar/Desktop/local-github/ApolloAuto`  
**Package:** `com.apolloauto.player` (debug: `com.apolloauto.player.debug`)  
**Last updated:** 2026-10-09

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
| Visualizers (real audio) | Missing | Phase D |
| Now Playing layout modes | Missing | Phase D |
| Smart playlists / M3U | Missing | Phase E/G |
| Offline pin cache | Missing | Phase G |
| Library maintenance | Missing | Phase E/G |
| Automated tests | Missing | Phase H |

### Checkpoint
- Git baseline: `b29787a` — phase-2 baseline before HUD/EQ/theme expansion
- Device: `00021157P002695` (authorized)

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
