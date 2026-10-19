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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
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
    val press = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val pressScale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 1.03f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "miniPress")
    val back = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .widthIn(max = NavigationBarMaxWidth)
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .clip(shape)
                .background(scheme.surfaceContainerHigh.copy(alpha = 0.88f))
                .border(0.5.dp, Color.White.copy(alpha = 0.14f), shape)
                .clickable(interactionSource = press, indication = null) { vm.showPlayer = true }
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
                Modifier.fillMaxSize().padding(start = 8.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = meta.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(scheme.surfaceVariant)
                )

                Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.Center) {
                    AnimatedContent(
                        targetState = meta.title?.toString() ?: "",
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "miniTitle"
                    ) { title ->
                        Text(
                            title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee()
                        )
                    }
                    Text(
                        meta.artist?.toString() ?: "", style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = vm::toggle, modifier = Modifier.size(44.dp)) {
                    Icon(painterResource(if (vm.playing) R.drawable.pause else R.drawable.play), null, tint = scheme.onSurface, modifier = Modifier.size(28.dp))
                }
                // swiping the bar still skips, the button makes way in a narrow window
                if (!isTinyWidth()) IconButton(onClick = vm::next, modifier = Modifier.size(44.dp)) {
                    Icon(painterResource(R.drawable.skip_next), null, tint = scheme.onSurface, modifier = Modifier.size(26.dp))
                }
            }

            // thin progress line at the bottom edge
            Box(
                Modifier.align(Alignment.BottomStart)
                    .fillMaxWidth(if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f)
                    .height(2.dp)
                    .background(scheme.primary.copy(alpha = 0.8f))
            )
        }
    }
}
