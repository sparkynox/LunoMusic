package lumi.sparkynox.lunomusic.extract

import org.json.JSONArray
import org.json.JSONObject

enum class Kind { SONG, ALBUM, PLAYLIST, ARTIST }

data class HomeItem(
    val kind: Kind,
    val title: String,
    val subtitle: String,
    val artist: String,
    val thumb: String?,
    val videoId: String?,
    val playlistId: String?,
)

data class HomeSection(val title: String, val items: List<HomeItem>)

data class Chip(val title: String, val browseId: String, val params: String?)

data class HomePage(val sections: List<HomeSection>, val continuation: String?, val chips: List<Chip> = emptyList())

object Feed {
    private val seps = setOf(",", "&", "•", "·", "-", "and")

    fun home(continuation: String? = null, chip: Chip? = null): HomePage {
        val root = when {
            continuation != null -> InnerTube.browse(continuation = continuation)
            chip != null -> InnerTube.browse(chip.browseId, params = chip.params)
            else -> InnerTube.browse("FEmusic_home")
        }

        val list = if (continuation != null) {
            root.obj("continuationContents").obj("sectionListContinuation")
        } else {
            root.obj("contents").obj("singleColumnBrowseResultsRenderer").arr("tabs").objs().firstOrNull()
                .obj("tabRenderer").obj("content").obj("sectionListRenderer")
        }

        val sections = list.arr("contents").objs().mapNotNull { section(it.obj("musicCarouselShelfRenderer")) }
        val next = list.arr("continuations").objs().firstOrNull().obj("nextContinuationData").str("continuation")
        val chips = list.obj("header").obj("chipCloudRenderer").arr("chips").objs().mapNotNull { c ->
            val r = c.obj("chipCloudChipRenderer")
            val be = r.obj("navigationEndpoint").obj("browseEndpoint")
            Chip(r.obj("text").firstText() ?: return@mapNotNull null, be.str("browseId") ?: return@mapNotNull null, be.str("params"))
        }
        return HomePage(sections, next, chips)
    }

    fun queue(playlistId: String?, videoId: String? = null): List<Track> {
        val root = InnerTube.next(playlistId, videoId)
        val panel = root.obj("contents").obj("singleColumnMusicWatchNextResultsRenderer").obj("tabbedRenderer")
            .obj("watchNextTabbedResultsRenderer").arr("tabs").objs().firstOrNull()
            .obj("tabRenderer").obj("content").obj("musicQueueRenderer").obj("content").obj("playlistPanelRenderer")

        return panel.arr("contents").objs().mapNotNull { it.obj("playlistPanelVideoRenderer") }.mapNotNull { v ->
            val id = v.str("videoId") ?: return@mapNotNull null
            val title = v.obj("title").firstText() ?: return@mapNotNull null
            Track(
                id = id,
                title = title,
                artist = v.obj("shortBylineText").firstText() ?: "",
                thumb = v.obj("thumbnail").arr("thumbnails").objs().lastOrNull().str("url"),
                seconds = seconds(v.obj("lengthText").firstText()),
            )
        }
    }

    private fun section(shelf: JSONObject?): HomeSection? {
        if (shelf == null) return null
        val title = shelf.obj("header").obj("musicCarouselShelfBasicHeaderRenderer").obj("title").firstText() ?: return null
        val items = shelf.arr("contents").objs().mapNotNull {
            it.obj("musicTwoRowItemRenderer")?.let { r -> twoRow(r) }
                ?: it.obj("musicResponsiveListItemRenderer")?.let { r -> listRow(r) }
        }
        return if (items.isEmpty()) null else HomeSection(title, items)
    }

    private fun twoRow(r: JSONObject): HomeItem? {
        val title = r.obj("title").firstText() ?: return null
        val runs = r.obj("subtitle").runs()
        val parts = parts(runs)
        val thumb = thumb(r.obj("thumbnailRenderer"))

        val nav = r.obj("navigationEndpoint")
        val watch = nav.obj("watchEndpoint")
        val mix = nav.obj("watchPlaylistEndpoint")
        val browse = nav.obj("browseEndpoint")
        val pageType = browse.obj("browseEndpointContextSupportedConfigs").obj("browseEndpointContextMusicConfig").str("pageType")
        val overlayPlaylist = r.obj("thumbnailOverlay").obj("musicItemThumbnailOverlayRenderer").obj("content")
            .obj("musicPlayButtonRenderer").obj("playNavigationEndpoint").obj("watchPlaylistEndpoint").str("playlistId")

        val sub = parts.joinToString(" • ")
        val artist = runs
            .filter { it.obj("navigationEndpoint").obj("browseEndpoint").str("browseId")?.startsWith("UC") == true }
            .joinToString(", ") { it.optString("text") }
            .ifEmpty { parts.firstOrNull() ?: "" }

        return when {
            watch != null -> HomeItem(Kind.SONG, title, sub, artist, thumb, watch.str("videoId") ?: return null, null)
            mix != null -> HomeItem(Kind.PLAYLIST, title, sub, artist, thumb, null, mix.str("playlistId") ?: return null)
            pageType == "MUSIC_PAGE_TYPE_ARTIST" -> HomeItem(Kind.ARTIST, title, sub, artist, thumb, null, null)
            pageType == "MUSIC_PAGE_TYPE_ALBUM" || pageType == "MUSIC_PAGE_TYPE_AUDIOBOOK" ->
                HomeItem(Kind.ALBUM, title, sub, artist, thumb, null, overlayPlaylist ?: return null)
            pageType == "MUSIC_PAGE_TYPE_PLAYLIST" ->
                HomeItem(Kind.PLAYLIST, title, sub, artist, thumb, null, overlayPlaylist ?: browse.str("browseId")?.removePrefix("VL") ?: return null)
            else -> null
        }
    }

    // quick picks style rows
    private fun listRow(r: JSONObject): HomeItem? {
        val cols = r.arr("flexColumns").objs().map { it.obj("musicResponsiveListItemFlexColumnRenderer").obj("text") }
        val title = cols.getOrNull(0).firstText() ?: return null
        val parts = parts(cols.getOrNull(1).runs())
        val videoId = r.obj("playlistItemData").str("videoId")
            ?: r.obj("overlay").obj("musicItemThumbnailOverlayRenderer").obj("content").obj("musicPlayButtonRenderer")
                .obj("playNavigationEndpoint").obj("watchEndpoint").str("videoId")
            ?: return null
        return HomeItem(Kind.SONG, title, parts.joinToString(" • "), parts.firstOrNull() ?: "", thumb(r.obj("thumbnail")), videoId, null)
    }

    private fun thumb(renderer: JSONObject?) = renderer.obj("musicThumbnailRenderer").obj("thumbnail")
        .arr("thumbnails").objs().lastOrNull().str("url")

    private fun parts(runs: List<JSONObject>) = runs.map { it.optString("text") }
        .filter { it.isNotBlank() && it.trim().lowercase() !in seps }

    private fun seconds(text: String?): Long =
        text?.split(":")?.mapNotNull { it.toLongOrNull() }?.fold(0L) { acc, n -> acc * 60 + n } ?: 0L

    private fun JSONObject?.obj(k: String): JSONObject? = this?.optJSONObject(k)
    private fun JSONObject?.arr(k: String): JSONArray? = this?.optJSONArray(k)
    private fun JSONObject?.str(k: String): String? = this?.optString(k)?.takeIf { it.isNotEmpty() }
    private fun JSONArray?.objs(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
    private fun JSONObject?.runs(): List<JSONObject> = arr("runs").objs()
    private fun JSONObject?.firstText(): String? = runs().firstOrNull().str("text")
}
