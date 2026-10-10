package com.apolloauto.player.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.apolloauto.player.media.AudioFeatures
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * XR3Player-inspired visualizer aesthetics (Compose).
 * Inspired by goxr3plus/XR3Player display modes (LGPL-3.0) — original Apollo
 * Canvas scenes, not a JavaFX source port. See docs/licenses/GOXR3PLUS_XR3PLAYER_LGPL_NOTE.txt.
 */

fun DrawScope.drawXr3OscilloScene(
    features: AudioFeatures,
    palette: HudPalette,
    wave: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.4f
    val midY = size.height * 0.5f
    val amp = size.height * (0.28f + features.rms * 0.18f) * drive
    fun strokeChannel(offsetY: Float, color: Color, invert: Boolean) {
        val path = Path()
        val n = wave.size.coerceAtLeast(2)
        for (i in 0 until n) {
            val x = i / (n - 1f) * size.width
            val v = wave[i] * (if (invert) -1f else 1f)
            val y = midY + offsetY + v * amp
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            color.copy(alpha = 0.9f),
            style = Stroke(2.2f + features.onset * 2f, cap = StrokeCap.Round)
        )
    }
    strokeChannel(-size.height * 0.12f, palette.primary, false)
    strokeChannel(size.height * 0.12f, palette.secondary, true)
    drawLine(
        palette.dim.copy(alpha = 0.35f),
        Offset(0f, midY),
        Offset(size.width, midY),
        strokeWidth = 1f
    )
}

fun DrawScope.drawXr3CircleScene(
    features: AudioFeatures,
    palette: HudPalette,
    spectrum: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.35f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = min(size.width, size.height) * (0.22f + features.bass * 0.04f)
    val spokes = 72
    var prevEnd: Offset? = null
    for (i in 0 until spokes) {
        val ang = (i / spokes.toFloat()) * PI.toFloat() * 2f + phase * 0.002f
        val band = spectrum[(i * spectrum.size / spokes).coerceIn(0, spectrum.lastIndex)]
        val add = abs(band) * radius * (0.9f + features.rms) * drive
        val sx = cx + sin(ang) * radius
        val sy = cy + cos(ang) * radius
        val ex = cx + sin(ang) * (radius + add)
        val ey = cy + cos(ang) * (radius + add)
        val ix = cx + sin(ang) * (radius - add * 0.55f)
        val iy = cy + cos(ang) * (radius - add * 0.55f)
        val hue = ((i * 5 + phase * 0.2f) % 360f) / 360f
        val color = Color.hsv(hue * 360f, 0.85f, 1f)
        drawLine(color.copy(alpha = 0.85f), Offset(sx, sy), Offset(ex, ey), strokeWidth = 2f)
        drawLine(color.copy(alpha = 0.45f), Offset(sx, sy), Offset(ix, iy), strokeWidth = 1.5f)
        prevEnd?.let {
            drawLine(color.copy(alpha = 0.55f), it, Offset(ex, ey), strokeWidth = 1.2f)
        }
        prevEnd = Offset(ex, ey)
    }
    drawCircle(
        palette.soft.copy(alpha = 0.15f + features.onset * 0.25f),
        radius = radius * (0.35f + features.bass * 0.2f),
        center = Offset(cx, cy)
    )
}

fun DrawScope.drawXr3PolyScene(
    features: AudioFeatures,
    palette: HudPalette,
    spectrum: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.4f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val arms = 5
    val turns = 9
    val steps = arms * turns * 8
    val path = Path()
    for (i in 0 until steps) {
        val t = i / steps.toFloat()
        val ang = t * PI.toFloat() * 2f * turns + phase * 0.003f
        val band = spectrum[(t * (spectrum.size - 1)).toInt().coerceIn(0, spectrum.lastIndex)]
        val r = min(size.width, size.height) * (0.08f + t * 0.38f) *
            (1f + band * 0.35f * drive + features.rms * 0.08f)
        val x = cx + cos(ang) * r
        val y = cy + sin(ang) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path,
        brush = Brush.sweepGradient(
            listOf(palette.primary, palette.secondary, palette.accent, palette.primary)
        ),
        style = Stroke(2.5f + features.onset * 2f, cap = StrokeCap.Round)
    )
}

