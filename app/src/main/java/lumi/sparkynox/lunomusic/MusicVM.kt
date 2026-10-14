package lumi.sparkynox.lunomusic

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lumi.sparkynox.lunomusic.auth.Session
import lumi.sparkynox.lunomusic.extract.Chip
import lumi.sparkynox.lunomusic.extract.Extractor
import lumi.sparkynox.lunomusic.extract.Feed
import lumi.sparkynox.lunomusic.extract.HomeItem
import lumi.sparkynox.lunomusic.extract.HomeSection
import lumi.sparkynox.lunomusic.extract.Kind
import lumi.sparkynox.lunomusic.extract.Track

class MusicVM(app: Application) : AndroidViewModel(app) {
    var tab by mutableIntStateOf(0)
    var showLogin by mutableStateOf(false)
    var showAbout by mutableStateOf(false)
    var showPlayer by mutableStateOf(false)
    var loggedIn by mutableStateOf(Session.loggedIn)

    var home by mutableStateOf<List<HomeSection>>(emptyList())
    var homeCont by mutableStateOf<String?>(null)
    var homeLoading by mutableStateOf(false)
    var chips by mutableStateOf<List<Chip>>(emptyList())
    var chip by mutableStateOf<Chip?>(null)

    var query by mutableStateOf("")
    var results by mutableStateOf<List<Track>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    var nowPlaying by mutableStateOf<MediaMetadata?>(null)
    var playing by mutableStateOf(false)
    var shuffle by mutableStateOf(false)
    var repeat by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    var queue by mutableStateOf<List<MediaItem>>(emptyList())
    var queueIndex by mutableIntStateOf(0)

    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playing = isPlaying
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            nowPlaying = mediaMetadata
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            shuffle = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            repeat = repeatMode
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            controller?.let { syncQueue(it) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            queueIndex = controller?.currentMediaItemIndex ?: 0
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
            shuffle = c.shuffleModeEnabled
            repeat = c.repeatMode
            if (c.mediaItemCount > 0) {
                nowPlaying = c.mediaMetadata
                syncQueue(c)
            }
        }, ContextCompat.getMainExecutor(app))

        loadHome()
    }

    private fun syncQueue(c: MediaController) {
        queue = (0 until c.mediaItemCount).map { c.getMediaItemAt(it) }
        queueIndex = c.currentMediaItemIndex
    }

    fun loadHome(more: Boolean = false) {
        if (homeLoading) return
        viewModelScope.launch {
            homeLoading = true
            error = null
            try {
                val page = withContext(Dispatchers.IO) {
                    Feed.home(if (more) homeCont else null, if (more) null else chip)
                }
                home = if (more) home + page.sections else page.sections
                homeCont = page.continuation
                if (!more && chip == null && page.chips.isNotEmpty()) chips = page.chips
            } catch (e: Exception) {
                error = e.message ?: "home failed"
            }
            homeLoading = false
        }
    }

    fun selectChip(c: Chip) {
        chip = if (chip == c) null else c
        home = emptyList()
        homeCont = null
        loadHome()
    }

    private fun resetHome() {
        chip = null
        chips = emptyList()
        home = emptyList()
        homeCont = null
        loadHome()
    }

    fun onLoginDone() {
        showLogin = false
        loggedIn = true
        resetHome()
    }

    fun logout() {
        Session.logout()
        loggedIn = false
        resetHome()
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

    fun play(index: Int) = playTracks(results, index)

    fun playItem(section: HomeSection, item: HomeItem) {
        when (item.kind) {
            Kind.SONG -> {
                val songs = section.items.filter { it.kind == Kind.SONG && it.videoId != null }
                val start = songs.indexOfFirst { it.videoId == item.videoId }.coerceAtLeast(0)
                playTracks(songs.map { Track(it.videoId!!, it.title, it.artist, it.thumb, 0L) }, start)
            }
            Kind.ALBUM, Kind.PLAYLIST -> viewModelScope.launch {
                try {
                    val q = withContext(Dispatchers.IO) { Feed.queue(item.playlistId) }
                    if (q.isEmpty()) error = "nothing to play" else playTracks(q, 0)
                } catch (e: Exception) {
                    error = e.message ?: "couldn't load that"
                }
            }
            Kind.ARTIST -> Unit
        }
    }

    fun playSection(section: HomeSection) {
        val songs = section.items.filter { it.kind == Kind.SONG && it.videoId != null }
        if (songs.isNotEmpty()) playTracks(songs.map { Track(it.videoId!!, it.title, it.artist, it.thumb, 0L) }, 0)
    }

    private fun playTracks(tracks: List<Track>, index: Int) {
        val c = controller ?: return
        error = null
        c.setMediaItems(tracks.map { it.toItem() }, index, 0L)
        c.prepare()
        c.play()
    }

    fun toggle() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
    }

    fun position(): Long = controller?.currentPosition ?: 0L

    fun duration(): Long = controller?.duration?.takeIf { it > 0 } ?: 0L

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun playQueueIndex(i: Int) {
        controller?.seekTo(i, 0L)
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
