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
import kotlin.math.sin
fun DrawScope.drawHyperdriveScene(
    features: AudioFeatures,
    palette: HudPalette,
    phase: Float,
    pulse: Float,
    playing: Boolean
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val speed = (0.35f + features.bass * 0.55f + features.onset * 0.25f) * if (playing) 1f else 0.45f
    val starCount = 120
    val depth = size.minDimension * 0.55f
    drawRect(palette.voidBlack)
    for (i in 0 until starCount) {
        val seed = i * 97.13f
        val angle = (seed * 0.017f + phase * 0.002f) % (PI.toFloat() * 2f)
        var z = ((seed * 0.31f + phase * speed * 0.08f) % 1f)
        z = z * z
        val spread = depth * (0.15f + z * 0.85f)
        val x = cx + cos(angle) * spread * (0.35f + features.mid * 0.15f)
        val y = cy + sin(angle) * spread * (0.35f + features.mid * 0.15f)
        val streak = (4f + z * 28f + features.onset * 12f) * pulse.coerceIn(0.9f, 1.1f)
        val color = when {
            i % 5 == 0 -> palette.accent
            i % 3 == 0 -> palette.secondary
            else -> palette.primary
        }
        val alpha = (0.12f + z * 0.65f + features.rms * 0.15f).coerceIn(0.08f, 0.92f)
        drawLine(
            color = color.copy(alpha = alpha),
            start = Offset(x, y),
            end = Offset(
                cx + (x - cx) * (1f - streak / spread.coerceAtLeast(1f)),
                cy + (y - cy) * (1f - streak / spread.coerceAtLeast(1f))
            ),
            strokeWidth = 1f + z * 2.5f,
            cap = StrokeCap.Round
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                palette.primary.copy(alpha = 0.35f + features.bass * 0.25f),
                palette.voidBlack.copy(alpha = 0f)
            ),
            center = Offset(cx, cy),
            radius = depth * (0.22f + features.bass * 0.12f)
        ),
        radius = depth * 0.5f,
        center = Offset(cx, cy)
    )
}

fun DrawScope.drawNebulaScene(
    features: AudioFeatures,
    palette: HudPalette,
    phase: Float,
    pulse: Float
) {
    drawRect(
        Brush.radialGradient(
            listOf(palette.panel.copy(alpha = 0.95f), palette.voidBlack),
            center = Offset(size.width * 0.4f, size.height * 0.35f),
            radius = size.maxDimension
        )
    )
    val blobs = 14
    for (b in 0 until blobs) {
        val t = phase * 0.0015f + b * 0.7f
        val bx = size.width * (0.2f + 0.6f * ((sin(t * 0.9f + b) + 1f) * 0.5f))
        val by = size.height * (0.15f + 0.7f * ((cos(t * 0.7f + b * 1.3f) + 1f) * 0.5f))
        val band = when (b % 3) {
            0 -> features.bass
            1 -> features.mid
            else -> features.high
        }
        val radius = size.minDimension * (0.08f + band * 0.14f + pulse * 0.02f)
        val color = when (b % 3) {
            0 -> palette.primary
            1 -> palette.secondary
            else -> palette.accent
        }
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    color.copy(alpha = 0.08f + band * 0.22f),
                    color.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                center = Offset(bx, by),
                radius = radius
            ),
            radius = radius,
            center = Offset(bx, by)
        )
    }
    // Soft dust
    for (i in 0 until 40) {
        val ang = i * 0.42f + phase * 0.003f
        val r = size.minDimension * (0.08f + (i % 7) * 0.04f)
        val px = size.width / 2f + cos(ang) * r * (1.2f + features.mid * 0.3f)
        val py = size.height / 2f + sin(ang) * r * (1.2f + features.mid * 0.3f)
        drawCircle(
            color = palette.soft.copy(alpha = 0.04f + features.rms * 0.08f),
            radius = 1.5f + features.high * 2f,
            center = Offset(px, py)
        )
    }
}

