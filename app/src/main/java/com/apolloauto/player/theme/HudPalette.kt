package com.apolloauto.player.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class HudThemeId(val label: String, val subtitle: String) {
    NEON_GRID("Neon Grid", "Classic cyan / magenta cyber"),
    HADES_WATCH("Hades Watch", "Crimson, gold, underworld HUD"),
    TOXIC_ACID("Toxic Acid", "Lime corrosion / blackout"),
    ICE_PROTOCOL("Ice Protocol", "Arctic cyan / steel"),
    VOID_ROSE("Void Rose", "Hot pink / deep violet night")
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
    val glowB: Color
)

val NeonGridPalette = HudPalette(
    id = HudThemeId.NEON_GRID,
    voidBlack = Color(0xFF05070D),
    panel = Color(0xFF0B1220),
    grid = Color(0x3322F7FF),
    primary = Color(0xFF00F0FF),
    secondary = Color(0xFFFF2D95),
    accent = Color(0xFFB6FF3B),
    soft = Color(0xFFE8F7FF),
    dim = Color(0xFF7FA3B8),
    danger = Color(0xFFFF4D4D),
    glowA = Color(0xFFFF2D95),
    glowB = Color(0xFF00F0FF)
)

val HadesWatchPalette = HudPalette(
    id = HudThemeId.HADES_WATCH,
    voidBlack = Color(0xFF0A0608),
    panel = Color(0xFF1A0D12),
    grid = Color(0x33FFB703),
    primary = Color(0xFFFFB703),
    secondary = Color(0xFFE63946),
    accent = Color(0xFFF4A261),
    soft = Color(0xFFFFF1D6),
    dim = Color(0xFFB08968),
    danger = Color(0xFFFF2E2E),
    glowA = Color(0xFFE63946),
    glowB = Color(0xFFFFB703)
)

val ToxicAcidPalette = HudPalette(
    id = HudThemeId.TOXIC_ACID,
    voidBlack = Color(0xFF040805),
    panel = Color(0xFF0C1610),
    grid = Color(0x334CFF00),
    primary = Color(0xFF7CFF00),
    secondary = Color(0xFFC8FF3D),
    accent = Color(0xFF39FF14),
    soft = Color(0xFFEFFFF0),
    dim = Color(0xFF7FA88A),
    danger = Color(0xFFFF5A36),
    glowA = Color(0xFF7CFF00),
    glowB = Color(0xFF39FF14)
)

val IceProtocolPalette = HudPalette(
    id = HudThemeId.ICE_PROTOCOL,
    voidBlack = Color(0xFF04080F),
    panel = Color(0xFF0A1524),
    grid = Color(0x3348CAE4),
    primary = Color(0xFF48CAE4),
    secondary = Color(0xFF90E0EF),
    accent = Color(0xFFADE8F4),
    soft = Color(0xFFF0FAFF),
    dim = Color(0xFF7B9BB0),
    danger = Color(0xFFFF6B6B),
    glowA = Color(0xFF00B4D8),
    glowB = Color(0xFFCAF0F8)
)

val VoidRosePalette = HudPalette(
    id = HudThemeId.VOID_ROSE,
    voidBlack = Color(0xFF09050F),
    panel = Color(0xFF160B1F),
    grid = Color(0x33FF4D9D),
    primary = Color(0xFFFF4D9D),
    secondary = Color(0xFFC77DFF),
    accent = Color(0xFFFF85C8),
    soft = Color(0xFFFFF0FA),
    dim = Color(0xFFB39BC7),
    danger = Color(0xFFFF5C5C),
    glowA = Color(0xFFFF4D9D),
    glowB = Color(0xFF9B5DE5)
)

fun paletteFor(id: HudThemeId): HudPalette = when (id) {
    HudThemeId.NEON_GRID -> NeonGridPalette
    HudThemeId.HADES_WATCH -> HadesWatchPalette
    HudThemeId.TOXIC_ACID -> ToxicAcidPalette
    HudThemeId.ICE_PROTOCOL -> IceProtocolPalette
    HudThemeId.VOID_ROSE -> VoidRosePalette
}

val LocalHudPalette = staticCompositionLocalOf { NeonGridPalette }

// Backward-compatible aliases used by older screens during migration
val VoidBlack get() = NeonGridPalette.voidBlack
val PanelInk get() = NeonGridPalette.panel
val GridLine get() = NeonGridPalette.grid
val NeonCyan get() = NeonGridPalette.primary
val NeonMagenta get() = NeonGridPalette.secondary
val NeonLime get() = NeonGridPalette.accent
val SoftWhite get() = NeonGridPalette.soft
val DimText get() = NeonGridPalette.dim
