package com.apolloauto.player.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.apolloauto.player.media.AudioFeatures
import com.apolloauto.player.media.BandDrive
import com.apolloauto.player.media.VisualIntensity
import com.apolloauto.player.media.VisualResponseMapper
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.hypot
import kotlin.math.sign

/**
 * Live Compose ports inspired by
 * [CalebABG/MusicVisualizerV2](https://github.com/CalebABG/MusicVisualizerV2)
 * (MIT). Reimplemented for Compose Canvas — not a Processing/Minim port.
 */

/** Blend theme / album-art palette across the spectrum (t in 0..1). */
private fun bandColor(palette: HudPalette, t: Float, alpha: Float = 1f): Color {
    val x = t.coerceIn(0f, 1f)
    val c = when {
        x < 0.33f -> lerpColor(palette.primary, palette.secondary, x / 0.33f)
        x < 0.66f -> lerpColor(palette.secondary, palette.accent, (x - 0.33f) / 0.33f)
        else -> lerpColor(palette.accent, palette.primary, (x - 0.66f) / 0.34f)
    }
    return c.copy(alpha = alpha.coerceIn(0f, 1f))
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val u = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * u,
        green = a.green + (b.green - a.green) * u,
        blue = a.blue + (b.blue - a.blue) * u,
        alpha = a.alpha + (b.alpha - a.alpha) * u
    )
}

fun DrawScope.drawBassNationScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val n = fft.size.coerceAtLeast(1)
    val scale = if (playing) 1f else 0.45f
    val radius = size.minDimension * (0.18f + features.bass * 0.04f)
    val barH = size.minDimension * 0.42f
    val cx = size.width / 2f
    val cy = size.height / 2f
    // Two mirrored radial sweeps (clockwise / counter-clockwise).
    for (pass in 0..1) {
        val dir = if (pass == 0) -1f else 1f
        for (i in 0 until n) {
            val amp = fft[i].coerceIn(0f, 1f) * scale
            val angleDeg = dir * i * (360f / n) * 0.55f
            val color = bandColor(palette, i / n.toFloat(), 0.85f)
            rotate(angleDeg, Offset(cx, cy)) {
                drawRect(
                    color = color.copy(alpha = 0.85f),
                    topLeft = Offset(cx - 1.5f, cy - radius - amp * barH),
                    size = Size(3f, amp * barH)
                )
            }
        }
    }
    drawCircle(
        color = palette.accent.copy(alpha = 0.12f + features.bass * 0.25f),
        radius = radius * (0.35f + features.rms * 0.2f),
        center = Offset(cx, cy)
    )
}

fun DrawScope.drawBubbleBassScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val count = min(28, fft.size)
    val drive = if (playing) 1f else 0.35f
    for (i in 0 until count) {
        val t = phase * 0.0012f + i * 1.7f
        val nx = (sin(t * 0.9f + i) + 1f) * 0.5f
        val ny = (cos(t * 0.7f + i * 1.3f) + 1f) * 0.5f
        val x = size.width * (0.05f + nx * 0.9f)
        val y = size.height * (0.08f + ny * 0.84f)
        val amp = fft[i].coerceIn(0f, 1f) * drive
        val r = size.minDimension * (0.012f + amp * 0.08f + features.bass * 0.01f)
        val color = bandColor(palette, (i / count.toFloat() + amp * 0.35f) % 1f, 0.35f + amp * 0.45f)
        drawCircle(color = color, radius = r, center = Offset(x, y))
        drawCircle(
            color = palette.soft.copy(alpha = 0.08f + amp * 0.15f),
            radius = r * 1.35f,
            center = Offset(x, y)
        )
    }
}

fun DrawScope.drawCircularPulseMvScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val baseR = size.minDimension * 0.16f
    val scale = if (playing) 1f else 0.4f
    val step = max(1, fft.size / 48)
    var i = 0
    while (i < fft.size) {
        val angle = i.toFloat() / fft.size * PI.toFloat() * 2f
        val amp = fft[i].coerceIn(0f, 1f) * scale
        val outer = baseR + amp * size.minDimension * 0.28f
        val color = bandColor(palette, i / fft.size.toFloat().coerceAtLeast(1f), 0.9f)
        drawLine(
            color = color.copy(alpha = 0.9f),
            start = Offset(cx + cos(angle) * baseR, cy + sin(angle) * baseR),
            end = Offset(cx + cos(angle) * outer, cy + sin(angle) * outer),
            strokeWidth = 2.5f + features.rms * 2f,
            cap = StrokeCap.Round
        )
        i += step
    }
    val core = size.minDimension * (0.06f + features.bass * 0.12f) * scale
    drawCircle(
        color = bandColor(palette, features.bass.coerceIn(0f, 1f), 0.55f + features.rms * 0.3f),
        radius = core,
        center = Offset(cx, cy)
    )
}

