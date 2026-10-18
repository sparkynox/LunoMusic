package lumi.sparkynox.lunomusic.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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

/*
 * Color picking follows Echo Music's PlayerColorExtractor (GPL-3.0, github.com/iad1tya/Echo-Music):
 * take the swatch that is both common and vivid, boost its saturation, keep brightness in a comfortable range.
 */
private fun vivid(c: Int): Boolean {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c, hsv)
    return hsv[1] > 0.25f && hsv[2] > 0.2f && hsv[2] < 0.9f
}

private fun boost(c: Int, sat: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c, hsv)
    hsv[1] = (hsv[1] * sat).coerceAtMost(1f)
    hsv[2] = (hsv[2] * 0.9f).coerceIn(0.4f, 0.85f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun weight(sw: Palette.Swatch): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(sw.rgb, hsv)
    val bonus = if (hsv[1] > 0.3f && hsv[2] > 0.3f) 1.5f else 1f
    return sw.population * 2f * bonus * (hsv[1] + hsv[2]) / 2f
}

// three stops for the player background: vivid color of the cover, a darker copy, black
@Composable
fun rememberArtGradient(url: Any?, fallback: Color): List<Color> {
    val ctx = LocalContext.current
    val target = remember { mutableStateOf(listOf(fallback, fallback, Color.Black)) }

    LaunchedEffect(url, fallback) {
        val picked = if (url == null) null else withContext(Dispatchers.IO) {
            runCatching {
                val req = ImageRequest.Builder(ctx).data(url).size(128).allowHardware(false).build()
                val bmp = (ctx.imageLoader.execute(req) as? SuccessResult)?.drawable?.toBitmap() ?: return@runCatching null
                val pal = Palette.from(bmp).maximumColorCount(32).generate()
                val all = listOfNotNull(
                    pal.dominantSwatch, pal.vibrantSwatch, pal.darkVibrantSwatch, pal.lightVibrantSwatch,
                    pal.mutedSwatch, pal.darkMutedSwatch, pal.lightMutedSwatch
                )
                val best = all.maxByOrNull { weight(it) }
                val base = if (best != null && vivid(best.rgb)) boost(best.rgb, 1.3f)
                else boost(pal.dominantSwatch?.rgb ?: pal.getDominantColor(fallback.toArgb()), 1.1f)
                listOf(base, Color(base.red * 0.6f, base.green * 0.6f, base.blue * 0.6f), Color.Black)
            }.getOrNull()
        }
        target.value = picked ?: listOf(fallback, fallback, Color.Black)
    }

    val a by animateColorAsState(target.value[0], tween(800), label = "g0")
    val b by animateColorAsState(target.value[1], tween(800), label = "g1")
    val c by animateColorAsState(target.value[2], tween(800), label = "g2")
    return listOf(a, b, c)
}

// Heavy blur that works on every Android version (Modifier.blur only does something on 12+): the cover is shrunk to a
// tiny image, box blurred a few times and the screen stretches it back up smoothly.
class SoftBlur(private val passes: Int = 3, private val radius: Int = 4) : coil.transform.Transformation {
    override val cacheKey = "softblur-$passes-$radius"

    override suspend fun transform(input: android.graphics.Bitmap, size: coil.size.Size): android.graphics.Bitmap {
        val n = 48
        val small = android.graphics.Bitmap.createScaledBitmap(input, n, n, true)
            .copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        val px = IntArray(n * n)
        small.getPixels(px, 0, n, 0, 0, n, n)
        repeat(passes) {
            boxPass(px, n, true)
            boxPass(px, n, false)
        }
        small.setPixels(px, 0, n, 0, 0, n, n)
        return small
    }

    private fun boxPass(px: IntArray, n: Int, horizontal: Boolean) {
        val out = IntArray(n * n)
        for (line in 0 until n) {
            for (i in 0 until n) {
                var r = 0; var g = 0; var b = 0; var cnt = 0
                for (k in -radius..radius) {
                    val j = (i + k).coerceIn(0, n - 1)
                    val c = if (horizontal) px[line * n + j] else px[j * n + line]
                    r += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF; cnt++
                }
                val v = (0xFF shl 24) or ((r / cnt) shl 16) or ((g / cnt) shl 8) or (b / cnt)
                if (horizontal) out[line * n + i] = v else out[i * n + line] = v
            }
        }
        System.arraycopy(out, 0, px, 0, out.size)
    }
}
