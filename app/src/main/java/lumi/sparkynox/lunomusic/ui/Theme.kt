package lumi.sparkynox.lunomusic.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val colors = darkColorScheme(
    primary = Color(0xFFFFB300),
    onPrimary = Color.Black,
    secondary = Color(0xFFFF5C7A),
    secondaryContainer = Color(0xFF4A3A10),
    onSecondaryContainer = Color(0xFFFFE6A8),
    background = Color(0xFF0E0E10),
    surface = Color(0xFF17171A),
    surfaceVariant = Color(0xFF232327),
    onBackground = Color(0xFFECECEC),
    onSurface = Color(0xFFECECEC),
    onSurfaceVariant = Color(0xFFB5B5BA),
)

private val type = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun LunoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = type, content = content)
}
