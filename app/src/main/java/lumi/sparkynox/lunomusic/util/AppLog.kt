package lumi.sparkynox.lunomusic.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class LogEntry(val text: String) {
    // "2026-10-14 18:44:03.512 W/Tag: msg", level char sits at index 24
    val level: Char = text.getOrNull(24) ?: 'I'
}

object AppLog {
    private const val MAX_MEM = 6000
    private const val MAX_FILE = 2_000_000L

    private val lock = Any()
    private val mem = ArrayList<LogEntry>()
    private val io = Executors.newSingleThreadExecutor()
    private val head = Regex("""^\d{4}-\d{2}-\d{2} """)
    private var file: File? = null
    private var header = ""

    fun init(ctx: Context) {
        val f = File(ctx.filesDir, "luno.log")
        file = f
        if (f.exists()) runCatching {
            val cur = StringBuilder()
            val loaded = ArrayList<LogEntry>()
            f.forEachLine { line ->
                if (head.containsMatchIn(line)) {
                    if (cur.isNotEmpty()) loaded.add(LogEntry(cur.toString()))
                    cur.setLength(0)
                    cur.append(line)
                } else if (cur.isNotEmpty()) cur.append('\n').append(line)
            }
            if (cur.isNotEmpty()) loaded.add(LogEntry(cur.toString()))
            synchronized(lock) { mem.addAll(loaded.takeLast(MAX_MEM)) }
        }

        val version = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull()
        header = "LunoMusic $version | Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) | " +
            "${Build.MANUFACTURER} ${Build.MODEL} | ABI ${Build.SUPPORTED_ABIS.joinToString()}"

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            e("Crash", "uncaught exception on thread ${t.name}", e)
            io.shutdown()
            runCatching { io.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS) }
            previous?.uncaughtException(t, e)
        }
        i("App", "---- session start ---- $header")
    }

    fun d(tag: String, msg: String) {
        if (Prefs.verboseLogs) add('D', tag, msg, null)
    }
    fun i(tag: String, msg: String) = add('I', tag, msg, null)
    fun w(tag: String, msg: String, t: Throwable? = null) = add('W', tag, msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null) = add('E', tag, msg, t)

    private fun add(level: Char, tag: String, msg: String, t: Throwable?) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val sb = StringBuilder("$ts $level/$tag: $msg")
        if (t != null) sb.append('\n').append(Log.getStackTraceString(t).trimEnd())
        val text = sb.toString()

        when (level) {
            'E' -> Log.e(tag, msg, t)
            'W' -> Log.w(tag, msg, t)
            'D' -> Log.d(tag, msg)
            else -> Log.i(tag, msg)
        }

        synchronized(lock) {
            mem.add(LogEntry(text))
            if (mem.size > MAX_MEM + 500) mem.subList(0, mem.size - MAX_MEM).clear()
        }
        io.execute {
            runCatching {
                val f = file ?: return@execute
                if (f.length() > MAX_FILE) f.renameTo(File(f.parentFile, "luno.log.1"))
                f.appendText(text + "\n")
            }
        }
    }

    fun snapshot(): List<LogEntry> = synchronized(lock) { ArrayList(mem) }

    fun clear() {
        synchronized(lock) { mem.clear() }
        io.execute { runCatching { file?.writeText("") } }
        i("App", "log cleared")
    }

    // whole log, oldest first, with a device header on top
    fun export(ctx: Context): File {
        val out = File(ctx.cacheDir, "luno-log.txt")
        val all = snapshot()
        out.bufferedWriter().use { w ->
            w.write(header + "\n")
            w.write("entries: ${all.size}\n\n")
            all.forEach { w.write(it.text + "\n") }
        }
        return out
    }

    fun headerText() = header
}
