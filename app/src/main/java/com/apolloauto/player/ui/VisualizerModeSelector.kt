package com.apolloauto.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.media.VisualizerMode
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudFont
import com.apolloauto.player.theme.LocalHudPalette

enum class VisualizerCategory(val title: String) {
    SIGNAL("SIGNAL"),
    SPECTR("SPECTR"),
    COSMIC("COSMIC"),
    RAVE("RAVE"),
    MV2("MV2"),
    DNA("DNA"),
    DROP("DROP"),
    XR3("XR3")
}

data class VisualizerModeEntry(
    val mode: VisualizerMode,
    val category: VisualizerCategory,
    val shortName: String,
    val description: String
)

object VisualizerModeCatalog {
    val all: List<VisualizerModeEntry> = listOf(
        VisualizerModeEntry(VisualizerMode.SPECTRUM, VisualizerCategory.SIGNAL, "FFT", "Full spectrum bars"),
        VisualizerModeEntry(VisualizerMode.BARS, VisualizerCategory.SIGNAL, "BARS", "Classic level meters"),
        VisualizerModeEntry(VisualizerMode.WAVEFORM, VisualizerCategory.SIGNAL, "WAVE", "Oscilloscope trace"),
        VisualizerModeEntry(VisualizerMode.CIRCULAR, VisualizerCategory.SIGNAL, "ORB", "Radial FFT ring"),
        VisualizerModeEntry(VisualizerMode.RINGS, VisualizerCategory.SIGNAL, "RINGS", "Concentric energy rings"),
        VisualizerModeEntry(VisualizerMode.LEVEL, VisualizerCategory.SIGNAL, "VU", "Dual VU meters"),
        VisualizerModeEntry(VisualizerMode.MINIMAL, VisualizerCategory.SIGNAL, "MIN", "Clean level strips"),
        VisualizerModeEntry(
            VisualizerMode.WAVEBARS,
            VisualizerCategory.SPECTR,
            "WBAR",
            "Symmetric seewav-style bars (audiospectr)"
        ),
        VisualizerModeEntry(
            VisualizerMode.MIRROR_BARS,
            VisualizerCategory.SPECTR,
            "MIRR",
            "Mirrored neon spectrum (audiospectr)"
        ),
        VisualizerModeEntry(
            VisualizerMode.PULSE,
            VisualizerCategory.SPECTR,
            "PULSE",
            "Bass rings + spokes (audiospectr)"
        ),
        VisualizerModeEntry(
            VisualizerMode.PARTICLES,
            VisualizerCategory.SPECTR,
            "DOTS",
            "Radial sparks (audiospectr)"
        ),
        VisualizerModeEntry(
            VisualizerMode.HYPERDRIVE,
            VisualizerCategory.COSMIC,
            "WIRE",
            "Wireframe audio mesh (nixonyh-inspired)"
        ),
        VisualizerModeEntry(VisualizerMode.NEBULA, VisualizerCategory.COSMIC, "NEBULA", "Soft gas cloud drift"),
        VisualizerModeEntry(VisualizerMode.SIGNAL_OCEAN, VisualizerCategory.COSMIC, "OCEAN", "Wave mesh horizon"),
        VisualizerModeEntry(VisualizerMode.PLASMA_HEART, VisualizerCategory.COSMIC, "PLASMA", "Pulsing reactor core"),
        VisualizerModeEntry(VisualizerMode.RAVE_CORE, VisualizerCategory.RAVE, "CORE", "Industrial pillar grid"),
        VisualizerModeEntry(VisualizerMode.FRACTURE, VisualizerCategory.RAVE, "FRACT", "Kaleidoscope shards"),
        VisualizerModeEntry(VisualizerMode.DECORATIVE, VisualizerCategory.RAVE, "DECO", "Ambient idle sweep"),
        VisualizerModeEntry(
            VisualizerMode.BASS_NATION,
            VisualizerCategory.MV2,
            "NATION",
            "Radial HSB bars (MusicVisualizerV2)"
        ),
        VisualizerModeEntry(
            VisualizerMode.BUBBLE_BASS,
            VisualizerCategory.MV2,
            "BUBBLE",
            "Floating FFT bubbles (MusicVisualizerV2)"
        ),
        VisualizerModeEntry(
            VisualizerMode.CIRCULAR_PULSE,
            VisualizerCategory.MV2,
            "CPULSE",
            "Radial spokes + core (MusicVisualizerV2)"
        ),
        VisualizerModeEntry(
            VisualizerMode.COLOR_SIGNAL,
            VisualizerCategory.MV2,
            "CSIG",
            "Zigzag color spectrum (MusicVisualizerV2)"
        ),
        VisualizerModeEntry(
            VisualizerMode.DUAL_SIGNAL,
            VisualizerCategory.MV2,
            "DUAL",
            "Mirrored dual spectrum (MusicVisualizerV2)"
        ),
        VisualizerModeEntry(
            VisualizerMode.MIXED_WAVE,
            VisualizerCategory.MV2,
            "MIXED",
            "Sine-warped spectrum (MusicVisualizerV2)"
        ),
        VisualizerModeEntry(
            VisualizerMode.SHAPE_WAVE,
            VisualizerCategory.MV2,
            "SHAPE",
            "Reactive shape grid (MusicVisualizerV2)"
        ),
        VisualizerModeEntry(
            VisualizerMode.STAR_FIELD,
            VisualizerCategory.MV2,
            "STARS",
            "Warp starfield (MusicVisualizerV2)"
        ),
        VisualizerModeEntry(
            VisualizerMode.STARS2,
            VisualizerCategory.MV2,
            "STARS2",
            "Streaking FFT stars (mostafa7904-inspired)"
        ),
        VisualizerModeEntry(
            VisualizerMode.DNA_HELIX,
            VisualizerCategory.DNA,
            "HELIX",
            "Double-helix waveform (MusicDNA-inspired)"
        ),
        VisualizerModeEntry(
            VisualizerMode.DNA_WAVE,
            VisualizerCategory.DNA,
            "STRAND",
            "Twisted spectrum ribbon"
        ),
        VisualizerModeEntry(
            VisualizerMode.BEAT_TUNNEL,
            VisualizerCategory.DROP,
            "TUNNEL",
            "Beat warp rings (MilkDrop-inspired)"
        ),
        VisualizerModeEntry(
            VisualizerMode.BEAT_CASCADE,
            VisualizerCategory.DROP,
            "DROP",
            "Cascading beat columns"
        ),
        VisualizerModeEntry(
            VisualizerMode.XR3_OSCILLO,
            VisualizerCategory.XR3,
            "OSC",
            "Stereo oscilloscope (XR3-inspired)"
        ),
        VisualizerModeEntry(
            VisualizerMode.XR3_CIRCLE,
            VisualizerCategory.XR3,
            "CIRC",
            "Circle with lines (XR3-inspired)"
        ),
        VisualizerModeEntry(
            VisualizerMode.XR3_POLY,
            VisualizerCategory.XR3,
            "POLY",
            "Polyspiral / rosette (XR3-inspired)"
        ),
        VisualizerModeEntry(
            VisualizerMode.XR3_BARS,
            VisualizerCategory.XR3,
            "XBARS",
            "Classic spectrum + peaks (XR3-inspired)"
        ),
        VisualizerModeEntry(
            VisualizerMode.XR3_VU,
            VisualizerCategory.XR3,
            "XVU",
            "Dual VU meters (XR3-inspired)"
        ),
        VisualizerModeEntry(
            VisualizerMode.XR3_SIERPINSKI,
            VisualizerCategory.XR3,
            "SIER",
            "Sierpinski triangles (XR3-inspired)"
        ),
        VisualizerModeEntry(
            VisualizerMode.XR3_JULIA,
            VisualizerCategory.XR3,
            "JULIA",
            "Julia-set point field (XR3-inspired)"
        ),
        VisualizerModeEntry(
            VisualizerMode.XR3_SPRITE,
            VisualizerCategory.XR3,
            "SPHERE",
            "Wireframe sprite sphere (XR3-inspired)"
        )
    )

