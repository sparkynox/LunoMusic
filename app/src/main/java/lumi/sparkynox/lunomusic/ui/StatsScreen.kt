package lumi.sparkynox.lunomusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.data.Play
import lumi.sparkynox.lunomusic.data.Stats
import lumi.sparkynox.lunomusic.extract.Track
import java.util.Calendar

private val fixedPalette = listOf(Color(0xFFFF5C7A), Color(0xFF6BCB77), Color(0xFF4D96FF), Color(0xFFB983FF))

private fun dur(ms: Long): String {
    val m = ms / 60000
    return if (m >= 60) "${m / 60}h ${m % 60}m" else if (m > 0) "${m}m ${(ms / 1000) % 60}s" else "${ms / 1000}s"
}

private fun firstArtist(a: String) = a.substringBefore(",").trim().ifEmpty { "Unknown" }

private fun dayStart(offset: Int): Long {
    val c = Calendar.getInstance()
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    c.add(Calendar.DAY_OF_YEAR, -offset)
    return c.timeInMillis
}

@Composable
fun StatsScreen(vm: MusicVM) {
    var period by remember { mutableIntStateOf(0) } // 0 week, 1 month, 2 all
    var plays by remember { mutableStateOf<List<Play>>(emptyList()) }

    LaunchedEffect(period) {
        val from = when (period) {
            0 -> dayStart(6)
            1 -> dayStart(29)
            else -> 0L
        }
        plays = withContext(Dispatchers.IO) { Stats.since(from) }
    }

    val palette = listOf(MaterialTheme.colorScheme.primary) + fixedPalette
    val total = plays.sumOf { it.ms }
    val bySong = plays.groupBy { it.id }
    val byArtist = plays.groupBy { firstArtist(it.artist) }.mapValues { e -> e.value.sumOf { it.ms } }.toList().sortedByDescending { it.second }
    val topSongs = bySong.values.sortedByDescending { it.size }.take(10)
    val days = (6 downTo 0).map { off ->
        val s = dayStart(off)
        s to plays.filter { it.ts >= s && it.ts < s + 86_400_000L }.sumOf { it.ms }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPad) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = vm::pop) { Text("Back") }
            }
            Text("Stats", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp))
            Text("Counted when you listen to at least 15 seconds of a song", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
        }
        item {
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Last 7 days", "Last 30 days", "All time").withIndex().toList()) { (i, label) ->
                    FilterChip(
                        selected = period == i, onClick = { period = i }, label = { Text(label) },
                        shape = CircleShape, border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }

        if (plays.isEmpty()) {
            item { Text("No plays yet, go listen to something", modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            return@LazyColumn
        }

        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Total time listened", style = MaterialTheme.typography.bodyMedium)
                    Text(dur(total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat(plays.size.toString(), "Plays")
                        Stat(bySong.size.toString(), "Unique songs")
                        Stat(byArtist.size.toString(), "Artists")
                    }
                }
            }
        }

        item {
            SectionHeader("Artist breakdown")
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    val top = byArtist.take(5)
                    val sum = top.sumOf { it.second }.coerceAtLeast(1L)
                    Canvas(Modifier.size(120.dp)) {
                        val stroke = 26.dp.toPx()
                        var start = -90f
                        top.forEachIndexed { i, (_, ms) ->
                            val sweep = 360f * ms / sum
                            drawArc(
                                palette[i % palette.size], start, (sweep - 2f).coerceAtLeast(1f), false,
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(stroke, cap = StrokeCap.Butt)
                            )
                            start += sweep
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        top.forEachIndexed { i, (name, ms) ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(palette[i % palette.size]))
                                Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false))
                                Text("${100 * ms / sum}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            val artist = byArtist.firstOrNull()
            val song = bySong.values.maxByOrNull { it.size }?.first()
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                artist?.let { (name, ms) ->
                    HighlightCard("Your favourite artist", name, dur(ms), plays.firstOrNull { firstArtist(it.artist) == name }?.thumb, true)
                }
                song?.let { s ->
                    HighlightCard("Your favourite song", s.title, "${bySong[s.id]?.size} plays", s.thumb, false)
                }
            }
        }

        item {
            SectionHeader("Listening by day")
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                val max = (days.maxOf { it.second }).coerceAtLeast(1L)
                Row(Modifier.fillMaxWidth().height(140.dp).padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                    days.forEach { (start, ms) ->
                        val cal = Calendar.getInstance().apply { timeInMillis = start }
                        val label = "SMTWTFS"[cal.get(Calendar.DAY_OF_WEEK) - 1].toString()
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(width = 18.dp, height = maxOf(4.dp, 80.dp * (ms.toFloat() / max.toFloat())))
                                    .clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary)
                            )
                            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item { SectionHeader("Top songs") }
        items(topSongs) { group ->
            val s = group.first()
            val t = Track(s.id, s.title, s.artist, s.thumb, 0L)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(model = s.thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)))
                Column(Modifier.weight(1f)) {
                    Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text("${firstArtist(s.artist)} • ${group.size} plays", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { vm.playTracks(listOf(t), 0) }) { Text("Play") }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HighlightCard(label: String, title: String, sub: String, thumb: String?, round: Boolean) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AsyncImage(
                model = thumb, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(if (round) CircleShape else RoundedCornerShape(14.dp))
            )
            Column {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
