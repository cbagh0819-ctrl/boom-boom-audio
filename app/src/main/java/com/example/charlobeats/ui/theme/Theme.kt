package com.example.charlobeats.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BoomBoomColorScheme = darkColorScheme(
    primary = PurpleBrand,
    onPrimary = TextWhite,
    primaryContainer = PurpleDark,
    onPrimaryContainer = PurpleNeon,
    secondary = CyanNeon,
    onSecondary = DarkCanvas,
    secondaryContainer = CyanGlow,
    onSecondaryContainer = CyanNeon,
    tertiary = PinkNeon,
    onTertiary = TextWhite,
    background = DarkCanvas,
    onBackground = TextWhite,
    surface = DarkCardBg,
    onSurface = TextWhite,
    surfaceVariant = DarkCardBgElevated,
    onSurfaceVariant = TextMuted,
    outline = CardBorder
)

@Composable
fun CharloBeatsTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = BoomBoomColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkCanvas.toArgb()
            window.navigationBarColor = DarkCanvas.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
