package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jketterer.leaflog.domain.models.ActivityCell
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")
private val MONTH_NAMES = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

@Composable
fun BrewingActivityHeatmap(
    cells: List<ActivityCell>,
    period: AnalyticsPeriod,
    modifier: Modifier = Modifier,
) {
    if (cells.isEmpty()) return

    val isWeekView = period == AnalyticsPeriod.LAST_7_DAYS || period == AnalyticsPeriod.THIS_WEEK
    val isMonthView = period == AnalyticsPeriod.LAST_30_DAYS ||
        period == AnalyticsPeriod.THIS_MONTH ||
        period == AnalyticsPeriod.LAST_MONTH

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Brewing Activity",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            when {
                isWeekView -> WeekHeatmapCanvas(cells = cells)
                isMonthView -> MonthCalendarHeatmapCanvas(cells = cells)
                else -> YearlyHeatmapCanvas(cells = cells)
            }
        }
    }
}

@Composable
private fun WeekHeatmapCanvas(cells: List<ActivityCell>) {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val activeDefaultColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = labelColor,
        textAlign = TextAlign.Center,
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        val cellSize = 24.dp.toPx()
        val gap = 4.dp.toPx()
        val labelHeight = 14.dp.toPx()
        val labelGap = 6.dp.toPx()
        val cornerRadius = CornerRadius(4.dp.toPx())

        val totalCellWidth = 7 * cellSize + 6 * gap
        val startX = (size.width - totalCellWidth) / 2f

        // Day-of-week labels
        DAY_LABELS.forEachIndexed { index, label ->
            val cx = startX + index * (cellSize + gap) + cellSize / 2f
            val textResult = textMeasurer.measure(text = label, style = labelStyle)
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(cx - textResult.size.width / 2f, 0f),
            )
        }

        val cellTop = labelHeight + labelGap
        cells.take(7).forEachIndexed { index, cell ->
            val cellX = startX + index * (cellSize + gap)
            val color = cell.dominantTeaTypeColorHex?.let { hex ->
                val parsed = hex.hexToColor()
                if (parsed == Color.Unspecified) activeDefaultColor else parsed
            } ?: emptyColor
            drawRoundRect(
                color = color,
                topLeft = Offset(cellX, cellTop),
                size = Size(cellSize, cellSize),
                cornerRadius = cornerRadius,
            )
        }
    }
}

@Composable
private fun MonthCalendarHeatmapCanvas(cells: List<ActivityCell>) {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val activeDefaultColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = labelColor,
        textAlign = TextAlign.Center,
    )
    val monthStyle = TextStyle(
        fontSize = 12.sp,
        color = labelColor,
    )

    // Determine calendar grid alignment
    val firstDate = cells.first().date
    val emptyStart = firstDate.dayOfWeek.ordinal  // 0=Mon, 6=Sun
    val totalPositions = emptyStart + cells.size
    val numRows = (totalPositions + 6) / 7  // ceil division

    val cellSize = 24.dp
    val gap = 4.dp
    val monthLabelH = 20.dp
    val headerH = 18.dp
    val headerGap = 4.dp
    val canvasHeight: Dp = monthLabelH + headerH + headerGap +
        (cellSize + gap) * numRows - gap

    // Month label text: "February 2026"
    val monthLabel = "${MONTH_NAMES[firstDate.month.ordinal]} ${firstDate.year}"

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(canvasHeight),
    ) {
        val cellSizePx = cellSize.toPx()
        val gapPx = gap.toPx()
        val monthLabelHPx = monthLabelH.toPx()
        val headerHPx = headerH.toPx()
        val headerGapPx = headerGap.toPx()
        val cornerRadius = CornerRadius(4.dp.toPx())

        val gridWidth = 7 * cellSizePx + 6 * gapPx
        val startX = (size.width - gridWidth) / 2f

        // Month/year label
        val monthTextResult = textMeasurer.measure(text = monthLabel, style = monthStyle)
        drawText(
            textLayoutResult = monthTextResult,
            topLeft = Offset(startX, (monthLabelHPx - monthTextResult.size.height) / 2f),
        )

        // Day-of-week column headers
        DAY_LABELS.forEachIndexed { index, label ->
            val cx = startX + index * (cellSizePx + gapPx) + cellSizePx / 2f
            val textResult = textMeasurer.measure(text = label, style = labelStyle)
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(
                    cx - textResult.size.width / 2f,
                    monthLabelHPx + (headerHPx - textResult.size.height) / 2f,
                ),
            )
        }

        val cellsTop = monthLabelHPx + headerHPx + headerGapPx

        // Empty leading cells
        repeat(emptyStart) { index ->
            val col = index % 7
            val row = index / 7
            val cellX = startX + col * (cellSizePx + gapPx)
            val cellY = cellsTop + row * (cellSizePx + gapPx)
            drawRoundRect(
                color = emptyColor,
                topLeft = Offset(cellX, cellY),
                size = Size(cellSizePx, cellSizePx),
                cornerRadius = cornerRadius,
            )
        }

        // Data cells
        cells.forEachIndexed { index, cell ->
            val position = index + emptyStart
            val col = position % 7
            val row = position / 7
            val cellX = startX + col * (cellSizePx + gapPx)
            val cellY = cellsTop + row * (cellSizePx + gapPx)
            val color = cell.dominantTeaTypeColorHex?.let { hex ->
                val parsed = hex.hexToColor()
                if (parsed == Color.Unspecified) activeDefaultColor else parsed
            } ?: emptyColor
            drawRoundRect(
                color = color,
                topLeft = Offset(cellX, cellY),
                size = Size(cellSizePx, cellSizePx),
                cornerRadius = cornerRadius,
            )
        }
    }
}

