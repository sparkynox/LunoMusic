/*
 * Layout and behaviour follow ArchiveTune's mini player (GPL-3.0, (c) Rukamori, github.com/rukamori):
 * wavy progress ring around round artwork, crossfading title, swipe to skip, expressive transport buttons.
 */
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)

package lumi.sparkynox.lunomusic.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.R
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MiniPlayer(vm: MusicVM, proximity: Float, modifier: Modifier = Modifier) {
    val meta = vm.nowPlaying ?: return
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(vm.playing) {
        while (true) {
            position = vm.position()
            duration = vm.duration()
            delay(if (vm.playing) 400 else 900)
        }
    }

    val shape = RoundedCornerShape(
        topStart = lerp(FloatingBarStandaloneCornerRadius.value, FloatingBarOuterCornerRadius.value, proximity).dp,
        topEnd = lerp(FloatingBarStandaloneCornerRadius.value, FloatingBarOuterCornerRadius.value, proximity).dp,
        bottomStart = lerp(FloatingBarStandaloneCornerRadius.value, FloatingBarJunctionCornerRadius.value, proximity).dp,
        bottomEnd = lerp(FloatingBarStandaloneCornerRadius.value, FloatingBarJunctionCornerRadius.value, proximity).dp,
    )
    val back = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .widthIn(max = NavigationBarMaxWidth)
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .clip(shape)
                .background(scheme.surfaceContainerHigh)
                .clickable { vm.showPlayer = true }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val o = offsetX.value
                            scope.launch {
                                if (abs(o) > 140f) {
                                    if (o > 0) vm.previous() else vm.next()
                                }
                                offsetX.animateTo(0f, back)
                            }
                        },
                        onDragCancel = { scope.launch { offsetX.animateTo(0f, back) } },
                        onHorizontalDrag = { _, d -> scope.launch { offsetX.snapTo(offsetX.value + d) } },
                    )
                }
        ) {
            Row(
                Modifier.fillMaxSize().padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                    CircularWavyProgressIndicator(
                        progress = { if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.fillMaxSize(),
                        color = scheme.primary,
                        trackColor = scheme.outline.copy(alpha = 0.18f),
                    )
                    AsyncImage(
                        model = meta.artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(scheme.surfaceVariant)
                            .border(1.dp, scheme.outline.copy(alpha = 0.2f), CircleShape)
                    )
                }

                Column(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.Center) {
                    AnimatedContent(
                        targetState = meta.title?.toString() ?: "",
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "miniTitle"
                    ) { title ->
                        Text(
                            title, style = MaterialTheme.typography.titleMediumEmphasized, color = scheme.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee()
                        )
                    }
                    AnimatedContent(
                        targetState = meta.artist?.toString() ?: "",
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "miniArtist"
                    ) { artist ->
                        Text(
                            artist, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee()
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = vm::previous,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = scheme.surfaceContainerHighest, contentColor = scheme.onSurface
                        )
                    ) { Icon(painterResource(R.drawable.skip_previous), null, modifier = Modifier.size(20.dp)) }

                    FilledIconButton(
                        onClick = vm::toggle,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = scheme.primary, contentColor = scheme.onPrimary
                        )
                    ) { Icon(painterResource(if (vm.playing) R.drawable.pause else R.drawable.play), null, modifier = Modifier.size(24.dp)) }

                    IconButton(
                        onClick = vm::next,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = scheme.surfaceContainerHighest, contentColor = scheme.onSurface
                        )
                    ) { Icon(painterResource(R.drawable.skip_next), null, modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}
