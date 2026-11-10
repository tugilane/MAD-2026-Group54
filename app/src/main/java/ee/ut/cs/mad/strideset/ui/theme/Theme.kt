package ee.ut.cs.mad.strideset.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** Different colors for the app, based on the Figma design system. */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryActionColor,
    onPrimary = HomeActionTextColor,
    background = BackgroundColor,
    onBackground = PrimaryTextColor,
    surface = BackgroundColor,
    onSurface = PrimaryTextColor,
    surfaceVariant = SurfaceOrCardColor,
    surfaceContainer = SurfaceOrCardColor,
    surfaceContainerHigh = SurfaceOrCardColor,
    surfaceContainerHighest = SurfaceOrCardColor,
    onSurfaceVariant = SecondaryTextColor,
    outline = BorderOrDividerColor,
    outlineVariant = BorderOrDividerColor
)

@Composable
fun StrideSetTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        shapes = StrideSetShapes,
        typography = Typography,
        content = content
    )
}