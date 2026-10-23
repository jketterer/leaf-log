package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jketterer.leaflog.domain.models.ActivityBucket
import dev.jketterer.leaflog.domain.models.ActivityCell
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.DateSkeleton
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.activityBucket
import dev.jketterer.leaflog.presentation.ui.components.common.currentDate
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.contentColorOn
import dev.jketterer.leaflog.presentation.ui.theme.isDarkTheme
import dev.jketterer.leaflog.presentation.ui.theme.teaTypeColor
import kotlinx.datetime.DateTimeUnit
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.analytics_activity
import leaf_log.shared.generated.resources.analytics_activity_summary
import leaf_log.shared.generated.resources.analytics_week_of
import leaf_log.shared.generated.resources.session_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.plus
import kotlin.math.max
import kotlin.math.min

// Indexed by DayOfWeek.ordinal (MONDAY = 0)
private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

private val CELL_GAP = 3.dp
private val LABEL_GAP = 4.dp

/** Scale alpha based on session count: min 0.35 (1 session), max 1.0 (3+ sessions). */
private fun intensityAlpha(sessionCount: Int): Float =
    if (sessionCount <= 0) 1f else min(1f, 0.35f + sessionCount * 0.22f)

/**
 * The period's activity, one cell per [AnalyticsPeriod.activityBucket]: a week and a month as
 * calendars, a season as weekday rows by week, a year as weeks by month, All time as a row a year.
 * Tapping a cell with sessions passes its date to [onTapCell].
 */
@Composable
fun BrewingActivityHeatmap(
    cells: List<ActivityCell>,
    period: AnalyticsPeriod,
    onTapCell: (date: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (cells.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.analytics_activity),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            val first = cells.first().date
            val summary = activitySummary(cells, period.activityBucket)
            Column(modifier = Modifier.semantics { contentDescription = summary }) {
                when (period) {
                    AnalyticsPeriod.THIS_WEEK, AnalyticsPeriod.THIS_MONTH -> {
                        if (period == AnalyticsPeriod.THIS_MONTH) {
                            Text(
                                text = TimeFormatter.formatDate(first, DateSkeleton.MONTH_YEAR),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(LABEL_GAP))
                        }
                        HeatmapGrid(
                            cells = cells,
                            lineLength = 7,
                            offset = first.dayOfWeek.ordinal,
                            columnLabels = DAY_LABELS,
                            dayNumbers = true,
                            today = currentDate(),
                            onTapCell = onTapCell,
                        )
                    }

                    AnalyticsPeriod.THIS_SEASON -> {
                        val offset = first.dayOfWeek.ordinal
                        // A month is named over the week its first day falls in
                        val weeks = arrayOfNulls<String>((offset + cells.size + 6) / 7)
                        cells.forEachIndexed { index, cell ->
                            if (cell.date.day == 1) {
                                weeks[(offset + index) / 7] =
                                    TimeFormatter.formatDate(cell.date, DateSkeleton.MONTH)
                            }
                        }
                        HeatmapGrid(
                            cells = cells,
                            lineLength = 7,
                            offset = offset,
                            transposed = true,
                            columnLabels = weeks.toList(),
                            rowLabels = DAY_LABELS,
                            today = currentDate(),
                            onTapCell = onTapCell,
                        )
                    }

                    AnalyticsPeriod.THIS_YEAR -> YearlyHeatmapCanvas(cells = cells, onTapCell = onTapCell)

                    AnalyticsPeriod.ALL_TIME -> {
                        val years = cells.last().date.year - first.year + 1
                        HeatmapGrid(
                            cells = cells,
                            lineLength = 12,
                            offset = first.month.ordinal,
                            columnLabels = Month.entries.map {
                                TimeFormatter.formatDate(LocalDate(first.year, it, 1), DateSkeleton.NARROW_MONTH)
                            },
                            rowLabels = List(years) { (first.year + it).toString() },
                            onTapCell = onTapCell,
                        )
                    }
                }
            }
        }
    }
}

