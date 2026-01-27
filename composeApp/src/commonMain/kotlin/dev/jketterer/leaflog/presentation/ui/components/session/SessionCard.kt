package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Expand
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

/**
 * Card component displaying session summary information.
 */
@Composable
fun SessionCard(
    session: TeaSession,
    teaName: String,
    teaTypeName: String,
    teaPhotoUrl: String?,
    onSessionClick: () -> Unit,
    onBrewAgainClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSessionClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tea photo
            if (session.photos.isNotEmpty()) {
                AsyncImage(
                    model = session.photos.first(),
                    contentDescription = teaName,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            } else if (teaPhotoUrl != null) {
                AsyncImage(
                    model = teaPhotoUrl,
                    contentDescription = teaName,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }

            if (session.photos.isNotEmpty() || teaPhotoUrl != null) {
                Spacer(modifier = Modifier.width(16.dp))
            }

            // Session info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Draft badge
                if (session.status == SessionStatus.DRAFT) {
                    AssistChip(
                        onClick = {},
                        label = { Text("DRAFT") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = teaName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "$teaTypeName • ${session.brewingTime} • ${session.temperatureCelsius}°C",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val timeText = session.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
                    .let { "${it.hour}:${it.minute.toString().padStart(2, '0')}" }

                Text(
                    text = timeText + if (session.rating != null) " • ${"⭐".repeat(session.rating.toInt())}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (session.notes != null) {
                    Text(
                        text = session.notes,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = FontAwesomeIcons.Solid.Expand,
                        contentDescription = "More options"
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Brew Again") },
                        onClick = {
                            showMenu = false
                            onBrewAgainClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SessionCardPreview() {
    LeafLogTheme {
        SessionCard(
            session = TeaSession(
                id = "0",
                teaId = "1",
                steepNumber = 1,
                vesselId = "1",
                waterType = WaterType.FILTERED,
                timestamp = Instant.fromEpochMilliseconds(1),
                status = SessionStatus.COMPLETED,
                brewingTime = 200.toDuration(DurationUnit.SECONDS),
                updatedAt = Instant.fromEpochMilliseconds(1),
                deletedAt = Instant.fromEpochMilliseconds(1),
                createdAt = Instant.fromEpochMilliseconds(1),
                temperatureCelsius = 95,
                waterQuantityMl = 400,
                photos = emptyList(),
                syncStatus = SyncStatus.LOCAL_ONLY,
            ),
            teaName = "Test Tea",
            teaTypeName = "Black Tea",
            teaPhotoUrl = null,
            onSessionClick = {},
            onDeleteClick = {},
            onBrewAgainClick = {}
        )
    }
}