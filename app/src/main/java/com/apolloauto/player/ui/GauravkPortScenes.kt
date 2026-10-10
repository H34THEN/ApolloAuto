package com.apolloauto.player.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.apolloauto.player.media.immersiveViewportDim
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Algorithm ports of [gauravk95/audio-visualizer-android](https://github.com/gauravk95/audio-visualizer-android)
 * (Apache License 2.0 — Copyright 2018 Gaurav Kumar).
 *
 * Adapted from Canvas View subclasses to Compose DrawScope using Apollo FFT floats
 * (0..1) in place of the library's raw Visualizer byte array.
 */

private fun ampAt(fft: FloatArray, index: Int, points: Int): Float {
    if (fft.isEmpty() || points <= 0) return 0f
    val x = ceil((index + 1f) * (fft.size / points.toFloat())).toInt()
        .coerceIn(0, fft.lastIndex)
    return fft[x].coerceIn(0f, 1.2f)
}

/** BarVisualizer — bottom-anchored bars with batch lerp. */
fun DrawScope.drawAvBarScene(
    palette: HudPalette,
    fft: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.25f
    val n = (120 * 0.55f).toInt().coerceIn(24, 96)
    val barW = size.width / n
    val h = size.height
    for (i in 0 until n) {
        val amp = ampAt(fft, i, n) * drive
        val barY = h - amp * h * 0.92f
        val barX = i * barW + barW / 2f
        drawLine(
            color = palette.primary.copy(alpha = (0.45f + amp * 0.5f).coerceIn(0.2f, 1f)),
            start = Offset(barX, h),
            end = Offset(barX, barY),
            strokeWidth = (barW * 0.55f).coerceAtLeast(2f),
            cap = StrokeCap.Butt
        )
    }
}

/** BlastVisualizer — spiky radial path. */
fun DrawScope.drawAvBlastScene(
    palette: HudPalette,
    fft: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.2f
    val n = (400 * 0.35f).toInt().coerceIn(48, 180)
    val span = immersiveViewportDim(size.width, size.height)
    val radius = span * 0.32f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val path = Path()
    for (i in 0 until n) {
        val amp = ampAt(fft, i, n) * drive
        val t = amp * span * 0.28f
        val angle = i * (360f / n)
        val rad = Math.toRadians(angle.toDouble())
        val x = cx + (radius + t) * cos(rad).toFloat()
        val y = cy + (radius + t) * sin(rad).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, palette.accent.copy(alpha = 0.85f), style = Stroke(2.2f))
    drawPath(path, palette.primary.copy(alpha = 0.22f), style = Fill)
}

/** BlobVisualizer — radial blob (simplified cubic-free closed path). */
fun DrawScope.drawAvBlobScene(
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.25f
    val n = 36
    val span = immersiveViewportDim(size.width, size.height)
    val radius = span * 0.32f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val path = Path()
    for (i in 0 until n) {
        val amp = ampAt(fft, i, n) * drive
        val t = amp * span * 0.28f
        val angle = i * (360f / n) + phase * 0.02f
        val rad = Math.toRadians(angle.toDouble())
        val x = cx + (radius + t) * cos(rad).toFloat()
        val y = cy + (radius + t) * sin(rad).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, palette.secondary.copy(alpha = 0.55f), style = Fill)
    drawPath(path, palette.primary.copy(alpha = 0.9f), style = Stroke(3f))
}

/** WaveVisualizer — filled cubic wave (library algorithm). */
fun DrawScope.drawAvWaveFilledScene(
    palette: HudPalette,
    fft: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.25f
    val n = 56
    val pts = FloatArray(n + 1)
    for (i in 0..n) {
        pts[i] = ampAt(fft, i.coerceAtMost(n - 1), n) * drive
    }
    val path = Path()
    val bottom = size.height
    path.moveTo(0f, bottom)
    for (i in 0..n) {
        val x = size.width * (i / n.toFloat())
        val y = bottom - pts[i] * size.height * 0.78f
        if (i == 0) path.lineTo(x, y)
        else {
            val px = size.width * ((i - 1) / n.toFloat())
            val py = bottom - pts[i - 1] * size.height * 0.78f
            val mx = (px + x) / 2f
            path.cubicTo(mx, py, mx, y, x, y)
        }
    }
    path.lineTo(size.width, bottom)
    path.close()
    drawPath(path, palette.primary.copy(alpha = 0.55f), style = Fill)
    drawPath(path, palette.accent.copy(alpha = 0.9f), style = Stroke(2.5f))
}

/** CircleLineVisualizer — radial bars on a ring. */
fun DrawScope.drawAvCircleLineScene(
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.25f
    val points = 72
    val span = immersiveViewportDim(size.width, size.height)
    val radius = span * 0.28f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val pointR = max(2f, (2f * radius * sin(PI / points / 3)).toFloat())
    rotate(degrees = phase * 0.015f, pivot = Offset(cx, cy)) {
        for (i in 0 until points) {
            val deg = i * (360f / points)
            val amp = ampAt(fft, i, points) * drive
            val barLen = amp * span * 0.22f
            val rad = Math.toRadians(deg.toDouble())
            val ox = cx + cos(rad).toFloat() * radius
            val oy = cy - sin(rad).toFloat() * radius
            drawCircle(palette.primary.copy(alpha = 0.7f), pointR, Offset(ox, oy))
            if (barLen > 1f) {
                val ex = cx + cos(rad).toFloat() * (radius + barLen)
                val ey = cy - sin(rad).toFloat() * (radius + barLen)
                drawLine(
                    color = palette.accent.copy(alpha = (0.5f + amp * 0.5f).coerceIn(0.3f, 1f)),
                    start = Offset(ox, oy),
                    end = Offset(ex, ey),
                    strokeWidth = pointR * 1.6f,
                    cap = StrokeCap.Round
                )
                drawCircle(palette.secondary, pointR, Offset(ex, ey))
            }
        }
    }
}

/** HiFiVisualizer — dual inward/outward circular waves. */
fun DrawScope.drawAvHifiScene(
    palette: HudPalette,
    fft: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.25f
    val points = 64
    val span = immersiveViewportDim(size.width, size.height)
    val radius = span * 0.38f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val heights = IntArray(points) { i ->
        (-(ampAt(fft, i, points) * drive * radius * 0.85f)).toInt()
    }
    val ctrl = radius / cos(PI / points).toFloat()
    val out = Path()
    val inn = Path()
    fun pt(deg: Float, h: Float): Offset {
        val r = Math.toRadians(deg.toDouble())
        return Offset(
            cx + cos(r).toFloat() * h,
            cy - sin(r).toFloat() * h
        )
    }
    val lastH = heights[points - 1].toFloat()
    val startOut = pt(360f - 360f / points, radius + lastH)
    val startIn = pt(360f - 360f / points, radius - lastH)
    out.moveTo(startOut.x, startOut.y)
    inn.moveTo(startIn.x, startIn.y)
    var iDeg = 0
    while (iDeg < 360) {
        val idx = (iDeg * points / 360).coerceIn(0, points - 1)
        val lastIdx = if (iDeg == 0) points - 1 else (idx - 1).coerceAtLeast(0)
        val h = heights[idx].toFloat()
        val hl = heights[lastIdx].toFloat()
        val cxP = pt(iDeg.toFloat(), radius + h)
        val ax = pt(iDeg - 180f / points, ctrl + hl)
        val bx = pt(iDeg - 180f / points, ctrl + h)
        out.cubicTo(ax.x, ax.y, bx.x, bx.y, cxP.x, cxP.y)
        val cx1 = pt(iDeg.toFloat(), radius - h)
        val ax1 = pt(iDeg - 180f / points, ctrl - hl)
        val bx1 = pt(iDeg - 180f / points, ctrl - h)
        inn.cubicTo(ax1.x, ax1.y, bx1.x, bx1.y, cx1.x, cx1.y)
        drawLine(palette.primary.copy(alpha = 0.35f), cxP, cx1, strokeWidth = 1.2f)
        iDeg += 360 / points
    }
    drawPath(out, palette.accent.copy(alpha = 0.9f), style = Stroke(1.8f))
    drawPath(inn, palette.secondary.copy(alpha = 0.9f), style = Stroke(1.8f))
}