fun DrawScope.drawXr3BarsScene(
    features: AudioFeatures,
    palette: HudPalette,
    spectrum: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.35f
    val n = spectrum.size.coerceAtLeast(8)
    val gap = 2f
    val barW = (size.width - gap * (n + 1)) / n
    val floor = size.height * 0.94f
    for (i in 0 until n) {
        val v = (spectrum[i] * drive).coerceIn(0f, 1.2f)
        val h = size.height * (0.05f + v * 0.85f)
        val x = gap + i * (barW + gap)
        val segments = (h / 4f).toInt().coerceAtLeast(1)
        for (s in 0 until segments) {
            val t = s / segments.toFloat()
            val y = floor - (s + 1) * 4f
            val color = when {
                t > 0.75f -> palette.accent
                t > 0.4f -> palette.primary
                else -> palette.secondary
            }
            drawRect(color.copy(alpha = 0.55f + t * 0.4f), Offset(x, y), Size(barW, 2.5f))
        }
        // Peak marker
        drawRect(
            palette.soft.copy(alpha = 0.9f),
            Offset(x, floor - h - 3f),
            Size(barW, 2f)
        )
    }
    drawCircle(
        palette.primary.copy(alpha = 0.08f + features.bass * 0.12f),
        radius = min(size.width, size.height) * (0.2f + features.rms * 0.1f),
        center = Offset(size.width / 2f, size.height / 2f)
    )
}

fun DrawScope.drawXr3VuScene(
    features: AudioFeatures,
    palette: HudPalette,
    level: Float,
    bass: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.3f
    val left = (level * 0.65f + bass * 0.35f) * drive
    val right = (level * 0.55f + features.mid * 0.45f) * drive
    val barH = size.height * 0.28f
    fun meter(y: Float, value: Float, labelColor: Color) {
        val maxW = size.width - 24f
        val w = maxW * value.coerceIn(0f, 1f)
        var x = 12f
        while (x < 12f + w) {
            val t = ((x - 12f) / maxW).coerceIn(0f, 1f)
            val c = when {
                t > 0.85f -> palette.accent
                t > 0.55f -> palette.primary
                else -> labelColor
            }
            drawRect(c.copy(alpha = 0.85f), Offset(x, y), Size(3f, barH))
            x += 5f
        }
        // Tick marks
        var tick = 12f
        while (tick < size.width - 12f) {
            drawRect(Color.Black.copy(alpha = 0.55f), Offset(tick, y), Size(1f, barH))
            tick += 16f
        }
    }
    meter(size.height * 0.22f, left, palette.secondary)
    meter(size.height * 0.55f, right, palette.primary)
}

fun DrawScope.drawXr3SierpinskiScene(
    features: AudioFeatures,
    palette: HudPalette,
    spectrum: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.4f
    val cx = size.width / 2f
    val top = size.height * 0.12f
    val baseY = size.height * 0.88f
    val half = min(size.width, size.height) * (0.38f + features.bass * 0.06f)
    val depth = (3 + (features.rms * 2f).toInt()).coerceIn(3, 5)
    fun tri(ax: Float, ay: Float, bx: Float, by: Float, cx2: Float, cy2: Float, d: Int) {
        if (d <= 0) {
            val path = Path().apply {
                moveTo(ax, ay); lineTo(bx, by); lineTo(cx2, cy2); close()
            }
            val band = spectrum[((ax / size.width) * spectrum.lastIndex).toInt().coerceIn(0, spectrum.lastIndex)]
            drawPath(
                path,
                palette.primary.copy(alpha = 0.15f + band * 0.35f * drive),
                style = Stroke(1.2f + features.onset)
            )
            return
        }
        val mx1 = (ax + bx) / 2f
        val my1 = (ay + by) / 2f
        val mx2 = (bx + cx2) / 2f
        val my2 = (by + cy2) / 2f
        val mx3 = (cx2 + ax) / 2f
        val my3 = (cy2 + ay) / 2f
        tri(ax, ay, mx1, my1, mx3, my3, d - 1)
        tri(mx1, my1, bx, by, mx2, my2, d - 1)
        tri(mx3, my3, mx2, my2, cx2, cy2, d - 1)
    }
    rotate(phase * 0.02f * drive, Offset(cx, size.height / 2f)) {
        tri(cx, top, cx - half, baseY, cx + half, baseY, depth)
    }
}

