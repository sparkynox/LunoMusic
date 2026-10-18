/*
 * Player layout is modelled on ArchiveTune's player (GPL-3.0, (c) Rukamori, github.com/rukamori) and
 * uses its wavy slider and animated playback controls (see ui/at).
 */
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package lumi.sparkynox.lunomusic.ui

import android.content.Context
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.TextStyle
import kotlin.math.abs
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.R
import lumi.sparkynox.lunomusic.data.Downloads
import lumi.sparkynox.lunomusic.extract.Extractor
import lumi.sparkynox.lunomusic.util.Img
import lumi.sparkynox.lunomusic.ui.at.V9AnimatedPlaybackControls
import lumi.sparkynox.lunomusic.ui.at.WavySliderExpressive

private fun fmt(ms: Long) = "%d:%02d".format(ms / 60000, (ms / 1000) % 60)

@Composable
fun NowPlaying(vm: MusicVM) {
    BackHandler { vm.showPlayer = false }

    val scheme = MaterialTheme.colorScheme
    val meta = vm.nowPlaying
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var drag by remember { mutableStateOf<Float?>(null) }
    var panel by remember { mutableIntStateOf(0) } // 0 art, 1 queue, 2 lyrics

    val id = vm.queue.getOrNull(vm.queueIndex)?.mediaId
    val liked = id != null && id in vm.likedIds
    val artistId = meta?.extras?.getString("artistId")
    val artColor by rememberArtColor(meta?.artworkUri, scheme.surfaceContainerHigh)
    val bg = lerp(scheme.background, artColor, 0.85f)
    val text = Color.White

    LaunchedEffect(vm.playing) {
        while (true) {
            if (drag == null) {
                pos = vm.position()
                dur = vm.duration()
            }
            delay(if (vm.playing) 100 else 400)
        }
    }
    LaunchedEffect(panel, meta?.title) { if (panel == 2) vm.loadLyrics() }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bg, scheme.background)))) {
        // blurred cover behind everything, like Apple Music (blur needs Android 12+, older phones keep the gradient)
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            AsyncImage(
                model = meta?.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(80.dp).graphicsLayer { alpha = 0.6f }
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        }
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                AnimatedContent(
                    targetState = panel,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                    label = "playerPanel"
                ) { p ->
                    when (p) {
                        1 -> QueuePanel(vm)
                        2 -> LyricsPanel(vm) { pos }
                        else -> {
                            // square rounded artwork, shrinks a bit while paused
                            val artScale by animateFloatAsState(
                                if (vm.playing) 1f else 0.86f,
                                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow), label = "artScale"
                            )
                            Box(Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = 56.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                                AnimatedContent(
                                    targetState = meta?.artworkUri,
                                    transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
                                    label = "playerArt"
                                ) { art ->
                                    PlayerArt(
                                        art?.toString(),
                                        Modifier.fillMaxWidth()
                                            .aspectRatio(1f)
                                            .graphicsLayer { scaleX = artScale; scaleY = artScale }
                                            .shadow(28.dp, RoundedCornerShape(20.dp))
                                            .clip(RoundedCornerShape(20.dp))
                                    )
                                }
                            }
                        }
                    }
                }
                RoundIcon(
                    R.drawable.expand_more, { vm.showPlayer = false },
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp),
                    container = Color.Black.copy(alpha = 0.35f), tint = Color.White
                )
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = meta?.title?.toString() ?: "",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "playerTitle"
                        ) { t ->
                            Text(
                                t, color = text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee()
                            )
                        }
                        Text(
                            meta?.artist?.toString() ?: "",
                            color = text.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(enabled = artistId != null) {
                                vm.showPlayer = false
                                vm.openArtist(artistId!!, meta?.artist?.toString() ?: "", null)
                            }
                        )
                    }
                    RoundIcon(
                        R.drawable.playlist_add,
                        { vm.currentTrack()?.let { vm.openAddToPlaylist(it) } },
                        size = 48.dp, iconSize = 22.dp,
                        container = Color.White.copy(alpha = 0.14f), tint = Color.White
                    )
                    val likeBg by animateColorAsState(
                        if (liked) scheme.primaryContainer else Color.White.copy(alpha = 0.14f), label = "likeBg"
                    )
                    val likeScale by animateFloatAsState(
                        if (liked) 1.12f else 1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "likeScale"
                    )
                    RoundIcon(
                        if (liked) R.drawable.favorite else R.drawable.favorite_border,
                        { id?.let { vm.toggleLike(it) } },
                        modifier = Modifier.graphicsLayer { scaleX = likeScale; scaleY = likeScale },
                        size = 48.dp, iconSize = 22.dp,
                        container = likeBg,
                        tint = if (liked) scheme.onPrimaryContainer else Color.White
                    )
                }

                Spacer(Modifier.height(8.dp))

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
                        thumbColor = text, activeTrackColor = text, inactiveTrackColor = text.copy(alpha = 0.25f)
                    ),
                    thumb = { Box(Modifier.size(12.dp).background(text, CircleShape)) },
                    modifier = Modifier.fillMaxWidth()
                )
                TimeRow(
                    position = { drag?.let { (it * dur).toLong() } ?: pos },
                    duration = { dur },
                    format = id?.let { Extractor.formatOf(it) },
                    color = text
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(onClick = vm::previous, modifier = Modifier.size(76.dp)) {
                        Icon(painterResource(R.drawable.skip_previous), null, tint = Color.White, modifier = Modifier.size(46.dp))
                    }
                    androidx.compose.material3.IconButton(onClick = vm::toggle, modifier = Modifier.size(88.dp)) {
                        Icon(painterResource(if (vm.playing) R.drawable.pause else R.drawable.play), null, tint = Color.White, modifier = Modifier.size(68.dp))
                    }
                    androidx.compose.material3.IconButton(onClick = vm::next, modifier = Modifier.size(76.dp)) {
                        Icon(painterResource(R.drawable.skip_next), null, tint = Color.White, modifier = Modifier.size(46.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))
                VolumeRow(text)
                Spacer(Modifier.height(6.dp))

                val offline = id != null && id in Downloads.ids
                val dlProgress = id?.let { Downloads.active[it] }
                LazyRow(
                    Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { PlayerToggle(R.drawable.queue_music, panel == 1) { panel = if (panel == 1) 0 else 1 } }
                    item { PlayerToggle(R.drawable.lyrics, panel == 2) { panel = if (panel == 2) 0 else 2 } }
                    item { PlayerToggle(R.drawable.bedtime, vm.sleepMinutes > 0, if (vm.sleepMinutes > 0) "${vm.sleepMinutes}m" else null) { vm.cycleSleep() } }
                    item { PlayerToggle(if (vm.shuffle) R.drawable.shuffle_on else R.drawable.shuffle, vm.shuffle) { vm.toggleShuffle() } }
                    item {
                        PlayerToggle(
                            when (vm.repeat) {
                                Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                                else -> R.drawable.repeat
                            },
                            vm.repeat != Player.REPEAT_MODE_OFF
                        ) { vm.cycleRepeat() }
                    }
                    item { PlayerToggle(R.drawable.all_inclusive, vm.endless, "Endless") { vm.setEndlessEnabled(!vm.endless) } }
                    item {
                        PlayerToggle(
                            if (offline) R.drawable.offline else R.drawable.download,
                            offline,
                            dlProgress?.let { "${(it * 100).toInt()}%" }
                        ) { vm.currentTrack()?.let { vm.download(it) } }
                    }
                }
            }
        }
    }
}

