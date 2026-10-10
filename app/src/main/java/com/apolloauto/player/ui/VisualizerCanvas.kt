package com.apolloauto.player.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.R
import com.apolloauto.player.media.VisualizerMode
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.theme.LocalMotionProfile
import com.apolloauto.player.theme.MotionProfile
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ApolloVisualizerSurface(
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    val p = LocalHudPalette.current
    val motion = LocalMotionProfile.current
    val viz = ApolloApp.instance.visualizerController
    val fft by viz.fft.collectAsState()
    val wave by viz.waveform.collectAsState()
    val level by viz.level.collectAsState()
    val bass by viz.bass.collectAsState()
    val capturing by viz.capturing.collectAsState()
    val mode by viz.mode.collectAsState()
    val animate = motion != MotionProfile.STATIC

    val spin = rememberInfiniteTransition(label = "vizSpin")
    val spinAngle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    val pulse by spin.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val gridStep = size.width / 12f
            for (i in 0..12) {
                val x = i * gridStep
                drawLine(
                    color = p.primary.copy(alpha = 0.05f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
            }
            for (i in 0..6) {
                val y = i * (size.height / 6f)
                drawLine(
                    color = p.primary.copy(alpha = 0.04f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            val idle = FloatArray(fft.size) { i ->
                val t = (spinAngle / 360f) * Math.PI.toFloat() * 2f
                (0.1f + 0.07f * sin(t + i * 0.4f) + if (playing) 0.04f * sin(t * 2f + i) else 0f)
                    .coerceIn(0f, 1f)
            }
            // Prefer live buffers whenever capture is on (even briefly paused).
            val live = capturing && animate
            val spectrum = if (live) fft else idle
            val waveData = if (live) wave else FloatArray(wave.size) { i ->
                0.12f * sin((spinAngle / 40f) + i * 0.28f)
            }
            val lvl = if (live) level else 0.18f + 0.08f * (pulse - 1f)
            val bassLvl = if (live) bass else lvl * 0.7f
            val energy = (lvl * 0.55f + bassLvl * 0.45f).coerceIn(0f, 1f)

            when (mode) {
                VisualizerMode.SPECTRUM, VisualizerMode.DECORATIVE -> {
                    val barCount = spectrum.size
                    val gap = size.width * 0.008f
                    val barW = (size.width - gap * (barCount + 1)) / barCount
                    val midY = size.height * 0.52f
                    spectrum.forEachIndexed { i, v ->
                        val amp = (v * (0.75f + energy * 0.55f)).coerceIn(0f, 1.2f)
                        val h = size.height * (0.08f + amp * 0.78f)
                        val x = gap + i * (barW + gap)
                        val color = when {
                            i < barCount * 0.2f -> p.secondary
                            i > barCount * 0.75f -> p.accent
                            else -> p.primary
                        }
                        drawRoundRect(
                            color = color.copy(alpha = 0.2f + energy * 0.15f),
                            topLeft = Offset(x - 2f, midY - h * 0.55f),
                            size = Size(barW + 4f, h * 1.1f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(color.copy(alpha = 0.98f), color.copy(alpha = 0.3f))
                            ),
                            topLeft = Offset(x, midY - h * 0.5f),
                            size = Size(barW, h),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                        drawRoundRect(
                            color = color.copy(alpha = 0.2f),
                            topLeft = Offset(x, midY + h * 0.06f),
                            size = Size(barW, h * 0.32f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                    drawLine(
                        color = p.accent.copy(alpha = 0.4f + energy * 0.35f),
                        start = Offset(0f, midY),
                        end = Offset(size.width, midY),
                        strokeWidth = 1.5f + energy * 2f
                    )
                }

                VisualizerMode.BARS -> {
                    val count = (spectrum.size / 2).coerceAtLeast(16)
                    val gap = size.width * 0.01f
                    val barW = (size.width - gap * (count + 1)) / count
                    val floor = size.height * 0.92f
                    for (i in 0 until count) {
                        val v = ((spectrum[i * 2] + spectrum[(i * 2 + 1).coerceAtMost(spectrum.lastIndex)]) * 0.5f)
                            .coerceIn(0f, 1f)
                        val h = size.height * (0.06f + v * 0.82f)
                        val x = gap + i * (barW + gap)
                        val color = Brush.verticalGradient(
                            listOf(p.accent, p.primary, p.secondary.copy(alpha = 0.7f))
                        )
                        drawRoundRect(
                            color = p.primary.copy(alpha = 0.12f + v * 0.2f),
                            topLeft = Offset(x - 1f, floor - h - 4f),
                            size = Size(barW + 2f, h + 8f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        drawRoundRect(
                            brush = color,
                            topLeft = Offset(x, floor - h),
                            size = Size(barW, h),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        // Peak cap
                        drawRect(
                            color = p.soft.copy(alpha = 0.75f),
                            topLeft = Offset(x, floor - h - 3f),
                            size = Size(barW, 3f)
                        )
                    }
                }

                VisualizerMode.WAVEFORM -> {
                    if (waveData.size > 1) {
                        val mid = size.height * 0.5f
                        val amp = size.height * (0.28f + energy * 0.22f)
                        val fill = Path().apply {
                            moveTo(0f, mid)
                            for (i in waveData.indices) {
                                val x = size.width * i / (waveData.size - 1f)
                                val y = mid + waveData[i] * amp
                                lineTo(x, y)
                            }
                            lineTo(size.width, mid)
                            close()
                        }
                        drawPath(
                            fill,
                            brush = Brush.verticalGradient(
                                listOf(
                                    p.primary.copy(alpha = 0.35f + energy * 0.25f),
                                    p.accent.copy(alpha = 0.08f)
                                )
                            )
                        )
                        for (i in 0 until waveData.lastIndex) {
                            val x0 = size.width * i / (waveData.size - 1f)
                            val x1 = size.width * (i + 1) / (waveData.size - 1f)
                            val y0 = mid + waveData[i] * amp
                            val y1 = mid + waveData[i + 1] * amp
                            drawLine(
                                color = p.accent.copy(alpha = 0.95f),
                                start = Offset(x0, y0),
                                end = Offset(x1, y1),
                                strokeWidth = 2.5f + energy * 2f,
                                cap = StrokeCap.Round
                            )
                        }
                        // Ghost secondary wave
                        for (i in 0 until waveData.lastIndex step 2) {
                            val x0 = size.width * i / (waveData.size - 1f)
                            val x1 = size.width * (i + 1).coerceAtMost(waveData.lastIndex) / (waveData.size - 1f)
                            val y0 = mid - waveData[i] * amp * 0.55f
                            val y1 = mid - waveData[(i + 1).coerceAtMost(waveData.lastIndex)] * amp * 0.55f
                            drawLine(
                                color = p.secondary.copy(alpha = 0.45f),
                                start = Offset(x0, y0),
                                end = Offset(x1, y1),
                                strokeWidth = 1.5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                VisualizerMode.CIRCULAR -> {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val base = size.minDimension * (0.16f + bassLvl * 0.08f) * (if (playing) pulse else 1f)
                    rotate(spinAngle * 0.12f + energy * 8f, Offset(cx, cy)) {
                        spectrum.forEachIndexed { i, v ->
                            val ang = (i / spectrum.size.toFloat()) * Math.PI.toFloat() * 2f
                            val outer = base + size.minDimension * 0.34f * (v * (0.7f + energy * 0.5f))
                            val c = when {
                                i % 3 == 0 -> p.primary
                                i % 3 == 1 -> p.secondary
                                else -> p.accent
                            }
                            drawLine(
                                color = c.copy(alpha = 0.85f + v * 0.15f),
                                start = Offset(cx + cos(ang) * base, cy + sin(ang) * base),
                                end = Offset(cx + cos(ang) * outer, cy + sin(ang) * outer),
                                strokeWidth = 2.5f + v * 3f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    drawCircle(
                        color = p.primary.copy(alpha = 0.35f),
                        radius = base * 1.05f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = p.accent.copy(alpha = 0.18f + energy * 0.45f),
                        radius = base * (0.5f + energy * 0.45f),
                        center = Offset(cx, cy)
                    )
                    rotate(spinAngle * (1f + energy), Offset(cx, cy)) {
                        drawLine(
                            brush = Brush.linearGradient(
                                listOf(p.accent.copy(alpha = 0.9f), Color.Transparent)
                            ),
                            start = Offset(cx, cy),
                            end = Offset(cx + base * (1.6f + energy), cy),
                            strokeWidth = 3f
                        )
                    }
                }

                VisualizerMode.RINGS -> {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = size.minDimension * 0.46f
                    val ringCount = 6
                    for (r in 0 until ringCount) {
                        val band = spectrum[(r * spectrum.size / ringCount).coerceIn(0, spectrum.lastIndex)]
                        val radius = maxR * ((r + 1) / ringCount.toFloat()) * (0.55f + band * 0.55f + bassLvl * 0.12f)
                        val color = if (r % 2 == 0) p.primary else p.secondary
                        drawCircle(
                            color = color.copy(alpha = 0.25f + band * 0.55f),
                            radius = radius,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2f + band * 6f + energy * 2f)
                        )
                    }
                    // Spokes driven by FFT
                    val spokes = 24
                    for (i in 0 until spokes) {
                        val ang = (i / spokes.toFloat()) * Math.PI.toFloat() * 2f + spinAngle * 0.01f
                        val v = spectrum[(i * spectrum.size / spokes).coerceIn(0, spectrum.lastIndex)]
                        val inner = maxR * 0.12f
                        val outer = maxR * (0.2f + v * 0.55f)
                        drawLine(
                            color = p.accent.copy(alpha = 0.35f + v * 0.5f),
                            start = Offset(cx + cos(ang) * inner, cy + sin(ang) * inner),
                            end = Offset(cx + cos(ang) * outer, cy + sin(ang) * outer),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                    }
                    drawCircle(
                        color = p.accent.copy(alpha = 0.15f + energy * 0.4f),
                        radius = maxR * (0.1f + energy * 0.18f),
                        center = Offset(cx, cy)
                    )
                }

                VisualizerMode.LEVEL, VisualizerMode.MINIMAL -> {
                    val left = (bassLvl * 0.7f + lvl * 0.3f).coerceIn(0f, 1f)
                    val right = (lvl * 0.75f + energy * 0.25f).coerceIn(0f, 1f)
                    val trackH = size.height * 0.18f
                    val gap = size.height * 0.08f
                    val topL = size.height * 0.28f
                    val topR = topL + trackH + gap
                    fun meter(top: Float, value: Float, labelBias: Float) {
                        drawRoundRect(
                            color = p.dim.copy(alpha = 0.35f),
                            topLeft = Offset(size.width * 0.06f, top),
                            size = Size(size.width * 0.88f, trackH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(p.primary, p.accent, p.secondary)
                            ),
                            topLeft = Offset(size.width * 0.06f, top),
                            size = Size(size.width * 0.88f * value, trackH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        // Peak needle
                        val nx = size.width * 0.06f + size.width * 0.88f * value
                        drawLine(
                            color = p.soft.copy(alpha = 0.9f),
                            start = Offset(nx, top - 4f),
                            end = Offset(nx, top + trackH + 4f),
                            strokeWidth = 2.5f
                        )
                        for (i in 1..10) {
                            val x = size.width * 0.06f + size.width * 0.88f * (i / 10f)
                            drawLine(
                                color = p.soft.copy(alpha = 0.2f + labelBias),
                                start = Offset(x, top - 2f),
                                end = Offset(x, top + trackH + 2f),
                                strokeWidth = 1f
                            )
                        }
                    }
                    meter(topL, left, 0.05f)
                    meter(topR, right, 0.1f)
                }
            }
        }
        Image(
            painter = painterResource(R.drawable.apollo_crosshair_c),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(16.dp),
            colorFilter = ColorFilter.tint(p.primary.copy(alpha = 0.7f), BlendMode.SrcAtop)
        )
    }
}
