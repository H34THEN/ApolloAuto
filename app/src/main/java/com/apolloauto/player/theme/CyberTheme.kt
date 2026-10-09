package com.apolloauto.player.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.apolloauto.player.ApolloApp

@Composable
fun ApolloCyberTheme(content: @Composable () -> Unit) {
    val themeId by ApolloApp.instance.themeStore.themeId.collectAsState()
    val palette = paletteFor(themeId)
    CompositionLocalProvider(LocalHudPalette provides palette) {
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
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
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
