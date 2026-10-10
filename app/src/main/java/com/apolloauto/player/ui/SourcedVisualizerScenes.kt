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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Compose-only scenes inspired by public visualizer projects.
 * Not ports of their engines/presets — original geometry for Apollo.
 *
 * Attribution (see VisualizerModeCatalog categories):
 * - projectM-visualizer/projectm
 * - milkdrop2077/MilkDrop3
 * - gauravk95/audio-visualizer-android
 * - bradleybauer/music_visualizer
 * - harjot-oberai/MusicDNA (CC BY-NC-SA — non-commercial inspired aesthetic)
 */

fun DrawScope.drawProjectmPlasmaScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(Color.Black)
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.35f
    val cx = size.width / 2f
    val cy = size.height / 2f
    // FILL-style span (max edge) so landscape Immersive is occupied like POLY.
    val maxR = immersiveViewportDim(size.width, size.height) * 0.48f
    val rings = 10
    for (i in 0 until rings) {
        val t = i / (rings - 1f)
        val bin = fft.getOrElse((t * fft.lastIndex.coerceAtLeast(0)).toInt()) { d.rms }
            .coerceIn(0f, 1.2f)
        val r = maxR * (0.12f + t * 0.92f) * (0.85f + bin * 0.7f * drive)
        val wobble = phase * 0.025f + t * 2.4f + d.bass * 1.1f
        val path = Path()
        val steps = 64
        for (s in 0..steps) {
            val a = (s / steps.toFloat()) * PI.toFloat() * 2f + wobble
            val rr = r * (1f + 0.14f * sin(a * 3f + phase * 0.04f) * drive + bin * 0.12f)
            val x = cx + cos(a) * rr
            val y = cy + sin(a) * rr * (0.82f + d.lowMid * 0.12f)
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        val hue = ((t * 260f + phase * 0.12f + d.onset * 55f) % 360f).coerceIn(0f, 360f)
        val col = Color.hsv(
            hue,
            0.95f.coerceIn(0f, 1f),
            (0.85f + bin * 0.15f).coerceIn(0.5f, 1f)
        )
        // Solid bright stroke — previous radial brush washed out on car screens
        drawPath(
            path,
            col.copy(alpha = (0.75f + bin * 0.25f).coerceIn(0.65f, 1f)),
            style = Stroke((3.5f + bin * 5f + d.bass * 2f).coerceIn(3f, 12f), cap = StrokeCap.Round)
        )
        if (i % 2 == 0) {
            drawPath(
                path,
                col.copy(alpha = 0.18f),
                style = Stroke((8f + bin * 6f).coerceIn(6f, 18f))
            )
        }
    }
}

fun DrawScope.drawMilkdropWaveScene(
    features: AudioFeatures,
    palette: HudPalette,
    wave: FloatArray,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.28f
    val layers = 4
    for (layer in 0 until layers) {
        val path = Path()
        val n = max(wave.size, 32)
        val yBase = size.height * (0.35f + layer * 0.12f)
        val amp = size.height * (0.08f + d.rms * 0.1f + layer * 0.02f) * drive
        for (i in 0 until n) {
            val t = i / (n - 1f)
            val w = wave.getOrElse((t * wave.lastIndex.coerceAtLeast(0)).toInt()) { 0f }
            val f = fft.getOrElse((t * fft.lastIndex.coerceAtLeast(0)).toInt()) { d.lowMid }
            val x = size.width * t
            val y = yBase +
                sin(t * PI.toFloat() * 6f + phase * 0.04f + layer) * amp * 0.35f +
                w * amp +
                f * amp * 0.4f * sin(phase * 0.02f + t * 8f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val alpha = (0.35f + d.onset * 0.25f - layer * 0.05f).coerceIn(0.15f, 0.9f)
        val col = when (layer % 3) {
            0 -> palette.primary.copy(alpha = alpha)
            1 -> palette.secondary.copy(alpha = alpha)
            else -> palette.accent.copy(alpha = alpha)
        }
        drawPath(path, col, style = Stroke((2f + d.bass * 2f).coerceIn(1f, 6f), cap = StrokeCap.Round))
    }
    // Soft radial “warp” glow on kicks
    if (d.bass > 0.45f) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    palette.accent.copy(alpha = (0.15f + d.bass * 0.2f).coerceAtMost(0.4f)),
                    Color.Transparent
                ),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = immersiveViewportDim(size.width, size.height) * 0.5f
            ),
            radius = immersiveViewportDim(size.width, size.height) * 0.5f,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    }
}

