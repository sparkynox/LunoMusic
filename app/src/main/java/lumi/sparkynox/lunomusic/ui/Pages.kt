/*
 * Hero headers follow ArchiveTune's album and artist screens (GPL-3.0, (c) Rukamori, github.com/rukamori)
 */
package lumi.sparkynox.lunomusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import lumi.sparkynox.lunomusic.R
import lumi.sparkynox.lunomusic.SettingsPage
import lumi.sparkynox.lunomusic.StatsPage
import lumi.sparkynox.lunomusic.data.Downloads

@Composable
fun PageHost(vm: MusicVM) {
    val page = vm.pages.lastOrNull()
    BackHandler(enabled = page != null) { vm.pop() }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (targetState == null) {
                fadeIn(tween(1)) togetherWith (slideOutHorizontally(tween(260)) { it / 5 } + fadeOut(tween(220)))
            } else {
                (slideInHorizontally(tween(300)) { it / 5 } + fadeIn(tween(300))) togetherWith fadeOut(tween(160))
            }
        },
        label = "pages"
    ) { p ->
        if (p == null) {
            Box(Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (p) {
                    is DetailPage -> DetailScreen(vm, p)
                    is ArtistPage -> ArtistScreen(vm, p)
                    is SettingsPage -> SettingsScreen(vm)
                    is LogsPage -> LogsScreen(vm)
                    is StatsPage -> StatsScreen(vm)
                    is AboutPage -> AboutScreen(onBack = vm::pop)
                }
            }
        }
    }
}

@Composable
private fun Hero(
    art: String?,
    height: androidx.compose.ui.unit.Dp,
    vm: MusicVM,
    title: String,
    subtitle: String,
    actions: @Composable () -> Unit,
) {
    val bg = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxWidth().height(height)) {
        AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.35f), 0.4f to Color.Transparent, 0.85f to bg.copy(alpha = 0.92f), 1f to bg)
            )
        )
        RoundIcon(
            R.drawable.arrow_back, vm::pop,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            container = Color.Black.copy(alpha = 0.35f), tint = Color.White
        )
        Column(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                actions()
            }
        }
    }
}

@Composable
private fun PlayPill(onClick: () -> Unit) {
    Button(onClick = onClick, shape = CircleShape, modifier = Modifier.height(52.dp)) {
        Icon(painterResource(R.drawable.play), null)
        Text("  Play", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DetailScreen(vm: MusicVM, page: DetailPage) {
    // downloads page follows the files on disk
    val tracks = if (page.isDownloads) page.tracks.filter { it.id in Downloads.ids } else page.tracks

    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPad) {
        item {
            Hero(
                art = page.thumb ?: tracks.firstOrNull()?.thumb,
                height = 380.dp, vm = vm, title = page.title,
                subtitle = listOf(page.subtitle, if (tracks.isNotEmpty()) "${tracks.size} songs" else "").filter { it.isNotBlank() }.joinToString(" • ")
            ) {
                if (tracks.isNotEmpty()) {
                    val c = MaterialTheme.colorScheme.surfaceContainerHigh
                    RoundIcon(R.drawable.shuffle, { vm.playTracks(tracks, 0, shuffled = true) }, size = 52.dp, iconSize = 22.dp, container = c)
                    PlayPill { vm.playTracks(tracks, 0) }
                    if (!page.isDownloads) RoundIcon(R.drawable.download, { Downloads.startAll(tracks) }, size = 52.dp, iconSize = 22.dp, container = c)
                }
            }
        }
        if (page.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp)) }
        page.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) } }
        if (!page.loading && tracks.isEmpty() && page.error == null) {
            item { Text("Nothing here yet", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp)) }
        }
        item { SectionHeader("Songs") }
        itemsIndexed(tracks) { i, t ->
            TrackRow(vm, t, number = if (page.numbered) i + 1 else null) { vm.playTracks(tracks, i) }
        }
    }
}

@Composable
private fun ArtistScreen(vm: MusicVM, page: ArtistPage) {
    var showAll by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPad) {
        item {
            Hero(
                art = page.thumb, height = 420.dp, vm = vm, title = page.name,
                subtitle = if (page.songs.isNotEmpty()) "${page.songs.size}+ songs" else ""
            ) {
                if (page.songs.isNotEmpty()) {
                    val c = MaterialTheme.colorScheme.surfaceContainerHigh
                    RoundIcon(R.drawable.shuffle, { vm.playTracks(page.songs, 0, shuffled = true) }, size = 52.dp, iconSize = 22.dp, container = c)
                    PlayPill { vm.playTracks(page.songs, 0) }
                    RoundIcon(R.drawable.radio, { vm.playRadio(page.songs.first().id) }, size = 52.dp, iconSize = 22.dp, container = c)
                }
            }
        }
        if (page.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp)) }
        page.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) } }

        if (page.songs.isNotEmpty()) {
            item { SectionHeader("Top songs") }
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
