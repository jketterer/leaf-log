package dev.jketterer.leaflog.presentation.ui.components.home

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Edit
import compose.icons.feathericons.Plus
import compose.icons.feathericons.X
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.add_tea
import leaf_log.shared.generated.resources.close_menu
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.log_session
import leaf_log.shared.generated.resources.open_menu
import leaf_log.shared.generated.resources.quick_timer
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/** Home's + menu: start a session, run a bare timer, or add a tea to the collection. */
@Composable
fun ExpandableFAB(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLogSessionClick: () -> Unit,
    onQuickTimerClick: () -> Unit,
    onAddTeaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggleLabel = stringResource(if (expanded) Res.string.close_menu else Res.string.open_menu)
    // Top to bottom, so the most used sits nearest the button
    val items = listOf(
        MenuItem(Res.string.add_tea, vectorResource(Res.drawable.ic_tea_leaf), onAddTeaClick),
        MenuItem(Res.string.quick_timer, FeatherIcons.Clock, onQuickTimerClick),
        MenuItem(Res.string.log_session, FeatherIcons.Edit, onLogSessionClick),
    )

    FloatingActionButtonMenu(
        expanded = expanded,
        modifier = modifier,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = onExpandedChange,
                modifier = Modifier.semantics {
                    // Read before the items, which sit above it
                    traversalIndex = -1f
                    contentDescription = toggleLabel
                },
            ) {
                val icon by remember { derivedStateOf { if (checkedProgress > 0.5f) FeatherIcons.X else FeatherIcons.Plus } }
                Icon(icon, contentDescription = null, modifier = Modifier.animateIcon({ checkedProgress }))
            }
        },
    ) {
        items.forEach { item ->
            FloatingActionButtonMenuItem(
                onClick = {
                    onExpandedChange(false)
                    item.onClick()
                },
                text = { Text(stringResource(item.label)) },
                // The leaf's vector declares 800dp
                icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
            )
        }
    }
}

private class MenuItem(val label: StringResource, val icon: ImageVector, val onClick: () -> Unit)

@Preview(showBackground = true)
@Composable
private fun ExpandableFABCollapsedPreview() {
    LeafLogTheme {
        ExpandableFAB(
            expanded = false,
            onExpandedChange = {},
            onLogSessionClick = {},
            onQuickTimerClick = {},
            onAddTeaClick = {},
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
            onAddTeaClick = {},
        )
    }
}
