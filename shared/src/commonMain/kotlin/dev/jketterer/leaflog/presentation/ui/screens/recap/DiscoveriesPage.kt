package dev.jketterer.leaflog.presentation.ui.screens.recap

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.recap_discoveries
import org.jetbrains.compose.resources.pluralStringResource

@Composable
internal fun DiscoveriesPage(discoveries: RecapPage.Discoveries, onTapTea: (teaId: String) -> Unit) {
    RecapPageColumn {
        Text(
            text = discoveries.teas.size.toString(),
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = pluralStringResource(Res.plurals.recap_discoveries, discoveries.teas.size),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(24.dp))
        discoveries.teas.forEach { RecapTeaRow(it, onTapTea) }
    }
}

@Preview
@Composable
private fun DiscoveriesPagePreview() {
    LeafLogTheme {
        RecapPageSurface(index = 3) {
            DiscoveriesPage(
                discoveries = RecapPage.Discoveries(
                    listOf("Long Jing Shi Feng 2026 First Flush", "Dan Cong Mi Lan Xiang", "Yunnan Golden Snail")
                        .mapIndexed { index, name ->
                            Tea(
                                id = "$index",
                                name = name,
                                teaTypeId = "green",
                                createdAt = PreviewInstant,
                                updatedAt = PreviewInstant,
                                syncStatus = SyncStatus.LOCAL_ONLY,
                            )
                        },
                ),
                onTapTea = {},
            )
        }
    }
}