fun DrawScope.drawAvCircleBarsScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.25f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val span = immersiveViewportDim(size.width, size.height)
    val bars = min(fft.size, 64).coerceAtLeast(24)
    val inner = span * (0.16f + d.bass * 0.08f)
    val maxLen = span * 0.38f
    rotate(degrees = phase * 0.02f, pivot = Offset(cx, cy)) {
        for (i in 0 until bars) {
            val v = fft.getOrElse(i * fft.size / bars) { 0f }.coerceIn(0f, 1.2f) * drive
            val ang = (i / bars.toFloat()) * PI.toFloat() * 2f - PI.toFloat() / 2f
            val len = inner + v * maxLen
            val c0 = Offset(cx + cos(ang) * inner, cy + sin(ang) * inner)
            val c1 = Offset(cx + cos(ang) * len, cy + sin(ang) * len)
            val hue = ((i / bars.toFloat()) * 360f).coerceIn(0f, 360f)
            val col = Color.hsv(hue, 0.9f.coerceIn(0f, 1f), (0.7f + v * 0.3f).coerceIn(0.2f, 1f))
            drawLine(
                color = col.copy(alpha = (0.55f + v * 0.4f).coerceIn(0.3f, 1f)),
                start = c0,
                end = c1,
                strokeWidth = (4f + v * 4f).coerceIn(2f, 10f),
                cap = StrokeCap.Round
            )
        }
    }
    drawCircle(
        color = palette.soft.copy(alpha = 0.15f + d.rms * 0.25f),
        radius = inner * 0.55f,
        center = Offset(cx, cy)
    )
}

fun DrawScope.drawBauerFountainScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val drive = if (playing) 1f else 0.22f
    val cx = size.width / 2f
    val baseY = size.height * 0.88f
    val count = 90
    for (i in 0 until count) {
        val seed = i * 17.3f
        val band = fft.getOrElse(i % max(1, fft.size)) { d.rms }.coerceIn(0f, 1.2f)
        val life = fract01(phase * (0.01f + band * 0.02f) + seed)
        val spread = (i / count.toFloat() - 0.5f) * size.width * (0.7f + d.lowMid * 0.2f)
        val x = cx + spread * (0.3f + life) + sin(seed + phase * 0.03f) * 12f * drive
        val y = baseY - life * size.height * (0.55f + band * 0.45f + d.bass * 0.2f) * drive
        if (!x.isFinite() || !y.isFinite()) continue
        val r = (2f + band * 5f + (1f - life) * 2f).coerceIn(1f, 12f)
        val alpha = ((1f - life) * (0.4f + band * 0.5f)).coerceIn(0.1f, 1f)
        val col = when {
            i % 5 == 0 -> palette.accent.copy(alpha = alpha)
            i % 3 == 0 -> palette.secondary.copy(alpha = alpha)
            else -> palette.primary.copy(alpha = alpha)
        }
        drawCircle(color = col, radius = r, center = Offset(x, y))
        if (band > 0.55f && i % 2 == 0) {
            drawLine(
                color = col.copy(alpha = alpha * 0.5f),
                start = Offset(x, y),
                end = Offset(x, y + r * 3f),
                strokeWidth = 1.2f
            )
        }
    }
}

/** bradleybauer fft preset — log-spaced spectrum ribbons. */
fun DrawScope.drawBauerFftScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.25f
    val n = 96
    val midY = size.height * 0.55f
    for (i in 0 until n) {
        val t = i / (n - 1f)
        // log stretch like bauer fft geom
        val stretch = 100f
        val freqT = (Math.pow(stretch.toDouble(), (t - 1.0)) - (1.0 - t) / stretch).toFloat()
            .coerceIn(0f, 1f)
        val idx = (freqT * fft.lastIndex.coerceAtLeast(0)).toInt()
        val amp = (fft.getOrElse(idx) { 0f } * drive).coerceIn(0f, 1.2f)
        val x = size.width * t
        val h = amp * size.height * 0.42f
        val col = Color.hsv((t * 280f + 20f) % 360f, 0.85f, (0.55f + amp * 0.45f).coerceIn(0.2f, 1f))
        drawLine(
            color = col.copy(alpha = (0.4f + amp * 0.55f).coerceIn(0.2f, 1f)),
            start = Offset(x, midY - h),
            end = Offset(x, midY + h * 0.35f),
            strokeWidth = (size.width / n * 0.7f).coerceAtLeast(1.5f),
            cap = StrokeCap.Round
        )
    }
    drawCircle(
        color = palette.accent.copy(alpha = 0.1f + features.bass * 0.2f),
        radius = immersiveViewportDim(size.width, size.height) * (0.08f + features.onset * 0.1f),
        center = Offset(size.width / 2f, midY)
    )
}

/** bradleybauer spectrogram — scrolling column history. */
private object BauerSpecHistory {
    private const val COLS = 96
    private const val ROWS = 48
    private val cells = Array(COLS) { FloatArray(ROWS) }
    private var write = 0

    fun push(fft: FloatArray, drive: Float) {
        val col = cells[write % COLS]
        for (r in 0 until ROWS) {
            val idx = (r / (ROWS - 1f) * fft.lastIndex.coerceAtLeast(0)).toInt()
            col[r] = (fft.getOrElse(idx) { 0f } * drive).coerceIn(0f, 1.2f)
        }
        write++
    }