    /** Modes cycled by immersive next / randomizer (skip idle-only variants). */
    val rotatable: List<VisualizerMode> = all
        .map { it.mode }
        .filter { it != VisualizerMode.DECORATIVE && it != VisualizerMode.MINIMAL }

    fun entry(mode: VisualizerMode): VisualizerModeEntry =
        all.firstOrNull { it.mode == mode } ?: all.first()

    fun compactLabel(mode: VisualizerMode): String = entry(mode).shortName

    fun activePool(enabled: Set<VisualizerMode>): List<VisualizerMode> {
        val pool = rotatable.filter { it in enabled }
        return pool.ifEmpty {
            rotatable.take(1).ifEmpty { listOf(VisualizerMode.SPECTRUM) }
        }
    }

    fun nextMode(current: VisualizerMode, enabled: Set<VisualizerMode> = VisualizerMode.entries.toSet()): VisualizerMode {
        val list = activePool(enabled)
        val idx = list.indexOf(current).let { if (it < 0) 0 else it }
        return list[(idx + 1) % list.size]
    }

    fun randomMode(
        excluding: VisualizerMode? = null,
        enabled: Set<VisualizerMode> = VisualizerMode.entries.toSet()
    ): VisualizerMode {
        val pool = activePool(enabled).filter { it != excluding }.ifEmpty { activePool(enabled) }
        return pool.random()
    }
}

