package lumi.sparkynox.lunomusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.media3.common.Player
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import lumi.sparkynox.lunomusic.MusicVM

private fun fmt(ms: Long) = "%d:%02d".format(ms / 60000, (ms / 1000) % 60)

@Composable
fun NowPlaying(vm: MusicVM) {
    BackHandler { vm.showPlayer = false }

    val meta = vm.nowPlaying
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var drag by remember { mutableStateOf<Float?>(null) }
    var showQueue by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        while (true) {
            if (drag == null) {
                pos = vm.position()
                dur = vm.duration()
            }
            delay(400)
        }
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

        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { vm.showPlayer = false }) { Text("Close", color = Color.White) }
                Spacer(Modifier.weight(1f))
                Text("Now playing", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(64.dp))
            }

            if (showQueue) {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    itemsIndexed(vm.queue) { i, item ->
                        val current = i == vm.queueIndex
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.playQueueIndex(i) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = item.mediaMetadata.artworkUri,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.mediaMetadata.title?.toString() ?: "",
                                    color = if (current) accent else Color.White,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    item.mediaMetadata.artist?.toString() ?: "",
                                    color = Color.White.copy(alpha = 0.6f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
                AsyncImage(
                    model = meta?.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp))
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
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
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )

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
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = vm::previous, modifier = Modifier.size(64.dp)) {
                    SkipIcon(forward = false, dp = 36.dp, tint = Color.White)
                }
                Surface(onClick = vm::toggle, shape = CircleShape, color = Color.White, modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        PlayPauseIcon(vm.playing, 36.dp, Color.Black)
                    }
                }
                IconButton(onClick = vm::next, modifier = Modifier.size(64.dp)) {
                    SkipIcon(forward = true, dp = 36.dp, tint = Color.White)
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Pill("Queue", showQueue) { showQueue = !showQueue }
                Pill("Shuffle", vm.shuffle) { vm.toggleShuffle() }
                Pill(
                    when (vm.repeat) {
                        Player.REPEAT_MODE_ALL -> "Repeat all"
                        Player.REPEAT_MODE_ONE -> "Repeat one"
                        else -> "Repeat off"
                    },
                    vm.repeat != Player.REPEAT_MODE_OFF
                ) { vm.cycleRepeat() }
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
