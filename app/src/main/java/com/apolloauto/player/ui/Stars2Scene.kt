package com.apolloauto.player.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.apolloauto.player.media.AudioFeatures
import com.apolloauto.player.theme.HudPalette
import kotlin.math.max
import kotlin.random.Random

/**
 * Compose port of the streaking starfield idea from
 * [mostafa7904/Stars-Audio-Visualizer](https://github.com/mostafa7904/Stars-Audio-Visualizer)
 * — FFT-driven velocity + short trailing strokes. Not a JS/AudioContext port.
 */

private data class Star2(
    var x: Float,
    var y: Float,
    var z: Float
)

private object Stars2Field {
    private const val MIN_SCALE = 0.2f
    private val stars = ArrayList<Star2>(160)
    private var lastW = 0f
    private var lastH = 0f
    private var velocity = 1f

    fun ensure(width: Float, height: Float) {
        val target = ((width + height) / 8f).toInt().coerceIn(60, 220)
        if (stars.isEmpty() || width != lastW || height != lastH || stars.size != target) {
            lastW = width
            lastH = height
            stars.clear()
            repeat(target) {
                stars += Star2(
                    x = Random.nextFloat() * width,
                    y = Random.nextFloat() * height,
                    z = MIN_SCALE + Random.nextFloat() * (1f - MIN_SCALE)
                )
            }
        }
    }

    fun tick(fft: FloatArray, playing: Boolean) {
        // Mirror JS: sample a bin into global velocity (smoothed).
        val idx = (fft.size * 0.2f).toInt().coerceIn(0, fft.lastIndex)
        val instant = if (playing) (fft[idx] * 12f).coerceIn(0.35f, 14f) else 0.4f
        velocity = velocity * 0.72f + instant * 0.28f
        val w = lastW
        val h = lastH
        for (star in stars) {
            star.x += Random.nextFloat() * velocity
            star.y += Random.nextFloat() * velocity
            if (star.x >= w) star.x = Random.nextFloat() * 4f
            if (star.y >= h) star.y = Random.nextFloat() * 4f
        }
    }

    fun draw(
        scope: DrawScope,
        palette: HudPalette,
        features: AudioFeatures,
        playing: Boolean
    ) = with(scope) {
        val starSize = 3f
        val tail = max(0.5f, velocity * 2f)
        for ((i, star) in stars.withIndex()) {
            val alpha = (0.45f + 0.45f * Random.nextFloat() + features.rms * 0.15f)
                .coerceIn(0.25f, 0.95f)
            val color = if (i % 7 == 0) {
                palette.accent.copy(alpha = alpha)
            } else {
                palette.soft.copy(alpha = alpha)
            }
            drawLine(
                color = color,
                start = Offset(star.x, star.y),
                end = Offset(star.x + tail, star.y + tail),
                strokeWidth = starSize * star.z * if (playing) 1f else 0.7f,
                cap = StrokeCap.Round
            )
        }
    }
}

fun DrawScope.drawStars2Scene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    Stars2Field.ensure(size.width, size.height)
    Stars2Field.tick(fft, playing)
    Stars2Field.draw(this, palette, features, playing)
    // Soft center bloom on bass so it feels tied to the track.
    if (playing && features.bass > 0.25f) {
        drawCircle(
            color = palette.primary.copy(alpha = 0.06f + features.bass * 0.12f),
            radius = size.minDimension * (0.12f + features.bass * 0.1f),
            center = Offset(size.width / 2f, size.height / 2f)
        )
    }
}
