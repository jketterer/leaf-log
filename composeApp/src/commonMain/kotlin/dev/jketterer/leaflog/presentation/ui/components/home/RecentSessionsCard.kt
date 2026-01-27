package dev.jketterer.leaflog.presentation.ui.components.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Compact session card for recent sessions list on home screen.
 */
@Composable
fun RecentSessionCard(
    teaName: String,
    teaTypeName: String,
    session: TeaSession,
    teaPhotoUrl: String? = null,
    onSessionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSessionClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tea photo thumbnail
            if (session.photos.isNotEmpty()) {
                AsyncImage(
                    model = session.photos.first(),
                    contentDescription = teaName,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else if (teaPhotoUrl != null) {
                AsyncImage(
                    model = teaPhotoUrl,
                    contentDescription = teaName,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Session info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = teaName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$teaTypeName • ${session.brewingTime} • ${session.temperatureCelsius}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val timeText = session.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
                    .let { "${it.hour}:${it.minute.toString().padStart(2, '0')}" }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecentSessionCardPreview() {
    LeafLogTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecentSessionCard(
                teaName = "Dragon Well",
                teaTypeName = "Green",
                session = TeaSession(
                    id = "1",
                    teaId = "tea-1",
                    vesselId = "gaiwan",
                    waterType = WaterType.FILTERED,
                    timestamp = Clock.System.now(),
                    brewingTime = 2.minutes + 30.seconds,
                    temperatureCelsius = 80,
                    waterQuantityMl = 200,
                    steepNumber = 1,
                    status = SessionStatus.COMPLETED,
                    syncStatus = SyncStatus.LOCAL_ONLY,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                ),
                onSessionClick = {},
            )
            RecentSessionCard(
                teaName = "Sencha",
                teaTypeName = "Green",
                session = TeaSession(
                    id = "2",
                    teaId = "tea-2",
                    vesselId = "kyusu",
                    waterType = WaterType.FILTERED,
                    timestamp = Clock.System.now(),
                    brewingTime = 2.minutes,
                    temperatureCelsius = 75,
                    waterQuantityMl = 150,
                    status = SessionStatus.COMPLETED,
                    syncStatus = SyncStatus.LOCAL_ONLY,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                ),
                onSessionClick = {},
            )
        }
    }
}