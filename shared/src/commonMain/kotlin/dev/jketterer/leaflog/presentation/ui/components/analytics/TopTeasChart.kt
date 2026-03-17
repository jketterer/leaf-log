package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TopTea
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Instant

@Composable
fun TopTeasChart(
    topTeas: List<TopTea>,
    onTapTea: (teaId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Top Teas",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (topTeas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val maxCount = remember(topTeas) {
                    topTeas.maxOf { it.sessionCount }.coerceAtLeast(1)
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    topTeas.forEachIndexed { index, topTea ->
                        TopTeaRow(
                            topTea = topTea,
                            maxCount = maxCount,
                            rank = index,
                            onClick = { onTapTea(topTea.tea.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopTeaRow(
    topTea: TopTea,
    maxCount: Int,
    rank: Int,
    onClick: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val barColor = remember(rank) {
        primaryColor.copy(alpha = 1f - (rank * 0.15f).coerceAtMost(0.6f))
    }

    var animationTriggered by remember { mutableStateOf(false) }
    val barFraction by animateFloatAsState(
        targetValue = if (animationTriggered) topTea.sessionCount.toFloat() / maxCount else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = rank * 100),
        label = "bar",
    )

    LaunchedEffect(Unit) {
        animationTriggered = true
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = topTea.tea.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(100.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = topTea.sessionCount.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TopTeasChartPreview() {
    val now = Instant.fromEpochMilliseconds(0)
    LeafLogTheme {
        TopTeasChart(
            topTeas = listOf(
                TopTea(
                    tea = Tea(id = "1", name = "Dragon Well", teaTypeId = "1", createdAt = now, updatedAt = now, syncStatus = SyncStatus.LOCAL_ONLY),
                    sessionCount = 12,
                ),
                TopTea(
                    tea = Tea(id = "2", name = "Tie Guan Yin", teaTypeId = "2", createdAt = now, updatedAt = now, syncStatus = SyncStatus.LOCAL_ONLY),
                    sessionCount = 8,
                ),
                TopTea(
                    tea = Tea(id = "3", name = "Earl Grey", teaTypeId = "3", createdAt = now, updatedAt = now, syncStatus = SyncStatus.LOCAL_ONLY),
                    sessionCount = 6,
                ),
                TopTea(
                    tea = Tea(id = "4", name = "Sencha", teaTypeId = "1", createdAt = now, updatedAt = now, syncStatus = SyncStatus.LOCAL_ONLY),
                    sessionCount = 4,
                ),
                TopTea(
                    tea = Tea(id = "5", name = "Silver Needle", teaTypeId = "4", createdAt = now, updatedAt = now, syncStatus = SyncStatus.LOCAL_ONLY),
                    sessionCount = 2,
                ),
            ),
            onTapTea = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TopTeasChartEmptyPreview() {
    LeafLogTheme {
        TopTeasChart(
            topTeas = emptyList(),
            onTapTea = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
