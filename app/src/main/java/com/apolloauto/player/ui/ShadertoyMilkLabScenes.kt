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
import com.apolloauto.player.media.safeHue
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * V5.3 Shadertoy + MILK//LAB scenes.
 *
 * Shadertoy adaptations are original Compose Canvas interpretations of inspected
 * visual identities (not GPU ports of the GLSL). See docs/APOLLO_V5_3_SHADER_*.md.
 *
 * MILK//LAB scenes are Apollo originals in the XorDev/MilkDrop2077 aesthetic vein.
 * The commercial XorDev pack cannot be redistributed; only publicly documented
 * names (e.g. "Black hole") are cited as inspiration.
 */

// ─── ST01 GrafEq (davidar / Xs3GWj) — CRT implicit equation graphs ───────────

fun DrawScope.drawSt01GrafEqScene(
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
    val pattern = ((phase * 0.008f + d.onset * 2f + d.bass * 1.5f) % 5f).toInt().coerceIn(0, 4)
    val cols = 48
    val rows = 28
    val cellW = size.width / cols
    val cellH = size.height / rows
    val zoom = 0.7f + d.lowMid * 0.35f * drive + sin(phase * 0.01f) * 0.05f
    for (yi in 0 until rows) {
        for (xi in 0 until cols) {
            val nx = ((xi + 0.5f) / cols - 0.5f) * 50f * zoom
            val ny = ((yi + 0.5f) / rows - 0.5f) * 50f * zoom *
                (size.height / size.width.coerceAtLeast(1f))
            val lit = grafEqCell(nx, ny, pattern, phase * 0.02f + d.treble * 2f)
            if (!lit) continue
            val hue = safeHue(
                when (pattern) {
                    0 -> 0f + d.bass * 40f
                    1 -> 280f + d.upperMid * 40f
                    2 -> 200f + d.onset * 80f
                    3 -> 50f + d.treble * 40f
                    else -> 90f + d.rms * 60f
                }
            )
            val a = (0.55f + fft.getOrElse(xi % fft.size.coerceAtLeast(1)) { d.rms } * 0.45f)
                .coerceIn(0.35f, 1f) * drive
            drawRect(
                Color.hsv(hue, 0.85f, 0.95f, a),
                topLeft = Offset(xi * cellW, yi * cellH),
                size = androidx.compose.ui.geometry.Size(cellW * 0.92f, cellH * 0.92f)
            )
        }
    }
    // CRT scanlines
    val scanStep = (size.height / 90f).coerceAtLeast(2f)
    var y = 0f
    while (y < size.height) {
        drawLine(
            Color.Black.copy(alpha = 0.18f + d.treble * 0.1f),
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth = 1f
        )
        y += scanStep
    }
    // Beat shock vignette ring
    if (d.onset > 0.35f) {
        drawCircle(
            Color.White.copy(alpha = (d.onset * 0.2f * drive).coerceIn(0f, 0.35f)),
            radius = span * (0.2f + d.bass * 0.25f),
            center = Offset(cx, cy),
            style = Stroke(3f + d.onset * 6f)
        )
    }
}

private fun grafEqCell(x: Float, y: Float, pattern: Int, t: Float): Boolean {
    val p = PI.toFloat()
    return when (pattern) {
        0 -> { // margarita-ish
            val z = sqrt(x * x + y * y) - 3.5f * atan2(y, x) + sin(x + t) + cos(y - t)
            (z % (7f * p) + 7f * p) % (7f * p) < p * 0.5f
        }
        1 -> { // plaid meltdown-ish
            val a = 2f * sin(x * 0.08f * sin(y * 0.08f) + y * 0.08f * sin(x * 0.08f) + t)
            val d = sin(5.5f * x) + sin(5.5f * y)
            0.25f * a > 0.35f * d
        }
        2 -> { // threesome petals
            val z = sin(sqrt((x + 5f).pow(2) + y * y) * 0.4f + t) *
                cos(8f * atan2(y, x + 5f))
            abs(z) in 0.05f..0.55f
        }
        3 -> { // sunlight wedges
            val a = sqrt((3f - x).pow(2) + y * y) + abs(y) + abs(1f - x)
            a in 5f..7f && ((atan2(y, x - 1f) + p) % (p / 7f)) < p / 14f
        }
        else -> { // digital bacteria blocks
            val fx = floor(x * 0.25f)
            val fy = floor(y * 0.25f)
            val z = (sin(fx) + fy).pow(2) + (cos(fy) + fx).pow(2)
            z in 17f..21f
        }
    }
}

