# Apollo V5 — Feature Audit (pre-duplication)

**Date:** 2026-10-09  
**Purpose:** Avoid re-implementing V5 pieces that already exist after V5.1 visualizer repair.

| Area | Status in tree | Notes |
|---|---|---|
| Music Source Manager | Partial → largely Implemented | `MusicSourceStore`, phone MediaStore + SAF folders, USB SSD, SD kinds |
| M3U / M3U8 | Partial Implemented | `M3uParser`, `M3uPathResolver`, `M3uPlaylistService` + unit tests |
| Library browser | Partial | Albums/artists via browse; smart playlists TBD |
| Play history / recently played | Partial | `PlayHistoryStore` present |
| EQ / sleep timer | Implemented | Preserve |
| ReplayGain / gapless / crossfade | Partial / TBD | Crossfade pref exists; verify behavior |
| Chromaprint / AcoustID / MusicBrainz | Missing | ECHOHUNT scaffold next — **no secrets in repo** |
| Android Auto | Implemented (session) | DHU soak unverified |

## Credentials policy

- Never commit MusicBrainz passwords or AcoustID keys.
- AcoustID application API key belongs in encrypted/local Settings prefs only.
- Rotate any password that was pasted into chat.
