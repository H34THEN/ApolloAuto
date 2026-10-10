package com.apolloauto.player.media

enum class CaptureSource {
    PLAYBACK_SESSION,
    DECORATIVE
    // MICROPHONE is Planned — not implemented; would require explicit permission UX.
}

fun CaptureSource.shortLabel(): String = when (this) {
    CaptureSource.PLAYBACK_SESSION -> "LIVE SESSION"
    CaptureSource.DECORATIVE -> "DECORATIVE IDLE"
}

/**
 * Shared music-analysis snapshot for all visualizer scenes.
 * Optional advanced fields (chroma / tempo / centroid) are Planned — not present.
 */
data class AudioFeatures(
    val subBass: Float = 0f,
    val bass: Float = 0f,
    val lowMid: Float = 0f,
    val upperMid: Float = 0f,
    val presence: Float = 0f,
    val treble: Float = 0f,
    /** Legacy alias average of lowMid+upperMid for older scenes */
    val mid: Float = 0f,
    /** Legacy alias for treble */
    val high: Float = 0f,
    val rms: Float = 0f,
    val peak: Float = 0f,
    val spectralFlux: Float = 0f,
    val onset: Float = 0f,
    /** 1 = fully silent / idle, 0 = loud */
    val silence: Float = 1f
) {
    fun withProfile(p: TranceResponseProfile): AudioFeatures {
        val m = p.multipliers
        return copy(
            subBass = (subBass * m.subBass).coerceIn(0f, 1f),
            bass = (bass * m.bass).coerceIn(0f, 1f),
            lowMid = (lowMid * m.lowMid).coerceIn(0f, 1f),
            upperMid = (upperMid * m.upperMid).coerceIn(0f, 1f),
            presence = (presence * m.presence).coerceIn(0f, 1f),
            treble = (treble * m.treble).coerceIn(0f, 1f),
            mid = (mid * ((m.lowMid + m.upperMid) * 0.5f)).coerceIn(0f, 1f),
            high = (high * m.treble).coerceIn(0f, 1f),
            rms = (rms * m.energy).coerceIn(0f, 1f),
            peak = (peak * m.energy).coerceIn(0f, 1f),
            spectralFlux = (spectralFlux * m.transient).coerceIn(0f, 1f),
            onset = (onset * m.transient).coerceIn(0f, 1f)
        )
    }
}

data class BandMultipliers(
    val subBass: Float = 1f,
    val bass: Float = 1f,
    val lowMid: Float = 1f,
    val upperMid: Float = 1f,
    val presence: Float = 1f,
    val treble: Float = 1f,
    val energy: Float = 1f,
    val transient: Float = 1f,
    val trail: Float = 1f,
    val particleDensity: Float = 1f,
    val attack: Float = 1f,
    val release: Float = 1f
)

enum class TranceResponseProfile(val title: String, val multipliers: BandMultipliers) {
    UPLIFTING(
        "Uplifting Trance",
        BandMultipliers(
            bass = 0.9f, upperMid = 1.25f, presence = 1.2f, treble = 1.1f,
            transient = 0.75f, trail = 1.4f, particleDensity = 1.1f,
            attack = 0.7f, release = 0.55f
        )
    ),
    PROGRESSIVE(
        "Progressive Trance",
        BandMultipliers(
            bass = 1.0f, lowMid = 1.1f, upperMid = 1.05f,
            transient = 0.85f, trail = 1.15f, particleDensity = 0.95f,
            attack = 0.55f, release = 0.45f
        )
    ),
    PSYTRANCE(
        "Psytrance",
        BandMultipliers(
            subBass = 1.35f, bass = 1.4f, treble = 1.2f,
            transient = 1.45f, trail = 0.85f, particleDensity = 1.45f,
            attack = 1.2f, release = 1.1f
        )
    ),
    GOA(
        "Goa",
        BandMultipliers(
            subBass = 1.2f, bass = 1.15f, upperMid = 1.2f, presence = 1.25f,
            transient = 1.2f, trail = 1.1f, particleDensity = 1.25f
        )
    ),
    HARD(
        "Hard Trance",
        BandMultipliers(
            bass = 1.35f, presence = 1.15f, treble = 1.15f,
            transient = 1.5f, trail = 0.75f, particleDensity = 1.2f,
            attack = 1.25f, release = 1.15f
        )
    ),
    MELODIC_TECHNO(
        "Melodic Techno",
        BandMultipliers(
            bass = 1.1f, lowMid = 1.15f, upperMid = 1.1f,
            transient = 0.9f, trail = 1.2f, particleDensity = 1.0f
        )
    ),
    AMBIENT(
        "Ambient / Downtempo",
        BandMultipliers(
            bass = 0.7f, upperMid = 0.9f, treble = 0.85f,
            energy = 0.75f, transient = 0.45f, trail = 1.6f,
            particleDensity = 0.7f, attack = 0.35f, release = 0.3f
        )
    )
}

enum class VizPaletteMode {
    THEME,
    RAINBOW,
    SPECTRAL,
    ALBUM_ART,
    SCENE
}

enum class VizQualityTier(val particleScale: Float, val meshScale: Float, val targetFpsHint: Int) {
    ECO(0.55f, 0.6f, 30),
    BALANCED(1f, 1f, 60),
    HIGH(1.35f, 1.25f, 60)
}

/**
 * Pure band extraction from normalized FFT magnitudes (0..1), used by runtime + tests.
 */
object SpectralBandExtractor {
    fun extract(fft: FloatArray): BandEnergies {
        if (fft.isEmpty()) return BandEnergies()
        fun avg(fromFrac: Float, toFrac: Float): Float {
            val a = (fft.size * fromFrac).toInt().coerceIn(0, fft.lastIndex)
            val b = (fft.size * toFrac).toInt().coerceIn(a + 1, fft.size)
            var s = 0f
            for (i in a until b) s += fft[i]
            return (s / (b - a)).coerceIn(0f, 1f)
        }
        val sub = avg(0f, 0.06f)
        val bass = avg(0.06f, 0.16f)
        val lowMid = avg(0.16f, 0.32f)
        val upperMid = avg(0.32f, 0.52f)
        val presence = avg(0.52f, 0.72f)
        val treble = avg(0.72f, 1f)
        return BandEnergies(
            subBass = sub,
            bass = bass,
            lowMid = lowMid,
            upperMid = upperMid,
            presence = presence,
            treble = treble,
            mid = ((lowMid + upperMid) * 0.5f).coerceIn(0f, 1f),
            high = treble
        )
    }

    fun flux(prev: FloatArray?, current: FloatArray): Float {
        if (prev == null || prev.size != current.size || current.isEmpty()) return 0f
        var sum = 0f
        for (i in current.indices) {
            sum += (current[i] - prev[i]).coerceAtLeast(0f)
        }
        return (sum / current.size * 2.2f).coerceIn(0f, 1f)
    }
}

data class BandEnergies(
    val subBass: Float = 0f,
    val bass: Float = 0f,
    val lowMid: Float = 0f,
    val upperMid: Float = 0f,
    val presence: Float = 0f,
    val treble: Float = 0f,
    val mid: Float = 0f,
    val high: Float = 0f
)
