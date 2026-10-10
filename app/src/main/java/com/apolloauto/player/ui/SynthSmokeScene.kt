package com.apolloauto.player.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.apolloauto.player.media.AudioFeatures
import com.apolloauto.player.media.VizPaletteMode
import com.apolloauto.player.media.VizQualityTier
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * SYNTH SMOKE — procedural translucent vapor (not stroked waveforms).
 *
 * V5.1 root cause of "wavy lines": the prior renderer drew Path strokes along
 * sine curves. This implementation builds soft density plumes via hashed fBm
 * and layered radial falloff so still frames read as billowing smoke.
 */

enum class SmokeColorPreset(val title: String) {
    NEON_VAPOR("Neon Vapor"),
    TOXIC_DREAMS("Toxic Dreams"),
    HADES_ASH("Hades Ash"),
    RAINBOW_ETHER("Rainbow Ether"),
    GHOST_SMOKE("Ghost Smoke"),
    BLACKLIGHT("Blacklight Plume")
}

data class SmokeControls(
    val density: Float = 1f,
    val turbulence: Float = 1f,
    val flowSpeed: Float = 1f,
    val bassPressure: Float = 1f,
    val softness: Float = 1f,
    val glow: Float = 1f,
    val saturation: Float = 1f,
    val plumeSize: Float = 1f,
    val audioSensitivity: Float = 1f
)

fun DrawScope.drawSynthSmokeScene(
    features: AudioFeatures,
    palette: HudPalette,
    phase: Float,
    playing: Boolean,
    clearBackdrop: Boolean,
    paletteMode: VizPaletteMode,
    quality: VizQualityTier,
    sat: Float,
    glow: Float,
    smokePreset: SmokeColorPreset = SmokeColorPreset.NEON_VAPOR,
    controls: SmokeControls = SmokeControls()
) {
    if (!clearBackdrop) drawRect(Color.Black)

    // Idle still-frames must read as vapor even without FFT (acceptance: screenshot look).
    val drive = if (playing) 1f else 0.72f
    val sens = controls.audioSensitivity.coerceIn(0.3f, 2f)
    val sub = features.subBass * sens
    val bass = features.bass * sens
    val lowMid = features.lowMid * sens
    val upperMid = features.upperMid * sens
    val treble = features.treble * sens
    val onset = features.onset * sens
    val flux = features.spectralFlux * sens
    // Ambient floor so silence still billows gently.
    val ambient = if (playing) 0.12f else 0.28f

    val t = phase * 0.0012f * controls.flowSpeed.coerceIn(0.2f, 2.5f)
    // Keep cell counts modest — soft radial plumes read as smoke without line geometry.
    val layers = (4 + (quality.meshScale * 2f).toInt()).coerceIn(4, 7)
    val dens = controls.density.coerceIn(0.4f, 1.6f)
    val cellsX = (12 * quality.meshScale * dens).toInt().coerceIn(10, 18)
    val cellsY = (8 * quality.meshScale * dens).toInt().coerceIn(6, 12)
    val baseR = min(size.width, size.height) *
        (0.16f + controls.plumeSize.coerceIn(0.4f, 1.8f) * 0.12f) *
        (1f + sub * 0.35f * controls.bassPressure + ambient * 0.35f)

    // Deep atmospheric wash so the field reads as volume, not strokes.
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                smokeColor(0.05f, smokePreset, paletteMode, palette, sat * controls.saturation, glow * controls.glow)
                    .copy(alpha = 0.14f + bass * 0.08f + ambient * 0.1f),
                Color.Transparent,
                smokeColor(0.55f, smokePreset, paletteMode, palette, sat * controls.saturation, glow * controls.glow)
                    .copy(alpha = 0.12f + lowMid * 0.06f + ambient * 0.08f)
            )
        )
    )

    for (layer in 0 until layers) {
        val depth = layer / (layers - 1f).coerceAtLeast(1f)
        val layerScale = 0.65f + depth * 0.7f
        val layerAlpha = (0.16f + (1f - depth) * 0.22f + ambient * 0.12f) *
            drive * controls.density.coerceIn(0.4f, 1.6f)
        val flow = t * (0.55f + depth * 0.9f) + layer * 1.7f
        val warpAmt = (0.35f + controls.turbulence * 0.55f + upperMid * 0.45f + treble * 0.25f)
        val pressure = 1f + bass * 0.55f * controls.bassPressure + onset * 0.35f + flux * 0.2f

        for (iy in 0 until cellsY) {
            for (ix in 0 until cellsX) {
                val u = (ix + 0.5f) / cellsX
                val v = (iy + 0.5f) / cellsY
                // Domain warp — curl-ish displacement from multi-octave noise
                val n1 = fbm(u * 2.4f + flow * 0.7f, v * 2.1f - flow * 0.4f + layer * 0.3f, octaves = 4)
                val n2 = fbm(u * 1.8f - flow * 0.5f + 10f, v * 2.6f + flow * 0.55f, octaves = 4)
                val n3 = fbm(u * 3.2f + n1 * warpAmt, v * 3.0f + n2 * warpAmt + lowMid * 0.4f, octaves = 3)
                val density = ((n3 - 0.18f) * 1.45f).coerceIn(0f, 1f)
                if (density < 0.05f) continue

                val dx = (n1 - 0.5f) * size.width * 0.12f * warpAmt +
                    sin(v * PI.toFloat() * 2f + flow + bass * 2f) * size.width * 0.03f * pressure
                val dy = (n2 - 0.5f) * size.height * 0.14f * warpAmt -
                    sub * size.height * 0.04f * controls.bassPressure +
                    cos(u * PI.toFloat() * 2f + flow * 1.3f) * size.height * 0.025f * (1f + lowMid)

                // Fine treble wisps nudge individual cells
                val wisp = treble * 10f * sin(u * 40f + flow * 8f + n3 * 6f)
                val x = u * size.width + dx + wisp
                val y = v * size.height + dy +
                    sin(flow * 2f + u * 6f + layer) * size.height * 0.02f * (1f + upperMid)

                val radius = baseR * layerScale *
                    (0.55f + density * 0.9f) *
                    (0.85f + controls.softness * 0.25f) *
                    (1f + onset * 0.15f * (1f - depth))

                val hueT = (u * 0.35f + v * 0.25f + depth * 0.4f + n3 * 0.2f + phase * 0.00015f)
                val c = smokeColor(
                    hueT,
                    smokePreset,
                    paletteMode,
                    palette,
                    sat * controls.saturation,
                    glow * controls.glow
                )
                val a = (layerAlpha * density * (0.7f + ambient + features.rms * 0.35f))
                    .coerceIn(0.04f, 0.62f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c.copy(alpha = a),
                            c.copy(alpha = a * 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(x, y),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }
    }

    // Kick / flux shock ripples — soft rings, not hard geometry
    if (onset > 0.28f || flux > 0.35f) {
        val cx = size.width * (0.35f + bass * 0.3f)
        val cy = size.height * (0.55f - sub * 0.1f)
        val shock = min(size.width, size.height) * (0.15f + onset * 0.35f + flux * 0.2f)
        val sc = smokeColor(0.7f + onset * 0.1f, smokePreset, paletteMode, palette, sat, glow)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(sc.copy(alpha = 0.18f * onset), Color.Transparent)
            ),
            radius = shock,
            center = Offset(cx, cy)
        )
    }
}

