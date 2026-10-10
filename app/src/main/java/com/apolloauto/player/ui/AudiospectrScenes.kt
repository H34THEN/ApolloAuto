package com.apolloauto.player.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.apolloauto.player.media.AudioFeatures
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Live Compose ports of [bradsec/audiospectr](https://github.com/bradsec/audiospectr)
 * layer styles (MIT). Not a Python/FFmpeg video exporter — immersive realtime only.
 */

fun DrawScope.drawWavebarsScene(
    features: AudioFeatures,
    palette: HudPalette,
    wave: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val barCount = 64
    val values = WavebarSmoother.envelope(wave, barCount, playing)
    val midY = size.height * 0.5f
    val maxHalf = size.height * (0.28f + features.rms * 0.08f)
    val slot = size.width / barCount
    val barW = (slot * 0.72f).coerceAtLeast(2f)
    val color = palette.primary
    val reflect = palette.primary.copy(alpha = 0.55f)
    for (i in 0 until barCount) {
        val v = values[i].coerceIn(0f, 1f)
        val half = max(2f, v * maxHalf)
        val x = i * slot + (slot - barW) * 0.5f
        drawRoundRect(
            color = color.copy(alpha = 0.92f),
            topLeft = Offset(x, midY - half),
            size = Size(barW, half),
            cornerRadius = CornerRadius(barW * 0.35f, barW * 0.35f)
        )
        drawRoundRect(
            color = reflect,
            topLeft = Offset(x, midY),
            size = Size(barW, half * 0.9f),
            cornerRadius = CornerRadius(barW * 0.35f, barW * 0.35f)
        )
    }
    drawLine(
        color = palette.accent.copy(alpha = 0.35f + features.onset * 0.4f),
        start = Offset(0f, midY),
        end = Offset(size.width, midY),
        strokeWidth = 1.5f
    )
}

fun DrawScope.drawMirrorBarsScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val barCount = 36
    val halfW = size.width * 0.5f
    val spacing = halfW * 0.012f
    val barW = ((halfW - spacing * (barCount + 1)) / barCount).coerceAtLeast(2f)
    val scale = if (playing) 1f else 0.55f
    for (i in 0 until barCount) {
        val raw = fft[(i * fft.size / barCount).coerceIn(0, fft.lastIndex)]
        val v = (raw * 2.4f).coerceIn(0f, 1f).pow(0.7f) * scale
        val h = size.height * (0.08f + v * 0.84f)
        val xL = spacing + i * (barW + spacing)
        val xR = size.width - spacing - barW - i * (barW + spacing)
        val y = size.height - h
        val brush = Brush.verticalGradient(
            listOf(palette.accent, palette.primary, palette.secondary.copy(alpha = 0.75f))
        )
        drawRoundRect(
            brush = brush,
            topLeft = Offset(xL, y),
            size = Size(barW, h),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawRoundRect(
            brush = brush,
            topLeft = Offset(xR, y),
            size = Size(barW, h),
            cornerRadius = CornerRadius(3f, 3f)
        )
        if (h > 8f) {
            drawRect(
                color = palette.soft.copy(alpha = 0.7f),
                topLeft = Offset(xL, y),
                size = Size(barW, 2.5f)
            )
            drawRect(
                color = palette.soft.copy(alpha = 0.7f),
                topLeft = Offset(xR, y),
                size = Size(barW, 2.5f)
            )
        }
    }
    drawLine(
        color = palette.primary.copy(alpha = 0.2f + features.bass * 0.25f),
        start = Offset(halfW, size.height * 0.1f),
        end = Offset(halfW, size.height * 0.95f),
        strokeWidth = 1f
    )
}

fun DrawScope.drawPulseScene(
    features: AudioFeatures,
    palette: HudPalette,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val drive = (features.bass * 0.75f + features.onset * 0.45f).coerceIn(0f, 1.2f) *
        if (playing) 1f else 0.4f
    val base = size.minDimension * 0.18f
    val radius = base * (1f + 0.75f * drive)
    val ringCount = 4
    for (r in 0 until ringCount) {
        val rr = radius * (0.45f + r * 0.28f + features.rms * 0.05f)
        val alpha = (0.55f - r * 0.1f + drive * 0.2f).coerceIn(0.12f, 0.75f)
        drawCircle(
            color = (if (r % 2 == 0) palette.primary else palette.secondary).copy(alpha = alpha),
            radius = rr,
            center = Offset(cx, cy),
            style = Stroke(width = 3f + drive * 4f)
        )
    }
    drawCircle(
        color = palette.accent.copy(alpha = 0.18f + features.rms * 0.35f),
        radius = base * (0.28f + drive * 0.2f),
        center = Offset(cx, cy)
    )
    val spokes = 16
    val rot = phase * 0.003f
    for (s in 0 until spokes) {
        val ang = s / spokes.toFloat() * PI.toFloat() * 2f + rot
        val inner = radius * 0.55f
        val outer = radius * (1.05f + features.mid * 0.25f)
        drawLine(
            color = palette.accent.copy(alpha = 0.25f + drive * 0.35f),
            start = Offset(cx + cos(ang) * inner, cy + sin(ang) * inner),
            end = Offset(cx + cos(ang) * outer, cy + sin(ang) * outer),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

fun DrawScope.drawParticlesScene(
    features: AudioFeatures,
    palette: HudPalette,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val field = ParticleField
    field.tick(
        width = size.width,
        height = size.height,
        rms = features.rms * if (playing) 1f else 0.2f,
        bass = features.bass,
        onset = features.onset
    )
    for (p in field.particles) {
        val life = (1f - p.age / p.lifetime).coerceIn(0f, 1f)
        val color = if (p.accent) palette.accent else palette.primary
        drawCircle(
            color = color.copy(alpha = 0.15f + life * 0.7f),
            radius = p.size * (0.6f + life * 0.6f),
            center = Offset(p.x, p.y)
        )
    }
    // Soft center bloom
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                palette.primary.copy(alpha = 0.12f + features.rms * 0.15f),
                Color.Transparent
            ),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.minDimension * 0.35f
        ),
        radius = size.minDimension * 0.35f,
        center = Offset(size.width / 2f, size.height / 2f)
    )
}

private object WavebarSmoother {
    private var prev: FloatArray? = null

    fun envelope(wave: FloatArray, barCount: Int, playing: Boolean): FloatArray {
        val out = FloatArray(barCount)
        if (wave.isEmpty()) return out
        val chunk = (wave.size / barCount).coerceAtLeast(1)
        for (i in 0 until barCount) {
            var peak = 0f
            val start = i * chunk
            val end = minOf(wave.size, start + chunk)
            for (j in start until end) peak = max(peak, kotlin.math.abs(wave[j]))
            // Approx 90th percentile via soft boost
            out[i] = (peak * 2.4f).coerceIn(0f, 1f).pow(0.9f) * if (playing) 1f else 0.35f
        }
        val p = prev
        if (p != null && p.size == barCount) {
            for (i in out.indices) out[i] = p[i] * 0.55f + out[i] * 0.45f
        }
        prev = out.copyOf()
        return out
    }
}

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var lifetime: Float,
    var age: Float = 0f,
    var size: Float = 4f,
    var accent: Boolean = false
)

