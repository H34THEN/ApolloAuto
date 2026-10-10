package com.apolloauto.player.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.apolloauto.player.media.AudioFeatures
import com.apolloauto.player.media.VisualIntensity
import com.apolloauto.player.media.VisualResponseMapper
import com.apolloauto.player.media.immersiveViewportDim
import com.apolloauto.player.media.safeHue
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cosmic reactive space-warp — hyperspace tunnel + streaking starfield.
 * Replaces the old STARS / STARS2 looks with a single music-driven warp.
 */
fun DrawScope.drawCosmicWarpScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    // Always fill pure black unless album art is intentionally showing through.
    if (!clearBackdrop) drawRect(Color.Black)
    if (size.width <= 1f || size.height <= 1f) return

    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.32f
    val kick = (VisualResponseMapper.kickPressure(d) * drive).coerceIn(0f, 2.2f)
    val spark = (VisualResponseMapper.sparkle(d) * drive).coerceIn(0f, 1.8f)
    val phaseSafe = if (phase.isFinite()) phase else 0f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val span = immersiveViewportDim(size.width, size.height)
    val fov = span * (0.42f + d.bass * 0.1f + kick * 0.06f)
    val speed = (0.9f + d.bass * 7f + d.onset * 5f + d.sub * 2.5f + d.rms * 2f)
        .coerceIn(0.2f, 28f) * drive
    val hueBase = safeHue(phaseSafe * 0.08f + d.upperMid * 80f + d.presence * 40f)

    // Deep space wash — keep value low so nothing flashes yellow/white.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.hsv(safeHue(hueBase + 220f), 0.55f, (0.12f + d.rms * 0.08f).coerceIn(0.05f, 0.28f), 0.85f),
                Color.hsv(safeHue(hueBase + 280f), 0.65f, 0.06f, 0.9f),
                Color.Black
            ),
            center = Offset(cx, cy),
            radius = span * 0.75f
        )
    )

    // Warp tunnel rings (FFT-warped) — denser, more elliptical for widescreen
    val rings = 16
    for (i in 0 until rings) {
        val t = ((i / rings.toFloat()) + phaseSafe * speed * 0.00055f) % 1f
        val depth = t * t
        val bin = fft.getOrElse((t * fft.lastIndex.coerceAtLeast(0)).toInt()) { d.rms }
            .coerceIn(0f, 1.2f)
        val r = 5f + depth * fov * (1.1f + bin * 0.4f * drive + kick * 0.1f)
        val wobble = 1f + d.lowMid * 0.14f * sin(phaseSafe * 0.035f + i) +
            bin * 0.22f * drive
        val squash = 0.62f + d.upperMid * 0.2f + sin(phaseSafe * 0.02f + i * 0.3f) * 0.06f
        val hue = safeHue(hueBase + t * 160f + i * 14f + kick * 20f)
        val alpha = ((1f - depth) * (0.4f + d.rms * 0.4f + spark * 0.18f))
            .coerceIn(0.1f, 0.9f)
        drawOval(
            color = Color.hsv(hue, 0.9f.coerceIn(0f, 1f), 0.9f.coerceIn(0f, 1f), alpha),
            topLeft = Offset(cx - r * wobble, cy - r * wobble * squash),
            size = Size(r * 2f * wobble, r * 2f * wobble * squash),
            style = Stroke(
                width = (1.4f + (1f - depth) * 3f + bin * 2.5f + kick * 0.8f).coerceIn(1f, 9f)
            )
        )
        // Ghost trail ring (mid energy)
        if (i % 2 == 0) {
            drawOval(
                color = Color.hsv(safeHue(hue + 180f), 0.7f, 0.7f, alpha * 0.35f),
                topLeft = Offset(cx - r * wobble * 1.08f, cy - r * wobble * squash * 1.08f),
                size = Size(
                    r * 2.16f * wobble,
                    r * 2.16f * wobble * squash
                ),
                style = Stroke(width = 1f)
            )
        }
    }

    // Hyperspace star streaks — denser warp field
    val count = 200
    for (i in 0 until count) {
        val seed = i * 97.13f
        val zCycle = fract01(phaseSafe * speed * 0.085f + seed * 13f)
        val z = (0.06f + (1f - zCycle) * 0.94f).coerceIn(0.06f, 1f)
        val swirl = d.upperMid * 0.12f + phaseSafe * 0.0006f * (1f + d.lowMid)
        val spread = 0.55f + d.bass * 0.35f + kick * 0.12f
        val rx = ((sin(seed + swirl) + 1f) * 0.5f - 0.5f) * size.width * spread
        val ry = ((cos(seed * 1.37f + swirl * 0.8f) + 1f) * 0.5f - 0.5f) * size.height * spread
        val sx = cx + rx / z
        val sy = cy + ry / z
        if (!sx.isFinite() || !sy.isFinite()) continue
        // Keep streaks on-screen — extreme z used to draw a long yellow/white slash across center.
        if (sx < -size.width || sx > size.width * 2f || sy < -size.height || sy > size.height * 2f) continue
        val zBack = (z + 0.14f + d.onset * 0.04f).coerceAtMost(1.2f)
        val ex = cx + rx / zBack
        val ey = cy + ry / zBack
        val lenBoost = (0.85f + kick * 0.35f + d.rms * 0.2f).coerceIn(0.5f, 1.6f)
        val streakEnd = Offset(
            (sx + (sx - ex) * lenBoost).coerceIn(-size.width * 0.2f, size.width * 1.2f),
            (sy + (sy - ey) * lenBoost).coerceIn(-size.height * 0.2f, size.height * 1.2f)
        )
        val twinkle = if (i % 5 == 0) {
            1f + spark * (0.45f + 0.55f * sin(phaseSafe * 0.09f + seed))
        } else {
            1f
        }.coerceIn(0.5f, 2.0f)
        val alpha = (0.28f + (1f - z) * 0.65f + spark * 0.15f + d.rms * 0.1f)
            .coerceIn(0.2f, 0.95f)
        val hue = safeHue(hueBase + (i % 24) * 11f + d.treble * 30f)
        val col = when {
            i % 13 == 0 -> palette.accent.copy(alpha = alpha * 0.85f)
            i % 7 == 0 -> Color.hsv(hue, 0.75f, 0.9f, alpha)
            else -> Color.hsv(safeHue(hue + 40f), 0.4f, 0.85f, alpha)
        }
        val sw = (((1f - z) * 2.8f + 0.5f) * twinkle + kick * 0.5f).coerceIn(0.4f, 4.5f)
        drawLine(
            color = col,
            start = Offset(sx, sy),
            end = streakEnd,
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        if (z < 0.35f) {
            drawCircle(
                color = col.copy(alpha = (alpha * 0.85f).coerceAtMost(0.9f)),
                radius = (1.2f + (1f - z) * 3f * twinkle).coerceIn(0.8f, 8f),
                center = Offset(sx, sy)
            )
        }
    }

    // Bass shock rings — soft cyan/magenta, never a solid yellow slab
    if (kick > 0.5f) {
        val shockR = span * (0.1f + kick * 0.18f + d.sub * 0.05f)
        drawCircle(
            color = palette.primary.copy(alpha = (0.08f + kick * 0.14f).coerceIn(0f, 0.32f)),
            radius = shockR.coerceIn(4f, span * 0.5f),
            center = Offset(cx, cy),
            style = Stroke((1.5f + kick * 2.5f).coerceIn(1f, 8f))
        )
    }

    // Soft core — low value, no white flash
    val coreR = span * (0.035f + d.rms * 0.04f + kick * 0.02f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color.hsv(safeHue(hueBase + 200f), 0.55f, 0.55f, (0.2f + d.onset * 0.25f).coerceAtMost(0.45f)),
                palette.primary.copy(alpha = 0.12f + d.bass * 0.15f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = coreR * 2.8f
        ),
        radius = coreR * 2.8f,
        center = Offset(cx, cy)
    )

    // Soft dust motes for depth (treble)
    val dust = (36 + (d.treble * 28f).toInt()).coerceIn(28, 64)
    for (i in 0 until dust) {
        val a = i * (PI.toFloat() * 2f / dust) + phaseSafe * 0.004f
        val rad = fov * (0.15f + (i % 7) * 0.08f) * (1f + d.presence * 0.15f)
        val x = cx + cos(a) * rad
        val y = cy + sin(a * 1.15f) * rad * 0.72f
        val aBin = fft.getOrElse(i % fft.size.coerceAtLeast(1)) { 0f }
        drawCircle(
            color = palette.soft.copy(alpha = (0.1f + aBin * 0.3f + spark * 0.08f).coerceIn(0.06f, 0.45f)),
            radius = 1.2f + aBin * 2.2f,
            center = Offset(x, y)
        )
    }

    // Midrange spiral arms (hyperspace lanes)
    val arms = 3
    for (arm in 0 until arms) {
        val path = Path()
        val segs = 56
        for (s in 0..segs) {
            val t = s / segs.toFloat()
            val ang = arm * (PI.toFloat() * 2f / arms) + t * 4.2f + phaseSafe * 0.03f * drive
            val rad = span * t * (0.48f + d.bass * 0.08f) * (0.85f + kick * 0.08f)
            val x = cx + cos(ang) * rad
            val y = cy + sin(ang) * rad * (0.7f + d.lowMid * 0.15f)
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            Color.hsv(safeHue(hueBase + arm * 40f + 200f), 0.75f, 0.85f)
                .copy(alpha = (0.18f + d.upperMid * 0.25f + spark * 0.1f) * drive),
            style = Stroke((1.5f + d.rms * 2f).coerceIn(1f, 4.5f), cap = StrokeCap.Round)
        )
    }
}

private fun fract01(v: Float): Float {
    if (!v.isFinite()) return 0f
    val f = v - kotlin.math.floor(v)
    return if (f.isFinite()) f.coerceIn(0f, 1f) else 0f
}