@Composable
private fun YearlyHeatmapCanvas(cells: List<ActivityCell>) {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val activeDefaultColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = labelColor,
    )

    val cellSizeDp = 24.dp
    val gapDp = 4.dp
    val rowLabelHDp = 14.dp
    val rowGapDp = 6.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cols = maxOf(1, ((maxWidth.value + gapDp.value) / (cellSizeDp.value + gapDp.value)).toInt())
        val numRows = (cells.size + cols - 1) / cols
        val rowHeightDp = rowLabelHDp + cellSizeDp + rowGapDp
        val canvasHeight = rowHeightDp * numRows - rowGapDp

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight),
        ) {
            val cellSizePx = cellSizeDp.toPx()
            val gapPx = gapDp.toPx()
            val rowLabelHPx = rowLabelHDp.toPx()
            val rowGapPx = rowGapDp.toPx()
            val rowHeightPx = rowLabelHPx + cellSizePx + rowGapPx
            val cornerRadius = CornerRadius(4.dp.toPx())

            cells.forEachIndexed { index, cell ->
                val row = index / cols
                val col = index % cols
                val cellX = col * (cellSizePx + gapPx)
                val rowTop = row * rowHeightPx
                val cellY = rowTop + rowLabelHPx

                // Draw month label when month changes or at start of a row
                val prevCell = if (index > 0) cells[index - 1] else null
                val isNewMonth = prevCell == null || cell.date.month != prevCell.date.month
                if (isNewMonth) {
                    val monthName = MONTH_NAMES[cell.date.month.ordinal]
                    val textResult = textMeasurer.measure(text = monthName, style = labelStyle)
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset(cellX, rowTop),
                    )
                }

                val color = cell.dominantTeaTypeColorHex?.let { hex ->
                    val parsed = hex.hexToColor()
                    if (parsed == Color.Unspecified) activeDefaultColor else parsed
                } ?: emptyColor
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cellX, cellY),
                    size = Size(cellSizePx, cellSizePx),
                    cornerRadius = cornerRadius,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingActivityHeatmapWeekPreview() {
    LeafLogTheme {
        BrewingActivityHeatmap(
            cells = listOf(
                ActivityCell(LocalDate(2026, 2, 23), null),
                ActivityCell(LocalDate(2026, 2, 24), "#4CAF50"),
                ActivityCell(LocalDate(2026, 2, 25), "#4CAF50"),
                ActivityCell(LocalDate(2026, 2, 26), null),
                ActivityCell(LocalDate(2026, 2, 27), "#795548"),
                ActivityCell(LocalDate(2026, 2, 28), null),
                ActivityCell(LocalDate(2026, 3, 1), "#FF9800"),
            ),
            period = AnalyticsPeriod.THIS_WEEK,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingActivityHeatmapMonthPreview() {
    LeafLogTheme {
        val cells = (1..28).map { day ->
            ActivityCell(
                date = LocalDate(2026, 2, day),
                dominantTeaTypeColorHex = if (day % 3 == 0) "#4CAF50" else if (day % 5 == 0) "#795548" else null,
            )
        }
        BrewingActivityHeatmap(
            cells = cells,
            period = AnalyticsPeriod.THIS_MONTH,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingActivityHeatmapYearPreview() {
    LeafLogTheme {
        val startDate = LocalDate(2026, 1, 5)  // First Monday of Jan 2026
        val cells = (0 until 52).map { weekIndex ->
            val weekStart = startDate.plus(weekIndex * 7, DateTimeUnit.DAY)
            ActivityCell(
                date = weekStart,
                dominantTeaTypeColorHex = when {
                    weekIndex % 7 == 0 -> null
                    weekIndex % 3 == 0 -> "#4CAF50"
                    weekIndex % 5 == 0 -> "#795548"
                    else -> "#FF9800"
                },
            )
        }
        BrewingActivityHeatmap(
            cells = cells,
            period = AnalyticsPeriod.THIS_YEAR,
            modifier = Modifier.padding(16.dp),
        )
    }
}
