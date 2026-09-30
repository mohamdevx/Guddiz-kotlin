package com.guddiz.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Plum40, onPrimary = Color.White,
    primaryContainer = Plum90, onPrimaryContainer = Plum10,
    secondary = Rose40, onSecondary = Color.White,
    secondaryContainer = Rose90, onSecondaryContainer = Rose10,
    tertiary = Clay40, onTertiary = Color.White,
    tertiaryContainer = Clay90, onTertiaryContainer = Clay10,
    background = NeutralLightBg, onBackground = NeutralLightOn,
    surface = NeutralLightSurface, onSurface = NeutralLightOn,
    surfaceVariant = NeutralLightVariant, onSurfaceVariant = NeutralLightOnVariant,
    outline = NeutralLightOutline,
    outlineVariant = Color(0xFFD8C2C9),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Plum80, onPrimary = Plum10,
    primaryContainer = Color(0xFF6B1445), onPrimaryContainer = Plum90,
    secondary = Color(0xFFE0BDCB), onSecondary = Rose10,
    secondaryContainer = Color(0xFF593F4A), onSecondaryContainer = Rose90,
    tertiary = Color(0xFFF0BD93), onTertiary = Clay10,
    tertiaryContainer = Color(0xFF623E22), onTertiaryContainer = Clay90,
    background = NeutralDarkBg, onBackground = NeutralDarkOn,
    surface = NeutralDarkSurface, onSurface = NeutralDarkOn,
    surfaceVariant = NeutralDarkVariant, onSurfaceVariant = NeutralDarkOnVariant,
    outline = NeutralDarkOutline,
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun GuddizTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GuddizTypography,
        content = content
    )
}