// ─── ST02 lunchbreak (nabr / flGyWc) — minkowski reflective solids ───────────

fun DrawScope.drawSt02LunchbreakScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color(0xFF050508))
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.28f
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val span = immersiveViewportDim(size.width, size.height)
    val rot = phase * 0.03f + d.upperMid * 1.2f
    // Soft reflective floor grid
    for (i in 0..16) {
        val t = i / 16f
        val y = cy + span * (0.05f + t * 0.55f) * (0.7f + d.bass * 0.3f)
        drawLine(
            Color.White.copy(alpha = (0.04f + (1f - t) * 0.08f) * drive),
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth = 1f
        )
    }
    // Minkowski-ish superellipse shells
    val shells = 7
    for (s in 0 until shells) {
        val bin = fft.getOrElse((s * fft.size / shells).coerceAtMost(fft.lastIndex)) { d.rms }
        val m = 1.4f + d.treble * 1.2f + s * 0.08f
        val rx = span * (0.12f + s * 0.07f) * (0.85f + bin * 0.55f * drive + d.bass * 0.2f)
        val ry = rx * (0.55f + d.lowMid * 0.25f)
        val path = Path()
        val steps = 72
        for (i in 0..steps) {
            val a = i / steps.toFloat() * PI.toFloat() * 2f + rot + s * 0.2f
            val c = cos(a)
            val sn = sin(a)
            // Approximate minkowski p-norm radius
            val k = (abs(c).pow(m) + abs(sn).pow(m)).pow(1f / m).coerceAtLeast(0.15f)
            val x = cx + (c / k) * rx
            val y = cy + (sn / k) * ry * 0.9f - d.onset * span * 0.02f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        val hue = safeHue(200f + s * 18f + d.onset * 70f + phase * 0.1f)
        val col = Color.hsv(hue, 0.55f + d.upperMid * 0.3f, 0.95f)
        drawPath(
            path,
            col.copy(alpha = (0.2f + bin * 0.45f).coerceIn(0.15f, 0.85f) * drive),
            style = Stroke((2f + bin * 4f + d.onset * 3f).coerceIn(1.5f, 10f), cap = StrokeCap.Round)
        )
        if (s % 2 == 0) {
            drawPath(path, col.copy(alpha = 0.08f * drive), style = Stroke(10f + bin * 8f))
        }
    }
    // Specular highlight blob on beat
    drawCircle(
        Brush.radialGradient(
            listOf(
                Color.White.copy(alpha = (0.35f + d.onset * 0.4f) * drive),
                Color.Transparent
            ),
            center = Offset(cx - span * 0.1f, cy - span * 0.12f),
            radius = span * (0.18f + d.bass * 0.12f)
        ),
        radius = span * (0.18f + d.bass * 0.12f),
        center = Offset(cx - span * 0.1f, cy - span * 0.12f)
    )
}

// ─── ST03 Cyber Fuji 2020 (kaiware007 / Wt33Wf) — sun / fuji / synth grid ───