private object ParticleField {
    val particles = ArrayList<Particle>(256)
    private const val MAX = 420

    fun tick(width: Float, height: Float, rms: Float, bass: Float, onset: Float) {
        val cx = width * 0.5f
        val cy = height * 0.5f
        val volume = rms.coerceIn(0f, 1f)
        val spawn = if (volume > 0.02f) {
            ((18f * volume * 2f) + onset * 12f).toInt().coerceIn(0, 28)
        } else 0
        val speedBase = volume * 7f + bass * 2f
        repeat(spawn) {
            if (particles.size >= MAX) return@repeat
            val angle = Random.nextFloat() * PI.toFloat() * 2f
            val jitter = Random.nextFloat() * 0.6f + 0.65f
            val speed = speedBase * jitter
            val spawnR = Random.nextFloat() * minOf(width, height) * 0.22f
            val sa = Random.nextFloat() * PI.toFloat() * 2f
            particles += Particle(
                x = cx + cos(sa) * spawnR,
                y = cy + sin(sa) * spawnR,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                lifetime = 70f + Random.nextFloat() * 40f,
                size = 2.5f + Random.nextFloat() * 4f + onset * 2f,
                accent = Random.nextBoolean()
            )
        }
        var i = 0
        while (i < particles.size) {
            val p = particles[i]
            p.x += p.vx
            p.y += p.vy
            p.age += 1f
            if (p.age >= p.lifetime || p.x !in 0f..width || p.y !in 0f..height) {
                particles.removeAt(i)
            } else {
                i++
            }
        }
        if (particles.size > MAX) {
            particles.subList(0, particles.size - MAX).clear()
        }
    }
}
