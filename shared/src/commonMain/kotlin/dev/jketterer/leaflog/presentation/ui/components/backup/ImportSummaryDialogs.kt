package dev.jketterer.leaflog.presentation.ui.components.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.ImportSummary
import dev.jketterer.leaflog.domain.models.RelativeDay
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.relativeDayOf
import dev.jketterer.leaflog.presentation.ui.components.common.DialogPreviewContainer
import dev.jketterer.leaflog.presentation.ui.components.common.currentInstant
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** What importing a backup would change, confirmed before anything does. */
@Composable
fun ImportPreviewDialog(
    summary: ImportSummary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text("Import this backup?") },
        text = { ImportSummaryContent(summary, isPreview = true) },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !summary.changesNothing) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/** What an import changed. */
@Composable
fun ImportResultDialog(
    result: ImportSummary,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text("Import Complete") },
        text = { ImportSummaryContent(result, isPreview = false) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}

@Composable
private fun ImportSummaryContent(summary: ImportSummary, isPreview: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isPreview) {
            val now = currentInstant()
            val made = TimeFormatter.formatRelativeTimestamp(summary.exportedAt, now)
            val day = relativeDayOf(
                summary.exportedAt,
                now,
                TimeZone.currentSystemDefault(),
            )
            val date = when (day) {
                RelativeDay.TODAY -> "today at $made"
                RelativeDay.YESTERDAY -> "yesterday"
                else -> made
            }
            val emphasis = SpanStyle(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = buildAnnotatedString {
                    append("Backed up ")
                    withStyle(emphasis) { append(date) }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (summary.changesNothing) {
            Text(
                text = "This device already has everything in the backup.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            // The counts share the widest one's width, so their labels line up
            val counts = listOf(summary.added, summary.updated, summary.removed)
            val countStyle = MaterialTheme.typography.titleLarge
                .copy(fontWeight = FontWeight.SemiBold)
            val measurer = rememberTextMeasurer()
            val countWidth = with(LocalDensity.current) {
                counts.maxOf { measurer.measure(it.toString(), countStyle).size.width }.toDp()
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ImportStat(summary.added, "new", countWidth, countStyle)
                ImportStat(summary.updated, "updated", countWidth, countStyle)
                ImportStat(
                    summary.removed,
                    "removed",
                    countWidth,
                    countStyle,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (summary.restoresSettings) {
                Text(
                    text = if (isPreview) {
                        "Your settings come with it."
                    } else {
                        "Your settings were restored."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = "Everything else stays as it is.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (summary.skipped > 0) {
            Text(
                text = "${summary.skipped} records are left out: what they belonged to was " +
                    "deleted before this backup was made.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A count beside its label, both dimmed when there's nothing to count. */
@Composable
private fun ImportStat(
    count: Int,
    label: String,
    countWidth: Dp,
    countStyle: TextStyle,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val dimmed = MaterialTheme.colorScheme.onSurfaceVariant
    Row {
        Text(
            text = count.toString(),
            style = countStyle,
            color = if (count == 0) dimmed else color,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(countWidth)
                .alignByBaseline(),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (count == 0) dimmed else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

// Previews
private val previewSummary = ImportSummary(
    exportedAt = Instant.parse("2026-09-02T09:00:00Z"),
    added = 12,
    updated = 3,
    removed = 1,
    skipped = 3,
    restoresSettings = true,
)

@Preview(showBackground = true)
@Composable
private fun ImportPreviewDialogPreview() {
    LeafLogTheme {
        DialogPreviewContainer(
            title = "Import this backup?",
            buttons = {
                TextButton(onClick = {}) { Text("Cancel") }
                Button(onClick = {}) { Text("Import") }
            },
        ) {
            ImportSummaryContent(previewSummary, isPreview = true)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ImportPreviewDialogNothingNewPreview() {
    LeafLogTheme {
        DialogPreviewContainer(
            title = "Import this backup?",
            buttons = {
                TextButton(onClick = {}) { Text("Cancel") }
                Button(onClick = {}, enabled = false) { Text("Import") }
            },
        ) {
            ImportSummaryContent(
                previewSummary.copy(
                    added = 0,
                    updated = 0,
                    removed = 0,
                    skipped = 0,
                    restoresSettings = false,
                ),
                isPreview = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ImportResultDialogPreview() {
    LeafLogTheme {
        DialogPreviewContainer(
            title = "Import Complete",
            buttons = { Button(onClick = {}) { Text("OK") } },
        ) {
            ImportSummaryContent(previewSummary, isPreview = false)
        }
    }
}
