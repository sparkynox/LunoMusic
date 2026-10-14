package lumi.sparkynox.lunomusic.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private class LunoDb(ctx: Context) : SQLiteOpenHelper(ctx, "luno.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE plays(id TEXT, title TEXT, artist TEXT, thumb TEXT, ts INTEGER, ms INTEGER)")
        db.execSQL("CREATE INDEX plays_ts ON plays(ts)")
        db.execSQL("CREATE TABLE downloads(id TEXT PRIMARY KEY, title TEXT, artist TEXT, thumb TEXT, artist_id TEXT, size INTEGER, ts INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
}

object Db {
    private var helper: LunoDb? = null

    fun init(ctx: Context) {
        helper = LunoDb(ctx.applicationContext)
    }

    val db: SQLiteDatabase get() = helper!!.writableDatabase
}
