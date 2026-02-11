package dev.jketterer.leaflog.presentation.ui.components.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Edit
import compose.icons.feathericons.Plus
import compose.icons.feathericons.X
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/**
 * Expandable FAB with options for "Log Session" and "Quick Timer".
 */
@Composable
fun ExpandableFAB(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLogSessionClick: () -> Unit,
    onQuickTimerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        label = "fab_rotation",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Menu items (shown when expanded)
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Quick Timer option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Quick Timer",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                    )
                    SmallFloatingActionButton(
                        onClick = {
                            onExpandedChange(false)
                            onQuickTimerClick()
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Clock,
                            contentDescription = "Quick Timer",
                        )
                    }
                }

                // Log Session option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Log Session",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                    )
                    SmallFloatingActionButton(
                        onClick = {
                            onExpandedChange(false)
                            onLogSessionClick()
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Edit,
                            contentDescription = "Log Session",
                        )
                    }
                }
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
        ) {
            Icon(
                imageVector = if (expanded) FeatherIcons.X else FeatherIcons.Plus,
                contentDescription = if (expanded) "Close menu" else "Open menu",
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpandableFABCollapsedPreview() {
    LeafLogTheme {
        ExpandableFAB(
            expanded = false,
            onExpandedChange = {},
            onLogSessionClick = {},
            onQuickTimerClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpandableFABExpandedPreview() {
    LeafLogTheme {
        ExpandableFAB(
            expanded = true,
            onExpandedChange = {},
            onLogSessionClick = {},
            onQuickTimerClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
