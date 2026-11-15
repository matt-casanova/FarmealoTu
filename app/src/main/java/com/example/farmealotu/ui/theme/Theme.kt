package com.example.farmealotu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Esquema claro: el de siempre. Es el que se usa a pleno sol, con el teléfono en modo claro.
 * Sin color dinámico: la identidad (verde de cultivo, tierra, papel) no depende del fondo de pantalla.
 */
private val FarmLightScheme = lightColorScheme(
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
    surfaceContainer = Color(0xFFECEFE3),
    surfaceContainerHigh = CardSurfaceVariant,
    surfaceContainerHighest = Color(0xFFDFE3D2),
    outline = Outline,
    outlineVariant = OutlineSoft,
    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedContainer,
    onErrorContainer = Color(0xFF5C120D),
)

/**
 * Esquema oscuro diseñado (no invertido): carbón cálido con un matiz verde, los mismos tonos de
 * cultivo, tierra y ocre en versión nocturna. Texto principal ≈ 14:1 y secundario ≈ 10:1 sobre las
 * superficies; los contenedores de procedencia mantienen su par fondo/texto ≥ 7:1.
 */
private val FarmDarkScheme = darkColorScheme(
    primary = Color(0xFF9DD49A),
    onPrimary = Color(0xFF0E3A14),
    primaryContainer = Color(0xFF1F4A24),
    onPrimaryContainer = Color(0xFFC9EEC4),
    secondary = Color(0xFFE3BE9A),
    onSecondary = Color(0xFF3B2410),
    secondaryContainer = Color(0xFF55381D),
    onSecondaryContainer = Color(0xFFF6DEC6),
    tertiary = Color(0xFFF0C46C),
    onTertiary = Color(0xFF3D2800),
    tertiaryContainer = Color(0xFF5A4000),
    onTertiaryContainer = Color(0xFFFFE6B0),
    background = Color(0xFF121410),
    onBackground = Color(0xFFE6E3D8),
    surface = Color(0xFF1A1D17),
    onSurface = Color(0xFFE6E3D8),
    surfaceVariant = Color(0xFF2C3027),
    onSurfaceVariant = Color(0xFFC7C9BC),
    surfaceContainerLowest = Color(0xFF0E100C),
    surfaceContainerLow = Color(0xFF171A14),
    surfaceContainer = Color(0xFF1E211B),
    surfaceContainerHigh = Color(0xFF282C25),
    surfaceContainerHighest = Color(0xFF33372F),
    outline = Color(0xFF8F9384),
    outlineVariant = Color(0xFF444A3E),
    error = Color(0xFFFFB4A8),
    onError = Color(0xFF5C120D),
    errorContainer = Color(0xFF7A2119),
    onErrorContainer = Color(0xFFFFDAD4),
)

/** true cuando la app se dibuja con el esquema oscuro (los gráficos lo usan para ir sobre tarjeta clara). */
val LocalFarmDarkTheme = staticCompositionLocalOf { false }

/** Sigue el modo del teléfono: claro a pleno sol, oscuro de noche o si el agricultor lo eligió. */
@Composable
fun FarmealoTuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalFarmDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) FarmDarkScheme else FarmLightScheme,
            typography = Typography,
            content = content,
        )
    }
}
