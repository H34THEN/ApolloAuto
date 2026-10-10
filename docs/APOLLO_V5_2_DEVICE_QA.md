# Apollo V5.2 — Device QA

**Package:** `com.apolloauto.player.debug`  
**APK:** `app/build/outputs/apk/debug/app-debug.apk`  
**Device:** Metroid A024 `00021157P002695`

## Automated
| Test | Result |
|---|---|
| `VisualResponseEngineTest` | Passed |
| `SynthSmokeNoiseTest` | Passed |
| `VisualizerBackgroundPolicyTest` | Passed |
| `compileDebugKotlin` / `assembleDebug` | Passed |

## Physical device
| Test | Result |
|---|---|
| APK install | Passed |
| Idle SMOKE screenshot | Passed — `docs/qa/v5_2/car_smoke.png` (magenta vapor, not sine strokes; HSV crash fixed) |
| Idle POLY screenshot | Captured — `docs/qa/v5_2/car_poly.png` (FILL spiral idle arcs in Car HUD; music fill TBD) |
| SMOKE crash (HSV S>1) | Found + fixed before ship |
| Music playback reactivity (STAR/STAR2/SIER/RAIN/SMOKE) | **Blocked** — 0 tracks in library |
| LIQUID/SPIRAL/POLY regression with music | Pending tracks |
| 30-minute soak | Not executed |
| Android Auto | Not executed |

Do not mark music-reactivity tests passed without audible playback.
