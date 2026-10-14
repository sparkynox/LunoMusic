package lumi.sparkynox.lunomusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import lumi.sparkynox.lunomusic.AboutPage
import lumi.sparkynox.lunomusic.ArtistPage
import lumi.sparkynox.lunomusic.DetailPage
import lumi.sparkynox.lunomusic.LogsPage
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.SettingsPage
import lumi.sparkynox.lunomusic.StatsPage
import lumi.sparkynox.lunomusic.data.Downloads

@Composable
fun PageHost(vm: MusicVM) {
    val page = vm.pages.lastOrNull() ?: return
    BackHandler { vm.pop() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (page) {
            is DetailPage -> DetailScreen(vm, page)
            is ArtistPage -> ArtistScreen(vm, page)
            is SettingsPage -> SettingsScreen(vm)
            is LogsPage -> LogsScreen(vm)
            is StatsPage -> StatsScreen(vm)
            is AboutPage -> AboutScreen(onBack = vm::pop)
        }
    }
}

@Composable
private fun BackRow(vm: MusicVM) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        TextButton(onClick = vm::pop) { Text("Back") }
    }
}

@Composable
private fun DetailScreen(vm: MusicVM, page: DetailPage) {
    // downloads page follows the files on disk
    val tracks = if (page.isDownloads) page.tracks.filter { it.id in Downloads.ids } else page.tracks

    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPad) {
        item { BackRow(vm) }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val art = page.thumb ?: tracks.firstOrNull()?.thumb
                if (art != null) {
                    AsyncImage(
                        model = art,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(24.dp))
                    )
                }
                Text(page.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
                Text(
                    page.subtitle, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tracks.isNotEmpty()) {
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.playTracks(tracks, 0) }) { Text("Play") }
                        OutlinedButton(onClick = { vm.playTracks(tracks, 0, shuffled = true) }) { Text("Shuffle") }
                        if (!page.isDownloads) OutlinedButton(onClick = { Downloads.startAll(tracks) }) { Text("Download all") }
                    }
                }
            }
        }
        if (page.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 16.dp)) }
        page.error?.let { item { Text(it, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(16.dp)) } }
        if (!page.loading && tracks.isEmpty() && page.error == null) {
            item { Text("Nothing here yet", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp)) }
        }
        item { Box(Modifier.height(8.dp)) }
        itemsIndexed(tracks) { i, t -> TrackRow(vm, t) { vm.playTracks(tracks, i) } }
    }
}

@Composable
private fun ArtistScreen(vm: MusicVM, page: ArtistPage) {
    var showAll by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPad) {
        item { BackRow(vm) }
        item {
            Box(Modifier.fillMaxWidth().height(260.dp)) {
                AsyncImage(
                    model = page.thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0E0E10)))
                    )
                )
                Text(
                    page.name,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                )
            }
        }
        if (page.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        page.error?.let { item { Text(it, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(16.dp)) } }

        if (page.songs.isNotEmpty()) {
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.playTracks(page.songs, 0) }) { Text("Play") }
                    OutlinedButton(onClick = { vm.playTracks(page.songs, 0, shuffled = true) }) { Text("Shuffle") }
                }
            }
            item { SectionHeader("Songs") }
            itemsIndexed(if (showAll) page.songs else page.songs.take(5)) { i, t ->
                TrackRow(vm, t) { vm.playTracks(page.songs, i) }
            }
            if (page.songs.size > 5) {
                item { TextButton(onClick = { showAll = !showAll }, modifier = Modifier.padding(horizontal = 8.dp)) { Text(if (showAll) "Show less" else "Show all") } }
            }
        }

        page.description?.let {
            item {
                Text(
                    it, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        itemsIndexed(page.sections) { _, sec -> CarouselSection(vm, sec) }
    }
}
