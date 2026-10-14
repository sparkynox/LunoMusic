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
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lumi.sparkynox.lunomusic.auth.Session
import lumi.sparkynox.lunomusic.extract.Extractor
import lumi.sparkynox.lunomusic.extract.Feed
import lumi.sparkynox.lunomusic.extract.HomeItem
import lumi.sparkynox.lunomusic.extract.HomeSection
import lumi.sparkynox.lunomusic.extract.Kind
import lumi.sparkynox.lunomusic.extract.Track

class MusicVM(app: Application) : AndroidViewModel(app) {
    var tab by mutableIntStateOf(0)
    var showLogin by mutableStateOf(false)
    var loggedIn by mutableStateOf(Session.loggedIn)

    var home by mutableStateOf<List<HomeSection>>(emptyList())
    var homeCont by mutableStateOf<String?>(null)
    var homeLoading by mutableStateOf(false)

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

        loadHome()
    }

    fun loadHome(more: Boolean = false) {
        if (homeLoading) return
        viewModelScope.launch {
            homeLoading = true
            error = null
            try {
                val page = withContext(Dispatchers.IO) { Feed.home(if (more) homeCont else null) }
                home = if (more) home + page.sections else page.sections
                homeCont = page.continuation
            } catch (e: Exception) {
                error = e.message ?: "home failed"
            }
            homeLoading = false
        }
    }

    fun onLoginDone() {
        showLogin = false
        loggedIn = true
        home = emptyList()
        homeCont = null
        loadHome()
    }

    fun logout() {
        Session.logout()
        loggedIn = false
        home = emptyList()
        homeCont = null
        loadHome()
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
