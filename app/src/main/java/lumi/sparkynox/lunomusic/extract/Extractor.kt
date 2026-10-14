package lumi.sparkynox.lunomusic.extract

import android.util.Log
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.ServiceList
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
)

object Extractor {
    // flip this if yt-dlp turns out faster than the pipe extractor
    private const val YTDLP_FIRST = false
    private const val CACHE_MS = 5 * 60 * 60 * 1000L

    private class Cached(val url: String, val at: Long, val src: Int)

    private val cache = ConcurrentHashMap<String, Cached>()
    private val avoid = ConcurrentHashMap<String, Int>()

    // call when a url turned out dead, next resolve tries the other extractor first
    fun invalidate(id: String) {
        cache.remove(id)?.let { avoid[id] = it.src }
    }

    fun search(query: String): List<Track> {
        val yt = ServiceList.YouTube
        val songs = yt.searchQHFactory.getFilterItem(YoutubeFilters.ID_CF_MAIN_YOUTUBE_MUSIC_SONGS)
        val q = yt.searchQHFactory.fromQuery(query, listOf(songs), emptyList())
        return SearchInfo.getInfo(yt, q).relatedItems
            .filterIsInstance<StreamInfoItem>()
            .mapNotNull { item ->
                val id = runCatching { YoutubeStreamLinkHandlerFactory.getInstance().getId(item.url) }.getOrNull()
                    ?: return@mapNotNull null
                Track(id, item.name, item.uploaderName ?: "", item.thumbnails.lastOrNull()?.url, item.duration)
            }
    }

    fun resolve(id: String): String {
        cache[id]?.let { if (System.currentTimeMillis() - it.at < CACHE_MS) return it.url }

        // 0 = pipe extractor, 1 = yt-dlp
        var order = if (YTDLP_FIRST) listOf(1, 0) else listOf(0, 1)
        avoid[id]?.let { bad -> order = order.filter { it != bad } + bad }

        var last: Throwable? = null
        for (src in order) {
            try {
                val url = (if (src == 0) viaPipe(id) else viaYtDlp(id)) ?: continue
                cache[id] = Cached(url, System.currentTimeMillis(), src)
                return url
            } catch (e: Throwable) {
                Log.w("Extractor", "source $src failed for $id", e)
                last = e
            }
        }
        throw IOException("no stream for $id", last)
    }

    private fun viaPipe(id: String): String? {
        val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$id")
        val all = info.audioStreams.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl }
        val m4a = all.filter { it.format == MediaFormat.M4A }
        return (m4a.ifEmpty { all }).maxByOrNull { it.averageBitrate }?.content
    }

    private fun viaYtDlp(id: String): String? = PyYtDlp.audioUrl(id)
}
