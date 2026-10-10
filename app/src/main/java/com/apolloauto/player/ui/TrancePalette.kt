package com.apolloauto.player.ui

import androidx.compose.ui.graphics.Color
import com.apolloauto.player.media.VizPaletteMode
import com.apolloauto.player.theme.HudPalette
import kotlin.math.abs

object TrancePalette {
    fun color(
        t: Float,
        mode: VizPaletteMode,
        palette: HudPalette,
        saturation: Float = 1f,
        glow: Float = 1f,
        artPrimary: Color? = null,
        artSecondary: Color? = null
    ): Color {
        val sat = saturation.coerceIn(0.2f, 1.4f)
        val g = glow.coerceIn(0.2f, 1.6f)
        val base = when (mode) {
            VizPaletteMode.THEME -> lerp(palette.primary, palette.secondary, wrap01(t))
            VizPaletteMode.RAINBOW -> Color.hsv(wrap01(t) * 360f, 0.9f * sat, (0.85f * g).coerceIn(0.2f, 1f))
            VizPaletteMode.SPECTRAL -> Color.hsv(
                (wrap01(t) * 280f + 20f).coerceIn(0f, 360f),
                0.85f * sat,
                (0.9f * g).coerceIn(0.2f, 1f)
            )
            VizPaletteMode.ALBUM_ART -> {
                val a = artPrimary ?: palette.primary
                val b = artSecondary ?: palette.accent
                lerp(a, b, wrap01(t))
            }
            VizPaletteMode.SCENE -> Color.hsv(
                (wrap01(t) * 360f + 40f) % 360f,
                0.95f * sat,
                (0.95f * g).coerceIn(0.2f, 1f)
            )
        }
        return base.copy(
            red = (base.red * g).coerceIn(0f, 1f),
            green = (base.green * g).coerceIn(0f, 1f),
            blue = (base.blue * g).coerceIn(0f, 1f)
        )
    }

    fun spiralPreset(name: String, t: Float, palette: HudPalette): Color = when (name) {
        "cyber" -> lerp(Color(0xFF00F0FF), Color(0xFF7A5CFF), wrap01(t))
        "hades" -> lerp(Color(0xFFFF2244), Color(0xFF8B0000), wrap01(t))
        "ultraviolet" -> lerp(Color(0xFFB14EFF), Color(0xFF3D1B6E), wrap01(t))
        "acid" -> Color.hsv((wrap01(t) * 90f + 60f), 1f, 1f)
        else -> Color.hsv(wrap01(t) * 360f, 0.92f, 1f) // rainbow plasma
    }

    private fun wrap01(t: Float): Float {
        val v = t - kotlin.math.floor(t.toDouble()).toFloat()
        return if (v < 0f) v + 1f else v
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

    fun hash01(i: Int): Float = abs((i * 2654435761L).toInt() % 1000) / 1000f
}
