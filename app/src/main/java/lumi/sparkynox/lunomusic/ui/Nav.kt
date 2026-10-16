/*
 * Adapted from ArchiveTune's FloatingNavigationToolbar (GPL-3.0, (c) Rukamori, github.com/rukamori)
 */
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package lumi.sparkynox.lunomusic.ui

import androidx.compose.animation.Crossfade
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import lumi.sparkynox.lunomusic.R

private class NavTab(val label: String, val off: Int, val on: Int)

private val tabs = listOf(
    NavTab("Home", R.drawable.home_outlined, R.drawable.home_filled),
    NavTab("Search", R.drawable.search, R.drawable.search),
    NavTab("Library", R.drawable.library_outlined, R.drawable.library_filled),
)

@Composable
fun FloatingNav(tab: Int, proximity: Float, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(
        topStart = lerp(FloatingBarStandaloneCornerRadius.value, FloatingBarJunctionCornerRadius.value, proximity).dp,
        topEnd = lerp(FloatingBarStandaloneCornerRadius.value, FloatingBarJunctionCornerRadius.value, proximity).dp,
        bottomStart = lerp(FloatingBarStandaloneCornerRadius.value, FloatingBarOuterCornerRadius.value, proximity).dp,
        bottomEnd = lerp(FloatingBarStandaloneCornerRadius.value, FloatingBarOuterCornerRadius.value, proximity).dp,
    )

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.widthIn(max = NavigationBarMaxWidth).fillMaxWidth().height(NavigationBarHeight),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = NavigationBarDefaults.Elevation,
            shadowElevation = NavigationBarDefaults.Elevation,
        ) {
            ShortNavigationBar(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                windowInsets = WindowInsets(0, 0, 0, 0),
                arrangement = ShortNavigationBarArrangement.EqualWeight,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Row(
                        Modifier.widthIn(max = 360.dp).fillMaxWidth().fillMaxHeight().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        tabs.forEachIndexed { i, t ->
                            ShortNavigationBarItem(
                                selected = tab == i,
                                onClick = { onSelect(i) },
                                modifier = Modifier.weight(1f),
                                icon = {
                                    Crossfade(targetState = tab == i, label = "navIcon") { sel ->
                                        Icon(painterResource(if (sel) t.on else t.off), contentDescription = null)
                                    }
                                },
                                label = { Text(t.label, maxLines = 1) },
                            )
                        }
                    }
                }
            }
        }
    }
}
