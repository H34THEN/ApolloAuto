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
// Repair notes: prior sparse cell sampling + phase*0.008 pattern hopping left the
// screen mostly black. Rebuild as a dense CRT field that always fills, with audio
// driving zoom / hue / pattern blend / beat pulses (Compose interpretation).

fun DrawScope.drawSt01GrafEqScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color(0xFF050308))
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.35f
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val shortSide = min(size.width, size.height)
    // Convert continuous warpClock (≈ ms*0.12) → seconds-ish for gentle motion.
    val tSec = phase * 0.008f
    // Hold each equation family for several seconds (not every ~1s).
    val pattern = ((tSec * 0.18f + d.onset * 0.35f + d.bass * 0.25f) % 5f).toInt().coerceIn(0, 4)
    val cols = 64
    val rows = 40
    val cellW = size.width / cols
    val cellH = size.height / rows
    val zoom = (1.15f + d.bass * 0.55f * drive + d.rms * 0.2f).coerceIn(0.85f, 2.0f)
    val panX = sin(tSec * 0.35f) * (0.8f + d.lowMid * 1.2f) * drive
    val panY = cos(tSec * 0.28f) * (0.6f + d.upperMid) * drive

    var litCount = 0
    for (yi in 0 until rows) {
        for (xi in 0 until cols) {
            // Height-normalized plane (Shadertoy-style), then zoom/pan.
            val u = ((xi + 0.5f) / cols - 0.5f) * (size.width / shortSide) * 2.2f
            val v = ((yi + 0.5f) / rows - 0.5f) * 2.2f
            val nx = u * zoom + panX
            val ny = v * zoom + panY
            val field = grafEqField(nx, ny, pattern, tSec + d.treble * 0.8f)
            if (field < 0.08f) continue
            litCount++
            val bin = fft.getOrElse(((xi + yi) % fft.size.coerceAtLeast(1))) { d.rms }
            val hue = safeHue(
                when (pattern) {
                    0 -> 350f + d.bass * 50f + field * 40f
                    1 -> 270f + d.upperMid * 50f
                    2 -> 190f + d.onset * 70f
                    3 -> 40f + d.treble * 50f
                    else -> 95f + d.rms * 55f
                } + tSec * 8f
            )
            val bright = (0.45f + field * 0.55f + bin * 0.25f).coerceIn(0.25f, 1f) * drive
            drawRect(
                Color.hsv(hue, (0.75f + d.presence * 0.2f).coerceIn(0.5f, 1f), bright),
                topLeft = Offset(xi * cellW, yi * cellH),
                size = androidx.compose.ui.geometry.Size(cellW * 0.94f, cellH * 0.94f)
            )
        }
    }
    // Guarantee non-blank frame if equations undersample (fallback spiral).
    if (litCount < 40) {
        for (i in 0 until 96) {
            val a = i / 96f * PI.toFloat() * 6f + tSec
            val r = shortSide * (0.08f + (i / 96f) * 0.42f) * (0.9f + d.bass * 0.25f)
            drawCircle(
                Color.hsv(safeHue(i * 3f + tSec * 20f), 0.9f, 0.95f)
                    .copy(alpha = (0.35f + d.rms * 0.4f) * drive),
                radius = 2.5f + d.treble * 2f,
                center = Offset(cx + cos(a) * r, cy + sin(a) * r * 0.75f)
            )
        }
    }
    // CRT scanlines + grille
    val scanStep = (size.height / 72f).coerceAtLeast(2f)
    var y = 0f
    while (y < size.height) {
        drawLine(
            Color.Black.copy(alpha = (0.22f + d.treble * 0.12f).coerceIn(0.12f, 0.4f)),
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth = 1.2f
        )
        y += scanStep
    }
    // Soft vignette
    drawCircle(
        Brush.radialGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
            center = Offset(cx, cy),
            radius = shortSide * 0.85f
        ),
        radius = shortSide * 0.95f,
        center = Offset(cx, cy)
    )
    // Beat shock ring
    if (d.onset > 0.28f) {
        drawCircle(
            Color.White.copy(alpha = (d.onset * 0.28f * drive).coerceIn(0f, 0.45f)),
            radius = shortSide * (0.18f + d.bass * 0.22f),
            center = Offset(cx, cy),
            style = Stroke(2.5f + d.onset * 7f)
        )
    }
}