    fun draw(scope: DrawScope) = with(scope) {
        val cw = size.width / COLS
        val rh = size.height / ROWS
        for (c in 0 until COLS) {
            val src = cells[(write + c) % COLS]
            for (r in 0 until ROWS) {
                val v = src[ROWS - 1 - r]
                if (v < 0.04f) continue
                drawRect(
                    color = Color.hsv(
                        (200f - v * 160f).coerceIn(0f, 360f),
                        0.9f,
                        (0.35f + v * 0.65f).coerceIn(0.2f, 1f),
                        alpha = (0.35f + v * 0.55f).coerceIn(0.15f, 1f)
                    ),
                    topLeft = Offset(c * cw, r * rh),
                    size = androidx.compose.ui.geometry.Size(cw + 0.5f, rh + 0.5f)
                )
            }
        }
    }
}

fun DrawScope.drawBauerSpecScene(
    palette: HudPalette,
    fft: FloatArray,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    BauerSpecHistory.push(fft, if (playing) 1f else 0.2f)
    BauerSpecHistory.draw(this)
}

/** projectM-style waveform scope. */
fun DrawScope.drawProjectmScopeScene(
    palette: HudPalette,
    wave: FloatArray,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.25f
    val path = Path()
    val n = max(wave.size, 64)
    for (i in 0 until n) {
        val t = i / (n - 1f)
        val w = wave.getOrElse((t * wave.lastIndex.coerceAtLeast(0)).toInt()) { 0f }
        val f = fft.getOrElse((t * fft.lastIndex.coerceAtLeast(0)).toInt()) { 0f }
        val x = size.width * t
        val y = size.height * 0.5f +
            (w * 0.55f + f * 0.35f) * size.height * 0.4f * drive +
            sin(t * PI.toFloat() * 4f + phase * 0.05f) * 8f
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, palette.primary.copy(alpha = 0.9f), style = Stroke(2.5f, cap = StrokeCap.Round))
    drawPath(path, palette.accent.copy(alpha = 0.25f), style = Stroke(8f))
}

/** MilkDrop rotor — spinning mirrored spectrum. */
fun DrawScope.drawMilkdropRotorScene(
    features: AudioFeatures,
    palette: HudPalette,
    fft: FloatArray,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean = false
) {
    if (!clearBackdrop) drawRect(palette.voidBlack)
    val drive = if (playing) 1f else 0.25f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val arms = 6
    val span = immersiveViewportDim(size.width, size.height)
    for (arm in 0 until arms) {
        val path = Path()
        val base = phase * 0.04f + arm * (PI.toFloat() * 2f / arms)
        for (i in 0 until 40) {
            val t = i / 39f
            val amp = fft.getOrElse((t * fft.lastIndex.coerceAtLeast(0)).toInt()) { 0f } * drive
            val r = t * span * (0.22f + amp * 0.42f)
            val a = base + t * 1.8f + features.bass * 0.4f
            val x = cx + cos(a) * r
            val y = cy + sin(a) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            Color.hsv((arm * 55f + phase * 0.1f) % 360f, 0.85f, 0.9f, alpha = 0.75f),
            style = Stroke(2.2f + features.onset * 2f)
        )
    }
}

fun DrawScope.drawDnaCircleScene(
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
    val cx = size.width / 2f
    val cy = size.height / 2f
    val bars = 48
    val span = immersiveViewportDim(size.width, size.height)
    val baseR = span * 0.26f
    val maxReach = span * 0.48f
    for (i in 0 until bars) {
        val t = i / bars.toFloat()
        val v = fft.getOrElse((t * fft.lastIndex.coerceAtLeast(0)).toInt()) { 0f }
            .coerceIn(0f, 1f) * drive
        val ang = t * PI.toFloat() * 2f + phase * 0.015f
        val twist = sin(ang * 2f + phase * 0.04f) * 8f
        val r0 = baseR + twist
        val r1 = (r0 + v * span * 0.22f).coerceAtMost(maxReach)
        val a = ang
        val c0 = Offset(cx + cos(a) * r0, cy + sin(a) * r0)
        val c1 = Offset(cx + cos(a) * r1, cy + sin(a) * r1)
        if (!c0.x.isFinite() || !c1.x.isFinite()) continue
        val strand = if (i % 2 == 0) palette.primary else palette.secondary
        drawLine(
            color = strand.copy(alpha = (0.5f + v * 0.45f).coerceIn(0.25f, 1f)),
            start = c0,
            end = c1,
            strokeWidth = (2.5f + v * 2.5f).coerceIn(1.5f, 6f),
            cap = StrokeCap.Round
        )
        if (i % 4 == 0) {
            drawCircle(
                color = palette.accent.copy(alpha = (0.35f + d.onset * 0.25f).coerceAtMost(0.7f)),
                radius = 2.5f + v * 1.8f,
                center = c1
            )
        }
    }
}

private fun fract01(v: Float): Float {
    if (!v.isFinite()) return 0f
    val f = v - kotlin.math.floor(v.toDouble()).toFloat()
    return if (f < 0f) f + 1f else f.coerceIn(0f, 1f)
}
