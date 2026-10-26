package dev.jketterer.leaflog.presentation.ui.components.recap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import dev.jketterer.leaflog.domain.models.CalendarYear
import dev.jketterer.leaflog.domain.models.Hemisphere
import dev.jketterer.leaflog.domain.models.RecapPeriod
import dev.jketterer.leaflog.domain.models.Season
import dev.jketterer.leaflog.presentation.ui.components.common.SheetPreviewContainer
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.LocalDate
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.recaps
import org.jetbrains.compose.resources.stringResource

/** Every recap, the newest ending first, each opening it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecapSheet(
    recaps: List<RecapPeriod>,
    hemisphere: Hemisphere?,
    onOpen: (RecapPeriod) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        RecapSheetContent(recaps, hemisphere, onOpen)
    }
}

@Composable
private fun RecapSheetContent(
    recaps: List<RecapPeriod>,
    hemisphere: Hemisphere?,
    onOpen: (RecapPeriod) -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(Res.string.recaps),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        recaps.forEach { period ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { onOpen(period) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = recapLabel(period, hemisphere),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(imageVector = FeatherIcons.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Preview
@Composable
private fun RecapSheetPreview() {
    LeafLogTheme {
        SheetPreviewContainer(height = 360.dp) {
            RecapSheetContent(
                recaps = listOf(
                    CalendarYear(2025),
                    Season(LocalDate(2025, 9, 1)),
                    Season(LocalDate(2025, 6, 1)),
                    Season(LocalDate(2025, 3, 1)),
                ),
                hemisphere = Hemisphere.NORTHERN,
                onOpen = {},
            )
        }
    }
}
