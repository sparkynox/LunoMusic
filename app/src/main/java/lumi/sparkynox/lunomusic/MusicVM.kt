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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lumi.sparkynox.lunomusic.auth.Session
import lumi.sparkynox.lunomusic.extract.Chip
import lumi.sparkynox.lunomusic.extract.Extractor
import lumi.sparkynox.lunomusic.extract.Feed
import lumi.sparkynox.lunomusic.extract.HomeItem
import lumi.sparkynox.lunomusic.extract.HomeSection
import lumi.sparkynox.lunomusic.extract.Kind
import lumi.sparkynox.lunomusic.extract.LyricLine
import lumi.sparkynox.lunomusic.extract.Lyrics
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
    var suggestions by mutableStateOf<List<String>>(emptyList())
    var results by mutableStateOf<List<Track>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    var libTitle by mutableStateOf<String?>(null)
    var libTracks by mutableStateOf<List<Track>>(emptyList())
    var libPlaylists by mutableStateOf<List<HomeItem>>(emptyList())
    var libLoading by mutableStateOf(false)

    var nowPlaying by mutableStateOf<MediaMetadata?>(null)
    var playing by mutableStateOf(false)
    var shuffle by mutableStateOf(false)
    var repeat by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    var queue by mutableStateOf<List<MediaItem>>(emptyList())
    var queueIndex by mutableIntStateOf(0)
    var endless by mutableStateOf(true)
    var sleepMinutes by mutableIntStateOf(0)

    var lyrics by mutableStateOf<List<LyricLine>>(emptyList())
    var lyricsState by mutableStateOf("")

    private var controller: MediaController? = null
    private var extending = false
    private var sleepJob: Job? = null
    private var suggestJob: Job? = null
    private var lyricsFor: String? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playing = isPlaying
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) error = null
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
            maybeExtend()
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

    // endless queue: when the queue is about to run out, append the youtube music radio for the current song
    private fun maybeExtend() {
        val c = controller ?: return
        if (!endless || extending || c.repeatMode != Player.REPEAT_MODE_OFF) return
        if (c.mediaItemCount - c.currentMediaItemIndex > 3) return
        val seed = c.currentMediaItem?.mediaId ?: return
        extending = true
        viewModelScope.launch {
            try {
                val more = withContext(Dispatchers.IO) { Feed.queue("RDAMVM$seed", seed) }
                val have = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId }.toSet()
                val fresh = more.filter { it.id !in have }
                if (fresh.isNotEmpty()) c.addMediaItems(fresh.map { it.toItem() })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // try again on the next transition
            }
            extending = false
        }
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
            } catch (e: CancellationException) {
                throw e
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
        libTitle = null
        libPlaylists = emptyList()
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

    fun onQuery(q: String) {
        query = q
        suggestJob?.cancel()
        if (q.isBlank()) {
            suggestions = emptyList()
            return
        }
        suggestJob = viewModelScope.launch {
            delay(250)
            suggestions = try {
                withContext(Dispatchers.IO) { Feed.suggestions(q.trim()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun search(q: String = query) {
        if (q.isBlank()) return
        query = q
        suggestJob?.cancel()
        suggestions = emptyList()
        viewModelScope.launch {
            loading = true
            error = null
            try {
                results = withContext(Dispatchers.IO) { Extractor.search(q.trim()) }
                if (results.isEmpty()) error = "nothing found"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "search failed"
            }
            loading = false
        }
    }

    fun loadLibrary() {
        if (!loggedIn || libLoading) return
        viewModelScope.launch {
            libLoading = true
            try {
                libPlaylists = withContext(Dispatchers.IO) { Feed.playlists() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "library failed"
            }
            libLoading = false
        }
    }

    fun openLiked() = openList("Liked songs") { Feed.playlistSongs("LM").ifEmpty { Feed.queue("LM") } }

    fun openHistory() = openList("History") { Feed.history() }

    fun openPlaylist(item: HomeItem) {
        val id = item.playlistId ?: return
        openList(item.title) { Feed.playlistSongs(id).ifEmpty { Feed.queue(id) } }
    }

    private fun openList(title: String, load: () -> List<Track>) {
        libTitle = title
        libTracks = emptyList()
        libLoading = true
        viewModelScope.launch {
            try {
                libTracks = withContext(Dispatchers.IO) { load() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "couldn't load that"
            }
            libLoading = false
        }
    }

    fun closeList() {
        libTitle = null
    }

    fun playLib(index: Int) = playTracks(libTracks, index)

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
                } catch (e: CancellationException) {
                    throw e
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
        maybeExtend()
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

    fun cycleSleep() {
        val opts = listOf(0, 15, 30, 60)
        sleepMinutes = opts[(opts.indexOf(sleepMinutes) + 1) % opts.size]
        sleepJob?.cancel()
        if (sleepMinutes > 0) {
            val m = sleepMinutes
            sleepJob = viewModelScope.launch {
                delay(m * 60_000L)
                controller?.pause()
                sleepMinutes = 0
            }
        }
    }

    fun loadLyrics() {
        val meta = nowPlaying ?: return
        val title = meta.title?.toString() ?: return
        val artist = meta.artist?.toString() ?: ""
        val key = "$title|$artist"
        if (key == lyricsFor) return
        lyricsFor = key
        lyrics = emptyList()
        lyricsState = "loading"
        val secs = duration() / 1000
        viewModelScope.launch {
            val lines = try {
                withContext(Dispatchers.IO) { Lyrics.fetch(title, artist, secs) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            if (lyricsFor == key) {
                lyrics = lines
                lyricsState = if (lines.isEmpty()) "none" else ""
            }
        }
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
