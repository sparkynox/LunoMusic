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

    // songs the user marked "not interested" — hidden everywhere
    var blockedIds: Set<String>
        get() = sp.getStringSet("blocked_ids", emptySet()) ?: emptySet()
        set(v) = sp.edit().putStringSet("blocked_ids", v).apply()

    var bio: String
        get() = sp.getString("bio", "") ?: ""
        set(v) = sp.edit().putString("bio", v).apply()

    var customName: String
        get() = sp.getString("custom_name", "") ?: ""
        set(v) = sp.edit().putString("custom_name", v).apply()

    // path of the photo picked from the gallery, empty = use the youtube music photo
    var customAvatar: String
        get() = sp.getString("custom_avatar", "") ?: ""
        set(v) = sp.edit().putString("custom_avatar", v).apply()

    var accountName: String
        get() = sp.getString("account_name", "") ?: ""
        set(v) = sp.edit().putString("account_name", v).apply()

    var accountHandle: String
        get() = sp.getString("account_handle", "") ?: ""
        set(v) = sp.edit().putString("account_handle", v).apply()

    var accountEmail: String
        get() = sp.getString("account_email", "") ?: ""
        set(v) = sp.edit().putString("account_email", v).apply()

    var accountPhoto: String
        get() = sp.getString("account_photo", "") ?: ""
        set(v) = sp.edit().putString("account_photo", v).apply()

    var autoUpdate: Boolean
        get() = sp.getBoolean("auto_update", true)
        set(v) = sp.edit().putBoolean("auto_update", v).apply()

    var lastUpdateCheck: Long
        get() = sp.getLong("last_update_check", 0L)
        set(v) = sp.edit().putLong("last_update_check", v).apply()

    var skippedVersion: String
        get() = sp.getString("skipped_version", "") ?: ""
        set(v) = sp.edit().putString("skipped_version", v).apply()

    // the battery optimization prompt stays quiet until this time after the user said "not now"
    var batteryAskAfter: Long
        get() = sp.getLong("battery_ask_after", 0L)
        set(v) = sp.edit().putLong("battery_ask_after", v).apply()
}
