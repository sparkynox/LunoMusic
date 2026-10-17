package lumi.sparkynox.lunomusic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import lumi.sparkynox.lunomusic.extract.HomeSection
import lumi.sparkynox.lunomusic.extract.Track

// full screen pages that sit on top of the tabs, kept in a stack in the view model
sealed class Page

object SettingsPage : Page()
object LogsPage : Page()
object StatsPage : Page()
object AboutPage : Page()
object ProfilePage : Page()

// album, playlist, liked songs, history, downloads, top 50
class DetailPage(
    val title: String,
    val subtitle: String,
    val thumb: String?,
    val isDownloads: Boolean = false,
    val numbered: Boolean = false,
) : Page() {
    var tracks by mutableStateOf<List<Track>>(emptyList())
    var loading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
}

class ArtistPage(val id: String, name: String, thumb: String?) : Page() {
    var name by mutableStateOf(name)
    var thumb by mutableStateOf(thumb)
    var description by mutableStateOf<String?>(null)
    var songs by mutableStateOf<List<Track>>(emptyList())
    var sections by mutableStateOf<List<HomeSection>>(emptyList())
    var loading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
}
