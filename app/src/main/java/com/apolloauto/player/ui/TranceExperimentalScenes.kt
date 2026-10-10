package com.apolloauto.player.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.apolloauto.player.media.AudioFeatures
import com.apolloauto.player.media.VizPaletteMode
import com.apolloauto.player.media.VizQualityTier
import com.apolloauto.player.media.immersiveViewportDim
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class SpiralVariant { SINGLE, DUAL, INTERWOVEN, RIBBONS, FREQ_BANDS }

fun DrawScope.drawAudibleSpiralsScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean,
    paletteMode: VizPaletteMode,
    quality: VizQualityTier,
    sat: Float,
    glow: Float,
    variant: SpiralVariant = SpiralVariant.INTERWOVEN,
    spiralPreset: String = "rainbow"
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.35f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val turns = when (variant) {
        SpiralVariant.SINGLE -> 1
        SpiralVariant.DUAL -> 2
        SpiralVariant.INTERWOVEN -> 3
        SpiralVariant.RIBBONS -> 4
        SpiralVariant.FREQ_BANDS -> 5
    }
    val steps = (180 * quality.meshScale).toInt().coerceIn(90, 280)
    val a = immersiveViewportDim(size.width, size.height) * (0.014f + features.bass * 0.005f)
    for (arm in 0 until turns) {
        val path = Path()
        val dir = if (variant == SpiralVariant.DUAL && arm % 2 == 1) -1f else 1f
        for (i in 0 until steps) {
            val t = i / (steps - 1f)
            val theta = t * PI.toFloat() * 8f * dir + arm * (PI.toFloat() * 2f / turns) +
                phase * 0.004f * (1f + features.presence * 0.5f)
            val innerCurve = 1f + features.lowMid * 0.35f * sin(theta * 2f)
            val outerDef = 1f + features.upperMid * 0.4f * sin(theta * 3f + i * 0.05f)
            val fine = features.treble * 6f * sin(theta * 12f + phase * 0.02f)
            val pulse = 1f + features.onset * 0.25f * sin(t * PI.toFloat() * 2f + phase * 0.03f)
            val band = if (variant == SpiralVariant.FREQ_BANDS) {
                fft[((t * (fft.size - 1)).toInt())]
            } else fft[(i * fft.size / steps).coerceIn(0, fft.lastIndex)]
            val r = (a * theta * innerCurve * outerDef + fine + band * 18f) *
                (1f + features.subBass * 0.2f) * pulse * drive
            val x = cx + cos(theta) * r
            val y = cy + sin(theta) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val c = if (paletteMode == VizPaletteMode.SCENE || paletteMode == VizPaletteMode.RAINBOW) {
            TrancePalette.spiralPreset(spiralPreset, arm / turns.toFloat() + phase * 0.0005f, palette)
        } else {
            TrancePalette.color(arm / turns.toFloat() + phase * 0.0005f, paletteMode, palette, sat, glow)
        }
        val thickness = when (variant) {
            SpiralVariant.RIBBONS -> 3.5f + features.rms * 3f
            else -> 1.8f + features.onset * 2f
        }
        drawPath(path, c.copy(alpha = 0.85f), style = Stroke(thickness, cap = StrokeCap.Round))
        if (variant == SpiralVariant.RIBBONS) {
            drawPath(path, c.copy(alpha = 0.2f), style = Stroke(thickness * 3f))
        }
    }
}

fun DrawScope.drawFluctusFlowScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean,
    paletteMode: VizPaletteMode,
    quality: VizQualityTier,
    sat: Float,
    glow: Float
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.35f
    val ribbons = (5 * quality.meshScale).toInt().coerceIn(3, 8)
    val cols = (36 * quality.meshScale).toInt().coerceIn(20, 48)
    val aspect = size.width / size.height.coerceAtLeast(1f)
    for (r in 0 until ribbons) {
        val path = Path()
        val depth = 0.35f + r / ribbons.toFloat() * 0.55f
        for (i in 0 until cols) {
            val u = i / (cols - 1f)
            val x = (u - 0.5f) * size.width * (1.15f + aspect * 0.05f)
            val z = depth
            val band = fft[(i + r * 3) % fft.size]
            val bend = features.subBass * 80f * sin(u * PI.toFloat() + phase * 0.01f + r)
            val height = features.bass * 70f * band + features.lowMid * 40f * sin(u * 4f + phase * 0.02f)
            val curve = features.upperMid * 25f * cos(u * 6f + r)
            val ripple = features.treble * 12f * sin(u * 18f + phase * 0.05f)
            val wave = features.onset * 30f * sin(u * PI.toFloat() * 2f - phase * 0.04f)
            val y3 = height + curve + ripple + wave + bend * 0.3f
            val persp = 1f / (0.55f + z)
            val sx = size.width / 2f + x * persp
            val sy = size.height * 0.55f + y3 * persp * drive
            if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
        }
        val c = TrancePalette.color(r / ribbons.toFloat() + phase * 0.0003f, paletteMode, palette, sat, glow)
        drawPath(path, c.copy(alpha = 0.75f), style = Stroke(2.5f + features.rms * 2f, cap = StrokeCap.Round))
        drawPath(path, c.copy(alpha = 0.18f), style = Stroke(10f))
    }
}

