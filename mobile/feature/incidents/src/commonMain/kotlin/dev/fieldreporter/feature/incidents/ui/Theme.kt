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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.fieldreporter.feature.incidents.resources.Res
import dev.fieldreporter.feature.incidents.resources.plus_jakarta_sans_bold
import dev.fieldreporter.feature.incidents.resources.plus_jakarta_sans_extrabold
import org.jetbrains.compose.resources.Font

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
    // Every role is set. One left out falls back to Material's purple baseline, which is how a
    // purple "Undo" ended up on the snackbar.
    tertiaryContainer = Color(0xFFEAF0FD),
    onTertiaryContainer = Color(0xFF1C3F9A),
    inverseSurface = Color(0xFF2A2F38),
    inverseOnSurface = Color(0xFFECEEF2),
    inversePrimary = Color(0xFFA9C2FF),
    surfaceTint = Color(0xFF2A56C6),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE1E5EA),
    scrim = Color(0xFF000000),
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
    tertiaryContainer = Color(0xFF1C2740),
    onTertiaryContainer = Color(0xFFC9D8FF),
    inverseSurface = Color(0xFFECEEF2),
    inverseOnSurface = Color(0xFF12161D),
    inversePrimary = Color(0xFF2A56C6),
    surfaceTint = Color(0xFFA9C2FF),
    surfaceBright = Color(0xFF222833),
    surfaceDim = Color(0xFF0A0C10),
    scrim = Color(0xFF000000),
)

/*
 * The display face: Plus Jakarta Sans, bundled in composeResources/font (SIL Open Font License,
 * mobile/licenses). Only the two weights the scale uses are shipped. A glyph it lacks, such as
 * Burmese or Thai in an Incident's title, falls back to the platform typeface.
 */
@Composable
private fun displayFontFamily() = FontFamily(
    Font(Res.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(Res.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

/*
 * The type scale, the one place a typeface, weight or tracking is set.
 *
 * Display, headline and `titleLarge` are the prominent text: screen and sheet titles, an
 * Incident's title, empty-state headings, headline numbers. They take the display face: extra
 * bold and slightly tightened for display and headline, bold for `titleLarge`. Everything a person reads at length or types into
 * (the smaller titles, body, labels, fields) stays on the platform typeface at Material 3's
 * sizes, so it reads as native and scales with the system font size. All sizes are in sp.
 */
@Composable
private fun appTypography(): Typography {
    val display = displayFontFamily()
    return remember(display) {
        val base = Typography()
        fun TextStyle.display(weight: FontWeight, tracking: Double) =
            copy(fontFamily = display, fontWeight = weight, letterSpacing = tracking.em)
        base.copy(
            displayLarge = base.displayLarge.display(FontWeight.ExtraBold, -0.025),
            displayMedium = base.displayMedium.display(FontWeight.ExtraBold, -0.025),
            displaySmall = base.displaySmall.display(FontWeight.ExtraBold, -0.02),
            headlineLarge = base.headlineLarge.display(FontWeight.ExtraBold, -0.015),
            headlineMedium = base.headlineMedium.display(FontWeight.ExtraBold, -0.01),
            headlineSmall = base.headlineSmall.display(FontWeight.ExtraBold, -0.01),
            titleLarge = base.titleLarge.display(FontWeight.Bold, 0.0),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            // bodyLarge/Medium/Small and labelMedium/Small are Material 3's, unchanged.
        )
    }
}

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
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = appTypography(), shapes = AppShapes, content = content)
    }
}
