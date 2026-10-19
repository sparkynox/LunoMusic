package lumi.sparkynox.lunomusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

val NavigationBarHorizontalPadding = 12.dp
val NavigationBarMaxWidth = 420.dp
val NavigationBarHeight = 60.dp
val MiniPlayerHeight = 60.dp
val FloatingBarStandaloneCornerRadius = 22.dp
val FloatingBarOuterCornerRadius = 22.dp
val FloatingBarJunctionCornerRadius = 12.dp

// The app can run in a small floating window or in split screen, these say how much room the window really has.
// Configuration follows the window size, not the screen size.
@Composable
fun isTinyWidth() = LocalConfiguration.current.screenWidthDp < 340

@Composable
fun isShortHeight() = LocalConfiguration.current.screenHeightDp < 560

@Composable
fun isVeryShortHeight() = LocalConfiguration.current.screenHeightDp < 400
