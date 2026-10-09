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
    val capturing by viz.capturing.collectAsState()
    val mode by viz.mode.collectAsState()
    val animate = motion != MotionProfile.STATIC && playing

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
            // HUD grid
            val gridStep = size.width / 12f
            for (i in 0..12) {
                val x = i * gridStep
                drawLine(
                    color = p.primary.copy(alpha = 0.06f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
            }
            for (i in 0..6) {
                val y = i * (size.height / 6f)
                drawLine(
                    color = p.primary.copy(alpha = 0.05f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            val idle = FloatArray(fft.size) { i ->
                val t = (spinAngle / 360f) * Math.PI.toFloat() * 2f
                (0.12f + 0.08f * sin(t + i * 0.45f)).coerceIn(0f, 1f)
            }
            val spectrum = if (capturing && animate) fft else idle
            val waveData = if (capturing && animate) wave else FloatArray(wave.size) { i ->
                0.15f * sin((spinAngle / 40f) + i * 0.35f)
            }
            val lvl = if (capturing && animate) level else 0.2f + 0.1f * (pulse - 1f)

            when (mode) {
                VisualizerMode.SPECTRUM, VisualizerMode.DECORATIVE -> {
                    val barCount = spectrum.size
                    val gap = size.width * 0.012f
                    val barW = (size.width - gap * (barCount + 1)) / barCount
                    val midY = size.height * 0.55f
                    spectrum.forEachIndexed { i, v ->
                        val h = size.height * (0.1f + v * 0.75f) * (if (animate) 1f else 0.7f)
                        val x = gap + i * (barW + gap)
                        val color = if (i % 2 == 0) p.primary else p.secondary
                        // Glow
                        drawRoundRect(
                            color = color.copy(alpha = 0.18f),
                            topLeft = Offset(x - 2f, midY - h * 0.55f),
                            size = Size(barW + 4f, h * 1.1f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.35f))
                            ),
                            topLeft = Offset(x, midY - h * 0.5f),
                            size = Size(barW, h),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                        // Mirror reflection
                        drawRoundRect(
                            color = color.copy(alpha = 0.22f),
                            topLeft = Offset(x, midY + h * 0.08f),
                            size = Size(barW, h * 0.35f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                    drawLine(
                        color = p.accent.copy(alpha = 0.45f),
                        start = Offset(0f, midY),
                        end = Offset(size.width, midY),
                        strokeWidth = 1.5f
                    )
                }

                VisualizerMode.WAVEFORM -> {
                    if (waveData.size > 1) {
                        val mid = size.height * 0.5f
                        // Soft fill under waveform
                        for (i in 0 until waveData.lastIndex) {
                            val x0 = size.width * i / (waveData.size - 1f)
                            val x1 = size.width * (i + 1) / (waveData.size - 1f)
                            val y0 = mid + waveData[i] * size.height * 0.42f
                            val y1 = mid + waveData[i + 1] * size.height * 0.42f
                            drawLine(
                                color = p.accent.copy(alpha = 0.25f),
                                start = Offset(x0, mid),
                                end = Offset(x0, y0),
                                strokeWidth = 2f
                            )
                            drawLine(
                                color = p.accent,
                                start = Offset(x0, y0),
                                end = Offset(x1, y1),
                                strokeWidth = 3.5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                VisualizerMode.CIRCULAR -> {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val base = size.minDimension * 0.2f * pulse
                    rotate(spinAngle * 0.15f, Offset(cx, cy)) {
                        spectrum.forEachIndexed { i, v ->
                            val ang = (i / spectrum.size.toFloat()) * Math.PI.toFloat() * 2f
                            val outer = base + size.minDimension * 0.32f * v
                            val c = if (i % 2 == 0) p.primary else p.secondary
                            drawLine(
                                color = c.copy(alpha = 0.9f),
                                start = Offset(cx + cos(ang) * base, cy + sin(ang) * base),
                                end = Offset(cx + cos(ang) * outer, cy + sin(ang) * outer),
                                strokeWidth = 3.5f,
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
                        color = p.accent.copy(alpha = 0.2f + lvl * 0.4f),
                        radius = base * (0.55f + lvl * 0.35f),
                        center = Offset(cx, cy)
                    )
                    // Sweep radar arm
                    rotate(spinAngle, Offset(cx, cy)) {
                        drawLine(
                            brush = Brush.linearGradient(
                                listOf(p.accent.copy(alpha = 0.8f), Color.Transparent)
                            ),
                            start = Offset(cx, cy),
                            end = Offset(cx + base * 1.8f, cy),
                            strokeWidth = 3f
                        )
                    }
                }

                VisualizerMode.LEVEL, VisualizerMode.MINIMAL -> {
                    val v = lvl.coerceIn(0f, 1f)
                    val trackH = size.height * 0.28f
                    val top = size.height * 0.36f
                    drawRoundRect(
                        color = p.dim.copy(alpha = 0.35f),
                        topLeft = Offset(size.width * 0.05f, top),
                        size = Size(size.width * 0.9f, trackH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(p.primary, p.accent, p.secondary)
                        ),
                        topLeft = Offset(size.width * 0.05f, top),
                        size = Size(size.width * 0.9f * v, trackH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    // Peak ticks
                    for (i in 1..8) {
                        val x = size.width * 0.05f + size.width * 0.9f * (i / 8f)
                        drawLine(
                            color = p.soft.copy(alpha = 0.25f),
                            start = Offset(x, top - 4f),
                            end = Offset(x, top + trackH + 4f),
                            strokeWidth = 1f
                        )
                    }
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
