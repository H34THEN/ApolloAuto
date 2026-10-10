package com.apolloauto.player.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.apolloauto.player.media.AudioFeatures
import com.apolloauto.player.media.VisualIntensity
import com.apolloauto.player.media.VisualResponseMapper
import com.apolloauto.player.media.immersiveViewportDim
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Techno / trance topographic + trippy stylized scenes (Apollo originals).
 */

/** Contour-map elevation lines — topographic techno floor. */
fun DrawScope.drawTopoContourScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color.Black)
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.35f
    val cols = 40
    val rows = 22
    val scroll = phase * 0.035f

    fun heightAt(fx: Float, fy: Float): Float {
        val bin = fft.getOrElse((fx * fft.lastIndex.coerceAtLeast(0)).toInt()) { d.rms }
            .coerceIn(0f, 1.2f)
        return (
            0.5f * sin(fx * 10f + scroll) +
                0.4f * sin(fy * 8f - scroll * 1.3f + d.lowMid * 2f) +
                0.55f * bin * drive +
                d.bass * 0.4f * sin(fx * 4f + fy * 3f + phase * 0.02f)
            ).coerceIn(0f, 1.8f)
    }

    // Isolines: for each elevation, walk columns and interpolate row
    val levels = 11
    for (lvl in 0 until levels) {
        val threshold = 0.15f + lvl * 0.12f
        val hue = (170f + lvl * 16f + phase * 0.06f + d.onset * 35f) % 360f
        val col = Color.hsv(hue, 0.9f, 0.95f)
        val path = Path()
        var started = false
        for (c in 0..cols) {
            val fx = c / cols.toFloat()
            var bestY = -1f
            for (r in 0 until rows) {
                val fy0 = r / rows.toFloat()
                val fy1 = (r + 1) / rows.toFloat()
                val h0 = heightAt(fx, fy0)
                val h1 = heightAt(fx, fy1)
                if ((h0 - threshold) * (h1 - threshold) <= 0f) {
                    val t = ((threshold - h0) / ((h1 - h0).takeIf { kotlin.math.abs(it) > 1e-4f } ?: 1f))
                        .coerceIn(0f, 1f)
                    bestY = (fy0 + t * (fy1 - fy0)) * size.height
                    break
                }
            }
            if (bestY < 0f) {
                started = false
                continue
            }
            val x = fx * size.width
            if (!started) {
                path.moveTo(x, bestY)
                started = true
            } else {
                path.lineTo(x, bestY)
            }
        }
        if (started) {
            drawPath(
                path,
                col.copy(alpha = (0.65f + d.rms * 0.3f).coerceIn(0.5f, 1f)),
                style = Stroke((2.2f + d.bass * 2.8f).coerceIn(2f, 6f), cap = StrokeCap.Round)
            )
        }
    }
}

/** Wireframe 3D-ish terrain scrolling toward the viewer. */
fun DrawScope.drawWireTerrainScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color(0xFF050510))
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.3f
    val horizon = size.height * 0.28f
    val rows = 22
    val cols = 24
    val scroll = (phase * 0.08f) % 1f

    for (r in 0 until rows) {
        val depth = ((r + scroll) / rows).coerceIn(0.02f, 1f)
        val y = horizon + depth * depth * (size.height - horizon)
        val widthScale = 0.15f + depth * 0.95f
        val path = Path()
        for (c in 0..cols) {
            val u = c / cols.toFloat()
            val bin = fft.getOrElse((u * fft.lastIndex.coerceAtLeast(0)).toInt()) { 0f }
            val elev = (bin * 0.55f + d.bass * 0.35f) * drive *
                (0.4f + depth) * size.height * 0.18f *
                sin(u * PI.toFloat() * 3f + phase * 0.03f + r * 0.4f)
            val x = size.width * 0.5f + (u - 0.5f) * size.width * widthScale
            val yy = y - elev
            if (c == 0) path.moveTo(x, yy) else path.lineTo(x, yy)
        }
        val alpha = (0.25f + depth * 0.7f).coerceIn(0.2f, 1f)
        val hue = (200f + depth * 80f + d.onset * 30f + phase * 0.04f) % 360f
        drawPath(
            path,
            Color.hsv(hue, 0.85f, 0.95f, alpha = alpha),
            style = Stroke((1f + depth * 2.5f).coerceIn(1f, 4f))
        )
    }
    // Perspective side rails
    drawLine(
        palette.primary.copy(alpha = 0.35f),
        Offset(size.width * 0.5f, horizon),
        Offset(0f, size.height),
        strokeWidth = 2f
    )
    drawLine(
        palette.primary.copy(alpha = 0.35f),
        Offset(size.width * 0.5f, horizon),
        Offset(size.width, size.height),
        strokeWidth = 2f
    )
}

