package lumi.sparkynox.lunomusic.util

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("luno_prefs", Context.MODE_PRIVATE)
    }

    var endless: Boolean
        get() = sp.getBoolean("endless", true)
        set(v) = sp.edit().putBoolean("endless", v).apply()

    var ytdlpFirst: Boolean
        get() = sp.getBoolean("ytdlp_first", false)
        set(v) = sp.edit().putBoolean("ytdlp_first", v).apply()

    var verboseLogs: Boolean
        get() = sp.getBoolean("verbose_logs", true)
        set(v) = sp.edit().putBoolean("verbose_logs", v).apply()

    var dynamicColor: Boolean
        get() = sp.getBoolean("dynamic_color", true)
        set(v) = sp.edit().putBoolean("dynamic_color", v).apply()

    var pureBlack: Boolean
        get() = sp.getBoolean("pure_black", false)
        set(v) = sp.edit().putBoolean("pure_black", v).apply()
}
