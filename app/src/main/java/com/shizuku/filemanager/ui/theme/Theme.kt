package com.shizuku.filemanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val SuperBlackColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF1C1B1F),
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color(0xFF1C1B1F),
    surfaceContainerHighest = Color(0xFF2B2930),
    surfaceContainerLow = Color.Black,
    surfaceContainerLowest = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCAC4D0)
)

val FileManagerTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = Typography().headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Typography().titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.Bold),
    labelMedium = Typography().labelMedium.copy(fontWeight = FontWeight.Bold),
    labelSmall = Typography().labelSmall.copy(fontWeight = FontWeight.Bold)
)

val FileManagerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** Curated accent presets shown in Settings; 0 is the sentinel for "no override". */
val AccentColorPresets: List<Color> = listOf(
    Color(0xFF6750A4), // Material default purple
    Color(0xFF2196F3), // blue
    Color(0xFF009688), // teal
    Color(0xFF4CAF50), // green
    Color(0xFFFF9800), // orange
    Color(0xFFE91E63), // pink
    Color(0xFFF44336), // red
)

@Composable
fun FileManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    superBlack: Boolean = false,
    /** ARGB int from EnginePrefs.getAccentColor; 0 means fall back to dynamic/default behavior. */
    accentColorArgb: Int = 0,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        superBlack -> SuperBlackColorScheme
        accentColorArgb != 0 ->
            (if (darkTheme) darkColorScheme(primary = Color(accentColorArgb)) else lightColorScheme(primary = Color(accentColorArgb)))
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FileManagerTypography,
        shapes = FileManagerShapes,
        content = content
    )
}
