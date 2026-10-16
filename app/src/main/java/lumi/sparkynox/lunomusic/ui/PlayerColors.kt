package lumi.sparkynox.lunomusic.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// dominant dark tone of the artwork, the player background fades to it
@Composable
fun rememberArtColor(url: Any?, fallback: Color): State<Color> {
    val ctx = LocalContext.current
    val target = remember { mutableStateOf(fallback) }

    LaunchedEffect(url, fallback) {
        val picked = if (url == null) null else withContext(Dispatchers.IO) {
            runCatching {
                val req = ImageRequest.Builder(ctx).data(url).size(128).allowHardware(false).build()
                val bmp = (ctx.imageLoader.execute(req) as? SuccessResult)?.drawable?.toBitmap()
                    ?: return@runCatching null
                val pal = Palette.from(bmp).maximumColorCount(16).generate()
                val sw = pal.darkMutedSwatch ?: pal.darkVibrantSwatch ?: pal.mutedSwatch ?: pal.dominantSwatch
                sw?.let { Color(it.rgb) }
            }.getOrNull()
        }
        target.value = picked ?: fallback
    }

    return animateColorAsState(target.value, tween(700), label = "artColor")
}
