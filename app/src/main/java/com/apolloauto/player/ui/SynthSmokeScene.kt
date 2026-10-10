package com.apolloauto.player.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.apolloauto.player.media.AudioFeatures
import com.apolloauto.player.media.VisualIntensity
import com.apolloauto.player.media.VisualResponseMapper
import com.apolloauto.player.media.VizPaletteMode
import com.apolloauto.player.media.VizQualityTier
import com.apolloauto.player.theme.HudPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
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
    val bassPressure: Float = 1.25f,
    val softness: Float = 1f,
    val glow: Float = 1.15f,
    val saturation: Float = 1.2f,
    val plumeSize: Float = 1f,
    val audioSensitivity: Float = 1.25f,
    /** V5.2 — stretch density into dark voids vs luminous cores. */
    val densityContrast: Float = 1.35f,
    /** Beat-reactive chromatic shimmer strength (photosensitive-capped in renderer). */
    val chromaticVibration: Float = 1f
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

    val drive = if (playing) 1f else 0.55f
    val sens = controls.audioSensitivity.coerceIn(0.3f, 2.2f)
    val d = VisualResponseMapper.bandDrive(features, VisualIntensity.REACTIVE)
    val sub = d.sub * sens
    val bass = d.bass * sens
    val lowMid = d.lowMid * sens
    val upperMid = d.upperMid * sens
    val treble = d.treble * sens
    val onset = d.onset * sens
    val flux = d.flux * sens
    val kick = VisualResponseMapper.kickPressure(d) * sens * controls.bassPressure
    val chrom = controls.chromaticVibration.coerceIn(0f, 2f) *
        VisualResponseMapper.safePulse(onset, photosensitive = false)
    val ambient = if (playing) 0.06f else 0.18f
    val contrast = controls.densityContrast.coerceIn(0.6f, 2.2f)

    val t = phase * 0.0012f * controls.flowSpeed.coerceIn(0.2f, 2.5f) *
        (1f + kick * 0.15f) // accelerate flow on beats — do not reset noise coords
    val layers = (4 + (quality.meshScale * 2f).toInt()).coerceIn(4, 7)
    val dens = controls.density.coerceIn(0.4f, 1.6f)
    val cellsX = (12 * quality.meshScale * dens).toInt().coerceIn(10, 18)
    val cellsY = (8 * quality.meshScale * dens).toInt().coerceIn(6, 12)
    val baseR = min(size.width, size.height) *
        (0.14f + controls.plumeSize.coerceIn(0.4f, 1.8f) * 0.12f) *
        (1f + sub * 0.55f * controls.bassPressure + kick * 0.35f + ambient * 0.2f)

    // Thin atmospheric wash — keep black negative space (contrast).
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                smokeColor(0.05f + chrom * 0.04f, smokePreset, paletteMode, palette,
                    sat * controls.saturation * (1f + chrom * 0.25f), glow * controls.glow)
                    .copy(alpha = 0.08f + bass * 0.1f + ambient * 0.06f),
                Color.Transparent,
                smokeColor(0.55f, smokePreset, paletteMode, palette,
                    sat * controls.saturation, glow * controls.glow)
                    .copy(alpha = 0.06f + lowMid * 0.08f)
            )
        )
    )

    for (layer in 0 until layers) {
        val depth = layer / (layers - 1f).coerceAtLeast(1f)
        val layerScale = 0.65f + depth * 0.7f
        val layerAlpha = (0.12f + (1f - depth) * 0.28f) *
            drive * dens * (0.85f + kick * 0.35f)
        val flow = t * (0.55f + depth * 0.9f) + layer * 1.7f
        val warpAmt = (0.4f + controls.turbulence * 0.65f + upperMid * 0.7f + treble * 0.4f)
        val pressure = 1f + bass * 0.9f * controls.bassPressure + kick * 0.75f + flux * 0.35f

        for (iy in 0 until cellsY) {
            for (ix in 0 until cellsX) {
                val u = (ix + 0.5f) / cellsX
                val v = (iy + 0.5f) / cellsY
                val n1 = fbm(u * 2.4f + flow * 0.7f, v * 2.1f - flow * 0.4f + layer * 0.3f, octaves = 4)
                val n2 = fbm(u * 1.8f - flow * 0.5f + 10f, v * 2.6f + flow * 0.55f, octaves = 4)
                val n3 = fbm(u * 3.2f + n1 * warpAmt, v * 3.0f + n2 * warpAmt + lowMid * 0.55f, octaves = 3)
                // Contrast stretch: push mid densities apart → dark voids + bright cores.
                val raw = ((n3 - 0.22f) * 1.55f).coerceIn(0f, 1f)
                val density = raw.pow(1f / contrast).let { p ->
                    if (p < 0.12f) p * 0.35f else (0.12f + (p - 0.12f) * (1.15f + kick * 0.4f))
                }.coerceIn(0f, 1f)
                if (density < 0.08f) continue

                val dx = (n1 - 0.5f) * size.width * 0.16f * warpAmt +
                    sin(v * PI.toFloat() * 2f + flow + bass * 3f) * size.width * 0.06f * pressure
                val dy = (n2 - 0.5f) * size.height * 0.18f * warpAmt -
                    sub * size.height * 0.08f * controls.bassPressure -
                    kick * size.height * 0.05f * (1f - depth) +
                    cos(u * PI.toFloat() * 2f + flow * 1.3f) * size.height * 0.04f * (1f + lowMid)

                val wisp = treble * 18f * sin(u * 40f + flow * 8f + n3 * 6f)
                val x = u * size.width + dx + wisp
                val y = v * size.height + dy +
                    sin(flow * 2f + u * 6f + layer) * size.height * 0.035f * (1f + upperMid)

                val radius = baseR * layerScale *
                    (0.45f + density * 1.05f) *
                    (0.8f + controls.softness * 0.25f) *
                    (1f + onset * 0.28f * (1f - depth) + kick * 0.2f)

                val hueT = (u * 0.35f + v * 0.25f + depth * 0.4f + n3 * 0.2f +
                    phase * 0.00015f + chrom * 0.08f + kick * 0.03f)
                val localSat = sat * controls.saturation * (1f + chrom * 0.45f + density * 0.15f)
                val localGlow = glow * controls.glow * (1f + kick * 0.25f + density * 0.2f)
                val c = smokeColor(hueT, smokePreset, paletteMode, palette, localSat, localGlow)
                val a = (layerAlpha * density * (0.55f + d.rms * 0.45f + kick * 0.2f))
                    .coerceIn(0.06f, 0.78f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c.copy(alpha = a),
                            c.copy(alpha = a * 0.4f),
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

    // Kick pressure wave — soft luminous bloom, not a white flash
    if (kick > 0.4f || flux > 0.35f) {
        val cx = size.width * (0.4f + bass * 0.25f)
        val cy = size.height * (0.55f - sub * 0.12f)
        val shock = min(size.width, size.height) * (0.18f + kick * 0.4f + flux * 0.15f)
        val sc = smokeColor(
            0.65f + chrom * 0.1f, smokePreset, paletteMode, palette,
            sat * controls.saturation * 1.2f, glow * controls.glow
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(sc.copy(alpha = (0.12f + kick * 0.22f).coerceAtMost(0.4f)), Color.Transparent)
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
    // Color.hsv requires S,V in 0..1 — boost contrast via value, not illegal S>1.
    val s = sat.coerceIn(0.35f, 1.55f).coerceAtMost(1f)
    val v = (0.88f * glow * (0.85f + (sat - 1f).coerceAtLeast(0f) * 0.2f)).coerceIn(0.35f, 1f)
    fun hsv(h: Float, satAmt: Float, value: Float = v) =
        Color.hsv(((h % 360f) + 360f) % 360f, (satAmt * s).coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    return when (preset) {
        SmokeColorPreset.NEON_VAPOR -> lerp(hsv(185f, 0.95f), hsv(300f, 1f), u)
        SmokeColorPreset.TOXIC_DREAMS -> lerp(hsv(110f, 1f), hsv(175f, 0.95f), u)
        SmokeColorPreset.HADES_ASH -> lerp(hsv(5f, 1f), hsv(285f, 0.85f, v * 0.9f), u)
        SmokeColorPreset.RAINBOW_ETHER -> hsv(u * 360f, 1f)
        SmokeColorPreset.GHOST_SMOKE -> lerp(
            hsv(210f, 0.45f),
            hsv(270f, 0.35f, (v * 1.05f).coerceAtMost(1f)),
            u
        )
        SmokeColorPreset.BLACKLIGHT -> lerp(hsv(265f, 1f), hsv(325f, 0.95f), u)
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