fun DrawScope.drawSt03CyberFujiScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color(0xFF000A14))
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.35f
    val battery = (0.65f + d.rms * 0.35f * drive).coerceIn(0.4f, 1.1f)
    val cx = size.width * 0.5f
    val horizon = size.height * 0.58f
    val span = immersiveViewportDim(size.width, size.height)

    // Sky wash
    drawRect(
        Brush.verticalGradient(
            listOf(Color(0xFF1A0030), Color(0xFF001428), Color(0xFF000A14)),
            startY = 0f,
            endY = horizon
        ),
        size = androidx.compose.ui.geometry.Size(size.width, horizon)
    )

    // Sun
    val sunC = Offset(cx + span * 0.18f, horizon - span * (0.22f + d.bass * 0.06f))
    val sunR = span * (0.14f + d.bass * 0.05f) * battery
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFFFF6622), Color(0xFFFF2299), Color.Transparent),
            center = sunC,
            radius = sunR * 1.6f
        ),
        radius = sunR * 1.6f,
        center = sunC
    )
    drawCircle(Color(0xFFFF4488).copy(alpha = 0.9f * drive), sunR, sunC)
    // Sun cut stripes
    val stripes = 8
    for (i in 0 until stripes) {
        val yy = sunC.y - sunR + (i + 1) * (sunR * 2f / (stripes + 1))
        val half = sqrt(max(0f, sunR * sunR - (yy - sunC.y).pow(2)))
        val thick = (2f + i * 0.6f + d.treble * 2f) * (1f + sin(phase * 0.05f + i) * 0.2f)
        drawLine(
            Color(0xFF000A14).copy(alpha = 0.85f),
            Offset(sunC.x - half, yy),
            Offset(sunC.x + half, yy),
            strokeWidth = thick
        )
    }

    // Fuji trapezoid
    val fujiTop = horizon - span * 0.28f
    val fujiBaseY = horizon + span * 0.02f
    val fujiPath = Path().apply {
        moveTo(cx - span * 0.42f, fujiBaseY)
        lineTo(cx - span * 0.08f, fujiTop)
        lineTo(cx + span * 0.08f, fujiTop)
        lineTo(cx + span * 0.42f, fujiBaseY)
        close()
    }
    drawPath(
        fujiPath,
        Brush.verticalGradient(
            listOf(Color(0xFFFF1A80), Color(0xFF220066), Color(0xFF000033)),
            startY = fujiTop,
            endY = fujiBaseY
        )
    )
    // Snow cap wave
    val snow = Path()
    val snowY = fujiTop + span * 0.04f
    snow.moveTo(cx - span * 0.1f, snowY)
    for (i in 0..24) {
        val t = i / 24f
        val x = cx - span * 0.1f + t * span * 0.2f
        val y = snowY + sin(t * 12f + phase * 0.08f + d.upperMid * 3f) * span * 0.012f *
            (0.5f + d.onset)
        snow.lineTo(x, y)
    }
    snow.lineTo(cx + span * 0.08f, fujiTop)
    snow.lineTo(cx - span * 0.08f, fujiTop)
    snow.close()
    drawPath(snow, Color(0xFFFFAAFF).copy(alpha = 0.85f * drive))

    // Perspective synth grid
    val gridH = size.height - horizon
    for (row in 1..14) {
        val t = row / 14f
        val y = horizon + t * t * gridH
        val alpha = (0.15f + (1f - t) * 0.35f + d.onset * 0.15f) * drive * battery
        drawLine(
            Color(0xFFFF66FF).copy(alpha = alpha.coerceIn(0.05f, 0.7f)),
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth = 1.5f + d.bass * 1.5f
        )
    }
    val vanish = Offset(cx, horizon - span * 0.02f)
    for (col in -10..10) {
        val edgeX = cx + col * (size.width / 10f) * (1.1f + d.lowMid * 0.2f)
        drawLine(
            Color(0xFFFF66FF).copy(alpha = (0.12f + d.treble * 0.2f) * drive),
            vanish,
            Offset(edgeX, size.height),
            strokeWidth = 1.2f
        )
    }
    // Bass pressure flash on horizon
    if (d.bass > 0.45f) {
        drawRect(
            Color(0xFFFF00AA).copy(alpha = (d.bass - 0.45f) * 0.35f * drive),
            topLeft = Offset(0f, horizon - 4f),
            size = androidx.compose.ui.geometry.Size(size.width, 8f + d.onset * 12f)
        )
    }
}