fun DrawScope.drawRaveCoreScene(
    features: AudioFeatures,
    palette: HudPalette,
    phase: Float,
    fft: FloatArray
) {
    drawRect(palette.voidBlack)
    val floorY = size.height * 0.88f
    drawLine(
        color = palette.primary.copy(alpha = 0.35f),
        start = Offset(0f, floorY),
        end = Offset(size.width, floorY),
        strokeWidth = 2f
    )
    val pillars = 9
    val gap = size.width / (pillars + 1)
    for (p in 0 until pillars) {
        val x = gap * (p + 1)
        val band = fft[(p * fft.size / pillars).coerceIn(0, fft.lastIndex)]
        val h = size.height * (0.15f + band * 0.55f + features.bass * 0.12f)
        val w = gap * 0.35f
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    palette.accent.copy(alpha = 0.85f),
                    palette.primary.copy(alpha = 0.45f + band * 0.35f),
                    palette.secondary.copy(alpha = 0.25f)
                ),
                startY = floorY - h,
                endY = floorY
            ),
            topLeft = Offset(x - w / 2f, floorY - h),
            size = Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )
        // Industrial cross-brace
        val braceY = floorY - h * (0.35f + (sin(phase * 0.004f + p) + 1f) * 0.1f)
        drawLine(
            color = palette.soft.copy(alpha = 0.25f + band * 0.35f),
            start = Offset(x - w, braceY),
            end = Offset(x + w, braceY),
            strokeWidth = 2f
        )
    }
    val gridLines = 8
    for (g in 0..gridLines) {
        val gy = floorY - size.height * 0.75f * (g / gridLines.toFloat())
        drawLine(
            color = palette.primary.copy(alpha = 0.06f + features.mid * 0.05f),
            start = Offset(0f, gy),
            end = Offset(size.width, gy),
            strokeWidth = 1f
        )
    }
    drawCircle(
        color = palette.accent.copy(alpha = 0.12f + features.onset * 0.35f),
        radius = size.minDimension * (0.06f + features.onset * 0.08f),
        center = Offset(size.width / 2f, floorY - size.height * 0.35f)
    )
}

fun DrawScope.drawSignalOceanScene(
    features: AudioFeatures,
    palette: HudPalette,
    phase: Float,
    wave: FloatArray
) {
    drawRect(
        Brush.verticalGradient(
            listOf(palette.voidBlack, palette.panel.copy(alpha = 0.9f), palette.voidBlack)
        )
    )
    val rows = 14
    val cols = 24
    val cellW = size.width / cols
    val baseY = size.height * 0.55f
    for (row in 0 until rows) {
        val path = Path()
        var started = false
        for (col in 0..cols) {
            val x = col * cellW
            val waveIdx = (col * wave.size / cols.toFloat()).toInt().coerceIn(0, wave.lastIndex)
            val wv = wave[waveIdx]
            val swell = sin(phase * 0.003f + col * 0.35f + row * 0.2f) * 0.5f + 0.5f
            val y = baseY + row * (size.height * 0.028f) +
                (wv * 22f + swell * 18f + features.bass * 28f) * (1f - row / rows.toFloat())
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
        }
        val alpha = (0.15f + (1f - row / rows.toFloat()) * 0.45f + features.mid * 0.15f).coerceIn(0.1f, 0.75f)
        val color = if (row % 2 == 0) palette.primary else palette.secondary
        drawPath(
            path,
            color = color.copy(alpha = alpha),
            style = Stroke(width = 1.5f + features.rms * 2f, cap = StrokeCap.Round)
        )
    }
    // Horizon glow
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                palette.accent.copy(alpha = 0.25f + features.high * 0.35f),
                Color.Transparent
            )
        ),
        start = Offset(0f, baseY - 8f),
        end = Offset(size.width, baseY - 8f),
        strokeWidth = 3f + features.rms * 4f
    )
}