@Composable
fun VisualizerModeSelector(
    selected: VisualizerMode,
    onSelect: (VisualizerMode) -> Unit,
    modifier: Modifier = Modifier,
    showDescriptions: Boolean = true,
    enabledModes: Set<VisualizerMode>? = null
) {
    val p = LocalHudPalette.current
    val hud = LocalHudFont.current
    val visible = if (enabledModes == null) {
        VisualizerModeCatalog.all
    } else {
        VisualizerModeCatalog.all.filter { it.mode in enabledModes }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VisualizerCategory.entries.forEach { cat ->
            val entries = visible.filter { it.category == cat }
            if (entries.isEmpty()) return@forEach
            Text(
                cat.title,
                color = p.primary,
                fontFamily = hud,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                entries
                    .chunked(3)
                    .forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { entry ->
                                ModeChipCell(
                                    entry = entry,
                                    selected = selected == entry.mode,
                                    showDescription = showDescriptions,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelect(entry.mode) }
                                )
                            }
                            repeat(3 - row.size) {
                                Row(Modifier.weight(1f)) {}
                            }
                        }
                    }
            }
        }
    }
}

@Composable
fun VisualizerModeCompactChips(
    selected: VisualizerMode,
    onSelect: (VisualizerMode) -> Unit,
    modifier: Modifier = Modifier,
    enabledModes: Set<VisualizerMode>? = null
) {
    val scroll = rememberScrollState()
    val entries = if (enabledModes == null) {
        VisualizerModeCatalog.all
    } else {
        VisualizerModeCatalog.all.filter { it.mode in enabledModes }
    }
    Row(
        modifier = modifier.horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        entries.forEach { entry ->
            CompactModeChip(
                label = entry.shortName,
                selected = selected == entry.mode,
                onClick = { onSelect(entry.mode) }
            )
        }
    }
}

@Composable
private fun ModeChipCell(
    entry: VisualizerModeEntry,
    selected: Boolean,
    showDescription: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val p = LocalHudPalette.current
    val hud = LocalHudFont.current
    val body = LocalBodyFont.current
    Column(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .border(
                1.dp,
                if (selected) p.accent else p.primary.copy(alpha = 0.25f),
                RoundedCornerShape(4.dp)
            )
            .background(if (selected) p.accent.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Text(
            entry.shortName,
            color = if (selected) p.soft else p.dim,
            fontFamily = hud,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (showDescription) {
            Text(
                entry.description,
                color = p.dim.copy(alpha = if (selected) 0.9f else 0.65f),
                fontFamily = body,
                fontSize = 9.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CompactModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Text(
        label,
        color = if (selected) p.soft else p.dim,
        fontFamily = body,
        fontSize = 9.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .border(
                1.dp,
                if (selected) p.accent else p.dim.copy(alpha = 0.35f),
                RoundedCornerShape(3.dp)
            )
            .background(
                if (selected) p.accent.copy(alpha = 0.18f)
                else p.voidBlack.copy(alpha = 0.45f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    )
}
