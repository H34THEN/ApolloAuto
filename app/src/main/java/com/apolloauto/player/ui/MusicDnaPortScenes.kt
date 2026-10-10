package com.apolloauto.player.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.apolloauto.player.media.AudioFeatures
import com.apolloauto.player.media.immersiveViewportDim
import com.apolloauto.player.media.safeHue
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Port of harjot-oberai/MusicDNA DNA spiral renderer
 * ([HomeActivity.updatePoints](https://github.com/harjot-oberai/MusicDNA)).
 *
 * License: CC BY-NC-SA — non-commercial use only. Apollo is not sold.
 * Algorithm adapted to Compose + float FFT (original used Visualizer byte pairs).
 */

private data class DnaDot(
    val x: Float,
    val y: Float,
    val r: Float,
    val hue: Float,
    var alpha: Float
)

private object MusicDnaTrail {
    private const val LOG_MAX = 4.15888308336 // ln(64)
    private val BASE = ln(4.0) / LOG_MAX
    private const val MAX_DOT = 0.5
    private const val TAU = PI * 2.0
    private val dots = ArrayList<DnaDot>(768)
    private val rng = Random(0xD1A)
    private var lastW = 0f
    private var lastH = 0f

    fun ensure(w: Float, h: Float) {
        if (w != lastW || h != lastH) {
            lastW = w
            lastH = h
            dots.clear()
        }
    }

    fun tick(
        fft: FloatArray,
        progress01: Float,
        phase: Float,
        features: AudioFeatures,
        playing: Boolean
    ) {
        if (lastW <= 1f || lastH <= 1f || fft.isEmpty()) return
        // Fade existing trail (bitmap persistence in original).
        val it = dots.iterator()
        while (it.hasNext()) {
            val d = it.next()
            d.alpha *= 0.982f
            if (d.alpha < 0.015f) it.remove()
        }
        if (!playing) return

        val span = immersiveViewportDim(lastW, lastH)
        val outerRadius = span * 0.42f
        // Continuous spin + song progress so the trail paints a spiral, not one stuck ray.
        val baseAngle = (
            PI - progress01.coerceIn(0f, 1f) * TAU +
                phase * 0.012
            ).toFloat()
        val midx = lastW / 2f
        val midy = lastH / 2f
        val minAudio = 0.06f + features.silence * 0.04f

        var maxA = 0.001f
        var minA = Float.MAX_VALUE
        val start = 8
        val end = fft.size
        for (a in start until end) {
            val amp = fft[a] * fft[a]
            if (amp > maxA) maxA = amp
            if (amp < minA) minA = amp
        }
        if (maxA <= 1e-6f) return

        val ratio = (span / 720f).coerceIn(0.7f, 1.8f)
        var added = 0
        for (a in start until end) {
            if (added > 72) break
            var amp = fft[a] * fft[a]
            amp = if (maxA != minA) (amp - minA) / (maxA - minA) else 0f
            val volume = amp.coerceIn(0f, 1f)
            if (volume < minAudio) continue

            val hue = safeHue((progress01 - 0.12f + rng.nextFloat() * 0.24f) * 360f)
            val lnDist = ((ln((a - 4).toDouble().coerceAtLeast(1.0)) / LOG_MAX) - BASE).toFloat()
            // Slight per-bin twist builds helix thickness (original was a single ray per frame).
            val twist = lnDist * 1.15f + (a % 5) * 0.04f
            val angle = baseAngle + twist
            val distance = (lnDist * outerRadius).coerceIn(4f, outerRadius * 1.05f)
            val size = ratio * (4.5f * volume * MAX_DOT.toFloat() + rng.nextFloat() * 2f)
            var alpha = volume * 0.12f
            alpha = when {
                size >= 8f && size < 29f -> 22f / 255f
                size >= 29f && size <= 60f -> 12f / 255f
                size > 60f -> 5f / 255f
                else -> (alpha * 1000f / 255f).coerceIn(0.05f, 0.85f)
            }
            val x = midx + sin(angle) * distance
            val y = midy + cos(angle) * distance
            if (!x.isFinite() || !y.isFinite()) continue
            // Mirror arm (DNA double-helix feel)
            dots += DnaDot(x, y, size.coerceIn(1.5f, 36f), hue, alpha)
            if (added % 2 == 0) {
                val x2 = midx - sin(angle) * distance * 0.92f
                val y2 = midy - cos(angle) * distance * 0.92f
                if (x2.isFinite() && y2.isFinite()) {
                    dots += DnaDot(
                        x2, y2,
                        (size * 0.85f).coerceIn(1.5f, 32f),
                        safeHue(hue + 40f),
                        alpha * 0.9f
                    )
                }
            }
            added++
        }
        while (dots.size > 1100) dots.removeAt(0)
    }

    fun draw(scope: DrawScope) = with(scope) {
        for (d in dots) {
            if (!d.x.isFinite() || !d.y.isFinite()) continue
            if (d.x < -20f || d.x > size.width + 20f || d.y < -20f || d.y > size.height + 20f) continue
            drawCircle(
                color = Color.hsv(
                    safeHue(d.hue),
                    0.85f,
                    0.9f,
                    alpha = d.alpha.coerceIn(0f, 1f)
                ),
                radius = d.r,
                center = Offset(d.x, d.y)
            )
        }
    }
}

fun DrawScope.drawMusicDnaSpiralScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    progress01: Float,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color.Black)
    if (size.width <= 1f || size.height <= 1f) return
    MusicDnaTrail.ensure(size.width, size.height)
    MusicDnaTrail.tick(fft, progress01, phase, features, playing)
    MusicDnaTrail.draw(this)
    // Soft center cue (no thick accent “slab”)
    drawCircle(
        color = palette.primary.copy(alpha = 0.06f + features.bass * 0.1f),
        radius = min(size.width, size.height) * (0.04f + features.rms * 0.03f),
        center = Offset(size.width / 2f, size.height / 2f)
    )
}
