package lumi.sparkynox.lunomusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp

@Composable
fun PlayPauseIcon(playing: Boolean, dp: Dp, tint: Color) {
    Canvas(Modifier.size(dp)) {
        val w = size.width
        val h = size.height
        if (playing) {
            drawRoundRect(tint, Offset(w * 0.24f, h * 0.2f), Size(w * 0.2f, h * 0.6f), CornerRadius(w * 0.05f))
            drawRoundRect(tint, Offset(w * 0.56f, h * 0.2f), Size(w * 0.2f, h * 0.6f), CornerRadius(w * 0.05f))
        } else {
            val p = Path().apply {
                moveTo(w * 0.3f, h * 0.2f)
                lineTo(w * 0.8f, h * 0.5f)
                lineTo(w * 0.3f, h * 0.8f)
                close()
            }
            drawPath(p, tint)
        }
    }
}

@Composable
fun SkipIcon(forward: Boolean, dp: Dp, tint: Color) {
    Canvas(Modifier.size(dp)) {
        val w = size.width
        val h = size.height
        scale(if (forward) 1f else -1f, 1f, center) {
            val p = Path().apply {
                moveTo(w * 0.2f, h * 0.2f)
                lineTo(w * 0.68f, h * 0.5f)
                lineTo(w * 0.2f, h * 0.8f)
                close()
            }
            drawPath(p, tint)
            drawRoundRect(tint, Offset(w * 0.72f, h * 0.2f), Size(w * 0.1f, h * 0.6f), CornerRadius(w * 0.03f))
        }
    }
}
