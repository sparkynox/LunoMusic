package lumi.sparkynox.lunomusic

import android.app.Application
import lumi.sparkynox.lunomusic.auth.Session
import lumi.sparkynox.lunomusic.extract.LunoDownloader
import lumi.sparkynox.lunomusic.extract.PyYtDlp
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

class LunoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Session.init(this)
        NewPipe.init(LunoDownloader, Localization("en", "IN"), ContentCountry("IN"))
        PyYtDlp.ctx = this
        Thread { runCatching { PyYtDlp.ensure() } }.start()
    }
}
