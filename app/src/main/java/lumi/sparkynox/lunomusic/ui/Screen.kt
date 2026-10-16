package lumi.sparkynox.lunomusic.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.R
import lumi.sparkynox.lunomusic.SettingsPage
import lumi.sparkynox.lunomusic.StatsPage
import lumi.sparkynox.lunomusic.data.Downloads
import lumi.sparkynox.lunomusic.data.Stats
import lumi.sparkynox.lunomusic.extract.HomeSection
import lumi.sparkynox.lunomusic.extract.Kind
import lumi.sparkynox.lunomusic.extract.SearchFilter
import lumi.sparkynox.lunomusic.extract.Track
import lumi.sparkynox.lunomusic.extract.asTrack

@Composable
fun Screen(vm: MusicVM) {
    LaunchedEffect(vm.tab, vm.loggedIn) {
        if (vm.tab == 2) vm.loadLibrary()
    }
    val hasMini = vm.nowPlaying != null
    val proximity by animateFloatAsState(if (hasMini) 1f else 0f, spring(stiffness = Spring.StiffnessLow), label = "barJunction")

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = vm.tab,
            transitionSpec = {
                (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 28 }) togetherWith fadeOut(tween(140))
            },
            label = "tabs"
        ) { tab ->
            when (tab) {
                0 -> HomeTab(vm)
                1 -> SearchTab(vm)
                else -> LibraryTab(vm)
            }
        }

        PageHost(vm)

        Column(
            Modifier.align(Alignment.BottomCenter)
                .padding(horizontal = NavigationBarHorizontalPadding)
                .padding(bottom = 10.dp)
        ) {
            AnimatedVisibility(
                visible = hasMini,
                enter = slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) { MiniPlayer(vm, proximity) }
            FloatingNav(tab = vm.tab, proximity = proximity, onSelect = { vm.pages = emptyList(); vm.tab = it })
        }

        AnimatedVisibility(
            visible = vm.showPlayer && hasMini,
            enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
            exit = slideOutVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) { it } + fadeOut()
        ) { NowPlaying(vm) }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    labelColor = MaterialTheme.colorScheme.onSurface,
    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
)

@Composable
private fun HomeTab(vm: MusicVM) {
    var menu by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPad) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(painterResource(R.drawable.music_note), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                Text("LunoMusic", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                RoundIcon(R.drawable.history, { vm.openHistory() })
                RoundIcon(R.drawable.stats, { vm.push(StatsPage) })
                RoundIcon(R.drawable.settings, { vm.push(SettingsPage) })
                Box {
                    RoundIcon(
                        R.drawable.person,
                        { if (vm.loggedIn) menu = true else vm.showLogin = true },
                        container = if (vm.loggedIn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        tint = if (vm.loggedIn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Log out") }, onClick = { menu = false; vm.logout() },
                            leadingIcon = { Icon(painterResource(R.drawable.logout), null) }
                        )
                    }
                }
            }
        }

        if (!vm.loggedIn) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Your own feed", style = MaterialTheme.typography.titleMedium)
                        Text("Log in with Google for personal mixes, liked songs and your library", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        if (vm.homeLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) }
        vm.error?.let {
            item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
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

// columns of four songs you swipe through
@Composable
private fun QuickPicks(vm: MusicVM, sec: HomeSection) {
    SectionHeader(sec.title) { TextButton(onClick = { vm.playSection(sec) }) { Text("Play all") } }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(sec.items.chunked(4)) { col ->
            Column(Modifier.fillParentMaxWidth(0.92f)) {
                col.forEach { item ->
                    val t = item.asTrack() ?: return@forEach
                    TrackRow(vm, t, horizontalPad = 8.dp) { vm.playItem(sec, item) }
                }
            }
        }
    }
}

@Composable
private fun SearchTab(vm: MusicVM) {
    Column(Modifier.fillMaxSize()) {
        TextField(
            value = vm.query,
            onValueChange = vm::onQuery,
            singleLine = true,
            shape = CircleShape,
            leadingIcon = { Icon(painterResource(R.drawable.search), null) },
            placeholder = { Text("Search YouTube Music") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
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
        if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
        vm.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        }

        if (vm.suggestions.isNotEmpty()) {
            LazyColumn(contentPadding = listPad) {
                items(vm.suggestions) { s ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).bounceClick { vm.search(s) }.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(painterResource(R.drawable.search), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Text(s, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(painterResource(R.drawable.arrow_top_left), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
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
    val top by produceState<List<Track>>(emptyList(), Downloads.ids.size) {
        value = withContext(Dispatchers.IO) { runCatching { Stats.topTracks(50) }.getOrDefault(emptyList()) }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPad) {
        item {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                RoundIcon(R.drawable.settings, { vm.push(SettingsPage) })
            }
        }

        if (!vm.loggedIn) {
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Log in to see your liked songs, history and playlists", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { vm.showLogin = true }) { Text("Log in") }
                    }
                }
            }
        }

        if (top.isNotEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            AsyncImage(
                                model = top.first().thumb, contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(20.dp))
                            )
                            Column {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                                    Row(Modifier.padding(horizontal = 10.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(painterResource(R.drawable.star), null, modifier = Modifier.size(12.dp))
                                        Text(" MOST PLAYED", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Text(top.first().title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                                Text("${top.size} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { vm.playTracks(top, 0) }, modifier = Modifier.weight(1f)) {
                                Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(20.dp))
                                Text("  Play all")
                            }
                            RoundIcon(R.drawable.shuffle, { vm.playTracks(top, 0, shuffled = true) }, size = 48.dp, container = MaterialTheme.colorScheme.surfaceContainerHighest)
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibTile(R.drawable.favorite, "Liked songs", "Your likes", MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), Modifier.weight(1f)) {
                        if (vm.loggedIn) vm.openLiked() else vm.showLogin = true
                    }
                    LibTile(R.drawable.offline, "Offline", "${Downloads.ids.size} downloaded", MaterialTheme.colorScheme.surfaceContainerHigh, Modifier.weight(1f)) { vm.openDownloads() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibTile(R.drawable.history, "History", "Recently played", MaterialTheme.colorScheme.surfaceContainerHigh, Modifier.weight(1f)) {
                        if (vm.loggedIn) vm.openHistory() else vm.showLogin = true
                    }
                    LibTile(R.drawable.trending_up, "My top 50", "All time", MaterialTheme.colorScheme.surfaceContainerHigh, Modifier.weight(1f)) { vm.openTop50() }
                }
            }
        }

        if (vm.libLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp)) }
        if (vm.libPlaylists.isNotEmpty()) {
            item { SectionHeader("Your playlists") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(vm.libPlaylists) { p -> ItemCard(p) { vm.openItem(p) } }
                }
            }
        }
    }
}

@Composable
private fun LibTile(icon: Int, title: String, sub: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(color = color, shape = RoundedCornerShape(28.dp), modifier = modifier.bounceClick(onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) { Icon(painterResource(icon), null, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Spacer(m: Modifier) = androidx.compose.foundation.layout.Spacer(m)
