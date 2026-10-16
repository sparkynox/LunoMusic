package lumi.sparkynox.lunomusic.ui

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lumi.sparkynox.lunomusic.AboutPage
import lumi.sparkynox.lunomusic.LogsPage
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.R
import lumi.sparkynox.lunomusic.StatsPage
import lumi.sparkynox.lunomusic.data.Downloads
import lumi.sparkynox.lunomusic.data.Stats
import lumi.sparkynox.lunomusic.extract.Extractor
import lumi.sparkynox.lunomusic.util.AppLog
import lumi.sparkynox.lunomusic.util.Prefs

@Composable
fun SettingsScreen(vm: MusicVM) {
    var ytFirst by remember { mutableStateOf(Prefs.ytdlpFirst) }
    var verbose by remember { mutableStateOf(Prefs.verboseLogs) }
    var confirm by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 200.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(R.drawable.arrow_back, vm::pop)
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 14.dp))
        }

        Group("Account") {
            ClickRow(
                R.drawable.account,
                if (vm.loggedIn) "Log out" else "Log in with Google",
                if (vm.loggedIn) "Your feed, likes and library are active" else "Personal feed, liked songs, library"
            ) { if (vm.loggedIn) vm.logout() else { vm.pop(); vm.showLogin = true } }
        }

        Group("Appearance") {
            SwitchRow(R.drawable.palette, "Dynamic colors", "Follow your wallpaper colors (Android 12+)", ThemeState.dynamic) {
                ThemeState.dynamic = it
                Prefs.dynamicColor = it
            }
            SwitchRow(R.drawable.contrast, "Pure black", "Black backgrounds, saves battery on OLED", ThemeState.pureBlack) {
                ThemeState.pureBlack = it
                Prefs.pureBlack = it
            }
        }

        Group("Playback") {
            SwitchRow(R.drawable.all_inclusive, "Endless queue", "When the queue runs low, keep going with the YouTube Music radio of the current song", vm.endless) { vm.setEndlessEnabled(it) }
            SwitchRow(R.drawable.speed, "Try yt-dlp first", "Off: BravePipe first, yt-dlp as fallback. A song that fails to play switches the order by itself", ytFirst) {
                ytFirst = it
                Prefs.ytdlpFirst = it
                Extractor.clearCache()
                AppLog.i("Settings", "yt-dlp first = $it")
            }
        }

        Group("Downloads") {
            ClickRow(R.drawable.offline, "Downloaded songs", "${Downloads.ids.size} songs, ${"%.1f".format(Downloads.totalBytes() / 1048576.0)} MB") {
                vm.pop(); vm.openDownloads()
            }
            ClickRow(R.drawable.delete, "Delete all downloads", "Frees the space, songs stay in your library online") { confirm = "downloads" }
        }

        Group("Listening") {
            ClickRow(R.drawable.stats, "Stats", "Your top songs, artists and listening time") { vm.push(StatsPage) }
            ClickRow(R.drawable.delete, "Clear listening stats", "Starts the stats from zero") { confirm = "stats" }
            ClickRow(R.drawable.sync, "Clear stream url cache", "Forces a fresh lookup for every song") { Extractor.clearCache() }
        }

        Group("Troubleshooting") {
            SwitchRow(R.drawable.error, "Verbose logging", "Also records every network call and player state, handy when hunting a bug", verbose) {
                verbose = it
                Prefs.verboseLogs = it
                AppLog.i("Settings", "verbose logging = $it")
            }
            ClickRow(R.drawable.list, "Logs", "Everything the app did, from the first line to the latest") { vm.push(LogsPage) }
        }

        Group("About") {
            ClickRow(R.drawable.info, "About LunoMusic", "Version, credits and license") { vm.push(AboutPage) }
        }
    }

    confirm?.let { what ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (what == "downloads") "Delete all downloads?" else "Clear listening stats?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    if (what == "downloads") Downloads.deleteAll() else Stats.clear()
                    confirm = null
                }) { Text("Yes") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(
        title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 22.dp, bottom = 8.dp)
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun IconBadge(icon: Int) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) { Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun ClickRow(icon: Int, title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconBadge(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(icon: Int, title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconBadge(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
