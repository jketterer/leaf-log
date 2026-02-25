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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.math.min

private val DAY_LABELS = listOf("S", "M", "T", "W", "T", "F", "S")
private val MONTH_NAMES = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** Scale alpha based on session count: min 0.35 (1 session), max 1.0 (3+ sessions). */
private fun intensityAlpha(sessionCount: Int): Float =
    if (sessionCount <= 0) 1f else min(1f, 0.35f + sessionCount * 0.22f)

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
                isWeekView -> WeekHeatmapCanvas(cells = cells, period = period)
                isMonthView -> MonthCalendarHeatmapCanvas(cells = cells, period = period)
                else -> YearlyHeatmapCanvas(cells = cells)
            }
        }
    }
}

@Composable
private fun WeekHeatmapCanvas(
    cells: List<ActivityCell>,
    period: AnalyticsPeriod,
) {
    val activeDefaultColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val todayRingColor = MaterialTheme.colorScheme.primary
    val dayNumberFilledColor = MaterialTheme.colorScheme.onPrimary
    val dayNumberEmptyColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = labelColor,
        textAlign = TextAlign.Center,
    )
    val dayNumberStyle = TextStyle(
        fontSize = 8.sp,
        textAlign = TextAlign.Center,
    )
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    // For THIS_WEEK, use static day-of-week labels; for LAST_7_DAYS, use actual dates
    val headerLabels = if (period == AnalyticsPeriod.THIS_WEEK) {
        DAY_LABELS
    } else {
        cells.take(7).map { DAY_LABELS[(it.date.dayOfWeek.ordinal + 1) % 7] }
    }

    val gap = 4.dp
    val labelHeightDp = 14.dp
    val labelGapDp = 6.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize: Dp = minOf(28.dp, (maxWidth - gap * 6) / 7)
        val totalGridWidth: Dp = cellSize * 7 + gap * 6
        val startXDp: Dp = (maxWidth - totalGridWidth) / 2
        val canvasHeight: Dp = labelHeightDp + labelGapDp + cellSize

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight),
        ) {
            val cellSizePx = cellSize.toPx()
            val gapPx = gap.toPx()
            val labelHeightPx = labelHeightDp.toPx()
            val labelGapPx = labelGapDp.toPx()
            val startX = startXDp.toPx()
            val cornerRadius = CornerRadius(4.dp.toPx())
            val strokeWidth = 1.5.dp.toPx()
            val todayRingWidth = 2.dp.toPx()

            headerLabels.forEachIndexed { index, label ->
                val cx = startX + index * (cellSizePx + gapPx) + cellSizePx / 2f
                val textResult = textMeasurer.measure(text = label, style = labelStyle)
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(cx - textResult.size.width / 2f, 0f),
                )
            }

            val cellTop = labelHeightPx + labelGapPx
            cells.take(7).forEachIndexed { index, cell ->
                val cellX = startX + index * (cellSizePx + gapPx)
                val color = cell.dominantTeaTypeColorHex?.let { hex ->
                    val parsed = hex.hexToColor()
                    if (parsed == Color.Unspecified) activeDefaultColor else parsed
                }
                if (color != null) {
                    drawRoundRect(
                        color = color.copy(alpha = intensityAlpha(cell.sessionCount)),
                        topLeft = Offset(cellX, cellTop),
                        size = Size(cellSizePx, cellSizePx),
                        cornerRadius = cornerRadius,
                    )
                } else {
                    drawRoundRect(
                        color = outlineColor,
                        topLeft = Offset(cellX, cellTop),
                        size = Size(cellSizePx, cellSizePx),
                        cornerRadius = cornerRadius,
                        style = Stroke(width = strokeWidth),
                    )
                }

                // Day number
                val dayText = cell.date.day.toString()
                val dayTextColor = if (color != null) dayNumberFilledColor else dayNumberEmptyColor
                val dayResult = textMeasurer.measure(
                    text = dayText,
                    style = dayNumberStyle.copy(color = dayTextColor),
                )
                drawText(
                    textLayoutResult = dayResult,
                    topLeft = Offset(
                        cellX + (cellSizePx - dayResult.size.width) / 2f,
                        cellTop + (cellSizePx - dayResult.size.height) / 2f,
                    ),
                )

                // Today indicator ring
                if (cell.date == today) {
                    val inset = todayRingWidth / 2f
                    drawRoundRect(
                        color = todayRingColor,
                        topLeft = Offset(cellX - inset, cellTop - inset),
                        size = Size(cellSizePx + todayRingWidth, cellSizePx + todayRingWidth),
                        cornerRadius = CornerRadius(4.dp.toPx() + inset),
                        style = Stroke(width = todayRingWidth),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthCalendarHeatmapCanvas(
    cells: List<ActivityCell>,
    period: AnalyticsPeriod,
) {
    val activeDefaultColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val todayRingColor = MaterialTheme.colorScheme.primary
    val dayNumberFilledColor = MaterialTheme.colorScheme.onPrimary
    val dayNumberEmptyColor = MaterialTheme.colorScheme.onSurfaceVariant
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
    val dayNumberStyle = TextStyle(
        fontSize = 8.sp,
        textAlign = TextAlign.Center,
    )
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    val firstDate = cells.first().date
    val lastDate = cells.last().date
    val emptyStart = (firstDate.dayOfWeek.ordinal + 1) % 7  // 0=Sun, 6=Sat
    val totalPositions = emptyStart + cells.size
    val numRows = (totalPositions + 6) / 7
    val emptyEnd = numRows * 7 - totalPositions

    val gap = 4.dp
    val monthLabelH = 20.dp
    val headerH = 18.dp
    val headerGap = 4.dp
    val monthLabel = when (period) {
        AnalyticsPeriod.LAST_30_DAYS -> {
            val startLabel = "${MONTH_NAMES[firstDate.month.ordinal]} ${firstDate.day}"
            val endLabel = "${MONTH_NAMES[lastDate.month.ordinal]} ${lastDate.day}"
            "$startLabel – $endLabel"
        }
        else -> "${MONTH_NAMES[firstDate.month.ordinal]} ${firstDate.year}"
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize: Dp = minOf(28.dp, (maxWidth - gap * 6) / 7)
        val totalGridWidth: Dp = cellSize * 7 + gap * 6
        val startXDp: Dp = (maxWidth - totalGridWidth) / 2
        val canvasHeight: Dp = monthLabelH + headerH + headerGap +
            (cellSize + gap) * numRows - gap

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
            val startX = startXDp.toPx()
            val cornerRadius = CornerRadius(4.dp.toPx())
            val strokeWidth = 1.5.dp.toPx()
            val todayRingWidth = 2.dp.toPx()

            val monthTextResult = textMeasurer.measure(text = monthLabel, style = monthStyle)
            drawText(
                textLayoutResult = monthTextResult,
                topLeft = Offset(startX, (monthLabelHPx - monthTextResult.size.height) / 2f),
            )

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
                }
                if (color != null) {
                    drawRoundRect(
                        color = color.copy(alpha = intensityAlpha(cell.sessionCount)),
                        topLeft = Offset(cellX, cellY),
                        size = Size(cellSizePx, cellSizePx),
                        cornerRadius = cornerRadius,
                    )
                } else {
                    drawRoundRect(
                        color = outlineColor,
                        topLeft = Offset(cellX, cellY),
                        size = Size(cellSizePx, cellSizePx),
                        cornerRadius = cornerRadius,
                        style = Stroke(width = strokeWidth),
                    )
                }

                // Day number
                val dayText = cell.date.day.toString()
                val dayTextColor = if (color != null) dayNumberFilledColor else dayNumberEmptyColor
                val dayResult = textMeasurer.measure(
                    text = dayText,
                    style = dayNumberStyle.copy(color = dayTextColor),
                )
                drawText(
                    textLayoutResult = dayResult,
                    topLeft = Offset(
                        cellX + (cellSizePx - dayResult.size.width) / 2f,
                        cellY + (cellSizePx - dayResult.size.height) / 2f,
                    ),
                )

                // Today indicator ring
                if (cell.date == today) {
                    val inset = todayRingWidth / 2f
                    drawRoundRect(
                        color = todayRingColor,
                        topLeft = Offset(cellX - inset, cellY - inset),
                        size = Size(cellSizePx + todayRingWidth, cellSizePx + todayRingWidth),
                        cornerRadius = CornerRadius(4.dp.toPx() + inset),
                        style = Stroke(width = todayRingWidth),
                    )
                }
            }
        }
    }
}

