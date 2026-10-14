package lumi.sparkynox.lunomusic.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.util.AppLog

@Composable
fun LogsScreen(vm: MusicVM) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var entries by remember { mutableStateOf(AppLog.snapshot()) }
    var minLevel by remember { mutableStateOf('D') }
    var text by remember { mutableStateOf("") }
    var follow by remember { mutableStateOf(true) }
    val state = rememberLazyListState()

    LaunchedEffect(Unit) {
        while (true) {
            entries = AppLog.snapshot()
            delay(1000)
        }
    }

    val order = "DIWE"
    val shown = entries.filter {
        order.indexOf(it.level) >= order.indexOf(minLevel) && (text.isBlank() || it.text.contains(text, ignoreCase = true))
    }

    // oldest entry on top, newest at the bottom, stays on the bottom while following
    LaunchedEffect(shown.size, follow) {
        if (follow && shown.isNotEmpty()) state.scrollToItem(shown.lastIndex)
    }

    Column(Modifier.fillMaxSize().padding(bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = vm::pop) { Text("Back") }
            Text("Logs", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("${shown.size}/${entries.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(AppLog.headerText() + "\n\n" + entries.joinToString("\n") { it.text }))
            }) { Text("Copy all") }
            TextButton(onClick = {
                val f = AppLog.export(ctx)
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ctx.startActivity(Intent.createChooser(send, "Share logs"))
            }) { Text("Share file") }
            TextButton(onClick = { AppLog.clear(); entries = AppLog.snapshot() }) { Text("Clear") }
            TextButton(onClick = { follow = !follow }) { Text(if (follow) "Following" else "Follow") }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf('D' to "All", 'I' to "Info", 'W' to "Warn", 'E' to "Error").forEach { (lvl, label) ->
                FilterChip(
                    selected = minLevel == lvl,
                    onClick = { minLevel = lvl },
                    label = { Text(label) },
                    shape = CircleShape,
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            placeholder = { Text("Filter (tag, song id, error...)") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp), state = state) {
            items(shown) { e ->
                Text(
                    e.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = when (e.level) {
                        'E' -> Color(0xFFFF6B6B)
                        'W' -> Color(0xFFFFB300)
                        'D' -> Color.White.copy(alpha = 0.5f)
                        else -> Color.White.copy(alpha = 0.9f)
                    },
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }
    }
}
