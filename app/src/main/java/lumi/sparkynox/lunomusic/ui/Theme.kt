package lumi.sparkynox.lunomusic.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colors = darkColorScheme(
    primary = Color(0xFFFFB300),
    onPrimary = Color.Black,
    secondary = Color(0xFFFF5C7A),
    background = Color(0xFF0E0E10),
    surface = Color(0xFF17171A),
    onBackground = Color(0xFFECECEC),
    onSurface = Color(0xFFECECEC),
)

@Composable
fun LunoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
