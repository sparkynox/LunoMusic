package lumi.sparkynox.lunomusic

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lumi.sparkynox.lunomusic.extract.Extractor
import lumi.sparkynox.lunomusic.extract.Track

class MusicVM(app: Application) : AndroidViewModel(app) {
    var query by mutableStateOf("")
    var results by mutableStateOf<List<Track>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var nowPlaying by mutableStateOf<MediaMetadata?>(null)
    var playing by mutableStateOf(false)

    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playing = isPlaying
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            nowPlaying = mediaMetadata
        }

        override fun onPlayerError(error: PlaybackException) {
            this@MusicVM.error = "playback failed: ${error.errorCodeName}"
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlayerService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(listener)
            playing = c.isPlaying
            if (c.mediaItemCount > 0) nowPlaying = c.mediaMetadata
        }, ContextCompat.getMainExecutor(app))
    }

    fun search() {
        if (query.isBlank()) return
        viewModelScope.launch {
            loading = true
            error = null
            try {
                results = withContext(Dispatchers.IO) { Extractor.search(query.trim()) }
                if (results.isEmpty()) error = "nothing found"
            } catch (e: Exception) {
                error = e.message ?: "search failed"
            }
            loading = false
        }
    }

    fun play(index: Int) {
        val c = controller ?: return
        error = null
        c.setMediaItems(results.map { it.toItem() }, index, 0L)
        c.prepare()
        c.play()
    }

    fun toggle() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    override fun onCleared() {
        controller?.release()
    }

    private fun Track.toItem() = MediaItem.Builder()
        .setMediaId(id)
        .setUri("luno://track/$id")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(thumb?.let { Uri.parse(it) })
                .build()
        )
        .build()
}