// ─── MILK//LAB — Apollo originals (XorDev aesthetic vein) ────────────────────

fun DrawScope.drawMlBlackHoleScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    // Inspired by publicly cited "XorDev - MilkDrop2077 - Black hole.milk" (not a .milk port).
    if (!clearBackdrop) drawRect(Color.Black)
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.3f
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val span = immersiveViewportDim(size.width, size.height)
    val rings = 18
    for (i in rings downTo 1) {
        val t = i / rings.toFloat()
        val bin = fft.getOrElse((t * fft.lastIndex).toInt()) { d.rms }
        val r = span * t * (0.48f + d.bass * 0.12f) * (0.9f + bin * 0.35f * drive)
        val twist = phase * 0.04f * (1.2f - t) + d.upperMid * t * 2f
        rotate(degrees = twist * 57.3f, pivot = Offset(cx, cy)) {
            drawCircle(
                Color.hsv(safeHue(20f + t * 40f + d.onset * 80f), 0.9f, 1f)
                    .copy(alpha = (0.08f + (1f - t) * 0.35f + bin * 0.2f) * drive),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke((2f + (1f - t) * 8f + d.onset * 4f).coerceIn(1f, 14f))
            )
        }
    }
    drawCircle(Color.Black, span * (0.08f + d.bass * 0.04f), Offset(cx, cy))
    drawCircle(
        Color.hsv(safeHue(35f + d.onset * 40f), 1f, 1f).copy(alpha = 0.5f * drive),
        span * (0.09f + d.onset * 0.03f),
        Offset(cx, cy),
        style = Stroke(3f + d.onset * 5f)
    )
}

fun DrawScope.drawMlEventHorizonScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) = drawLensWarpScene(features, phase, playing, clearBackdrop, baseHue = 260f, nameBoost = 1.15f)

fun DrawScope.drawMlXorPlasmaScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color.Black)
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.28f
    val cols = 36
    val rows = 22
    val cw = size.width / cols
    val ch = size.height / rows
    for (yi in 0 until rows) {
        for (xi in 0 until cols) {
            val u = xi / cols.toFloat()
            val v = yi / rows.toFloat()
            val n = sin(u * 9f + phase * 0.05f + d.bass * 3f) *
                cos(v * 11f - phase * 0.04f + d.upperMid * 2f) +
                sin((u + v) * 7f + phase * 0.03f + d.treble * 4f)
            val bin = fft.getOrElse(((u * fft.lastIndex).toInt())) { d.rms }
            val bright = ((n + 1.4f) * 0.35f + bin * 0.4f).coerceIn(0f, 1f) * drive
            if (bright < 0.12f) continue
            drawRect(
                Color.hsv(safeHue(160f + n * 90f + d.onset * 50f), 0.95f, bright),
                topLeft = Offset(xi * cw, yi * ch),
                size = androidx.compose.ui.geometry.Size(cw * 0.95f, ch * 0.95f)
            )
        }
    }
}

fun DrawScope.drawMlInfernalMeshScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color(0xFF0A0000))
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.3f
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val span = immersiveViewportDim(size.width, size.height)
    val arms = 10
    for (a in 0 until arms) {
        val ang0 = a / arms.toFloat() * PI.toFloat() * 2f + phase * 0.02f
        val path = Path()
        val segs = 40
        for (s in 0..segs) {
            val t = s / segs.toFloat()
            val bin = fft.getOrElse((t * fft.lastIndex).toInt()) { d.rms }
            val ang = ang0 + t * (1.8f + d.upperMid) + sin(t * 8f + phase * 0.05f) * 0.4f * drive
            val r = span * t * (0.48f + d.bass * 0.1f) * (0.85f + bin * 0.4f)
            val x = cx + cos(ang) * r
            val y = cy + sin(ang) * r
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            Color.hsv(safeHue(5f + a * 8f + d.onset * 30f), 1f, 1f)
                .copy(alpha = (0.35f + d.onset * 0.4f) * drive),
            style = Stroke(2.5f + d.bass * 4f, cap = StrokeCap.Round)
        )
    }
}

