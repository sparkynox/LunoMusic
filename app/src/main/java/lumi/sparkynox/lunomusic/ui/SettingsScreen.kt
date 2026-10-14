package lumi.sparkynox.lunomusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import lumi.sparkynox.lunomusic.AboutPage
import lumi.sparkynox.lunomusic.LogsPage
import lumi.sparkynox.lunomusic.MusicVM
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 190.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = vm::pop) { Text("Back") }
        }
        Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))

        Group("Account")
        ClickRow(
            if (vm.loggedIn) "Log out" else "Log in with Google",
            if (vm.loggedIn) "Your feed, likes and library are active" else "Personal feed, liked songs, library"
        ) { if (vm.loggedIn) vm.logout() else { vm.pop(); vm.showLogin = true } }

        Group("Playback")
        SwitchRow("Endless queue", "When the queue runs low, keep going with the YouTube Music radio of the current song", vm.endless) { vm.setEndless(it) }
        SwitchRow("Try yt-dlp first", "Off: BravePipe first, yt-dlp as fallback. A song that fails to play switches the order by itself", ytFirst) {
            ytFirst = it
            Prefs.ytdlpFirst = it
            Extractor.clearCache()
            AppLog.i("Settings", "yt-dlp first = $it")
        }

        Group("Downloads")
        ClickRow("Downloaded songs", "${Downloads.ids.size} songs, ${"%.1f".format(Downloads.totalBytes() / 1048576.0)} MB") {
            vm.pop(); vm.openDownloads()
        }
        ClickRow("Delete all downloads", "Frees the space, songs stay in your library online") { confirm = "downloads" }

        Group("Listening")
        ClickRow("Stats", "Your top songs, artists and listening time") { vm.push(StatsPage) }
        ClickRow("Clear listening stats", "Starts the stats from zero") { confirm = "stats" }
        ClickRow("Clear stream url cache", "Forces a fresh lookup for every song") {
            Extractor.clearCache()
        }

        Group("Troubleshooting")
        SwitchRow("Verbose logging", "Also records every network call and player state, handy when hunting a bug", verbose) {
            verbose = it
            Prefs.verboseLogs = it
            AppLog.i("Settings", "verbose logging = $it")
        }
        ClickRow("Logs", "Everything the app did, from the first line to the latest") { vm.push(LogsPage) }

        Group("About")
        ClickRow("About LunoMusic", "Version, credits and license") { vm.push(AboutPage) }
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
private fun Group(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun ClickRow(title: String, sub: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
