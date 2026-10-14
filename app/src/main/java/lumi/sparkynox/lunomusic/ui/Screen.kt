package lumi.sparkynox.lunomusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import coil.compose.AsyncImage
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.SettingsPage
import lumi.sparkynox.lunomusic.StatsPage
import lumi.sparkynox.lunomusic.extract.HomeSection
import lumi.sparkynox.lunomusic.extract.Kind
import lumi.sparkynox.lunomusic.extract.SearchFilter
import lumi.sparkynox.lunomusic.extract.asTrack

@Composable
fun Screen(vm: MusicVM) {
    LaunchedEffect(vm.tab, vm.loggedIn) {
        if (vm.tab == 2) vm.loadLibrary()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (vm.tab) {
            0 -> HomeTab(vm)
            1 -> SearchTab(vm)
            else -> LibraryTab(vm)
        }

        PageHost(vm)

        Column(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            vm.nowPlaying?.let { MiniPlayer(vm, it) }
            NavPill(vm)
        }

        if (vm.showPlayer && vm.nowPlaying != null) NowPlaying(vm)
    }
}

@Composable
private fun NavPill(vm: MusicVM) {
    fun go(i: Int) {
        vm.pages = emptyList()
        vm.tab = i
    }
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            NavItem(vm.tab == 0, "Home", { go(0) }) { Icon(Icons.Default.Home, null, tint = it) }
            NavItem(vm.tab == 1, "Search", { go(1) }) { Icon(Icons.Default.Search, null, tint = it) }
            NavItem(vm.tab == 2, "Library", { go(2) }) { GridIcon(22.dp, it) }
        }
    }
}

@Composable
private fun NavItem(selected: Boolean, label: String, onClick: () -> Unit, icon: @Composable (Color) -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.width(64.dp).height(32.dp).clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
            contentAlignment = Alignment.Center
        ) { icon(tint) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
private fun MiniPlayer(vm: MusicVM, meta: MediaMetadata) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable { vm.showPlayer = true }
    ) {
        Row(
            Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = meta.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(CircleShape)
            )
            Column(Modifier.weight(1f)) {
                Text(meta.title?.toString() ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(meta.artist?.toString() ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RoundBtn(40.dp, Color.White.copy(alpha = 0.08f), vm::previous) { SkipIcon(false, 20.dp, Color.White) }
            RoundBtn(48.dp, MaterialTheme.colorScheme.secondaryContainer, vm::toggle) {
                PlayPauseIcon(vm.playing, 26.dp, MaterialTheme.colorScheme.onSecondaryContainer)
            }
            RoundBtn(40.dp, Color.White.copy(alpha = 0.08f), vm::next) { SkipIcon(true, 20.dp, Color.White) }
        }
    }
}

@Composable
private fun HomeTab(vm: MusicVM) {
    var menu by remember { mutableStateOf(false) }
    val btn = MaterialTheme.colorScheme.surfaceVariant

    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPad) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Luno", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                RoundBtn(40.dp, btn, { vm.push(StatsPage) }) { BarsIcon(20.dp, MaterialTheme.colorScheme.onSurface) }
                RoundBtn(40.dp, btn, { vm.push(SettingsPage) }) {
                    Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
                Box {
                    RoundBtn(
                        40.dp,
                        if (vm.loggedIn) MaterialTheme.colorScheme.secondaryContainer else btn,
                        { if (vm.loggedIn) menu = true else vm.showLogin = true }
                    ) {
                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Log out") }, onClick = { menu = false; vm.logout() })
                    }
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
                        FilterChip(
                            selected = vm.chip == c,
                            onClick = { vm.selectChip(c) },
                            label = { Text(c.title) },
                            shape = CircleShape,
                            border = null,
                            colors = chipColors()
                        )
                    }
                }
            }
        }

        if (vm.homeLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        vm.error?.let {
            item { Text(it, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(16.dp)) }
        }

        items(vm.home) { sec ->
            if (sec.items.size >= 4 && sec.items.all { it.kind == Kind.SONG }) QuickPicks(vm, sec)
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
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
)

// columns of four songs you swipe through
@Composable
private fun QuickPicks(vm: MusicVM, sec: HomeSection) {
    SectionHeader(sec.title) { TextButton(onClick = { vm.playSection(sec) }) { Text("Play all") } }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(sec.items.chunked(4)) { col ->
            Column(Modifier.fillParentMaxWidth(0.9f)) {
                col.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.playItem(sec, item) }.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = item.thumb,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Column(Modifier.weight(1f)) {
                            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTab(vm: MusicVM) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = vm.query,
            onValueChange = vm::onQuery,
            singleLine = true,
            shape = CircleShape,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Search YouTube Music") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search() }),
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SearchFilter.entries.toList()) { f ->
                FilterChip(
                    selected = vm.filter == f,
                    onClick = { vm.selectFilter(f) },
                    label = { Text(f.label) },
                    shape = CircleShape,
                    border = null,
                    colors = chipColors()
                )
            }
        }
        if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        vm.error?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(16.dp))
        }

        if (vm.suggestions.isNotEmpty()) {
            LazyColumn(contentPadding = listPad) {
                items(vm.suggestions) { s ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.search(s) }.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Text(s, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        } else {
            LazyColumn(contentPadding = listPad) {
                items(vm.results) { item ->
                    val track = if (item.kind == Kind.SONG) item.asTrack() else null
                    if (track != null) TrackRow(vm, track) { vm.openResult(item) }
                    else ItemRow(item) { vm.openResult(item) }
                }
            }
        }
    }
}

@Composable
private fun LibraryTab(vm: MusicVM) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPad) {
        item { Text("Library", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)) }

        if (!vm.loggedIn) {
            item {
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Log in to see your liked songs, history and playlists")
                        Button(onClick = { vm.showLogin = true }) { Text("Log in") }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (vm.loggedIn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LibTile("Liked songs", "Your likes", Color(0xFF2E2224), Modifier.weight(1f)) { vm.openLiked() }
                        LibTile("History", "Recently played", MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f)) { vm.openHistory() }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibTile("Downloads", "Offline songs", MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f)) { vm.openDownloads() }
                    LibTile("My top 50", "Most played, all time", MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f)) { vm.openTop50() }
                }
            }
        }

        if (vm.libLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 16.dp)) }
        if (vm.libPlaylists.isNotEmpty()) {
            item { SectionHeader("Your playlists") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(vm.libPlaylists) { p -> ItemCard(p) { vm.openItem(p) } }
                }
            }
        }
    }
}

@Composable
private fun LibTile(title: String, sub: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(color = color, shape = RoundedCornerShape(24.dp), modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp).height(64.dp), verticalArrangement = Arrangement.Bottom) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
