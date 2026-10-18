package lumi.sparkynox.lunomusic

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import lumi.sparkynox.lunomusic.auth.Session
import lumi.sparkynox.lunomusic.data.Db
import lumi.sparkynox.lunomusic.data.Downloads
import lumi.sparkynox.lunomusic.extract.LunoDownloader
import lumi.sparkynox.lunomusic.extract.PyYtDlp
import lumi.sparkynox.lunomusic.util.AppLog
import lumi.sparkynox.lunomusic.util.Prefs
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

class LunoApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this).crossfade(250).build()

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        AppLog.init(this)
        Session.init(this)
        Db.init(this)
        Downloads.init(this)
        AppLog.i("App", "logged in: ${Session.loggedIn}")

        NewPipe.init(LunoDownloader, Localization("en", "IN"), ContentCountry("IN"))
        lumi.sparkynox.lunomusic.extract.Extractor.warmUp()
        PyYtDlp.ctx = this
        Thread {
            val t0 = System.currentTimeMillis()
            runCatching { PyYtDlp.ensure() }
                .onSuccess { AppLog.i("App", "python started in ${System.currentTimeMillis() - t0}ms") }
                .onFailure { AppLog.e("App", "python failed to start", it) }
        }.start()
    }
}
