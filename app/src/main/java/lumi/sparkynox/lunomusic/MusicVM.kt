package lumi.sparkynox.lunomusic

import android.app.Application
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import lumi.sparkynox.lunomusic.data.Downloads
import lumi.sparkynox.lunomusic.data.UpdateInfo
import lumi.sparkynox.lunomusic.data.Updater
import lumi.sparkynox.lunomusic.data.Stats
import lumi.sparkynox.lunomusic.extract.Chip
import lumi.sparkynox.lunomusic.extract.Extractor
import lumi.sparkynox.lunomusic.extract.Feed
import lumi.sparkynox.lunomusic.extract.HomeItem
import lumi.sparkynox.lunomusic.extract.HomeSection
import lumi.sparkynox.lunomusic.extract.InnerTube
import lumi.sparkynox.lunomusic.extract.Kind
import lumi.sparkynox.lunomusic.extract.LyricLine
import lumi.sparkynox.lunomusic.extract.Lyrics
import lumi.sparkynox.lunomusic.extract.SearchFilter
import lumi.sparkynox.lunomusic.extract.Track
import lumi.sparkynox.lunomusic.util.AppLog
import lumi.sparkynox.lunomusic.util.Prefs
import java.io.File
import java.io.IOException

// the small bar at the bottom: a message, with an Undo button when there is something to take back
class Notice(val message: String, val undo: (() -> Unit)? = null)

class MusicVM(app: Application) : AndroidViewModel(app) {
    var tab by mutableIntStateOf(0)
    var showLogin by mutableStateOf(false)
    var showPlayer by mutableStateOf(false)
    var loggedIn by mutableStateOf(Session.loggedIn)
    var pages by mutableStateOf<List<Page>>(emptyList())

    var home by mutableStateOf<List<HomeSection>>(emptyList())
    var homeCont by mutableStateOf<String?>(null)
    var homeLoading by mutableStateOf(false)
    var chips by mutableStateOf<List<Chip>>(emptyList())
    var chip by mutableStateOf<Chip?>(null)

    var query by mutableStateOf("")
    var filter by mutableStateOf(SearchFilter.SONGS)
    var suggestions by mutableStateOf<List<String>>(emptyList())
    var results by mutableStateOf<List<HomeItem>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    var libPlaylists by mutableStateOf<List<HomeItem>>(emptyList())
    var libLoading by mutableStateOf(false)
    var likedIds by mutableStateOf<Set<String>>(emptySet())

    // profile
    var accountName by mutableStateOf(Prefs.accountName)
    var accountHandle by mutableStateOf(Prefs.accountHandle)
    var accountEmail by mutableStateOf(Prefs.accountEmail)
    var accountPhoto by mutableStateOf(Prefs.accountPhoto)
    var customName by mutableStateOf(Prefs.customName)
    var customAvatar by mutableStateOf(Prefs.customAvatar)
    var bio by mutableStateOf(Prefs.bio)

    // in app updates
    var update by mutableStateOf<UpdateInfo?>(null)
    var updateProgress by mutableStateOf<Float?>(null)
    var updateStatus by mutableStateOf("")
    var updateChecking by mutableStateOf(false)

    var nowPlaying by mutableStateOf<MediaMetadata?>(null)
    var playing by mutableStateOf(false)
    var shuffle by mutableStateOf(false)
    var repeat by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    var queue by mutableStateOf<List<MediaItem>>(emptyList())
    var queueIndex by mutableIntStateOf(0)
    var endless by mutableStateOf(Prefs.endless)
        private set
    var sleepMinutes by mutableIntStateOf(0)
    var sleepEndOfSong by mutableStateOf(false)
    var queueLocked by mutableStateOf(false)
    var speed by mutableFloatStateOf(1f)
        private set

    // "not interested" songs: hidden from queue, home, search and the endless radio
    var blockedIds by mutableStateOf(Prefs.blockedIds)
        private set

