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
import com.apolloauto.player.media.immersiveViewportDim
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val TronCyan = Color(0xFF00F0FF)
private val TronOrange = Color(0xFFFF6A00)
private val TronBlue = Color(0xFF1B4DFF)
private val TronWhite = Color(0xFFE8FFFF)

/**
 * Tron Legacy–styled audio scenes (original Apollo geometry; cyan/orange circuit aesthetic).
 */

/** Perspective light-cycle grid rushing under a neon skyline. */
fun DrawScope.drawTronGridScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color(0xFF02040A))
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.3f
    val horizon = size.height * 0.42f
    val cx = size.width * 0.5f
    val span = immersiveViewportDim(size.width, size.height)
    val scroll = phase * (0.04f + d.bass * 0.12f + d.rms * 0.06f) * drive

    drawRect(
        Brush.verticalGradient(
            listOf(Color(0xFF000818), Color(0xFF001030), Color(0xFF02040A)),
            startY = 0f,
            endY = horizon
        ),
        size = androidx.compose.ui.geometry.Size(size.width, horizon)
    )

    // Sky recognition rings
    for (i in 0 until 5) {
        val t = i / 4f
        val r = span * (0.08f + t * 0.22f) * (0.9f + d.onset * 0.15f)
        drawCircle(
            TronCyan.copy(alpha = (0.08f + (1f - t) * 0.12f) * drive),
            radius = r,
            center = Offset(cx, horizon * 0.55f),
            style = Stroke(1.2f + d.treble * 1.5f)
        )
    }

    // Floor grid
    val rows = 18
    for (i in 0 until rows) {
        val t = ((i / rows.toFloat()) + scroll * 0.15f) % 1f
        val y = horizon + t * t * (size.height - horizon)
        val a = ((1f - t) * (0.25f + d.rms * 0.35f) + d.onset * 0.1f).coerceIn(0.05f, 0.75f) * drive
        drawLine(
            TronCyan.copy(alpha = a),
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth = 1.2f + d.bass * 1.5f
        )
    }
    val vanish = Offset(cx, horizon)
    for (col in -12..12) {
        val edge = cx + col * (size.width / 11f) * (1f + d.lowMid * 0.15f)
        drawLine(
            TronCyan.copy(alpha = (0.12f + d.treble * 0.2f) * drive),
            vanish,
            Offset(edge, size.height),
            strokeWidth = 1.1f
        )
    }

    // Light-cycle ribbons from FFT
    val ribbons = 6
    for (r in 0 until ribbons) {
        val path = Path()
        val n = 48
        val yBase = horizon + size.height * (0.15f + r * 0.08f)
        val amp = size.height * (0.04f + d.rms * 0.08f) * drive
        val orange = r % 2 == 1
        for (i in 0 until n) {
            val t = i / (n - 1f)
            val bin = fft.getOrElse((t * fft.lastIndex).toInt()) { d.rms }
            val x = size.width * t
            val y = yBase + sin(t * 8f + phase * 0.06f + r) * amp * (0.5f + bin) -
                bin * amp * 0.8f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            (if (orange) TronOrange else TronCyan)
                .copy(alpha = (0.45f + d.onset * 0.35f) * drive),
            style = Stroke((2.5f + d.bass * 3f).coerceIn(2f, 8f), cap = StrokeCap.Round)
        )
    }

    // Horizon glow bar
    drawRect(
        Brush.horizontalGradient(
            listOf(Color.Transparent, TronCyan.copy(alpha = 0.45f * drive), Color.Transparent)
        ),
        topLeft = Offset(0f, horizon - 2f),
        size = androidx.compose.ui.geometry.Size(size.width, 4f + d.onset * 10f)
    )
}

