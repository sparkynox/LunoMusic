package lumi.sparkynox.lunomusic.extract

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/*
 * Word level lyrics from the community LyricsPlus (KPoe) servers, same idea as Echo Music's YouLyPlus client
 * (GPL-3.0, github.com/iad1tya/Echo-Music): ask every server at once and take the first usable answer.
 */
object YouLy {
    private val servers = listOf(
        "https://lyricsplus.prjktla.my.id",
        "https://lyricsplus.atomix.one",
        "https://lyricsplus.binimum.org",
        "https://lyricsplus.prjktla.workers.dev",
        "https://lyricsplus-seven.vercel.app",
        "https://lyrics-plus-backend.vercel.app",
    )
    private val lastGood = AtomicReference<String?>(null)
    private val http = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
    private val pool = Executors.newCachedThreadPool { r -> Thread(r, "luno-lyrics").apply { isDaemon = true } }

    fun fetch(title: String, artist: String, seconds: Long): List<LyricLine> {
        val order = lastGood.get()?.let { g -> listOf(g) + servers.filter { it != g } } ?: servers
        val results = LinkedBlockingQueue<Pair<String, List<LyricLine>>>()
        order.forEach { srv ->
            pool.execute {
                val lines = runCatching { ask(srv, title, artist, seconds) }.getOrDefault(emptyList())
                results.add(srv to lines)
            }
        }
        val deadline = System.currentTimeMillis() + 6000
        var got = 0
        while (got < order.size) {
            val left = deadline - System.currentTimeMillis()
            if (left <= 0) break
            val (srv, lines) = results.poll(left, TimeUnit.MILLISECONDS) ?: break
            got++
            if (lines.isNotEmpty()) {
                lastGood.set(srv)
                return lines
            }
        }
        return emptyList()
    }

    private fun ask(server: String, title: String, artist: String, seconds: Long): List<LyricLine> {
        val url = "$server/v2/lyrics/get".toHttpUrl().newBuilder()
            .addQueryParameter("title", title)
            .addQueryParameter("artist", artist)
            .apply { if (seconds > 0) addQueryParameter("duration", seconds.toString()) }
            .build()
        val body = http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (r.isSuccessful) r.body?.string() else null
        } ?: return emptyList()
        val json = JSONObject(body)

        val arr = json.optJSONArray("lyrics")
        if (arr != null && arr.length() > 0) {
            val out = ArrayList<LyricLine>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val time = o.optLong("time", -1)
                if (time < 0) continue
                val syl = o.optJSONArray("syllabus")
                var words: List<LyricWord>? = null
                var text = o.optString("text")
                if (syl != null && syl.length() > 0) {
                    // KPoe syllables, glue them into words (a word ends where a syllable ends with a space)
                    val list = ArrayList<LyricWord>()
                    var sb = StringBuilder(); var start = -1L; var end = 0L
                    for (j in 0 until syl.length()) {
                        val sy = syl.optJSONObject(j) ?: continue
                        if (sy.optBoolean("isBackground", false)) continue
                        val st = sy.optLong("time", time)
                        val en = st + sy.optLong("duration", 0)
                        val tx = sy.optString("text")
                        if (start < 0) start = st
                        end = maxOf(end, en)
                        sb.append(tx)
                        if (tx.endsWith(" ") || j == syl.length() - 1) {
                            if (sb.isNotBlank()) list.add(LyricWord(sb.toString().trim(), start, maxOf(end, start + 80)))
                            sb = StringBuilder(); start = -1L; end = 0L
                        }
                    }
                    if (list.isNotEmpty()) {
                        words = list
                        text = list.joinToString(" ") { it.text }
                    }
                }
                if (text.isBlank()) continue
                out.add(LyricLine(time, text.trim(), words))
            }
            if (out.isNotEmpty()) return out.sortedBy { it.ms }
        }
        // some servers answer in the lrclib shape
        val synced = json.optString("syncedLyrics")
        return if (synced.isNotBlank()) Lyrics.parseLrc(synced) else emptyList()
    }
}
