package com.apolloauto.player.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class HudThemeId(val label: String, val subtitle: String) {
    APOLLO_PRIME("Apollo Prime", "Cyan / electric blue command"),
    HADES_WATCH("Hades Watch", "Crimson, charcoal, steel"),
    EIDOLON("Eidolon", "Violet / electric blue midnight"),
    NEON_REVENANT("Neon Revenant", "Magenta / purple / cyan"),
    GHOST_PROTOCOL("Ghost Protocol", "Tactical green / amber"),
    MONOCHROME_TERMINAL("Monochrome Terminal", "White / grayscale / black"),
    CUSTOM("Custom Theme", "User-defined accents")
}

enum class MotionProfile {
    FULL,
    REDUCED,
    STATIC,
    SYSTEM
}

@Immutable
data class HudPalette(
    val id: HudThemeId,
    val voidBlack: Color,
    val panel: Color,
    val grid: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val soft: Color,
    val dim: Color,
    val danger: Color,
    val glowA: Color,
    val glowB: Color,
    val glowIntensity: Float = 1f
)

val ApolloPrimePalette = HudPalette(
    id = HudThemeId.APOLLO_PRIME,
    voidBlack = Color(0xFF04080F),
    panel = Color(0xFF0A1424),
    grid = Color(0x3300E5FF),
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFF2F7BFF),
    accent = Color(0xFFE8F7FF),
    soft = Color(0xFFF4FBFF),
    dim = Color(0xFF7FA3B8),
    danger = Color(0xFFFF4D4D),
    glowA = Color(0xFF00E5FF),
    glowB = Color(0xFF2F7BFF)
)

val HadesWatchPalette = HudPalette(
    id = HudThemeId.HADES_WATCH,
    voidBlack = Color(0xFF0A0608),
    panel = Color(0xFF171012),
    grid = Color(0x33C1121F),
    primary = Color(0xFFC1121F),
    secondary = Color(0xFF8D99AE),
    accent = Color(0xFFB0B7C3),
    soft = Color(0xFFF2F2F2),
    dim = Color(0xFF8A8F98),
    danger = Color(0xFFFF2E2E),
    glowA = Color(0xFFC1121F),
    glowB = Color(0xFF6C757D)
)

val EidolonPalette = HudPalette(
    id = HudThemeId.EIDOLON,
    voidBlack = Color(0xFF070612),
    panel = Color(0xFF12102A),
    grid = Color(0x337B2CBF),
    primary = Color(0xFF9B5DE5),
    secondary = Color(0xFF3A86FF),
    accent = Color(0xFFBDE0FE),
    soft = Color(0xFFF3F0FF),
    dim = Color(0xFF9AA0C3),
    danger = Color(0xFFFF5C7A),
    glowA = Color(0xFF9B5DE5),
    glowB = Color(0xFF3A86FF)
)

val NeonRevenantPalette = HudPalette(
    id = HudThemeId.NEON_REVENANT,
    voidBlack = Color(0xFF08050F),
    panel = Color(0xFF160B22),
    grid = Color(0x33FF2D95),
    primary = Color(0xFFFF2D95),
    secondary = Color(0xFFC77DFF),
    accent = Color(0xFF00F0FF),
    soft = Color(0xFFFFF0FA),
    dim = Color(0xFFB39BC7),
    danger = Color(0xFFFF5C5C),
    glowA = Color(0xFFFF2D95),
    glowB = Color(0xFF00F0FF)
)

val GhostProtocolPalette = HudPalette(
    id = HudThemeId.GHOST_PROTOCOL,
    voidBlack = Color(0xFF050805),
    panel = Color(0xFF0F1610),
    grid = Color(0x334CFF00),
    primary = Color(0xFF70E000),
    secondary = Color(0xFFFFB703),
    accent = Color(0xFFB5E48C),
    soft = Color(0xFFEFFFEF),
    dim = Color(0xFF8FA88A),
    danger = Color(0xFFFF5A36),
    glowA = Color(0xFF70E000),
    glowB = Color(0xFFFFB703)
)

val MonochromeTerminalPalette = HudPalette(
    id = HudThemeId.MONOCHROME_TERMINAL,
    voidBlack = Color(0xFF000000),
    panel = Color(0xFF121212),
    grid = Color(0x33FFFFFF),
    primary = Color(0xFFFFFFFF),
    secondary = Color(0xFFB0B0B0),
    accent = Color(0xFFE0E0E0),
    soft = Color(0xFFF5F5F5),
    dim = Color(0xFF8A8A8A),
    danger = Color(0xFFCCCCCC),
    glowA = Color(0xFFFFFFFF),
    glowB = Color(0xFF9E9E9E)
)

fun builtInPalette(id: HudThemeId): HudPalette = when (id) {
    HudThemeId.APOLLO_PRIME -> ApolloPrimePalette
    HudThemeId.HADES_WATCH -> HadesWatchPalette
    HudThemeId.EIDOLON -> EidolonPalette
    HudThemeId.NEON_REVENANT -> NeonRevenantPalette
    HudThemeId.GHOST_PROTOCOL -> GhostProtocolPalette
    HudThemeId.MONOCHROME_TERMINAL -> MonochromeTerminalPalette
    HudThemeId.CUSTOM -> ApolloPrimePalette
}

fun paletteFor(id: HudThemeId, custom: HudPalette? = null, glow: Float = 1f): HudPalette {
    val base = if (id == HudThemeId.CUSTOM && custom != null) custom else builtInPalette(id)
    return base.copy(glowIntensity = glow.coerceIn(0.15f, 1.5f))
}

fun Color.toHexRgb(): String = String.format("#%06X", 0xFFFFFF and toArgb())

fun parseHexColor(raw: String, fallback: Color): Color {
    val cleaned = raw.trim().removePrefix("#")
    if (cleaned.length != 6) return fallback
    return runCatching { Color(android.graphics.Color.parseColor("#$cleaned")) }.getOrDefault(fallback)
}

val LocalHudPalette = staticCompositionLocalOf { ApolloPrimePalette }

// Legacy aliases (always Apollo Prime — prefer LocalHudPalette)
val VoidBlack get() = ApolloPrimePalette.voidBlack
val PanelInk get() = ApolloPrimePalette.panel
val GridLine get() = ApolloPrimePalette.grid
val NeonCyan get() = ApolloPrimePalette.primary
val NeonMagenta get() = ApolloPrimePalette.secondary
val NeonLime get() = ApolloPrimePalette.accent
val SoftWhite get() = ApolloPrimePalette.soft
val DimText get() = ApolloPrimePalette.dim