/** Identity disc / recognizer ring with spectrum teeth. */
fun DrawScope.drawTronDiscScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color(0xFF010208))
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.28f
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val span = immersiveViewportDim(size.width, size.height)
    val rot = phase * (0.8f + d.bass * 2.5f + d.onset * 3f) * drive

    rotate(degrees = rot, pivot = Offset(cx, cy)) {
        val teeth = fft.size.coerceIn(32, 96)
        for (i in 0 until teeth) {
            val a = i / teeth.toFloat() * PI.toFloat() * 2f
            val bin = fft.getOrElse(i % fft.size) { d.rms }.coerceIn(0f, 1.2f)
            val inner = span * (0.16f + d.sub * 0.03f)
            val outer = inner + span * (0.08f + bin * 0.28f + d.bass * 0.06f) * drive
            val c = cos(a)
            val s = sin(a)
            val col = if (i % 3 == 0) TronOrange else TronCyan
            drawLine(
                col.copy(alpha = (0.35f + bin * 0.55f) * drive),
                Offset(cx + c * inner, cy + s * inner),
                Offset(cx + c * outer, cy + s * outer),
                strokeWidth = (2f + bin * 3f).coerceIn(1.5f, 6f),
                cap = StrokeCap.Round
            )
        }
        for (ring in 1..4) {
            drawCircle(
                TronCyan.copy(alpha = (0.2f + d.rms * 0.25f) * drive / ring),
                radius = span * (0.14f + ring * 0.07f) * (0.95f + d.onset * 0.08f),
                center = Offset(cx, cy),
                style = Stroke(1.5f + d.treble)
            )
        }
    }
    // Core identity glyph
    drawCircle(
        Brush.radialGradient(
            listOf(
                TronWhite.copy(alpha = (0.35f + d.onset * 0.4f) * drive),
                TronCyan.copy(alpha = 0.25f * drive),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = span * (0.12f + d.bass * 0.06f)
        ),
        radius = span * (0.12f + d.bass * 0.06f),
        center = Offset(cx, cy)
    )
    drawCircle(
        TronOrange.copy(alpha = (0.5f + d.onset * 0.4f) * drive),
        radius = span * 0.04f,
        center = Offset(cx, cy),
        style = Stroke(2.5f + d.bass * 2f)
    )
}

/** Light-ribbon tunnel — Tron highway into the Grid. */
fun DrawScope.drawTronRibbonScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color.Black)
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.3f
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val span = immersiveViewportDim(size.width, size.height)
    val speed = 0.02f + d.bass * 0.08f + d.onset * 0.05f

    for (i in 0 until 20) {
        val t = ((i / 20f) + phase * speed) % 1f
        val depth = t * t
        val bin = fft.getOrElse((t * fft.lastIndex).toInt()) { d.rms }
        val r = 8f + depth * span * 0.55f * (1f + bin * 0.25f * drive)
        val path = Path()
        val steps = 64
        for (s in 0..steps) {
            val a = s / steps.toFloat() * PI.toFloat() * 2f + phase * 0.03f * (1f - t)
            val wob = 1f + sin(a * 4f + phase * 0.05f) * 0.08f * drive + bin * 0.12f
            val x = cx + cos(a) * r * wob
            val y = cy + sin(a) * r * wob * (0.72f + d.lowMid * 0.1f)
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        val col = if (i % 2 == 0) TronCyan else TronOrange
        drawPath(
            path,
            col.copy(alpha = ((1f - depth) * (0.35f + d.rms * 0.4f)).coerceIn(0.06f, 0.8f) * drive),
            style = Stroke((1.5f + (1f - depth) * 4f + bin * 2f).coerceIn(1f, 8f))
        )
    }

    // Corner HUD brackets
    val m = span * 0.06f
    val bracket = TronCyan.copy(alpha = 0.45f * drive)
    fun corner(x: Float, y: Float, sx: Float, sy: Float) {
        drawLine(bracket, Offset(x, y), Offset(x + sx * m, y), strokeWidth = 2f)
        drawLine(bracket, Offset(x, y), Offset(x, y + sy * m), strokeWidth = 2f)
    }
    corner(m, m, 1f, 1f)
    corner(size.width - m, m, -1f, 1f)
    corner(m, size.height - m, 1f, -1f)
    corner(size.width - m, size.height - m, -1f, -1f)

    if (d.onset > 0.4f) {
        drawCircle(
            TronWhite.copy(alpha = (d.onset * 0.15f * drive).coerceAtMost(0.3f)),
            radius = span * (0.1f + d.bass * 0.1f),
            center = Offset(cx, cy),
            style = Stroke(2f + d.onset * 4f)
        )
    }
}
