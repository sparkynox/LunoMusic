package lumi.sparkynox.lunomusic.util

// artwork urls come in whatever size the feed happened to send (often 226px), the big screens want the best there is
object Img {
    private const val SIZE = 1024
    private val sized = Regex("=w\\d+-h\\d+[^/?]*$")
    private val square = Regex("=s\\d+[^/?]*$")
    private val ytThumb = Regex("^https?://i\\d?\\.ytimg\\.com/vi(?:_webp)?/([\\w-]{11})/[\\w]+\\.(?:jpg|webp)(?:\\?.*)?$")

    private fun googleHosted(url: String) = "googleusercontent.com/" in url || "ggpht.com/" in url

    // google hosted art can be resized by changing the suffix, this always works so it's safe without a fallback
    fun hq(url: String?): String? {
        if (url.isNullOrBlank() || !googleHosted(url)) return url
        return when {
            sized.containsMatchIn(url) -> url.replace(sized, "=w$SIZE-h$SIZE-l90-rj")
            square.containsMatchIn(url) -> url.replace(square, "=s$SIZE")
            else -> url
        }
    }

    // like hq, and for youtube video thumbnails the maxres one. That file doesn't exist for every video,
    // so only use this where the caller can fall back to the original url when it fails to load
    fun best(url: String?): String? {
        if (url == null) return null
        ytThumb.find(url)?.let { return "https://i.ytimg.com/vi/${it.groupValues[1]}/maxresdefault.jpg" }
        return hq(url)
    }
}
