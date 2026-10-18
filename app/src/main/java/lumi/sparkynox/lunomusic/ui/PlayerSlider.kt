package lumi.sparkynox.lunomusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// slim rounded track with no thumb, the look of Echo Music's player slider (GPL-3.0, github.com/iad1tya/Echo-Music)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSliderTrack(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    colors: SliderColors = SliderDefaults.colors(),
    trackHeight: Dp = 10.dp,
) {
    val range = sliderState.valueRange
    val span = range.endInclusive - range.start
    val frac = if (span == 0f) 0f else ((sliderState.value - range.start) / span).coerceIn(0f, 1f)
    Canvas(modifier.fillMaxWidth().height(trackHeight)) {
        val stroke = trackHeight.toPx()
        val half = stroke / 2f
        drawLine(colors.inactiveTrackColor, Offset(half, center.y), Offset(size.width - half, center.y), stroke, StrokeCap.Round)
        val end = half + (size.width - stroke) * frac
        drawLine(colors.activeTrackColor, Offset(half, center.y), Offset(end, center.y), stroke, StrokeCap.Round)
    }
}