fun DrawScope.drawMlAstralCollapseScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color.Black)
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.28f
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val span = immersiveViewportDim(size.width, size.height)
    val collapse = 0.55f + d.bass * 0.45f * drive
    for (i in 0 until 48) {
        val t = i / 47f
        val ang = t * PI.toFloat() * 8f + phase * 0.06f
        val r = span * (1f - t) * collapse * (0.2f + fft.getOrElse(i % fft.size) { d.rms } * 0.5f)
        val x = cx + cos(ang) * r
        val y = cy + sin(ang) * r * (0.75f + d.lowMid * 0.2f)
        drawCircle(
            Color.hsv(safeHue(210f + t * 120f + d.onset * 40f), 0.85f, 1f)
                .copy(alpha = (0.25f + (1f - t) * 0.5f) * drive),
            radius = (2f + (1f - t) * 7f + d.onset * 4f).coerceIn(1.5f, 12f),
            center = Offset(x, y)
        )
    }
}

fun DrawScope.drawMlNeonRitualScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color(0xFF050010))
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.3f
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val span = immersiveViewportDim(size.width, size.height)
    val sides = 6 + (d.upperMid * 4f).toInt()
    for (ring in 1..6) {
        val path = Path()
        val r = span * (0.08f + ring * 0.07f) * (0.9f + d.bass * 0.25f * drive)
        for (i in 0..sides) {
            val a = i / sides.toFloat() * PI.toFloat() * 2f + phase * 0.02f * ring
            val wob = 1f + sin(a * 3f + phase * 0.05f) * 0.12f * drive +
                fft.getOrElse(i % fft.size) { 0f } * 0.2f
            val x = cx + cos(a) * r * wob
            val y = cy + sin(a) * r * wob
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(
            path,
            Color.hsv(safeHue(300f + ring * 25f + d.onset * 50f), 1f, 1f)
                .copy(alpha = (0.4f + d.onset * 0.35f) * drive),
            style = Stroke(2f + ring * 0.4f + d.treble * 2f)
        )
    }
}

fun DrawScope.drawMlLiquidWarpScene(
    features: AudioFeatures,
    palette: HudPalette,
    wave: FloatArray,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color(0xFF000818))
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.28f
    for (layer in 0 until 5) {
        val path = Path()
        val n = max(wave.size, 48)
        val y0 = size.height * (0.25f + layer * 0.12f)
        val amp = size.height * (0.06f + d.rms * 0.12f) * drive
        for (i in 0 until n) {
            val t = i / (n - 1f)
            val w = wave.getOrElse((t * wave.lastIndex).toInt()) { 0f }
            val f = fft.getOrElse((t * fft.lastIndex).toInt()) { d.lowMid }
            val x = size.width * t
            val y = y0 + sin(t * 10f + phase * 0.05f + layer) * amp +
                w * amp * 1.4f + f * amp * 0.5f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            Color.hsv(safeHue(170f + layer * 25f + d.onset * 40f), 0.9f, 1f)
                .copy(alpha = (0.45f - layer * 0.05f) * drive),
            style = Stroke(3f + d.bass * 4f, cap = StrokeCap.Round)
        )
    }
}

fun DrawScope.drawMlGlitchCoreScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color.Black)
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.25f
    val bands = 24
    val h = size.height / bands
    for (i in 0 until bands) {
        val bin = fft.getOrElse((i * fft.size / bands).coerceAtMost(fft.lastIndex)) { d.rms }
        val shift = if (d.onset > 0.4f || bin > 0.7f) {
            sin(phase * 0.2f + i) * size.width * 0.08f * drive * d.onset
        } else 0f
        val w = size.width * (0.2f + bin * 0.75f) * drive
        drawRect(
            Color.hsv(safeHue(i * 14f + phase * 0.3f), 1f, 1f)
                .copy(alpha = (0.35f + bin * 0.5f).coerceIn(0.2f, 1f)),
            topLeft = Offset((size.width - w) * 0.5f + shift, i * h),
            size = androidx.compose.ui.geometry.Size(w, h * 0.85f)
        )
    }
}

