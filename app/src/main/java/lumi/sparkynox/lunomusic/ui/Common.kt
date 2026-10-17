package lumi.sparkynox.lunomusic.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.R
import lumi.sparkynox.lunomusic.data.Downloads
import lumi.sparkynox.lunomusic.extract.HomeItem
import lumi.sparkynox.lunomusic.extract.HomeSection
import lumi.sparkynox.lunomusic.extract.Kind
import lumi.sparkynox.lunomusic.extract.Track

val listPad = PaddingValues(bottom = 200.dp)

// springy press feedback, used on cards and buttons
fun Modifier.bounceClick(onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    this.graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

@Composable
fun RoundIcon(
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(onClick = onClick, shape = CircleShape, color = container, modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
fun SectionHeader(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun CarouselSection(vm: MusicVM, sec: HomeSection) {
    SectionHeader(sec.title)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(sec.items) { item -> ItemCard(item) { vm.playItem(sec, item) } }
    }
}

@Composable
fun ItemCard(item: HomeItem, onClick: () -> Unit) {
    Column(Modifier.width(150.dp).bounceClick(onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AsyncImage(
            model = item.thumb,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(150.dp).clip(if (item.kind == Kind.ARTIST) CircleShape else RoundedCornerShape(20.dp))
        )
        Text(
            item.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

// song list item: thumbnail (or number), title, artist, download state and the three dot menu
@Composable
fun TrackRow(
    vm: MusicVM,
    t: Track,
    modifier: Modifier = Modifier,
    number: Int? = null,
    horizontalPad: Dp = 16.dp,
    onClick: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val progress = Downloads.active[t.id]
    val offline = t.id in Downloads.ids
    val current = vm.queue.getOrNull(vm.queueIndex)?.mediaId == t.id && vm.nowPlaying != null

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPad - 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (current) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 0.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (number != null && !current) {
            Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                Text("$number", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = t.thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                )
                if (current) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.graphic_eq), null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                t.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                color = if (current) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (offline) "${t.artist} • Offline" else t.artist,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (progress != null) CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Box {
            RoundIcon(
                R.drawable.more_vert, { menu = true }, size = 40.dp, iconSize = 20.dp,
                container = Color.Transparent, tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Play next") }, onClick = { menu = false; vm.playNext(t) },
                    leadingIcon = { Icon(painterResource(R.drawable.playlist_play), null) }
                )
                DropdownMenuItem(
                    text = { Text("Add to queue") }, onClick = { menu = false; vm.addToQueue(t) },
                    leadingIcon = { Icon(painterResource(R.drawable.queue_music), null) }
                )
                DropdownMenuItem(
                    text = { Text("Not interested") }, onClick = { menu = false; vm.notInterested(t.id, t.title) },
                    leadingIcon = { Icon(painterResource(R.drawable.block), null) }
                )
                if (offline) {
                    DropdownMenuItem(
                        text = { Text("Remove download") }, onClick = { menu = false; Downloads.delete(t.id) },
                        leadingIcon = { Icon(painterResource(R.drawable.delete), null) }
                    )
                } else if (progress == null) {
                    DropdownMenuItem(
                        text = { Text("Download") }, onClick = { menu = false; vm.download(t) },
                        leadingIcon = { Icon(painterResource(R.drawable.download), null) }
                    )
                }
                t.artistId?.let { aid ->
                    DropdownMenuItem(
                        text = { Text("Go to artist") }, onClick = { menu = false; vm.openArtist(aid, t.artist, null) },
                        leadingIcon = { Icon(painterResource(R.drawable.artist), null) }
                    )
                }
            }
        }
    }
}

// non song search result (album, playlist, artist)
@Composable
fun ItemRow(item: HomeItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(8.dp),
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
        Icon(painterResource(R.drawable.navigate_next), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun MusicVM.displayName(): String = customName.ifBlank { accountName.ifBlank { "Guest" } }

// profile photo: the one picked from the gallery wins, then the youtube music photo, then the first letter
@Composable
fun Avatar(vm: MusicVM, size: Dp, modifier: Modifier = Modifier) {
    val model: Any? = if (vm.customAvatar.isNotBlank()) java.io.File(vm.customAvatar) else vm.accountPhoto.ifBlank { null }
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                vm.displayName().take(1).uppercase(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                style = if (size > 80.dp) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleMedium
            )
        }
    }
}
