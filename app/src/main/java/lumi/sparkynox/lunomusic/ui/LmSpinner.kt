package lumi.sparkynox.lunomusic.ui

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// loading indicator: ring spins around, the LM flips on its own axis
@Composable
fun LmSpinner(modifier: Modifier = Modifier, size: Dp = 72.dp) {
    val t = rememberInfiniteTransition(label = "lm")
    val ring by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "ring")
    val flip by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "flip")
    val color = MaterialTheme.colorScheme.primary

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = ring }) {
            val w = this.size.minDimension * 0.07f
            drawArc(
                color = color.copy(alpha = 0.18f), startAngle = 0f, sweepAngle = 360f, useCenter = false,
                style = Stroke(w)
            )
            drawArc(
                color = color, startAngle = 0f, sweepAngle = 100f, useCenter = false,
                style = Stroke(w, cap = StrokeCap.Round)
            )
        }
        Text(
            "LM", color = color, fontWeight = FontWeight.Black, fontSize = (size.value * 0.34f).sp,
            modifier = Modifier.graphicsLayer { rotationY = flip; cameraDistance = 12f * density }
        )
    }
}
