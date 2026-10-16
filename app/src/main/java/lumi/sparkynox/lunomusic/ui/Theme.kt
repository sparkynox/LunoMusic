/*
 * Theme setup follows the ArchiveTune approach (GPL-3.0, (c) Rukamori, github.com/rukamori):
 * dynamic material you colors, expressive motion scheme and the Poppins typography.
 */
package lumi.sparkynox.lunomusic.ui

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import lumi.sparkynox.lunomusic.ui.at.AppTypography
import lumi.sparkynox.lunomusic.util.Prefs

// settings flip these, the theme follows live
object ThemeState {
    var dynamic by mutableStateOf(Prefs.dynamicColor)
    var pureBlack by mutableStateOf(Prefs.pureBlack)
}

// used on android 11 and below, or when dynamic color is off
private val FallbackScheme = darkColorScheme(
    primary = Color(0xFFFFC857),
    onPrimary = Color(0xFF3F2D00),
    primaryContainer = Color(0xFF5A4200),
    onPrimaryContainer = Color(0xFFFFDF9E),
    secondary = Color(0xFFDDC38C),
    onSecondary = Color(0xFF3E2E04),
    secondaryContainer = Color(0xFF574519),
    onSecondaryContainer = Color(0xFFFADFA6),
    tertiary = Color(0xFFB8CEA2),
    onTertiary = Color(0xFF243515),
    tertiaryContainer = Color(0xFF3A4C2A),
    onTertiaryContainer = Color(0xFFD4EABD),
    background = Color(0xFF131312),
    onBackground = Color(0xFFE6E2DB),
    surface = Color(0xFF131312),
    onSurface = Color(0xFFE6E2DB),
    surfaceVariant = Color(0xFF4D4639),
    onSurfaceVariant = Color(0xFFD0C5B4),
    surfaceContainerLowest = Color(0xFF0E0E0D),
    surfaceContainerLow = Color(0xFF1C1B1A),
    surfaceContainer = Color(0xFF201F1E),
    surfaceContainerHigh = Color(0xFF2B2A28),
    surfaceContainerHighest = Color(0xFF363432),
    outline = Color(0xFF999080),
    outlineVariant = Color(0xFF4D4639),
    error = Color(0xFFFFB4AB),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LunoTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    var scheme: ColorScheme =
        if (ThemeState.dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(ctx) else FallbackScheme

    if (ThemeState.pureBlack) {
        scheme = scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF080808),
            surfaceContainer = Color(0xFF0E0E0E),
        )
    }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        typography = AppTypography,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content,
        )
    }
}