fun DrawScope.drawWaveformVoidScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    wave: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean,
    paletteMode: VizPaletteMode,
    quality: VizQualityTier,
    sat: Float,
    glow: Float
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.35f
    val layers = (6 * quality.meshScale).toInt().coerceIn(4, 10)
    val extend = size.width * 0.08f
    for (l in 0 until layers) {
        val path = Path()
        val z = l / (layers - 1f)
        val yBase = size.height * (0.2f + z * 0.6f)
        val steps = 64
        for (i in 0..steps) {
            val u = i / steps.toFloat()
            val x = -extend + u * (size.width + extend * 2f)
            val idx = (u * (fft.size - 1)).toInt().coerceIn(0, fft.lastIndex)
            val wIdx = (u * (wave.size - 1)).toInt().coerceIn(0, wave.lastIndex)
            val bandE = when {
                z < 0.33f -> features.bass
                z < 0.66f -> features.lowMid
                else -> features.treble
            }
            val disp = fft[idx] * (40f + bandE * 50f) +
                wave[wIdx] * 20f * features.presence +
                features.onset * 25f * sin(u * PI.toFloat() * 4f + phase * 0.03f)
            val persp = 0.65f + z * 0.5f
            val y = yBase - disp * persp * drive + sin(u * 3f + phase * 0.01f + l) * features.upperMid * 10f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val c = TrancePalette.color(z + phase * 0.0004f, paletteMode, palette, sat, glow)
        drawPath(
            path,
            c.copy(alpha = 0.35f + (1f - z) * 0.35f),
            style = Stroke(2f + (1f - z) * 3f + features.rms * 2f, cap = StrokeCap.Round)
        )
        // Chromatic trail twin
        val path2 = Path()
        path2.addPath(path)
        drawPath(path2, c.copy(alpha = 0.12f), style = Stroke(8f + features.spectralFlux * 6f))
    }
}

fun DrawScope.drawObsidianReactorScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean,
    paletteMode: VizPaletteMode,
    quality: VizQualityTier,
    sat: Float,
    glow: Float,
    crystalPreset: String = "violet"
) {
    if (!clearBackdrop) drawRect(Color(0xFF050508))
    val drive = if (playing) 1f else 0.35f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val detail = (8 * quality.meshScale).toInt().coerceIn(5, 12)
    val scale = immersiveViewportDim(size.width, size.height) *
        (0.3f + features.bass * 0.12f + features.subBass * 0.1f) *
        (1f + features.onset * 0.2f) * drive
    val edge = when (crystalPreset) {
        "emerald" -> Color(0xFF39FF14)
        "crimson" -> Color(0xFFFF2244)
        "cyan" -> Color(0xFF00F0FF)
        "rainbow" -> TrancePalette.color(phase * 0.001f, VizPaletteMode.RAINBOW, palette, sat, glow)
        else -> Color(0xFFB14EFF)
    }
    rotate(phase * 0.015f * (1f + features.presence * 0.4f), Offset(cx, cy)) {
        for (face in 0 until detail) {
            val a0 = face / detail.toFloat() * PI.toFloat() * 2f
            val a1 = (face + 1) / detail.toFloat() * PI.toFloat() * 2f
            val deform = 1f + features.lowMid * 0.25f * sin(a0 * 3f + phase * 0.02f) +
                features.upperMid * 0.2f * cos(a0 * 5f) +
                fft[face % fft.size] * 0.3f
            val shock = 1f + features.onset * 0.35f
            val r = scale * deform * shock
            val top = Offset(cx, cy - scale * (0.55f + features.treble * 0.15f))
            val p0 = Offset(cx + cos(a0) * r, cy + sin(a0) * r * 0.55f)
            val p1 = Offset(cx + cos(a1) * r, cy + sin(a1) * r * 0.55f)
            val path = Path().apply {
                moveTo(top.x, top.y)
                lineTo(p0.x, p0.y)
                lineTo(p1.x, p1.y)
                close()
            }
            drawPath(path, Color(0xFF0A0A10).copy(alpha = 0.85f))
            drawPath(path, edge.copy(alpha = 0.55f + features.treble * 0.35f), style = Stroke(1.2f + features.rms))
            // Internal glow channel
            drawLine(
                edge.copy(alpha = 0.2f + features.presence * 0.35f),
                top,
                Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f),
                strokeWidth = 1f
            )
        }
    }
    drawCircle(
        brush = Brush.radialGradient(
            listOf(edge.copy(alpha = 0.25f + features.onset * 0.3f), Color.Transparent)
        ),
        radius = scale * 1.2f,
        center = Offset(cx, cy)
    )
}

