package dev.jketterer.leaflog.presentation.ui.components.recap

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import dev.jketterer.leaflog.domain.models.Hemisphere
import dev.jketterer.leaflog.domain.models.RecapPeriod
import dev.jketterer.leaflog.domain.models.Season
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.LocalDate

/** A new recap, as a tonal hero card opening it. */
@Composable
fun RecapCard(
    period: RecapPeriod,
    hemisphere: Hemisphere?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
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
                    text = recapLabel(period, hemisphere),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = recapTitle(period, hemisphere),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Icon(imageVector = FeatherIcons.ChevronRight, contentDescription = null)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecapCardPreview() {
    LeafLogTheme {
        RecapCard(
            period = Season(LocalDate(2026, 6, 1)),
            hemisphere = Hemisphere.NORTHERN,
            onOpen = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
