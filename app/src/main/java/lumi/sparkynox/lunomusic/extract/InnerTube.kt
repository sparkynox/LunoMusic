package lumi.sparkynox.lunomusic.extract

import lumi.sparkynox.lunomusic.auth.Session
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object InnerTube {
    private const val ORIGIN = "https://music.youtube.com"
    private const val CLIENT_VERSION = "1.20260213.01.00"
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

    private val jsonType = "application/json".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun browse(browseId: String? = null, continuation: String? = null, params: String? = null): JSONObject {
        val body = JSONObject()
        browseId?.let { body.put("browseId", it) }
        params?.let { body.put("params", it) }
        continuation?.let { body.put("continuation", it) }
        return post("browse", body)
    }

    fun next(playlistId: String? = null, videoId: String? = null): JSONObject {
        val body = JSONObject()
        playlistId?.let { body.put("playlistId", it) }
        videoId?.let { body.put("videoId", it) }
        body.put("isAudioOnly", true)
        body.put("enablePersistentPlaylistPanel", true)
        return post("next", body)
    }

    fun suggestions(input: String): JSONObject =
        post("music/get_search_suggestions", JSONObject().put("input", input))

    private fun post(path: String, body: JSONObject): JSONObject {
        val client = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", CLIENT_VERSION)
            .put("gl", "IN")
            .put("hl", "en")
        Session.visitorData?.let { client.put("visitorData", it) }

        val ctx = JSONObject().put("client", client)
        if (Session.loggedIn) Session.dataSyncId?.let { ctx.put("user", JSONObject().put("onBehalfOfUser", it)) }
        body.put("context", ctx)

        val rb = Request.Builder()
            .url("$ORIGIN/youtubei/v1/$path?prettyPrint=false")
            .post(body.toString().toRequestBody(jsonType))
            .header("User-Agent", UA)
            .header("X-Goog-Api-Format-Version", "1")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", CLIENT_VERSION)
            .header("X-Origin", ORIGIN)
            .header("Referer", "$ORIGIN/")
        Session.visitorData?.let { rb.header("X-Goog-Visitor-Id", it) }

        Session.cookie?.let { cookie ->
            rb.header("Cookie", cookie)
            Session.sapisid()?.let { s ->
                val t = System.currentTimeMillis() / 1000
                rb.header("Authorization", "SAPISIDHASH ${t}_${sha1("$t $s $ORIGIN")}")
            }
        }

        http.newCall(rb.build()).execute().use { r ->
            if (!r.isSuccessful) throw IOException("youtube music ${r.code}")
            return JSONObject(r.body?.string() ?: throw IOException("empty response"))
        }
    }

    private fun sha1(s: String) = MessageDigest.getInstance("SHA-1")
        .digest(s.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
