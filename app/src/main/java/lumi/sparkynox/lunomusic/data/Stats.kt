package lumi.sparkynox.lunomusic.data

import android.content.ContentValues
import lumi.sparkynox.lunomusic.extract.Track
import lumi.sparkynox.lunomusic.util.AppLog

class Play(val id: String, val title: String, val artist: String, val thumb: String?, val ts: Long, val ms: Long)

object Stats {
    fun record(id: String, title: String, artist: String, thumb: String?, ms: Long) {
        val v = ContentValues().apply {
            put("id", id); put("title", title); put("artist", artist); put("thumb", thumb)
            put("ts", System.currentTimeMillis()); put("ms", ms)
        }
        Db.db.insert("plays", null, v)
        AppLog.i("Stats", "recorded '$title' listened ${ms / 1000}s")
    }

    fun since(from: Long): List<Play> {
        val out = ArrayList<Play>()
        Db.db.rawQuery("SELECT id,title,artist,thumb,ts,ms FROM plays WHERE ts>=? ORDER BY ts", arrayOf(from.toString())).use { c ->
            while (c.moveToNext()) out.add(Play(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getLong(5)))
        }
        return out
    }

    fun topTracks(n: Int): List<Track> {
        val out = ArrayList<Track>()
        Db.db.rawQuery(
            "SELECT id,title,artist,thumb,COUNT(*) c FROM plays GROUP BY id ORDER BY c DESC LIMIT ?",
            arrayOf(n.toString())
        ).use { c ->
            while (c.moveToNext()) out.add(Track(c.getString(0), c.getString(1), c.getString(2), c.getString(3), 0L))
        }
        return out
    }

    fun clear() {
        Db.db.delete("plays", null, null)
        AppLog.i("Stats", "cleared")
    }
}
