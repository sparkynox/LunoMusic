package lumi.sparkynox.lunomusic

import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import lumi.sparkynox.lunomusic.data.Downloads
import lumi.sparkynox.lunomusic.data.Stats
import lumi.sparkynox.lunomusic.extract.Extractor
import lumi.sparkynox.lunomusic.util.AppLog

class PlayerService : MediaSessionService() {
    private var session: MediaSession? = null
    private val retries = mutableMapOf<String, Int>()

    // listening stats for the current item
    private var curItem: MediaItem? = null
    private var playStart = 0L
    private var listened = 0L

    override fun onCreate() {
        super.onCreate()

        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)

        // items are luno://track/<id>: downloaded file if we have one, otherwise the real url is looked up right when playback needs it
        val resolving = ResolvingDataSource.Factory(DefaultDataSource.Factory(this, http)) { spec ->
            if (spec.uri.scheme == "luno") {
                val id = spec.uri.lastPathSegment!!
                val local = Downloads.file(id)
                if (local.exists()) {
                    AppLog.d("Player", "$id: playing from downloads")
                    spec.withUri(Uri.fromFile(local))
                } else {
                    spec.withUri(Uri.parse(Extractor.resolve(id)))
                }
            } else spec
        }

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolving))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val name = when (state) {
                    Player.STATE_IDLE -> "idle"
                    Player.STATE_BUFFERING -> "buffering"
                    Player.STATE_READY -> "ready"
                    else -> "ended"
                }
                AppLog.d("Player", "state=$name item=${player.currentMediaItem?.mediaId}")
                if (state == Player.STATE_READY) player.currentMediaItem?.mediaId?.let { retries.remove(it) }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    playStart = SystemClock.elapsedRealtime()
                } else if (playStart > 0) {
                    listened += SystemClock.elapsedRealtime() - playStart
                    playStart = 0
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                commitStats()
                curItem = mediaItem
                listened = 0
                playStart = if (player.isPlaying) SystemClock.elapsedRealtime() else 0
                AppLog.i("Player", "now playing ${mediaItem?.mediaId} '${mediaItem?.mediaMetadata?.title}' (reason=$reason, queue=${player.mediaItemCount})")
            }

            // dead/expired url: drop it, resolve again (other extractor goes first) and carry on from the same spot
            override fun onPlayerError(error: PlaybackException) {
                val id = player.currentMediaItem?.mediaId
                AppLog.e("Player", "error ${error.errorCodeName} (${error.errorCode}) on $id at ${player.currentPosition}ms: ${error.message}", error)
                if (id == null) return
                val tries = retries[id] ?: 0
                if (error.errorCode in 2000..2999 && tries < 2) {
                    retries[id] = tries + 1
                    AppLog.w("Player", "$id: io error, retry ${tries + 1}/2 with a fresh url")
                    Extractor.invalidate(id)
                    player.seekTo(player.currentMediaItemIndex, player.currentPosition)
                    player.prepare()
                }
            }
        })

        session = MediaSession.Builder(this, player).build()
    }

    private fun commitStats() {
        val item = curItem ?: return
        var total = listened
        if (playStart > 0) total += SystemClock.elapsedRealtime() - playStart
        if (total >= 15_000) {
            val m = item.mediaMetadata
            runCatching {
                Stats.record(item.mediaId, m.title?.toString() ?: "", m.artist?.toString() ?: "", m.artworkUri?.toString(), total)
            }.onFailure { AppLog.e("Stats", "couldn't record play", it) }
        }
        listened = 0
        if (playStart > 0) playStart = SystemClock.elapsedRealtime()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        commitStats()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
