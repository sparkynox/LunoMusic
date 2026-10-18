/*
 * Layout follows Echo Music's queue (GPL-3.0, github.com/iad1tya/Echo-Music): current song on top with like / lock /
 * more, shuffle + repeat + radio buttons, "Continue Playing / Next in Queue" with clear all, rows with a drag handle.
 */
package lumi.sparkynox.lunomusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.compose.AsyncImage
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.R

private val RowHeight = 70.dp

@Composable
fun QueueScreen(vm: MusicVM) {
    val scheme = MaterialTheme.colorScheme
    val current = vm.queue.getOrNull(vm.queueIndex)
    val currentId = current?.mediaId
    val liked = currentId != null && currentId in vm.likedIds
    var confirmClear by remember { mutableStateOf(false) }

    // unique key per row, the same song can sit in the queue twice
    val keys = remember(vm.queue) {
        val seen = HashMap<String, Int>()
        vm.queue.map { m ->
            val n = (seen[m.mediaId] ?: 0) + 1
            seen[m.mediaId] = n
            "${m.mediaId}#$n"
        }
    }
    val upcoming = remember(vm.queue, vm.queueIndex) {
        (vm.queueIndex + 1 until vm.queue.size).toList()
    }

    var dragging by remember { mutableStateOf<Int?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val rowPx = with(LocalDensity.current) { RowHeight.toPx() }
    val listState = rememberLazyListState()

    Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
        // current song
        Row(
            Modifier.fillMaxWidth().padding(start = 72.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = current?.mediaMetadata?.artworkUri, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(scheme.surfaceVariant)
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    current?.mediaMetadata?.title?.toString() ?: "", color = Color.White,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    current?.mediaMetadata?.artist?.toString() ?: "", color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            RoundIcon(
                if (liked) R.drawable.favorite else R.drawable.favorite_border,
                { currentId?.let { vm.toggleLike(it) } }, size = 40.dp, iconSize = 20.dp,
                container = if (liked) scheme.primaryContainer else Color.White.copy(alpha = 0.14f),
                tint = if (liked) scheme.onPrimaryContainer else Color.White
            )
            Box(Modifier.size(6.dp))
            RoundIcon(
                if (vm.queueLocked) R.drawable.lock else R.drawable.lock_open,
                { vm.queueLocked = !vm.queueLocked }, size = 40.dp, iconSize = 20.dp,
                container = if (vm.queueLocked) scheme.primaryContainer else Color.White.copy(alpha = 0.14f),
                tint = if (vm.queueLocked) scheme.onPrimaryContainer else Color.White
            )
            Box(Modifier.size(6.dp))
            CurrentMenu(vm)
        }

        // shuffle / repeat / radio
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QueueAction(
                "Shuffle", if (vm.shuffle) R.drawable.shuffle_on else R.drawable.shuffle, vm.shuffle,
                Modifier.weight(1f)
            ) { vm.toggleShuffle() }
            QueueAction(
                "Repeat",
                when (vm.repeat) {
                    Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                    else -> R.drawable.repeat
                },
                vm.repeat != Player.REPEAT_MODE_OFF, Modifier.weight(1f)
            ) { vm.cycleRepeat() }
            QueueAction("Radio", R.drawable.radio, false, Modifier.weight(1f)) { currentId?.let { vm.playRadio(it) } }
        }

        // section header
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Continue Playing", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("Next in Queue", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "${upcoming.size} songs", color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 10.dp)
            )
            if (!vm.queueLocked && upcoming.isNotEmpty()) {
                RoundIcon(
                    R.drawable.clear_all, { confirmClear = true }, size = 38.dp, iconSize = 20.dp,
                    container = Color.White.copy(alpha = 0.14f), tint = Color.White
                )
            }
        }

        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
            if (upcoming.isEmpty()) {
                item {
                    Text(
                        "Nothing queued after this song", color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(upcoming, key = { keys.getOrElse(it) { "q$it" } }) { i ->
                val item = vm.queue.getOrNull(i) ?: return@items
                val isDragged = dragging == i
                val idx = rememberUpdatedState(i)
                QueueRow(
                    vm = vm, item = item, onPlay = { vm.playQueueIndex(i) },
                    onRemove = { vm.removeFromQueue(i) },
                    modifier = Modifier
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragged) dragY else 0f }
                        .then(if (isDragged) Modifier else Modifier.animateItem()),
                    handle = {
                        if (!vm.queueLocked) {
                            Icon(
                                painterResource(R.drawable.drag_handle), null, tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(44.dp).padding(10.dp).pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { dragging = idx.value; dragY = 0f },
                                        onDragEnd = { dragging = null; dragY = 0f },
                                        onDragCancel = { dragging = null; dragY = 0f },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            val cur = dragging ?: return@detectDragGestures
                                            dragY += amount.y
                                            if (dragY > rowPx * 0.55f && cur + 1 < vm.queue.size) {
                                                vm.moveQueueItem(cur, cur + 1)
                                                dragging = cur + 1
                                                dragY -= rowPx
                                            } else if (dragY < -rowPx * 0.55f && cur - 1 > vm.queueIndex) {
                                                vm.moveQueueItem(cur, cur - 1)
                                                dragging = cur - 1
                                                dragY += rowPx
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(painterResource(R.drawable.clear_all), null) },
            title = { Text("Clear queue") },
            text = { Text("Remove all the songs after the current one from the queue?") },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.clearUpcoming() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun QueueAction(label: String, icon: Int, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick, modifier = modifier.height(52.dp), shape = RoundedCornerShape(26.dp),
        color = if (active) scheme.primaryContainer else Color.White.copy(alpha = 0.14f)
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            val tint = if (active) scheme.onPrimaryContainer else Color.White
            Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(20.dp))
            Box(Modifier.size(8.dp))
            Text(label, color = tint, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun QueueRow(
    vm: MusicVM, item: MediaItem, onPlay: () -> Unit, onRemove: () -> Unit,
    modifier: Modifier, handle: @Composable () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(
            Modifier.fillMaxWidth().height(RowHeight - 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .clickable(onClick = onPlay)
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.mediaMetadata.artworkUri, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp))
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    item.mediaMetadata.title?.toString() ?: "", color = Color.White,
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.mediaMetadata.artist?.toString() ?: "", color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) {
                    Icon(painterResource(R.drawable.more_vert), null, tint = Color.White)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Add to playlist") },
                        leadingIcon = { Icon(painterResource(R.drawable.playlist_add), null) },
                        onClick = {
                            menu = false
                            val m = item.mediaMetadata
                            vm.openAddToPlaylist(
                                lumi.sparkynox.lunomusic.extract.Track(
                                    item.mediaId, m.title?.toString() ?: "", m.artist?.toString() ?: "",
                                    m.artworkUri?.toString(), 0L, m.extras?.getString("artistId")
                                )
                            )
                        }
                    )
                    if (!vm.queueLocked) DropdownMenuItem(
                        text = { Text("Remove from queue") },
                        leadingIcon = { Icon(painterResource(R.drawable.delete), null) },
                        onClick = { menu = false; onRemove() }
                    )
                }
            }
            handle()
        }
    }
}

// more menu of the current song: add to playlist, download, playback speed
@Composable
private fun CurrentMenu(vm: MusicVM) {
    var open by remember { mutableStateOf(false) }
    var speedMenu by remember { mutableStateOf(false) }
    Box {
        RoundIcon(
            R.drawable.more_vert, { open = true }, size = 40.dp, iconSize = 20.dp,
            container = Color.White.copy(alpha = 0.14f), tint = Color.White
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false; speedMenu = false }) {
            if (!speedMenu) {
                DropdownMenuItem(
                    text = { Text("Add to playlist") },
                    leadingIcon = { Icon(painterResource(R.drawable.playlist_add), null) },
                    onClick = { open = false; vm.currentTrack()?.let { vm.openAddToPlaylist(it) } }
                )
                DropdownMenuItem(
                    text = { Text("Download") },
                    leadingIcon = { Icon(painterResource(R.drawable.download), null) },
                    onClick = { open = false; vm.currentTrack()?.let { vm.download(it) } }
                )
                DropdownMenuItem(
                    text = { Text("Playback speed  (${speedLabel(vm.speed)})") },
                    leadingIcon = { Icon(painterResource(R.drawable.speed), null) },
                    onClick = { speedMenu = true }
                )
            } else {
                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                    DropdownMenuItem(
                        text = { Text(speedLabel(s) + if (vm.speed == s) "  ✓" else "") },
                        onClick = { vm.setSpeed(s); open = false; speedMenu = false }
                    )
                }
            }
        }
    }
}

private fun speedLabel(s: Float) = if (s == 1f) "Normal" else "${s}x"

// Echo style sleep timer: minutes slider, "End of song" and Reset
@Composable
fun SleepTimerDialog(vm: MusicVM, onDismiss: () -> Unit) {
    var minutes by remember { mutableIntStateOf(if (vm.sleepMinutes > 0) vm.sleepMinutes else 30) }
    var endOfSong by remember { mutableStateOf(vm.sleepEndOfSong) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(if (endOfSong) "End of song" else "$minutes minutes", style = MaterialTheme.typography.titleLarge)
                Slider(
                    value = minutes.toFloat(), onValueChange = { minutes = it.toInt(); endOfSong = false },
                    valueRange = 5f..120f, steps = 22
                )
                OutlinedButton(onClick = { endOfSong = !endOfSong }) {
                    Text(if (endOfSong) "End of song  ✓" else "End of song")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.setSleep(minutes, endOfSong); onDismiss() }) { Text("OK") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { vm.setSleep(0, false); onDismiss() }) { Text("Reset") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
