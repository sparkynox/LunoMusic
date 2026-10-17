package lumi.sparkynox.lunomusic.ui

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.R

private const val REPO = "https://github.com/sparkynox/LunoMusic"

private class Credit(val name: String, val what: String, val url: String)

private val credits = listOf(
    Credit("ArchiveTune", "Theme, typography, icons, wavy slider, playback controls and the overall UI design", "https://github.com/rukamori/ArchiveTune"),
    Credit("EchoMusic", "Feature ideas and the reference for the YT Music login and home feed", "https://github.com/EchoMusicApp/Echo-Music"),
    Credit("BravePipe / NewPipe extractor", "Search and stream extraction", "https://github.com/bravepipeproject"),
    Credit("LRCLIB", "Synced lyrics", "https://lrclib.net"),
    Credit("yt-dlp", "Fallback stream extraction", "https://github.com/yt-dlp/yt-dlp"),
    Credit("Chaquopy", "Python runtime on Android", "https://chaquo.com/chaquopy/"),
    Credit("AndroidX Media3 and Jetpack Compose", "Playback and UI", "https://developer.android.com/"),
    Credit("Coil and OkHttp", "Images and networking", "https://github.com/coil-kt/coil"),
)

@Composable
fun AboutScreen(vm: MusicVM) {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val pkg = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }.getOrNull() }
    val version = pkg?.versionName ?: ""
    val build = pkg?.let { PackageInfoCompat.getLongVersionCode(it) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 200.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(R.drawable.arrow_back, vm::pop)
            Text("About", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 14.dp))
        }

        // hero
        Box(
            Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(36.dp)).background(
                Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), MaterialTheme.colorScheme.surfaceContainer))
            )
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(
                    painterResource(R.drawable.lm_logo), contentDescription = null,
                    modifier = Modifier.size(116.dp).clip(RoundedCornerShape(30.dp))
                )
                Text("LunoMusic", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "An open source YouTube Music client, no ads", style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(
                        "v$version" + if (build != null) "  •  build $build" else "",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { vm.checkUpdate(true) }, enabled = !vm.updateChecking) {
                        Icon(painterResource(R.drawable.update), null, modifier = Modifier.size(18.dp))
                        Text("  Check for updates")
                    }
                    OutlinedButton(onClick = { uri.openUri(REPO) }) { Text("GitHub") }
                }
                if (vm.updateStatus.isNotBlank()) {
                    Text(
                        vm.updateStatus, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                        color = if (vm.updateStatus.startsWith("Update available")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AboutGroup("Project") {
            AboutRow(R.drawable.link, "Source code", "github.com/sparkynox/LunoMusic") { uri.openUri(REPO) }
            AboutRow(R.drawable.info, "Report a problem", "Open an issue and attach the log from Settings > Logs") { uri.openUri("$REPO/issues") }
            AboutRow(R.drawable.info, "License", "GPL-3.0, free software") { uri.openUri("$REPO/blob/main/LICENSE") }
        }

        AboutGroup("Credits") {
            credits.forEach { c -> AboutRow(null, c.name, c.what) { uri.openUri(c.url) } }
        }

        AboutGroup("This device") {
            AboutRow(R.drawable.info, "Android ${Build.VERSION.RELEASE}", "API ${Build.VERSION.SDK_INT}, ${Build.SUPPORTED_ABIS.firstOrNull() ?: ""}") {}
            AboutRow(R.drawable.info, "${Build.MANUFACTURER} ${Build.MODEL}", "Include this when you report a bug") {}
        }

        Text(
            "Made by SparkyNox\nNot affiliated with YouTube or Google.",
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        )
    }
}

@Composable
private fun AboutGroup(title: String, content: @Composable () -> Unit) {
    Text(
        title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) { Column(Modifier.padding(vertical = 4.dp)) { content() } }
}

@Composable
private fun AboutRow(icon: Int?, title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
            } else {
                Text(title.take(1), color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
