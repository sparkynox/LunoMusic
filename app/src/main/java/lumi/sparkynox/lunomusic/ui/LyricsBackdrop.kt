package lumi.sparkynox.lunomusic.ui

import android.content.ContextWrapper
import android.view.TextureView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import lumi.sparkynox.lunomusic.util.AppLog

// the picked image or video behind the lyrics. A video loops and has its sound removed.
@Composable
fun LyricsBackdrop(uri: String, video: Boolean, onError: () -> Unit, modifier: Modifier = Modifier) {
    if (video) {
        key(uri) { VideoBackdrop(uri, onError, modifier) }
    } else {
        AsyncImage(
            model = uri, contentDescription = null, contentScale = ContentScale.Crop,
            onError = { onError() }, modifier = modifier
        )
    }
}

@Composable
private fun VideoBackdrop(uri: String, onError: () -> Unit, modifier: Modifier) {
    val ctx = LocalContext.current
    val onErr by rememberUpdatedState(onError)
    var videoW by remember { mutableIntStateOf(0) }
    var videoH by remember { mutableIntStateOf(0) }

    // A second, separate player. It has no audio attributes so it never asks for audio focus, and the audio track is
    // switched off completely: nothing is decoded and nothing can ever play over (or stop) the music.
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoW = (videoSize.width * videoSize.pixelWidthHeightRatio).toInt()
                videoH = videoSize.height
            }

            override fun onPlayerError(error: PlaybackException) {
                AppLog.e("LyricsBg", "the background video failed", error)
                onErr()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // no point decoding a video nobody can see
    val owner = remember(ctx) {
        generateSequence(ctx) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<LifecycleOwner>().firstOrNull()
    }
    DisposableEffect(owner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
            else if (event == Lifecycle.Event.ON_START) player.play()
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }

    BoxWithConstraints(modifier.clipToBounds()) {
        val density = LocalDensity.current
        // center crop: scale the video until it covers the whole area, the overflow is clipped
        val fit = if (videoW > 0 && videoH > 0) {
            val scale = maxOf(constraints.maxWidth.toFloat() / videoW, constraints.maxHeight.toFloat() / videoH)
            with(density) { Modifier.requiredSize((videoW * scale).toDp(), (videoH * scale).toDp()) }
        } else Modifier.fillMaxSize()
        AndroidView(
            factory = { c -> TextureView(c).also { player.setVideoTextureView(it) } },
            modifier = fit.align(Alignment.Center)
        )
    }
}

@Composable
fun LyricsBackgroundDialog(hasBackground: Boolean, onChoose: () -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lyrics background") },
        text = {
            Text("Pick an image or a video from your phone. A video loops with its sound removed, so it never mixes with your music.")
        },
        confirmButton = { TextButton(onClick = onChoose) { Text(if (hasBackground) "Change" else "Choose") } },
        dismissButton = {
            TextButton(onClick = if (hasBackground) onRemove else onDismiss) { Text(if (hasBackground) "Remove" else "Cancel") }
        }
    )
}
