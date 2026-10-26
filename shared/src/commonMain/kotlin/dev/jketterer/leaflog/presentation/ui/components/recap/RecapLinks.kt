package dev.jketterer.leaflog.presentation.ui.components.recap

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
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
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.LocalDate

/** The newest recap as a hero card, then the ones before it. Nothing when [recaps] is empty. */
@Composable
fun RecapLinks(
    recaps: List<RecapPeriod>,
    hemisphere: Hemisphere?,
    onOpen: (RecapPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val newest = recaps.firstOrNull() ?: return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            onClick = { onOpen(newest) },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recapLabel(newest, hemisphere),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = recapTitle(newest, hemisphere),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                Icon(imageVector = FeatherIcons.ChevronRight, contentDescription = null)
            }
        }
        if (recaps.size > 1) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                recaps.drop(1).forEach { period ->
                    SuggestionChip(
                        onClick = { onOpen(period) },
                        label = { Text(recapLabel(period, hemisphere)) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecapLinksPreview() {
    LeafLogTheme {
        RecapLinks(
            recaps = listOf(
                CalendarYear(2025),
                Season(LocalDate(2025, 9, 1)),
                Season(LocalDate(2025, 6, 1)),
                Season(LocalDate(2025, 3, 1)),
            ),
            hemisphere = Hemisphere.NORTHERN,
            onOpen = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
