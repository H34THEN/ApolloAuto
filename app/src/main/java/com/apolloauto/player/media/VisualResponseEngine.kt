package com.apolloauto.player.media

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Shared audio→visual response helpers (V5.2).
 * Does not capture audio — maps existing [AudioFeatures] into expressive, band-specific drives.
 * Principles extracted from LIQUID / SPIRAL / POLY: large geometry deltas, not alpha-only.
 */

enum class VisualIntensity(val title: String, val gain: Float) {
    CALM("Calm", 0.65f),
    BALANCED("Balanced", 1f),
    REACTIVE("Reactive", 1.35f),
    HYPER("Hyper", 1.75f)
}

data class ResponseEnvelope(
    val attack: Float = 0.7f,
    val release: Float = 0.22f
) {
    fun step(current: Float, target: Float): Float {
        val a = attack.coerceIn(0.05f, 1f)
        val r = release.coerceIn(0.05f, 1f)
        return if (target > current) {
            current + (target - current) * a
        } else {
            current + (target - current) * r
        }
    }
}

object VisualResponseMapper {
    /** Compress soft signals upward so quiet tracks still move geometry. */
    fun expand(v: Float, gamma: Float = 0.72f, floor: Float = 0.02f): Float {
        val x = v.coerceIn(0f, 1f)
        if (x <= floor) return 0f
        return ((x - floor) / (1f - floor)).pow(gamma).coerceIn(0f, 1f)
    }

    fun bandDrive(features: AudioFeatures, intensity: VisualIntensity = VisualIntensity.REACTIVE): BandDrive {
        val g = intensity.gain
        return BandDrive(
            sub = expand(features.subBass) * g,
            bass = expand(features.bass) * g,
            lowMid = expand(features.lowMid) * g,
            upperMid = expand(features.upperMid) * g,
            presence = expand(features.presence) * g,
            treble = expand(features.treble) * g,
            rms = expand(features.rms, gamma = 0.85f) * g,
            onset = expand(features.onset, gamma = 0.55f) * g,
            flux = expand(features.spectralFlux, gamma = 0.6f) * g,
            silence = features.silence.coerceIn(0f, 1f)
        )
    }

    /** Kick-like pressure for large-scale motion (fast attack feel). */
    fun kickPressure(d: BandDrive): Float =
        (d.sub * 0.55f + d.bass * 0.85f + d.onset * 1.1f).coerceIn(0f, 2.2f)

    /** Fine sparkle / edge energy. */
    fun sparkle(d: BandDrive): Float =
        (d.treble * 0.9f + d.presence * 0.45f + d.flux * 0.35f).coerceIn(0f, 1.8f)

    /** Photosensitivity-safe pulse (no full-screen flash). */
    fun safePulse(onset: Float, photosensitive: Boolean): Float {
        val p = onset.coerceIn(0f, 1f)
        return if (photosensitive) (p * 0.35f).coerceAtMost(0.4f) else p
    }
}

data class BandDrive(
    val sub: Float = 0f,
    val bass: Float = 0f,
    val lowMid: Float = 0f,
    val upperMid: Float = 0f,
    val presence: Float = 0f,
    val treble: Float = 0f,
    val rms: Float = 0f,
    val onset: Float = 0f,
    val flux: Float = 0f,
    val silence: Float = 1f
) {
    val energy: Float get() = max(rms, (bass + lowMid) * 0.5f)
}

enum class SpiralFillMode { FIT, FILL }

/**
 * Per-mode visualizer size presets (Immersive cycle + Settings).
 * Applied via DrawScope.scale on geometry only — backdrop stays full-bleed.
 */
enum class VizSizePreset(val title: String, val shortLabel: String, val scale: Float) {
    XS("Compact", "XS", 0.52f),
    S("Small", "S", 0.68f),
    M("Medium", "M", 0.84f),
    L("Large", "L", 1.0f),
    XL("Max", "XL", 1.18f);

    fun next(): VizSizePreset = entries[(ordinal + 1) % entries.size]
}

/** Map retired / aliased visualizer modes to their live replacements. */
fun resolveVisualizerMode(mode: VisualizerMode): VisualizerMode = when (mode) {
    VisualizerMode.XR3_SIERPINSKI -> VisualizerMode.XR3_POLY
    VisualizerMode.PRISM_RAIN -> VisualizerMode.TOPO_CONTOUR
    VisualizerMode.STAR_FIELD, VisualizerMode.STARS2 -> VisualizerMode.SPACE_WARP
    else -> mode
}

/** Viewport scale for spiral/poly scenes — FILL uses max side so landscape is occupied. */
fun spiralViewportDim(width: Float, height: Float, mode: SpiralFillMode): Float =
    when (mode) {
        SpiralFillMode.FIT -> min(width, height)
        SpiralFillMode.FILL -> max(width, height)
    }

/**
 * Immersive / Car Mode span — longer edge (FILL).
 * Per-mode size is applied by [VisualizerCanvas] via DrawScope.scale, not here
 * (avoids double-scaling and animation trail resets).
 */
fun immersiveViewportDim(width: Float, height: Float): Float =
    spiralViewportDim(width, height, SpiralFillMode.FILL)

/** Safe HSV hue in 0..360 (Kotlin `%` can be negative). */
fun safeHue(degrees: Float): Float {
    if (!degrees.isFinite()) return 0f
    val m = degrees % 360f
    return if (m < 0f) m + 360f else m
}

/** Boost FFT bins for geometry (clamped); gain is relative to REACTIVE baseline. */
fun boostSpectrum(fft: FloatArray, intensity: VisualIntensity): FloatArray {
    val g = intensity.gain / VisualIntensity.REACTIVE.gain
    if (kotlin.math.abs(g - 1f) < 0.01f || fft.isEmpty()) return fft
    return FloatArray(fft.size) { i -> (fft[i] * g).coerceIn(0f, 1.6f) }
}
