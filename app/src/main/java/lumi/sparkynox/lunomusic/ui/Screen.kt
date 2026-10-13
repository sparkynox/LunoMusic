package lumi.sparkynox.lunomusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import coil.compose.AsyncImage
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.extract.Track

@Composable
fun Screen(vm: MusicVM) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { vm.nowPlaying?.let { MiniPlayer(it, vm.playing, vm::toggle) } }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                singleLine = true,
                placeholder = { Text("Search songs") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search() }),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            vm.error?.let {
                Text(it, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(16.dp))
            }
            LazyColumn {
                itemsIndexed(vm.results) { i, t -> TrackRow(t) { vm.play(i) } }
            }
        }
    }
}

@Composable
private fun TrackRow(t: Track, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = t.thumb,
            contentDescription = null,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp))
        )
        Column(Modifier.weight(1f)) {
            Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (t.seconds > 0) Text("%d:%02d".format(t.seconds / 60, t.seconds % 60), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MiniPlayer(meta: MediaMetadata, playing: Boolean, onToggle: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = meta.artworkUri,
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
            )
            Column(Modifier.weight(1f)) {
                Text(meta.title?.toString() ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    meta.artist?.toString() ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onToggle) { Text(if (playing) "Pause" else "Play") }
        }
    }
}