fun DrawScope.drawMlChaosTunnelScene(
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
    for (i in 0 until 16) {
        val t = ((i / 16f) + (phase * 0.01f + d.bass * 0.05f) % 1f) % 1f
        val r = span * (0.05f + t * 0.55f) * (0.85f + d.rms * 0.3f * drive)
        val path = Path()
        val steps = 48
        for (s in 0..steps) {
            val a = s / steps.toFloat() * PI.toFloat() * 2f + t * 4f + phase * 0.03f
            val jagged = 1f + sin(a * 5f + phase * 0.1f) * 0.15f * drive +
                fft.getOrElse(s % fft.size) { 0f } * 0.2f
            val x = cx + cos(a) * r * jagged
            val y = cy + sin(a) * r * jagged * (0.8f + d.lowMid * 0.15f)
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(
            path,
            Color.hsv(safeHue(t * 360f + d.onset * 60f), 0.95f, 1f)
                .copy(alpha = ((1f - t) * 0.55f + 0.1f) * drive),
            style = Stroke((2f + (1f - t) * 5f).coerceIn(1.5f, 8f))
        )
    }
}

fun DrawScope.drawMlFractalBiteScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color(0xFF080000))
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.28f
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val span = immersiveViewportDim(size.width, size.height)
    fun tri(level: Int, x: Float, y: Float, r: Float, ang: Float) {
        if (level <= 0 || r < 6f) return
        val path = Path()
        for (i in 0..3) {
            val a = ang + i / 3f * PI.toFloat() * 2f
            val px = x + cos(a) * r
            val py = y + sin(a) * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        val bin = fft.getOrElse(level * 3 % fft.size.coerceAtLeast(1)) { d.rms }
        drawPath(
            path,
            Color.hsv(safeHue(330f - level * 35f + d.onset * 40f), 0.9f, 1f)
                .copy(alpha = (0.25f + bin * 0.4f) * drive),
            style = Stroke((1.5f + level * 0.4f + d.bass * 2f).coerceIn(1f, 6f))
        )
        val nr = r * (0.48f + d.upperMid * 0.08f)
        for (k in 0 until 3) {
            val a = ang + k / 3f * PI.toFloat() * 2f + phase * 0.01f
            tri(level - 1, x + cos(a) * r * 0.55f, y + sin(a) * r * 0.55f, nr, a)
        }
    }
    tri(4, cx, cy, span * (0.28f + d.bass * 0.1f) * drive.coerceAtLeast(0.5f), phase * 0.02f)
}

private fun DrawScope.drawLensWarpScene(
    features: AudioFeatures,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean,
    baseHue: Float,
    nameBoost: Float
) {
    if (!clearBackdrop) drawRect(Color.Black)
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.3f
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val span = immersiveViewportDim(size.width, size.height) * nameBoost
    for (i in 0 until 22) {
        val t = i / 21f
        val r = span * (0.06f + t * 0.42f) * (0.9f + d.bass * 0.2f * drive)
        val squash = 0.55f + d.upperMid * 0.35f + sin(phase * 0.04f + t * 3f) * 0.1f
        drawOval(
            Color.hsv(safeHue(baseHue + t * 80f + d.onset * 50f), 0.85f, 1f)
                .copy(alpha = (0.12f + (1f - t) * 0.4f) * drive),
            topLeft = Offset(cx - r, cy - r * squash),
            size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f * squash),
            style = Stroke((1.5f + (1f - t) * 5f + d.onset * 3f).coerceIn(1f, 10f))
        )
    }
}
