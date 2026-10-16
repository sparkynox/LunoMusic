package lumi.sparkynox.lunomusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class Credit(val name: String, val what: String, val url: String)

private val credits = listOf(
    Credit("EchoMusic", "Feature and UI inspiration, reference for the YT Music login and home feed", "https://github.com/EchoMusicApp/Echo-Music"),
    Credit("ArchiveTune", "Theme, typography, icons, wavy slider, playback controls and the overall UI design", "https://github.com/rukamori/ArchiveTune"),
    Credit("BravePipe / NewPipe extractor", "Search and stream extraction", "https://github.com/bravepipeproject"),
    Credit("LRCLIB", "Synced lyrics", "https://lrclib.net"),
    Credit("yt-dlp", "Fallback stream extraction", "https://github.com/yt-dlp/yt-dlp"),
    Credit("Chaquopy", "Python runtime on Android", "https://chaquo.com/chaquopy/"),
    Credit("AndroidX Media3, Jetpack Compose", "Playback and UI", "https://developer.android.com/"),
    Credit("Coil, OkHttp", "Images and networking", "https://github.com/coil-kt/coil"),
)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val version = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: ""

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row { TextButton(onClick = onBack) { Text("Back") } }
        Text("LunoMusic", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("v$version by SparkyNox", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Text("Credits", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        credits.forEach { c ->
            Column(Modifier.fillMaxWidth().clickable { uri.openUri(c.url) }.padding(vertical = 8.dp)) {
                Text(c.name, fontWeight = FontWeight.SemiBold)
                Text(c.what, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "The theme, typography, icons, wavy slider and animated playback controls are taken from ArchiveTune (GPL-3.0, by Rukamori). EchoMusic (GPL-3.0) inspired some features. Thank you to both developers.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Not affiliated with YouTube or Google. LunoMusic is free software under the GPL-3.0 license.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
