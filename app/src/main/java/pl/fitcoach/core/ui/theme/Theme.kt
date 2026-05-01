package pl.fitcoach.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = FitGreen,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = FitGreenContainer,
    onPrimaryContainer = FitGreen,
    secondary = FitOrange,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = FitOrangeContainer,
    onSecondaryContainer = FitOrange,
    tertiary = FitGray,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    background = FitBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    surface = FitSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    surfaceVariant = FitSurfaceVariant,
    onSurfaceVariant = FitGray,
    error = FitError,
    errorContainer = FitErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = FitGreenDark,
    onPrimary = FitGreenContainerDark,
    primaryContainer = FitGreenContainerDark,
    onPrimaryContainer = FitGreenDark,
    secondary = FitOrangeDark,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF3E2000),
    background = FitBackgroundDark,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    surface = FitSurfaceDark,
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    surfaceVariant = FitSurfaceVariantDark,
    onSurfaceVariant = FitGrayLight,
    error = FitErrorDark
)

@Composable
fun FitCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FitTypography,
        content = content
    )
}

object Spacing {
    val xs = 4
    val sm = 8
    val md = 16
    val lg = 24
    val xl = 32
    val xxl = 48
}
