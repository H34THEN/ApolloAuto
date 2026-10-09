package com.apolloauto.player.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.apolloauto.player.R

enum class HudFontOption(val label: String, val subtitle: String) {
    E1D1C_TEXT("E1D1C Text", "Baked-in display / HUD type"),
    KENNEY_FUTURE("Kenney Future", "Sci-Fi pack display"),
    KENNEY_FUTURE_NARROW("Kenney Future Narrow", "Compact HUD labels"),
    SYSTEM_SANS("System Sans", "Best Unicode metadata coverage"),
    SYSTEM_MONO("System Mono", "Technical terminal look")
}

val KenneyFuture = FontFamily(
    Font(R.font.kenney_future, FontWeight.Normal)
)

val KenneyFutureNarrow = FontFamily(
    Font(R.font.kenney_future_narrow, FontWeight.Normal)
)

val E1d1cText = FontFamily(
    Font(R.font.e1d1c_text_regular, FontWeight.Normal)
)

fun fontFamilyFor(option: HudFontOption): FontFamily = when (option) {
    HudFontOption.E1D1C_TEXT -> E1d1cText
    HudFontOption.KENNEY_FUTURE -> KenneyFuture
    HudFontOption.KENNEY_FUTURE_NARROW -> KenneyFutureNarrow
    HudFontOption.SYSTEM_SANS -> FontFamily.SansSerif
    HudFontOption.SYSTEM_MONO -> FontFamily.Monospace
}

/** Display / HUD chrome font (user-selected). */
val LocalHudFont = staticCompositionLocalOf { E1d1cText }

/** Metadata-safe body font (always readable Unicode). */
val LocalBodyFont = staticCompositionLocalOf { FontFamily.SansSerif }

@Composable
fun hudFont(): FontFamily = LocalHudFont.current

@Composable
fun bodyFont(): FontFamily = LocalBodyFont.current
