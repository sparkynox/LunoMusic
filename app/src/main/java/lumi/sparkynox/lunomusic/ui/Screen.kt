package lumi.sparkynox.lunomusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import coil.compose.AsyncImage
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.extract.HomeItem
import lumi.sparkynox.lunomusic.extract.Kind
import lumi.sparkynox.lunomusic.extract.Track

@Composable
fun Screen(vm: MusicVM) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                vm.nowPlaying?.let { MiniPlayer(it, vm.playing, vm::toggle) }
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(selected = vm.tab == 0, onClick = { vm.tab = 0 }, icon = {}, label = { Text("Home") })
                    NavigationBarItem(selected = vm.tab == 1, onClick = { vm.tab = 1 }, icon = {}, label = { Text("Search") })
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (vm.tab == 0) HomeTab(vm) else SearchTab(vm)
        }
    }
}

@Composable
private fun HomeTab(vm: MusicVM) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            if (vm.loggedIn) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your feed", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::logout) { Text("Log out") }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Log in with Google to get your own feed")
                        Button(onClick = { vm.showLogin = true }) { Text("Log in") }
                    }
                }
            }
        }

        if (vm.homeLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        vm.error?.let {
            item { Text(it, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(16.dp)) }
        }

        items(vm.home) { sec ->
            Text(
                sec.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sec.items) { item -> ItemCard(item) { vm.playItem(sec, item) } }
            }
        }

        if (vm.homeCont != null && !vm.homeLoading) {
            item {
                TextButton(onClick = { vm.loadHome(more = true) }, modifier = Modifier.padding(16.dp)) {
                    Text("Load more")
                }
            }
        }
    }
}

@Composable
private fun ItemCard(item: HomeItem, onClick: () -> Unit) {
    Column(Modifier.width(140.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AsyncImage(
            model = item.thumb,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(140.dp).clip(if (item.kind == Kind.ARTIST) CircleShape else RoundedCornerShape(8.dp))
        )
        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SearchTab(vm: MusicVM) {
    Column(Modifier.fillMaxSize()) {
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
