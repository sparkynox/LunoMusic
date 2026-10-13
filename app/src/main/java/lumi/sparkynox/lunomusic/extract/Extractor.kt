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

    private val cache = ConcurrentHashMap<String, Pair<String, Long>>()

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
        cache[id]?.let { (url, at) -> if (System.currentTimeMillis() - at < CACHE_MS) return url }

        val chain = listOf<(String) -> String?>({ viaPipe(it) }, { viaYtDlp(it) })
            .let { if (YTDLP_FIRST) it.reversed() else it }

        var last: Throwable? = null
        for (step in chain) {
            try {
                val url = step(id) ?: continue
                cache[id] = url to System.currentTimeMillis()
                return url
            } catch (e: Throwable) {
                Log.w("Extractor", "step failed for $id", e)
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
