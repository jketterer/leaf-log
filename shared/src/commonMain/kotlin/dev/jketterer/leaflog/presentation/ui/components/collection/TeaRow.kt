package dev.jketterer.leaflog.presentation.ui.components.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.teaTypeColor

/**
 * A tea as one line of a list: its type's color down the side, its name, [supportingText] under
 * it and [trailingContent] at the end. Lighter than a [TeaCard], for a list that isn't the page's
 * subject.
 */
@Composable
fun TeaRow(
    tea: Tea,
    teaTypeColorHex: String?,
    supportingText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val accentColor = teaTypeColorHex?.let { teaTypeColor(it) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color.Transparent
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (accentColor != null) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = tea.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (tea.finishedAt != null) {
                    BrewingParamChip(icon = FeatherIcons.Check, text = "Finished")
                }
            }
            if (!supportingText.isNullOrEmpty()) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(modifier = Modifier.padding(end = 16.dp)) {
            trailingContent?.invoke()
        }

        if (isSelected) {
            Icon(
                imageVector = FeatherIcons.Check,
                contentDescription = "Selected",
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeaRowPreview() {
    LeafLogTheme {
        Column {
            TeaRow(
                tea = Tea(id = "1", name = "Dragon Well", teaTypeId = "green", createdAt = PreviewInstant, updatedAt = PreviewInstant),
                teaTypeColorHex = "#4CAF50",
                supportingText = "Green",
                onClick = {},
            )
            TeaRow(
                tea = Tea(id = "2", name = "Sencha", teaTypeId = "green", createdAt = PreviewInstant, updatedAt = PreviewInstant),
                teaTypeColorHex = "#4CAF50",
                supportingText = "Green",
                onClick = {},
                isSelected = true,
            )
            TeaRow(
                tea = Tea(
                    id = "3",
                    name = "Lapsang Souchong",
                    teaTypeId = "black",
                    finishedAt = PreviewInstant,
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                ),
                teaTypeColorHex = null,
                supportingText = null,
                onClick = {},
            )
        }
    }
}
