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

/**
 * Gallery groups keyed by upstream GitHub source (or Apollo original).
 * Titles match repo paths so modes are easy to rediscover.
 */
enum class VisualizerCategory(val title: String) {
    APOLLO("apollo // original"),
    PROJECTM("projectM-visualizer/projectm"),
    MILKDROP("milkdrop2077/MilkDrop3"),
    GAURAVK("gauravk95/audio-visualizer-android"),
    BAUER("bradleybauer/music_visualizer"),
    MUSICDNA("harjot-oberai/MusicDNA"),
    AUDIOSPECTR("audiospectr / seewav-style"),
    MV2("MusicVisualizerV2"),
    MOSTAFA("mostafa7904/Stars-Audio-Visualizer"),
    XR3("goxr3plus/XR3Player"),
    NIXON("nixonyh // wire mesh"),
    TRANCE("apollo // trance reactor"),
    TOPO("apollo // techno topographic"),
    PSY("apollo // trippy / stylized"),
    EXPERIMENTAL("apollo // experimental"),
    FUN("apollo // fun")
}

data class VisualizerModeEntry(
    val mode: VisualizerMode,
    val category: VisualizerCategory,
    val shortName: String,
    val description: String
)

object VisualizerModeCatalog {
    /** Modes from the GitHub source packs — pinned first in Car Mode chips. */
    val sourcePackModes: Set<VisualizerMode> = setOf(
        VisualizerMode.PROJECTM_PLASMA,
        VisualizerMode.PROJECTM_SCOPE,
        VisualizerMode.BEAT_TUNNEL,
        VisualizerMode.BEAT_CASCADE,
        VisualizerMode.MILKDROP_WAVE,
        VisualizerMode.MILKDROP_ROTOR,
        VisualizerMode.AV_BAR,
        VisualizerMode.AV_BLAST,
        VisualizerMode.AV_BLOB,
        VisualizerMode.AV_WAVE,
        VisualizerMode.AV_CIRCLE_BARS,
        VisualizerMode.AV_HIFI,
        VisualizerMode.BAUER_FOUNTAIN,
        VisualizerMode.BAUER_FFT,
        VisualizerMode.BAUER_SPEC,
        VisualizerMode.DNA_SPIRAL,
        VisualizerMode.DNA_HELIX,
        VisualizerMode.DNA_WAVE,
        VisualizerMode.DNA_CIRCLE,
        VisualizerMode.LYRICS_RAINBOW,
        VisualizerMode.PARTY_MARQUEE,
        VisualizerMode.TOPO_CONTOUR,
        VisualizerMode.WIRE_TERRAIN,
        VisualizerMode.NEON_TOPO,
        VisualizerMode.PSY_KALEIDO,
        VisualizerMode.ACID_TUNNEL
    )