fun DrawScope.drawPlasmaHeartScene(
    features: AudioFeatures,
    palette: HudPalette,
    phase: Float,
    pulse: Float
) {
    drawRect(palette.voidBlack)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val layers = 6
    for (layer in 0 until layers) {
        val t = layer / layers.toFloat()
        val radius = size.minDimension * (0.08f + t * 0.38f + features.bass * 0.08f) * pulse
        val wobble = sin(phase * 0.004f + layer * 1.1f) * (8f + features.mid * 14f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    palette.accent.copy(alpha = 0.04f + (1f - t) * 0.2f),
                    palette.primary.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                center = Offset(cx, cy + wobble * 0.15f),
                radius = radius
            ),
            radius = radius,
            center = Offset(cx, cy + wobble * 0.15f)
        )
    }
    val coreR = size.minDimension * (0.06f + features.rms * 0.1f + features.onset * 0.05f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                palette.soft.copy(alpha = 0.9f),
                palette.accent.copy(alpha = 0.55f),
                palette.primary.copy(alpha = 0.05f)
            ),
            center = Offset(cx, cy),
            radius = coreR * 2f
        ),
        radius = coreR * 2f,
        center = Offset(cx, cy)
    )
    val filaments = 16
    for (f in 0 until filaments) {
        val ang = f / filaments.toFloat() * PI.toFloat() * 2f + phase * 0.002f
        val len = coreR * (2.5f + features.high * 2f + sin(phase * 0.005f + f) * 0.4f)
        drawLine(
            color = palette.secondary.copy(alpha = 0.35f + features.onset * 0.4f),
            start = Offset(cx, cy),
            end = Offset(cx + cos(ang) * len, cy + sin(ang) * len),
            strokeWidth = 2f + features.mid * 3f,
            cap = StrokeCap.Round
        )
    }
}

fun DrawScope.drawFractureScene(
    features: AudioFeatures,
    palette: HudPalette,
    phase: Float,
    fft: FloatArray,
    spin: Float
) {
    drawRect(palette.voidBlack.copy(alpha = 0.98f))
    val cx = size.width / 2f
    val cy = size.height / 2f
    val segments = 8
    val maxR = size.minDimension * 0.46f
    rotate(spin * 0.15f + features.mid * 6f, Offset(cx, cy)) {
        for (s in 0 until segments) {
            val ang0 = s / segments.toFloat() * PI.toFloat() * 2f
            val ang1 = (s + 1) / segments.toFloat() * PI.toFloat() * 2f
            val band = fft[(s * fft.size / segments).coerceIn(0, fft.lastIndex)]
            val inner = maxR * (0.08f + features.bass * 0.06f)
            val outer = maxR * (0.35f + band * 0.55f + features.onset * 0.12f)
            val path = Path().apply {
                moveTo(
                    cx + cos(ang0) * inner,
                    cy + sin(ang0) * inner
                )
                lineTo(
                    cx + cos(ang0) * outer,
                    cy + sin(ang0) * outer
                )
                lineTo(
                    cx + cos(ang1) * outer,
                    cy + sin(ang1) * outer
                )
                lineTo(
                    cx + cos(ang1) * inner,
                    cy + sin(ang1) * inner
                )
                close()
            }
            val color = when (s % 3) {
                0 -> palette.primary
                1 -> palette.secondary
                else -> palette.accent
            }
            drawPath(
                path,
                brush = Brush.linearGradient(
                    listOf(
                        color.copy(alpha = 0.55f + band * 0.35f),
                        color.copy(alpha = 0.08f)
                    ),
                    start = Offset(cx, cy),
                    end = Offset(cx + cos((ang0 + ang1) / 2f) * outer, cy + sin((ang0 + ang1) / 2f) * outer)
                )
            )
            drawPath(
                path,
                color = palette.soft.copy(alpha = 0.12f + band * 0.2f),
                style = Stroke(width = 1.5f)
            )
        }
    }
    // Central kaleidoscope mirror hint
    val shards = 12
    for (i in 0 until shards) {
        val a = i / shards.toFloat() * PI.toFloat() * 2f + phase * 0.0035f
        val r = maxR * (0.05f + abs(sin(a * 2f + phase * 0.002f)) * (0.12f + features.high * 0.15f))
        drawLine(
            color = palette.accent.copy(alpha = 0.2f + features.rms * 0.3f),
            start = Offset(cx, cy),
            end = Offset(cx + cos(a) * r, cy + sin(a) * r),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round
        )
    }
}
