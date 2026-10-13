package lumi.sparkynox.lunomusic.extract

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject

object PyYtDlp {
    var ctx: Context? = null

    @Synchronized
    fun ensure() {
        if (Python.isStarted()) return
        Python.start(AndroidPlatform(ctx ?: error("no context")))
    }

    fun audioUrl(id: String): String? {
        ensure()
        val raw = Python.getInstance().getModule("luno_ytdlp").callAttr("resolve", id).toString()
        return JSONObject(raw).optString("url").takeIf { it.isNotEmpty() && it != "null" }
    }
}