    val all: List<VisualizerModeEntry> = listOf(
        // --- apollo // original ---
        VisualizerModeEntry(VisualizerMode.SPECTRUM, VisualizerCategory.APOLLO, "FFT", "Full spectrum bars"),
        VisualizerModeEntry(VisualizerMode.BARS, VisualizerCategory.APOLLO, "BARS", "Classic level meters"),
        VisualizerModeEntry(VisualizerMode.WAVEFORM, VisualizerCategory.APOLLO, "WAVE", "Oscilloscope trace"),
        VisualizerModeEntry(VisualizerMode.CIRCULAR, VisualizerCategory.APOLLO, "ORB", "Radial FFT ring"),
        VisualizerModeEntry(VisualizerMode.RINGS, VisualizerCategory.APOLLO, "RINGS", "Concentric energy rings"),
        VisualizerModeEntry(VisualizerMode.LEVEL, VisualizerCategory.APOLLO, "VU", "Dual VU meters"),
        VisualizerModeEntry(VisualizerMode.MINIMAL, VisualizerCategory.APOLLO, "MIN", "Clean level strips"),
        VisualizerModeEntry(VisualizerMode.NEBULA, VisualizerCategory.APOLLO, "NEBULA", "Soft gas cloud drift"),
        VisualizerModeEntry(VisualizerMode.SIGNAL_OCEAN, VisualizerCategory.APOLLO, "OCEAN", "Wave mesh horizon"),
        VisualizerModeEntry(VisualizerMode.PLASMA_HEART, VisualizerCategory.APOLLO, "PLASMA", "Pulsing reactor core"),
        VisualizerModeEntry(VisualizerMode.RAVE_CORE, VisualizerCategory.APOLLO, "CORE", "Industrial pillar grid"),
        VisualizerModeEntry(VisualizerMode.FRACTURE, VisualizerCategory.APOLLO, "FRACT", "Kaleidoscope shards"),
        VisualizerModeEntry(VisualizerMode.DECORATIVE, VisualizerCategory.APOLLO, "DECO", "Ambient idle sweep"),
        // --- projectM-visualizer/projectm ---
        VisualizerModeEntry(
            VisualizerMode.PROJECTM_PLASMA,
            VisualizerCategory.PROJECTM,
            "PMPLAS",
            "Plasma warp rings (projectM look)"
        ),
        VisualizerModeEntry(
            VisualizerMode.PROJECTM_SCOPE,
            VisualizerCategory.PROJECTM,
            "PMSCOPE",
            "Classic waveform scope (projectM look)"
        ),
        // --- milkdrop2077/MilkDrop3 ---
        VisualizerModeEntry(
            VisualizerMode.BEAT_TUNNEL,
            VisualizerCategory.MILKDROP,
            "TUNNEL",
            "Beat warp rings"
        ),
        VisualizerModeEntry(
            VisualizerMode.BEAT_CASCADE,
            VisualizerCategory.MILKDROP,
            "DROP",
            "Cascading beat columns"
        ),
        VisualizerModeEntry(
            VisualizerMode.MILKDROP_WAVE,
            VisualizerCategory.MILKDROP,
            "MDWAVE",
            "Oscillating waveform mesh"
        ),
        VisualizerModeEntry(
            VisualizerMode.MILKDROP_ROTOR,
            VisualizerCategory.MILKDROP,
            "ROTOR",
            "Spinning spectrum arms"
        ),
        // --- gauravk95/audio-visualizer-android (Apache-2.0 ports) ---
        VisualizerModeEntry(VisualizerMode.AV_BAR, VisualizerCategory.GAURAVK, "AV-BAR", "BarVisualizer port"),
        VisualizerModeEntry(VisualizerMode.AV_BLAST, VisualizerCategory.GAURAVK, "AV-BLAST", "BlastVisualizer port"),
        VisualizerModeEntry(VisualizerMode.AV_BLOB, VisualizerCategory.GAURAVK, "AV-BLOB", "BlobVisualizer port"),
        VisualizerModeEntry(VisualizerMode.AV_WAVE, VisualizerCategory.GAURAVK, "AV-WAVE", "WaveVisualizer port"),
        VisualizerModeEntry(VisualizerMode.AV_CIRCLE_BARS, VisualizerCategory.GAURAVK, "AV-CIRC", "CircleLineVisualizer port"),
        VisualizerModeEntry(VisualizerMode.AV_HIFI, VisualizerCategory.GAURAVK, "AV-HIFI", "HiFiVisualizer port"),
        // --- bradleybauer/music_visualizer ---
        VisualizerModeEntry(
            VisualizerMode.BAUER_FOUNTAIN,
            VisualizerCategory.BAUER,
            "FOUNT",
            "Particle fountain"
        ),
        VisualizerModeEntry(
            VisualizerMode.BAUER_FFT,
            VisualizerCategory.BAUER,
            "B-FFT",
            "Log-spaced FFT ribbons"
        ),
        VisualizerModeEntry(
            VisualizerMode.BAUER_SPEC,
            VisualizerCategory.BAUER,
            "B-SPEC",
            "Scrolling spectrogram"
        ),
        // --- harjot-oberai/MusicDNA (CC BY-NC-SA; non-commercial) ---
        VisualizerModeEntry(
            VisualizerMode.DNA_SPIRAL,
            VisualizerCategory.MUSICDNA,
            "DNA",
            "Original MusicDNA spiral trail algorithm"
        ),
        VisualizerModeEntry(
            VisualizerMode.DNA_HELIX,
            VisualizerCategory.MUSICDNA,
            "HELIX",
            "Double-helix waveform"
        ),
        VisualizerModeEntry(
            VisualizerMode.DNA_WAVE,
            VisualizerCategory.MUSICDNA,
            "STRAND",
            "Twisted spectrum ribbon"
        ),
        VisualizerModeEntry(
            VisualizerMode.DNA_CIRCLE,
            VisualizerCategory.MUSICDNA,
            "DNACIRC",
            "Circular DNA bars"
        ),
        // --- audiospectr ---
        VisualizerModeEntry(
            VisualizerMode.WAVEBARS,
            VisualizerCategory.AUDIOSPECTR,
            "WBAR",
            "Symmetric seewav-style bars"
        ),
        VisualizerModeEntry(
            VisualizerMode.MIRROR_BARS,
            VisualizerCategory.AUDIOSPECTR,
            "MIRR",
            "Mirrored neon spectrum"
        ),
        VisualizerModeEntry(
            VisualizerMode.PULSE,
            VisualizerCategory.AUDIOSPECTR,
            "PULSE",
            "Bass rings + spokes"
        ),
        VisualizerModeEntry(
            VisualizerMode.PARTICLES,
            VisualizerCategory.AUDIOSPECTR,
            "DOTS",
            "Radial sparks"
        ),
        // --- MusicVisualizerV2 ---
        VisualizerModeEntry(
            VisualizerMode.BASS_NATION,
            VisualizerCategory.MV2,
            "NATION",
            "Radial HSB bars"
        ),
        VisualizerModeEntry(
            VisualizerMode.BUBBLE_BASS,
            VisualizerCategory.MV2,
            "BUBBLE",
            "Floating FFT bubbles"
        ),
        VisualizerModeEntry(
            VisualizerMode.CIRCULAR_PULSE,
            VisualizerCategory.MV2,
            "CPULSE",
            "Radial spokes + core"
        ),
        VisualizerModeEntry(
            VisualizerMode.COLOR_SIGNAL,
            VisualizerCategory.MV2,
            "CSIG",
            "Zigzag color spectrum"
        ),
        VisualizerModeEntry(
            VisualizerMode.DUAL_SIGNAL,
            VisualizerCategory.MV2,
            "DUAL",
            "Mirrored dual spectrum"
        ),
        VisualizerModeEntry(
            VisualizerMode.MIXED_WAVE,
            VisualizerCategory.MV2,
            "MIXED",
            "Sine-warped spectrum"
        ),
        VisualizerModeEntry(
            VisualizerMode.SHAPE_WAVE,
            VisualizerCategory.MV2,
            "SHAPE",
            "Reactive shape grid"
        ),
        VisualizerModeEntry(
            VisualizerMode.SPACE_WARP,
            VisualizerCategory.APOLLO,
            "WARP",
            "Cosmic reactive hyperspace warp"
        ),
        // --- nixonyh ---
        VisualizerModeEntry(
            VisualizerMode.HYPERDRIVE,
            VisualizerCategory.NIXON,
            "WIRE",
            "Wireframe audio mesh"
        ),
        // --- goxr3plus/XR3Player ---
        VisualizerModeEntry(
            VisualizerMode.XR3_OSCILLO,
            VisualizerCategory.XR3,
            "OSC",
            "Stereo oscilloscope"
        ),
        VisualizerModeEntry(
            VisualizerMode.XR3_CIRCLE,
            VisualizerCategory.XR3,
            "CIRC",
            "Circle with lines"
        ),
        VisualizerModeEntry(
            VisualizerMode.XR3_POLY,
            VisualizerCategory.XR3,
            "POLY",
            "Polyspiral / rosette"
        ),
        VisualizerModeEntry(
            VisualizerMode.XR3_BARS,
            VisualizerCategory.XR3,
            "XBARS",
            "Classic spectrum + peaks"
        ),
        VisualizerModeEntry(
            VisualizerMode.XR3_VU,
            VisualizerCategory.XR3,
            "XVU",
            "Dual VU meters"
        ),
        VisualizerModeEntry(
            VisualizerMode.XR3_JULIA,
            VisualizerCategory.XR3,
            "JULIA",
            "Julia-set point field"
        ),
        VisualizerModeEntry(
            VisualizerMode.XR3_SPRITE,
            VisualizerCategory.XR3,
            "SPHERE",
            "Wireframe sprite sphere"
        ),
        // --- apollo // trance / experimental / fun ---
        VisualizerModeEntry(VisualizerMode.CHROMATIC_PHI, VisualizerCategory.TRANCE, "PHI", "Golden-angle particle bloom"),
        VisualizerModeEntry(VisualizerMode.ECTOPLASM, VisualizerCategory.TRANCE, "SLIME", "Iridescent metaball ectoplasm"),
        VisualizerModeEntry(VisualizerMode.CIRC_INFINITY, VisualizerCategory.TRANCE, "CIRC∞", "Radial spectral infinity"),
        // PRISM_RAIN retired — chaotic / hard to read
        VisualizerModeEntry(VisualizerMode.SYNTH_SMOKE, VisualizerCategory.TRANCE, "SMOKE", "Billowing fBm vapor plumes"),
        VisualizerModeEntry(VisualizerMode.NOTE_SWARM, VisualizerCategory.TRANCE, "SWARM", "Per-band particle constellations"),
        VisualizerModeEntry(VisualizerMode.HARMONIC_MANDALA, VisualizerCategory.TRANCE, "MANDALA", "Symmetric harmonic petals"),
        VisualizerModeEntry(VisualizerMode.LIQUID_SPECTRUM, VisualizerCategory.TRANCE, "LIQUID", "Flowing liquid spectrum field"),
        VisualizerModeEntry(
            VisualizerMode.TOPO_CONTOUR,
            VisualizerCategory.TOPO,
            "TOPO",
            "Techno topographic contour map"
        ),
        VisualizerModeEntry(
            VisualizerMode.WIRE_TERRAIN,
            VisualizerCategory.TOPO,
            "TERRAIN",
            "Perspective wireframe terrain"
        ),
        VisualizerModeEntry(
            VisualizerMode.NEON_TOPO,
            VisualizerCategory.TOPO,
            "NEOTOPO",
            "Stacked neon elevation plates"
        ),
        VisualizerModeEntry(
            VisualizerMode.PSY_KALEIDO,
            VisualizerCategory.PSY,
            "KALEIDO",
            "Trippy kaleidoscope shards"
        ),
        VisualizerModeEntry(
            VisualizerMode.ACID_TUNNEL,
            VisualizerCategory.PSY,
            "ACID",
            "Acid spiral tunnel"
        ),
        VisualizerModeEntry(VisualizerMode.AUDIBLE_SPIRALS, VisualizerCategory.EXPERIMENTAL, "SPIRAL", "Archimedean audible spirals"),
        VisualizerModeEntry(VisualizerMode.FLUCTUS_FLOW, VisualizerCategory.EXPERIMENTAL, "FLUCTUS", "3D ribbon wave sculpture"),
        VisualizerModeEntry(VisualizerMode.WAVEFORM_VOID, VisualizerCategory.EXPERIMENTAL, "VOID", "Layered waveform landscape"),
        VisualizerModeEntry(VisualizerMode.OBSIDIAN_REACTOR, VisualizerCategory.EXPERIMENTAL, "OBSIDIAN", "Dark crystalline reactor"),
        VisualizerModeEntry(VisualizerMode.VISSONANCE_MATRIX, VisualizerCategory.EXPERIMENTAL, "VISSON", "Multi-mode spectral matrix"),
        VisualizerModeEntry(
            VisualizerMode.LYRICS_RAINBOW,
            VisualizerCategory.FUN,
            "LYRIC",
            "Rainbow E1D1C lyrics that pulse with the beat"
        ),
        VisualizerModeEntry(
            VisualizerMode.PARTY_MARQUEE,
            VisualizerCategory.FUN,
            "SCROLL",
            "Goofy scrolling messages for Car Mode funsies"
        )
    )

    /** Modes cycled by immersive next / randomizer (skip idle-only / retired). */
    val rotatable: List<VisualizerMode> = all
        .map { it.mode }
        .filter {
            it != VisualizerMode.DECORATIVE &&
                it != VisualizerMode.MINIMAL &&
                it != VisualizerMode.XR3_SIERPINSKI &&
                it != VisualizerMode.PRISM_RAIN &&
                it != VisualizerMode.STAR_FIELD &&
                it != VisualizerMode.STARS2
        }

    fun isRetired(mode: VisualizerMode): Boolean =
        mode == VisualizerMode.XR3_SIERPINSKI ||
            mode == VisualizerMode.PRISM_RAIN ||
            mode == VisualizerMode.STAR_FIELD ||
            mode == VisualizerMode.STARS2

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
    val pack = VisualizerModeCatalog.sourcePackModes
    val entries = (if (enabledModes == null) {
        VisualizerModeCatalog.all
    } else {
        VisualizerModeCatalog.all.filter { it.mode in enabledModes }
    }).sortedBy { if (it.mode in pack) 0 else 1 }
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