fun DrawScope.drawColorSignalScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val midY = size.height * 0.48f
    val drive = (0.65f + features.rms * 0.55f + features.bass * 0.35f) * if (playing) 1f else 0.35f
    val ampScale = size.height * 0.28f * drive
    val step = max(1, fft.size / 64)
    var i = 0
    while (i < fft.size - 1) {
        val x1 = i / (fft.size - 1f) * size.width
        val x2 = (i + 1) / (fft.size - 1f) * size.width
        val amp = fft[i].coerceIn(0f, 1f) * ampScale
        val color = bandColor(palette, ((i / fft.size.toFloat()) + features.mid * 0.15f) % 1f, 0.75f + features.rms * 0.2f)
        drawLine(
            color = color.copy(alpha = 0.75f + features.rms * 0.2f),
            start = Offset(x1, midY + amp),
            end = Offset(x2, midY - amp),
            strokeWidth = 2.2f + features.high * 3f + features.onset * 2f,
            cap = StrokeCap.Round
        )
        i += step
    }
}

fun DrawScope.drawDualSignalScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val midY = size.height * 0.5f
    val ampScale = size.height * 0.28f * if (playing) 1f else 0.4f
    val last = (fft.size - 1).coerceAtLeast(1)
    val step = max(1, fft.size / 64)
    var i = 0
    while (i < last) {
        val t0 = i / last.toFloat()
        val t1 = (i + 1) / last.toFloat()
        val x1 = t0 * size.width
        val x2 = t1 * size.width
        val xr1 = (1f - t0) * size.width
        val xr2 = (1f - t1) * size.width
        val amp = fft[i].coerceIn(0f, 1f) * ampScale *
            (0.7f + features.rms * 0.5f + features.bass * 0.3f)
        val color = bandColor(palette, i / fft.size.coerceAtLeast(1).toFloat(), 0.9f)
        drawLine(
            color = color.copy(alpha = 0.75f + features.rms * 0.2f),
            start = Offset(x1, midY),
            end = Offset(x2, midY + amp),
            strokeWidth = 2f + features.high * 2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color.copy(alpha = 0.75f + features.rms * 0.2f),
            start = Offset(xr1, midY),
            end = Offset(xr2, midY - amp),
            strokeWidth = 2f + features.high * 2f,
            cap = StrokeCap.Round
        )
        i += step
    }
}

fun DrawScope.drawMixedWaveScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val ampScale = size.height * 0.2f * if (playing) 1f else 0.4f
    val angle = phase * 0.015f
    val step = max(1, fft.size / 72)
    var i = 0
    while (i < fft.size - 1) {
        val x1 = i / (fft.size - 1f) * size.width
        val x2 = (i + 1) / (fft.size - 1f) * size.width
        val amp = fft[i].coerceIn(0f, 1f) * ampScale
        val y1 = size.height / 2f - 75f * sin(0.015f * x1 + angle)
        val y2 = size.height / 2f - 75f * sin(0.015f * x2 + angle)
        val color = bandColor(palette, i / fft.size.toFloat(), 0.88f)
        drawLine(
            color = color.copy(alpha = 0.88f + features.mid * 0.1f),
            start = Offset(x1, y1 + amp),
            end = Offset(x2, y2 - amp),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )
        i += step
    }
    // Subtle secondary transform ribbon
    var j = 0
    while (j < fft.size - 1) {
        val x = j / (fft.size - 1f) * size.width
        val a = 325f
        val m = 2f
        val base = m * (x - a) * sign(a - x) + m * a - size.height / 2f +
            40f * sin(0.08f * x + angle)
        val amp = fft[j].coerceIn(0f, 1f) * ampScale * 0.35f
        drawCircle(
            color = palette.accent.copy(alpha = 0.15f + fft[j] * 0.25f),
            radius = 1.5f + amp * 0.04f,
            center = Offset(x, size.height / 2f - base * 0.15f)
        )
        j += step * 2
    }
}

