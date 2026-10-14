package lumi.sparkynox.lunomusic.extract

import lumi.sparkynox.lunomusic.util.AppLog
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LyricLine(val ms: Long, val text: String)

// synced lyrics from lrclib.net
object Lyrics {
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val tag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")

    fun fetch(title: String, artist: String, seconds: Long): List<LyricLine> {
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
        for (i in 0 until arr.length()) {
            val lines = parse(arr.optJSONObject(i)?.optString("syncedLyrics"))
            if (lines.isNotEmpty()) {
                AppLog.i("Lyrics", "'$t' found via search (#$i), ${lines.size} lines")
                return lines
            }
        }
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