/** The period's sessions and its busiest cell, for a screen reader. */
@Composable
private fun activitySummary(cells: List<ActivityCell>, bucket: ActivityBucket): String {
    val busiest = cells.maxBy { it.sessionCount }
    val busiestLabel = when (bucket) {
        ActivityBucket.DAY -> TimeFormatter.formatDate(busiest.date, DateSkeleton.MONTH_DAY)
        ActivityBucket.WEEK -> stringResource(
            Res.string.analytics_week_of,
            TimeFormatter.formatDate(busiest.date, DateSkeleton.MONTH_DAY),
        )
        ActivityBucket.MONTH -> TimeFormatter.formatDate(busiest.date, DateSkeleton.MONTH_YEAR)
    }
    val total = cells.sumOf { it.sessionCount }
    return stringResource(
        Res.string.analytics_activity_summary,
        pluralStringResource(Res.plurals.session_count, total, total),
        busiestLabel,
    )
}

/**
 * [cells] in lines of [lineLength], the first [offset] positions in. Lines are rows, or columns
 * when [transposed]. [columnLabels] head the columns and [rowLabels] lead the rows; a null leaves
 * that one blank. [today]'s cell is ringed.
 */
@Composable
private fun HeatmapGrid(
    cells: List<ActivityCell>,
    lineLength: Int,
    offset: Int,
    columnLabels: List<String?>,
    onTapCell: (date: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    transposed: Boolean = false,
    rowLabels: List<String?> = emptyList(),
    dayNumbers: Boolean = false,
    today: LocalDate? = null,
) {
    val activeDefaultColor = MaterialTheme.colorScheme.primary
    val dark = isDarkTheme()
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val todayRingColor = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    val lines = (offset + cells.size + lineLength - 1) / lineLength
    val rows = if (transposed) lineLength else lines
    val columns = if (transposed) lines else lineLength

    val columnLabelLayouts = columnLabels.map { it?.let { textMeasurer.measure(it, labelStyle) } }
    val rowLabelLayouts = rowLabels.map { it?.let { textMeasurer.measure(it, labelStyle) } }
    val density = LocalDensity.current
    val columnLabelHeight: Dp = columnLabelLayouts.maxOfOrNull { it?.size?.height ?: 0 }
        ?.let { with(density) { it.toDp() } + LABEL_GAP } ?: 0.dp
    val rowLabelWidth: Dp = rowLabelLayouts.maxOfOrNull { it?.size?.width ?: 0 }
        ?.let { with(density) { it.toDp() } + LABEL_GAP } ?: 0.dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cellSize: Dp =
            minOf(28.dp, (maxWidth - rowLabelWidth - CELL_GAP * (columns - 1)) / columns)
        val gridWidth: Dp = rowLabelWidth + cellSize * columns + CELL_GAP * (columns - 1)
        val startXDp: Dp = (maxWidth - gridWidth) / 2
        val canvasHeight: Dp = columnLabelHeight + cellSize * rows + CELL_GAP * (rows - 1)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
                .pointerInput(cells, cellSize, startXDp) {
                    detectTapGestures { tap ->
                        val step = (cellSize + CELL_GAP).toPx()
                        val x = tap.x - (startXDp + rowLabelWidth).toPx()
                        val y = tap.y - columnLabelHeight.toPx()
                        if (x < 0 || y < 0) return@detectTapGestures
                        val column = (x / step).toInt()
                        val row = (y / step).toInt()
                        if (column >= columns || row >= rows) return@detectTapGestures
                        val along = if (transposed) row else column
                        val line = if (transposed) column else row
                        cells.getOrNull(line * lineLength + along - offset)
                            ?.takeIf { it.sessionCount > 0 }
                            ?.let { onTapCell(it.date) }
                    }
                },
        ) {
            val cellSizePx = cellSize.toPx()
            val step = cellSizePx + CELL_GAP.toPx()
            val gridX = startXDp.toPx() + rowLabelWidth.toPx()
            val gridY = columnLabelHeight.toPx()
            val todayRingWidth = 2.dp.toPx()
            // Follows the font scale until it would outgrow the cell
            val dayNumberStyle = TextStyle(fontSize = minOf(8.sp.toPx(), cellSizePx * 0.4f).toSp())

            columnLabelLayouts.forEachIndexed { col, layout ->
                if (layout == null) return@forEachIndexed
                // Centered over its column, or from its left edge when wider, as a month name is
                val x = gridX + col * step + max(0f, (cellSizePx - layout.size.width) / 2f)
                drawText(layout, topLeft = Offset(x, 0f))
            }
            rowLabelLayouts.forEachIndexed { row, layout ->
                if (layout == null) return@forEachIndexed
                val y = gridY + row * step + (cellSizePx - layout.size.height) / 2f
                drawText(layout, topLeft = Offset(startXDp.toPx(), y))
            }

            cells.forEachIndexed { index, cell ->
                val position = offset + index
                val along = position % lineLength
                val line = position / lineLength
                val topLeft = Offset(
                    gridX + (if (transposed) line else along) * step,
                    gridY + (if (transposed) along else line) * step,
                )
                val color = cell.color(dark, activeDefaultColor)
                drawCell(cell, color, topLeft, cellSizePx, outlineColor)

                if (dayNumbers) {
                    val dayResult = textMeasurer.measure(
                        text = cell.date.day.toString(),
                        style = dayNumberStyle.copy(
                            color = if (color != null) contentColorOn(color) else labelColor,
                        ),
                    )
                    drawText(
                        textLayoutResult = dayResult,
                        topLeft = topLeft + Offset(
                            (cellSizePx - dayResult.size.width) / 2f,
                            (cellSizePx - dayResult.size.height) / 2f,
                        ),
                    )
                }

                if (cell.date == today) {
                    val inset = todayRingWidth / 2f
                    drawRoundRect(
                        color = todayRingColor,
                        topLeft = topLeft - Offset(inset, inset),
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
private fun YearlyHeatmapCanvas(cells: List<ActivityCell>, onTapCell: (date: LocalDate) -> Unit) {
    val activeDefaultColor = MaterialTheme.colorScheme.primary
    val dark = isDarkTheme()
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = labelColor,
    )
    // Measured, so a large font scale pushes the cells down rather than under the label
    val monthLabelH = with(LocalDensity.current) {
        textMeasurer.measure("M", labelStyle).size.height.toDp()
    }

    // Cells arrive in date order, so grouping keeps months in order across a year boundary
    val monthGroups = cells.groupBy { it.date.year to it.date.month }.values.toList()

    val columnGap = 8.dp
    val maxCellsPerMonth = 5
    val rowGap = 10.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val monthsPerRow = if (maxWidth >= 300.dp) 3 else 2
        val columnWidth: Dp = (maxWidth - columnGap * (monthsPerRow - 1)) / monthsPerRow
        val cellSize: Dp = (columnWidth - CELL_GAP * (maxCellsPerMonth - 1)) / maxCellsPerMonth
        val blockHeight: Dp = monthLabelH + CELL_GAP + cellSize

        val numMonthRows = (monthGroups.size + monthsPerRow - 1) / monthsPerRow
        val canvasHeight: Dp = blockHeight * numMonthRows + rowGap * (numMonthRows - 1)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
                .pointerInput(monthGroups, cellSize, monthsPerRow) {
                    detectTapGestures { tap ->
                        val blockWidth = (columnWidth + columnGap).toPx()
                        val col = (tap.x / blockWidth).toInt()
                        val row = (tap.y / (blockHeight + rowGap).toPx()).toInt()
                        val cellsTop = row * (blockHeight + rowGap).toPx() + (monthLabelH + CELL_GAP).toPx()
                        if (col >= monthsPerRow || tap.y < cellsTop) return@detectTapGestures
                        val cellIndex = ((tap.x - col * blockWidth) / (cellSize + CELL_GAP).toPx()).toInt()
                        monthGroups.getOrNull(row * monthsPerRow + col)?.getOrNull(cellIndex)
                            ?.takeIf { it.sessionCount > 0 }
                            ?.let { onTapCell(it.date) }
                    }
                },
        ) {
            val cellSizePx = cellSize.toPx()
            val cellGapPx = CELL_GAP.toPx()
            val columnGapPx = columnGap.toPx()
            val columnWidthPx = columnWidth.toPx()
            val monthLabelHPx = monthLabelH.toPx()
            val blockHeightPx = blockHeight.toPx()
            val rowGapPx = rowGap.toPx()

            monthGroups.forEachIndexed { monthIndex, monthCells ->
                val row = monthIndex / monthsPerRow
                val col = monthIndex % monthsPerRow
                val blockX = col * (columnWidthPx + columnGapPx)
                val blockY = row * (blockHeightPx + rowGapPx)

                val monthName =
                    TimeFormatter.formatDate(monthCells.first().date, DateSkeleton.MONTH)
                val textResult = textMeasurer.measure(text = monthName, style = labelStyle)
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(blockX, blockY + (monthLabelHPx - textResult.size.height) / 2f),
                )

                val cellsY = blockY + monthLabelHPx + cellGapPx
                monthCells.forEachIndexed { cellIndex, cell ->
                    val cellX = blockX + cellIndex * (cellSizePx + cellGapPx)
                    drawCell(
                        cell = cell,
                        color = cell.color(dark, activeDefaultColor),
                        topLeft = Offset(cellX, cellsY),
                        size = cellSizePx,
                        outlineColor = outlineColor,
                    )
                }
            }
        }
    }
}

/** The cell's tea type color, [fallback] for a type without one, null when nothing was brewed. */
private fun ActivityCell.color(dark: Boolean, fallback: Color): Color? =
    dominantTeaTypeColorHex?.let { hex ->
        teaTypeColor(hex, dark).takeIf { it != Color.Unspecified } ?: fallback
    }

/**
 * Filled with [color] by how much was brewed, or empty when [color] is null, and outlined either
 * way so a pale tea type color reads on any surface.
 */
private fun DrawScope.drawCell(
    cell: ActivityCell,
    color: Color?,
    topLeft: Offset,
    size: Float,
    outlineColor: Color,
) {
    val cornerRadius = CornerRadius(4.dp.toPx())
    if (color != null) {
        drawRoundRect(
            color = color.copy(alpha = intensityAlpha(cell.sessionCount)),
            topLeft = topLeft,
            size = Size(size, size),
            cornerRadius = cornerRadius,
        )
    }
    val outlineWidth = 1.dp.toPx()
    drawRoundRect(
        color = outlineColor,
        topLeft = topLeft + Offset(outlineWidth / 2, outlineWidth / 2),
        size = Size(size - outlineWidth, size - outlineWidth),
        cornerRadius = cornerRadius,
        style = Stroke(width = outlineWidth),
    )
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
            onTapCell = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun previewCells(first: LocalDate, count: Int, unit: DateTimeUnit.DateBased) =
    (0 until count).map { index ->
        ActivityCell(
            date = first.plus(index, unit),
            dominantTeaTypeColorHex = when {
                index % 7 == 0 -> null
                index % 3 == 0 -> "#4CAF50"
                index % 5 == 0 -> "#795548"
                else -> "#FF9800"
            },
            sessionCount = if (index % 7 == 0) 0 else index % 4 + 1,
        )
    }

@Preview(showBackground = true)
@Composable
private fun BrewingActivityHeatmapThisMonthPreview() {
    LeafLogTheme {
        BrewingActivityHeatmap(
            cells = previewCells(LocalDate(2026, 2, 1), 28, DateTimeUnit.DAY),
            period = AnalyticsPeriod.THIS_MONTH,
            onTapCell = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingActivityHeatmapWinterPreview() {
    LeafLogTheme {
        BrewingActivityHeatmap(
            cells = previewCells(LocalDate(2025, 12, 1), 90, DateTimeUnit.DAY),
            period = AnalyticsPeriod.THIS_SEASON,
            onTapCell = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingActivityHeatmapYearPreview() {
    LeafLogTheme {
        BrewingActivityHeatmap(
            cells = listOf(ActivityCell(LocalDate(2026, 1, 1), "#4CAF50", sessionCount = 2)) +
                previewCells(LocalDate(2026, 1, 5), 52, DateTimeUnit.WEEK),
            period = AnalyticsPeriod.THIS_YEAR,
            onTapCell = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingActivityHeatmapAllTimePreview() {
    LeafLogTheme {
        BrewingActivityHeatmap(
            cells = previewCells(LocalDate(2024, 5, 1), 30, DateTimeUnit.MONTH),
            period = AnalyticsPeriod.ALL_TIME,
            onTapCell = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
