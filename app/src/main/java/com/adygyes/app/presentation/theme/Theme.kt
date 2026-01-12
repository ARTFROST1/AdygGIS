package com.adygyes.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light color scheme for Adygyes app
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = SecondaryGold,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = TertiaryTeal,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    errorContainer = ErrorContainer,
    onError = OnError,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    inverseOnSurface = InverseOnSurface,
    inverseSurface = InverseSurface,
    inversePrimary = InversePrimary,
    surfaceTint = PrimaryGreen,
    outlineVariant = OutlineVariant,
    scrim = Scrim,
)

/**
 * Dark color scheme for Adygyes app
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreenLight,
    onPrimary = OnPrimaryContainer,
    primaryContainer = PrimaryGreenDark,
    onPrimaryContainer = PrimaryContainer,
    secondary = SecondaryGoldLight,
    onSecondary = OnSecondaryContainer,
    secondaryContainer = SecondaryGoldDark,
    onSecondaryContainer = SecondaryContainer,
    tertiary = TertiaryTeal,
    onTertiary = OnTertiaryContainer,
    tertiaryContainer = Color(0xFF005048),
    onTertiaryContainer = TertiaryContainer,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = Color(0xFF938F99),
    inverseOnSurface = InverseSurface,
    inverseSurface = OnSurfaceDark,
    inversePrimary = PrimaryGreen,
    surfaceTint = PrimaryGreenLight,
    outlineVariant = SurfaceVariantDark,
    scrim = Scrim,
)

/**
 * Main theme composable for Adygyes app
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to use dynamic color (Android 12+)
 * @param content The composable content
 */
@Composable
fun AdygyesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Dynamic color is available on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val useDarkIcons = !darkTheme
        
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = useDarkIcons
                isAppearanceLightNavigationBars = useDarkIcons
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
