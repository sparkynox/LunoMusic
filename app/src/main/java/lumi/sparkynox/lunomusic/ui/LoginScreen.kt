package lumi.sparkynox.lunomusic.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import lumi.sparkynox.lunomusic.auth.Session
import org.json.JSONObject
import org.json.JSONTokener

private const val GET_CONFIG =
    "(function(){try{var c=window.yt.config_;return JSON.stringify({v:c.VISITOR_DATA||'',d:c.DATASYNC_ID||''});}catch(e){return '';}})()"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(onDone: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            TextButton(onClick = onBack) { Text("Close") }
        }
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = settings.userAgentString.replace("; wv", "")

                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().flush()

                    webViewClient = object : WebViewClient() {
                        private var done = false

                        override fun onPageFinished(view: WebView, url: String?) {
                            if (done || url == null || !url.startsWith("https://music.youtube.com")) return
                            poll(view, url, 0)
                        }

                        // config isn't always ready right at page finish
                        private fun poll(view: WebView, url: String, tries: Int) {
                            val cookie = CookieManager.getInstance().getCookie(url)
                            if (cookie == null || !cookie.contains("SAPISID=")) return

                            view.evaluateJavascript(GET_CONFIG) { raw ->
                                val cfg = runCatching { JSONObject(JSONTokener(raw).nextValue() as String) }.getOrNull()
                                if (cfg == null || cfg.optString("v").isEmpty()) {
                                    if (tries < 8) view.postDelayed({ poll(view, url, tries + 1) }, 700)
                                    return@evaluateJavascript
                                }
                                if (done) return@evaluateJavascript
                                done = true
                                Session.save(cookie, cfg.optString("v"), cfg.optString("d").substringBefore("||"))
                                onDone()
                            }
                        }
                    }

                    loadUrl("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com")
                }
            }
        )
    }
}
