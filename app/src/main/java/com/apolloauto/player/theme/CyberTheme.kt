package com.apolloauto.player.theme

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.apolloauto.player.ApolloApp

val LocalMotionProfile = staticCompositionLocalOf { MotionProfile.FULL }

@Composable
fun ApolloCyberTheme(content: @Composable () -> Unit) {
    val themeStore = ApolloApp.instance.themeStore
    val themeId by themeStore.themeId.collectAsState()
    val custom by themeStore.custom.collectAsState()
    val glow by themeStore.glowIntensity.collectAsState()
    val motion by themeStore.motionProfile.collectAsState()
    val fontOption by themeStore.hudFont.collectAsState()
    val uiFontScale by themeStore.uiFontScale.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current

    val animatorScale = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }

    val palette = remember(themeId, custom, glow) {
        paletteFor(themeId, custom.toPalette(), glow)
    }
    val effectiveMotion = when (motion) {
        MotionProfile.SYSTEM -> if (animatorScale == 0f) MotionProfile.STATIC else MotionProfile.FULL
        else -> motion
    }
    // Selected HUD font applies app-wide (headers, body, buttons, rows).
    val appFont = remember(fontOption) { fontFamilyFor(fontOption) }
    val baseText = TextStyle(
        fontFamily = appFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = palette.soft
    )

    val scaledDensity = remember(density, uiFontScale) {
        Density(
            density = density.density,
            fontScale = density.fontScale * uiFontScale.coerceIn(0.8f, 1.45f)
        )
    }

    CompositionLocalProvider(
        LocalHudPalette provides palette,
        LocalMotionProfile provides effectiveMotion,
        LocalHudFont provides appFont,
        LocalBodyFont provides appFont,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = palette.primary,
                secondary = palette.secondary,
                tertiary = palette.accent,
                background = palette.voidBlack,
                surface = palette.panel,
                onPrimary = palette.voidBlack,
                onSecondary = palette.soft,
                onBackground = palette.soft,
                onSurface = palette.soft,
                error = palette.danger
            ),
            typography = MaterialTheme.typography.copy(
                displayLarge = baseText.copy(fontWeight = FontWeight.Bold, fontSize = 42.sp, letterSpacing = 2.sp),
                displayMedium = baseText.copy(fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = 1.5.sp),
                displaySmall = baseText.copy(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = 1.sp),
                headlineLarge = baseText.copy(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = 1.sp),
                headlineMedium = baseText.copy(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = 1.5.sp),
                headlineSmall = baseText.copy(fontWeight = FontWeight.Medium, fontSize = 20.sp, letterSpacing = 1.sp),
                titleLarge = baseText.copy(fontWeight = FontWeight.Medium, fontSize = 18.sp, letterSpacing = 1.sp),
                titleMedium = baseText.copy(fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = 0.5.sp),
                titleSmall = baseText.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.5.sp),
                bodyLarge = baseText.copy(fontSize = 16.sp),
                bodyMedium = baseText.copy(fontSize = 14.sp),
                bodySmall = baseText.copy(fontSize = 12.sp, color = palette.dim),
                labelLarge = baseText.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.5.sp, color = palette.primary),
                labelMedium = baseText.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.sp, color = palette.primary),
                labelSmall = baseText.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.sp, color = palette.dim)
            )
        ) {
            ProvideTextStyle(baseText) {
                content()
            }
        }
    }
}
