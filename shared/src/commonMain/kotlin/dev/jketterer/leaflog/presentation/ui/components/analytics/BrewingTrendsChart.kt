package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jketterer.leaflog.domain.models.TrendGranularity
import dev.jketterer.leaflog.domain.models.TrendPoint
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.LocalDate

@Composable
fun BrewingTrendsChart(
    trendPoints: List<TrendPoint>,
    granularity: TrendGranularity,
    onTapPoint: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allZero = trendPoints.all { it.sessionCount == 0 }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Brewing Trends",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (allZero || trendPoints.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No brewing sessions in this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                BrewingTrendsCanvas(
                    trendPoints = trendPoints,
                    granularity = granularity,
                    onTapPoint = onTapPoint,
                )
            }
        }
    }
}

@Composable
private fun BrewingTrendsCanvas(
    trendPoints: List<TrendPoint>,
    granularity: TrendGranularity,
    onTapPoint: (LocalDate) -> Unit,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val pointColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    val monthNames = remember {
        arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
    val isMultiYear = remember(trendPoints) {
        if (trendPoints.size < 2) false
        else trendPoints.first().date.year != trendPoints.last().date.year
    }

    val maxCount = remember(trendPoints) { trendPoints.maxOf { it.sessionCount }.coerceAtLeast(1) }
    val yAxisMax = remember(maxCount) { ((maxCount / 2) + 1) * 2 } // Round up to even number

    // Pick evenly-spaced x-axis labels (3-5)
    val labelCount = when {
        trendPoints.size <= 3 -> trendPoints.size
        trendPoints.size <= 7 -> 3
        else -> 5
    }.coerceAtLeast(2)

    val labelIndices = remember(trendPoints.size, labelCount) {
        if (trendPoints.size <= labelCount) {
            trendPoints.indices.toList()
        } else {
            (0 until labelCount).map { i ->
                (i * (trendPoints.size - 1)) / (labelCount - 1)
            }
        }
    }

    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = labelColor,
        textAlign = TextAlign.Center,
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(trendPoints) {
                detectTapGestures { offset ->
                    val leftPadding = 30.dp.toPx()
                    val rightPadding = 8.dp.toPx()
                    val chartWidth = size.width - leftPadding - rightPadding
                    if (trendPoints.size < 2) return@detectTapGestures

                    val stepX = chartWidth / (trendPoints.size - 1)
                    val tappedIndex = ((offset.x - leftPadding) / stepX)
                        .toInt()
                        .coerceIn(0, trendPoints.size - 1)

                    // Check proximity
                    val pointX = leftPadding + tappedIndex * stepX
                    if (kotlin.math.abs(offset.x - pointX) < stepX / 2 + 20.dp.toPx()) {
                        onTapPoint(trendPoints[tappedIndex].date)
                    }
                }
            },
    ) {
        val leftPadding = 30.dp.toPx()
        val rightPadding = 8.dp.toPx()
        val topPadding = 8.dp.toPx()
        val bottomPadding = 24.dp.toPx()

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        // Draw Y-axis labels
        val ySteps = 3
        for (i in 0..ySteps) {
            val value = (yAxisMax * i) / ySteps
            val y = topPadding + chartHeight - (chartHeight * i / ySteps)
            val textResult = textMeasurer.measure(
                text = value.toString(),
                style = labelStyle,
            )
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(
                    x = leftPadding - textResult.size.width - 4.dp.toPx(),
                    y = y - textResult.size.height / 2f,
                ),
            )
        }

        // Calculate points
        val stepX = if (trendPoints.size > 1) chartWidth / (trendPoints.size - 1) else 0f
        val points = trendPoints.mapIndexed { index, point ->
            val x = leftPadding + index * stepX
            val y = topPadding + chartHeight - (point.sessionCount.toFloat() / yAxisMax * chartHeight)
            Offset(x, y)
        }

        // Draw gradient fill under line
        if (points.size >= 2) {
            val fillPath = Path().apply {
                moveTo(points.first().x, topPadding + chartHeight)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, topPadding + chartHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fillColor, Color.Transparent),
                    startY = topPadding,
                    endY = topPadding + chartHeight,
                ),
            )

            // Draw line
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // Draw data point circles
        points.forEach { point ->
            drawCircle(
                color = pointColor,
                radius = 4.dp.toPx(),
                center = point,
            )
        }

        // Draw X-axis date labels
        labelIndices.forEach { index ->
            if (index < trendPoints.size) {
                val date = trendPoints[index].date
                val label = when (granularity) {
                    TrendGranularity.MONTHLY -> {
                        val name = monthNames[date.month.ordinal]
                        if (isMultiYear) "$name '${date.year % 100}" else name
                    }
                    else -> "${date.month.ordinal + 1}/${date.day}"
                }
                val textResult = textMeasurer.measure(
                    text = label,
                    style = labelStyle,
                )
                val x = leftPadding + index * stepX
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(
                        x = x - textResult.size.width / 2f,
                        y = topPadding + chartHeight + 4.dp.toPx(),
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingTrendsChartPreview() {
    LeafLogTheme {
        BrewingTrendsChart(
            trendPoints = listOf(
                TrendPoint(LocalDate(2024, 1, 1), 2),
                TrendPoint(LocalDate(2024, 1, 2), 4),
                TrendPoint(LocalDate(2024, 1, 3), 1),
                TrendPoint(LocalDate(2024, 1, 4), 6),
                TrendPoint(LocalDate(2024, 1, 5), 3),
                TrendPoint(LocalDate(2024, 1, 6), 5),
                TrendPoint(LocalDate(2024, 1, 7), 2),
            ),
            granularity = TrendGranularity.DAILY,
            onTapPoint = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingTrendsChartMonthlyPreview() {
    LeafLogTheme {
        BrewingTrendsChart(
            trendPoints = listOf(
                TrendPoint(LocalDate(2024, 1, 1), 8),
                TrendPoint(LocalDate(2024, 2, 1), 12),
                TrendPoint(LocalDate(2024, 3, 1), 5),
                TrendPoint(LocalDate(2024, 4, 1), 15),
                TrendPoint(LocalDate(2024, 5, 1), 10),
                TrendPoint(LocalDate(2024, 6, 1), 18),
                TrendPoint(LocalDate(2024, 7, 1), 7),
            ),
            granularity = TrendGranularity.MONTHLY,
            onTapPoint = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingTrendsChartEmptyPreview() {
    LeafLogTheme {
        BrewingTrendsChart(
            trendPoints = listOf(
                TrendPoint(LocalDate(2024, 1, 1), 0),
                TrendPoint(LocalDate(2024, 1, 2), 0),
            ),
            granularity = TrendGranularity.DAILY,
            onTapPoint = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