enum class VissonanceSubmode { SPECTRAL_ARCHITECTURE, PARTICLE_MATRIX, GEOMETRIC_ORBIT, HARMONIC_FIELD }

fun DrawScope.drawVissonanceMatrixScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean,
    paletteMode: VizPaletteMode,
    quality: VizQualityTier,
    sat: Float,
    glow: Float,
    submode: VissonanceSubmode
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.35f
    when (submode) {
        VissonanceSubmode.SPECTRAL_ARCHITECTURE -> {
            val cols = (24 * quality.meshScale).toInt().coerceIn(12, 36)
            for (i in 0 until cols) {
                val x = (i + 0.5f) / cols * size.width
                val v = fft[i % fft.size]
                val h = size.height * (0.15f + v * 0.55f + features.bass * 0.1f) * drive
                val c = TrancePalette.color(i / cols.toFloat(), paletteMode, palette, sat, glow)
                val top = size.height * 0.7f - h
                drawLine(c.copy(alpha = 0.8f), Offset(x, size.height * 0.7f), Offset(x, top), strokeWidth = 3f)
                drawCircle(c, radius = 2f + features.treble * 3f, center = Offset(x, top))
            }
        }
        VissonanceSubmode.PARTICLE_MATRIX -> {
            val n = (160 * quality.particleScale).toInt().coerceIn(60, 260)
            val cols = 20
            val rows = (n / cols).coerceAtLeast(4)
            for (i in 0 until n) {
                val col = i % cols
                val row = i / cols
                val u = col / (cols - 1f)
                val v = row / (rows - 1f).coerceAtLeast(1f)
                val band = fft[(col * fft.size / cols).coerceIn(0, fft.lastIndex)]
                val x = u * size.width
                val y = v * size.height + sin(u * 6f + phase * 0.02f) * features.lowMid * 20f -
                    band * features.bass * 40f * drive
                val c = TrancePalette.color(u + v * 0.3f, paletteMode, palette, sat, glow)
                drawCircle(
                    c.copy(alpha = 0.45f + band * 0.5f),
                    radius = 1.5f + band * 4f + features.onset * 2f,
                    center = Offset(x, y)
                )
            }
        }
        VissonanceSubmode.GEOMETRIC_ORBIT -> {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val rings = (5 * quality.meshScale).toInt().coerceIn(3, 8)
            for (r in 0 until rings) {
                val rad = immersiveViewportDim(size.width, size.height) * (0.12f + r * 0.08f) *
                    (1f + features.bass * 0.14f) * drive
                val steps = 48
                val path = Path()
                for (i in 0..steps) {
                    val a = i / steps.toFloat() * PI.toFloat() * 2f + phase * 0.01f * (r + 1)
                    val wobble = 1f + features.upperMid * 0.15f * sin(a * (3 + r)) +
                        fft[i % fft.size] * 0.2f
                    val x = cx + cos(a) * rad * wobble
                    val y = cy + sin(a) * rad * wobble * 0.7f
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                val c = TrancePalette.color(r / rings.toFloat(), paletteMode, palette, sat, glow)
                drawPath(path, c.copy(alpha = 0.7f), style = Stroke(1.5f + features.treble))
            }
        }
        VissonanceSubmode.HARMONIC_FIELD -> {
            val gridX = (18 * quality.meshScale).toInt().coerceIn(10, 24)
            val gridY = (10 * quality.meshScale).toInt().coerceIn(6, 14)
            for (iy in 0 until gridY) {
                for (ix in 0 until gridX) {
                    val u = ix / (gridX - 1f)
                    val v = iy / (gridY - 1f)
                    val band = fft[((u * (fft.size - 1)).toInt())]
                    val dx = features.lowMid * 16f * sin(v * 4f + phase * 0.02f)
                    val dy = features.bass * 22f * band * drive +
                        features.treble * 8f * cos(u * 8f + phase * 0.03f)
                    val x = u * size.width + dx
                    val y = v * size.height + dy
                    val c = TrancePalette.color(u * 0.5f + v * 0.5f, paletteMode, palette, sat, glow)
                    drawCircle(c.copy(alpha = 0.4f + band * 0.45f), radius = 2f + band * 3f, center = Offset(x, y))
                    if (ix < gridX - 1) {
                        val x2 = (ix + 1) / (gridX - 1f) * size.width
                        drawLine(c.copy(alpha = 0.15f), Offset(x, y), Offset(x2 + dx * 0.5f, y), strokeWidth = 1f)
                    }
                }
            }
        }
    }
}
