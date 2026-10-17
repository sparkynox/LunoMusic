package lumi.sparkynox.lunomusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.Notice
import lumi.sparkynox.lunomusic.R
import lumi.sparkynox.lunomusic.extract.HomeItem
import lumi.sparkynox.lunomusic.extract.Kind

// bottom bar for "removed" / "hidden" with an Undo button, and plain confirmations
@Composable
fun NoticeBar(vm: MusicVM, modifier: Modifier = Modifier) {
    val current = vm.notice
    // kept around so the text doesn't vanish while the bar slides out
    var last by remember { mutableStateOf<Notice?>(null) }
    LaunchedEffect(current) {
        if (current != null) {
            last = current
            delay(if (current.undo != null) 6000L else 3000L)
            if (vm.notice === current) vm.dismissNotice()
        }
    }

    AnimatedVisibility(
        visible = current != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        (current ?: last)?.let { n ->
            val scheme = MaterialTheme.colorScheme
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = scheme.inverseSurface,
                contentColor = scheme.inverseOnSurface,
                shadowElevation = 6.dp,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(start = 18.dp, end = if (n.undo != null) 6.dp else 18.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        n.message, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                    )
                    if (n.undo != null) {
                        TextButton(onClick = vm::runUndo) {
                            Text("Undo", color = scheme.inversePrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// pick one of your YouTube Music playlists for the song, or start a new one with it
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(vm: MusicVM) {
    val track = vm.playlistTarget ?: return
    var creating by remember(track) { mutableStateOf(false) }
    val lists = vm.libPlaylists.filter { it.kind == Kind.PLAYLIST && it.playlistId != null }

    ModalBottomSheet(onDismissRequest = vm::closeAddToPlaylist) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Add to playlist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
            )

            PlaylistRowShell(onClick = { creating = true }) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.add), null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text("New playlist", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }

            when {
                lists.isEmpty() && vm.libLoading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                }
                lists.isEmpty() -> Text(
                    "No playlists in your library yet. Make one above.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                else -> LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(lists) { p -> PlaylistPick(p) { vm.addToPlaylist(p) } }
                }
            }
        }
    }

    if (creating) NewPlaylistDialog(
        onDismiss = { creating = false },
        onCreate = { name -> creating = false; vm.createPlaylistWith(name) }
    )
}

@Composable
private fun PlaylistRowShell(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) { content() }
}

@Composable
private fun PlaylistPick(item: HomeItem, onClick: () -> Unit) {
    PlaylistRowShell(onClick) {
        AsyncImage(
            model = item.thumb, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
        )
        Column(Modifier.weight(1f)) {
            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (item.subtitle.isNotBlank()) Text(
                item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NewPlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(100) },
                singleLine = true, label = { Text("Name") }
            )
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