@Composable
private fun YearlyHeatmapCanvas(cells: List<ActivityCell>) {
    val activeDefaultColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = labelColor,
    )

    val monthGroups = cells
        .groupBy { it.date.month }
        .entries
        .sortedBy { it.key.ordinal }
        .map { it.key to it.value }

    val columnGap = 8.dp
    val cellGap = 3.dp
    val maxCellsPerMonth = 5
    val monthLabelH = 14.dp
    val labelGap = 3.dp
    val rowGap = 10.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val monthsPerRow = if (maxWidth >= 300.dp) 3 else 2
        val columnWidth: Dp = (maxWidth - columnGap * (monthsPerRow - 1)) / monthsPerRow
        val cellSize: Dp = (columnWidth - cellGap * (maxCellsPerMonth - 1)) / maxCellsPerMonth
        val blockHeight: Dp = monthLabelH + labelGap + cellSize

        val numMonthRows = (monthGroups.size + monthsPerRow - 1) / monthsPerRow
        val canvasHeight: Dp = blockHeight * numMonthRows + rowGap * (numMonthRows - 1)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight),
        ) {
            val cellSizePx = cellSize.toPx()
            val cellGapPx = cellGap.toPx()
            val columnGapPx = columnGap.toPx()
            val columnWidthPx = columnWidth.toPx()
            val monthLabelHPx = monthLabelH.toPx()
            val labelGapPx = labelGap.toPx()
            val blockHeightPx = blockHeight.toPx()
            val rowGapPx = rowGap.toPx()
            val cornerRadius = CornerRadius(4.dp.toPx())
            val strokeWidth = 1.5.dp.toPx()

            monthGroups.forEachIndexed { monthIndex, (month, monthCells) ->
                val row = monthIndex / monthsPerRow
                val col = monthIndex % monthsPerRow
                val blockX = col * (columnWidthPx + columnGapPx)
                val blockY = row * (blockHeightPx + rowGapPx)

                val monthName = MONTH_NAMES[month.ordinal]
                val textResult = textMeasurer.measure(text = monthName, style = labelStyle)
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(blockX, blockY + (monthLabelHPx - textResult.size.height) / 2f),
                )

                val cellsY = blockY + monthLabelHPx + labelGapPx
                monthCells.forEachIndexed { cellIndex, cell ->
                    val cellX = blockX + cellIndex * (cellSizePx + cellGapPx)
                    val color = cell.dominantTeaTypeColorHex?.let { hex ->
                        val parsed = hex.hexToColor()
                        if (parsed == Color.Unspecified) activeDefaultColor else parsed
                    }
                    if (color != null) {
                        drawRoundRect(
                            color = color.copy(alpha = intensityAlpha(cell.sessionCount)),
                            topLeft = Offset(cellX, cellsY),
                            size = Size(cellSizePx, cellSizePx),
                            cornerRadius = cornerRadius,
                        )
                    } else {
                        drawRoundRect(
                            color = outlineColor,
                            topLeft = Offset(cellX, cellsY),
                            size = Size(cellSizePx, cellSizePx),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = strokeWidth),
                        )
                    }
                }
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
                ActivityCell(LocalDate(2026, 2, 24), "#4CAF50", sessionCount = 1),
                ActivityCell(LocalDate(2026, 2, 25), "#4CAF50", sessionCount = 3),
                ActivityCell(LocalDate(2026, 2, 26), null),
                ActivityCell(LocalDate(2026, 2, 27), "#795548", sessionCount = 2),
                ActivityCell(LocalDate(2026, 2, 28), null),
                ActivityCell(LocalDate(2026, 3, 1), "#FF9800", sessionCount = 1),
            ),
            period = AnalyticsPeriod.THIS_WEEK,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingActivityHeatmapThisMonthPreview() {
    LeafLogTheme {
        val cells = (1..28).map { day ->
            ActivityCell(
                date = LocalDate(2026, 2, day),
                dominantTeaTypeColorHex = if (day % 3 == 0) "#4CAF50" else if (day % 5 == 0) "#795548" else null,
                sessionCount = if (day % 3 == 0) (day % 4) + 1 else if (day % 5 == 0) 1 else 0,
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
private fun BrewingActivityHeatmapLastMonthPreview() {
    LeafLogTheme {
        // Jan 2026: starts on Thursday, 31 days
        val cells = (1..31).map { day ->
            ActivityCell(
                date = LocalDate(2026, 1, day),
                dominantTeaTypeColorHex = if (day % 3 == 0) "#4CAF50" else if (day % 5 == 0) "#795548" else null,
                sessionCount = if (day % 3 == 0) (day % 4) + 1 else if (day % 5 == 0) 2 else 0,
            )
        }
        BrewingActivityHeatmap(
            cells = cells,
            period = AnalyticsPeriod.LAST_MONTH,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingActivityHeatmapLast30DaysPreview() {
    LeafLogTheme {
        // Jan 26 (Mon) through Feb 25 (Wed) = 31 days spanning two months
        val startDate = LocalDate(2026, 1, 26)
        val cells = (0 until 31).map { offset ->
            val date = startDate.plus(offset, DateTimeUnit.DAY)
            ActivityCell(
                date = date,
                dominantTeaTypeColorHex = if (offset % 3 == 0) "#4CAF50" else if (offset % 5 == 0) "#795548" else null,
                sessionCount = if (offset % 3 == 0) (offset % 5) + 1 else if (offset % 5 == 0) 1 else 0,
            )
        }
        BrewingActivityHeatmap(
            cells = cells,
            period = AnalyticsPeriod.LAST_30_DAYS,
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
            val count = when {
                weekIndex % 7 == 0 -> 0
                weekIndex % 3 == 0 -> 3
                weekIndex % 5 == 0 -> 1
                else -> 2
            }
            ActivityCell(
                date = weekStart,
                dominantTeaTypeColorHex = when {
                    weekIndex % 7 == 0 -> null
                    weekIndex % 3 == 0 -> "#4CAF50"
                    weekIndex % 5 == 0 -> "#795548"
                    else -> "#FF9800"
                },
                sessionCount = count,
            )
        }
        BrewingActivityHeatmap(
            cells = cells,
            period = AnalyticsPeriod.THIS_YEAR,
            modifier = Modifier.padding(16.dp),
        )
    }
}
