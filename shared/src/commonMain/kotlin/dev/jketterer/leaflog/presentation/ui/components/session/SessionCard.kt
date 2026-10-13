package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Thermometer
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeFormatter
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.FullscreenImageViewer
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.ic_tea_leaf
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * One session: its first steep's parameters, its rating across steeps, and when it started.
 */
@Composable
fun SessionCard(
    modifier: Modifier = Modifier,
    session: SessionWithSteeps,
    teaName: String,
    teaTypeName: String,
    vesselName: String,
    teaPhotoUrl: String?,
    userPrefs: UserPreferences,
    onSessionClick: () -> Unit,
    onBrewAgainClick: () -> Unit,
    onDeleteClick: () -> Unit,
    canBrewAgain: Boolean = true,
    // Null in a preview, where no Koin application is running and resolving would throw.
    imageStorage: ImageStorage? = if (LocalInspectionMode.current) null else koinInject(),
) {
    var showMenu by remember { mutableStateOf(false) }
    var showPhotoViewer by remember { mutableStateOf(false) }
    val first = session.steeps.first()
    val photos = session.steeps.flatMap { it.photos }
    val notes = session.steeps.firstNotNullOfOrNull { it.notes }
    val rating = session.effectiveRating

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
            if (photos.isNotEmpty() && imageStorage != null) {
                AsyncImage(
                    model = imageStorage.resolveImagePath(photos.first()),
                    contentDescription = teaName,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { showPhotoViewer = true },
                    contentScale = ContentScale.Crop
                )
            } else if (teaPhotoUrl != null && imageStorage != null) {
                AsyncImage(
                    model = imageStorage.resolveImagePath(teaPhotoUrl),
                    contentDescription = teaName,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }

            if (photos.isNotEmpty() || teaPhotoUrl != null) {
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Session info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // In Progress badge
                if (session.session.status == SessionStatus.IN_PROGRESS) {
                    AssistChip(
                        onClick = {},
                        label = { Text("IN PROGRESS") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }

                Text(
                    text = teaName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "$teaTypeName • $vesselName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    session.session.teaQuantityGrams?.let {
                        BrewingParamChip(vectorResource(Res.drawable.ic_tea_leaf), "${it}g")
                    }
                    BrewingParamChip(
                        FeatherIcons.Thermometer,
                        TemperatureFormatter.format(
                            first.temperatureCelsius,
                            userPrefs.temperatureUnit
                        ),
                    )
                    BrewingParamChip(
                        FeatherIcons.Clock,
                        formatBrewingTime(first.brewingTime.inWholeSeconds.toInt()),
                    )
                    BrewingParamChip(
                        FeatherIcons.Droplet,
                        VolumeFormatter.format(first.waterQuantityMl, userPrefs.volumeUnit),
                    )
                }

                val timeText = TimeFormatter.formatRelativeTimestamp(session.startedAt)

                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = timeText + if (rating != null) " • " else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (rating != null) {
                        RatingDisplay(rating)
                    }
                }

                if (notes != null) {
                    Text(
                        text = notes,
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
                        imageVector = FeatherIcons.MoreVertical,
                        contentDescription = "More options"
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (session.session.status == SessionStatus.COMPLETED && canBrewAgain) {
                        DropdownMenuItem(
                            text = { Text("Brew Again") },
                            onClick = {
                                showMenu = false
                                onBrewAgainClick()
                            }
                        )
                    }
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

    if (showPhotoViewer && photos.isNotEmpty() && imageStorage != null) {
        FullscreenImageViewer(
            photos = photos.map { imageStorage.resolveImagePath(it) },
            initialIndex = 0,
            onDismiss = { showPhotoViewer = false },
        )
    }
}

@Preview
@Composable
private fun SessionCardCompletedPreview() {
    LeafLogTheme {
        SessionCard(
            session = previewSession(
                ratings = listOf(4.5f, 4f, null),
                status = SessionStatus.COMPLETED,
                brewingTime = 3.minutes,
                temperatureCelsius = 95.0,
                teaQuantityGrams = 4f,
                notes = "Perfect brewing time, great flavor profile with hints of honey",
            ),
            teaName = "Dragon Well Green Tea",
            teaTypeName = "Green Tea",
            vesselName = "Gaiwan",
            teaPhotoUrl = null,
            userPrefs = UserPreferences.IMPERIAL,
            onSessionClick = {},
            onDeleteClick = {},
            onBrewAgainClick = {},
            imageStorage = null,
        )
    }
}

@Preview
@Composable
private fun SessionCardInProgressPreview() {
    LeafLogTheme {
        SessionCard(
            session = previewSession(
                brewingTime = 4.minutes,
                temperatureCelsius = 100.0,
                waterQuantityMl = 250.0,
                teaQuantityGrams = null,
            ),
            teaName = "English Breakfast",
            teaTypeName = "Black Tea",
            vesselName = "Mug",
            teaPhotoUrl = null,
            userPrefs = UserPreferences.METRIC,
            onSessionClick = {},
            onDeleteClick = {},
            onBrewAgainClick = {},
            imageStorage = null,
        )
    }
}

@Preview
@Composable
private fun SessionCardNoRatingPreview() {
    LeafLogTheme {
        SessionCard(
            session = previewSession(
                status = SessionStatus.COMPLETED,
                brewingTime = 45.seconds,
                temperatureCelsius = 85.0,
                waterQuantityMl = 150.0,
                teaQuantityGrams = null,
                notes = "Quick morning steep, didn't have time to rate",
            ),
            teaName = "Jasmine Silver Needle",
            teaTypeName = "White Tea",
            vesselName = "Teapot",
            teaPhotoUrl = null,
            userPrefs = UserPreferences.IMPERIAL,
            onSessionClick = {},
            onDeleteClick = {},
            onBrewAgainClick = {},
            imageStorage = null,
        )
    }
}

@Preview
@Composable
private fun SessionCardLongNamesPreview() {
    LeafLogTheme {
        SessionCard(
            session = previewSession(
                ratings = listOf(5f),
                status = SessionStatus.COMPLETED,
                temperatureCelsius = 80.0,
                waterQuantityMl = 175.0,
                notes = "This is a very long note that will be truncated with an ellipsis because it exceeds the maximum line limit for the card display",
            ),
            teaName = "Fujian Province Premium Grade Dragon Phoenix Pearl Green Tea",
            teaTypeName = "Green Tea - Premium Grade",
            vesselName = "Black Teapot",
            teaPhotoUrl = null,
            userPrefs = UserPreferences(),
            onSessionClick = {},
            onDeleteClick = {},
            onBrewAgainClick = {},
            imageStorage = null,
        )
    }
}
