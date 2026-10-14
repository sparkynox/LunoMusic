package lumi.sparkynox.lunomusic.auth

import android.content.Context
import android.content.SharedPreferences

object Session {
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("luno_session", Context.MODE_PRIVATE)
    }

    val cookie: String? get() = sp.getString("cookie", null)?.ifBlank { null }
    val visitorData: String? get() = sp.getString("visitor", null)?.ifBlank { null }
    val dataSyncId: String? get() = sp.getString("sync", null)?.ifBlank { null }

    val loggedIn: Boolean get() = sapisid() != null

    fun sapisid(): String? = cookie?.split(";")
        ?.map { it.trim() }
        ?.firstOrNull { it.startsWith("SAPISID=") }
        ?.substringAfter("=")

    fun save(cookie: String, visitor: String, sync: String) {
        sp.edit().putString("cookie", cookie).putString("visitor", visitor).putString("sync", sync).apply()
    }

    fun logout() {
        sp.edit().clear().apply()
    }
}
