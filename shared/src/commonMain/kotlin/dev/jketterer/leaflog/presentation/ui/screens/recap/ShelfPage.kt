package dev.jketterer.leaflog.presentation.ui.screens.recap

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.recap_closing
import leaf_log.shared.generated.resources.recap_finished
import leaf_log.shared.generated.resources.recap_waiting
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ShelfPage(shelf: RecapPage.Shelf, onTapTea: (teaId: String) -> Unit) {
    RecapPageColumn {
        // Leads with the finished teas when there are any, the waiting ones a size down
        val typography = MaterialTheme.typography
        if (shelf.finished.isNotEmpty()) {
            ShelfGroup(shelf.finished, Res.plurals.recap_finished, typography.displayLarge, onTapTea)
            Spacer(modifier = Modifier.height(32.dp))
        }
        if (shelf.waiting.isNotEmpty()) {
            val numberStyle = if (shelf.finished.isEmpty()) typography.displayLarge else typography.displaySmall
            ShelfGroup(shelf.waiting, Res.plurals.recap_waiting, numberStyle, onTapTea)
            Spacer(modifier = Modifier.height(32.dp))
        }
        Text(
            text = stringResource(Res.string.recap_closing),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
private fun ShelfGroup(
    teas: List<Tea>,
    label: PluralStringResource,
    numberStyle: TextStyle,
    onTapTea: (teaId: String) -> Unit,
) {
    Text(
        text = teas.size.toString(),
        style = numberStyle,
    )
    Text(
        text = pluralStringResource(label, teas.size),
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(modifier = Modifier.height(16.dp))
    teas.forEach { RecapTeaRow(it, onTapTea) }
}

@Preview
@Composable
private fun ShelfPagePreview() {
    LeafLogTheme {
        RecapPageSurface(index = 7) {
            ShelfPage(
                shelf = RecapPage.Shelf(
                    finished = previewTeas("Long Jing Shi Feng 2026 First Flush", "Yunnan Golden Snail"),
                    waiting = previewTeas("Lapsang Souchong", "Bai Mu Dan 2019 Cake", "Shou Mei"),
                ),
                onTapTea = {},
            )
        }
    }
}

@Preview
@Composable
private fun ShelfPageWaitingOnlyPreview() {
    LeafLogTheme {
        RecapPageSurface(index = 7) {
            ShelfPage(
                shelf = RecapPage.Shelf(finished = emptyList(), waiting = previewTeas("Lapsang Souchong")),
                onTapTea = {},
            )
        }
    }
}

private fun previewTeas(vararg names: String) = names.mapIndexed { index, name ->
    Tea(
        id = "$index",
        name = name,
        teaTypeId = "green",
        createdAt = PreviewInstant,
        updatedAt = PreviewInstant,
        syncStatus = SyncStatus.LOCAL_ONLY,
    )
}
