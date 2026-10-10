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
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Original Apollo DNA / beat-cascade aesthetics (Compose).
 * Inspired by the *idea* of MusicDNA helix waveforms and MilkDrop-like
 * beat cascades — not a port of either codebase.
 */

fun DrawScope.drawDnaHelixScene(
    features: AudioFeatures,
    palette: HudPalette,
    wave: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false,
    cinematic: Float = 0f
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.35f
    val slow = 1f - cinematic * 0.55f
    val cx = size.width / 2f
    val amp = size.width * (0.16f + features.rms * 0.08f) * drive
    val steps = 48
    val pathA = Path()
    val pathB = Path()
    for (i in 0 until steps) {
        val t = i / (steps - 1f)
        val y = size.height * t
        val wIdx = (t * (wave.size - 1)).toInt().coerceIn(0, wave.lastIndex)
        val wobble = wave[wIdx] * amp * 0.45f
        val ang = t * PI.toFloat() * 4f + phase * 0.004f * slow
        val xA = cx + cos(ang) * amp + wobble
        val xB = cx + cos(ang + PI.toFloat()) * amp - wobble
        if (i == 0) {
            pathA.moveTo(xA, y)
            pathB.moveTo(xB, y)
        } else {
            pathA.lineTo(xA, y)
            pathB.lineTo(xB, y)
        }
        if (i % 3 == 0) {
            drawLine(
                color = palette.accent.copy(alpha = 0.25f + features.mid * 0.35f),
                start = Offset(xA, y),
                end = Offset(xB, y),
                strokeWidth = 1.5f + features.onset * 2f
            )
        }
        drawCircle(
            color = palette.primary.copy(alpha = 0.55f + features.bass * 0.3f),
            radius = 2.5f + features.high * 3f,
            center = Offset(xA, y)
        )
        drawCircle(
            color = palette.secondary.copy(alpha = 0.55f + features.bass * 0.3f),
            radius = 2.5f + features.high * 3f,
            center = Offset(xB, y)
        )
    }
    drawPath(pathA, palette.primary.copy(alpha = 0.85f), style = Stroke(3f, cap = StrokeCap.Round))
    drawPath(pathB, palette.secondary.copy(alpha = 0.85f), style = Stroke(3f, cap = StrokeCap.Round))
}

fun DrawScope.drawDnaWaveScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.4f
    val midY = size.height * 0.5f
    val path = Path()
    val n = fft.size.coerceAtLeast(2)
    for (i in 0 until n) {
        val x = i / (n - 1f) * size.width
        val twist = sin(i * 0.35f + phase * 0.01f) * size.height * 0.04f
        val h = fft[i].coerceIn(0f, 1f) * size.height * 0.35f * drive
        val y = midY - h * 0.5f + twist
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    for (i in n - 1 downTo 0) {
        val x = i / (n - 1f) * size.width
        val twist = sin(i * 0.35f + phase * 0.01f) * size.height * 0.04f
        val h = fft[i].coerceIn(0f, 1f) * size.height * 0.35f * drive
        val y = midY + h * 0.5f + twist
        path.lineTo(x, y)
    }
    path.close()
    drawPath(
        path,
        brush = Brush.verticalGradient(
            listOf(
                palette.primary.copy(alpha = 0.55f + features.bass * 0.25f),
                palette.accent.copy(alpha = 0.25f),
                palette.secondary.copy(alpha = 0.45f)
            )
        )
    )
}

fun DrawScope.drawBeatTunnelScene(
    features: AudioFeatures,
    palette: HudPalette,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false,
    beatFlash: Float = 0.35f,
    cinematic: Float = 0f
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val drive = (features.bass * 0.7f + features.onset * 0.8f + features.rms * 0.4f) *
        if (playing) 1f else 0.3f
    val slow = 1f - cinematic * 0.6f
    val rings = 10
    for (r in 0 until rings) {
        val drift = ((phase * 0.002f * slow + r * 0.11f) % 1f)
        val scale = 0.08f + drift * 0.95f
        val radius = size.minDimension * scale * (0.85f + drive * 0.25f)
        val alpha = ((1f - drift) * (0.15f + drive * 0.45f)).coerceIn(0.05f, 0.75f)
        drawCircle(
            color = (if (r % 2 == 0) palette.primary else palette.accent).copy(alpha = alpha),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 2f + drive * 4f + beatFlash * features.onset * 6f)
        )
    }
    if (beatFlash > 0.05f && features.onset > 0.35f) {
        drawRect(palette.soft.copy(alpha = beatFlash * features.onset * 0.18f))
    }
}

fun DrawScope.drawBeatCascadeScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false,
    beatFlash: Float = 0.35f
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val cols = 18
    val drive = if (playing) 1f else 0.35f
    val slot = size.width / cols
    for (c in 0 until cols) {
        val band = fft[(c * fft.size / cols).coerceIn(0, fft.lastIndex)]
        val h = size.height * (0.12f + band * 0.75f) * drive
        val x = c * slot
        val y = size.height - h
        val pulse = (sin(phase * 0.01f + c) + 1f) * 0.5f
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    palette.accent.copy(alpha = 0.15f + features.onset * 0.35f),
                    palette.primary.copy(alpha = 0.75f),
                    palette.secondary.copy(alpha = 0.35f)
                )
            ),
            topLeft = Offset(x + slot * 0.15f, y),
            size = Size(slot * 0.7f, h)
        )
        if (band > 0.55f) {
            drawCircle(
                color = palette.soft.copy(alpha = 0.2f + pulse * 0.3f),
                radius = slot * (0.2f + band * 0.3f),
                center = Offset(x + slot * 0.5f, y)
            )
        }
    }
    if (beatFlash > 0.05f && features.bass > 0.45f) {
        drawRect(palette.primary.copy(alpha = beatFlash * features.bass * 0.12f))
    }
}
