package lumi.sparkynox.lunomusic.extract

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.util.concurrent.TimeUnit

object LunoDownloader : Downloader() {
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        val body = request.dataToSend()?.toRequestBody()
            ?: if (method == "POST") ByteArray(0).toRequestBody() else null

        val rb = okhttp3.Request.Builder().method(method, body).url(request.url())
            .header("User-Agent", UA)
        request.headers().forEach { (k, vals) ->
            rb.removeHeader(k)
            vals.forEach { rb.addHeader(k, it) }
        }

        client.newCall(rb.build()).execute().use { r ->
            if (r.code == 429) throw ReCaptchaException("rate limited", request.url())
            return Response(r.code, r.message, r.headers.toMultimap(), r.body?.string(), r.request.url.toString())
        }
    }
}