// full screen artwork: asks for the biggest version, if that one fails to load the original url is used
@Composable
private fun PlayerArt(url: String?, modifier: Modifier = Modifier.fillMaxSize()) {
    var plain by remember(url) { mutableStateOf(false) }
    AsyncImage(
        model = if (plain) url else Img.best(url),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        onError = { plain = true },
        modifier = modifier
    )
}

@Composable
private fun PlayerToggle(icon: Int, active: Boolean, label: String? = null, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(if (active) scheme.primaryContainer else Color.White.copy(alpha = 0.14f), label = "toggleBg")
    Surface(onClick = onClick, shape = CircleShape, color = container, modifier = Modifier.height(48.dp).widthIn(min = 48.dp)) {
        Row(
            Modifier.padding(horizontal = if (label != null) 16.dp else 0.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(icon), null, tint = if (active) scheme.onPrimaryContainer else Color.White, modifier = Modifier.size(22.dp))
            if (label != null) {
                Spacer(Modifier.size(6.dp))
                Text(label, color = if (active) scheme.onPrimaryContainer else Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun VolumeRow(text: Color) {
    val ctx = LocalContext.current
    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val max = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var v by remember { mutableFloatStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max) }

    LaunchedEffect(Unit) {
        while (true) {
            v = am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
            delay(700)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(painterResource(R.drawable.volume_off), null, tint = text.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Slider(
            value = v,
            onValueChange = {
                v = it
                am.setStreamVolume(AudioManager.STREAM_MUSIC, (it * max).toInt(), 0)
            },
            colors = SliderDefaults.colors(
                thumbColor = text, activeTrackColor = text, inactiveTrackColor = text.copy(alpha = 0.22f)
            ),
            thumb = { Box(Modifier.size(10.dp).background(text, CircleShape)) },
            modifier = Modifier.weight(1f)
        )
        Icon(painterResource(R.drawable.volume_up), null, tint = text.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun QueuePanel(vm: MusicVM) {
    val keys = remember(vm.queue) {
        val seen = HashMap<String, Int>()
        vm.queue.map { m ->
            val n = (seen[m.mediaId] ?: 0) + 1
            seen[m.mediaId] = n
            "${m.mediaId}#$n"
        }
    }
    val state = rememberLazyListState()
    LaunchedEffect(Unit) { if (vm.queueIndex > 1) state.scrollToItem((vm.queueIndex - 1).coerceAtLeast(0)) }

    var q by remember { mutableStateOf("") }
    val needle = q.trim().lowercase()
    val shown = remember(vm.queue, q) {
        vm.queue.mapIndexed { i, item -> i to item }
            .filter { (_, item) ->
                needle.isEmpty() ||
                    item.mediaMetadata.title?.toString()?.lowercase()?.contains(needle) == true ||
                    item.mediaMetadata.artist?.toString()?.lowercase()?.contains(needle) == true
            }
    }

    Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
        Row(Modifier.fillMaxWidth().padding(start = 72.dp, end = 16.dp, top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Queue", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.weight(1f))
            Text("${vm.queue.size} songs", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            RoundIcon(
                R.drawable.clear_all, { vm.clearQueue() }, size = 38.dp, iconSize = 18.dp,
                container = Color.White.copy(alpha = 0.14f), tint = Color.White
            )
        }

        TextField(
            value = q,
            onValueChange = { q = it },
            placeholder = { Text("Search in queue", color = Color.White.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(painterResource(R.drawable.search), null, tint = Color.White.copy(alpha = 0.7f)) },
            trailingIcon = {
                if (q.isNotEmpty()) RoundIcon(
                    R.drawable.close, { q = "" }, size = 32.dp, iconSize = 16.dp,
                    container = Color.Transparent, tint = Color.White.copy(alpha = 0.7f)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.12f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            if (shown.isEmpty()) {
                item {
                    Text(
                        if (vm.queue.isEmpty()) "Queue is empty" else "No matches in the queue",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(shown, key = { (i, _) -> keys.getOrElse(i) { "$i" } }) { (i, item) ->
                    QueueRow(vm, i, item, i == vm.queueIndex, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun QueueRow(vm: MusicVM, i: Int, item: MediaItem, current: Boolean, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val index = rememberUpdatedState(i)
    val state = rememberSwipeToDismissBoxState(
        positionalThreshold = { total -> total * 0.62f },
        confirmValueChange = { v ->
            when (v) {
                SwipeToDismissBoxValue.StartToEnd -> { vm.removeFromQueue(index.value); true }
                SwipeToDismissBoxValue.EndToStart -> { vm.notInterested(index.value); true }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = state,
        modifier = modifier.padding(vertical = 3.dp).clip(RoundedCornerShape(18.dp)),
        backgroundContent = {
            val dir = state.dismissDirection
            val notInterested = dir == SwipeToDismissBoxValue.EndToStart
            Box(
                Modifier.fillMaxSize()
                    .background(if (notInterested) scheme.tertiaryContainer else scheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (notInterested) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Icon(
                    painterResource(if (notInterested) R.drawable.block else R.drawable.delete), null,
                    tint = if (notInterested) scheme.onTertiaryContainer else scheme.onErrorContainer
                )
            }
        }
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(if (current) scheme.secondaryContainer else scheme.surfaceContainer)
                .clickable { vm.playQueueIndex(i) }
                .padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = item.mediaMetadata.artworkUri, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                )
                if (current) Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.graphic_eq), null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    item.mediaMetadata.title?.toString() ?: "",
                    color = if (current) scheme.onSecondaryContainer else scheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.mediaMetadata.artist?.toString() ?: "",
                    color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            RoundIcon(
                R.drawable.close, { vm.removeFromQueue(i) }, size = 40.dp, iconSize = 18.dp,
                container = Color.Transparent, tint = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimeRow(position: () -> Long, duration: () -> Long, format: String?, color: Color) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(fmt(position()), color = color.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.weight(1f))
        format?.let { f ->
            Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.12f)) {
                Text(
                    f, color = color.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(fmt(duration()), color = color.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
    }
}

// keeps the active line in the middle of the list with a smooth glide
private suspend fun LazyListState.centerOn(index: Int) {
    var item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        scrollToItem(index)
        item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    }
    item ?: return
    val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    animateScrollBy((item.offset + item.size / 2 - center).toFloat(), tween(560, easing = FastOutSlowInEasing))
}

@Composable
private fun LyricsPanel(vm: MusicVM, pos: () -> Long) {
    val lines = vm.lyrics
    val state = rememberLazyListState()
    val dragged by state.interactionSource.collectIsDraggedAsState()
    // only changes when the sung line changes, so the list isn't recomposed on every tick
    val cur by remember(lines) { derivedStateOf { lines.indexOfLast { it.ms <= pos() + 120 }.coerceAtLeast(0) } }

    LaunchedEffect(cur, lines.size) {
        if (lines.isNotEmpty() && !dragged) state.centerOn(cur)
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
        when {
            vm.lyricsState == "loading" -> Text("Loading lyrics...", color = Color.White.copy(alpha = 0.7f))
            vm.lyricsState == "none" -> Text("No synced lyrics found for this one", color = Color.White.copy(alpha = 0.7f))
            else -> LazyColumn(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        // fade the top and bottom edges out
                        drawRect(
                            Brush.verticalGradient(0f to Color.Transparent, 0.14f to Color.Black, 0.86f to Color.Black, 1f to Color.Transparent),
                            blendMode = BlendMode.DstIn
                        )
                    },
                state = state,
                contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = maxHeight / 2, bottom = maxHeight / 2)
            ) {
                itemsIndexed(lines) { i, line ->
                    LyricLine(
                        text = line.text.ifBlank { "..." },
                        dist = abs(i - cur),
                        startMs = line.ms,
                        endMs = lines.getOrNull(i + 1)?.ms ?: (line.ms + 5000),
                        pos = pos,
                        onClick = { vm.seekTo(line.ms) }
                    )
                }
            }
        }
    }
}

// the sung line glows and fills as it plays, the lines around it blur out with distance
@Composable
private fun LyricLine(text: String, dist: Int, startMs: Long, endMs: Long, pos: () -> Long, onClick: () -> Unit) {
    val active = dist == 0
    val alpha by animateFloatAsState(if (active) 1f else (0.55f - dist * 0.08f).coerceAtLeast(0.16f), tween(420), label = "lyricAlpha")
    val scale by animateFloatAsState(if (active) 1f else 0.9f, spring(stiffness = Spring.StiffnessLow), label = "lyricScale")
    val blur by animateDpAsState(if (active) 0.dp else (dist.coerceAtMost(5) * 1.5f).dp, tween(420), label = "lyricBlur")
    val glow by animateFloatAsState(if (active) 1f else 0f, tween(500), label = "lyricGlow")

    val target = if (active) ((pos() - startMs).toFloat() / (endMs - startMs).coerceAtLeast(1L)).coerceIn(0f, 1f) else 0f
    val fill by animateFloatAsState(target, tween(160, easing = LinearEasing), label = "lyricFill")

    val base = TextStyle(fontSize = 28.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold)
    val glowShadow = Shadow(Color.White.copy(alpha = 0.6f * glow), Offset.Zero, 34f * glow + 0.1f)
    val feather = with(LocalDensity.current) { 28.dp.toPx() }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 12.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .alpha(alpha)
            .then(if (blur > 0.dp) Modifier.blur(blur, BlurredEdgeTreatment.Unbounded) else Modifier)
    ) {
        if (active) {
            // A brush over the whole text gives every wrapped line the same fill at the same time. So: a dim copy of the
            // text underneath, and on top a bright copy where the part that isn't sung yet is erased, line by line in
            // reading order. The first line fills up completely before the second one starts.
            Text(text, style = base.copy(color = Color.White.copy(alpha = 0.5f), shadow = glowShadow), onTextLayout = { layout = it })
            Text(
                text, style = base.copy(color = Color.White),
                modifier = Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        layout?.let { eraseUnsung(it, fill, feather) }
                    }
            )
        } else {
            Text(text, style = base.copy(color = Color.White, shadow = glowShadow))
        }
    }
}

// wipes the not yet sung part of the bright text. fill is 0..1 over the width of all the lines together
private fun DrawScope.eraseUnsung(layout: TextLayoutResult, fill: Float, feather: Float) {
    val n = layout.lineCount
    if (n == 0) return
    val widths = FloatArray(n) { (layout.getLineRight(it) - layout.getLineLeft(it)).coerceAtLeast(0f) }
    var left = fill.coerceIn(0f, 1f) * widths.sum()
    for (i in 0 until n) {
        val w = widths[i]
        val filled = left.coerceIn(0f, w)
        left -= w
        if (filled >= w) continue
        val top = layout.getLineTop(i)
        val h = layout.getLineBottom(i) - top
        if (filled <= 0f) {
            drawRect(Color.Transparent, Offset(0f, top), Size(size.width, h), blendMode = BlendMode.DstIn)
            continue
        }
        val edge = layout.getLineLeft(i) + filled
        drawRect(
            Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = edge, endX = edge + feather),
            Offset(edge, top), Size(feather, h), blendMode = BlendMode.DstIn
        )
        val rest = size.width - edge - feather
        if (rest > 0f) drawRect(Color.Transparent, Offset(edge + feather, top), Size(rest, h), blendMode = BlendMode.DstIn)
    }
}
