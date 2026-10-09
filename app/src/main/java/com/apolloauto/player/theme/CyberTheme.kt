package com.apolloauto.player.theme

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    val context = LocalContext.current

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

    CompositionLocalProvider(
        LocalHudPalette provides palette,
        LocalMotionProfile provides effectiveMotion
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
                displayLarge = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    letterSpacing = 2.sp,
                    color = palette.soft
                ),
                headlineMedium = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    letterSpacing = 1.5.sp,
                    color = palette.soft
                ),
                titleLarge = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp,
                    letterSpacing = 1.sp,
                    color = palette.soft
                ),
                bodyLarge = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = palette.soft
                ),
                labelLarge = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    color = palette.primary
                )
            ),
            content = content
        )
    }
}
