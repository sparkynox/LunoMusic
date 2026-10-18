package lumi.sparkynox.lunomusic.util

import android.app.Activity
import android.content.Context
import android.content.Intent

object Share {
    // opens the share sheet with the song name and its YouTube Music link
    fun song(ctx: Context, id: String, title: String, artist: String) {
        val text = listOf("$title - $artist".trim(' ', '-'), "https://music.youtube.com/watch?v=$id").joinToString("\n")
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .putExtra(Intent.EXTRA_TEXT, text)
        val chooser = Intent.createChooser(send, null)
        if (ctx !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(chooser) }.onFailure { AppLog.e("Share", "couldn't open the share sheet", it) }
    }
}
