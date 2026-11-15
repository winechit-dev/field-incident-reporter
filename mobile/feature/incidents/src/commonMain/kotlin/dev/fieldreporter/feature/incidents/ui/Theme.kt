package dev.fieldreporter.feature.incidents.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * The product's colours. The brand blue and the neutrals are the same values the dashboard uses
 * (admin/src/app/globals.css); see docs/design/design-system.md.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF2A56C6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE5FB),
    onPrimaryContainer = Color(0xFF0B2A6F),
    secondary = Color(0xFF586172),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEAF0FD),
    onSecondaryContainer = Color(0xFF1C3F9A),
    tertiary = Color(0xFF586172),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF12161D),
    surface = Color(0xFFF6F7F9),
    onSurface = Color(0xFF12161D),
    surfaceVariant = Color(0xFFE8EBF0),
    onSurfaceVariant = Color(0xFF586172),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEEF0F4),
    surfaceContainerHigh = Color(0xFFE8EBF0),
    surfaceContainerHighest = Color(0xFFE1E5EA),
    outline = Color(0xFF7A8496),
    outlineVariant = Color(0xFFD5DAE2),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE4E1),
    onErrorContainer = Color(0xFF7A1212),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C2FF),
    onPrimary = Color(0xFF0B2A6F),
    primaryContainer = Color(0xFF1C3F9A),
    onPrimaryContainer = Color(0xFFDCE5FB),
    secondary = Color(0xFFA0A9B8),
    onSecondary = Color(0xFF12161D),
    secondaryContainer = Color(0xFF1C2740),
    onSecondaryContainer = Color(0xFFC9D8FF),
    tertiary = Color(0xFFA0A9B8),
    onTertiary = Color(0xFF12161D),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFECEEF2),
    surface = Color(0xFF0E1116),
    onSurface = Color(0xFFECEEF2),
    surfaceVariant = Color(0xFF222833),
    onSurfaceVariant = Color(0xFFA0A9B8),
    surfaceContainerLowest = Color(0xFF0A0C10),
    surfaceContainerLow = Color(0xFF161A21),
    surfaceContainer = Color(0xFF1A1F27),
    surfaceContainerHigh = Color(0xFF1E232C),
    surfaceContainerHighest = Color(0xFF222833),
    outline = Color(0xFF7C8696),
    outlineVariant = Color(0xFF2E3542),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5F0F0F),
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/*
 * Material 3's scale on the platform typeface, with heavier titles and slightly tighter
 * headlines, so a screen's hierarchy reads from weight as well as size.
 */
private val Base = Typography()
private val AppTypography = Base.copy(
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

/** Rounder than Material's defaults: 12 dp for fields and thumbnails, 20 dp for cards, 28 dp for sheets. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Whether the dark scheme is showing. Status and Severity tones read it to pick their pair. */
val LocalDarkTheme = staticCompositionLocalOf { false }

/** The one theme. It follows the system's light or dark setting; there is no in-app switch. */
@Composable
fun FieldReporterTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
