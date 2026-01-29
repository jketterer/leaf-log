package dev.jketterer.leaflog.presentation.ui.components.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

@Composable
fun CircularTimerRing(
    progress: Float,
    timeText: String,
    modifier: Modifier = Modifier,
    ringColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "timer_progress",
    )

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val canvasSize = size.minDimension
            val strokeWidth = 16.dp.toPx()
            val radius = (canvasSize - strokeWidth) / 2

            // Background circle
            drawCircle(
                color = backgroundColor,
                radius = radius,
                style = Stroke(width = strokeWidth),
            )

            // Progress arc
            val sweepAngle = 360f * animatedProgress
            drawArc(
                color = ringColor,
                startAngle = -90f,  // Start from top
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                ),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(canvasSize - strokeWidth, canvasSize - strokeWidth),
            )
        }

        // Time text
        Text(
            text = timeText,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CircularTimerRingPreview() {
    LeafLogTheme {
        CircularTimerRing(
            progress = 0.65f,
            timeText = "1:30",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CircularTimerRingCompletePreview() {
    LeafLogTheme {
        CircularTimerRing(
            progress = 1.0f,
            timeText = "0:00",
        )
    }
}
