# Apollo II V2 — Test Report

**Branch:** `feature/apollo-v2-visualizers-library`  
**Date:** 2026-10-09  
**Device:** Metroid A024 (`00021157P002695`) when ADB available

## Unit tests
| Suite | Result |
|--------|--------|
| `FilenameMetadataParserTest` | Run with `:app:testDebugUnitTest` |
| `M3uParserTest` | Relative / Windows / duplicate / ambiguous paths |

## Compile / install
| Check | Result |
|--------|--------|
| `:app:compileDebugKotlin` | Pass (post CarMode chips fix) |
| `:app:assembleDebug` + `adb install -r` | Per final install step |
| App launch | Per final install step |

## Feature verification

### Library sources
| Item | Status |
|------|--------|
| Settings → Library matrix | Implemented |
| CHANGE PHONE FOLDER (not USB label) | Implemented |
| Multi-source merge | Implemented |
| Home ALL/PHONE/USB filter | Implemented |
| Physical non-default phone folder | **Device pending user folder pick** |
| SSD disconnect retain catalog | Partial (mark offline API present; hardware pending) |

### M3U
| Item | Status |
|------|--------|
| Parser + resolver unit tests | Implemented |
| IMPORT M3U UI | Implemented |
| Auto-discover on SAF scan | Implemented |
| Export M3U8 API | Implemented (picker UI minimal) |
| Android Auto browse imported lists | Uses existing playlist browse — **DHU/car unverified** |

### Visualizers
| Item | Status |
|------|--------|
| Six SIGNAL modes preserved | Code present |
| Six cosmic/rave scenes | Code present |
| CaptureSource labeling | Code present |
| Immersive activity + Car IMMERSIVE | Code present |
| Live reactivity on device | **Needs playback + RECORD_AUDIO grant** |
| AA projection of immersive viz | **Not claimed** (compliant Media3 browse only) |

## Explicitly unverified
- Desktop Head Unit / real head-unit Android Auto
- Large library performance (>10k tracks)
- Photosensitivity lab testing
- Thermal throttling under HIGH quality