fun DrawScope.drawXr3JuliaScene(
    features: AudioFeatures,
    palette: HudPalette,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.35f
    // Lightweight point sampling of a Julia-like set, audio-warped — not a pixel fractal port.
    val cx = -0.4f + features.bass * 0.15f * drive
    val cy = 0.6f + sin(phase * 0.01f) * 0.08f + features.mid * 0.05f
    val cols = 48
    val rows = 28
    val maxIter = 12
    val scale = 1.35f - features.rms * 0.2f
    for (iy in 0 until rows) {
        for (ix in 0 until cols) {
            var zx = (ix / (cols - 1f) * 2f - 1f) * scale
            var zy = (iy / (rows - 1f) * 2f - 1f) * scale * (size.height / size.width)
            var iter = 0
            while (iter < maxIter && zx * zx + zy * zy < 4f) {
                val xt = zx * zx - zy * zy + cx
                zy = 2f * zx * zy + cy
                zx = xt
                iter++
            }
            if (iter >= maxIter) continue
            val t = iter / maxIter.toFloat()
            val px = ix / (cols - 1f) * size.width
            val py = iy / (rows - 1f) * size.height
            val color = when {
                t < 0.33f -> palette.secondary
                t < 0.66f -> palette.primary
                else -> palette.accent
            }
            drawCircle(
                color.copy(alpha = 0.35f + t * 0.55f),
                radius = 2.2f + features.onset * 2f,
                center = Offset(px, py)
            )
        }
    }
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                palette.soft.copy(alpha = 0.2f + features.onset * 0.25f),
                Color.Transparent
            )
        ),
        radius = min(size.width, size.height) * (0.25f + features.bass * 0.15f),
        center = Offset(size.width / 2f, size.height / 2f)
    )
}

fun DrawScope.drawXr3SpriteScene(
    features: AudioFeatures,
    palette: HudPalette,
    spectrum: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.4f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = min(size.width, size.height) * (0.28f + features.bass * 0.08f) * drive
    val lat = 10
    val lon = 16
    for (i in 0..lat) {
        val v = i / lat.toFloat()
        val y = cy + (v - 0.5f) * 2f * r
        val ringR = r * sin(v * PI.toFloat())
        val band = spectrum[(v * spectrum.lastIndex).toInt().coerceIn(0, spectrum.lastIndex)]
        drawCircle(
            color = palette.primary.copy(alpha = 0.2f + band * 0.45f),
            radius = ringR * (1f + band * 0.15f),
            center = Offset(cx, y),
            style = Stroke(1.2f + features.onset)
        )
    }
    for (j in 0 until lon) {
        val ang0 = j / lon.toFloat() * PI.toFloat() * 2f + phase * 0.004f
        val path = Path()
        for (i in 0..lat) {
            val v = i / lat.toFloat()
            val y = cy + (v - 0.5f) * 2f * r
            val ringR = r * sin(v * PI.toFloat())
            val x = cx + cos(ang0) * ringR
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, palette.secondary.copy(alpha = 0.55f), style = Stroke(1.4f))
    }
}
