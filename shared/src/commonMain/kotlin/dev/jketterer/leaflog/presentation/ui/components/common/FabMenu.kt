package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import leaf_log.shared.generated.resources.close_menu
import leaf_log.shared.generated.resources.log_session
import leaf_log.shared.generated.resources.open_menu
import leaf_log.shared.generated.resources.quick_timer
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** One choice in a [FabMenu]. */
class FabMenuItem(val label: StringResource, val icon: ImageVector, val onClick: () -> Unit)

/**
 * A + button in the corner of a screen filling this box, opening [items] above it over a scrim that
 * closes them. Items go top to bottom, so the most used belongs last, nearest the button. The
 * screen's snackbars show from [snackbarHostState] above the button.
 */
@Composable
fun BoxScope.FabMenu(
    items: List<FabMenuItem>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    FabMenuLayer(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        items = items,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
private fun BoxScope.FabMenuLayer(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<FabMenuItem>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    onExpandedChange(false)
                },
        )
    }

    val toggleLabel = stringResource(if (expanded) Res.string.close_menu else Res.string.open_menu)
    FloatingActionButtonMenu(
        expanded = expanded,
        modifier = modifier.align(Alignment.BottomEnd),
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
                // Some vectors declare a size of their own, the leaf's 800dp
                icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
            )
        }
    }

    // Out of the menu's way while it's open. Leaving composition stops a snackbar's timeout, so
    // closing the menu brings it back rather than losing it
    AnimatedVisibility(
        visible = !expanded,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 80.dp),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        SnackbarHost(hostState = snackbarHostState)
    }
}

private val previewItems = listOf(
    FabMenuItem(Res.string.quick_timer, FeatherIcons.Clock) {},
    FabMenuItem(Res.string.log_session, FeatherIcons.Edit) {},
)

@Preview(showBackground = true)
@Composable
private fun FabMenuCollapsedPreview() {
    LeafLogTheme {
        Box(Modifier.size(320.dp, 360.dp)) {
            FabMenuLayer(
                expanded = false,
                onExpandedChange = {},
                items = previewItems,
                snackbarHostState = remember { SnackbarHostState() },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FabMenuExpandedPreview() {
    LeafLogTheme {
        Box(Modifier.size(320.dp, 360.dp)) {
            FabMenuLayer(
                expanded = true,
                onExpandedChange = {},
                items = previewItems,
                snackbarHostState = remember { SnackbarHostState() },
            )
        }
    }
}
