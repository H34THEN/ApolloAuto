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
import com.apolloauto.player.media.VisualIntensity
import com.apolloauto.player.media.VisualResponseMapper
import com.apolloauto.player.media.VizPaletteMode
import com.apolloauto.player.media.VizQualityTier
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val GOLDEN_ANGLE = 2.399963229728653f

fun DrawScope.drawChromaticPhiScene(
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
    val n = (220 * quality.particleScale).toInt().coerceIn(80, 360)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val maxR = min(size.width, size.height) * (0.42f + features.bass * 0.08f)
    for (i in 0 until n) {
        val ang = i * GOLDEN_ANGLE + phase * 0.002f * (1f + features.upperMid)
        val rNorm = sqrt((i + 1f) / n)
        val band = fft[(i % fft.size)].coerceIn(0f, 1f)
        val r = maxR * rNorm * (1f + features.subBass * 0.15f + band * 0.2f) * drive
        val x = cx + cos(ang) * r
        val y = cy + sin(ang) * r
        val c = TrancePalette.color(
            t = i / n.toFloat() + phase * 0.0003f + features.presence * 0.1f,
            mode = paletteMode,
            palette = palette,
            saturation = sat,
            glow = glow
        )
        val rad = (1.6f + features.treble * 2.2f + band * 3f + features.onset * 2f) *
            quality.particleScale
        drawCircle(c.copy(alpha = 0.55f + band * 0.4f), radius = rad, center = Offset(x, y))
    }
}

fun DrawScope.drawEctoplasmScene(
    features: AudioFeatures,
    palette: HudPalette,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean,
    paletteMode: VizPaletteMode,
    quality: VizQualityTier,
    sat: Float,
    glow: Float
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.4f
    val blobs = (7 * quality.particleScale).toInt().coerceIn(4, 12)
    val cx = size.width / 2f
    val cy = size.height / 2f
    for (i in 0 until blobs) {
        val t = i / blobs.toFloat()
        val ang = t * PI.toFloat() * 2f + phase * 0.004f
        val orbit = min(size.width, size.height) *
            (0.12f + features.lowMid * 0.08f + features.bass * 0.06f)
        val bx = cx + cos(ang) * orbit * (1f + sin(phase * 0.01f + i) * 0.2f)
        val by = cy + sin(ang * 1.3f) * orbit * 0.85f
        val rad = min(size.width, size.height) *
            (0.1f + features.rms * 0.08f + features.subBass * 0.06f) *
            (0.7f + TrancePalette.hash01(i) * 0.5f) * drive
        val c = TrancePalette.color(t + features.upperMid * 0.2f, paletteMode, palette, sat, glow)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(c.copy(alpha = 0.55f), c.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(bx, by),
                radius = rad * (1.4f + features.onset * 0.4f)
            ),
            radius = rad * 1.4f,
            center = Offset(bx, by)
        )
        drawCircle(c.copy(alpha = 0.35f), radius = rad * 0.45f, center = Offset(bx, by))
    }
}

fun DrawScope.drawCircInfinityScene(
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
    val cx = size.width / 2f
    val cy = size.height / 2f
    val baseR = min(size.width, size.height) * 0.18f
    val spokes = (fft.size * quality.meshScale).toInt().coerceIn(24, fft.size)
    for (i in 0 until spokes) {
        val ang = i / spokes.toFloat() * PI.toFloat() * 2f + phase * 0.003f
        val v = fft[i % fft.size]
        val inner = baseR * (1f + features.lowMid * 0.25f)
        val outer = baseR * (1.6f + features.bass * 0.5f + v * 1.4f) * drive
        val c = TrancePalette.color(i / spokes.toFloat() + features.treble * 0.1f, paletteMode, palette, sat, glow)
        drawLine(
            c.copy(alpha = 0.75f),
            Offset(cx + cos(ang) * inner, cy + sin(ang) * inner),
            Offset(cx + cos(ang) * outer, cy + sin(ang) * outer),
            strokeWidth = 1.5f + features.presence * 2f + v * 2f
        )
    }
    for (ring in 0..3) {
        val r = baseR * (1.1f + ring * 0.35f + features.subBass * 0.2f)
        drawCircle(
            palette.soft.copy(alpha = 0.12f + features.onset * 0.15f),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(1f + features.treble)
        )
    }
}