private fun smokeColor(
    t: Float,
    preset: SmokeColorPreset,
    paletteMode: VizPaletteMode,
    palette: HudPalette,
    sat: Float,
    glow: Float
): Color {
    if (paletteMode == VizPaletteMode.THEME || paletteMode == VizPaletteMode.ALBUM_ART) {
        return TrancePalette.color(t, paletteMode, palette, sat, glow)
    }
    val u = ((t % 1f) + 1f) % 1f
    val s = sat.coerceIn(0.25f, 1.4f)
    val v = (0.75f * glow).coerceIn(0.25f, 1f)
    return when (preset) {
        SmokeColorPreset.NEON_VAPOR -> lerp(
            Color.hsv(185f, 0.85f * s, v),
            Color.hsv(300f, 0.9f * s, v),
            u
        )
        SmokeColorPreset.TOXIC_DREAMS -> lerp(
            Color.hsv(110f, 0.95f * s, v),
            Color.hsv(170f, 0.9f * s, v),
            u
        )
        SmokeColorPreset.HADES_ASH -> lerp(
            Color.hsv(0f, 0.9f * s, v),
            Color.hsv(280f, 0.75f * s, v * 0.85f),
            u
        )
        SmokeColorPreset.RAINBOW_ETHER -> Color.hsv(u * 360f, 0.9f * s, v)
        SmokeColorPreset.GHOST_SMOKE -> lerp(
            Color.hsv(210f, 0.35f * s, v),
            Color.hsv(270f, 0.25f * s, (v * 1.05f).coerceAtMost(1f)),
            u
        )
        SmokeColorPreset.BLACKLIGHT -> lerp(
            Color.hsv(265f, 0.95f * s, v),
            Color.hsv(320f, 0.9f * s, v),
            u
        )
    }
}

/** Value-noise fBm in 0..1 */
private fun fbm(x: Float, y: Float, octaves: Int): Float {
    var amp = 0.5f
    var freq = 1f
    var sum = 0f
    var norm = 0f
    repeat(octaves.coerceIn(1, 5)) {
        sum += amp * valueNoise(x * freq, y * freq)
        norm += amp
        amp *= 0.5f
        freq *= 2.03f
    }
    return if (norm > 0f) (sum / norm).coerceIn(0f, 1f) else 0f
}

private fun valueNoise(x: Float, y: Float): Float {
    val x0 = floor(x)
    val y0 = floor(y)
    val fx = x - x0
    val fy = y - y0
    val ux = fx * fx * (3f - 2f * fx)
    val uy = fy * fy * (3f - 2f * fy)
    val a = hash2(x0, y0)
    val b = hash2(x0 + 1f, y0)
    val c = hash2(x0, y0 + 1f)
    val d = hash2(x0 + 1f, y0 + 1f)
    val ab = a + (b - a) * ux
    val cd = c + (d - c) * ux
    return ab + (cd - ab) * uy
}

private fun hash2(x: Float, y: Float): Float {
    val n = sin(x * 127.1f + y * 311.7f) * 43758.5453f
    return n - floor(n)
}

private fun lerp(a: Color, b: Color, t: Float): Color {
    val u = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * u,
        green = a.green + (b.green - a.green) * u,
        blue = a.blue + (b.blue - a.blue) * u,
        alpha = a.alpha + (b.alpha - a.alpha) * u
    )
}
