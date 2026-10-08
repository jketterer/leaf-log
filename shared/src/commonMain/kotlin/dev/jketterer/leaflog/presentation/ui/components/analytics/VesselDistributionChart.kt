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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.VesselDistribution
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.math.roundToInt
import kotlin.time.Instant

@Composable
fun VesselDistributionChart(
    distribution: List<VesselDistribution>,
    onTapSegment: (vesselId: String) -> Unit,
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
                text = "Vessel Distribution",
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
                        text = "No vessel data for this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val totalSessions = remember(distribution) {
                    distribution.sumOf { it.sessionCount }
                }

                val colors = remember(distribution) {
                    distribution.mapIndexed { index, _ ->
                        getChartColor("", index)
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
                        VesselDonutCanvas(
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
                                    text = dist.vessel.name,
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
private fun VesselDonutCanvas(
    distribution: List<VesselDistribution>,
    colors: List<androidx.compose.ui.graphics.Color>,
    onTapSegment: (vesselId: String) -> Unit,
) {
    val segments = remember(distribution) {
        val total = distribution.sumOf { it.sessionCount }.toFloat()
        var startAngle = -90f
        distribution.map { dist ->
            val sweepAngle = (dist.sessionCount / total) * 360f
            val segment = VesselSegmentInfo(
                vesselId = dist.vessel.id,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
            )
            startAngle += sweepAngle
            segment
        }
    }

    val summary = remember(distribution) {
        distribution.joinToString(prefix = "Sessions by vessel: ") {
            "${it.vessel.name} ${it.percentage.roundToInt()}%"
        }
    }

    Canvas(
        modifier = Modifier
            .size(160.dp)
            .semantics { contentDescription = summary }
            .pointerInput(distribution) {
                detectTapGestures { offset ->
                    val outerRadius = size.width / 2f
                    val index = donutSegmentAt(
                        dx = offset.x - size.width / 2f,
                        dy = offset.y - size.height / 2f,
                        innerRadius = outerRadius - 30.dp.toPx(),
                        outerRadius = outerRadius,
                        sweeps = segments.map { it.sweepAngle },
                    )
                    if (index != null) onTapSegment(segments[index].vesselId)
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
                sweepAngle = segment.sweepAngle - 1f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            )
        }
    }
}

private data class VesselSegmentInfo(
    val vesselId: String,
    val startAngle: Float,
    val sweepAngle: Float,
)

@Preview(showBackground = true)
@Composable
private fun VesselDistributionChartPreview() {
    val now = Instant.fromEpochMilliseconds(0)
    LeafLogTheme {
        VesselDistributionChart(
            distribution = listOf(
                VesselDistribution(
                    vessel = BrewingVessel(id = "1", name = "Gaiwan", iconName = null, isSystemDefault = false, displayOrder = 0, createdAt = now, updatedAt = now),
                    sessionCount = 18,
                    percentage = 45f,
                ),
                VesselDistribution(
                    vessel = BrewingVessel(id = "2", name = "Teapot", iconName = null, isSystemDefault = false, displayOrder = 1, createdAt = now, updatedAt = now),
                    sessionCount = 12,
                    percentage = 30f,
                ),
                VesselDistribution(
                    vessel = BrewingVessel(id = "3", name = "French Press", iconName = null, isSystemDefault = false, displayOrder = 2, createdAt = now, updatedAt = now),
                    sessionCount = 10,
                    percentage = 25f,
                ),
            ),
            onTapSegment = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselDistributionChartEmptyPreview() {
    LeafLogTheme {
        VesselDistributionChart(
            distribution = emptyList(),
            onTapSegment = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
