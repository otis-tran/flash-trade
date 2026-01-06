package com.otistran.flash_trade.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =============================================================================
// Dark Color Scheme (Primary - Kyber Brand)
// =============================================================================
private val DarkColorScheme = darkColorScheme(
    primary = KyberTeal,
    onPrimary = OnKyberTeal,
    primaryContainer = KyberTealContainer,
    onPrimaryContainer = KyberTealLight,
    secondary = KyberPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2D2650),
    onSecondaryContainer = Color(0xFFD4CBFF),
    tertiary = KyberBlue,
    onTertiary = Color.White,
    tertiaryContainer = InfoContainer,
    onTertiaryContainer = Color(0xFFB8D4FF),
    error = Error,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFFFFB4B4),
    background = KyberNavy,
    onBackground = TextPrimary,
    surface = KyberNavyLight,
    onSurface = TextPrimary,
    surfaceVariant = KyberNavySurface,
    onSurfaceVariant = TextSecondary,
    outline = KyberNavyBorder,
    outlineVariant = Color(0xFF2D3548),
    inverseSurface = SurfaceLight,
    inverseOnSurface = TextLightPrimary,
    inversePrimary = KyberTealDark,
    surfaceTint = KyberTeal
)

// =============================================================================
// Light Color Scheme (Secondary - Fallback)
// =============================================================================
private val LightColorScheme = lightColorScheme(
    primary = KyberTealDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA8F5D9),
    onPrimaryContainer = Color(0xFF00331F),
    secondary = Color(0xFF5D4AB3),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E0FF),
    onSecondaryContainer = Color(0xFF1A0A4D),
    tertiary = Color(0xFF0066CC),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E8FF),
    onTertiaryContainer = Color(0xFF001A33),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFFE0E0),
    onErrorContainer = Color(0xFF410000),
    background = SurfaceLight,
    onBackground = TextLightPrimary,
    surface = Color.White,
    onSurface = TextLightPrimary,
    surfaceVariant = SurfaceLightVariant,
    onSurfaceVariant = TextLightSecondary,
    outline = Color(0xFF8E99A4),
    outlineVariant = Color(0xFFC4CDD9),
    inverseSurface = KyberNavy,
    inverseOnSurface = TextPrimary,
    inversePrimary = KyberTeal,
    surfaceTint = KyberTealDark
)

// =============================================================================
// Flash Trade Theme - Kyber Brand Colors
// =============================================================================
@Composable
fun FlashTradeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Use Kyber brand colors - no dynamic colors to maintain brand consistency
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Set status bar color to match theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
