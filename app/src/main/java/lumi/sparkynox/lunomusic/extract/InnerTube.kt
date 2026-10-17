package lumi.sparkynox.lunomusic.extract

import android.os.SystemClock
import lumi.sparkynox.lunomusic.auth.Session
import lumi.sparkynox.lunomusic.util.AppLog
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
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
            .withHeaders()

        val t0 = SystemClock.elapsedRealtime()
        try {
            http.newCall(rb.build()).execute().use { r ->
                val text = r.body?.string() ?: ""
                val ms = SystemClock.elapsedRealtime() - t0
                if (!r.isSuccessful) {
                    AppLog.e("InnerTube", "$path -> HTTP ${r.code} in ${ms}ms (auth=${Session.loggedIn}) body=${text.take(300)}")
                    throw IOException("youtube music ${r.code}")
                }
                AppLog.d("InnerTube", "$path -> ${r.code} in ${ms}ms, ${text.length} bytes (auth=${Session.loggedIn})")
                return JSONObject(text)
            }
        } catch (e: IOException) {
            AppLog.e("InnerTube", "$path failed after ${SystemClock.elapsedRealtime() - t0}ms: ${e.message}")
            throw e
        }
    }

    fun accountMenu(): JSONObject = post(
        "account/account_menu",
        JSONObject().put("deviceTheme", "DEVICE_THEME_SELECTED").put("userInterfaceTheme", "USER_INTERFACE_THEME_DARK")
    )

    // like / unlike, needs login
    fun like(videoId: String, remove: Boolean) {
        val body = JSONObject().put("target", JSONObject().put("videoId", videoId))
        post(if (remove) "like/removelike" else "like/like", body)
    }

    // the headers every call to music.youtube.com needs, plus the login cookie when there is one
    private fun Request.Builder.withHeaders(): Request.Builder {
        header("User-Agent", UA)
        header("X-Goog-Api-Format-Version", "1")
        header("X-YouTube-Client-Name", "67")
        header("X-YouTube-Client-Version", CLIENT_VERSION)
        header("X-Origin", ORIGIN)
        header("Referer", "$ORIGIN/")
        Session.visitorData?.let { header("X-Goog-Visitor-Id", it) }
        Session.cookie?.let { cookie ->
            header("Cookie", cookie)
            Session.sapisid()?.let { s ->
                val t = System.currentTimeMillis() / 1000
                header("Authorization", "SAPISIDHASH ${t}_${sha1("$t $s $ORIGIN")}")
            }
        }
        return this
    }

    // add one song to a playlist of the account (needs login)
    fun addToPlaylist(playlistId: String, videoId: String) {
        val action = JSONObject()
            .put("action", "ACTION_ADD_VIDEO")
            .put("addedVideoId", videoId)
            .put("dedupeOption", "DEDUPE_OPTION_SKIP")
        val body = JSONObject()
            .put("playlistId", playlistId.removePrefix("VL"))
            .put("actions", JSONArray().put(action))
        val status = post("browse/edit_playlist", body).optString("status")
        if (status != "STATUS_SUCCEEDED") throw IOException("youtube music answered '${status.ifEmpty { "nothing" }}'")
    }

    // new private playlist that already holds the song, returns its id
    fun createPlaylist(title: String, videoId: String): String {
        val body = JSONObject()
            .put("title", title)
            .put("description", "")
            .put("privacyStatus", "PRIVATE")
            .put("videoIds", JSONArray().put(videoId))
        return post("playlist/create", body).optString("playlistId").ifEmpty { throw IOException("no playlist id in the answer") }
    }

    // The player plays raw stream urls, so youtube never hears about the play and the account history stays
    // empty. Asking for the tracking url of the song and pinging it is what the web player does.
    fun registerPlayback(videoId: String) {
        if (!Session.loggedIn) return
        val player = post("player", JSONObject().put("videoId", videoId).put("racyCheckOk", true).put("contentCheckOk", true))
        val base = player.optJSONObject("playbackTracking")?.optJSONObject("videostatsPlaybackUrl")?.optString("baseUrl")
            ?.takeIf { it.isNotEmpty() }
        if (base == null) {
            AppLog.w("InnerTube", "$videoId: no playback tracking url in the player answer, history not updated")
            return
        }
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
        val cpn = (1..16).map { chars.random() }.joinToString("")
        val url = base.replace("https://s.youtube.com", ORIGIN).toHttpUrl().newBuilder()
            .addQueryParameter("ver", "2")
            .addQueryParameter("c", "WEB_REMIX")
            .addQueryParameter("cpn", cpn)
            .build()
        http.newCall(Request.Builder().url(url).get().withHeaders().build()).execute().use { r ->
            AppLog.i("InnerTube", "$videoId: playback registered, HTTP ${r.code}")
        }
    }

    private fun sha1(s: String) = MessageDigest.getInstance("SHA-1")
        .digest(s.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
