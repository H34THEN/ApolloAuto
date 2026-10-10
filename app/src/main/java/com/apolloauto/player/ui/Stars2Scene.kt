package com.apolloauto.player.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.apolloauto.player.media.AudioFeatures
import com.apolloauto.player.media.BandDrive
import com.apolloauto.player.media.VisualIntensity
import com.apolloauto.player.media.VisualResponseMapper
import com.apolloauto.player.theme.HudPalette
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * STARS2 — multilayer streak field (crash-safe).
 * V5.2 bug: multiplicative radial "breath" each frame exploded positions to Infinity.
 */

private data class Star2(
    var x: Float,
    var y: Float,
    var z: Float,
    val layer: Int
)

private object Stars2Field {
    private const val MIN_SCALE = 0.15f
    private val stars = ArrayList<Star2>(220)
    private var lastW = 0f
    private var lastH = 0f
    private var velFar = 1f
    private var velMid = 1f
    private var velNear = 1f
    private var orbit = 0f
    private val rng = Random(0x51A22)

    fun ensure(width: Float, height: Float) {
        if (width <= 1f || height <= 1f || !width.isFinite() || !height.isFinite()) return
        val target = ((width + height) / 6f).toInt().coerceIn(90, 260)
        if (stars.isEmpty() || width != lastW || height != lastH || stars.size != target) {
            lastW = width
            lastH = height
            stars.clear()
            repeat(target) { i ->
                stars += Star2(
                    x = rng.nextFloat() * width,
                    y = rng.nextFloat() * height,
                    z = MIN_SCALE + rng.nextFloat() * (1f - MIN_SCALE),
                    layer = i % 3
                )
            }
        }
    }

    fun tick(d: BandDrive, fft: FloatArray, playing: Boolean) {
        if (lastW <= 1f || lastH <= 1f) return
        val drive = if (playing) 1f else 0.3f
        val kick = VisualResponseMapper.kickPressure(d).coerceIn(0f, 2.2f) * drive
        val spark = VisualResponseMapper.sparkle(d).coerceIn(0f, 1.8f) * drive
        val lowBin = safeBin(fft, 0.12f)
        val midBin = safeBin(fft, 0.4f)
        val hiBin = safeBin(fft, 0.75f)
        fun chase(cur: Float, target: Float) =
            (cur * 0.55f + target.coerceIn(0.2f, 18f) * 0.45f).coerceIn(0.2f, 18f)
        velFar = chase(velFar, (0.4f + lowBin * 14f + d.bass * 7f + kick * 5f) * drive)
        velMid = chase(velMid, (0.5f + midBin * 12f + d.lowMid * 6f + d.upperMid * 4f) * drive)
        velNear = chase(velNear, (0.6f + hiBin * 16f + spark * 8f) * drive)
        orbit = (orbit + 0.01f + d.upperMid * 0.03f + d.presence * 0.015f) % (PI2)
        val w = lastW
        val h = lastH
        val cx = w * 0.5f
        val cy = h * 0.5f
        for (star in stars) {
            val v = when (star.layer) {
                0 -> velFar
                1 -> velMid
                else -> velNear
            }
            // Additive radial nudge only (never multiply position by >1 each frame).
            val dx = star.x - cx
            val dy = star.y - cy
            val push = when (star.layer) {
                0 -> kick * 1.8f + d.sub * 1.2f
                1 -> d.lowMid * 1.4f
                else -> spark * 1.1f
            }.coerceIn(0f, 6f)
            val dist = max(1f, kotlin.math.hypot(dx, dy))
            star.x += (dx / dist) * push
            star.y += (dy / dist) * push
            val tang = orbit * (0.4f + star.layer * 0.25f) + star.z * 4f
            star.x += -sin(tang) * v * 0.35f + v * 0.55f
            star.y += cos(tang) * v * 0.25f + v * 0.4f
            if (!star.x.isFinite() || !star.y.isFinite() ||
                star.x < -8f || star.x > w + 8f || star.y < -8f || star.y > h + 8f
            ) {
                star.x = rng.nextFloat() * w
                star.y = rng.nextFloat() * h
                star.z = MIN_SCALE + rng.nextFloat() * (1f - MIN_SCALE)
            }
        }
    }

    fun draw(
        scope: DrawScope,
        palette: HudPalette,
        d: BandDrive,
        playing: Boolean
    ) = with(scope) {
        val kick = VisualResponseMapper.kickPressure(d).coerceIn(0f, 2.2f)
        val spark = VisualResponseMapper.sparkle(d).coerceIn(0f, 1.8f)
        for ((i, star) in stars.withIndex()) {
            if (!star.x.isFinite() || !star.y.isFinite()) continue
            val v = when (star.layer) {
                0 -> velFar
                1 -> velMid
                else -> velNear
            }.coerceIn(0.2f, 18f)
            val tail = max(1.2f, v * (1.4f + kick * 0.6f)).coerceAtMost(48f)
            val twinkle = if (star.layer == 2) {
                1f + spark * 0.6f * ((i * 17) % 5) / 4f
            } else {
                1f
            }.coerceIn(0.7f, 1.8f)
            val alpha = (0.4f + star.z * 0.45f + d.rms * 0.2f + spark * 0.15f)
                .coerceIn(0.3f, 1f) * (if (playing) 1f else 0.65f)
            val color = when (star.layer) {
                0 -> palette.primary.copy(alpha = alpha)
                1 -> palette.secondary.copy(alpha = alpha)
                else -> palette.accent.copy(alpha = (alpha * twinkle).coerceIn(0.25f, 1f))
            }
            val sw = ((2.2f + star.layer * 0.6f) * star.z * twinkle).coerceIn(0.5f, 10f)
            drawLine(
                color = color,
                start = Offset(star.x, star.y),
                end = Offset(star.x + tail, star.y + tail * 0.85f),
                strokeWidth = sw,
                cap = StrokeCap.Round
            )
        }
        if (playing && kick > 0.45f) {
            val r = (min(size.width, size.height) * (0.1f + kick * 0.18f)).coerceIn(4f, 400f)
            drawCircle(
                color = palette.accent.copy(alpha = (0.1f + kick * 0.18f).coerceAtMost(0.35f)),
                radius = r,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }
    }

    private fun safeBin(fft: FloatArray, frac: Float): Float {
        if (fft.isEmpty()) return 0f
        val idx = (fft.size * frac).toInt().coerceIn(0, fft.lastIndex)
        return fft[idx].coerceIn(0f, 1.5f)
    }

    private const val PI2 = (Math.PI * 2).toFloat()
}

fun DrawScope.drawStars2Scene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    if (size.width <= 1f || size.height <= 1f) return
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    Stars2Field.ensure(size.width, size.height)
    Stars2Field.tick(d, fft, playing)
    Stars2Field.draw(this, palette, d, playing)
}