fun DrawScope.drawShapeWaveScene(
    features: AudioFeatures,
    palette: HudPalette,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val space = max(28f, size.minDimension / 18f)
    val drive = (features.bass * 0.85f + features.rms * 0.65f + features.onset * 0.45f) *
        if (playing) 1f else 0.3f
    val tBase = phase * (0.0025f + features.mid * 0.004f)
    var x = -space
    while (x < size.width + space) {
        var y = -space
        while (y < size.height + space) {
            var t = tBase
            t += cos(abs(1f - x / (size.width / 2f))) * PI.toFloat() * 2f
            t += cos(abs(1f - y / (size.height / 2f))) * PI.toFloat() * 2f
            val rx = sin(t) * (2f + drive * 22f + features.high * 6f)
            if (rx > 0.5f) {
                val d = hypot(x - size.width / 2f, y - size.height / 2f)
                val color = bandColor(
                    palette,
                    (d / (size.maxDimension + 1f) + features.bass * 0.2f).coerceIn(0f, 1f),
                    0.3f + drive * 0.4f
                )
                drawRect(
                    color = color,
                    topLeft = Offset(x - rx / 2f, y - rx / 2f),
                    size = Size(rx, rx)
                )
            }
            y += space
        }
        x += space
    }
}

fun DrawScope.drawStarFieldScene(
    features: AudioFeatures,
    palette: HudPalette,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    if (size.width <= 1f || size.height <= 1f) return
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.28f
    val kick = (VisualResponseMapper.kickPressure(d) * drive).coerceIn(0f, 2.2f)
    val spark = (VisualResponseMapper.sparkle(d) * drive).coerceIn(0f, 1.8f)
    // Bass expands field; onset accelerates warp (geometry, not alpha-only).
    val phaseSafe = if (phase.isFinite()) phase else 0f
    val speed = (0.7f + d.bass * 6.5f + d.onset * 4.5f + d.sub * 2.5f)
        .coerceIn(0.1f, 24f) * drive
    val expand = (0.55f + kick * 0.45f + d.lowMid * 0.12f).coerceIn(0.2f, 3f)
    val count = 110
    val cx = size.width / 2f
    val cy = size.height / 2f
    // Kick shockwave ring
    if (kick > 0.55f) {
        val shockR = min(size.width, size.height) * (0.12f + kick * 0.28f)
        drawCircle(
            color = palette.accent.copy(alpha = (0.08f + kick * 0.18f).coerceIn(0f, 0.35f)),
            radius = shockR.coerceIn(2f, 600f),
            center = Offset(cx, cy),
            style = Stroke((2f + kick * 3f).coerceIn(1f, 12f))
        )
    }
    for (i in 0 until count) {
        val seed = i * 97.13f
        val zCycle = fract01(phaseSafe * speed * 0.09f + seed * 13f)
        val z = (0.08f + (1f - zCycle) * 0.92f).coerceIn(0.08f, 1f)
        val ang = seed + d.upperMid * 0.8f + phaseSafe * 0.0008f * (1f + d.lowMid)
        val rx = ((sin(seed) + 1f) * 0.5f - 0.5f) * size.width * (1f + d.bass * 0.35f)
        val ry = ((cos(seed * 1.3f) + 1f) * 0.5f - 0.5f) * size.height * (1f + d.sub * 0.25f)
        val sx = cx + (rx / z) * expand + cos(ang) * d.midOrbitPx()
        val sy = cy + (ry / z) * expand + sin(ang * 1.2f) * d.midOrbitPx() * 0.7f
        if (!sx.isFinite() || !sy.isFinite()) continue
        val twinkle = if (i % 4 == 0) {
            1f + spark * (0.5f + 0.5f * sin(phaseSafe * 0.08f + seed))
        } else {
            1f
        }.coerceIn(0.5f, 2.2f)
        val r = (((1f - z) * 5.5f + 0.4f + spark * 1.8f) * twinkle).coerceIn(0.2f, 28f)
        val alpha = (0.3f + (1f - z) * 0.65f + spark * 0.2f + d.rms * 0.15f).coerceIn(0.2f, 1f)
        val color = when {
            i % 11 == 0 -> palette.accent.copy(alpha = alpha)
            i % 5 == 0 -> palette.primary.copy(alpha = alpha)
            else -> palette.soft.copy(alpha = alpha)
        }
        drawCircle(color = color, radius = r, center = Offset(sx, sy))
        if (playing && kick > 0.4f && i % 2 == 0) {
            // kick can exceed 1 — alpha must stay in 0..1 (crashed STARS).
            drawLine(
                color = palette.primary.copy(alpha = (0.25f + kick * 0.35f).coerceIn(0.05f, 1f)),
                start = Offset(sx, sy),
                end = Offset(
                    cx + (rx / (z + 0.12f)) * expand,
                    cy + (ry / (z + 0.12f)) * expand
                ),
                strokeWidth = (1.2f + kick).coerceIn(0.5f, 8f)
            )
        }
    }
}

private fun fract01(v: Float): Float {
    if (!v.isFinite()) return 0f
    val f = v - kotlin.math.floor(v.toDouble()).toFloat()
    return if (f < 0f) f + 1f else f.coerceIn(0f, 1f)
}

private fun BandDrive.midOrbitPx(): Float = (lowMid * 22f + upperMid * 14f)
