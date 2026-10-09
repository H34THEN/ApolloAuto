#!/usr/bin/env python3
"""Kenney Sci-Fi UI asset inventory for Apollo II HUD integration."""

from __future__ import annotations

import json
import re
import struct
import zlib
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / "third_party" / "kenney" / "ui-pack-sci-fi"
OUT_JSON = ROOT / "docs" / "KENNEY_ASSET_INVENTORY.json"
OUT_SHEET_DIR = ROOT / "docs" / "kenney_previews"
OUT_MANIFEST_SEED = ROOT / "docs" / "HUD_ASSET_MANIFEST.md"


def png_size(path: Path) -> tuple[int | None, int | None]:
    try:
        with path.open("rb") as f:
            sig = f.read(8)
            if sig != b"\x89PNG\r\n\x1a\n":
                return None, None
            length = struct.unpack(">I", f.read(4))[0]
            chunk = f.read(4)
            if chunk != b"IHDR":
                return None, None
            data = f.read(length)
            w, h = struct.unpack(">II", data[:8])
            return int(w), int(h)
    except Exception:
        return None, None


def svg_viewbox(path: Path) -> tuple[float | None, float | None]:
    try:
        text = path.read_text(encoding="utf-8", errors="ignore")[:4000]
        m = re.search(r'viewBox\s*=\s*"([^"]+)"', text)
        if not m:
            m = re.search(r"viewBox\s*=\s*'([^']+)'", text)
        if not m:
            wm = re.search(r'\bwidth\s*=\s*"([\d.]+)"', text)
            hm = re.search(r'\bheight\s*=\s*"([\d.]+)"', text)
            if wm and hm:
                return float(wm.group(1)), float(hm.group(1))
            return None, None
        parts = m.group(1).replace(",", " ").split()
        if len(parts) >= 4:
            return float(parts[2]), float(parts[3])
        return None, None
    except Exception:
        return None, None


def detect_family(name: str) -> str:
    n = name.lower()
    if n.startswith("button_"):
        return "button"
    if n.startswith("bar_"):
        return "bar"
    if n.startswith("crosshair_"):
        return "crosshair"
    if n.startswith("panel_") or "panel" in n:
        return "panel"
    if n.startswith("slide_") or n.startswith("slider_"):
        return "slider"
    if n.startswith("icon_") or n.startswith("arrow_"):
        return "icon"
    return "other"


def detect_roles(name: str, family: str, w: float | None, h: float | None) -> list[str]:
    roles: list[str] = []
    n = name.lower()
    ar = (w / h) if w and h and h > 0 else None
    if family == "button":
        if "rectangle" in n:
            roles.append("primary_button")
        if "square" in n and "rectangle" not in n:
            roles.append("icon_button")
        if "header" in n:
            roles.append("section_header_control")
    if family == "bar":
        roles.append("progress_track")
        if n.endswith("_l") or n.endswith("_m") or n.endswith("_r"):
            roles.append("segmented_bar")
        if "small" in n:
            roles.append("compact_meter")
        if "large" in n:
            roles.append("timeline")
    if family == "crosshair":
        roles.append("artwork_frame_ornament")
        roles.append("visualizer_frame")
    if ar and ar >= 2.5:
        roles.append("wide_control")
    if ar and 0.85 <= ar <= 1.15:
        roles.append("square_control")
    return sorted(set(roles))


def color_variant(rel: str) -> str | None:
    parts = Path(rel).parts
    for p in parts:
        if p in {"Blue", "Green", "Red", "Yellow", "Grey", "Extra"}:
            return p
    return None


def resolution_bucket(rel: str) -> str | None:
    parts = Path(rel).parts
    for p in parts:
        if p in {"Default", "Double"}:
            return p
    return None


def segment_role(name: str) -> str | None:
    stem = Path(name).stem.lower()
    if stem.endswith("_l"):
        return "left"
    if stem.endswith("_m"):
        return "middle"
    if stem.endswith("_r"):
        return "right"
    return None


