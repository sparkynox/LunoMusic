package lumi.sparkynox.lunomusic

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import lumi.sparkynox.lunomusic.extract.Extractor

class PlayerService : MediaSessionService() {
    private var session: MediaSession? = null
    private val retries = mutableMapOf<String, Int>()

    override fun onCreate() {
        super.onCreate()

        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)

        // items are luno://track/<id>, real url gets looked up right when playback needs it
        val resolving = ResolvingDataSource.Factory(http) { spec ->
            if (spec.uri.scheme == "luno") {
                spec.withUri(Uri.parse(Extractor.resolve(spec.uri.lastPathSegment!!)))
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
                if (state == Player.STATE_READY) player.currentMediaItem?.mediaId?.let { retries.remove(it) }
            }

            // dead/expired url: drop it, resolve again (other extractor goes first) and carry on from the same spot
            override fun onPlayerError(error: PlaybackException) {
                val id = player.currentMediaItem?.mediaId ?: return
                val tries = retries[id] ?: 0
                if (error.errorCode in 2000..2999 && tries < 2) {
                    retries[id] = tries + 1
                    Extractor.invalidate(id)
                    player.seekTo(player.currentMediaItemIndex, player.currentPosition)
                    player.prepare()
                }
            }
        })

        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