/** Kaleidoscope shards — trippy stylized. */
fun DrawScope.drawPsyKaleidoScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color.Black)
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.3f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val wedges = 8
    val maxR = immersiveViewportDim(size.width, size.height) * 0.48f

    withTransform({
        rotate(degrees = phase * 0.06f + d.bass * 12f, pivot = Offset(cx, cy))
    }) {
        for (w in 0 until wedges) {
            rotate(degrees = w * (360f / wedges), pivot = Offset(cx, cy)) {
                val path = Path()
                path.moveTo(cx, cy)
                val steps = 16
                for (s in 0..steps) {
                    val t = s / steps.toFloat()
                    val bin = fft.getOrElse((t * fft.lastIndex.coerceAtLeast(0)).toInt()) { d.rms }
                    val r = maxR * t * (0.7f + bin * 0.55f * drive)
                    val a = (t * 28f - 14f) * (PI.toFloat() / 180f)
                    path.lineTo(cx + sin(a) * r, cy - cos(a) * r)
                }
                path.close()
                val hue = (w * 45f + phase * 0.12f + d.onset * 50f) % 360f
                drawPath(
                    path,
                    Color.hsv(hue, 0.9f, 1f, alpha = (0.35f + d.rms * 0.4f).coerceIn(0.3f, 0.85f))
                )
                drawPath(
                    path,
                    Color.hsv((hue + 40f) % 360f, 1f, 1f, alpha = 0.9f),
                    style = Stroke(2f + d.bass * 3f)
                )
            }
        }
    }
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color.White.copy(alpha = (0.15f + d.onset * 0.35f).coerceAtMost(0.5f)),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = maxR * 0.25f
        ),
        radius = maxR * 0.25f,
        center = Offset(cx, cy)
    )
}

/** Acid spiral tunnel — deep trance trip. */
fun DrawScope.drawAcidTunnelScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color.Black)
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.28f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val rings = 18
    val maxR = immersiveViewportDim(size.width, size.height) * 0.58f
    val spin = phase * 0.05f + d.bass * 0.8f

    for (i in 0 until rings) {
        val t = i / (rings - 1f)
        // Pull rings toward center over time
        val depth = ((t + phase * 0.0008f) % 1f)
        val r = 8f + depth * depth * depth * maxR * (1f + d.rms * 0.25f)
        val path = Path()
        val steps = 56
        val warp = 0.12f + d.onset * 0.2f + ampBand(fft, depth) * 0.25f * drive
        for (s in 0..steps) {
            val a = s / steps.toFloat() * PI.toFloat() * 2f + spin * (1f + depth)
            val rr = r * (1f + warp * sin(a * 4f + phase * 0.04f + i))
            val x = cx + cos(a) * rr
            val y = cy + sin(a) * rr * (0.78f + d.lowMid * 0.12f)
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        val hue = (depth * 300f + phase * 0.15f + i * 8f) % 360f
        val alpha = (0.55f + (1f - depth) * 0.4f).coerceIn(0.4f, 1f)
        drawPath(
            path,
            Color.hsv(hue, 0.95f, 1f, alpha = alpha),
            style = Stroke((2.5f + (1f - depth) * 4f + d.bass * 2f).coerceIn(2f, 9f))
        )
    }
}

/** Neon topographic rings — stacked elevation plates. */
fun DrawScope.drawNeonTopoScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color(0xFF020208))
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.3f
    val cx = size.width / 2f
    val cy = size.height * 0.58f
    val plates = 12
    for (i in plates downTo 0) {
        val t = i / plates.toFloat()
        val bin = ampBand(fft, 1f - t)
        val lift = (bin * 0.5f + d.bass * 0.45f) * drive * size.height * 0.08f
        val rx = size.width * (0.15f + t * 0.38f)
        val ry = size.height * (0.06f + t * 0.12f)
        val path = Path()
        val steps = 48
        for (s in 0..steps) {
            val a = s / steps.toFloat() * PI.toFloat() * 2f + phase * 0.01f
            val jagged = 1f + 0.08f * sin(a * 5f + i + phase * 0.03f) * drive + bin * 0.1f
            val x = cx + cos(a) * rx * jagged
            val y = cy - t * size.height * 0.35f - lift + sin(a) * ry * jagged
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        val hue = (160f + t * 140f + phase * 0.08f) % 360f
        drawPath(
            path,
            Color.hsv(hue, 0.9f, 0.95f, alpha = (0.5f + bin * 0.4f).coerceIn(0.4f, 1f)),
            style = Stroke(2.2f + (1f - t) * 2f)
        )
        if (i % 3 == 0) {
            drawPath(
                path,
                Color.hsv(hue, 0.7f, 0.5f, alpha = 0.12f)
            )
        }
    }
}

private fun ampBand(fft: FloatArray, t: Float): Float {
    if (fft.isEmpty()) return 0f
    val idx = (t.coerceIn(0f, 1f) * fft.lastIndex).toInt()
    return fft[idx].coerceIn(0f, 1.2f)
}
