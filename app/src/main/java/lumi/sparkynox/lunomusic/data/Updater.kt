package lumi.sparkynox.lunomusic.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import lumi.sparkynox.lunomusic.util.AppLog
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class UpdateInfo(val version: String, val notes: String, val url: String, val size: Long)

// in app updates straight from the github releases of the repo
object Updater {
    private const val API = "https://api.github.com/repos/sparkynox/LunoMusic/releases/latest"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    fun currentVersion(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "0"

    // null when we're already on the latest one
    fun check(current: String): UpdateInfo? {
        val req = Request.Builder().url(API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "LunoMusic")
            .build()
        http.newCall(req).execute().use { r ->
            if (r.code == 404) {
                AppLog.w("Update", "repo has no release yet")
                return null
            }
            if (!r.isSuccessful) throw IOException("github answered ${r.code}")
            val j = JSONObject(r.body?.string() ?: throw IOException("empty answer"))
            val tag = j.optString("tag_name").removePrefix("v")
            val assets = j.optJSONArray("assets")
            var apk: JSONObject? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk")) { apk = a; break }
                }
            }
            if (apk == null) {
                AppLog.w("Update", "release $tag has no apk attached")
                return null
            }
            if (!newer(tag, current)) {
                AppLog.i("Update", "latest is $tag, installed $current, nothing to do")
                return null
            }
            AppLog.i("Update", "new version $tag available (installed $current)")
            return UpdateInfo(tag, j.optString("body"), apk.getString("browser_download_url"), apk.optLong("size"))
        }
    }

    private fun parts(v: String) = v.removePrefix("v").split(".", "-").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

    fun newer(a: String, b: String): Boolean {
        val x = parts(a)
        val y = parts(b)
        for (i in 0 until maxOf(x.size, y.size)) {
            val p = x.getOrElse(i) { 0 }
            val q = y.getOrElse(i) { 0 }
            if (p != q) return p > q
        }
        return false
    }

    fun download(ctx: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File {
        val dir = File(ctx.cacheDir, "updates").also { it.mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val part = File(dir, "LunoMusic-${info.version}.part")
        val out = File(dir, "LunoMusic-${info.version}.apk")
        val t0 = System.currentTimeMillis()

        http.newCall(Request.Builder().url(info.url).header("User-Agent", "LunoMusic").build()).execute().use { r ->
            if (!r.isSuccessful) throw IOException("download answered ${r.code}")
            val body = r.body ?: throw IOException("empty download")
            val total = body.contentLength().takeIf { it > 0 } ?: info.size
            var done = 0L
            part.outputStream().use { o ->
                body.byteStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        o.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        }
        if (!part.renameTo(out)) throw IOException("couldn't move the downloaded apk")
        AppLog.i("Update", "downloaded ${out.length() / 1024} KB in ${(System.currentTimeMillis() - t0) / 1000}s")
        return out
    }

    // false = the user still has to allow installs from this app first
    fun install(ctx: Context, apk: File): Boolean {
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            AppLog.w("Update", "install permission missing, opening the settings page")
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        AppLog.i("Update", "handed the apk to the package installer")
        return true
    }
}
