package dev.jketterer.leaflog.presentation.ui.components.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/**
 * Card displaying a single daily statistic.
 */
@Composable
fun DailyStatsCard(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val baseStyle = MaterialTheme.typography.headlineMedium
            var scaleFactor by remember { mutableFloatStateOf(1f) }
            Text(
                text = value,
                style = baseStyle.copy(fontSize = baseStyle.fontSize * scaleFactor),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                onTextLayout = { result ->
                    if (result.hasVisualOverflow) {
                        scaleFactor *= 0.9f
                    }
                },
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyStatsCardPreview() {
    LeafLogTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DailyStatsCard(
                value = "3",
                label = "Sessions",
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            DailyStatsCard(
                value = "450ml",
                label = "Water",
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            DailyStatsCard(
                value = "2",
                label = "Teas",
                onClick = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}