def main() -> None:
    if not PACK.exists():
        raise SystemExit(f"Pack not found: {PACK}")

    assets = []
    for path in sorted(PACK.rglob("*")):
        if not path.is_file():
            continue
        if path.suffix.lower() not in {".png", ".svg", ".ttf"}:
            continue
        rel = str(path.relative_to(PACK))
        ext = path.suffix.lower().lstrip(".")
        w = h = None
        if ext == "png":
            w, h = png_size(path)
        elif ext == "svg":
            w, h = svg_viewbox(path)
        family = detect_family(path.name)
        entry = {
            "relative_path": rel,
            "filename": path.name,
            "extension": ext,
            "width": w,
            "height": h,
            "aspect_ratio": round(w / h, 3) if w and h and h else None,
            "color_variant": color_variant(rel),
            "resolution": resolution_bucket(rel),
            "asset_family": family,
            "segment": segment_role(path.name),
            "candidate_roles": detect_roles(path.name, family, w, h),
        }
        assets.append(entry)

    families = defaultdict(int)
    for a in assets:
        families[a["asset_family"]] += 1

    # Recommended picks for Apollo (Blue/Default preferred for tinting baseline)
    recommendations = []
    preferred = [
        ("button_square_header_blade_rectangle.png", "ApolloHudButton wide"),
        ("button_square_header_notch_square.png", "ApolloHudIconButton"),
        ("button_square_header_small_rectangle.png", "ApolloHudTab / compact button"),
        ("bar_round_gloss_large.png", "ApolloHudProgress timeline"),
        ("bar_square_small.png", "ApolloHudProgress compact / EQ meter"),
        ("bar_round_gloss_large_l.png", "segmented timeline left"),
        ("bar_round_gloss_large_m.png", "segmented timeline mid"),
        ("bar_round_gloss_large_r.png", "segmented timeline right"),
        ("crosshair_color_a.png", "ApolloArtworkFrame ornament"),
        ("crosshair_color_c.png", "ApolloAudioSpectrumFrame ornament"),
    ]
    index = {(a["filename"], a.get("color_variant"), a.get("resolution")): a for a in assets}
    for fname, role in preferred:
        hit = next(
            (
                a
                for a in assets
                if a["filename"] == fname
                and a.get("color_variant") == "Blue"
                and a.get("resolution") == "Default"
                and a["extension"] == "png"
            ),
            None,
        )
        if hit:
            recommendations.append({"role": role, **hit})

    payload = {
        "source_archive": str(Path.home() / "Downloads" / "kenney_ui-pack-space-expansion.zip"),
        "extracted_root": str(PACK),
        "license": "CC0 — Kenney UI Pack: Sci-fi (2.0)",
        "counts": {
            "total_inventoried": len(assets),
            "png": sum(1 for a in assets if a["extension"] == "png"),
            "svg": sum(1 for a in assets if a["extension"] == "svg"),
            "ttf": sum(1 for a in assets if a["extension"] == "ttf"),
            "families": dict(families),
        },
        "recommendations": recommendations,
        "assets": assets,
    }
    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    OUT_JSON.write_text(json.dumps(payload, indent=2), encoding="utf-8")

    # Lightweight text contact sheet (filenames + dims) — no Pillow dependency required
    OUT_SHEET_DIR.mkdir(parents=True, exist_ok=True)
    lines = ["# Kenney Blue/Default contact sheet (text)", ""]
    for a in assets:
        if a.get("color_variant") == "Blue" and a.get("resolution") == "Default" and a["extension"] == "png":
            lines.append(f"- {a['filename']}  {a['width']}x{a['height']}  roles={','.join(a['candidate_roles'])}")
    (OUT_SHEET_DIR / "blue_default_contact_sheet.md").write_text("\n".join(lines), encoding="utf-8")

    print(f"Wrote {OUT_JSON} ({len(assets)} assets)")
    print(f"Recommendations: {len(recommendations)}")
    for r in recommendations:
        print(f"  - {r['role']}: {r['relative_path']} ({r['width']}x{r['height']})")


if __name__ == "__main__":
    main()
