# M3U / M3U8 Playlist Engine (V2.2)

## Components
- `M3uParser` — `#EXTM3U`, `#EXTINF`, comments, blank lines; UTF-8 with legacy charset fallback
- `M3uPathResolver` — ranked match: playlist-relative → root-relative → suffix (Windows paths) → unambiguous filename; **ambiguous = no silent pick**
- `M3uPlaylistService` — SAF document preview/import + M3U8 export
- `PlaylistStore.createImported` — refreshes by `importUri` (no duplicate clones); never touches Car Favorites

## Discovery
`SafLibraryScanner` records `.m3u` / `.m3u8` files; coordinator can auto-import on scan.

## UI
- Lists → **IMPORT M3U** (document picker)
- Imported rows show `M3U` tag and missing-entry counts

## Playback / AA
Imported playlists resolve to catalog `Track` ids and use existing `PlayerController.playTracks` / Media3 browse (`PlaylistStore` in service). No separate engine.

## Unit tests
`M3uParserTest` — extended M3U, Windows paths, duplicates, ambiguous filenames.
