package lumi.sparkynox.lunomusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.data.Downloads
import lumi.sparkynox.lunomusic.extract.Track

private fun fmt(ms: Long) = "%d:%02d".format(ms / 60000, (ms / 1000) % 60)

@Composable
fun NowPlaying(vm: MusicVM) {
    BackHandler { vm.showPlayer = false }

    val meta = vm.nowPlaying
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var drag by remember { mutableStateOf<Float?>(null) }
    var panel by remember { mutableIntStateOf(0) } // 0 art, 1 queue, 2 lyrics
    val accent = MaterialTheme.colorScheme.primary
    val lyricState = rememberLazyListState()
    val cur = vm.lyrics.indexOfLast { it.ms <= pos + 250 }.coerceAtLeast(0)

    val id = vm.queue.getOrNull(vm.queueIndex)?.mediaId
    val liked = id != null && id in vm.likedIds
    val artistId = meta?.extras?.getString("artistId")

    LaunchedEffect(Unit) {
        while (true) {
            if (drag == null) {
                pos = vm.position()
                dur = vm.duration()
            }
            delay(400)
        }
    }

    LaunchedEffect(panel, meta?.title) {
        if (panel == 2) vm.loadLyrics()
    }

    LaunchedEffect(cur, panel, vm.lyrics.size) {
        if (panel == 2 && vm.lyrics.isNotEmpty()) lyricState.animateScrollToItem((cur - 2).coerceAtLeast(0))
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // blurred art as backdrop (blur is a no-op below android 12)
        AsyncImage(
            model = meta?.artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(80.dp).alpha(0.55f)
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0x66000000), Color(0xF20E0E10)))
            )
        )

        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { vm.showPlayer = false }) { Text("Close", color = Color.White) }
                Spacer(Modifier.weight(1f))
                Text("Now playing", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(64.dp))
            }

            when (panel) {
                1 -> {
                    // unique key per row, the same song can sit in the queue twice
                    val keys = remember(vm.queue) {
                        val seen = HashMap<String, Int>()
                        vm.queue.map { m ->
                            val n = (seen[m.mediaId] ?: 0) + 1
                            seen[m.mediaId] = n
                            "${m.mediaId}#$n"
                        }
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                        itemsIndexed(vm.queue, key = { i, _ -> keys[i] }) { i, item ->
                            QueueRow(vm, i, item, i == vm.queueIndex)
                        }
                    }
                }

                2 -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when {
                        vm.lyricsState == "loading" -> Text("Loading lyrics...", color = Color.White.copy(alpha = 0.7f))
                        vm.lyricsState == "none" -> Text("No synced lyrics found for this one", color = Color.White.copy(alpha = 0.7f))
                        else -> LazyColumn(Modifier.fillMaxSize(), state = lyricState) {
                            itemsIndexed(vm.lyrics) { i, line ->
                                Text(
                                    line.text.ifBlank { "..." },
                                    color = if (i == cur) Color.White else Color.White.copy(alpha = 0.4f),
                                    fontWeight = if (i == cur) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 22.sp,
                                    modifier = Modifier.fillMaxWidth().clickable { vm.seekTo(line.ms) }.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                else -> Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = meta?.artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true).clip(RoundedCornerShape(24.dp))
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        meta?.title?.toString() ?: "",
                        color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        meta?.artist?.toString() ?: "",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(enabled = artistId != null) {
                            vm.showPlayer = false
                            vm.openArtist(artistId!!, meta?.artist?.toString() ?: "", null)
                        }
                    )
                }
                RoundBtn(48.dp, Color.White.copy(alpha = 0.15f), { id?.let { vm.toggleLike(it) } }) {
                    Icon(
                        if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (liked) MaterialTheme.colorScheme.secondary else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Slider(
                value = drag ?: if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f,
                onValueChange = { drag = it },
                onValueChangeFinished = {
                    drag?.let {
                        val target = (it * dur).toLong()
                        vm.seekTo(target)
                        pos = target
                    }
                    drag = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
            Row(Modifier.fillMaxWidth()) {
                Text(fmt(pos), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text(fmt(dur), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = vm::previous, modifier = Modifier.size(64.dp)) {
                    SkipIcon(forward = false, dp = 34.dp, tint = Color.White)
                }
                Surface(onClick = vm::toggle, shape = CircleShape, color = Color.White, modifier = Modifier.size(68.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        PlayPauseIcon(vm.playing, 34.dp, Color.Black)
                    }
                }
                IconButton(onClick = vm::next, modifier = Modifier.size(64.dp)) {
                    SkipIcon(forward = true, dp = 34.dp, tint = Color.White)
                }
            }

            LazyRow(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pills = buildList<Triple<String, Boolean, () -> Unit>> {
                    add(Triple("Queue", panel == 1) { panel = if (panel == 1) 0 else 1 })
                    add(Triple("Lyrics", panel == 2) { panel = if (panel == 2) 0 else 2 })
                    add(Triple(if (vm.sleepMinutes > 0) "Sleep ${vm.sleepMinutes}m" else "Sleep", vm.sleepMinutes > 0) { vm.cycleSleep() })
                    add(Triple("Endless", vm.endless) { vm.setEndless(!vm.endless) })
                    add(Triple("Shuffle", vm.shuffle) { vm.toggleShuffle() })
                    add(
                        Triple(
                            when (vm.repeat) {
                                Player.REPEAT_MODE_ALL -> "Repeat all"
                                Player.REPEAT_MODE_ONE -> "Repeat one"
                                else -> "Repeat off"
                            },
                            vm.repeat != Player.REPEAT_MODE_OFF
                        ) { vm.cycleRepeat() }
                    )
                    val progress = id?.let { Downloads.active[it] }
                    add(
                        Triple(
                            when {
                                progress != null -> "Downloading ${(progress * 100).toInt()}%"
                                id != null && id in Downloads.ids -> "Downloaded"
                                else -> "Download"
                            },
                            id != null && id in Downloads.ids
                        ) { vm.currentTrack()?.let { vm.download(it) } }
                    )
                }
                itemsIndexed(pills) { _, (label, active, action) -> Pill(label, active, action) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueRow(vm: MusicVM, i: Int, item: MediaItem, current: Boolean) {
    val index = rememberUpdatedState(i)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v != SwipeToDismissBoxValue.Settled) {
                vm.removeFromQueue(index.value)
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = state,
        modifier = Modifier.padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)),
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(Color(0xFF6B2A38)).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                Text("Remove", color = Color.White)
            }
        }
    ) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF17171A)).clickable { vm.playQueueIndex(i) }.padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = item.mediaMetadata.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
            )
            Column(Modifier.weight(1f)) {
                Text(
                    item.mediaMetadata.title?.toString() ?: "",
                    color = if (current) MaterialTheme.colorScheme.primary else Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.mediaMetadata.artist?.toString() ?: "",
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = { vm.removeFromQueue(i) }) {
                Text("✕", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun Pill(text: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
    ) {
        Text(
            text,
            color = if (active) Color.Black else Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}