/** Soft field 0..1 for denser CRT fill (not sparse boolean). */
private fun grafEqField(x: Float, y: Float, pattern: Int, t: Float): Float {
    val p = PI.toFloat()
    return when (pattern) {
        0 -> { // margarita spirals
            val z = sqrt(x * x + y * y) - 2.2f * atan2(y, x + 1e-4f) +
                sin(x * 0.7f + t) + cos(y * 0.7f - t)
            val m = abs(((z % (2f * p)) + 2f * p) % (2f * p) - p) / p
            (1f - m).coerceIn(0f, 1f)
        }
        1 -> { // plaid interference
            val a = sin(x * 1.4f + t) * cos(y * 1.4f - t) +
                0.5f * sin((x + y) * 2.2f + t * 1.3f)
            ((a + 1.2f) * 0.45f).coerceIn(0f, 1f)
        }
        2 -> { // petal lobes
            val r = sqrt(x * x + y * y) + 0.001f
            val ang = atan2(y, x)
            val petals = abs(sin(ang * 4f + t)) * (0.55f + 0.45f * sin(r * 1.8f - t))
            (petals * (1.15f - (r * 0.12f).coerceAtMost(1f))).coerceIn(0f, 1f)
        }
        3 -> { // radial wedges / sunburst
            val ang = atan2(y, x)
            val r = sqrt(x * x + y * y)
            val wedge = abs(sin(ang * 7f + t * 0.6f))
            val band = (1f - abs(r - 4.5f - sin(t) * 1.2f) * 0.35f).coerceIn(0f, 1f)
            (wedge * 0.65f + band * 0.55f).coerceIn(0f, 1f)
        }
        else -> { // digital bacteria / pixel blocks
            val fx = floor(x * 1.6f + t * 0.4f)
            val fy = floor(y * 1.6f - t * 0.3f)
            val cell = abs(sin(fx * 12.9898f + fy * 78.233f)) * 43758.5453f
            val n = cell - floor(cell)
            val edge = ((abs(x * 1.6f - fx - 0.5f) + abs(y * 1.6f - fy - 0.5f)) * 1.6f)
                .coerceIn(0f, 1f)
            ((1f - edge) * (0.35f + n * 0.8f)).coerceIn(0f, 1f)
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
// Repair notes: prior layout used immersiveViewportDim (max edge) for vertical
// offsets, pushing Fuji/sun off-screen in landscape. Use height-normalized units
// matching Shadertoy uv = (2*frag - res)/res.y.

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
    val drive = if (playing) 1f else 0.4f
    // Audio → "battery" energy like the original mouse/battery control.
    val battery = (0.55f + d.rms * 0.35f + d.bass * 0.25f * drive).coerceIn(0.45f, 1.15f)
    val h = size.height.coerceAtLeast(1f)
    val w = size.width.coerceAtLeast(1f)
    val cx = w * 0.5f
    // Horizon at uv.y ≈ -0.2 → pixel Y = h/2 + 0.2*(h/2) wait:
    // uv.y = (2*py - h)/h → py = (uv.y * h + h) / 2 = h*(uv.y+1)/2
    // horizon uv.y = -0.2 → py = h * 0.4
    val horizon = h * 0.42f
    val tSec = phase * 0.008f

    // Sky
    drawRect(
        Brush.verticalGradient(
            listOf(Color(0xFF2A0050), Color(0xFF100028), Color(0xFF000A18)),
            startY = 0f,
            endY = horizon
        ),
        size = androidx.compose.ui.geometry.Size(w, horizon)
    )

    // Sun — place in upper-right of sky using height units (never max-edge span).
    val sunR = h * (0.11f + d.bass * 0.04f) * battery
    val sunC = Offset(
        cx + w * 0.18f,
        horizon - h * (0.18f + d.bass * 0.04f)
    )
    drawCircle(
        Brush.radialGradient(
            listOf(
                Color(0xFFFF8844).copy(alpha = 0.95f * drive),
                Color(0xFFFF2288).copy(alpha = 0.85f * drive),
                Color.Transparent
            ),
            center = sunC,
            radius = sunR * 1.75f
        ),
        radius = sunR * 1.75f,
        center = sunC
    )
    drawCircle(Color(0xFFFF4488).copy(alpha = 0.95f * drive), sunR, sunC)
    val stripes = 9
    for (i in 0 until stripes) {
        val yy = sunC.y - sunR + (i + 1) * (sunR * 2f / (stripes + 1))
        val half = sqrt(max(0f, sunR * sunR - (yy - sunC.y).pow(2)))
        val thick = (2.5f + i * 0.55f + d.treble * 2.5f) *
            (1f + 0.15f * sin(tSec * 3f + i + d.onset))
        drawLine(
            Color(0xFF000A14).copy(alpha = 0.9f),
            Offset(sunC.x - half, yy),
            Offset(sunC.x + half, yy),
            strokeWidth = thick.coerceIn(1.5f, 14f)
        )
    }

    // Fuji — height-relative trapezoid, always on-screen.
    val fujiTop = horizon - h * (0.22f + d.sub * 0.03f)
    val fujiBaseY = horizon + h * 0.03f
    val fujiHalfBase = w * (0.38f + d.lowMid * 0.04f)
    val fujiHalfTop = w * 0.055f
    val fujiPath = Path().apply {
        moveTo(cx - fujiHalfBase, fujiBaseY)
        lineTo(cx - fujiHalfTop, fujiTop)
        lineTo(cx + fujiHalfTop, fujiTop)
        lineTo(cx + fujiHalfBase, fujiBaseY)
        close()
    }
    drawPath(
        fujiPath,
        Brush.verticalGradient(
            listOf(
                Color(0xFFFF2A90).copy(alpha = drive),
                Color(0xFF3A0080),
                Color(0xFF080020)
            ),
            startY = fujiTop,
            endY = fujiBaseY
        )
    )
    // Snow cap
    val snow = Path()
    val snowY = fujiTop + h * 0.035f
    val snowHalf = fujiHalfTop + w * 0.04f
    snow.moveTo(cx - snowHalf, snowY)
    for (i in 0..28) {
        val tt = i / 28f
        val x = cx - snowHalf + tt * snowHalf * 2f
        val y = snowY + sin(tt * 14f + tSec * 2.2f + d.upperMid * 4f) *
            h * 0.01f * (0.6f + d.onset)
        snow.lineTo(x, y)
    }
    snow.lineTo(cx + fujiHalfTop, fujiTop)
    snow.lineTo(cx - fujiHalfTop, fujiTop)
    snow.close()
    drawPath(snow, Color(0xFFFFCCFF).copy(alpha = 0.9f * drive))
    // Outline
    drawPath(
        fujiPath,
        Color(0xFFFF88FF).copy(alpha = (0.55f + d.onset * 0.35f) * drive),
        style = Stroke((2f + d.treble * 2f).coerceIn(1.5f, 5f))
    )

    // Perspective synth grid (floor below horizon)
    val gridH = h - horizon
    val scroll = tSec * (1.2f + d.bass * 2.5f) * drive
    for (row in 1..16) {
        val t = ((row / 16f) + scroll * 0.08f) % 1f
        val y = horizon + t * t * gridH
        val bin = fft.getOrElse((t * fft.lastIndex).toInt()) { d.rms }
        val alpha = ((1f - t) * (0.2f + d.rms * 0.35f) + bin * 0.15f + d.onset * 0.1f) *
            drive * battery
        drawLine(
            Color(0xFFFF66FF).copy(alpha = alpha.coerceIn(0.06f, 0.8f)),
            Offset(0f, y),
            Offset(w, y),
            strokeWidth = (1.4f + d.bass * 2f + bin * 1.5f).coerceIn(1f, 5f)
        )
    }
    val vanish = Offset(cx, horizon)
    for (col in -12..12) {
        val edgeX = cx + col * (w / 11f) * (1.05f + d.lowMid * 0.15f)
        drawLine(
            Color(0xFFFF66FF).copy(alpha = (0.14f + d.treble * 0.22f) * drive),
            vanish,
            Offset(edgeX, h),
            strokeWidth = 1.2f
        )
    }

    // Horizon glow + bass pressure
    drawRect(
        Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color(0xFFFF40AA).copy(alpha = (0.25f + d.onset * 0.35f) * drive),
                Color.Transparent
            )
        ),
        topLeft = Offset(0f, horizon - 3f),
        size = androidx.compose.ui.geometry.Size(w, 6f + d.bass * 14f)
    )
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
