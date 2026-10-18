package lumi.sparkynox.lunomusic.ui

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import lumi.sparkynox.lunomusic.R

// loading indicator: ring spins around, the LM flips on its own axis
@Composable
fun LmSpinner(modifier: Modifier = Modifier, size: Dp = 72.dp) {
    val t = rememberInfiniteTransition(label = "lm")
    val ring by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "ring")
    val flip by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "flip")
    val color = MaterialTheme.colorScheme.primary
    val ctx = LocalContext.current
    val px = with(LocalDensity.current) { size.toPx() }

    // A Text is centered by its line box, and a font's ascent and descent are never symmetric around the capital
    // letters, so the LM sat a bit off. Here the letters are measured and centered by their own bounds instead.
    val paint = remember(color, px) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = runCatching { ResourcesCompat.getFont(ctx, R.font.poppins) }.getOrNull() ?: Typeface.DEFAULT_BOLD
            isFakeBoldText = true
            this.color = color.toArgb()
            textSize = px * 0.36f
        }
    }
    val bounds = remember(paint) { Rect().also { paint.getTextBounds("LM", 0, 2, it) } }

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = ring }) {
            // the stroke is drawn half inside, half outside its rectangle, so inset it to keep the ring inside the box
            val w = this.size.minDimension * 0.07f
            val arcTopLeft = Offset(w / 2, w / 2)
            val arcSize = Size(this.size.width - w, this.size.height - w)
            drawArc(
                color = color.copy(alpha = 0.18f), startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = arcTopLeft, size = arcSize, style = Stroke(w)
            )
            drawArc(
                color = color, startAngle = 0f, sweepAngle = 100f, useCenter = false,
                topLeft = arcTopLeft, size = arcSize, style = Stroke(w, cap = StrokeCap.Round)
            )
        }
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationY = flip; cameraDistance = 12f * density }) {
            drawIntoCanvas {
                it.nativeCanvas.drawText("LM", center.x - bounds.exactCenterX(), center.y - bounds.exactCenterY(), paint)
            }
        }
    }
}