    var lyrics by mutableStateOf<List<LyricLine>>(emptyList())
    var lyricsState by mutableStateOf("")

    // custom background behind the lyrics, an image or a video that plays without sound
    var lyricsBg by mutableStateOf(Prefs.lyricsBg)
        private set
    var lyricsBgVideo by mutableStateOf(Prefs.lyricsBgVideo)
        private set

    var notice by mutableStateOf<Notice?>(null)
        private set

    // song the "add to playlist" sheet is open for, null = sheet closed
    var playlistTarget by mutableStateOf<Track?>(null)
        private set

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
            if (sleepEndOfSong && reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                sleepEndOfSong = false
                controller?.pause()
                AppLog.i("VM", "sleep timer: song ended, paused")
            }
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
            AppLog.i("VM", "connected to player service, ${c.mediaItemCount} items in queue")
        }, ContextCompat.getMainExecutor(app))

        loadHome()
        loadLiked()
        loadAccount()
        if (Prefs.autoUpdate && System.currentTimeMillis() - Prefs.lastUpdateCheck > 6 * 3600_000L) checkUpdate(false)
    }

    private fun fail(tag: String, msg: String, e: Exception): String {
        AppLog.e(tag, msg, e)
        return e.message ?: msg
    }

    private fun syncQueue(c: MediaController) {
        queue = (0 until c.mediaItemCount).map { c.getMediaItemAt(it) }
        queueIndex = c.currentMediaItemIndex
    }

    // ---------- navigation

    fun push(p: Page) {
        pages = pages + p
    }

    fun pop() {
        if (pages.isNotEmpty()) pages = pages.dropLast(1)
    }

    // ---------- endless queue: when the queue is about to run out, append the youtube music radio for the current song

    fun setEndlessEnabled(v: Boolean) {
        endless = v
        Prefs.endless = v
        AppLog.i("VM", "endless queue ${if (v) "on" else "off"}")
    }

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
                val fresh = more.filter { it.id !in have && it.id !in blockedIds }
                if (fresh.isNotEmpty()) c.addMediaItems(fresh.map { it.toItem() })
                AppLog.i("Endless", "radio for $seed: ${more.size} songs, ${fresh.size} new added")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w("Endless", "couldn't extend the queue from $seed", e)
            }
            extending = false
        }
    }

    // ---------- not interested: block a song everywhere

    fun showNotice(message: String, undo: (() -> Unit)? = null) {
        notice = Notice(message, undo)
    }

    fun runUndo() {
        val n = notice ?: return
        notice = null
        n.undo?.invoke()
    }

    fun dismissNotice() {
        notice = null
    }

    fun notInterested(id: String, title: String) {
        if (id.isBlank()) return
        // remember what is about to disappear so Undo can put it all back
        val gone = ArrayList<Pair<Int, MediaItem>>()
        controller?.let { c ->
            for (i in 0 until c.mediaItemCount) if (c.getMediaItemAt(i).mediaId == id) gone += i to c.getMediaItemAt(i)
        }
        val homeBefore = home
        val homeContBefore = homeCont
        val resultsBefore = results
        val queryBefore = query

        blockedIds = blockedIds + id
        Prefs.blockedIds = blockedIds
        controller?.let { c -> gone.asReversed().forEach { (i, _) -> c.removeMediaItem(i) } }
        home = home.map { s -> s.copy(items = s.items.filter { it.videoId != id }) }
        results = results.filter { it.videoId != id }
        AppLog.i("VM", "not interested in '$title' ($id), ${blockedIds.size} hidden total")

        showNotice("Hidden: ${title.ifBlank { "song" }}") {
            blockedIds = blockedIds - id
            Prefs.blockedIds = blockedIds
            controller?.let { c ->
                gone.forEach { (i, item) -> c.addMediaItem(i.coerceAtMost(c.mediaItemCount), item) }
            }
            // only put the lists back when nothing was loaded on top of them in the meantime
            if (homeCont == homeContBefore) home = homeBefore
            if (query == queryBefore) results = resultsBefore
            AppLog.i("VM", "undo: '$title' ($id) is visible again")
        }
    }

    fun notInterested(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) {
            val m = c.getMediaItemAt(index)
            notInterested(m.mediaId, m.mediaMetadata.title?.toString() ?: "")
        }
    }

    fun clearHidden() {
        blockedIds = emptySet()
        Prefs.blockedIds = emptySet()
        AppLog.i("VM", "unhid all songs")
    }

    // ---------- home / login

    fun loadHome(more: Boolean = false) {
        if (homeLoading) return
        viewModelScope.launch {
            homeLoading = true
            error = null
            try {
                val page = withContext(Dispatchers.IO) {
                    Feed.home(if (more) homeCont else null, if (more) null else chip)
                }
                val sections = page.sections
                    .map { s -> s.copy(items = s.items.filter { it.videoId == null || it.videoId !in blockedIds }) }
                    .filter { it.items.isNotEmpty() }
                home = if (more) home + sections else sections
                homeCont = page.continuation
                if (!more && chip == null && page.chips.isNotEmpty()) chips = page.chips
                AppLog.i("Home", "loaded ${page.sections.size} sections, ${page.chips.size} chips (more=$more, chip=${chip?.title})")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = fail("Home", "couldn't load the home feed", e)
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
        libPlaylists = emptyList()
        likedIds = emptySet()
        loadHome()
        loadLiked()
    }

    fun onLoginDone() {
        AppLog.i("Login", "login finished, cookie saved")
        showLogin = false
        loggedIn = true
        resetHome()
        loadAccount()
    }

    fun logout() {
        AppLog.i("Login", "logged out")
        Session.logout()
        loggedIn = false
        accountName = ""; accountHandle = ""; accountEmail = ""; accountPhoto = ""
        Prefs.accountName = ""; Prefs.accountHandle = ""; Prefs.accountEmail = ""; Prefs.accountPhoto = ""
        resetHome()
    }

    private fun loadLiked() {
        if (!Session.loggedIn) return
        viewModelScope.launch {
            try {
                likedIds = withContext(Dispatchers.IO) { Feed.playlistSongs("LM").map { it.id }.toSet() }
                AppLog.i("Likes", "${likedIds.size} liked songs loaded")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w("Likes", "couldn't load liked songs", e)
            }
        }
    }

    fun toggleLike(id: String) {
        if (!loggedIn) {
            showLogin = true
            return
        }
        val remove = id in likedIds
        likedIds = if (remove) likedIds - id else likedIds + id
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { InnerTube.like(id, remove) }
                AppLog.i("Likes", "${if (remove) "unliked" else "liked"} $id")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                likedIds = if (remove) likedIds + id else likedIds - id
                error = fail("Likes", "couldn't ${if (remove) "unlike" else "like"} $id", e)
            }
        }
    }

    // ---------- search

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
                AppLog.w("Search", "suggestions failed for '$q'", e)
                emptyList()
            }
        }
    }

    fun selectFilter(f: SearchFilter) {
        filter = f
        if (query.isNotBlank()) search()
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
                val found = withContext(Dispatchers.IO) { Extractor.search(q.trim(), filter) }
                results = found.filter { it.videoId == null || it.videoId !in blockedIds }
                if (results.isEmpty()) error = "nothing found"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = fail("Search", "search '$q' [${filter.label}] failed", e)
            }
            loading = false
        }
    }

    fun openResult(item: HomeItem) {
        when (item.kind) {
            Kind.SONG -> playSong(item.toTrack())
            else -> openItem(item)
        }
    }

    // ---------- pages

    fun openItem(item: HomeItem) {
        when (item.kind) {
            Kind.ALBUM, Kind.PLAYLIST -> openDetail(item)
            Kind.ARTIST -> item.browseId?.let { openArtist(it, item.title, item.thumb) }
            Kind.SONG -> Unit
        }
    }

    private fun openDetail(item: HomeItem) {
        val id = item.playlistId ?: return
        val page = DetailPage(item.title, item.subtitle, item.thumb, numbered = true)
        push(page)
        loadPage(page, "Detail ${item.title}") { Feed.playlistSongs(id).ifEmpty { Feed.queue(id) } }
    }

    fun openLiked() {
        val page = DetailPage("Liked songs", "Your likes", null)
        push(page)
        loadPage(page, "Liked songs") { Feed.playlistSongs("LM").ifEmpty { Feed.queue("LM") }.also { l -> likedIds = l.map { it.id }.toSet() } }
    }

    fun openHistory() {
        val page = DetailPage("History", "Recently played", null)
        push(page)
        loadPage(page, "History") { history() }
    }

    // what was played on this device first, then what the youtube music account has, every song once
    private fun history(): List<Track> {
        val local = Stats.recent(200)
        val remote = if (Session.loggedIn) {
            runCatching { Feed.history() }
                .onFailure { AppLog.w("History", "couldn't load the youtube music history", it) }
                .getOrDefault(emptyList())
        } else emptyList()
        val seen = HashSet<String>()
        return (local + remote).filter { seen.add(it.id) }
    }

    fun openDownloads() {
        val page = DetailPage("Downloads", "Available offline", null, isDownloads = true)
        push(page)
        loadPage(page, "Downloads") { Downloads.all() }
    }

    fun openTop50() {
        val page = DetailPage("My top 50", "Your most played, all time", null)
        push(page)
        loadPage(page, "Top 50") { Stats.topTracks(50) }
    }

    private fun loadPage(page: DetailPage, tag: String, load: () -> List<Track>) {
        viewModelScope.launch {
            try {
                page.tracks = withContext(Dispatchers.IO) { load() }
                    .filter { it.id !in blockedIds }
                AppLog.i("Page", "$tag: ${page.tracks.size} tracks")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                page.error = fail("Page", "$tag failed to load", e)
            }
            page.loading = false
        }
    }

    fun openArtist(id: String, name: String, thumb: String?) {
        val page = ArtistPage(id, name, thumb)
        push(page)
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) { Feed.artist(id) }
                if (data.name.isNotBlank()) page.name = data.name
                data.thumb?.let { page.thumb = it }
                page.description = data.description
                page.songs = data.songs.filter { it.id !in blockedIds }
                page.sections = data.sections
                AppLog.i("Artist", "${page.name}: ${data.songs.size} songs, ${data.sections.size} sections")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                page.error = fail("Artist", "artist $id failed to load", e)
            }
            page.loading = false
        }
    }

    fun deleteDownload(page: DetailPage, t: Track) {
        Downloads.delete(t.id)
        page.tracks = page.tracks.filter { it.id != t.id }
    }

    fun loadLibrary() {
        if (!loggedIn || libLoading) return
        viewModelScope.launch {
            libLoading = true
            try {
                libPlaylists = withContext(Dispatchers.IO) { Feed.playlists() }
                AppLog.i("Library", "${libPlaylists.size} playlists")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = fail("Library", "couldn't load your playlists", e)
            }
            libLoading = false
        }
    }

    // ---------- playback

    fun playItem(section: HomeSection, item: HomeItem) {
        when (item.kind) {
            Kind.SONG -> playSong(item.toTrack())
            else -> openItem(item)
        }
    }

    fun playSection(section: HomeSection) {
        val songs = section.items.filter { it.kind == Kind.SONG && it.videoId != null }
        if (songs.isNotEmpty()) playTracks(songs.map { it.toTrack() }, 0)
    }

    fun playSong(t: Track) = playTracks(listOf(t), 0)

    fun playTracks(tracks: List<Track>, index: Int, shuffled: Boolean = false) {
        val c = controller ?: run {
            AppLog.w("VM", "play requested but the player service isn't connected yet")
            return
        }
        if (tracks.isEmpty()) return
        val playable = tracks.filter { it.id !in blockedIds }
        if (playable.isEmpty()) {
            error = "nothing to play"
            return
        }
        val start = playable.indexOf(tracks[index.coerceIn(0, tracks.lastIndex)]).coerceAtLeast(0)
        error = null
        AppLog.i("VM", "play ${playable.size} tracks from #$start: '${playable[start].title}'")
        if (shuffled) c.shuffleModeEnabled = true
        c.setMediaItems(playable.map { it.toItem() }, if (shuffled) playable.indices.random() else start, 0L)
        c.prepare()
        c.play()
        maybeExtend()
    }

    fun playRadio(seed: String) {
        viewModelScope.launch {
            try {
                val q = withContext(Dispatchers.IO) { Feed.queue("RDAMVM$seed", seed) }
                    .filter { it.id !in blockedIds }
                if (q.isNotEmpty()) playTracks(q, 0) else error = "no radio for this one"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = fail("Radio", "couldn't start the radio for $seed", e)
            }
        }
    }

    fun playNext(t: Track) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) playTracks(listOf(t), 0) else c.addMediaItem(c.currentMediaItemIndex + 1, t.toItem())
        AppLog.i("VM", "play next: '${t.title}'")
    }

    fun addToQueue(t: Track) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) playTracks(listOf(t), 0) else c.addMediaItem(t.toItem())
        AppLog.i("VM", "added to queue: '${t.title}'")
    }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        val item = c.getMediaItemAt(index)
        val title = item.mediaMetadata.title?.toString() ?: ""
        AppLog.i("VM", "removed #$index '$title' from the queue")
        c.removeMediaItem(index)
        showNotice("Removed from queue") {
            controller?.let { q -> q.addMediaItem(index.coerceAtMost(q.mediaItemCount), item) }
            AppLog.i("VM", "undo: '$title' is back in the queue at #$index")
        }
    }

    // ---------- add to playlist

    fun openAddToPlaylist(t: Track) {
        if (!loggedIn) {
            showLogin = true
            return
        }
        playlistTarget = t
        loadLibrary()
    }

    fun closeAddToPlaylist() {
        playlistTarget = null
    }

    fun addToPlaylist(playlist: HomeItem) {
        val t = playlistTarget ?: return
        val pid = playlist.playlistId ?: return
        playlistTarget = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { InnerTube.addToPlaylist(pid, t.id) }
                AppLog.i("Playlist", "'${t.title}' added to '${playlist.title}'")
                showNotice("Saved to ${playlist.title}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("Playlist", "couldn't add '${t.title}' to '${playlist.title}'", e)
                showNotice("Couldn't add to ${playlist.title}")
            }
        }
    }

    fun createPlaylistWith(name: String) {
        val t = playlistTarget ?: return
        val title = name.trim().take(100)
        if (title.isEmpty()) return
        playlistTarget = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { InnerTube.createPlaylist(title, t.id) }
                AppLog.i("Playlist", "created '$title' with '${t.title}'")
                showNotice("Saved to new playlist $title")
                libPlaylists = emptyList()
                loadLibrary()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("Playlist", "couldn't create '$title'", e)
                showNotice("Couldn't create the playlist")
            }
        }
    }

    fun clearQueue() {
        val c = controller ?: return
        if (queueLocked) return
        AppLog.i("VM", "queue cleared")
        c.stop()
        c.clearMediaItems()
        queue = emptyList()
        queueIndex = 0
    }

    // clears only what comes after the current song
    fun clearUpcoming() {
        val c = controller ?: return
        if (queueLocked) return
        val from = c.currentMediaItemIndex + 1
        if (from < c.mediaItemCount) c.removeMediaItems(from, c.mediaItemCount)
        AppLog.i("VM", "upcoming songs cleared")
    }

    fun download(t: Track) = Downloads.start(t)

    fun currentTrack(): Track? {
        val item = controller?.currentMediaItem ?: return null
        val m = item.mediaMetadata
        return Track(item.mediaId, m.title?.toString() ?: "", m.artist?.toString() ?: "", m.artworkUri?.toString(), 0L, m.extras?.getString("artistId"))
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

    // sleep timer dialog: minutes > 0 starts a countdown, endOfSong stops after the current song, both off = reset
    fun setSleep(minutes: Int, endOfSong: Boolean) {
        sleepJob?.cancel()
        sleepMinutes = if (endOfSong) 0 else minutes
        sleepEndOfSong = endOfSong
        AppLog.i("VM", "sleep timer: ${if (endOfSong) "end of song" else "$minutes min"}")
        if (!endOfSong && minutes > 0) {
            sleepJob = viewModelScope.launch {
                delay(minutes * 60_000L)
                controller?.pause()
                sleepMinutes = 0
                AppLog.i("VM", "sleep timer fired, paused")
            }
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        if (queueLocked || from !in 0 until c.mediaItemCount || to !in 0 until c.mediaItemCount) return
        c.moveMediaItem(from, to)
        syncQueue(c)
    }

    fun changeSpeed(v: Float) {
        controller?.setPlaybackSpeed(v)
        speed = v
    }

    fun cycleSleep() {
        val opts = listOf(0, 15, 30, 60)
        sleepMinutes = opts[(opts.indexOf(sleepMinutes) + 1) % opts.size]
        sleepJob?.cancel()
        AppLog.i("VM", "sleep timer: $sleepMinutes min")
        if (sleepMinutes > 0) {
            val m = sleepMinutes
            sleepJob = viewModelScope.launch {
                delay(m * 60_000L)
                controller?.pause()
                sleepMinutes = 0
                AppLog.i("VM", "sleep timer fired, paused")
            }
        }
    }

    fun setLyricsBg(uri: Uri?) {
        val app = getApplication<Application>()
        val old = lyricsBg
        if (uri == null) {
            applyLyricsBg("", false, old)
            return
        }
        val type = app.contentResolver.getType(uri) ?: ""
        val video = type.startsWith("video/")
        if (!video && !type.startsWith("image/")) {
            showNotice("Pick an image or a video")
            return
        }
        viewModelScope.launch {
            try {
                val stored = withContext(Dispatchers.IO) {
                    try {
                        app.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        uri.toString()
                    } catch (e: Exception) {
                        // the file provider won't let us keep access to it, so keep our own copy
                        AppLog.w("LyricsBg", "can't keep access to the picked file, copying it", e)
                        val copy = File(app.filesDir, "lyrics_bg_${System.currentTimeMillis()}")
                        val input = app.contentResolver.openInputStream(uri) ?: throw IOException("can't open the picked file")
                        input.use { i -> copy.outputStream().use { o -> i.copyTo(o) } }
                        Uri.fromFile(copy).toString()
                    }
                }
                applyLyricsBg(stored, video, old)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("LyricsBg", "couldn't use the picked file", e)
                showNotice("Couldn't use that file")
            }
        }
    }

    private fun applyLyricsBg(stored: String, video: Boolean, old: String) {
        Prefs.lyricsBg = stored
        Prefs.lyricsBgVideo = video
        lyricsBg = stored
        lyricsBgVideo = video
        // our own copy of the previous file isn't needed any more, access to a picked one can be given back
        if (old.isNotEmpty() && old != stored) {
            val u = Uri.parse(old)
            if (u.scheme == "file") u.path?.let { File(it).delete() }
            else runCatching {
                getApplication<Application>().contentResolver.releasePersistableUriPermission(u, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        AppLog.i("LyricsBg", if (stored.isEmpty()) "removed" else "set, ${if (video) "video" else "image"}")
    }

    fun lyricsBgFailed() {
        if (lyricsBg.isEmpty()) return
        AppLog.w("LyricsBg", "couldn't load $lyricsBg, back to the normal background")
        applyLyricsBg("", false, lyricsBg)
        showNotice("Couldn't load the lyrics background")
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
                AppLog.e("Lyrics", "lookup failed for '$title'", e)
                emptyList()
            }
            if (lyricsFor == key) {
                lyrics = lines
                lyricsState = if (lines.isEmpty()) "none" else ""
            }
        }
    }

    // ---------- profile

    fun loadAccount() {
        if (!Session.loggedIn) return
        viewModelScope.launch {
            try {
                val a = withContext(Dispatchers.IO) { Feed.account() } ?: return@launch
                accountName = a.name; accountHandle = a.handle ?: ""; accountEmail = a.email ?: ""; accountPhoto = a.photo ?: ""
                Prefs.accountName = accountName; Prefs.accountHandle = accountHandle
                Prefs.accountEmail = accountEmail; Prefs.accountPhoto = accountPhoto
                AppLog.i("Profile", "account loaded: ${a.name} ${a.handle ?: ""}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w("Profile", "couldn't load the account info", e)
            }
        }
    }

    fun updateBio(v: String) {
        bio = v.take(200)
        Prefs.bio = bio
    }

    fun updateCustomName(v: String) {
        customName = v.trim().take(40)
        Prefs.customName = customName
    }

    fun setAvatarFrom(uri: Uri) {
        viewModelScope.launch {
            try {
                val path = withContext(Dispatchers.IO) { saveAvatar(uri) }
                if (customAvatar.isNotBlank()) File(customAvatar).delete()
                customAvatar = path
                Prefs.customAvatar = path
                AppLog.i("Profile", "new profile photo saved")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = fail("Profile", "couldn't use that photo", e)
            }
        }
    }

    fun resetAvatar() {
        if (customAvatar.isNotBlank()) File(customAvatar).delete()
        customAvatar = ""
        Prefs.customAvatar = ""
    }

    private fun saveAvatar(uri: Uri): String {
        val app = getApplication<Application>()
        val cr = app.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        val bmp = cr.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IOException("couldn't read that image")
        val out = File(app.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return out.absolutePath
    }

    // ---------- updates

    fun checkUpdate(manual: Boolean) {
        if (updateChecking) return
        viewModelScope.launch {
            updateChecking = true
            if (manual) updateStatus = "Checking..."
            try {
                val cur = Updater.currentVersion(getApplication())
                val info = withContext(Dispatchers.IO) { Updater.check(cur) }
                Prefs.lastUpdateCheck = System.currentTimeMillis()
                when {
                    info == null -> updateStatus = "You're on the latest version ($cur)"
                    !manual && info.version == Prefs.skippedVersion -> {
                        updateStatus = "Update v${info.version} available (skipped)"
                    }
                    else -> {
                        update = info
                        updateStatus = "Update available: v${info.version}"
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w("Update", "update check failed", e)
                if (manual) updateStatus = "Couldn't check for updates (${e.message})"
            }
            updateChecking = false
        }
    }

    fun startUpdate() {
        val info = update ?: return
        if (updateProgress != null) return
        viewModelScope.launch {
            updateProgress = 0f
            try {
                val apk = withContext(Dispatchers.IO) { Updater.download(getApplication(), info) { p -> updateProgress = p } }
                updateProgress = null
                if (!Updater.install(getApplication(), apk)) {
                    updateStatus = "Allow installs from LunoMusic in the settings page that opened, then tap Update again"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateProgress = null
                updateStatus = "Download failed (${e.message})"
                AppLog.e("Update", "download or install failed", e)
            }
        }
    }

    fun dismissUpdate(skip: Boolean) {
        if (skip) Prefs.skippedVersion = update?.version ?: ""
        update = null
    }

    override fun onCleared() {
        controller?.release()
    }

    private fun HomeItem.toTrack() = Track(videoId!!, title, artist, thumb, 0L, artistId)

    private fun Track.toItem() = MediaItem.Builder()
        .setMediaId(id)
        .setUri("luno://track/$id")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(thumb?.let { Uri.parse(it) })
                .setExtras(Bundle().apply { putString("artistId", artistId) })
                .build()
        )
        .build()
}
