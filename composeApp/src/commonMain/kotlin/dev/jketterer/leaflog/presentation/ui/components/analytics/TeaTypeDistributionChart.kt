package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TeaTypeDistribution
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun TeaTypeDistributionChart(
    distribution: List<TeaTypeDistribution>,
    onTapSegment: (teaTypeId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Tea Type Distribution",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (distribution.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val totalSessions = remember(distribution) {
                    distribution.sumOf { it.sessionCount }
                }

                val colors = remember(distribution) {
                    distribution.mapIndexed { index, dist ->
                        getChartColor(dist.teaType.colorHex, index)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Donut chart
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp),
                    ) {
                        DonutCanvas(
                            distribution = distribution,
                            colors = colors,
                            onTapSegment = onTapSegment,
                        )
                        Text(
                            text = totalSessions.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Legend
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        distribution.forEachIndexed { index, dist ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(colors[index]),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = dist.teaType.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = dist.sessionCount.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutCanvas(
    distribution: List<TeaTypeDistribution>,
    colors: List<androidx.compose.ui.graphics.Color>,
    onTapSegment: (teaTypeId: String) -> Unit,
) {
    // Pre-calculate segment angles
    val segments = remember(distribution) {
        val total = distribution.sumOf { it.sessionCount }.toFloat()
        var startAngle = -90f
        distribution.map { dist ->
            val sweepAngle = (dist.sessionCount / total) * 360f
            val segment = SegmentInfo(
                teaTypeId = dist.teaType.id,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
            )
            startAngle += sweepAngle
            segment
        }
    }

    Canvas(
        modifier = Modifier
            .size(160.dp)
            .pointerInput(distribution) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val dx = offset.x - centerX
                    val dy = offset.y - centerY
                    val distance = sqrt(dx * dx + dy * dy)

                    val outerRadius = size.width / 2f
                    val innerRadius = outerRadius - 30.dp.toPx()

                    // Check if tap is in donut ring
                    if (distance in innerRadius..outerRadius) {
                        // Calculate angle in degrees (0 = right, clockwise)
                        var angle = atan2(dy, dx) * (180f / PI.toFloat())
                        // Convert to match drawing start (-90 = top)
                        angle = (angle + 360) % 360

                        for (segment in segments) {
                            val normalizedStart = (segment.startAngle + 90 + 360) % 360
                            val normalizedEnd = (normalizedStart + segment.sweepAngle) % 360

                            val inSegment = if (normalizedEnd > normalizedStart) {
                                angle in normalizedStart..normalizedEnd
                            } else {
                                angle >= normalizedStart || angle <= normalizedEnd
                            }

                            if (inSegment) {
                                onTapSegment(segment.teaTypeId)
                                break
                            }
                        }
                    }
                }
            },
    ) {
        val strokeWidth = 30.dp.toPx()
        val padding = strokeWidth / 2
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(padding, padding)

        segments.forEachIndexed { index, segment ->
            drawArc(
                color = colors[index],
                startAngle = segment.startAngle,
                sweepAngle = segment.sweepAngle - 1f, // Small gap between segments
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            )
        }
    }
}

private data class SegmentInfo(
    val teaTypeId: String,
    val startAngle: Float,
    val sweepAngle: Float,
)

@Preview(showBackground = true)
@Composable
private fun TeaTypeDistributionChartPreview() {
    LeafLogTheme {
        TeaTypeDistributionChart(
            distribution = listOf(
                TeaTypeDistribution(
                    teaType = TeaType(id = "1", name = "Green", colorHex = "#4CAF50"),
                    sessionCount = 15,
                    percentage = 37.5f,
                ),
                TeaTypeDistribution(
                    teaType = TeaType(id = "2", name = "Black", colorHex = "#795548"),
                    sessionCount = 10,
                    percentage = 25f,
                ),
                TeaTypeDistribution(
                    teaType = TeaType(id = "3", name = "Oolong", colorHex = "#FF9800"),
                    sessionCount = 8,
                    percentage = 20f,
                ),
                TeaTypeDistribution(
                    teaType = TeaType(id = "4", name = "White", colorHex = "#FFEB3B"),
                    sessionCount = 4,
                    percentage = 10f,
                ),
                TeaTypeDistribution(
                    teaType = TeaType(id = "5", name = "Pu-erh", colorHex = "#3E2723"),
                    sessionCount = 3,
                    percentage = 7.5f,
                ),
            ),
            onTapSegment = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TeaTypeDistributionChartEmptyPreview() {
    LeafLogTheme {
        TeaTypeDistributionChart(
            distribution = emptyList(),
            onTapSegment = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
