package dev.jketterer.leaflog.presentation.ui.components.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Edit
import dev.jketterer.leaflog.presentation.ui.components.common.FabMenu
import dev.jketterer.leaflog.presentation.ui.components.common.FabMenuItem
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.add_tea
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.log_session
import leaf_log.shared.generated.resources.quick_timer
import org.jetbrains.compose.resources.vectorResource

/** Home's + menu: start a session, run a bare timer, or add a tea to the collection. */
@Composable
fun BoxScope.ExpandableFAB(
    onLogSessionClick: () -> Unit,
    onQuickTimerClick: () -> Unit,
    onAddTeaClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    FabMenu(
        items = listOf(
            FabMenuItem(Res.string.add_tea, vectorResource(Res.drawable.ic_tea_leaf), onAddTeaClick),
            FabMenuItem(Res.string.quick_timer, FeatherIcons.Clock, onQuickTimerClick),
            FabMenuItem(Res.string.log_session, FeatherIcons.Edit, onLogSessionClick),
        ),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun ExpandableFABPreview() {
    LeafLogTheme {
        Box(Modifier.size(320.dp, 200.dp)) {
            ExpandableFAB(
                onLogSessionClick = {},
                onQuickTimerClick = {},
                onAddTeaClick = {},
                snackbarHostState = remember { SnackbarHostState() },
            )
        }
    }
}
