# Apollo II v1.0.0-beta.1 — Release Audit

**Repo:** https://github.com/H34THEN/ApolloAuto  
**Branch:** `release/apollo-ii-v1-beta`  
**Checkpoint tip:** `0b8dac9` (pre-ST repair) → subsequent ST01/ST03 fix commits  
**Package:** `com.apolloauto.player` (release) / `.debug` (debug)

## Feature status (honest)

| Area | Status | Notes |
|---|---|---|
| Core playback (Media3) | Implemented / Device tested | Phone install verified |
| Library scan (phone/SAF/USB) | Implemented / Device tested | Album-art collision fixed (hash keys) |
| Playlists + Car Favorites | Implemented | |
| M3U import/export | Implemented | |
| Equalizer / sleep timer | Implemented | |
| Android Auto discovery | Owner-confirmed | Visible in AA launcher |
| Android Auto browse/playback | Implemented / Needs owner car test | Service exported; MCT/DHU not fully exercised this session |
| Visualizer gallery | Implemented | Large catalog |
| ST01 GrafEq | **Repaired** / Partial device verify | Dense CRT field rewrite |
| ST02 lunchbreak | Implemented | Not reported broken |
| ST03 Cyber Fuji | **Repaired** / Partial device verify | Height-normalized layout |
| MILK//LAB (10) | Implemented / Experimental | Apollo originals; not XorDev ports |
| Tron Legacy (3) | Implemented / Experimental | |
| SPACE_WARP | Implemented / Improved | |
| LIQUID / POLY / SPIRAL | Implemented | Preserve as strong refs |
| SYNTH SMOKE | Implemented | |
| STAR/STAR2/SIER/RAIN | Retired aliases | Map to SPACE_WARP / POLY / TOPO |
| ECHOHUNT | Scaffold only | Hide as incomplete; no secrets |
| Signed release APK | Built | Local beta keystore (not in git) |
| Screenshots in README | **Not complete** | Capture pending |
| Public visibility change | **Not done** | Requires explicit approval |

## Blockers remaining for “public” claim
1. Owner visual QA of ST01/ST03 after repair  
2. Real-car Android Auto browse/playback confirmation  
3. README screenshots from device  
4. Explicit approval before making the private repo public  
