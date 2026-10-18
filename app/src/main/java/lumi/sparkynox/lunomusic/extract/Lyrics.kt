package lumi.sparkynox.lunomusic.extract

import lumi.sparkynox.lunomusic.util.AppLog
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// words is only filled when the source has word level timing (YouLyPlus), start/end are absolute ms
data class LyricWord(val text: String, val start: Long, val end: Long)
data class LyricLine(val ms: Long, val text: String, val words: List<LyricWord>? = null)

// synced lyrics from lrclib.net
object Lyrics {
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val tag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")

    // YouLyPlus has word timing and covers more songs (Hindi too), lrclib runs next to it and covers the rest
    fun fetch(title: String, artist: String, seconds: Long): List<LyricLine> {
        val lrc = java.util.concurrent.FutureTask { runCatching { fetchLrcLib(title, artist, seconds) }.getOrDefault(emptyList()) }
        Thread(lrc).start()
        val rich = runCatching { YouLy.fetch(clean(title), artist.substringBefore(",").trim(), seconds) }
            .onFailure { AppLog.w("Lyrics", "youlyplus failed", it) }.getOrDefault(emptyList())
        if (rich.isNotEmpty() && fits(rich, seconds)) {
            AppLog.i("Lyrics", "'$title' from youlyplus, ${rich.size} lines, word sync=${rich.any { it.words != null }}")
            return rich
        }
        if (rich.isNotEmpty()) AppLog.w("Lyrics", "'$title': youlyplus lyrics run to ${rich.last().ms / 1000}s but the song is ${seconds}s, wrong version, skipped")
        return lrc.get()
    }

    // The title is cleaned (brackets removed) before searching, so "SAD! (FUNK)" finds the lyrics of "SAD!", a different
    // cut that has other timing. Lyrics that carry on past the end of the song can't belong to it.
    private fun fits(lines: List<LyricLine>, seconds: Long) =
        seconds <= 0 || lines.isEmpty() || lines.last().ms <= (seconds + 4) * 1000

    private fun fetchLrcLib(title: String, artist: String, seconds: Long): List<LyricLine> {
        val t = clean(title)
        val a = artist.substringBefore(",").trim()

        val get = HttpUrl.Builder().scheme("https").host("lrclib.net").addPathSegments("api/get")
            .addQueryParameter("track_name", t)
            .addQueryParameter("artist_name", a)
            .apply { if (seconds > 0) addQueryParameter("duration", seconds.toString()) }
            .build()
        body(get)?.let { parse(JSONObject(it).optString("syncedLyrics")) }?.takeIf { it.isNotEmpty() }?.let {
            AppLog.i("Lyrics", "'$t' found via get, ${it.size} lines")
            return it
        }

        val search = HttpUrl.Builder().scheme("https").host("lrclib.net").addPathSegments("api/search")
            .addQueryParameter("q", "$t $a")
            .build()
        val arr = body(search)?.let { JSONArray(it) } ?: return emptyList()
        // take the result whose length is closest to the song, anything more than 5s off is another version
        var best: List<LyricLine> = emptyList()
        var bestDiff = Double.MAX_VALUE
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val lines = parse(o.optString("syncedLyrics"))
            if (lines.isEmpty()) continue
            val len = o.optDouble("duration", -1.0)
            val diff = if (seconds > 0 && len > 0) Math.abs(len - seconds) else 0.0
            if (seconds > 0 && len > 0 && diff > 5.0) continue
            if (diff < bestDiff) {
                best = lines
                bestDiff = diff
                AppLog.i("Lyrics", "'$t' found via search (#$i), ${lines.size} lines, ${if (len > 0) "length ${len.toInt()}s" else "length unknown"}")
            }
        }
        if (best.isNotEmpty()) return best
        AppLog.w("Lyrics", "'$t' by '$a' ($seconds s): nothing synced found")
        return emptyList()
    }

    private fun body(url: HttpUrl): String? {
        val req = Request.Builder().url(url).header("User-Agent", "LunoMusic (https://github.com/sparkynox/LunoMusic)").build()
        http.newCall(req).execute().use { r ->
            return if (r.isSuccessful) r.body?.string() else null
        }
    }

    private fun clean(title: String): String {
        val c = title.replace(Regex("""\s*[(\[].*?[)\]]"""), "").replace(" - Topic", "").trim()
        return c.ifEmpty { title }
    }

    fun parseLrc(lrc: String?) = parse(lrc)

    private fun parse(lrc: String?): List<LyricLine> {
        if (lrc.isNullOrBlank()) return emptyList()
        val out = ArrayList<LyricLine>()
        for (line in lrc.lines()) {
            val tags = tag.findAll(line).toList()
            if (tags.isEmpty()) continue
            val text = line.substring(tags.last().range.last + 1).trim()
            for (m in tags) {
                val frac = m.groupValues[3].let { if (it.isEmpty()) 0L else it.padEnd(3, '0').take(3).toLong() }
                out.add(LyricLine(m.groupValues[1].toLong() * 60000 + m.groupValues[2].toLong() * 1000 + frac, text))
            }
        }
        return out.sortedBy { it.ms }
    }
}
