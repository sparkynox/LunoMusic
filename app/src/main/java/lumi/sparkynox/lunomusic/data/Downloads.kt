package lumi.sparkynox.lunomusic.data

import android.content.ContentValues
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import lumi.sparkynox.lunomusic.extract.Extractor
import lumi.sparkynox.lunomusic.extract.Track
import lumi.sparkynox.lunomusic.util.AppLog
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object Downloads {
    private const val CHUNK = 9_500_000L
    private const val UA = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"

    // id -> 0..1, only while a download is running
    val active = mutableStateMapOf<String, Float>()
    // ids that are on disk, observed by the ui
    val ids = mutableStateListOf<String>()

    private lateinit var dir: File
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(2)
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun init(ctx: Context) {
        dir = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "music").also { it.mkdirs() }
        ids.clear()
        Db.db.rawQuery("SELECT id FROM downloads", null).use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                if (file(id).exists()) ids.add(id)
            }
        }
        AppLog.i("Downloads", "dir=${dir.absolutePath}, ${ids.size} songs on disk")
    }

    fun file(id: String) = File(dir, "$id.audio")

    fun has(id: String) = id in ids

    fun start(t: Track) {
        if (has(t.id) || active.containsKey(t.id)) return
        active[t.id] = 0f
        AppLog.i("Downloads", "queued ${t.id} '${t.title}'")
        scope.launch {
            gate.withPermit {
                val t0 = System.currentTimeMillis()
                try {
                    fetch(t)
                    AppLog.i("Downloads", "done ${t.id} in ${(System.currentTimeMillis() - t0) / 1000}s, ${file(t.id).length() / 1024} KB")
                } catch (e: Exception) {
                    AppLog.e("Downloads", "failed ${t.id} '${t.title}'", e)
                    File(dir, "${t.id}.part").delete()
                }
                active.remove(t.id)
            }
        }
    }

    fun startAll(tracks: List<Track>) = tracks.forEach { start(it) }

    private fun fetch(t: Track) {
        val part = File(dir, "${t.id}.part")
        var url = Extractor.resolve(t.id)
        var retries = 0
        var offset = 0L
        var total = -1L

        part.outputStream().use { out ->
            var done = false
            while (!done) {
                val req = Request.Builder().url(url).header("User-Agent", UA)
                    .header("Range", "bytes=$offset-${offset + CHUNK - 1}").build()
                http.newCall(req).execute().use { r ->
                    if (r.code == 403 && retries < 2) {
                        retries++
                        AppLog.w("Downloads", "403 for ${t.id}, re-resolving (try $retries)")
                        Extractor.invalidate(t.id)
                        url = Extractor.resolve(t.id)
                        return@use
                    }
                    if (r.code != 200 && r.code != 206) throw IOException("http ${r.code}")
                    val body = r.body ?: throw IOException("empty body")

                    if (r.code == 200) {
                        if (offset > 0) throw IOException("server ignored range request")
                        total = body.contentLength()
                    } else {
                        total = r.header("Content-Range")?.substringAfter("/")?.toLongOrNull() ?: -1L
                    }

                    val buf = ByteArray(64 * 1024)
                    var written = 0L
                    body.byteStream().use { input ->
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            written += n
                            if (total > 0) active[t.id] = ((offset + written).toFloat() / total).coerceIn(0f, 1f)
                        }
                    }
                    offset += written
                    done = r.code == 200 || (total > 0 && offset >= total) || written == 0L
                }
            }
        }

        val target = file(t.id)
        if (!part.renameTo(target)) throw IOException("couldn't move finished file")
        val v = ContentValues().apply {
            put("id", t.id); put("title", t.title); put("artist", t.artist); put("thumb", t.thumb)
            put("artist_id", t.artistId); put("size", target.length()); put("ts", System.currentTimeMillis())
        }
        Db.db.insertWithOnConflict("downloads", null, v, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        ids.add(t.id)
    }

    fun all(): List<Track> {
        val out = ArrayList<Track>()
        Db.db.rawQuery("SELECT id,title,artist,thumb,artist_id FROM downloads ORDER BY ts DESC", null).use { c ->
            while (c.moveToNext()) {
                if (file(c.getString(0)).exists())
                    out.add(Track(c.getString(0), c.getString(1), c.getString(2), c.getString(3), 0L, c.getString(4)))
            }
        }
        return out
    }

    fun delete(id: String) {
        file(id).delete()
        Db.db.delete("downloads", "id=?", arrayOf(id))
        ids.remove(id)
        AppLog.i("Downloads", "deleted $id")
    }

    fun deleteAll() {
        ids.toList().forEach { delete(it) }
    }

    fun totalBytes(): Long = ids.sumOf { file(it).length() }
}
