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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import compose.icons.FeatherIcons
import compose.icons.feathericons.Star
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TopRatedTea
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Instant

@Composable
fun TopRatedTeasChart(
    topRatedTeas: List<TopRatedTea>,
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
                text = "Highest Rated Teas",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (topRatedTeas.isEmpty()) {
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    topRatedTeas.forEachIndexed { index, topRatedTea ->
                        TopRatedTeaRow(
                            topRatedTea = topRatedTea,
                            rank = index,
                            onClick = { onTapTea(topRatedTea.tea.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopRatedTeaRow(
    topRatedTea: TopRatedTea,
    rank: Int,
    onClick: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.tertiary
    val barColor = remember(rank) {
        primaryColor.copy(alpha = 1f - (rank * 0.15f).coerceAtMost(0.6f))
    }

    var animationTriggered by remember { mutableStateOf(false) }
    val barFraction by animateFloatAsState(
        targetValue = if (animationTriggered) topRatedTea.averageRating / 5f else 0f,
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
        Column(
            modifier = Modifier.width(100.dp),
        ) {
            Text(
                text = topRatedTea.tea.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${topRatedTea.ratedSessionCount} sessions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = topRatedTea.averageRating.formatOneDecimal(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = FeatherIcons.Star,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

private fun Float.formatOneDecimal(): String {
    val tenths = (this * 10).toInt()
    return "${tenths / 10}.${tenths % 10}"
}

@Preview(showBackground = true)
@Composable
private fun TopRatedTeasChartPreview() {
    val now = Instant.fromEpochMilliseconds(0)
    LeafLogTheme {
        TopRatedTeasChart(
            topRatedTeas = listOf(
                TopRatedTea(
                    tea = Tea(
                        id = "1",
                        name = "Dragon Well",
                        teaTypeId = "1",
                        createdAt = now,
                        updatedAt = now,
                        syncStatus = SyncStatus.LOCAL_ONLY
                    ),
                    averageRating = 4.8f,
                    ratedSessionCount = 6,
                ),
                TopRatedTea(
                    tea = Tea(
                        id = "2",
                        name = "Tie Guan Yin",
                        teaTypeId = "2",
                        createdAt = now,
                        updatedAt = now,
                        syncStatus = SyncStatus.LOCAL_ONLY
                    ),
                    averageRating = 4.5f,
                    ratedSessionCount = 4,
                ),
                TopRatedTea(
                    tea = Tea(
                        id = "3",
                        name = "Earl Grey",
                        teaTypeId = "3",
                        createdAt = now,
                        updatedAt = now,
                        syncStatus = SyncStatus.LOCAL_ONLY
                    ),
                    averageRating = 4.2f,
                    ratedSessionCount = 3,
                ),
                TopRatedTea(
                    tea = Tea(
                        id = "4",
                        name = "Sencha",
                        teaTypeId = "1",
                        createdAt = now,
                        updatedAt = now,
                        syncStatus = SyncStatus.LOCAL_ONLY
                    ),
                    averageRating = 3.9f,
                    ratedSessionCount = 5,
                ),
                TopRatedTea(
                    tea = Tea(
                        id = "5",
                        name = "Silver Needle",
                        teaTypeId = "4",
                        createdAt = now,
                        updatedAt = now,
                        syncStatus = SyncStatus.LOCAL_ONLY
                    ),
                    averageRating = 3.7f,
                    ratedSessionCount = 2,
                ),
            ),
            onTapTea = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TopRatedTeasChartEmptyPreview() {
    LeafLogTheme {
        TopRatedTeasChart(
            topRatedTeas = emptyList(),
            onTapTea = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
