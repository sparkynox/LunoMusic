package lumi.sparkynox.lunomusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import coil.compose.AsyncImage
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.extract.HomeItem
import lumi.sparkynox.lunomusic.extract.HomeSection
import lumi.sparkynox.lunomusic.extract.Kind
import lumi.sparkynox.lunomusic.extract.Track

@Composable
fun Screen(vm: MusicVM) {
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column {
                    vm.nowPlaying?.let { MiniPlayer(it, vm.playing, vm::toggle) { vm.showPlayer = true } }
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

        if (vm.showPlayer && vm.nowPlaying != null) NowPlaying(vm)
    }
}

@Composable
private fun HomeTab(vm: MusicVM) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Luno",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { vm.showAbout = true }) { Text("About") }
                if (vm.loggedIn) {
                    TextButton(onClick = vm::logout) { Text("Log out") }
                } else {
                    TextButton(onClick = { vm.showLogin = true }) { Text("Log in") }
                }
            }
        }

        if (!vm.loggedIn) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Log in with Google to get your own feed")
                        Button(onClick = { vm.showLogin = true }) { Text("Log in") }
                    }
                }
            }
        }

        if (vm.chips.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vm.chips) { c ->
                        FilterChip(selected = vm.chip == c, onClick = { vm.selectChip(c) }, label = { Text(c.title) })
                    }
                }
            }
        }

        if (vm.homeLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        vm.error?.let {
            item { Text(it, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(16.dp)) }
        }

        items(vm.home) { sec ->
            if (sec.items.size >= 3 && sec.items.all { it.kind == Kind.SONG }) SongListSection(vm, sec)
            else CarouselSection(vm, sec)
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
private fun SectionHeader(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
private fun CarouselSection(vm: MusicVM, sec: HomeSection) {
    SectionHeader(sec.title)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sec.items) { item -> ItemCard(item) { vm.playItem(sec, item) } }
    }
}

@Composable
private fun SongListSection(vm: MusicVM, sec: HomeSection) {
    SectionHeader(sec.title) {
        OutlinedButton(onClick = { vm.playSection(sec) }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)) {
            Text("Play all")
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            sec.items.take(6).forEach { item ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.playItem(sec, item) }.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = item.thumb,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                    )
                    Column(Modifier.weight(1f)) {
                        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(item: HomeItem, onClick: () -> Unit) {
    Column(Modifier.width(150.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AsyncImage(
            model = item.thumb,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(150.dp).clip(if (item.kind == Kind.ARTIST) CircleShape else RoundedCornerShape(16.dp))
        )
        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
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
            shape = RoundedCornerShape(24.dp),
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
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
        )
        Column(Modifier.weight(1f)) {
            Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        }
        if (t.seconds > 0) Text("%d:%02d".format(t.seconds / 60, t.seconds % 60), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MiniPlayer(meta: MediaMetadata, playing: Boolean, onToggle: () -> Unit, onOpen: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp, modifier = Modifier.clickable(onClick = onOpen)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = meta.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
            )
            Column(Modifier.weight(1f)) {
                Text(meta.title?.toString() ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(meta.artist?.toString() ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onToggle) {
                PlayPauseIcon(playing, 28.dp, MaterialTheme.colorScheme.primary)
            }
        }
    }
}
