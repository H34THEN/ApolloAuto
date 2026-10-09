package com.apolloauto.player.ui

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            when (mode) {
                VisualizerMode.SPECTRUM, VisualizerMode.DECORATIVE -> {
                    val data = if (capturing && animate) fft else FloatArray(fft.size) { 0.08f }
                    val barW = size.width / (data.size * 1.4f)
                    data.forEachIndexed { i, v ->
                        val h = size.height * (0.08f + v * 0.9f)
                        val x = i * (barW * 1.4f)
                        drawRect(
                            color = if (i % 2 == 0) p.primary else p.secondary,
                            topLeft = Offset(x, size.height - h),
                            size = androidx.compose.ui.geometry.Size(barW, h),
                            alpha = 0.85f
                        )
                    }
                }
                VisualizerMode.WAVEFORM -> {
                    val data = if (capturing && animate) wave else FloatArray(wave.size)
                    if (data.size > 1) {
                        val pathPts = data.mapIndexed { i, v ->
                            Offset(
                                x = size.width * i / (data.size - 1f),
                                y = size.height * 0.5f + v * size.height * 0.4f
                            )
                        }
                        for (i in 0 until pathPts.lastIndex) {
                            drawLine(
                                color = p.accent,
                                start = pathPts[i],
                                end = pathPts[i + 1],
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
                VisualizerMode.CIRCULAR -> {
                    val data = if (capturing && animate) fft else FloatArray(fft.size) { 0.1f }
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val base = size.minDimension * 0.22f
                    data.forEachIndexed { i, v ->
                        val ang = (i / data.size.toFloat()) * Math.PI.toFloat() * 2f
                        val outer = base + size.minDimension * 0.28f * v
                        drawLine(
                            color = if (i % 2 == 0) p.primary else p.secondary,
                            start = Offset(cx + cos(ang) * base, cy + sin(ang) * base),
                            end = Offset(cx + cos(ang) * outer, cy + sin(ang) * outer),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }
                    drawCircle(
                        color = p.primary.copy(alpha = 0.5f),
                        radius = base,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f)
                    )
                }
                VisualizerMode.LEVEL, VisualizerMode.MINIMAL -> {
                    val v = if (capturing && animate) level else 0.12f
                    drawRect(
                        color = p.secondary.copy(alpha = 0.8f),
                        size = androidx.compose.ui.geometry.Size(size.width * v, size.height * 0.35f),
                        topLeft = Offset(0f, size.height * 0.32f)
                    )
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
