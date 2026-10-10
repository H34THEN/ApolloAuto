# Android Auto Desktop Head Unit (DHU) — Pre-release Test Suite

Long-term local harness for reviewing Apollo’s **projected** Android Auto UI before a public beta cut.

Official docs: [Test using the Desktop Head Unit](https://developer.android.com/training/cars/testing/dhu)

## What this suite covers

| In scope (DHU) | Out of scope |
|---|---|
| Browse / Now Playing / Queue chrome | Immersive shaders / Viz-Only |
| Next / Prev / Play-Pause visibility | Removing Maps from AA dashboard |
| Label truncation on compact HUD | Car App Library template migration |
| Day / night contrast | Sobalai wireless radio quirks (still need car) |

## Layout

```
tools/aa-dhu/
  install_dhu.sh          # sdkmanager extras;google;auto
  check_prereqs.sh        # device + AA + Apollo + DHU binary
  run_dhu.sh              # launch interactive or scripted DHU
  capture_review.sh       # prereqs + walkthrough + gallery dump
  config/sobalai_6_9.ini  # ~6.9" compact landscape
  config/compact_800x480.ini
  scripts/walkthrough_media.txt
docs/release/aa-dhu-gallery/   # PNG captures + REVIEW.md
```

## One-time setup

```bash
export ANDROID_HOME=/path/to/android-sdk   # if needed
./tools/aa-dhu/install_dhu.sh
./tools/aa-dhu/check_prereqs.sh
```

On the phone (each session):

1. Android Auto → tap **Version** 10× → Developer settings  
2. Enable **Unknown sources** (sideloaded Apollo)  
3. Overflow → **Start head unit server**  
4. Keep the screen unlocked  

## Interactive review

```bash
./tools/aa-dhu/run_dhu.sh
# optional compact:
./tools/aa-dhu/run_dhu.sh --compact
# USB AOA (DHU 2.x recommended when adb tunnel fails):
./tools/aa-dhu/run_dhu.sh --usb
```

In the DHU console you can run `screenshot name.png`, `keycode media_next`, `tap x y`, `night`, `quit`.

## Scripted pre-release capture

```bash
./tools/aa-dhu/capture_review.sh
```

Writes PNGs under `docs/release/aa-dhu-gallery/`. Fill in `REVIEW.md` (approve / change requests).

## GitHub / CI

- **Unit tests** in CI validate browse tree + queue expand (no phone needed).  
- **DHU gallery** is a **local pre-release gate** — GitHub-hosted runners cannot attach your phone or show the DHU window.  
- Workflow `aa-dhu-prerelease.yml` verifies the harness files exist and runs the Media3-related unit tests on every PR touching AA/media or the tools tree.  
- Release checklist requires a filled `aa-dhu-gallery/REVIEW.md` before cutting the next public APK when AA UX changed.

## Approval loop

1. Run `capture_review.sh` (or interactive `run_dhu.sh`).  
2. Open gallery PNGs in the IDE / Finder.  
3. Mark each screen Approve / Change in `REVIEW.md`.  
4. Implement changes → re-capture → re-approve.  
5. Check the RELEASE_CHECKLIST DHU box.
