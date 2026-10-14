package lumi.sparkynox.lunomusic.extract

import android.os.SystemClock
import lumi.sparkynox.lunomusic.util.AppLog
import lumi.sparkynox.lunomusic.util.Prefs
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.search.filter.YoutubeFilters
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val thumb: String?,
    val seconds: Long,
    val artistId: String? = null,
)

enum class SearchFilter(val label: String, val id: Int) {
    SONGS("Songs", YoutubeFilters.ID_CF_MAIN_YOUTUBE_MUSIC_SONGS),
    ALBUMS("Albums", YoutubeFilters.ID_CF_MAIN_YOUTUBE_MUSIC_ALBUMS),
    PLAYLISTS("Playlists", YoutubeFilters.ID_CF_MAIN_YOUTUBE_MUSIC_PLAYLISTS),
    ARTISTS("Artists", YoutubeFilters.ID_CF_MAIN_YOUTUBE_MUSIC_ARTISTS),
    VIDEOS("Videos", YoutubeFilters.ID_CF_MAIN_YOUTUBE_MUSIC_VIDEOS),
}

object Extractor {
    private const val CACHE_MS = 5 * 60 * 60 * 1000L

    private class Cached(val url: String, val at: Long, val src: Int)

    private val cache = ConcurrentHashMap<String, Cached>()
    private val avoid = ConcurrentHashMap<String, Int>()

    // call when a url turned out dead, next resolve tries the other extractor first
    fun invalidate(id: String) {
        cache.remove(id)?.let {
            avoid[id] = it.src
            AppLog.w("Resolve", "$id: dropped cached url from ${srcName(it.src)}, will prefer the other one")
        }
    }

    fun clearCache() {
        cache.clear()
        avoid.clear()
        AppLog.i("Resolve", "url cache cleared")
    }

    fun search(query: String, filter: SearchFilter = SearchFilter.SONGS): List<HomeItem> {
        val t0 = SystemClock.elapsedRealtime()
        val yt = ServiceList.YouTube
        val f = yt.searchQHFactory.getFilterItem(filter.id)
        val q = yt.searchQHFactory.fromQuery(query, listOf(f), emptyList())
        val out = SearchInfo.getInfo(yt, q).relatedItems.mapNotNull { map(it) }
        AppLog.i("Search", "'$query' [${filter.label}] -> ${out.size} results in ${SystemClock.elapsedRealtime() - t0}ms")
        return out
    }

    private fun map(item: InfoItem): HomeItem? {
        val thumb = item.thumbnails.lastOrNull()?.url
        when (item) {
            is StreamInfoItem -> {
                val id = runCatching { YoutubeStreamLinkHandlerFactory.getInstance().getId(item.url) }.getOrNull() ?: return null
                val sub = listOfNotNull(item.uploaderName, if (item.duration > 0) "%d:%02d".format(item.duration / 60, item.duration % 60) else null)
                return HomeItem(Kind.SONG, item.name, sub.joinToString(" • "), item.uploaderName ?: "", thumb, id, null, null, channelId(item.uploaderUrl))
            }
            is PlaylistInfoItem -> {
                val pid = item.url?.substringAfter("list=", "")?.substringBefore("&")?.ifEmpty { null } ?: return null
                val kind = if (pid.startsWith("OLAK5uy")) Kind.ALBUM else Kind.PLAYLIST
                val sub = listOfNotNull(item.uploaderName, if (item.streamCount > 0) "${item.streamCount} tracks" else null)
                return HomeItem(kind, item.name, sub.joinToString(" • "), item.uploaderName ?: "", thumb, null, pid, null, channelId(item.uploaderUrl))
            }
            is ChannelInfoItem -> {
                val cid = channelId(item.url) ?: return null
                return HomeItem(Kind.ARTIST, item.name, "Artist", item.name, thumb, null, null, cid, cid)
            }
            else -> return null
        }
    }

    private fun channelId(url: String?): String? =
        url?.substringAfter("/channel/", "")?.substringBefore("?")?.takeIf { it.startsWith("UC") }

    private fun srcName(src: Int) = if (src == 0) "pipe extractor" else "yt-dlp"

    fun resolve(id: String): String {
        cache[id]?.let { if (System.currentTimeMillis() - it.at < CACHE_MS) return it.url }

        // 0 = pipe extractor, 1 = yt-dlp
        var order = if (Prefs.ytdlpFirst) listOf(1, 0) else listOf(0, 1)
        avoid[id]?.let { bad -> order = order.filter { it != bad } + bad }
        AppLog.i("Resolve", "$id: start, order=${order.joinToString { srcName(it) }}")

        var last: Throwable? = null
        for (src in order) {
            val t0 = SystemClock.elapsedRealtime()
            try {
                val url = (if (src == 0) viaPipe(id) else viaYtDlp(id))
                if (url == null) {
                    AppLog.w("Resolve", "$id: ${srcName(src)} returned nothing")
                    continue
                }
                cache[id] = Cached(url, System.currentTimeMillis(), src)
                AppLog.i("Resolve", "$id: ok via ${srcName(src)} in ${SystemClock.elapsedRealtime() - t0}ms")
                return url
            } catch (e: Throwable) {
                AppLog.w("Resolve", "$id: ${srcName(src)} failed after ${SystemClock.elapsedRealtime() - t0}ms", e)
                last = e
            }
        }
        AppLog.e("Resolve", "$id: every source failed")
        throw IOException("no stream for $id", last)
    }

    private fun viaPipe(id: String): String? {
        val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$id")
        val all = info.audioStreams.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl }
        val m4a = all.filter { it.format == MediaFormat.M4A }
        val best = (m4a.ifEmpty { all }).maxByOrNull { it.averageBitrate }
        AppLog.d("Resolve", "$id: pipe found ${info.audioStreams.size} audio streams, ${all.size} progressive, picked ${best?.format?.name} ${best?.averageBitrate}kbps")
        return best?.content
    }

    private fun viaYtDlp(id: String): String? = PyYtDlp.audioUrl(id)
}
