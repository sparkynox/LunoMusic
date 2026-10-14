package lumi.sparkynox.lunomusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.data.Downloads
import lumi.sparkynox.lunomusic.extract.HomeItem
import lumi.sparkynox.lunomusic.extract.HomeSection
import lumi.sparkynox.lunomusic.extract.Kind
import lumi.sparkynox.lunomusic.extract.Track

val listPad = PaddingValues(bottom = 190.dp)

@Composable
fun SectionHeader(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun CarouselSection(vm: MusicVM, sec: HomeSection) {
    SectionHeader(sec.title)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sec.items) { item -> ItemCard(item) { vm.playItem(sec, item) } }
    }
}

@Composable
fun ItemCard(item: HomeItem, onClick: () -> Unit) {
    Column(Modifier.width(150.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AsyncImage(
            model = item.thumb,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(150.dp).clip(if (item.kind == Kind.ARTIST) CircleShape else RoundedCornerShape(16.dp))
        )
        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// song row with the three dot menu
@Composable
fun TrackRow(vm: MusicVM, t: Track, onClick: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val progress = Downloads.active[t.id]
    val offline = t.id in Downloads.ids

    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = t.thumb,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
        )
        Column(Modifier.weight(1f)) {
            Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                if (offline) "${t.artist} • Offline" else t.artist,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (progress != null) CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Box {
            IconButton(onClick = { menu = true }) { MoreIcon(22.dp, MaterialTheme.colorScheme.onSurfaceVariant) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Play next") }, onClick = { menu = false; vm.playNext(t) })
                DropdownMenuItem(text = { Text("Add to queue") }, onClick = { menu = false; vm.addToQueue(t) })
                if (offline) {
                    DropdownMenuItem(text = { Text("Remove download") }, onClick = { menu = false; Downloads.delete(t.id) })
                } else if (progress == null) {
                    DropdownMenuItem(text = { Text("Download") }, onClick = { menu = false; vm.download(t) })
                }
                t.artistId?.let { aid ->
                    DropdownMenuItem(text = { Text("Go to artist") }, onClick = { menu = false; vm.openArtist(aid, t.artist, null) })
                }
            }
        }
    }
}

// non song search result (album, playlist, artist)
@Composable
fun ItemRow(item: HomeItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = item.thumb,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(if (item.kind == Kind.ARTIST) CircleShape else RoundedCornerShape(12.dp))
        )
        Column(Modifier.weight(1f)) {
            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