fun DrawScope.drawPrismRainScene(
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
    val drive = if (playing) 1f else 0.28f
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val kick = VisualResponseMapper.kickPressure(d) * drive
    val spark = VisualResponseMapper.sparkle(d) * drive
    // Ambient drizzle + event-driven density (not timer-only rain).
    val drops = ((36 + d.rms * 40f + kick * 28f + spark * 20f) * quality.particleScale)
        .toInt().coerceIn(24, 120)
    val fall = phase * (1.6f + d.rms * 5.5f + kick * 3f) * drive
    val ground = size.height * 0.88f
    for (i in 0 until drops) {
        val x = (TrancePalette.hash01(i) * size.width + d.upperMid * 48f + d.lowMid * 24f) % size.width
        val speed = 0.45f + TrancePalette.hash01(i + 3) * 0.85f + d.treble * 1.1f + kick * 0.6f
        val y = ((fall * speed * 42f) + TrancePalette.hash01(i + 7) * size.height) % size.height
        val band = fft[i % fft.size]
        // Kick → thick long drops; treble → fine needles; onset → impact length.
        val len = 6f + band * 32f + kick * 36f + spark * 14f + d.onset * 22f
        val thick = 1.1f + d.presence * 1.4f + kick * 2.2f + if (i % 7 == 0) kick * 2f else 0f
        val c = TrancePalette.color(
            i / drops.toFloat() + phase * 0.0005f + d.lowMid * 0.08f,
            paletteMode, palette, sat, glow
        )
        drawLine(
            c.copy(alpha = (0.5f + band * 0.45f + spark * 0.2f).coerceIn(0.35f, 1f)),
            Offset(x, y),
            Offset(x + sin(i.toFloat()) * (3f + d.upperMid * 8f), y + len),
            strokeWidth = thick,
            cap = StrokeCap.Round
        )
        // Impact ripples — driven by onset/kick, not a constant splash.
        val impact = kick > 0.5f || d.onset > 0.32f
        if (impact && (i % 4 == 0 || (d.onset > 0.55f && i % 2 == 0))) {
            val ix = x
            val ripple = 8f + kick * 42f + d.onset * 28f + band * 12f
            drawCircle(
                c.copy(alpha = (0.2f + d.onset * 0.45f).coerceAtMost(0.55f)),
                radius = ripple,
                center = Offset(ix, ground),
                style = Stroke(1.4f + kick * 2f)
            )
            drawCircle(
                c.copy(alpha = 0.35f + kick * 0.3f),
                radius = 2.5f + kick * 4f,
                center = Offset(ix, ground)
            )
        }
    }
}

// SYNTH SMOKE renderer lives in SynthSmokeScene.kt (V5.1 procedural vapor).

fun DrawScope.drawNoteSwarmScene(
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
    val n = (fft.size * 2 * quality.particleScale).toInt().coerceIn(32, 120)
    for (i in 0 until n) {
        val band = fft[i % fft.size]
        val region = i / n.toFloat()
        val energy = when {
            region < 0.2f -> features.subBass
            region < 0.4f -> features.bass
            region < 0.6f -> features.lowMid
            region < 0.8f -> features.upperMid
            else -> features.treble
        }
        val ang = region * PI.toFloat() * 6f + phase * 0.005f * (1f + energy)
        val r = min(size.width, size.height) * (0.1f + region * 0.35f) *
            (1f + band * 0.5f + energy * 0.3f) * drive
        val x = size.width / 2f + cos(ang) * r * (1.1f + features.onset * 0.2f)
        val y = size.height / 2f + sin(ang) * r * 0.75f
        val c = TrancePalette.color(region + band * 0.2f, paletteMode, palette, sat, glow)
        drawCircle(c.copy(alpha = 0.5f + band * 0.45f), radius = 2f + band * 5f + energy * 2f, center = Offset(x, y))
    }
}

fun DrawScope.drawHarmonicMandalaScene(
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
    val cx = size.width / 2f
    val cy = size.height / 2f
    val folds = (6 + (features.lowMid * 4f).toInt()).coerceIn(6, 12)
    val rings = (4 * quality.meshScale).toInt().coerceIn(3, 7)
    rotate(phase * 0.02f * (1f + features.presence * 0.5f), Offset(cx, cy)) {
        for (r in 0 until rings) {
            val rad = min(size.width, size.height) * (0.12f + r * 0.08f) *
                (1f + features.bass * 0.1f) * drive
            val path = Path()
            val steps = folds * 8
            for (i in 0..steps) {
                val a = i / steps.toFloat() * PI.toFloat() * 2f
                val petal = 1f + 0.35f * sin(a * folds) * (0.6f + features.upperMid) +
                    features.treble * 0.15f * sin(a * folds * 2f + phase * 0.01f)
                val band = fft[(i * fft.size / steps).coerceIn(0, fft.lastIndex)]
                val rr = rad * petal * (1f + band * 0.15f)
                val x = cx + cos(a) * rr
                val y = cy + sin(a) * rr
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            val c = TrancePalette.color(r / rings.toFloat() + features.onset * 0.1f, paletteMode, palette, sat, glow)
            drawPath(path, c.copy(alpha = 0.55f), style = Stroke(1.5f + features.rms * 2f))
        }
    }
}

fun DrawScope.drawLiquidSpectrumScene(
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
    val cols = (fft.size * quality.meshScale).toInt().coerceIn(24, fft.size)
    val path = Path()
    val midY = size.height * 0.55f
    for (i in 0 until cols) {
        val x = i / (cols - 1f) * size.width
        val v = fft[i % fft.size]
        val bandBoost = when {
            i < cols * 0.2f -> features.subBass
            i < cols * 0.45f -> features.bass
            i < cols * 0.7f -> features.lowMid
            else -> features.treble
        }
        val h = size.height * (0.08f + v * 0.35f + bandBoost * 0.12f) * drive
        val y = midY - h + sin(i * 0.25f + phase * 0.015f) * features.upperMid * 18f
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    for (i in cols - 1 downTo 0) {
        val x = i / (cols - 1f) * size.width
        val v = fft[i % fft.size]
        val y = midY + size.height * (0.06f + v * 0.28f) * drive
        path.lineTo(x, y)
    }
    path.close()
    val c1 = TrancePalette.color(phase * 0.0004f, paletteMode, palette, sat, glow)
    val c2 = TrancePalette.color(phase * 0.0004f + 0.35f, paletteMode, palette, sat, glow)
    drawPath(
        path,
        brush = Brush.verticalGradient(listOf(c1.copy(alpha = 0.75f), c2.copy(alpha = 0.35f)))
    )
    drawPath(path, c1.copy(alpha = 0.8f), style = Stroke(2f + features.onset * 2f))
}
