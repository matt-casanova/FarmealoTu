package com.example.farmealotu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Un solo esquema claro, sin color dinámico: la app se usa en el campo, a pleno sol,
 * y el alto contraste importa más que seguir el tema del teléfono.
 */
private val FarmColorScheme = lightColorScheme(
    primary = FieldGreen,
    onPrimary = Color.White,
    primaryContainer = FieldGreenContainer,
    onPrimaryContainer = FieldGreenDark,
    secondary = Earth,
    onSecondary = Color.White,
    secondaryContainer = EarthContainer,
    onSecondaryContainer = Color(0xFF3B2410),
    tertiary = Clay,
    onTertiary = Color.White,
    tertiaryContainer = ClayContainer,
    onTertiaryContainer = Color(0xFF3D2800),
    background = Paper,
    onBackground = Ink,
    surface = CardSurface,
    onSurface = Ink,
    surfaceVariant = CardSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = CardSurface,
    surfaceContainer = Color(0xFFF3F0E6),
    surfaceContainerHigh = CardSurfaceVariant,
    surfaceContainerHighest = Color(0xFFE6E2D4),
    outline = Outline,
    outlineVariant = OutlineSoft,
    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedContainer,
    onErrorContainer = Color(0xFF5C120D),
)

@Composable
fun FarmealoTuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FarmColorScheme,
        typography = Typography,
        content = content,
    )
}
