package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.analytics_share
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** One slice of a [DistributionDonutCard]: [count] sessions, [percentage] of them all. */
internal data class DonutSlice(
    val id: String,
    val label: String,
    val count: Int,
    val percentage: Float,
    val color: Color,
)

private val DONUT_SIZE = 160.dp
private val RING_WIDTH = 30.dp

/**
 * A titled donut of [slices] with the total in its hole, and a legend whose rows tap like their
 * slices. [summary] takes the slices' shares to describe the donut to a screen reader.
 */
@Composable
internal fun DistributionDonutCard(
    title: String,
    slices: List<DonutSlice>,
    summary: StringResource,
    onTapSlice: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(DONUT_SIZE),
                ) {
                    val shares = slices.map {
                        stringResource(Res.string.analytics_share, it.label, it.percentage.roundToInt())
                    }
                    DonutCanvas(
                        slices = slices,
                        description = stringResource(summary, shares.joinToString()),
                        onTapSlice = onTapSlice,
                    )
                    Text(
                        text = slices.sumOf { it.count }.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    slices.forEach { slice ->
                        LegendRow(slice = slice, onClick = { onTapSlice(slice.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(
    slice: DonutSlice,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(slice.color)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = slice.label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = slice.count.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DonutCanvas(
    slices: List<DonutSlice>,
    description: String,
    onTapSlice: (id: String) -> Unit,
) {
    val total = slices.sumOf { it.count }.toFloat()
    val sweeps = slices.map { it.count / total * 360f }
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = Modifier
            .size(DONUT_SIZE)
            .semantics { contentDescription = description }
            .pointerInput(slices) {
                detectTapGestures { offset ->
                    val outerRadius = size.width / 2f
                    val index = donutSegmentAt(
                        dx = offset.x - size.width / 2f,
                        dy = offset.y - size.height / 2f,
                        innerRadius = outerRadius - RING_WIDTH.toPx(),
                        outerRadius = outerRadius,
                        sweeps = sweeps,
                    )
                    if (index != null) onTapSlice(slices[index].id)
                }
            },
    ) {
        val outlineWidth = 1.dp.toPx()
        val outer = Rect(Offset.Zero, size).deflate(outlineWidth / 2)
        val inner = Rect(Offset.Zero, size).deflate(RING_WIDTH.toPx())
        var startAngle = -90f
        slices.forEachIndexed { index, slice ->
            val sweep = (sweeps[index] - 1f).coerceAtLeast(0.5f) // a small gap between slices
            val path = Path().apply {
                arcTo(outer, startAngle, sweep, forceMoveTo = true)
                arcTo(inner, startAngle + sweep, -sweep, forceMoveTo = false)
                close()
            }
            drawPath(path, slice.color)
            // Outlined, so a pale tea type color reads on any surface
            drawPath(path, outlineColor, style = Stroke(width = outlineWidth))
            startAngle += sweeps[index]
        }
    }
}
