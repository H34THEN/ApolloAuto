# APOLLO II
### Cyberpunk Music Command System

**Offline music. Immersive visuals. Your entire library under command.**

Apollo II is a standalone Android music player for local libraries — phone folders, USB SSD / SD via SAF — with Media3 playback, Android Auto media browsing, and a large audio-reactive visualizer gallery (Immersive / Viz-Only / Car Mode).

> **Current release:** `v1.0.0-beta.2` · Package `com.apolloauto.player`

## Features (implemented)

- Local library scanning (MediaStore + SAF folder trees)
- Playlists, protected **Car Favorites**, M3U import/export
- Background playback, lock-screen / Bluetooth media controls
- Equalizer + sleep timer
- Android Auto media app (browse + playback via MediaLibraryService)
- Immersive visualizers with AUTO / BLACK / ALBUM / SCENE backgrounds
- Shadertoy-inspired ST01–ST03, MILK//LAB, Tron Legacy, Trance Reactor packs

## Installation

1. Download **Apollo-II-v1.0.0-beta.2.apk** from [GitHub Releases](https://github.com/H34THEN/ApolloAuto/releases).
2. Verify SHA-256 against `SHA256SUMS.txt` on the release.
3. Install on the phone’s **Owner / personal** profile (not Work).
4. If upgrading from a debug build (`com.apolloauto.player.debug`), uninstall the debug package first — signatures differ.

```bash
adb install --user 0 -r Apollo-II-v1.0.0-beta.2.apk
```

## Quick Start

1. Open Apollo II → grant media/storage permission.
2. Settings → Library → add a phone folder or USB tree.
3. Scan → pick a track → play.
4. Immersive / Viz gallery for visualizers.

## Android Auto Setup

1. Install Apollo and confirm phone playback works.
2. Open **Android Auto** phone settings → Customize launcher → enable **Apollo**.
3. For sideloaded APKs: AA → Version (tap repeatedly) → Developer settings → **Unknown sources**.
4. Connect to the car and select Apollo from media apps.
5. Prefer the **Apollo full-app media** entry from the AA app rail when you want a larger browse / Now Playing surface. The dashboard media pane shares space with Maps — that split is controlled by Android Auto, not Apollo.

### What Android Auto can and cannot do

| Supported in projection | Native phone / Immersive only |
|---|---|
| Browse, search, playlists, Car Favorites | Fullscreen visualizers / shaders |
| Play / pause / next / previous / seek | Trance Reactor, Viz-Only, ST/MILK scenes |
| Host Queue (open from Now Playing, Back to dismiss) | Unrestricted animated overlays |

Apollo does **not** remove Google Maps from the AA dashboard or project Immersive visualizers onto the head unit. See `docs/release/ANDROID_AUTO_UX_AUDIT.md`.

Discovery has been owner-confirmed on a Sobalai wireless HU. Always re-check Next/Queue after updates.

## Visualizer Guide

- **Immersive / Viz-Only / Car Mode** share the same scene engine.
- Backgrounds: **AUTO**, **BLACK** (recommended for rainbow scenes), **ALBUM**, **SCENE**.
- Per-mode size presets (XS–XL) scale geometry without resetting animation clocks.
- Strong references: LIQUID, SPIRAL/POLY, SYNTH SMOKE, SPACE WARP.
- ST01 (GrafEq) / ST03 (Cyber Fuji) repaired in this beta — report regressions.

## Permissions & Privacy

See [PRIVACY.md](PRIVACY.md). Core playback is offline-local. Optional future AcoustID keys stay in private prefs.

## Supported Audio Formats

Via Media3/ExoPlayer (device codecs apply): MP3, AAC, FLAC, WAV, OGG/Opus where the platform decoder exists.

## Troubleshooting

| Issue | Action |
|---|---|
| Missing music | Re-grant SAF permission; rescan library |
| Ringtones in library | Filtered in beta.2 (OEM MediaStore mis-tags); rescan |
| Same album art everywhere / almost no art | Hashed cache + post-scan art enrich; wait for enrich or rescan |
| Android Auto missing | Owner profile install + Unknown sources for sideloads |
| AA Next missing | Fixed in beta.2 (single-track queue expand); replay a track from browse |
| AA Maps taking half the screen | Expected AA dashboard layout — open Apollo full-app media from the rail |
| Signature conflict | Uninstall previous build (data wipe) then install release |
| Visualizer silent | Grant RECORD_AUDIO if prompted; play music; check capture diagnostics |

## Development

- JDK **17**
- Android SDK 34
- `./gradlew :app:assembleDebug` / `:app:assembleRelease`
- Release signing via **gitignored** `local-release.properties` pointing at a local keystore

## Roadmap

- Full ECHOHUNT (Chromaprint → AcoustID) beyond scaffolding
- Additional shader ports with clear licenses
- CI signing + automated GitHub Releases
- Real-car AA regression checklist automation

## Credits

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## License

[Apache License 2.0](LICENSE)
